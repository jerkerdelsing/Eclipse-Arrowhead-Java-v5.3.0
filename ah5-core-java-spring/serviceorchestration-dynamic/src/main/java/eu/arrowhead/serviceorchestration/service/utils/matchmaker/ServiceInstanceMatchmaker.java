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
package eu.arrowhead.serviceorchestration.service.utils.matchmaker;

import java.util.List;

import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

public interface ServiceInstanceMatchmaker {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationCandidate doMatchmaking(final OrchestrationForm form, final List<OrchestrationCandidate> candidates);
}