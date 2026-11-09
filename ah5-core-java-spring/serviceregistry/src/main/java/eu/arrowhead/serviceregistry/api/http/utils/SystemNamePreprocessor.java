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
package eu.arrowhead.serviceregistry.api.http.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.dto.ServiceInstanceCreateRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.SystemRegisterRequestDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class SystemNamePreprocessor {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SystemRequestDTO process(final SystemRegisterRequestDTO dto, final HttpServletRequest request, final String origin) throws InvalidParameterException {
		logger.debug("process (SystemRegisterRequestDTO to SystemRequestDTO) started");

		if (dto == null) {
			return null;
		}

		final String name = HttpUtilities.acquireName(request, origin);

		return new SystemRequestDTO(name, dto.metadata(), dto.version(), dto.addresses(), dto.deviceName());
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceRequestDTO process(final ServiceInstanceCreateRequestDTO dto, final HttpServletRequest request, final String origin) throws InvalidParameterException {
		logger.debug("process (ServiceInstanceCreateRequestDTO to ServiceInstanceRequestDTO) started");

		if (dto == null) {
			return null;
		}

		final String name = HttpUtilities.acquireName(request, origin);

		return new ServiceInstanceRequestDTO(
				name,
				dto.serviceDefinitionName(),
				dto.version(),
				dto.expiresAt(),
				dto.metadata(),
				dto.interfaces());
	}

	//-------------------------------------------------------------------------------------------------
	public String process(final HttpServletRequest request, final String origin) throws InvalidParameterException {
		logger.debug("process started");

		final String name = HttpUtilities.acquireName(request, origin);

		return name;
	}
}