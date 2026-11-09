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
package eu.arrowhead.authorization.jpa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.dto.enums.AuthorizationLevel;

@Repository
public interface AuthPolicyRepository extends RefreshableRepository<AuthPolicy, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void deleteByLevelAndHeaderId(final AuthorizationLevel level, final long headerId);

	//-------------------------------------------------------------------------------------------------
	public void deleteByLevelAndHeaderIdIn(final AuthorizationLevel level, final Collection<Long> headerIds);

	//-------------------------------------------------------------------------------------------------
	public List<AuthPolicy> findByLevelAndHeaderId(final AuthorizationLevel level, final long headerId);

	//-------------------------------------------------------------------------------------------------
	public List<AuthPolicy> findByLevelAndHeaderIdAndScopeIn(final AuthorizationLevel level, final long headerId, final Collection<String> scopes);
}