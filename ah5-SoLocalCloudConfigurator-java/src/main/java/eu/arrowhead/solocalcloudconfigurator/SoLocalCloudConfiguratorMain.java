/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;

// The Arrowhead support system: soLocalCloudConfiguration on the mutual-TLS port 8496. The second packaging stage (source and
// image archives) runs on a host with Docker: kit/bin/assemble-package.sh inside every package; this system has no Docker access.
@SpringBootApplication
@ComponentScan(basePackages = Constants.BASE_PACKAGE,
		excludeFilters = {
				// the standard Spring Boot filter: test configurations of other tests stay out of a test's context
				@ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
				@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = eu.arrowhead.common.http.filter.authorization.BlacklistFilter.class)
		})
@EntityScan(SoLocalCloudConfiguratorConstants.DATABASE_ENTITY_PACKAGE)
@EnableJpaRepositories(basePackages = SoLocalCloudConfiguratorConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
public class SoLocalCloudConfiguratorMain {

	public static void main(final String[] args) throws Exception {
		SpringApplication.run(SoLocalCloudConfiguratorMain.class, args);
	}

	protected SoLocalCloudConfiguratorMain() {
	}
}
