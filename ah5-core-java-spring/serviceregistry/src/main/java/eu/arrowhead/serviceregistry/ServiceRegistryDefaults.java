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
package eu.arrowhead.serviceregistry;

import eu.arrowhead.common.Defaults;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryInterfacePolicy;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryPolicy;

public final class ServiceRegistryDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String DISCOVERY_VERBOSE_DEFAULT = "false";
	public static final String EXTRA_INTERFACES_DEFAULT = "[]";
	public static final String SERVICE_DISCOVERY_POLICY_DEFAULT = ServiceDiscoveryPolicy.RESTRICTED_VALUE;
	public static final String SERVICE_DISCOVERY_DIRECT_ACCESS_DEFAULT = "\"\"";
	public static final String SERVICE_DISCOVERY_INTERFACE_POLICY_DEFAULT = ServiceDiscoveryInterfacePolicy.RESTRICTED_VALUE;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceRegistryDefaults() {
		throw new UnsupportedOperationException();
	}

}
