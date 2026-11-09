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
package eu.arrowhead.serviceregistry.service.validation.interf;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;

@Component
public class InterfaceNormalizer {

	//=================================================================================================
	// members

	@Autowired
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// members

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceInterfaceRequestDTO normalizeInterfaceDTO(final ServiceInstanceInterfaceRequestDTO dto) {
		logger.debug("normalizeInterfaceDTO started...");
		Assert.notNull(dto, "Interface instance dto is null");

		return new ServiceInstanceInterfaceRequestDTO(
				interfaceTemplateNameNormalizer.normalize(dto.templateName()),
				Utilities.isEmpty(dto.protocol()) ? "" : dto.protocol().trim().toLowerCase(),
				dto.policy().trim().toUpperCase(),
				dto.properties());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInterfaceTemplateRequestDTO normalizeTemplateDTO(final ServiceInterfaceTemplateRequestDTO dto) {
		logger.debug("normalizeTemplateDTO started...");
		Assert.notNull(dto, "Interface template dto is null");
		Assert.notNull(dto.protocol(), "protocol is null");
		Assert.isTrue(dto.propertyRequirements() == null || !Utilities.containsNull(dto.propertyRequirements().stream().map(prop -> prop.name()).toList()), "propertyRequirements contains null name element");

		return new ServiceInterfaceTemplateRequestDTO(
				interfaceTemplateNameNormalizer.normalize(dto.name()),
				dto.protocol().trim().toLowerCase(),
				Utilities.isEmpty(dto.propertyRequirements()) ? new ArrayList<>()
						: dto.propertyRequirements()
								.stream()
								.map(prop -> new ServiceInterfaceTemplatePropertyDTO(
										prop.name().trim(),
										prop.mandatory(),
										Utilities.isEmpty(prop.validator()) ? "" : prop.validator().trim().toUpperCase(),
										Utilities.isEmpty(prop.validatorParams())
												? new ArrayList<>()
												: prop.validatorParams()
														.stream()
														.map(param -> param.trim())
														.toList()))
								.toList());
	}
}