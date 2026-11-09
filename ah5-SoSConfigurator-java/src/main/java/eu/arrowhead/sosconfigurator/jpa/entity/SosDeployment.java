/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.jpa.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// The record of one submitted SoS: who submitted which request, and the plan the policies allowed. The workflow state itself is
// kept by ColonyOS and read through get-deployment.
@Entity
@Table(name = "sos_deployment")
public class SosDeployment {

	//=================================================================================================
	// members

	public static final int ID_LENGTH = 64;
	public static final int NAME_LENGTH = 255;
	public static final int SUBMITTER_LENGTH = 127;

	// the ColonyOS workflow id (64 hex characters)
	@Id
	@Column(length = ID_LENGTH)
	private String id;

	@Column(name = "sos_name", nullable = false, length = NAME_LENGTH)
	private String sosName;

	// "console" for the operator console, otherwise the authenticated Arrowhead system
	@Column(name = "submitted_by", nullable = false, length = SUBMITTER_LENGTH)
	private String submittedBy;

	@Column(name = "submitted_at", nullable = false)
	private Instant submittedAt;

	@Column(name = "request_json", nullable = false, columnDefinition = "MEDIUMTEXT")
	private String requestJson;

	@Column(name = "plan_json", nullable = false, columnDefinition = "MEDIUMTEXT")
	private String planJson;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SosDeployment() {
	}

	//-------------------------------------------------------------------------------------------------
	public SosDeployment(final String id, final String sosName, final String submittedBy, final Instant submittedAt, final String requestJson, final String planJson) {
		this.id = id;
		this.sosName = sosName;
		this.submittedBy = submittedBy;
		this.submittedAt = submittedAt;
		this.requestJson = requestJson;
		this.planJson = planJson;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public String getSosName() {
		return sosName;
	}

	//-------------------------------------------------------------------------------------------------
	public String getSubmittedBy() {
		return submittedBy;
	}

	//-------------------------------------------------------------------------------------------------
	public Instant getSubmittedAt() {
		return submittedAt;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRequestJson() {
		return requestJson;
	}

	//-------------------------------------------------------------------------------------------------
	public String getPlanJson() {
		return planJson;
	}
}
