/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A stored System of LocalClouds design: the SolcDesign document as JSON text and an optimistic revision.
@Entity
@Table(name = "solc_design")
public class SolcDesignEntity {

	@Id
	@Column(length = 36)
	private String id;

	@Column(nullable = false, length = 64, unique = true)
	private String name;

	@Column(nullable = false, columnDefinition = "mediumtext")
	private String design;

	@Column(nullable = false)
	private long revision;

	@Column(name = "updated_by", nullable = false, length = 64)
	private String updatedBy;

	@Column(name = "updated_at", nullable = false, length = 40)
	private String updatedAt;

	public SolcDesignEntity() {
	}

	public SolcDesignEntity(final String id, final String name, final String design, final long revision, final String updatedBy, final String updatedAt) {
		this.id = id;
		update(name, design, revision, updatedBy, updatedAt);
	}

	public final void update(final String name, final String design, final long revision, final String updatedBy, final String updatedAt) {
		this.name = name;
		this.design = design;
		this.revision = revision;
		this.updatedBy = updatedBy;
		this.updatedAt = updatedAt;
	}

	public String getId() { return id; }
	public String getName() { return name; }
	public String getDesign() { return design; }
	public long getRevision() { return revision; }
	public String getUpdatedBy() { return updatedBy; }
	public String getUpdatedAt() { return updatedAt; }
}
