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
package eu.arrowhead.serviceorchestration.service.model.validation;

import java.time.DateTimeException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameValidator;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.dto.enums.QoSOperation;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class OrchestrationFormValidation {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameValidator systemNameValidator;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefNameValidator;

	@Autowired
	private ServiceOperationNameValidator serviceOpNameValidator;

	@Autowired
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Autowired
	private OrchestrationFormNormalization normalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateOrchestrationForm(final OrchestrationForm form, final String origin) {
		if (form == null) {
			throw new InvalidParameterException("Request payload is missing", origin);
		}

		if (Utilities.isEmpty(form.getRequesterSystemName())) {
			throw new InvalidParameterException("Requester system name is empty", origin);
		}

		if (Utilities.isEmpty(form.getTargetSystemName())) {
			throw new InvalidParameterException("Target system name is empty", origin);
		}

		if (Utilities.isEmpty(form.getServiceDefinition())) {
			throw new InvalidParameterException("Service definition is empty", origin);
		}

		if (!Utilities.isEmpty(form.getOperations()) && Utilities.containsNullOrEmpty(form.getOperations())) {
			throw new InvalidParameterException("Operation list contains empty element", origin);
		}

		if (!Utilities.isEmpty(form.getVersions()) && Utilities.containsNullOrEmpty(form.getVersions())) {
			throw new InvalidParameterException("Version list contains empty element", origin);
		}

		if (!Utilities.isEmpty(form.getOrchestrationFlags())) {
			form.getOrchestrationFlags().forEach((k, v) -> {
				if (v == null) {
					throw new InvalidParameterException("Orchestration flag map contains null value", origin);
				}
			});
		}

		if (form.getExclusivityDuration() != null && form.getExclusivityDuration() <= 0) {
			throw new InvalidParameterException("Exclusivity duration must be greater than 0", origin);
		}

		if (!Utilities.isEmpty(form.getAlivesAt())) {
			try {
				Utilities.parseUTCStringToZonedDateTime(form.getAlivesAt());
			} catch (final DateTimeException ex) {
				throw new InvalidParameterException("Alives at time has an invalid time format", origin);
			}
		}

		if (!Utilities.isEmpty(form.getMetadataRequirements()) && Utilities.containsNull(form.getMetadataRequirements())) {
			throw new InvalidParameterException("Metadata requirement list contains null element", origin);
		}

		if (!Utilities.isEmpty(form.getInterfaceTemplateNames()) && Utilities.containsNullOrEmpty(form.getInterfaceTemplateNames())) {
			throw new InvalidParameterException("Interface template name list contains empty element", origin);
		}

		if (!Utilities.isEmpty(form.getInterfaceAddressTypes()) && Utilities.containsNullOrEmpty(form.getInterfaceAddressTypes())) {
			throw new InvalidParameterException("Interface address type list contains empty element", origin);
		}

		if (!Utilities.isEmpty(form.getInterfacePropertyRequirements()) && Utilities.containsNull(form.getInterfacePropertyRequirements())) {
			throw new InvalidParameterException("Interface property requirement list contains null element", origin);
		}

		if (!Utilities.isEmpty(form.getSecurityPolicies()) && Utilities.containsNullOrEmpty(form.getSecurityPolicies())) {
			throw new InvalidParameterException("Security policy list contains empty element", origin);

		}

		if (!Utilities.isEmpty(form.getPreferredProviders()) && Utilities.containsNullOrEmpty(form.getPreferredProviders())) {
			throw new InvalidParameterException("Preferred provider list contains empty element", origin);
		}

		if (!Utilities.isEmpty(form.getQualityRequirements())) {
			form.getQualityRequirements().forEach(qosPref -> {
				if (Utilities.isEmpty(qosPref.type())) {
					throw new InvalidParameterException("QoS type is empty", origin);
				}
				if (Utilities.isEmpty(qosPref.operation())) {
					throw new InvalidParameterException("QoS operation is empty", origin);
				}
				if (!Utilities.isEnumValue(qosPref.operation().trim().toUpperCase(), QoSOperation.class)) {
					throw new InvalidParameterException("Invalid QoS operation", origin);
				}
			});
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void validateAndNormalizeOrchestrationForm(final OrchestrationForm form, final boolean skipPrevalidation, final String origin) {
		logger.debug("validateAndNormalizeOrchestrationForm...");

		if (!skipPrevalidation) {
			validateOrchestrationForm(form, origin);
		}
		normalizer.normalizeOrchestrationForm(form);

		try {
			systemNameValidator.validateSystemName(form.getRequesterSystemName());
			systemNameValidator.validateSystemName(form.getTargetSystemName());
			serviceDefNameValidator.validateServiceDefinitionName(form.getServiceDefinition());

			if (!Utilities.isEmpty(form.getOrchestrationFlags())) {
				form.getOrchestrationFlags().keySet().forEach(f -> {
					if (!Utilities.isEnumValue(f, OrchestrationFlag.class)) {
						throw new InvalidParameterException("Invalid orchestration flag: " + f);
					}
				});
			}

			if (!Utilities.isEmpty(form.getOperations())) {
				form.getOperations().forEach(op -> serviceOpNameValidator.validateServiceOperationName(op));
			}

			if (!Utilities.isEmpty(form.getInterfaceTemplateNames())) {
				form.getInterfaceTemplateNames().forEach(itn -> interfaceTemplateNameValidator.validateInterfaceTemplateName(itn));
			}

			if (!Utilities.isEmpty(form.getInterfaceAddressTypes())) {
				form.getInterfaceAddressTypes().forEach(iat -> {
					if (!Utilities.isEnumValue(iat, AddressType.class)) {
						throw new InvalidParameterException("Invalid interface address type: " + iat);
					}
				});
			}

			if (!Utilities.isEmpty(form.getSecurityPolicies())) {
				form.getSecurityPolicies().forEach(sp -> {
					if (!Utilities.isEnumValue(sp, ServiceInterfacePolicy.class)
							|| !ServiceInterfacePolicy.isOfferable(ServiceInterfacePolicy.valueOf(sp))) {
						throw new InvalidParameterException("Invalid security policy: " + sp);
					}
				});
			}

			if (!Utilities.isEmpty(form.getPreferredProviders())) {
				form.getPreferredProviders().forEach(pp -> systemNameValidator.validateSystemName(pp));
			}
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}
}