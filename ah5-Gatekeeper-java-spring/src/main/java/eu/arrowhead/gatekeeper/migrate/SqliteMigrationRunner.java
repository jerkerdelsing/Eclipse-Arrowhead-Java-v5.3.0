/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.migrate;

import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.jpa.service.GatekeeperDbService;
import jakarta.annotation.Resource;

// "migrate-sqlite <file>": imports the clouds and relays of a native 5.2.2 gatekeeper.db into the private MySQL schema,
// then exits. Rows that already exist are kept. Nothing is registered in the ServiceRegistry.
@Component
@ConditionalOnProperty(name = GatekeeperConstants.MIGRATE_SQLITE_FILE)
public class SqliteMigrationRunner implements ApplicationRunner {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Value("${" + GatekeeperConstants.MIGRATE_SQLITE_FILE + "}")
	private String file;

	@Autowired
	private GatekeeperDbService dbService;

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// before the registration listener (order 10): this run is not a cloud system
	@EventListener
	@Order(5)
	public void standalone(final ContextRefreshedEvent event) {
		arrowheadContext.put(Constants.SERVER_STANDALONE_MODE, true);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void run(final ApplicationArguments args) throws Exception {
		final SqliteImport source = SqliteImport.read(Path.of(file));
		final int imported = dbService.importRows(source.getClouds(), source.getRelays(), source.getRelayCreatedAt());
		final String summary = "migrate-sqlite: " + source.getClouds().size() + " cloud(s) and " + source.getRelays().size() + " relay(s) read from " + file
				+ ", " + imported + " imported, " + (source.getClouds().size() + source.getRelays().size() - imported) + " already present";
		logger.warn(summary);
		System.out.println(summary);
	}
}
