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

// A system that can execute plan steps; its name is its Arrowhead system name.
@Entity
@Table(name = "executor")
public class Executor extends ArrowheadEntity {

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "name", "createdAt", "updatedAt");

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String name;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String address;

	@Column(nullable = false)
	private int port;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String basePath;

	// locked executors are excluded from selection (while being removed)
	@Column(nullable = false)
	private boolean locked;

	public Executor() {
	}

	public Executor(final String name, final String address, final int port, final String basePath) {
		this.name = name;
		this.address = address;
		this.port = port;
		this.basePath = basePath;
	}

	public String getName() { return name; }
	public String getAddress() { return address; }
	public int getPort() { return port; }
	public String getBasePath() { return basePath; }
	public boolean isLocked() { return locked; }
	public void setAddress(final String address) { this.address = address; }
	public void setPort(final int port) { this.port = port; }
	public void setBasePath(final String basePath) { this.basePath = basePath; }
	public void setLocked(final boolean locked) { this.locked = locked; }
}
