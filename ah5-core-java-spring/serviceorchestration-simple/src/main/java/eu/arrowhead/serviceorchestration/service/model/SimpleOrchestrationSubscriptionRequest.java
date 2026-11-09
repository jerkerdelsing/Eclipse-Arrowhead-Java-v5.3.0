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

import eu.arrowhead.dto.OrchestrationNotifyInterfaceDTO;

public class SimpleOrchestrationSubscriptionRequest {

	//=================================================================================================
	// members

	private String targetSystemName;
	private SimpleOrchestrationRequest orchestrationRequest;
	private OrchestrationNotifyInterfaceDTO notifyInterface;
	private Long duration;

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public SimpleOrchestrationSubscriptionRequest(final String targetSystemName, final SimpleOrchestrationRequest orchestrationRequest, final OrchestrationNotifyInterfaceDTO notifyInterface, final Long duration) {
		this.targetSystemName = targetSystemName;
		this.orchestrationRequest = orchestrationRequest;
		this.notifyInterface = notifyInterface;
		this.duration = duration;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTargetSystemName() {
		return targetSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetSystemName(final String targetSystemName) {
		this.targetSystemName = targetSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public SimpleOrchestrationRequest getOrchestrationRequest() {
		return orchestrationRequest;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOrchestrationRequest(final SimpleOrchestrationRequest orchestrationRequest) {
		this.orchestrationRequest = orchestrationRequest;
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationNotifyInterfaceDTO getNotifyInterface() {
		return notifyInterface;
	}

	//-------------------------------------------------------------------------------------------------
	public void setNotifyInterface(final OrchestrationNotifyInterfaceDTO notifyInterface) {
		this.notifyInterface = notifyInterface;
	}

	//-------------------------------------------------------------------------------------------------
	public Long getDuration() {
		return duration;
	}

	//-------------------------------------------------------------------------------------------------
	public void setDuration(final Long duration) {
		this.duration = duration;
	}

}
