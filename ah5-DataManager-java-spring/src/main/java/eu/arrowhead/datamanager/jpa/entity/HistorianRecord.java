/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.jpa.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

@Entity
@Table(indexes = {
		@Index(name = "idx_historian_endpoint_time", columnList = "systemName,serviceName,baseTime"),
		@Index(name = "idx_historian_time_range", columnList = "minTime,maxTime")
})
public class HistorianRecord {

	@Id
	private UUID id;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String systemName;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String serviceName;

	@Column(nullable = false)
	private double baseTime;

	@Column(nullable = false)
	private double minTime;

	@Column(nullable = false)
	private double maxTime;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String payload;

	@Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime createdAt;

	public HistorianRecord() {
	}

	public HistorianRecord(
			final String systemName,
			final String serviceName,
			final double baseTime,
			final double minTime,
			final double maxTime,
			final String payload) {
		this.id = UUID.randomUUID();
		this.systemName = systemName;
		this.serviceName = serviceName;
		this.baseTime = baseTime;
		this.minTime = minTime;
		this.maxTime = maxTime;
		this.payload = payload;
	}

	@PrePersist
	public void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Utilities.utcNow();
	}

	public UUID getId() {
		return id;
	}

	public String getSystemName() {
		return systemName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public double getBaseTime() {
		return baseTime;
	}

	public double getMinTime() {
		return minTime;
	}

	public double getMaxTime() {
		return maxTime;
	}

	public String getPayload() {
		return payload;
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}
}
