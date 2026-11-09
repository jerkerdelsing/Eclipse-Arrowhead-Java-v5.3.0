/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.certificateauthority.jpa.entity.IssuedCertificate;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface IssuedCertificateRepository extends RefreshableRepository<IssuedCertificate, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<IssuedCertificate> findBySerialNumber(final String serialNumber);

	//-------------------------------------------------------------------------------------------------
	public List<IssuedCertificate> findAllByOrderByIdDesc();

	//-------------------------------------------------------------------------------------------------
	public List<IssuedCertificate> findByCommonNameAndRevokedAtIsNullAndCreatedByNot(final String commonName, final String createdBy);

	//-------------------------------------------------------------------------------------------------
	public List<IssuedCertificate> findByRevokedAtIsNotNull();

	//-------------------------------------------------------------------------------------------------
	public long countByRevokedAtIsNotNull();
}
