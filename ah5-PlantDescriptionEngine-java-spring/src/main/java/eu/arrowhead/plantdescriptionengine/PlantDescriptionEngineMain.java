/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;

@SpringBootApplication
@ComponentScan(Constants.BASE_PACKAGE)
@EntityScan(PlantDescriptionEngineConstants.DATABASE_ENTITY_PACKAGE)
@EnableJpaRepositories(basePackages = PlantDescriptionEngineConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
@EnableScheduling
public class PlantDescriptionEngineMain {

	public static void main(final String[] args) {
		SpringApplication.run(PlantDescriptionEngineMain.class, args);
	}

	protected PlantDescriptionEngineMain() {
	}
}
