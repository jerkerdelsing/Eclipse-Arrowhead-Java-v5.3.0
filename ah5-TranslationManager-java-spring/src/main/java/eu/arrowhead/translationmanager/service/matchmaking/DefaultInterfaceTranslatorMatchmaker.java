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
package eu.arrowhead.translationmanager.service.matchmaking;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;

public class DefaultInterfaceTranslatorMatchmaker implements InterfaceTranslatorMatchmaker {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	private SecureRandom rnd = new SecureRandom();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public ServiceInstanceResponseDTO doMatchmaking(final List<ServiceInstanceResponseDTO> candidates, final Map<String, Object> context) {
		logger.debug("DefaultInterfaceTranslatorMatchmaker.doMatchmaking started...");

		if (Utilities.isEmpty(candidates)) {
			return null;
		}

		final int selectedIdx = rnd.nextInt(candidates.size());
		return candidates.get(selectedIdx);
	}
}