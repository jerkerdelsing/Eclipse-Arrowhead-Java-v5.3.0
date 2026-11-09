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

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.dto.QoSRequirementDTO;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@Service
public class OrchestrationFormNormalization {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Autowired
	private ServiceOperationNameNormalizer serviceOpNameNormalizer;

	@Autowired
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Autowired
	private VersionNormalizer versionNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void normalizeOrchestrationForm(final OrchestrationForm form) {
		logger.debug("normalizeOrchestrationForm started...");
		Assert.notNull(form, "form is null");

		form.setRequesterSystemName(systemNameNormalizer.normalize(form.getRequesterSystemName()));
		form.setTargetSystemName(systemNameNormalizer.normalize(form.getTargetSystemName()));
		form.setServiceDefinition(serviceDefNameNormalizer.normalize(form.getServiceDefinition()));

		if (!Utilities.isEmpty(form.getOrchestrationFlags())) {
			final Map<String, Boolean> normalizedFlags = new HashMap<>();
			form.getOrchestrationFlags().forEach((k, v) -> normalizedFlags.put(k.trim().toUpperCase(), v));
			form.setOrchestrationFlags(normalizedFlags);
		}

		if (!Utilities.isEmpty(form.getOperations())) {
			form.setOperations(form.getOperations().stream().map(op -> serviceOpNameNormalizer.normalize(op)).toList());
		}

		if (!Utilities.isEmpty(form.getVersions())) {
			form.setVersions(form.getVersions().stream().map(v -> versionNormalizer.normalize(v)).toList());
		}

		if (!Utilities.isEmpty(form.getInterfaceTemplateNames())) {
			form.setInterfaceTemplateNames(form.getInterfaceTemplateNames().stream().map(itn -> interfaceTemplateNameNormalizer.normalize(itn)).toList());
		}

		if (!Utilities.isEmpty(form.getInterfaceAddressTypes())) {
			form.setInterfaceAddressTypes(form.getInterfaceAddressTypes().stream().map(iat -> iat.trim().toUpperCase()).toList());
		}

		if (!Utilities.isEmpty(form.getSecurityPolicies())) {
			form.setSecurityPolicies(form.getSecurityPolicies().stream().map(sp -> sp.trim().toUpperCase()).toList());
		}

		if (!Utilities.isEmpty(form.getPreferredProviders())) {
			form.setPreferredProviders(form.getPreferredProviders().stream().map(pp -> systemNameNormalizer.normalize(pp)).toList());
		}

		if (!Utilities.isEmpty(form.getQualityRequirements())) {
			form.setQualityRequirements(
					form.getQualityRequirements().stream().map(qosPref -> new QoSRequirementDTO(qosPref.type().trim(), qosPref.operation().trim().toUpperCase(), qosPref.requirements())).toList());
		}
	}
}