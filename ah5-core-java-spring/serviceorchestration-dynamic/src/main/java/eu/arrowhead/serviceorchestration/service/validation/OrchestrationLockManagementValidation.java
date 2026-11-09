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
package eu.arrowhead.serviceorchestration.service.validation;

import java.time.DateTimeException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierValidator;
import eu.arrowhead.dto.OrchestrationLockListRequestDTO;
import eu.arrowhead.dto.OrchestrationLockQueryRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.service.normalization.OrchestrationLockManagementNormalization;

@Service
public class OrchestrationLockManagementValidation {

	//=================================================================================================
	// members

	@Autowired
	private OrchestrationLockManagementNormalization normalization;

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceInstanceIdentifierValidator serviceInstanceIdValidator;

	@Autowired
	private PageValidator pageValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION AND NORMALIZATION

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockListRequestDTO validateAndNormalizeCreateService(final OrchestrationLockListRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeCreateService started...");

		validateCreateService(dto, origin);
		final OrchestrationLockListRequestDTO normalized = normalization.normalizeOrchestrationLockListRequestDTO(dto);

		try {
			normalized.locks().forEach(lock -> {
				serviceInstanceIdValidator.validateServiceInstanceIdentifier(lock.serviceInstanceId());
				systemNameValidator.validateSystemName(lock.owner());
			});
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationLockQueryRequestDTO validateAndNormalizeQueryService(final OrchestrationLockQueryRequestDTO dto, final String origin) {
		logger.debug("validateAndNormalizeQueryService started...");

		validateQueryService(dto, origin);
		final OrchestrationLockQueryRequestDTO normalized = normalization.normalizeOrchestrationLockQueryRequestDTO(dto);

		try {
			if (!Utilities.isEmpty(normalized.orchestrationJobIds())) {
				normalized.orchestrationJobIds().forEach(jobId -> {
					if (!Utilities.isUUID(jobId)) {
						throw new InvalidParameterException("Invalid orchestration job id: " + jobId);
					}
				});
			}

			if (!Utilities.isEmpty(normalized.serviceInstanceIds())) {
				normalized.serviceInstanceIds().forEach(id -> serviceInstanceIdValidator.validateServiceInstanceIdentifier(id));
			}

			if (!Utilities.isEmpty(normalized.owners())) {
				normalized.owners().forEach(owner -> systemNameValidator.validateSystemName(owner));
			}

			if (!Utilities.isEmpty(normalized.expiresBefore())) {
				try {
					Utilities.parseUTCStringToZonedDateTime(normalized.expiresBefore());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Invalid expires before: " + normalized.expiresBefore());
				}
			}

			if (!Utilities.isEmpty(normalized.expiresAfter())) {
				try {
					Utilities.parseUTCStringToZonedDateTime(normalized.expiresAfter());
				} catch (final DateTimeException ex) {
					throw new InvalidParameterException("Invalid expires after: " + normalized.expiresAfter());
				}
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public Pair<String, List<String>> validateAndNormalizeRemoveService(final String owner, final List<String> serviceInstanceIds, final String origin) {
		logger.debug("validateAndNormalizeRemoveService started...");

		validateRemoveService(serviceInstanceIds, owner, origin);
		final Pair<String, List<String>> normalized = Pair.of(normalization.normalizeSystemName(owner), normalization.normalizeServiceInstanceIds(serviceInstanceIds));

		try {
			systemNameValidator.validateSystemName(normalized.getLeft());
			normalized.getRight().forEach(id -> serviceInstanceIdValidator.validateServiceInstanceIdentifier(id));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}

		return normalized;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// VALIDATION

	//-------------------------------------------------------------------------------------------------
	private void validateCreateService(final OrchestrationLockListRequestDTO dto, final String origin) {
		logger.debug("validateCreateService started...");

		if (dto == null) {
			throw new InvalidParameterException("Request payload is null", origin);
		}

		if (Utilities.containsNull(dto.locks())) {
			throw new InvalidParameterException("Request payload contains null element", origin);
		}

		final ZonedDateTime now = Utilities.utcNow();
		dto.locks().forEach(lock -> {
			if (Utilities.isEmpty(lock.serviceInstanceId())) {
				throw new InvalidParameterException("Service instance id is missing", origin);
			}

			if (Utilities.isEmpty(lock.owner())) {
				throw new InvalidParameterException("Owner is missing", origin);
			}

			if (!Utilities.isEmpty(lock.expiresAt())) {
				try {
					final ZonedDateTime expiresAt = Utilities.parseUTCStringToZonedDateTime(lock.expiresAt().trim());
					if (expiresAt.isBefore(now)) {
						throw new InvalidParameterException("Expires at is in the past: " + lock.expiresAt(), origin);
					}
				} catch (final DateTimeParseException ex) {
					throw new InvalidParameterException("Invalid expires at format: " + lock.expiresAt(), origin);
				}
			} else {
				throw new InvalidParameterException("Expiration time is missing", origin);
			}
		});
	}

	//-------------------------------------------------------------------------------------------------
	private void validateQueryService(final OrchestrationLockQueryRequestDTO dto, final String origin) {
		logger.debug("validateQueryService started...");

		if (dto == null) {
			return;
		}

		pageValidator.validatePageParameter(dto.pagination(), OrchestrationLock.SORTABLE_FIELDS_BY, origin);

		if (!Utilities.isEmpty(dto.ids()) && Utilities.containsNull(dto.ids())) {
			throw new InvalidParameterException("ID list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.orchestrationJobIds()) && Utilities.containsNullOrEmpty(dto.orchestrationJobIds())) {
			throw new InvalidParameterException("Orchestration job id list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.serviceInstanceIds()) && Utilities.containsNullOrEmpty(dto.serviceInstanceIds())) {
			throw new InvalidParameterException("Service instance id list contains empty element", origin);
		}

		if (!Utilities.isEmpty(dto.owners()) && Utilities.containsNullOrEmpty(dto.owners())) {
			throw new InvalidParameterException("Owner list contains empty element", origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void validateRemoveService(final List<String> serviceInstanceIds, final String owner, final String origin) {
		logger.debug("validateRemoveService started...");

		if (Utilities.isEmpty(serviceInstanceIds)) {
			throw new InvalidParameterException("Service instance id list empty", origin);
		}

		if (Utilities.containsNullOrEmpty(serviceInstanceIds)) {
			throw new InvalidParameterException("Service instance id list contains empty element", origin);
		}

		if (Utilities.isEmpty(owner)) {
			throw new InvalidParameterException("Owner is missing", origin);
		}
	}
}
