/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;

@SpringBootApplication
// Gatekeeper has never depended on Blacklist; the blacklist filter is not used (as CertificateAuthority).
@ComponentScan(basePackages = Constants.BASE_PACKAGE,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = eu.arrowhead.common.http.filter.authorization.BlacklistFilter.class))
@EntityScan(GatekeeperConstants.DATABASE_ENTITY_PACKAGE)
@EnableJpaRepositories(basePackages = GatekeeperConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
@EnableScheduling
public class GatekeeperMain {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {
		if (args.length > 0 && GatekeeperConstants.MIGRATE_SQLITE_ARGUMENT.equals(args[0])) {
			System.exit(migrate(args));
		}

		SpringApplication.run(GatekeeperMain.class, args);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// One-off import of a native 5.2.2 gatekeeper.db: no HTTP listener, no registration, no relay, no sysMonitor.
	private static int migrate(final String[] args) {
		if (args.length < 2 || args[1].isBlank()) {
			System.err.println("Usage: migrate-sqlite <gatekeeper.db> [--spring.config.additional-location=...]");
			return 2;
		}

		final SpringApplication application = new SpringApplication(GatekeeperMain.class);
		// highest precedence, above the deployment configuration file: no relay, no sysMonitor, and with TLS no Authentication
		// login (the certificate policy needs no identity token to write the private database)
		application.addInitializers(context -> {
			final Map<String, Object> overrides = new HashMap<>();
			overrides.put(GatekeeperConstants.MIGRATE_SQLITE_FILE, args[1]);
			// the common security configuration needs a web application context; -1 starts no HTTP listener
			overrides.put("server.port", "-1");
			overrides.put(GatekeeperConstants.RELAY_ENABLED, "false");
			overrides.put(Constants.SYS_MONITOR_ENABLED, "false");
			if (context.getEnvironment().getProperty(Constants.SERVER_SSL_ENABLED, Boolean.class, false)) {
				overrides.put(Constants.AUTHENTICATION_POLICY, "certificate");
			}

			context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("migrate-sqlite", overrides));
		});
		final String[] rest = Arrays.copyOfRange(args, 2, args.length);
		try (ConfigurableApplicationContext context = application.run(rest)) {
			return SpringApplication.exit(context);
		}
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	protected GatekeeperMain() {
	}
}
