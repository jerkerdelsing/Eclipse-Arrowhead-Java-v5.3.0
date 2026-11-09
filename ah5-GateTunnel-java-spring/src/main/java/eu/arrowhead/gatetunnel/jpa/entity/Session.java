/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

// A control session of the 4.x-compatible gateTunnelConnection (connect-provider / connect-consumer / close-sessions):
// bookkeeping of a reserved port of the control pool (8000-8099) and the session parties, kept as JSON documents.
@Entity
public class Session extends ArrowheadEntity {

	//=================================================================================================
	// members

	public static final String STATE_PROVIDER_READY = "PROVIDER_READY";
	public static final String STATE_ACTIVE = "ACTIVE";
	public static final String STATE_CLOSED = "CLOSED";

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String sessionId;

	@Column(nullable = false, unique = true, length = VARCHAR_MEDIUM)
	private String queueId;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String peerName;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String serviceDefinition;

	@Column(nullable = false, unique = true)
	private int tunnelPort;

	@Column(nullable = false, length = VARCHAR_TINY)
	private String state;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String consumerJson;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String providerJson;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String consumerCloudJson;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String providerCloudJson;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String relayJson;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Session() {
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumber")
	public Session(final String sessionId, final String queueId, final String peerName, final String serviceDefinition, final int tunnelPort, final String state,
			final String consumerJson, final String providerJson, final String consumerCloudJson, final String providerCloudJson, final String relayJson) {
		this.sessionId = sessionId;
		this.queueId = queueId;
		this.peerName = peerName;
		this.serviceDefinition = serviceDefinition;
		this.tunnelPort = tunnelPort;
		this.state = state;
		this.consumerJson = consumerJson;
		this.providerJson = providerJson;
		this.consumerCloudJson = consumerCloudJson;
		this.providerCloudJson = providerCloudJson;
		this.relayJson = relayJson;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public java.time.ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	public String getSessionId() {
		return sessionId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getQueueId() {
		return queueId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getPeerName() {
		return peerName;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public int getTunnelPort() {
		return tunnelPort;
	}

	//-------------------------------------------------------------------------------------------------
	public String getState() {
		return state;
	}

	//-------------------------------------------------------------------------------------------------
	public void setState(final String state) {
		this.state = state;
	}

	//-------------------------------------------------------------------------------------------------
	public String getConsumerJson() {
		return consumerJson;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProviderJson() {
		return providerJson;
	}

	//-------------------------------------------------------------------------------------------------
	public String getConsumerCloudJson() {
		return consumerCloudJson;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProviderCloudJson() {
		return providerCloudJson;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRelayJson() {
		return relayJson;
	}
}
