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
package eu.arrowhead.deviceqosevaluator.jpa.repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.deviceqosevaluator.jpa.entity.StatCpuTotalLoad;

@Repository
public interface StatCpuTotalLoadRepository extends RefreshableRepository<StatCpuTotalLoad, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Page<StatCpuTotalLoad> findAllByIdIn(final Collection<Long> ids, final Pageable pageable);

	//-------------------------------------------------------------------------------------------------
	public List<StatCpuTotalLoad> findAllByUuidAndTimestampAfter(final UUID uuid, final ZonedDateTime timestamp);

	//-------------------------------------------------------------------------------------------------
	public Page<StatCpuTotalLoad> findAllByTimestampBetween(final ZonedDateTime start, final ZonedDateTime end, final Pageable pageable);

	//-------------------------------------------------------------------------------------------------
	public Page<StatCpuTotalLoad> findAllByUuidInAndTimestampBetween(final Set<UUID> uuids, final ZonedDateTime start, final ZonedDateTime end, final Pageable pageable);

	//-------------------------------------------------------------------------------------------------
	public boolean existsByUuid(final UUID uuid);

	//-------------------------------------------------------------------------------------------------
	public void deleteAllByTimestampBefore(final ZonedDateTime timestamp);
}
