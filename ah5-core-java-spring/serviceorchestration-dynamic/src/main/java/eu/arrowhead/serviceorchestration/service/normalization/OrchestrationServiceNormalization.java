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
package eu.arrowhead.serviceorchestration.service.normalization;

import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

@Service
public class OrchestrationServiceNormalization {

	//=================================================================================================
	// members

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Pair<String, UUID> normalizePushUnsubscribe(final String requesterSystem, final String subscriptionId) {
		logger.debug("normalizePushUnsubscribe started...");
		Assert.isTrue(!Utilities.isEmpty(requesterSystem), "requesterSystem is empty");
		Assert.isTrue(!Utilities.isEmpty(subscriptionId), "subscriptionId is empty");

		return Pair.of(systemNameNormalizer.normalize(requesterSystem), UUID.fromString(subscriptionId.trim()));
	}
}