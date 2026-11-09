/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.serviceorchestration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(Constants.BASE_PACKAGE)
@EntityScan(SimpleStoreServiceOrchestrationConstants.DATABASE_ENTITY_PACKAGE)
@EnableJpaRepositories(basePackages = SimpleStoreServiceOrchestrationConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
@EnableScheduling
public class SimpleStoreServiceOrchestrationMain {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void main(final String[] args) {
		SpringApplication.run(SimpleStoreServiceOrchestrationMain.class, args);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	protected SimpleStoreServiceOrchestrationMain() {
	}
}