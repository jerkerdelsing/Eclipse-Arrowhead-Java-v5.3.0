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

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@Repository
public interface TokenHeaderRepository extends RefreshableRepository<TokenHeader, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> findByTokenHash(final String tokenHash);

	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByTokenHashIn(final List<String> tokenHashes);

	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByRequester(final String requester);

	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByTokenType(final AuthorizationTokenType tokenType);

	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByConsumerCloud(final String consumerCloud);

	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByConsumer(final String consumer);

	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByProvider(final String provider);

	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByTarget(final String target);

	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findAllByTargetType(final AuthorizationTargetType targetType);

	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> findByProviderAndTokenHash(final String provider, final String tokenHash);

	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope(
			final String consumerCloud,
			final String consumer,
			final String provider,
			final AuthorizationTargetType targetType,
			final String target,
			final String scope);

	//-------------------------------------------------------------------------------------------------
	public Page<TokenHeader> findAllByIdIn(final Collection<Long> ids, final Pageable pageble);
}