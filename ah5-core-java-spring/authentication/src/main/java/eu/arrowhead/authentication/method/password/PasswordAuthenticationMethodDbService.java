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
package eu.arrowhead.authentication.method.password;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.jpa.repository.PasswordAuthenticationRepository;
import eu.arrowhead.authentication.method.IAuthenticationMethodDbService;
import eu.arrowhead.authentication.service.dto.IdentityData;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;

@Service
public class PasswordAuthenticationMethodDbService implements IAuthenticationMethodDbService {

	//=================================================================================================
	// members

	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private PasswordAuthenticationRepository paRepository;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	@Transactional(rollbackFor = ArrowheadException.class, propagation = Propagation.REQUIRED)
	public List<String> createIdentifiableSystemsInBulk(final List<IdentityData> identities) throws InternalServerError {
		logger.debug("PasswordAuthenticationMethodDbService.createIdentifiableSystemsInBulk started...");
		Assert.notNull(identities, "Identity list is missing");
		Assert.isTrue(!Utilities.containsNull(identities), "Identity list contains null value");

		try {
			final List<PasswordAuthentication> entities = createEntities(identities);
			paRepository.saveAllAndFlush(entities);

			// intentionally
			return null;
		} catch (final IllegalArgumentException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	@Transactional(rollbackFor = ArrowheadException.class, propagation = Propagation.REQUIRED)
	public List<String> updateIdentifiableSystemsInBulk(final List<IdentityData> identities) throws InternalServerError {
		logger.debug("PasswordAuthenticationMethodDbService.updateIdentifiableSystemsInBulk started...");
		Assert.notNull(identities, "Identity list is missing");
		Assert.isTrue(!Utilities.containsNull(identities), "Identity list contains null value");

		try {
			final List<PasswordAuthentication> entities = paRepository.findAllBySystemIn(
					identities.stream()
						.filter(id -> id.system() != null)
						.map(id -> id.system())
						.toList());
			updateEntities(entities, identities);
			paRepository.saveAllAndFlush(entities);

			// intentionally
			return null;
		} catch (final IllegalArgumentException | InternalServerError ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	@Transactional(rollbackFor = ArrowheadException.class, propagation = Propagation.REQUIRED)
	public void removeIdentifiableSystemsInBulk(final List<System> systems) throws InternalServerError, ExternalServerError {
		logger.debug("PasswordAuthenticationMethodDbService.removeIdentifiableSystemsInBulk started...");
		Assert.notNull(systems, "System list is missing");
		Assert.isTrue(!Utilities.containsNull(systems), "System list contains null value");

		try {
			paRepository.deleteAllBySystemIn(systems);
			paRepository.flush();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private List<PasswordAuthentication> createEntities(final List<IdentityData> identities) {
		logger.debug("PasswordAuthenticationMethodDbService.createEntities started...");

		final List<PasswordAuthentication> result = new ArrayList<>(identities.size());
		for (final IdentityData identityData : identities) {
			Assert.notNull(identityData.system(), "system is null");
			Assert.notNull(identityData.credentials(), "credentials is null");
			Assert.isTrue(!Utilities.isEmpty(identityData.credentials().get(PasswordAuthenticationMethod.KEY_PASSWORD)), "password field is missing or empty");

			final String encodedPassword = encoder.encode(identityData.credentials().get(PasswordAuthenticationMethod.KEY_PASSWORD));
			result.add(new PasswordAuthentication(identityData.system(), encodedPassword));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private void updateEntities(final List<PasswordAuthentication> entities, final List<IdentityData> identities) {
		logger.debug("PasswordAuthenticationMethodDbService.updateEntities started...");

		for (final IdentityData identityData : identities) {
			Assert.notNull(identityData.system(), "system is null");
			Assert.notNull(identityData.credentials(), "credentials is null");
			Assert.isTrue(!Utilities.isEmpty(identityData.credentials().get(PasswordAuthenticationMethod.KEY_PASSWORD)), "password field is missing or empty");

			final PasswordAuthentication relatedEntity = findEntityInList(entities, identityData.system());
			if (relatedEntity == null) {
				// should not happen
				throw new InternalServerError("Credentials for system " + identityData.system().getName() + " are not found");
			}

			final String encodedPassword = encoder.encode(identityData.credentials().get(PasswordAuthenticationMethod.KEY_PASSWORD));
			relatedEntity.setPassword(encodedPassword);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private PasswordAuthentication findEntityInList(final List<PasswordAuthentication> entities, final System system) {
		logger.debug("PasswordAuthenticationMethodDbService.findEntityInList started...");

		return entities
				.stream()
				.filter(e -> e.getSystem().equals(system))
				.findFirst()
				.orElse(null);
	}
}