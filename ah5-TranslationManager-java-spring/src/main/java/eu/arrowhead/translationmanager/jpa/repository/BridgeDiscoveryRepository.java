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
package eu.arrowhead.translationmanager.jpa.repository;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDiscovery;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;

@Repository
public interface BridgeDiscoveryRepository extends RefreshableRepository<BridgeDiscovery, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<BridgeDiscovery> findByHeader(final BridgeHeader header);

	//-------------------------------------------------------------------------------------------------
	public void deleteByHeader(final BridgeHeader header);

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodNameCheck")
	public List<BridgeDiscovery> findByHeader_CreatedAtLessThan(final ZonedDateTime time);
}