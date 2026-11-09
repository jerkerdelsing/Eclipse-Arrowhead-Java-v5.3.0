/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// A configured neighbour cloud; (name, operator) is unique, as the native primary key was.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_cloud_name_operator", columnNames = { "name", "operator" }))
public class Cloud extends ArrowheadEntity {

	//=================================================================================================
	// members

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String name;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String operator;

	@Column(nullable = true, length = VARCHAR_MEDIUM)
	private String gatekeeperAddress;

	@Column(nullable = true)
	private Integer gatekeeperPort;

	@Column(nullable = false, columnDefinition = "MEDIUMTEXT")
	private String metadata;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Cloud() {
	}

	//-------------------------------------------------------------------------------------------------
	public Cloud(final String name, final String operator) {
		this.name = name;
		this.operator = operator;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public String getName() {
		return name;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOperator() {
		return operator;
	}

	//-------------------------------------------------------------------------------------------------
	public String getGatekeeperAddress() {
		return gatekeeperAddress;
	}

	//-------------------------------------------------------------------------------------------------
	public void setGatekeeperAddress(final String gatekeeperAddress) {
		this.gatekeeperAddress = gatekeeperAddress;
	}

	//-------------------------------------------------------------------------------------------------
	public Integer getGatekeeperPort() {
		return gatekeeperPort;
	}

	//-------------------------------------------------------------------------------------------------
	public void setGatekeeperPort(final Integer gatekeeperPort) {
		this.gatekeeperPort = gatekeeperPort;
	}

	//-------------------------------------------------------------------------------------------------
	public String getMetadata() {
		return metadata;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMetadata(final String metadata) {
		this.metadata = metadata;
	}
}
