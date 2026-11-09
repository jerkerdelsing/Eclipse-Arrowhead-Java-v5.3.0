/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;

// The CA is trust infrastructure: it must keep working when Blacklist is unavailable, so the blacklist filter is not used.
@SpringBootApplication
@ComponentScan(basePackages = Constants.BASE_PACKAGE,
		excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = eu.arrowhead.common.http.filter.authorization.BlacklistFilter.class))
@EntityScan(CertificateAuthorityConstants.DATABASE_ENTITY_PACKAGE)
@EnableJpaRepositories(basePackages = CertificateAuthorityConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
@EnableScheduling
public class CertificateAuthorityMain {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {
		SpringApplication.run(CertificateAuthorityMain.class, args);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	protected CertificateAuthorityMain() {
	}
}
