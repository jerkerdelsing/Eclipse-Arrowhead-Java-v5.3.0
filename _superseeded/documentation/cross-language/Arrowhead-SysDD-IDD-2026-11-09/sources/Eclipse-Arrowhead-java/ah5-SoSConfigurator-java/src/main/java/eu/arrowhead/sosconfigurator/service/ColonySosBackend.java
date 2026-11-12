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

import org.springframework.stereotype.Component;

import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.colony.ColonyClient;
import eu.arrowhead.sosconfigurator.colony.SosColony;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;

// ColonyOS workflows (COLONIES_* environment variables, the API's own executor identity). Connected on first use, so the
// system starts and serves the catalog and plans while the Colonies server is still coming up.
@Component
public class ColonySosBackend implements SosBackend {

	//=================================================================================================
	// members

	private final Catalog catalog;
	private volatile SosColony colony;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ColonySosBackend(final Catalog catalog) {
		this.catalog = catalog;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String submit(final Planner.Plan plan, final SosRequest request) throws IOException {
		return colony().submit(plan, request);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public ColonyClient.Deployment deployment(final String id) throws IOException {
		return colony().deployment(id);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String status(final String role, final Duration timeout) throws IOException {
		return colony().status(role, timeout);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SosColony colony() throws IOException {
		SosColony current = colony;
		if (current == null) {
			synchronized (this) {
				if (colony == null) {
					try {
						colony = new SosColony(new ColonyClient(ColonyClient.Config.fromEnv()), catalog);
					} catch (final IllegalStateException ex) {
						throw new IOException(ex.getMessage(), ex);
					}
				}

				current = colony;
			}
		}

		return current;
	}
}
