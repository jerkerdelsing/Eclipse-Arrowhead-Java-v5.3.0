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
package eu.arrowhead.common.service.util;

import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;

public final class ServiceInstanceIdUtils {

	//=================================================================================================
	// members

	private static final int parts = 3;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static String calculateInstanceId(final String systemName, final String serviceDefinitionName, final String version) {
		Assert.isTrue(!Utilities.isEmpty(systemName), "systemName is empty");
		Assert.isTrue(!Utilities.isEmpty(serviceDefinitionName), "serviceDefinitionName is empty");
		Assert.isTrue(!Utilities.isEmpty(version), "version is empty");

		return systemName + Constants.COMPOSITE_ID_DELIMITER + serviceDefinitionName + Constants.COMPOSITE_ID_DELIMITER + version;
	}

	//-------------------------------------------------------------------------------------------------
	public static String retrieveSystemNameFromInstanceId(final String instanceId) {
		Assert.isTrue(!Utilities.isEmpty(instanceId), "instanceId is empty");

		final String[] split = instanceId.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
		Assert.isTrue(split.length == parts, "Invalid instanceId");

		return split[0];
	}

    //-------------------------------------------------------------------------------------------------
    public static String retrieveServiceDefinitionFromInstanceId(final String instanceId) {
        Assert.isTrue(!Utilities.isEmpty(instanceId), "instanceId is empty");

        final String[] split = instanceId.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
        Assert.isTrue(split.length == parts, "Invalid instanceId");

        return split[1];
    }

	//-------------------------------------------------------------------------------------------------
	public static String retrieveVersionFromInstanceId(final String instanceId) {
		Assert.isTrue(!Utilities.isEmpty(instanceId), "instanceId is empty");

		final String[] split = instanceId.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
		Assert.isTrue(split.length == parts, "Invalid instanceId");

		return split[2];
	}

	//-------------------------------------------------------------------------------------------------
	public static ServiceInstanceIdParts breakDownInstanceId(final String instanceId) {
		Assert.isTrue(!Utilities.isEmpty(instanceId), "instanceId is empty");

		final String[] split = instanceId.split(Constants.COMPOSITE_ID_DELIMITER_REGEXP);
		Assert.isTrue(split.length == parts, "Invalid instanceId");

		return new ServiceInstanceIdParts(split[0], split[1], split[2]);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceIdUtils() {
		throw new UnsupportedOperationException();
	}
}