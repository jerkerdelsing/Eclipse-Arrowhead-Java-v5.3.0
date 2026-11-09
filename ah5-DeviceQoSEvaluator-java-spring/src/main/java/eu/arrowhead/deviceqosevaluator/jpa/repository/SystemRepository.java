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
package eu.arrowhead.deviceqosevaluator.jpa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;

@SuppressWarnings("checkstyle:MethodNameCheck")
@Repository
public interface SystemRepository extends RefreshableRepository<System, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByNameIn(final Iterable<String> names);

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByDevice(final Device device);

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByDevice_Id(final UUID deviceId);

	//-------------------------------------------------------------------------------------------------
	public List<System> findAllByDeviceIsNull();

	//-------------------------------------------------------------------------------------------------
	public void deleteAllByNameIn(final Iterable<String> names);
}
