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
package eu.arrowhead.common.quartz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.JobDetail;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import jakarta.annotation.PostConstruct;

@Configuration
@EnableAutoConfiguration
@ConditionalOnProperty(name = Constants.AUTHENTICATION_POLICY, havingValue = AuthenticationPolicy.OUTSOURCED_VALUE, matchIfMissing = false)
public class OutsourcedLoginConfig {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Value(Constants.$AUTHENTICATOR_LOGIN_INTERVAL_WD)
	private long interval;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean(Constants.OUTSOURCED_LOGIN_JOB_FACTORY)
	JobDetailFactoryBean outsourcedLoginJobDetail() {
		final JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
		jobDetailFactory.setJobClass(OutsourcedLoginJob.class);
		jobDetailFactory.setDescription("Login and refresh login to authentication system");
		jobDetailFactory.setDurability(true);
		return jobDetailFactory;
	}

	//-------------------------------------------------------------------------------------------------
	@Bean(Constants.OUTSOURCED_LOGIN_TRIGGER)
	SimpleTriggerFactoryBean outsourcedLoginTrigger(@Qualifier(Constants.OUTSOURCED_LOGIN_JOB_FACTORY) final JobDetail job) {
		final SimpleTriggerFactoryBean trigger = new SimpleTriggerFactoryBean();
		trigger.setJobDetail(job);
		trigger.setRepeatInterval(interval);
		trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
		return trigger;
	}

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	public void init() {
		logger.info("Login job (to authentication server) is initialized.");
	}
}