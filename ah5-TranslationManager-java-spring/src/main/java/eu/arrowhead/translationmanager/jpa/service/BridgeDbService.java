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
package eu.arrowhead.translationmanager.jpa.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.TranslationBridgeEventState;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDiscovery;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDetailsRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDiscoveryRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeHeaderRepository;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationQueryRequestDTO;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationReportRequestDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;

@Service
public class BridgeDbService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private BridgeHeaderRepository headerRepository;

	@Autowired
	private BridgeDetailsRepository detailsRepository;

	@Autowired
	private BridgeDiscoveryRepository discoveryRepository;

	private static final Object LOCK = new Object();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void handleTranslationReport(final NormalizedTranslationReportRequestDTO dto) {
		logger.debug("handleTranslationReport started...");
		Assert.notNull(dto, "dto is null");
		Assert.notNull(dto.bridgeId(), "bridgeId is null");
		Assert.notNull(dto.timestamp(), "timestamp is null");

		try {
			final Optional<BridgeHeader> headerOpt = headerRepository.findByUuid(dto.bridgeId().toString());
			if (headerOpt.isEmpty()) {
				throw new InvalidParameterException("Invalid bridge id: " + dto.bridgeId().toString());
			}

			final BridgeHeader header = headerOpt.get();
			final Optional<BridgeDetails> detailsOpt = detailsRepository.findByHeader(header);
			if (detailsOpt.isEmpty()) {
				throw new InvalidParameterException("Invalid bridge id: " + dto.bridgeId().toString());
			}

			if (!detailsOpt.get().getInterfaceTranslator().equals(dto.requester())) {
				// only the related interface translator has the right to report anything about the bridge
				throw new ForbiddenException("Requester has no permission to report about the specified translation bridge");
			}

			final TranslationBridgeStatus toStatus = TranslationBridgeEventState.transformToBridgeStatus(dto.state());
			if (!header.getStatus().isActiveStatus() || !TranslationBridgeStatus.isValidTransition(header.getStatus(), toStatus)) {
				// bridge should not reporting or the reported transition is invalid
				throw new InvalidParameterException("Invalid reporting case");
			}

			synchronized (LOCK) {
				header.setStatus(toStatus);
				header.setAlivesAt(dto.timestamp());
				if (TranslationBridgeStatus.USED == toStatus) {
					header.setUsageReportCount(header.getUsageReportCount() + 1);
				}
				if (!Utilities.isEmpty(dto.errorMessage())) {
					header.setMessage(dto.errorMessage());
				}

				headerRepository.saveAndFlush(header);
			}
		} catch (final InvalidParameterException | ForbiddenException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<BridgeHeader, List<BridgeDiscovery>> storeBridgeDiscoveries(final UUID bridgeId, final String createdBy, final List<TranslationDiscoveryModel> models) {
		logger.debug("storeBridgeDiscoveries started...");
		Assert.notNull(bridgeId, "bridgeId is null");
		Assert.isTrue(!Utilities.isEmpty(createdBy), "createdBy is missing");
		Assert.isTrue(!Utilities.isEmpty(models), "models list is missing");
		Assert.isTrue(!Utilities.containsNull(models), "models list contains null element");

		try {
			BridgeHeader header = new BridgeHeader(bridgeId, createdBy);
			List<BridgeDiscovery> discoveries = new ArrayList<>(models.size());
			for (final TranslationDiscoveryModel model : models) {
				discoveries.add(new BridgeDiscovery(header, Utilities.toJson(model)));
			}

			synchronized (LOCK) {
				header = headerRepository.saveAndFlush(header);
				discoveries = discoveryRepository.saveAllAndFlush(discoveries);

				header.setStatus(TranslationBridgeStatus.DISCOVERED);
				header = headerRepository.saveAndFlush(header);
			}

			return Pair.of(header, discoveries);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<TranslationDiscoveryModel, BridgeDetails> selectBridgeFromDiscoveries(final UUID bridgeId, final String instanceId) {
		logger.debug("selectBridgeFromDiscoveries started...");
		Assert.notNull(bridgeId, "bridgeId is null");
		Assert.isTrue(!Utilities.isEmpty(instanceId), "instanceId is missing");

		try {
			// finding related header
			final Optional<BridgeHeader> headerOpt = headerRepository.findByUuid(bridgeId.toString());
			if (headerOpt.isEmpty()) {
				throw new InvalidParameterException("Invalid bridge identifier: " + bridgeId.toString());
			}

			BridgeHeader header = headerOpt.get();
			if (!TranslationBridgeStatus.isValidTransition(header.getStatus(), TranslationBridgeStatus.PENDING)) {
				throw new InvalidParameterException("Invalid bridge identifier: " + bridgeId.toString());
			}

			// finding related discovery model
			final List<BridgeDiscovery> discoveries = discoveryRepository.findByHeader(header);
			final Optional<TranslationDiscoveryModel> modelOpt = findDiscoveryModelByInstanceId(discoveries, instanceId);
			if (modelOpt.isEmpty()) {
				throw new InvalidParameterException("Invalid bridge identifier: " + bridgeId.toString());
			}

			synchronized (LOCK) {
				// setting status
				header.setStatus(TranslationBridgeStatus.PENDING);
				header = headerRepository.saveAndFlush(header);

				// removing discovery records
				discoveryRepository.deleteByHeader(header);
				discoveryRepository.flush();

				// creating details record
				final TranslationDiscoveryModel model = modelOpt.get();
				BridgeDetails details = new BridgeDetails(
						header,
						model.getConsumer(),
						model.getProvider(),
						model.getServiceDefinition(),
						model.getOperation(),
						model.getInterfaceTranslator(),
						"", // will be set later
						model.getInputDataModelTranslator(),
						null, // will be set later if necessary
						model.getOutputDataModelTranslator(),
						null); // will be set later if necessary
				details = detailsRepository.saveAndFlush(details);

				return Pair.of(model, details);
			}
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
	public boolean updateDetailsRecord(final BridgeDetails record) {
		logger.debug("updateDetailsRecord started...");
		Assert.notNull(record, "record is missing");

		try {
			final Optional<BridgeDetails> currentDetailsOpt = detailsRepository.findById(record.getId());
			if (currentDetailsOpt.isPresent()) {
				if (currentDetailsOpt.get().getHeader().getStatus().isEndStatus()) {
					return true;
				}

				synchronized (LOCK) {
					detailsRepository.saveAndFlush(record);
				}
			}

			return false;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean bridgeInitialized(final BridgeHeader header) {
		logger.debug("bridgeInitialized started...");
		Assert.notNull(header, "header is missing");

		try {
			if (TranslationBridgeStatus.PENDING == header.getStatus()) {
				synchronized (LOCK) {
					header.setStatus(TranslationBridgeStatus.INITIALIZED);
					headerRepository.saveAndFlush(header);
				}
			} else if (header.getStatus().isEndStatus()) {
				return true;
			}

			return false;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void storeBridgeProblem(final UUID bridgeId, final String errorMessage) {
		logger.debug("storeBridgeProblem started...");
		Assert.notNull(bridgeId, "bridgeId is null");
		Assert.isTrue(!Utilities.isEmpty(errorMessage), "errorMessage is missing");

		try {
			final Optional<BridgeHeader> headerOpt = headerRepository.findByUuid(bridgeId.toString());
			if (headerOpt.isPresent() && TranslationBridgeStatus.isValidTransition(headerOpt.get().getStatus(), TranslationBridgeStatus.ERROR)) {
				final BridgeHeader header = headerOpt.get();
				synchronized (LOCK) {
					header.setStatus(TranslationBridgeStatus.ERROR);
					header.setMessage(errorMessage);
					headerRepository.saveAndFlush(header);
				}
			}
		} catch (final Exception ex) {
			// can't store a bridge problem in the db, so we store it at least in the log
			logger.error("Bridge {} should be in ERROR state with the following error message: {}", bridgeId.toString(), errorMessage);

			logger.error(ex.getMessage());
			logger.debug(ex);

			// no point to throw an exception since this method is about handling an original exception
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public AbortResult abortBridge(final UUID bridgeId, final String createdByRequirement) {
		logger.debug("abortBridge started...");
		Assert.notNull(bridgeId, "bridgeId is null");

		try {
			final Optional<BridgeHeader> headerOpt = headerRepository.findByUuid(bridgeId.toString());
			if (headerOpt.isEmpty()) {
				return new AbortResult(false, null, null);
			}

			BridgeHeader header = headerOpt.get();
			if (!Utilities.isEmpty(createdByRequirement)
					&& !createdByRequirement.equals(header.getCreatedBy())) {
				// no permission to abort this bridge
				throw new ForbiddenException("No permission to abort bridge: " + bridgeId.toString());
			}

			final TranslationBridgeStatus oldStatus = header.getStatus();
			if (TranslationBridgeStatus.isValidTransition(oldStatus, TranslationBridgeStatus.ABORTED)) {
				// delete related discovery records (if any)
				discoveryRepository.deleteByHeader(header);

				synchronized (LOCK) {
					header.setStatus(TranslationBridgeStatus.ABORTED);
					header = headerRepository.saveAndFlush(header);

					final Optional<BridgeDetails> detailsOpt = detailsRepository.findByHeader(header);

					return new AbortResult(true, oldStatus, detailsOpt.isEmpty() ? null : detailsOpt.get());
				}
			}

			// already aborted, closed or in error state
			return new AbortResult(false, oldStatus, null);
		} catch (final ForbiddenException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<BridgeDetails> getBridgeDetailsPage(final NormalizedTranslationQueryRequestDTO dto) {
		logger.debug("getBridgeDetailsPage started...");
		Assert.notNull(dto, "dto is missing");

		try {
			if (!dto.hasAnyFilter()) {
				return detailsRepository.findAll(dto.pageRequest());
			}

			return getBridgeDetailsPageByFilters(dto);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}

	}

	//-------------------------------------------------------------------------------------------------
	public List<BridgeDiscovery> getBridgeDiscoveriesCreatedBefore(final ZonedDateTime threshold) {
		logger.debug("getBridgeDiscoveriesCreatedBefore started...");
		Assert.notNull(threshold, "threshold is missing");

		try {
			return discoveryRepository.findByHeader_CreatedAtLessThan(threshold);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void handleObsoletedBridgeDiscovery(final BridgeDiscovery record) {
		logger.debug("handleObsoletedBridgeDiscovery started...");
		Assert.notNull(record, "record is missing");

		try {
			synchronized (LOCK) {
				final BridgeHeader header = record.getHeader();
				final TranslationBridgeStatus status = header.getStatus();
				if (!status.isEndStatus() && !status.isActiveStatus()) {
					header.setStatus(TranslationBridgeStatus.CLOSED);
					header.setMessage("Closed by TranslationManager because of inactivity");
					headerRepository.saveAndFlush(header);
				}
			}

			discoveryRepository.delete(record);
			discoveryRepository.flush();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Optional<TranslationDiscoveryModel> findDiscoveryModelByInstanceId(final List<BridgeDiscovery> discoveries, final String instanceId) {
		logger.debug("findDiscoveryModelByInstanceId started...");

		for (final BridgeDiscovery record : discoveries) {
			final TranslationDiscoveryModel model = Utilities.fromJson(record.getData(), TranslationDiscoveryModel.class);
			if (model.getTargetInstanceId().equals(instanceId)) {
				return Optional.of(model);
			}
		}

		return Optional.empty();
	}

	//-------------------------------------------------------------------------------------------------
	private Page<BridgeDetails> getBridgeDetailsPageByFilters(final NormalizedTranslationQueryRequestDTO dto) {
		logger.debug("getBridgeDetailsByFilters started...");

		BaseFilter baseFilter = BaseFilter.NONE;
		synchronized (LOCK) {
			List<BridgeDetails> toFilter = null;
			if (!Utilities.isEmpty(dto.bridgeIds())) {
				toFilter = detailsRepository.findByHeader_UuidIn(dto.bridgeIds()
						.stream()
						.map(UUID::toString)
						.toList());
				baseFilter = BaseFilter.BRIDGE_ID;
			} else if (!Utilities.isEmpty(dto.consumers())) {
				toFilter = detailsRepository.findByConsumerIn(dto.consumers());
				baseFilter = BaseFilter.CONSUMER;
			} else if (!Utilities.isEmpty(dto.providers())) {
				toFilter = detailsRepository.findByProviderIn(dto.providers());
				baseFilter = BaseFilter.PROVIDER;
			} else if (!Utilities.isEmpty(dto.serviceDefinitions())) {
				toFilter = detailsRepository.findByServiceDefinitionIn(dto.serviceDefinitions());
				baseFilter = BaseFilter.SERVICE_DEF;
			} else if (!Utilities.isEmpty(dto.interfaceTranslators())) {
				toFilter = detailsRepository.findByInterfaceTranslatorIn(dto.interfaceTranslators());
				baseFilter = BaseFilter.INTERFACE_TRANSLATOR;
			} else if (!Utilities.isEmpty(dto.statuses())) {
				toFilter = detailsRepository.findByHeader_StatusIn(dto.statuses());
				baseFilter = BaseFilter.STATUS;
			} else if (!Utilities.isEmpty(dto.creators())) {
				toFilter = detailsRepository.findByHeader_CreatedByIn(dto.creators());
				baseFilter = BaseFilter.CREATED_BY;
			} else {
				toFilter = detailsRepository.findAll();
			}

			final Set<Long> matchingIds = new HashSet<>();
			for (final BridgeDetails record : toFilter) {
				// Match against bridge ids is not needed since if bridge ids are defined then the toFilter list only contains matching records

				// Match against consumers
				if (baseFilter != BaseFilter.CONSUMER && !Utilities.isEmpty(dto.consumers()) && !dto.consumers().contains(record.getConsumer())) {
					continue;
				}

				// Match against providers
				if (baseFilter != BaseFilter.PROVIDER && !Utilities.isEmpty(dto.providers()) && !dto.providers().contains(record.getProvider())) {
					continue;
				}

				// Match against service definitions
				if (baseFilter != BaseFilter.SERVICE_DEF && !Utilities.isEmpty(dto.serviceDefinitions()) && !dto.serviceDefinitions().contains(record.getServiceDefinition())) {
					continue;
				}

				// Match against interface translators
				if (baseFilter != BaseFilter.INTERFACE_TRANSLATOR && !Utilities.isEmpty(dto.interfaceTranslators()) && !dto.interfaceTranslators().contains(record.getInterfaceTranslator())) {
					continue;
				}

				// Match against statuses
				if (baseFilter != BaseFilter.STATUS && !Utilities.isEmpty(dto.statuses()) && !dto.statuses().contains(record.getHeader().getStatus())) {
					continue;
				}

				// Match against creators
				if (baseFilter != BaseFilter.CREATED_BY && !Utilities.isEmpty(dto.creators()) && !dto.creators().contains(record.getHeader().getCreatedBy())) {
					continue;
				}

				// Match against data model translators
				if (!Utilities.isEmpty(dto.dataModelTranslators())
						&& !dto.dataModelTranslators().contains(record.getInputDmTranslator())
						&& !dto.dataModelTranslators().contains(record.getResultDmTranslator())) {
					continue;
				}

				// Match against creation from
				if (dto.creationFrom() != null && record.getHeader().getCreatedAt().isBefore(dto.creationFrom())) {
					continue;
				}

				// Match against creation to
				if (dto.creationTo() != null && record.getHeader().getCreatedAt().isAfter(dto.creationTo())) {
					continue;
				}

				// Match against alives from
				if (dto.alivesFrom() != null
						&& (record.getHeader().getAlivesAt() == null || record.getHeader().getAlivesAt().isBefore(dto.alivesFrom()))) {
					continue;
				}

				// Match against alives to
				if (dto.alivesTo() != null
						&& (record.getHeader().getAlivesAt() == null || record.getHeader().getAlivesAt().isAfter(dto.alivesTo()))) {
					continue;
				}

				// Match against min usage
				if (dto.minUsage() != null && record.getHeader().getUsageReportCount() < dto.minUsage().intValue()) {
					continue;
				}

				// Match against max usage
				if (dto.maxUsage() != null && record.getHeader().getUsageReportCount() > dto.maxUsage().intValue()) {
					continue;
				}

				matchingIds.add(record.getId());
			}

			return detailsRepository.findAllByIdIn(matchingIds, dto.pageRequest());
		}
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	public record AbortResult(
			boolean abortHappened,
			TranslationBridgeStatus fromStatus,
			BridgeDetails detailsRecord) {
	}

	//-------------------------------------------------------------------------------------------------
	private enum BaseFilter {
		NONE, BRIDGE_ID, CONSUMER, PROVIDER, SERVICE_DEF, INTERFACE_TRANSLATOR, STATUS, CREATED_BY
	}
}