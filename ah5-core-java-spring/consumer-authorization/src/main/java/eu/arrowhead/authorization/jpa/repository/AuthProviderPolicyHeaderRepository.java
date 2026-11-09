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
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@Repository
public interface AuthProviderPolicyHeaderRepository extends RefreshableRepository<AuthProviderPolicyHeader, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<AuthProviderPolicyHeader> findByCloudAndProviderAndTargetTypeAndTarget(final String cloud, final String provider, final AuthorizationTargetType targetType, final String target);

	//-------------------------------------------------------------------------------------------------
	public Optional<AuthProviderPolicyHeader> findByInstanceId(final String instanceId);

	//-------------------------------------------------------------------------------------------------
	public List<AuthProviderPolicyHeader> findByInstanceIdIn(final List<String> instanceIds);

	//-------------------------------------------------------------------------------------------------
	public List<AuthProviderPolicyHeader> findByProviderIn(final List<String> providers);

	//-------------------------------------------------------------------------------------------------
	public List<AuthProviderPolicyHeader> findByCloudIn(final List<String> clouds);

	//-------------------------------------------------------------------------------------------------
	public List<AuthProviderPolicyHeader> findByTargetIn(final List<String> targets);

	//-------------------------------------------------------------------------------------------------
	public Page<AuthProviderPolicyHeader> findAllByIdIn(final Collection<Long> ids, final Pageable pageble);

}