/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.authentication.jpa.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.AuthenticationConstants;
import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.repository.ActiveSessionRepository;
import eu.arrowhead.authentication.jpa.repository.PasswordAuthenticationRepository;
import eu.arrowhead.authentication.jpa.repository.SystemRepository;
import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.service.dto.IdentityData;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityListMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityMgmtRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentityQueryRequestDTO;
import eu.arrowhead.authentication.service.dto.NormalizedIdentitySessionQueryRequestDTO;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;

@Service
public class IdentityDbService {

	//=================================================================================================
	// members

	private static final Object SYSTEM_LOCK = new Object();
	private static final Object SESSION_LOCK = new Object();

	@Value(AuthenticationConstants.$IDENTITY_TOKEN_DURATION)
	private int identityTokenDuration;

	@Autowired
	private SystemRepository systemRepository;

	@Autowired
	private PasswordAuthenticationRepository paRepository;

	@Autowired
	private ActiveSessionRepository asRepository;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<System> getSystemByName(final String systemName) {
		logger.debug("getSystemByName started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");

		try {
			return systemRepository.findByName(systemName);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<System> getSystemsByNames(final List<String> names, final boolean strict) {
		logger.debug("getSystemsByNames started...");
		Assert.isTrue(!Utilities.isEmpty(names), "names is missing or empty");
		Assert.isTrue(!Utilities.containsNullOrEmpty(names), "names contains null or empty element");

		try {
			final List<System> result = systemRepository.findAllByNameIn(names);
			if (strict && result.size() != names.size()) {
				final List<String> missing = new ArrayList<>(names);
				missing.removeAll(result.stream().map(s -> s.getName()).toList());

				throw new InvalidParameterException("The following systems are not found: " + String.join(", ", missing));
			}

			return result;
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void changePassword(final System system, final String newPassword) {
		logger.debug("changePassword started...");
		Assert.notNull(system, "system is null");
		Assert.isTrue(!Utilities.isEmpty(newPassword), "newPassword is null or empty");

		try {
			final Optional<PasswordAuthentication> authOpt = paRepository.findBySystem(system);
			if (authOpt.isEmpty()) {
				throw new InvalidParameterException("Entry for system " + system.getName() + " is not found");
			}

			final PasswordAuthentication auth = authOpt.get();
			auth.setPassword(newPassword);
			paRepository.saveAndFlush(auth);
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<System> createIdentifiableSystemsInBulk(final String requester, final NormalizedIdentityListMgmtRequestDTO dto) {
		logger.debug("createIdentifiableSystemsInBulk started...");
		Assert.isTrue(!Utilities.isEmpty(requester), "Requester is missing or empty");
		Assert.notNull(dto, "Payload is missing");
		Assert.notNull(dto.authenticationMethod(), "Authentication method is missing");
		Assert.isTrue(!Utilities.isEmpty(dto.identities()), "Identities is missing or empty");
		Assert.isTrue(!Utilities.containsNull(dto.identities()), "Identities contains null");

		List<IdentityData> identityList;
		List<System> systems;
		synchronized (SYSTEM_LOCK) {
			try {
				checkSystemNamesNotExist(dto.identities()); // checks if none of the system names exist (system name has to be unique)

				// writing the system entities to the database
				identityList = createSystemEntitiesAndIdentityList(requester, dto.authenticationMethod(), dto.identities());
				systems = systemRepository.saveAllAndFlush(identityList.stream().map(id -> id.system()).toList());
				identityList = updateSystemEntitiesInIdentityList(identityList, systems);
			} catch (final InvalidParameterException ex) {
				throw ex;
			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				throw new InternalServerError("Database operation error");
			}

			// storing authentication method specific credentials
			final List<String> extras = dto.authenticationMethod().dbService().createIdentifiableSystemsInBulk(identityList);

			// handling the extra fields
			if (extras != null) {
				// check if extras list size is correct
				if (extras.size() != identityList.size()) {
					// something is not right => roll back everything
					logger.error("Extra list's size is incorrect");
					dto.authenticationMethod().dbService().rollbackCreateIdentifiableSystemsInBulk(identityList);
					throw new InternalServerError("Database operation error");
				}

				try {
					systems = systemRepository.saveAllAndFlush(updateSystemListWithExtras(systems, extras));
				} catch (final Exception ex) {
					logger.error(ex.getMessage());
					logger.debug(ex);
					dto.authenticationMethod().dbService().rollbackCreateIdentifiableSystemsInBulk(identityList);
					throw new InternalServerError("Database operation error");
				}
			}
		}

		return systems;
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<System> updateIdentifiableSystemsInBulk(final IAuthenticationMethod authenticationMethod, final String requester, final List<NormalizedIdentityMgmtRequestDTO> identities) {
		logger.debug("updateIdentifiableSystemsInBulk started...");
		Assert.notNull(authenticationMethod, "Authentication method is missing");
		Assert.isTrue(!Utilities.isEmpty(requester), "Requester is missing or empty");
		Assert.isTrue(!Utilities.isEmpty(identities), "Identities is missing or empty");
		Assert.isTrue(!Utilities.containsNull(identities), "Identities contains null");

		synchronized (SYSTEM_LOCK) {
			final List<IdentityData> identityList;

			// collect the system entities from the database
			identityList = getSystemEntitiesForUpdate(authenticationMethod, identities);

			// updating authentication method specific credentials
			final List<String> extras = authenticationMethod.dbService().updateIdentifiableSystemsInBulk(identityList);

			// checking the extra fields
			if (extras != null) {
				// check if extras list size is correct
				if (extras.size() != identityList.size()) {
					// something is not right => roll back everything
					logger.error("Extra list's size is incorrect");
					authenticationMethod.dbService().rollbackUpdateIdentifiableSystemsInBulk(identityList);
					throw new InternalServerError("Database operation error");
				}
			}

			// updating the authentication method independent fields
			try {
				List<System> systems = new ArrayList<>(identityList.size());
				for (int i = 0; i < identityList.size(); ++i) {
					final System system = identityList.get(i).system();
					system.setUpdatedBy(requester);
					system.setSysop(identityList.get(i).sysop());
					if (extras != null) {
						system.setExtra(extras.get(i));
					}
					systems.add(system);
				}

				systems = systemRepository.saveAllAndFlush(systems);
				authenticationMethod.dbService().commitUpdateIdentifiableSystemsInBulk(identityList);

				return systems;
			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				authenticationMethod.dbService().rollbackUpdateIdentifiableSystemsInBulk(identityList);
				throw new InternalServerError("Database operation error");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void removeIdentifiableSystemsInBulk(final IAuthenticationMethod authenticationMethod, final List<String> names) {
		logger.debug("removeIdentifiableSystemsInBulk started...");
		Assert.notNull(authenticationMethod, "Authentication method is missing");
		Assert.isTrue(!Utilities.isEmpty(names), "Names is missing or empty");
		Assert.isTrue(!Utilities.containsNullOrEmpty(names), "Names contains null or empty value");

		final List<System> systems = getSystemsByNames(names, false);

		// all authentication methods have to be the same in related systems (check again in a transaction)
		final Optional<System> problematicSystem = systems.stream().filter(s -> s.getAuthenticationMethod() != authenticationMethod.type()).findFirst();
		if (problematicSystem.isPresent()) {
			throw new InvalidParameterException("Bulk removing systems with different authentication methods is not supported");
		}

		authenticationMethod.dbService().removeIdentifiableSystemsInBulk(systems);

		try {
			systemRepository.deleteAllInBatch(systems);
			systemRepository.flush();
			authenticationMethod.dbService().commitRemoveIdentifiableSystemsInBulk(systems);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			authenticationMethod.dbService().rollbackRemoveIdentifiableSystemsInBulk(systems);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<System> queryIdentifiableSystems(final NormalizedIdentityQueryRequestDTO dto) {
		logger.debug("queryIdentifiableSystems started...");
		Assert.notNull(dto, "Payload is missing");
		Assert.notNull(dto.pageRequest(), "Page request is missing");

		try {
			if (!dto.hasFilters()) {
				return systemRepository.findAll(dto.pageRequest());
			}

			return findIdentifiableSystemsByFilters(dto);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<ActiveSession> getSessionByToken(final String token) {
		logger.debug("getSessionByToken started...");
		Assert.isTrue(!Utilities.isEmpty(token), "Token is missing or empty");

		try {
			return asRepository.findByToken(token);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<ActiveSession> querySessions(final NormalizedIdentitySessionQueryRequestDTO dto) {
		logger.debug("querySessions started...");
		Assert.notNull(dto, "Payload is missing");
		Assert.notNull(dto.pageRequest(), "Page request is missing");

		try {
			if (!dto.hasFilters()) {
				return asRepository.findAll(dto.pageRequest());
			}

			return findSessionsByFilters(dto);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public ActiveSession createOrUpdateSession(final System system, final String token) {
		logger.debug("createOrUpdateSession started...");
		Assert.notNull(system, "system is null");
		Assert.isTrue(!Utilities.isEmpty(token), "Token is missing or empty");

		synchronized (SESSION_LOCK) {
			try {
				final Optional<ActiveSession> sessionOpt = asRepository.findBySystem(system);
				final ZonedDateTime now = Utilities.utcNow();
				final ZonedDateTime expirationTime = identityTokenDuration > 0 ? now.plusSeconds(identityTokenDuration) : now.plusYears(AuthenticationConstants.INFINITE_TOKEN_DURATION);

				final ActiveSession session = sessionOpt.isPresent() ? sessionOpt.get() : new ActiveSession(system, token, now, expirationTime);
				if (sessionOpt.isPresent()) {
					if (now.isAfter(session.getExpirationTime())) {
						// session is already expired, but the record is not removed yet => re-using it
						session.setLoginTime(now);
					}

					// update
					session.setToken(token);
					session.setExpirationTime(expirationTime);
				}

				return asRepository.saveAndFlush(session);
			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				throw new InternalServerError("Database operation error");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void removeSession(final String systemName) {
		logger.debug("removeSession started...");
		Assert.isTrue(!Utilities.isEmpty(systemName), "System name is missing or empty");

		synchronized (SESSION_LOCK) {
			try {
				asRepository.deleteBySystem_Name(systemName);
				asRepository.flush();
			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				throw new InternalServerError("Database operation error");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void closeSessionsInBulk(final List<String> systemNames) {
		logger.debug("closeSessionsInBulk started...");
		Assert.isTrue(!Utilities.isEmpty(systemNames), "System name list is missing or empty");
		Assert.isTrue(!Utilities.containsNullOrEmpty(systemNames), "System name list contains null or empty value");

		synchronized (SESSION_LOCK) {
			try {
				asRepository.deleteBySystem_NameIn(systemNames);
				asRepository.flush();
			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				throw new InternalServerError("Database operation error");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void removeExpiredSessions() {
		logger.debug("removeExpiredSessions started...");

		final ZonedDateTime now = Utilities.utcNow();
		synchronized (SESSION_LOCK) {
			try {
				asRepository.deleteByExpirationTimeLessThan(now);
				asRepository.flush();
			} catch (final Exception ex) {
				logger.error(ex.getMessage());
				logger.debug(ex);
				throw new InternalServerError("Database operation error");
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<PasswordAuthentication> getPasswordAuthenticationBySystem(final System system) {
		logger.debug("getPasswordAuthenticationBySystem started...");
		Assert.notNull(system, "system is null");

		try {
			return paRepository.findBySystem(system);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// checks if system name already exists, throws exception if it does
	private void checkSystemNamesNotExist(final List<NormalizedIdentityMgmtRequestDTO> candidates) {
		logger.debug("checkSystemNamesNotExist started");

		final List<String> candidateNames = candidates
				.stream()
				.filter(c -> c.systemName() != null)
				.map(c -> c.systemName())
				.collect(Collectors.toList());

		final List<System> existingSystems = systemRepository.findAllByNameIn(candidateNames);

		if (!Utilities.isEmpty(existingSystems)) {
			final String existingSystemNames = existingSystems
					.stream()
					.map(e -> e.getName())
					.collect(Collectors.joining(", "));
			throw new InvalidParameterException("Identifiable systems with names already exist: " + existingSystemNames);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private List<IdentityData> createSystemEntitiesAndIdentityList(final String requester, final IAuthenticationMethod method, final List<NormalizedIdentityMgmtRequestDTO> candidates) {
		logger.debug("createSystemEntities started");

		return candidates
				.stream()
				.map(c -> new IdentityData(
						new System(c.systemName(), method.type(), c.sysop(), requester),
						c.credentials(),
						c.sysop()))
				.collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	// expects the same order in the two list
	private List<IdentityData> updateSystemEntitiesInIdentityList(final List<IdentityData> identityList, final List<System> systems) {
		logger.debug("updateSystemEntitiesInIdentityList started");

		final List<IdentityData> result = new ArrayList<>(identityList.size());
		for (int i = 0; i < identityList.size(); ++i) {
			result.add(new IdentityData(systems.get(i), identityList.get(i).credentials(), identityList.get(i).sysop()));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	// expects the same order in the two list
	private List<System> updateSystemListWithExtras(final List<System> systems, final List<String> extras) {
		logger.debug("updateSystemListWithExtras started");

		final List<System> result = new ArrayList<>(systems.size());
		for (int i = 0; i < systems.size(); ++i) {
			final System system = systems.get(i);
			system.setExtra(extras.get(i));
			result.add(system);
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private List<IdentityData> getSystemEntitiesForUpdate(final IAuthenticationMethod method, final List<NormalizedIdentityMgmtRequestDTO> identities) {
		logger.debug("getSystemEntitiesForUpdate started");

		// get the systems and also check again (now in a transaction) that all systems are in the database
		final List<System> systems = getSystemsByNames(identities.stream().map(id -> id.systemName()).toList(), true);

		// check again that authentication method is still match
		final Optional<System> problematicSystem = systems.stream().filter(s -> s.getAuthenticationMethod() != method.type()).findFirst();
		if (problematicSystem.isPresent()) {
			throw new InvalidParameterException("Bulk updating systems with different authentication methods is not supported");
		}

		return identities
				.stream()
				.map(id -> new IdentityData(
						findSystemInList(systems, id.systemName()),
						id.credentials(),
						id.sysop()))
				.collect(Collectors.toList());
	}

	//-------------------------------------------------------------------------------------------------
	private System findSystemInList(final List<System> systems, final String systemName) {
		logger.debug("findSystemInList started");

		return systems
				.stream()
				.filter(sys -> systemName.equals(sys.getName()))
				.findFirst()
				.orElse(null); // should never happen
	}

	//-------------------------------------------------------------------------------------------------
	private Page<System> findIdentifiableSystemsByFilters(final NormalizedIdentityQueryRequestDTO dto) {
		logger.debug("findIdentifiableSystemsByFilters started");

		synchronized (SYSTEM_LOCK) {
			final ZonedDateTime now = Utilities.utcNow();
			final List<Long> matchings = new ArrayList<>();
			final List<System> toFilter = dto.namePart() == null ? systemRepository.findAll() : systemRepository.findAllByNameContainsIgnoreCase(dto.namePart());

			for (final System system : toFilter) {
				// sysop
				if (dto.isSysop() != null && system.isSysop() != dto.isSysop().booleanValue()) {
					continue;
				}

				// created by
				if (dto.createdBy() != null && !system.getCreatedBy().equals(dto.createdBy())) {
					continue;
				}

				// creation from
				if (dto.creationFrom() != null && system.getCreatedAt().isBefore(dto.creationFrom())) {
					continue;
				}

				// creation to
				if (dto.creationTo() != null && system.getCreatedAt().isAfter(dto.creationTo())) {
					continue;
				}

				// has session
				if (dto.hasSession() != null) {
					final Optional<ActiveSession> sessionOpt = asRepository.findBySystem(system);
					final boolean validSession = sessionOpt.isPresent() && sessionOpt.get().getExpirationTime().isAfter(now);

					if (validSession != dto.hasSession().booleanValue()) {
						continue;
					}
				}

				matchings.add(system.getId());
			}

			return systemRepository.findAllByIdIn(dto.pageRequest(), matchings);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private Page<ActiveSession> findSessionsByFilters(final NormalizedIdentitySessionQueryRequestDTO dto) {
		logger.debug("findSessionsByFilters started");

		synchronized (SESSION_LOCK) {
			final List<Long> matchings = new ArrayList<>();
			final List<ActiveSession> toFilter = dto.namePart() == null ? asRepository.findAll() : asRepository.findAllBySystem_NameContainsIgnoreCase(dto.namePart());

			for (final ActiveSession session : toFilter) {
				// login from
				if (dto.loginFrom() != null && session.getLoginTime().isBefore(dto.loginFrom())) {
					continue;
				}

				// login to
				if (dto.loginTo() != null && session.getLoginTime().isAfter(dto.loginTo())) {
					continue;
				}

				matchings.add(session.getId());
			}

			return asRepository.findAllByIdIn(dto.pageRequest(), matchings);
		}
	}
}