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
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.arrowhead.sosconfigurator.SoSConfiguratorConstants;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.planner.Planner;

// The trusted catalog of the organization (relay, member clouds, exports and forbidden consumers) and the planner over it.
@Configuration
public class CatalogConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	Catalog catalog(@Value(SoSConfiguratorConstants.$CATALOG_FILE_WD) final String file) throws IOException {
		return Catalog.load(Path.of(file));
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	Planner planner(final Catalog catalog) {
		return new Planner(catalog);
	}
}
