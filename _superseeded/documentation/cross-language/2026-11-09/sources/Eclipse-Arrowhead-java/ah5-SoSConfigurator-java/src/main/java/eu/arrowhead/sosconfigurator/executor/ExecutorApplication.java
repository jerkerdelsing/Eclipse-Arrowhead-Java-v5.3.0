/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.executor;

import java.nio.file.Path;
import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

import eu.arrowhead.sosconfigurator.SoSConfiguratorConstants;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.cli.SosCli;
import eu.arrowhead.sosconfigurator.colony.ColonyClient;
import eu.arrowhead.sosconfigurator.colony.SosColony;
import eu.arrowhead.sosconfigurator.runner.StepRunner;

// A ColonyOS executor of one role (the relay or one local cloud), run on the host that holds that role's directories. It is a
// non-web Spring application without the Arrowhead filter chain and database: it is the configurator's agent, not a cloud system,
// and it checks every step against its own catalog. ColonyOS settings come from COLONIES_* environment variables.
@SpringBootConfiguration
public class ExecutorApplication {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	CommandLineRunner executor(
			@Value(SoSConfiguratorConstants.$CATALOG_FILE_WD) final String catalogFile,
			@Value("${sos.role}") final String role,
			@Value("${sos.base:.}") final String baseDir,
			@Value("${sos.scripts:}") final String scriptsDir) {
		return args -> {
			final Catalog catalog = Catalog.load(Path.of(catalogFile));
			final Path base = Path.of(baseDir).toAbsolutePath().normalize();
			final Path scripts = scriptsDir.isBlank() ? base.resolve("scripts") : Path.of(scriptsDir);
			final StepRunner runner = new StepRunner(catalog, role, base, scripts, new StepRunner.ExecCommander());
			final SosColony colony = new SosColony(new ColonyClient(ColonyClient.Config.fromEnv()), catalog);
			System.out.println(LocalTime.now() + " executor " + SosCli.executorName(role) + " (type " + catalog.executorType(role) + ") serving");
			colony.serve(runner, message -> System.out.println(LocalTime.now() + " " + message));
		};
	}
}
