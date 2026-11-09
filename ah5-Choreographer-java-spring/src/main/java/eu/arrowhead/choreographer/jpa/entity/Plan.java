/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.entity;

import java.util.List;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan")
public class Plan extends ArrowheadEntity {

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "name", "createdAt", "updatedAt");

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String name;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String firstActionName;

	public Plan() {
	}

	public Plan(final String name, final String firstActionName) {
		this.name = name;
		this.firstActionName = firstActionName;
	}

	public String getName() { return name; }
	public void setName(final String name) { this.name = name; }
	public String getFirstActionName() { return firstActionName; }
	public void setFirstActionName(final String firstActionName) { this.firstActionName = firstActionName; }
}
