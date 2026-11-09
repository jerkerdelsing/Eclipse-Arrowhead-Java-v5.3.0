/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// An accepted observation that is not yet published on the MQTT sysMonitorData output.
@Entity
@Table(name = "outbox")
public class Outbox {

	//=================================================================================================
	// members

	public static final int ID_LENGTH = 255;
	public static final int ERROR_LENGTH = 255;
	public static final int TIME_LENGTH = 40;

	@Id
	@Column(name = "observation_id", nullable = false, length = ID_LENGTH)
	private String observationId;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String payload;

	@Column(name = "mqtt_published", nullable = false)
	private boolean mqttPublished = false;

	@Column(nullable = false)
	private int attempts = 0;

	// epoch seconds
	@Column(name = "next_attempt", nullable = false)
	private double nextAttempt = 0;

	@Column(name = "last_error", nullable = true, length = ERROR_LENGTH)
	private String lastError;

	@Column(name = "created_at", nullable = false, length = TIME_LENGTH)
	private String createdAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Outbox() {
	}

	//-------------------------------------------------------------------------------------------------
	public Outbox(final String observationId, final String payload, final String createdAt) {
		this.observationId = observationId;
		this.payload = payload;
		this.createdAt = createdAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void failed(final String error, final double retryAt) {
		this.lastError = error;
		this.attempts++;
		this.nextAttempt = retryAt;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getObservationId() {
		return observationId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getPayload() {
		return payload;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isMqttPublished() {
		return mqttPublished;
	}

	//-------------------------------------------------------------------------------------------------
	public int getAttempts() {
		return attempts;
	}

	//-------------------------------------------------------------------------------------------------
	public double getNextAttempt() {
		return nextAttempt;
	}

	//-------------------------------------------------------------------------------------------------
	public String getLastError() {
		return lastError;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCreatedAt() {
		return createdAt;
	}
}
