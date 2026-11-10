/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.sosconfigurator.jpa.entity.SosDeployment;

@Repository
public interface SosDeploymentRepository extends RefreshableRepository<SosDeployment, String> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<SosDeployment> findTop100ByOrderBySubmittedAtDesc();
}
