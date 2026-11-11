/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.service;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.arrowhead.solocalcloudconfigurator.SoLocalCloudConfiguratorConstants;
import eu.arrowhead.solocalcloudconfigurator.catalog.PolicyCatalog;
import eu.arrowhead.solocalcloudconfigurator.policy.PolicyEngine;

@Configuration
public class CatalogConfig {

	@Bean
	public PolicyCatalog policyCatalog(@Value(SoLocalCloudConfiguratorConstants.$CATALOG_FILE_WD) final String file) throws Exception {
		return PolicyCatalog.load(Path.of(file));
	}

	@Bean
	public PolicyEngine policyEngine(final PolicyCatalog catalog) {
		return new PolicyEngine(catalog);
	}
}
