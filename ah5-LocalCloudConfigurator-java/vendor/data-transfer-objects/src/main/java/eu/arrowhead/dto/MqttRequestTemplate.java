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
package eu.arrowhead.dto;

import java.util.Map;

// TODO secretKey input field in order to encrypt the response (TLS only!)
public record MqttRequestTemplate(
		String traceId,
		String authentication,
		String responseTopic,
		Integer qosRequirement,
		Map<String, String> params,
		Object payload) {
}