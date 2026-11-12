/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.service;

import java.io.IOException;
import java.time.Duration;

import eu.arrowhead.sosconfigurator.colony.ColonyClient;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;

// The workflow side of the SoSConfigurator: submits plans to the executors and asks them for status.
public interface SosBackend {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	String submit(Planner.Plan plan, SosRequest request) throws IOException;

	//-------------------------------------------------------------------------------------------------
	ColonyClient.Deployment deployment(String id) throws IOException;

	//-------------------------------------------------------------------------------------------------
	String status(String role, Duration timeout) throws IOException;
}
