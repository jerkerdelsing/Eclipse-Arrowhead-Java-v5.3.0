/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.gatekeeper.jpa.entity.Cloud;

@Repository
public interface CloudRepository extends RefreshableRepository<Cloud, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	List<Cloud> findAllByOrderByOperatorAscNameAsc();

	//-------------------------------------------------------------------------------------------------
	Optional<Cloud> findByNameAndOperator(final String name, final String operator);

	//-------------------------------------------------------------------------------------------------
	boolean existsByNameAndOperator(final String name, final String operator);

	//-------------------------------------------------------------------------------------------------
	long deleteByOperatorAndName(final String operator, final String name);
}
