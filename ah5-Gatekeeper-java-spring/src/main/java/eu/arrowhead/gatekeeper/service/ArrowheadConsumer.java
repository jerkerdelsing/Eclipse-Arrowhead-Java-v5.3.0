/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.ArrowheadHttpService;

// The services Gatekeeper consumes, through the common library: the ServiceRegistry lookup (a bootstrap service) and
// everything else by orchestration on behalf of Gatekeeper. A provider that failed (not a refusal) is forgotten, so the
// next call orchestrates again, as the native implementation did.
@Service
public class ArrowheadConsumer {

	//=================================================================================================
	// members

	@Autowired
	private ArrowheadHttpService httpService;

	@Autowired
	private ServiceCollector collector;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> lookup(final Map<String, Object> payload) {
		return consume(Constants.SERVICE_DEF_SERVICE_DISCOVERY, Constants.SERVICE_OP_LOOKUP, Constants.SYS_NAME_SERVICE_REGISTRY, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public Map<String, Object> consume(final String definition, final String operation, final String provider, final Object payload) {
		try {
			final Map<String, Object> result = httpService.consumeService(definition, operation, provider, Map.class, payload);
			return result == null ? new java.util.LinkedHashMap<>() : result;
		} catch (final RuntimeException ex) {
			if (!refusal(ex)) {
				collector.forget(definition);
			}

			throw ex;
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static boolean refusal(final RuntimeException ex) {
		return ex instanceof ArrowheadException
				&& (ex instanceof AuthException || ex instanceof ForbiddenException || ex instanceof InvalidParameterException || ex instanceof DataNotFoundException);
	}
}
