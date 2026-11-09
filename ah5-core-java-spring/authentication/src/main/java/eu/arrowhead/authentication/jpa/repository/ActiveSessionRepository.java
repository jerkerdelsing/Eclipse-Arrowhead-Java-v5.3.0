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
package eu.arrowhead.authentication.jpa.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface ActiveSessionRepository extends RefreshableRepository<ActiveSession, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<ActiveSession> findBySystem(final System system);

	//-------------------------------------------------------------------------------------------------
	public Optional<ActiveSession> findByToken(final String token);

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodNameCheck")
	public List<ActiveSession> findAllBySystem_NameContainsIgnoreCase(final String namePart);

	//-------------------------------------------------------------------------------------------------
	public Page<ActiveSession> findAllByIdIn(final Pageable pageable, final List<Long> ids);

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodNameCheck")
	public void deleteBySystem_Name(final String systemName);

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodNameCheck")
	public void deleteBySystem_NameIn(final List<String> systemNames);

	//-------------------------------------------------------------------------------------------------
	public void deleteByExpirationTimeLessThan(final ZonedDateTime time);
}