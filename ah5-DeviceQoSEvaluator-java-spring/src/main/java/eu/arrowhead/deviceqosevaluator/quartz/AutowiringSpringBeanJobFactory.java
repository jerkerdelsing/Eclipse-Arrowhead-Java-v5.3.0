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

import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

public class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

	//=================================================================================================
	// members

	private AutowireCapableBeanFactory beanFactory;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void setApplicationContext(final ApplicationContext applicationContext) {
		this.beanFactory = applicationContext.getAutowireCapableBeanFactory();
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
		final Object jobInstance = super.createJobInstance(bundle);
		beanFactory.autowireBean(jobInstance); // Enable @Autowired in Quartz jobs
		return jobInstance;
	}
}
