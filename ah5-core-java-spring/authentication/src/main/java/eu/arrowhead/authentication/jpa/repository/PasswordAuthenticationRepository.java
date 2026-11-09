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

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authentication.jpa.entity.PasswordAuthentication;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface PasswordAuthenticationRepository extends RefreshableRepository<PasswordAuthentication, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<PasswordAuthentication> findBySystem(final System system);

	//-------------------------------------------------------------------------------------------------
	public List<PasswordAuthentication> findAllBySystemIn(final List<System> systems);

	//-------------------------------------------------------------------------------------------------
	public void deleteAllBySystemIn(final List<System> systems);
}