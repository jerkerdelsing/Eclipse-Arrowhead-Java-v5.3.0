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
package eu.arrowhead.blacklist.jpa.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.blacklist.jpa.entity.Entry;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface EntryRepository extends RefreshableRepository<Entry, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<Entry> findAllBySystemNameIn(final List<String> names);

	//-------------------------------------------------------------------------------------------------
	public List<Entry> findAllBySystemNameAndActive(final String name, final boolean active);

	//-------------------------------------------------------------------------------------------------
	public Page<Entry> findAllByIdIn(final List<Long> ids, final Pageable pageable);
}