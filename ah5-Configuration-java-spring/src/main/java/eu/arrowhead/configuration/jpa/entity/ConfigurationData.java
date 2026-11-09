/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;

// One configuration file per system. The content is stored as raw bytes; the HTTP interfaces carry it Base64 encoded.
@Entity
public class ConfigurationData extends ArrowheadEntity {

	@Column(nullable = false, unique = true, length = VARCHAR_SMALL)
	private String systemName;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String fileName;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String contentType;

	@Lob
	@Column(nullable = false, columnDefinition = "LONGBLOB")
	private byte[] data;

	public ConfigurationData() {
	}

	public ConfigurationData(final String systemName, final String fileName, final String contentType, final byte[] data) {
		this.systemName = systemName;
		this.fileName = fileName;
		this.contentType = contentType;
		this.data = data;
	}

	public String getSystemName() {
		return systemName;
	}

	public String getFileName() {
		return fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public byte[] getData() {
		return data;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public void setContentType(final String contentType) {
		this.contentType = contentType;
	}

	public void setData(final byte[] data) {
		this.data = data;
	}
}
