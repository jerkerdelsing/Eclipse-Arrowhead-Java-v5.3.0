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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;

@Repository
public interface BridgeDetailsRepository extends RefreshableRepository<BridgeDetails, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<BridgeDetails> findByHeader(final BridgeHeader header);

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodNameCheck")
	public List<BridgeDetails> findByHeader_UuidIn(final List<String> uuids);

	//-------------------------------------------------------------------------------------------------
	public List<BridgeDetails> findByConsumerIn(final List<String> consumers);

	//-------------------------------------------------------------------------------------------------
	public List<BridgeDetails> findByProviderIn(final List<String> providers);

	//-------------------------------------------------------------------------------------------------
	public List<BridgeDetails> findByServiceDefinitionIn(final List<String> serviceDefinitions);

	//-------------------------------------------------------------------------------------------------
	public List<BridgeDetails> findByInterfaceTranslatorIn(final List<String> interfaceTranslators);

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodNameCheck")
	public List<BridgeDetails> findByHeader_CreatedByIn(final List<String> creators);

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodNameCheck")
	public List<BridgeDetails> findByHeader_StatusIn(final List<TranslationBridgeStatus> statuses);

	//-------------------------------------------------------------------------------------------------
	public Page<BridgeDetails> findAllByIdIn(final Collection<Long> ids, final Pageable pageable);
}