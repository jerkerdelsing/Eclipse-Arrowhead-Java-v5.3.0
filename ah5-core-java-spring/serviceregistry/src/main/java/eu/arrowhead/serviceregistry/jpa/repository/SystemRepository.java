/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.System;

@Repository
public interface SystemRepository extends RefreshableRepository<System, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByNameIn(final Collection<String> names);

	//-------------------------------------------------------------------------------------------------
	public Optional<System> findByName(final String name);

	//-------------------------------------------------------------------------------------------------
	public Page<System> findAllByNameIn(final List<String> names, final Pageable pageble);

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByVersionIn(final List<String> versions);
}