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

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.common.jpa.RefreshableRepository;

@Repository
public interface TimeLimitedTokenRepository extends RefreshableRepository<TimeLimitedToken, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<TimeLimitedToken> findByHeader(final TokenHeader header);
}