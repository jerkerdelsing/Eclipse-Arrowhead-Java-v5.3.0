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
package eu.arrowhead.common.mqtt.model;

import java.util.HashMap;
import java.util.Map;

import eu.arrowhead.common.mqtt.MqttQoS;
import eu.arrowhead.dto.MqttRequestTemplate;

public class MqttRequestModel {

	//=================================================================================================
	// members

	private final String traceId;
	private final String operation;
	private final String baseTopic;
	private final String responseTopic;
	private final MqttQoS qosRequirement;
	private final Map<String, String> params;
	private final Object payload;

	private String requester;
	private boolean isSysOp = false;
	private final Map<String, String> attributes = new HashMap<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public MqttRequestModel(final String baseTopic, final String operation, final MqttRequestTemplate template) {
		this.traceId = template.traceId();
		this.baseTopic = baseTopic;
		this.operation = operation;
		this.responseTopic = template.responseTopic();
		this.qosRequirement = MqttQoS.valueOf(template.qosRequirement());
		this.params = template.params() == null ? new HashMap<>() : template.params();
		this.payload = template.payload();
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getTraceId() {
		return traceId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOperation() {
		return operation;
	}

	//-------------------------------------------------------------------------------------------------
	public String getBaseTopic() {
		return baseTopic;
	}

	//-------------------------------------------------------------------------------------------------
	public String getResponseTopic() {
		return responseTopic;
	}

	//-------------------------------------------------------------------------------------------------
	public MqttQoS getQosRequirement() {
		return qosRequirement;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, String> getParams() {
		return params;
	}

	//-------------------------------------------------------------------------------------------------
	public Object getPayload() {
		return payload;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRequester() {
		return requester;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRequester(final String requester) {
		this.requester = requester;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isSysOp() {
		return isSysOp;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSysOp(final boolean isSysOp) {
		this.isSysOp = isSysOp;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAttribute(final String key, final String value) {
		this.attributes.put(key, value);
	}

	//-------------------------------------------------------------------------------------------------
	public String getAttribute(final String key) {
		return this.attributes.get(key);
	}
}