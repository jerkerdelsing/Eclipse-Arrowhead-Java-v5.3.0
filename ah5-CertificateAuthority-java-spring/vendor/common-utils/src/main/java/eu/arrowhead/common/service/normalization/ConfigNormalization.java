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
package eu.arrowhead.common.service.normalization;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;

@Service
public class ConfigNormalization {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<String> normalizeConfigKeyList(final List<String> keys) {
		logger.debug("normalizeConfigKeyList started");

		if (Utilities.isEmpty(keys)) {
			return List.of();
		}

		return keys
				.stream()
				.map(k -> k.trim())
				.distinct()
				.collect(Collectors.toList());
	}
}