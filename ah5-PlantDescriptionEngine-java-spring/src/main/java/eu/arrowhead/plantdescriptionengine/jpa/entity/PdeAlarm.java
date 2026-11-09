/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.jpa.entity;

import java.time.ZonedDateTime;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

// A mismatch between the active plant description and the running local cloud (v4.6 PdeAlarm).
@Entity
public class PdeAlarm extends ArrowheadEntity {

	@Column(length = VARCHAR_MEDIUM)
	private String systemId;

	@Column(length = VARCHAR_SMALL)
	private String systemName;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String cause;

	@Column(nullable = false, length = VARCHAR_LARGE)
	private String description;

	@Column(nullable = false)
	private boolean acknowledged;

	private ZonedDateTime clearedAt;

	private ZonedDateTime acknowledgedAt;

	public PdeAlarm() {
	}

	public PdeAlarm(final String systemId, final String systemName, final String cause, final String description) {
		this.systemId = systemId;
		this.systemName = systemName;
		this.cause = cause;
		this.description = description;
	}

	public long getId() {
		return id;
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public String getSystemId() {
		return systemId;
	}

	public String getSystemName() {
		return systemName;
	}

	public String getCause() {
		return cause;
	}

	public String getDescription() {
		return description;
	}

	public boolean isAcknowledged() {
		return acknowledged;
	}

	public void setAcknowledged(final boolean acknowledged) {
		this.acknowledged = acknowledged;
	}

	public ZonedDateTime getClearedAt() {
		return clearedAt;
	}

	public void setClearedAt(final ZonedDateTime clearedAt) {
		this.clearedAt = clearedAt;
	}

	public ZonedDateTime getAcknowledgedAt() {
		return acknowledgedAt;
	}

	public void setAcknowledgedAt(final ZonedDateTime acknowledgedAt) {
		this.acknowledgedAt = acknowledgedAt;
	}

	public String getSeverity() {
		return clearedAt == null ? "WARNING" : "CLEARED";
	}
}
