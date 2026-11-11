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

package eu.arrowhead.deviceqosevaluator.quartz;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer;

@Configuration
public class QuartzConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	AutowiringSpringBeanJobFactory jobFactory(final ApplicationContext applicationContext) {
		final AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
		jobFactory.setApplicationContext(applicationContext);
		return jobFactory;
	}

	//-------------------------------------------------------------------------------------------------
	// Customizes the auto-configured scheduler instead of replacing it: Spring Boot's Quartz auto-configuration
	// also schedules the JobDetail/Trigger beans of the common library (the outsourced login job). A custom
	// SchedulerFactoryBean disabled it, so DeviceQoSEvaluator never logged in to Authentication.
	@Bean
	SchedulerFactoryBeanCustomizer jobFactoryCustomizer(final AutowiringSpringBeanJobFactory jobFactory) {
		return factory -> factory.setJobFactory(jobFactory);
	}
}
