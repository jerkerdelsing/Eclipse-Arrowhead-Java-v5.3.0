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
package eu.arrowhead.authorization.quartz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import eu.arrowhead.authorization.AuthorizationConstants;
import jakarta.annotation.PostConstruct;

@Configuration
@EnableAutoConfiguration
public class CleanerJobConfig {

	//=================================================================================================
	// members

	@Value(AuthorizationConstants.$CLEANER_JOB_INTERVAL_WD)
	private long interval;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean(AuthorizationConstants.CLEANER_JOB)
	JobDetailFactoryBean cleanerJobDetail() {
		final JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
		jobDetailFactory.setJobClass(CleanerJob.class);
		jobDetailFactory.setDescription("Removing expired and/or old tokens");
		jobDetailFactory.setDurability(true);
		return jobDetailFactory;
	}

	//-------------------------------------------------------------------------------------------------
	@Bean(AuthorizationConstants.CLEANER_TRIGGER)
	SimpleTriggerFactoryBean cleanerTrigger(@Qualifier(AuthorizationConstants.CLEANER_JOB) final JobDetail job) {
		final SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setJobDetail(job);
		trigger.setRepeatInterval(interval);
		trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
		trigger.setStartDelay(interval);
		return trigger;
	}

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	public void init() {
		logger.info("Cleaner job is initialized.");
	}
}