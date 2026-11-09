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
package eu.arrowhead.authorization.service.utils;

import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

public final class InstanceIdUtils {

	//=================================================================================================
	// members

	public static final int INSTANCE_ID_MIN_PARTS = 5; // number of parts can be 6 in case of foreign cloud

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static String calculateInstanceId(final AuthorizationLevel level, final String cloud, final String provider, final AuthorizationTargetType targetType, final String target) {
		Assert.notNull(level, "level is empty");
		Assert.isTrue(!Utilities.isEmpty(cloud), "cloud is empty");
		Assert.isTrue(!Utilities.isEmpty(provider), "provider is empty");
		Assert.notNull(targetType, "targetType is empty");
		Assert.isTrue(!Utilities.isEmpty(target), "target is empty");

		return String.join(Constants.COMPOSITE_ID_DELIMITER, level.getPrefix(), cloud, provider, targetType.name(), target);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	public static String retrieveProviderName(final String instanceId) {
		Assert.isTrue(!Utilities.isEmpty(instanceId), "Instance id is null or empty");

		final String[] split = instanceId.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
		Assert.isTrue(split.length >= INSTANCE_ID_MIN_PARTS, "Invalid instance id");

		final boolean isLocal = split.length == INSTANCE_ID_MIN_PARTS && Defaults.DEFAULT_CLOUD.equalsIgnoreCase(split[1].trim());
		final int expectedLength = isLocal ? INSTANCE_ID_MIN_PARTS : INSTANCE_ID_MIN_PARTS + 1;
		Assert.isTrue(split.length == expectedLength, "Invalid instance id");

		return isLocal ? split[2] : split[3];
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private InstanceIdUtils() {
		throw new UnsupportedOperationException();
	}
}