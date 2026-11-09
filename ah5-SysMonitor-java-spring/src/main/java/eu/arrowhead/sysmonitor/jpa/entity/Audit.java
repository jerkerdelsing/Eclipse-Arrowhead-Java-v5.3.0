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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Management actions (subscribe, unsubscribe, reconcile).
@Entity
@Table(name = "audit")
public class Audit {

	//=================================================================================================
	// members

	public static final int NAME_LENGTH = 63;
	public static final int TEXT_LENGTH = 1024;
	public static final int TIME_LENGTH = 40;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = TIME_LENGTH)
	private String at;

	@Column(nullable = false, length = NAME_LENGTH)
	private String actor;

	@Column(nullable = false, length = NAME_LENGTH)
	private String operation;

	@Column(nullable = false, length = TEXT_LENGTH)
	private String target;

	@Column(nullable = false, length = TEXT_LENGTH)
	private String outcome;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Audit() {
	}

	//-------------------------------------------------------------------------------------------------
	public Audit(final String at, final String actor, final String operation, final String target, final String outcome) {
		this.at = at;
		this.actor = actor;
		this.operation = operation;
		this.target = target;
		this.outcome = outcome;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public Long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public String getActor() {
		return actor;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOperation() {
		return operation;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTarget() {
		return target;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOutcome() {
		return outcome;
	}
}
