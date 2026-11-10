/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator;

import java.util.Arrays;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;
import eu.arrowhead.sosconfigurator.cli.SosCli;
import eu.arrowhead.sosconfigurator.executor.ExecutorApplication;

// Without arguments (or with "serve") the Arrowhead support system starts: the sosConfiguration service on the mutual-TLS port
// and the operator console on the loopback console port. "executor" starts a ColonyOS executor as a non-web Spring
// application. keygen, register, plan and apply are operator command-line tools.
@SpringBootApplication
@ComponentScan(basePackages = Constants.BASE_PACKAGE,
		excludeFilters = {
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = eu.arrowhead.common.http.filter.authorization.BlacklistFilter.class),
				// replaced by SosSecurityConfig, which leaves the plain-HTTP console connector on HTTP
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = eu.arrowhead.common.security.DefaultSecurityConfig.class),
				// the executor is a separate, non-web application
				@ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.arrowhead\\.sosconfigurator\\.executor\\..*")
		})
@EntityScan(SoSConfiguratorConstants.DATABASE_ENTITY_PACKAGE)
@EnableJpaRepositories(basePackages = SoSConfiguratorConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
public class SoSConfiguratorMain {

	//=================================================================================================
	// members

	private static final Set<String> TOOLS = Set.of("keygen", "register", "plan", "apply");

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) throws Exception {
		final String command = args.length == 0 ? "serve" : args[0];
		if (TOOLS.contains(command)) {
			System.exit(SosCli.run(args));
		}

		if ("executor".equals(command)) {
			// console logging only: the executor has no database for the JDBC log appender
			final String[] executorArgs = Arrays.copyOf(Arrays.copyOfRange(args, 1, args.length), args.length);
			executorArgs[args.length - 1] = "--logging.config=classpath:log4j2-executor.xml";
			new SpringApplicationBuilder(ExecutorApplication.class).web(WebApplicationType.NONE).run(executorArgs);
			return;
		}

		final String[] springArgs = "serve".equals(command) && args.length > 0 ? Arrays.copyOfRange(args, 1, args.length) : args;
		SpringApplication.run(SoSConfiguratorMain.class, springArgs);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	protected SoSConfiguratorMain() {
	}
}
