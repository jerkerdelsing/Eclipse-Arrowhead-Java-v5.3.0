/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.solocalcloudconfigurator.jpa.entity.SolcPackageEntity;

@Repository
public interface SolcPackageRepository extends RefreshableRepository<SolcPackageEntity, String> {

	/** Package metadata without the content: id, designId, designName, designRevision, imageMode, architectures, fileName, sha256, sizeBytes, createdBy, createdAt. */
	@Query("select p.id, p.designId, p.designName, p.designRevision, p.imageMode, p.architectures, p.fileName, p.sha256, p.sizeBytes, p.createdBy, p.createdAt "
			+ "from SolcPackageEntity p order by p.createdAt desc")
	List<Object[]> listMetadata();

	long countByDesignId(String designId);
}
