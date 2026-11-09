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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class OrchestrationFromContextValidation {

	//=================================================================================================
	// members

	@Autowired
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//-------------------------------------------------------------------------------------------------
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validate(final OrchestrationForm form, final String origin) {
		logger.debug("validate started...");

		if (form.getFlag(OrchestrationFlag.ONLY_INTERCLOUD)) {
			if (!sysInfo.isIntercloudEnabled()) {
				throw new InvalidParameterException("ONLY_INTERCLOUD flag is present, but intercloud orchestration is not enabled", origin);
			}

			if (form.getFlag(OrchestrationFlag.ALLOW_TRANSLATION)) {
				// Inter-cloud translation is not supported
				throw new InvalidParameterException("ONLY_INTERCLOUD and ALLOW_TRANSLATION flags cannot be present at the same time", origin);
			}

			if (Utilities.isEmpty(form.getOperations()) || form.getOperations().size() != 1) {
				// The creation of an inter-cloud bridge is limited to exactly one operation that the requester wants to use.
				throw new InvalidParameterException("Exactly one operation must be defined when only inter-cloud orchestration is required", origin);
			}
		}

		if (form.getFlag(OrchestrationFlag.ALLOW_INTERCLOUD) && (Utilities.isEmpty(form.getOperations()) || form.getOperations().size() != 1)) {
			// The creation of an inter-cloud bridge is limited to exactly one operation that the requester wants to use.
			throw new InvalidParameterException("Exactly one operation must be defined when only inter-cloud orchestration is allowed", origin);
		}

		if (form.getFlag(OrchestrationFlag.ALLOW_TRANSLATION) && (Utilities.isEmpty(form.getOperations()) || form.getOperations().size() != 1)) {
			// The creation of a translation bridge is limited to exactly one operation that the requester wants to use.
			throw new InvalidParameterException("Exactly one operation must be defined when translation is allowed", origin);
		}

		if (form.getFlag(OrchestrationFlag.ALLOW_TRANSLATION) && Utilities.isEmpty(form.getInterfaceTemplateNames())) {
			// The creation of a translation bridge requires interface template name(s) being defined.
			throw new InvalidParameterException("Interface template name(s) must be defined when translation is allowed", origin);
		}

		if (form.getFlag(OrchestrationFlag.ONLY_PREFERRED) && !form.hasPreferredProviders()) {
			throw new InvalidParameterException("ONLY_PREFERRED flag is present, but no preferred provider is defined", origin);
		}
	}
}