/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.gatekeeper.jpa.service.GatekeeperDbService;
import eu.arrowhead.gatekeeper.service.dto.DTOConverter;
import eu.arrowhead.gatekeeper.service.normalization.GatekeeperNormalization;
import eu.arrowhead.gatekeeper.service.validation.GatekeeperValidation;

// Neighbour clouds and relays (gatekeeperManagement).
@Service
public class ManagementService {

	//=================================================================================================
	// members

	@Autowired
	private GatekeeperDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> clouds() {
		return DTOConverter.entries(dbService.clouds().stream().map(DTOConverter::cloud).toList());
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> relays() {
		return DTOConverter.entries(dbService.relays().stream().map(DTOConverter::relay).toList());
	}

	//-------------------------------------------------------------------------------------------------
	// every item is checked before any row is written; all are written or none
	public Map<String, Object> upsertClouds(final Object body) {
		final List<Object> items = GatekeeperNormalization.managementItems(body);
		dbService.upsertClouds(items.stream().map(GatekeeperValidation::cloud).toList());
		return clouds();
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> upsertRelays(final Object body) {
		final List<Object> items = GatekeeperNormalization.managementItems(body);
		dbService.upsertRelays(items.stream().map(GatekeeperValidation::relay).toList());
		return relays();
	}

	//-------------------------------------------------------------------------------------------------
	// "<operator>/<name>" after /gatekeeper/mgmt/clouds/
	public boolean deleteCloud(final String rest) {
		final String[] p = rest.split("/", -1);
		if (p.length != 2) {
			throw new IllegalArgumentException("operator/name required");
		}

		return dbService.deleteCloud(p[0], p[1]);
	}
}
