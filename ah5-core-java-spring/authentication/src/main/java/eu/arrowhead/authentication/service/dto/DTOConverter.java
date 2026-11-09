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
package eu.arrowhead.authentication.service.dto;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.IdentityListMgmtResponseDTO;
import eu.arrowhead.dto.IdentityMgmtResponseDTO;
import eu.arrowhead.dto.IdentitySessionListMgmtResponseDTO;
import eu.arrowhead.dto.IdentitySessionResponseDTO;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IdentityListMgmtResponseDTO convertIdentifiableSystemListToDTO(final List<System> systems) {
		logger.debug("convertIdentifiableSystemListToDTO started...");
		Assert.notNull(systems, "systems is null");

		return new IdentityListMgmtResponseDTO(systems.stream().map(s -> convertIdentifiableSystemToDTO(s)).toList(), systems.size());
	}

	//-------------------------------------------------------------------------------------------------
	public IdentityListMgmtResponseDTO convertIdentifiableSystemListToDTO(final Page<System> systems) {
		logger.debug("convertIdentifiableSystemListToDTO started...");
		Assert.notNull(systems, "systems is null");

		return new IdentityListMgmtResponseDTO(systems.stream().map(s -> convertIdentifiableSystemToDTO(s)).toList(), systems.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public IdentityMgmtResponseDTO convertIdentifiableSystemToDTO(final System system) {
		logger.debug("convertIdentifiableSystemToDTO started...");
		Assert.notNull(system, "system is null");

		return new IdentityMgmtResponseDTO(
				system.getName(),
				system.getAuthenticationMethod().name(),
				system.isSysop(),
				system.getCreatedBy(),
				Utilities.convertZonedDateTimeToUTCString(system.getCreatedAt()),
				system.getUpdatedBy(),
				Utilities.convertZonedDateTimeToUTCString(system.getUpdatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public IdentitySessionListMgmtResponseDTO convertSessionListToDTO(final Page<ActiveSession> sessions) {
		logger.debug("convertSessionListToDTO started...");
		Assert.notNull(sessions, "sessions is null");

		return new IdentitySessionListMgmtResponseDTO(sessions.stream().map(s -> convertSessionToDTO(s)).toList(), sessions.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public IdentitySessionResponseDTO convertSessionToDTO(final ActiveSession session) {
		logger.debug("convertSessionToDTO started...");
		Assert.notNull(session, "session is null");

		return new IdentitySessionResponseDTO(
				session.getSystem().getName(),
				Utilities.convertZonedDateTimeToUTCString(session.getLoginTime()),
				Utilities.convertZonedDateTimeToUTCString(session.getExpirationTime()));
	}
}