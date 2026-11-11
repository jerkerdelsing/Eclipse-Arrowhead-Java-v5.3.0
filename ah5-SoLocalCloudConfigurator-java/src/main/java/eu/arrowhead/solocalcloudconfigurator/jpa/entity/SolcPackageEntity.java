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

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

// A built deployment package (tar.gz without source or image archives; the host assembler adds those).
@Entity
@Table(name = "solc_package")
public class SolcPackageEntity {

	@Id
	@Column(length = 36)
	private String id;

	@Column(name = "design_id", nullable = false, length = 36)
	private String designId;

	@Column(name = "design_name", nullable = false, length = 64)
	private String designName;

	@Column(name = "design_revision", nullable = false)
	private long designRevision;

	@Column(name = "image_mode", nullable = false, length = 16)
	private String imageMode;

	@Column(nullable = false, length = 64)
	private String architectures;

	@Column(name = "file_name", nullable = false, length = 160)
	private String fileName;

	@Column(nullable = false, length = 64, columnDefinition = "char(64)")
	private String sha256;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(nullable = false, columnDefinition = "longblob")
	private byte[] content;

	@Column(name = "created_by", nullable = false, length = 64)
	private String createdBy;

	@Column(name = "created_at", nullable = false, length = 40)
	private String createdAt;

	public SolcPackageEntity() {
	}

	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	public SolcPackageEntity(final String id, final String designId, final String designName, final long designRevision, final String imageMode, final String architectures,
			final String fileName, final String sha256, final byte[] content, final String createdBy, final String createdAt) {
		this.id = id;
		this.designId = designId;
		this.designName = designName;
		this.designRevision = designRevision;
		this.imageMode = imageMode;
		this.architectures = architectures;
		this.fileName = fileName;
		this.sha256 = sha256;
		this.sizeBytes = content.length;
		this.content = content;
		this.createdBy = createdBy;
		this.createdAt = createdAt;
	}

	public String getId() { return id; }
	public String getDesignId() { return designId; }
	public String getDesignName() { return designName; }
	public long getDesignRevision() { return designRevision; }
	public String getImageMode() { return imageMode; }
	public String getArchitectures() { return architectures; }
	public String getFileName() { return fileName; }
	public String getSha256() { return sha256; }
	public long getSizeBytes() { return sizeBytes; }
	public byte[] getContent() { return content; }
	public String getCreatedBy() { return createdBy; }
	public String getCreatedAt() { return createdAt; }
}
