/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;

@SpringBootApplication
@ComponentScan(Constants.BASE_PACKAGE)
@EntityScan(OnboardingConstants.DATABASE_ENTITY_PACKAGE)
@EnableJpaRepositories(basePackages = OnboardingConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
@EnableAsync
@EnableScheduling
public class OnboardingMain {

	public static void main(final String[] args) {
		SpringApplication.run(OnboardingMain.class, args);
	}

	protected OnboardingMain() {
	}
}
