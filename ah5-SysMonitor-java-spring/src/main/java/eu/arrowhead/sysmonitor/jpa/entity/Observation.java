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
import jakarta.persistence.Index;
import jakarta.persistence.Table;

// A normalised observation of the retention-bounded history; payload is the JSON published on sysMonitorData.
@Entity
@Table(name = "observation", indexes = {
		@Index(name = "idx_observation_by_system", columnList = "provider_system,observation_type,source_epoch"),
		@Index(name = "idx_observation_by_age", columnList = "received_epoch") })
public class Observation {

	//=================================================================================================
	// members

	public static final int ID_LENGTH = 255;
	public static final int NAME_LENGTH = 255;
	public static final int SHORT_LENGTH = 63;
	public static final int TIME_LENGTH = 40;

	@Id
	@Column(name = "observation_id", nullable = false, length = ID_LENGTH)
	private String observationId;

	@Column(name = "provider_system", nullable = false, length = NAME_LENGTH)
	private String providerSystem;

	@Column(name = "service_instance", nullable = false, length = NAME_LENGTH)
	private String serviceInstance;

	@Column(name = "observation_type", nullable = false, length = NAME_LENGTH)
	private String observationType;

	@Column(name = "source_time", nullable = false, length = TIME_LENGTH)
	private String sourceTime;

	// epoch seconds
	@Column(name = "source_epoch", nullable = false)
	private double sourceEpoch;

	@Column(name = "received_time", nullable = false, length = TIME_LENGTH)
	private String receivedTime;

	@Column(name = "received_epoch", nullable = false)
	private double receivedEpoch;

	@Column(nullable = true, length = SHORT_LENGTH)
	private String quality;

	@Column(nullable = true, length = SHORT_LENGTH)
	private String unit;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String payload;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Observation() {
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	public Observation(final String observationId, final String providerSystem, final String serviceInstance, final String observationType, final String sourceTime,
			final double sourceEpoch, final String receivedTime, final double receivedEpoch, final String quality, final String unit, final String payload) {
		this.observationId = observationId;
		this.providerSystem = providerSystem;
		this.serviceInstance = serviceInstance;
		this.observationType = observationType;
		this.sourceTime = sourceTime;
		this.sourceEpoch = sourceEpoch;
		this.receivedTime = receivedTime;
		this.receivedEpoch = receivedEpoch;
		this.quality = quality;
		this.unit = unit;
		this.payload = payload;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getObservationId() {
		return observationId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProviderSystem() {
		return providerSystem;
	}

	//-------------------------------------------------------------------------------------------------
	public String getObservationType() {
		return observationType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getSourceTime() {
		return sourceTime;
	}

	//-------------------------------------------------------------------------------------------------
	public double getSourceEpoch() {
		return sourceEpoch;
	}

	//-------------------------------------------------------------------------------------------------
	public double getReceivedEpoch() {
		return receivedEpoch;
	}

	//-------------------------------------------------------------------------------------------------
	public String getPayload() {
		return payload;
	}
}
