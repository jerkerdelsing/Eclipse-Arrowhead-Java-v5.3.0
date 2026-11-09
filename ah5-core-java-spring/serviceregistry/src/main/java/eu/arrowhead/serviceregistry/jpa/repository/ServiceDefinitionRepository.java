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
package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;

@Repository
public interface ServiceDefinitionRepository extends RefreshableRepository<ServiceDefinition, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<ServiceDefinition> findAllByNameIn(final Collection<String> names);
}