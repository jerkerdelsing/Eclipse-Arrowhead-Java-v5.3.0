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
package eu.arrowhead.common.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.ConfigValidation;
import eu.arrowhead.dto.KeyValuesDTO;

@Service
public class ConfigService {

	//=================================================================================================
	// members

	@Autowired
	private ConfigValidation validator;

	@Autowired
	private Environment environment;

	@Autowired
	private SystemInfo sysInfo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public KeyValuesDTO getConfig(final List<String> keys, final String origin) {
		logger.debug("getConfig started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		try {
			final List<String> normalized = validator.validateAndNormalizeConfigKeyList(keys);
			final Map<String, String> configDefaultsMap = sysInfo.getConfigDefaultsMap();

			final Map<String, String> result = new HashMap<>();
			for (final String key : normalized) {
				if (configDefaultsMap.containsKey(key)) {
					result.put(key, configDefaultsMap.get(key)); // default value
					if (!Utilities.isEmpty(environment.getProperty(key))) {
						result.put(key, environment.getProperty(key)); // override default
					}
				}

				// not public config => ignore
			}

			return convertConfigMapToDTO(result);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private KeyValuesDTO convertConfigMapToDTO(final Map<String, String> map) {
		return new KeyValuesDTO(map);

	}
}