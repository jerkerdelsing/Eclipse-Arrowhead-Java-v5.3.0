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
package eu.arrowhead.common.collector;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.model.ServiceModel;

public interface ICollectorDriver {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void init() throws ArrowheadException;

	//-------------------------------------------------------------------------------------------------
	public ServiceModel acquireService(final String serviceDefinitionName, final String interfaceTemplateName, final String providerName) throws ArrowheadException;

	//-------------------------------------------------------------------------------------------------
	// operation: the operation the caller is going to consume (null if unknown); an orchestrating driver asks for it explicitly
	public default ServiceModel acquireService(final String serviceDefinitionName, final String interfaceTemplateName, final String providerName, final String operation) throws ArrowheadException {
		return acquireService(serviceDefinitionName, interfaceTemplateName, providerName);
	}

	//-------------------------------------------------------------------------------------------------
	// how long an acquired model of the service may be reused; 0 means for the lifetime of the process
	public default long cacheTimeToLiveMillis(final String serviceDefinitionName) {
		return 0;
	}
}