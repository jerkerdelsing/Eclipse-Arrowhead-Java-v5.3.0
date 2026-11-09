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

package eu.arrowhead.serviceorchestration.service.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleOrchestrationRequest {

	//=================================================================================================
	// members

	private String serviceDefinition;
	private List<String> preferredProviders;
	private Map<String, Boolean> orchestrationFlags;
	private Set<String> warnings;

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public SimpleOrchestrationRequest() {
	}

	//-------------------------------------------------------------------------------------------------
	public SimpleOrchestrationRequest(final String serviceDefinition, final List<String> preferredProviders, final Map<String, Boolean> orchestrationFlags, final Set<String> warnings) {
		this.serviceDefinition = serviceDefinition;
		this.preferredProviders = preferredProviders;
		this.orchestrationFlags = orchestrationFlags;
		this.warnings = warnings;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(final String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Boolean> getOrchestrationFlags() {
		return orchestrationFlags;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOrchestrationFlags(final Map<String, Boolean> orchestrationFlags) {
		this.orchestrationFlags = orchestrationFlags;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getPreferredProviders() {
		return preferredProviders;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPreferredProviders(final List<String> preferredProviders) {
		this.preferredProviders = preferredProviders;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getWarnings() {
		return warnings;
	}

	//-------------------------------------------------------------------------------------------------
	public void setWarnings(final Set<String> warnings) {
		this.warnings = warnings;
	}

	//-------------------------------------------------------------------------------------------------
	public void addWarning(final String warning) {
		if (this.warnings == null) {
			this.warnings = new HashSet<>();
		}
		this.warnings.add(warning);
	}
}
