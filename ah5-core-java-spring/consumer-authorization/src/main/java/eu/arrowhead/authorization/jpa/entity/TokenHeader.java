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
package eu.arrowhead.authorization.jpa.entity;

import java.util.List;

import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
public class TokenHeader extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "tokenType", "requester", "consumerCloud", "consumer", "provider", "targetType", "target", "createdBy");
	public static final String DEFAULT_SORT_FIELD = "createdAt";

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AuthorizationTokenType tokenType;

	@Column(nullable = false)
	private String tokenHash;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String requester;

	@Column(nullable = false, length = VARCHAR_MEDIUM)
	private String consumerCloud = Defaults.DEFAULT_CLOUD;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String consumer;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String provider;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private AuthorizationTargetType targetType;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String target;

	@Column(nullable = true, length = VARCHAR_SMALL)
	private String scope;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TokenHeader() {
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	public TokenHeader(
			final AuthorizationTokenType tokenType,
			final String tokenHash,
			final String requester,
			final String consumerCloud,
			final String consumer,
			final String provider,
			final AuthorizationTargetType targetType,
			final String target,
			final String scope) {
		this.tokenType = tokenType;
		this.tokenHash = tokenHash;
		this.requester = requester;
		this.consumerCloud = consumerCloud;
		this.consumer = consumer;
		this.provider = provider;
		this.targetType = targetType;
		this.target = target;
		this.scope = Utilities.isEmpty(scope) ? null : scope;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "TokenHeader [id=" + id + ", createdAt=" + createdAt + ", tokenType=" + tokenType + ", tokenHash=" + tokenHash + ", requester=" + requester + ", consumerCloud=" + consumerCloud
				+ ", consumer=" + consumer + ", provider=" + provider + ", targetType=" + targetType + ", target=" + target + ", scope=" + scope + "]";
	}

	//=================================================================================================
	// boilerplate code

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenType getTokenType() {
		return tokenType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTokenType(final AuthorizationTokenType tokenType) {
		this.tokenType = tokenType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTokenHash() {
		return tokenHash;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTokenHash(final String tokenHash) {
		this.tokenHash = tokenHash;
	}

	//-------------------------------------------------------------------------------------------------
	public String getRequester() {
		return requester;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRequester(final String requester) {
		this.requester = requester;
	}

	//-------------------------------------------------------------------------------------------------
	public String getConsumerCloud() {
		return consumerCloud;
	}

	//-------------------------------------------------------------------------------------------------
	public void setConsumerCloud(final String consumerCloud) {
		this.consumerCloud = consumerCloud;
	}

	//-------------------------------------------------------------------------------------------------
	public String getConsumer() {
		return consumer;
	}

	//-------------------------------------------------------------------------------------------------
	public void setConsumer(final String consumer) {
		this.consumer = consumer;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProvider() {
		return provider;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProvider(final String provider) {
		this.provider = provider;
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTargetType getTargetType() {
		return targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetType(final AuthorizationTargetType targetType) {
		this.targetType = targetType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTarget() {
		return target;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTarget(final String target) {
		this.target = target;
	}

	//-------------------------------------------------------------------------------------------------
	public String getScope() {
		return scope;
	}

	//-------------------------------------------------------------------------------------------------
	public void setScope(final String scope) {
		this.scope = scope;
	}
}