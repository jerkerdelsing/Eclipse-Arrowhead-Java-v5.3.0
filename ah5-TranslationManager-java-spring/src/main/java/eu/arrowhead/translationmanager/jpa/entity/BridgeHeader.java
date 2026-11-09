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
package eu.arrowhead.translationmanager.jpa.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class BridgeHeader extends ArrowheadEntity {

	//=================================================================================================
	// members

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String uuid;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String createdBy;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private TranslationBridgeStatus status = TranslationBridgeStatus.NEW;

	@Column(nullable = true)
	private String message;

	private int usageReportCount = 0;

	@Column(nullable = true)
	private ZonedDateTime alivesAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public BridgeHeader() {
	}

	//-------------------------------------------------------------------------------------------------
	public BridgeHeader(final UUID uuid, final String createdBy) {
		this.uuid = uuid.toString();
		this.createdBy = createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "BridgeHeader [id=" + id + ", uuid=" + uuid + ", createdBy=" + getCreatedBy() + ", status=" + status + ", message=" + message + ", usageReportCount=" + usageReportCount
				+ ", alivesAt=" + alivesAt + ", updatedAt=" + updatedAt + ", createdAt=" + createdAt + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getUuid() {
		return uuid;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUuid(final String uuid) {
		this.uuid = uuid;
	}

	//-------------------------------------------------------------------------------------------------
	public String getCreatedBy() {
		return createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCreatedBy(final String createdBy) {
		this.createdBy = createdBy;
	}

	//-------------------------------------------------------------------------------------------------
	public TranslationBridgeStatus getStatus() {
		return status;
	}

	//-------------------------------------------------------------------------------------------------
	public void setStatus(final TranslationBridgeStatus status) {
		this.status = status;
	}

	//-------------------------------------------------------------------------------------------------
	public String getMessage() {
		return message;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMessage(final String message) {
		this.message = message;
	}

	//-------------------------------------------------------------------------------------------------
	public int getUsageReportCount() {
		return usageReportCount;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUsageReportCount(final int usageReportCount) {
		this.usageReportCount = usageReportCount;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getAlivesAt() {
		return alivesAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAlivesAt(final ZonedDateTime alivesAt) {
		this.alivesAt = alivesAt;
	}
}