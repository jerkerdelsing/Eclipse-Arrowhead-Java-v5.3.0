/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

// A plant description; includes, systems and connections are kept as one JSON document (v4.6 stored them the same way).
@Entity
public class PlantDescription extends ArrowheadEntity {

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String name;

	@Column(nullable = false)
	private boolean active;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGTEXT")
	private String body;

	public PlantDescription() {
	}

	public PlantDescription(final String name, final boolean active, final String body) {
		this.name = name;
		this.active = active;
		this.body = body;
	}

	public long getId() {
		return id;
	}

	public java.time.ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(final boolean active) {
		this.active = active;
	}

	public String getBody() {
		return body;
	}

	public void setBody(final String body) {
		this.body = body;
	}
}
