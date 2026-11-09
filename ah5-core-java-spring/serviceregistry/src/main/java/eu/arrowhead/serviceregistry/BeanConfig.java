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
package eu.arrowhead.serviceregistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import eu.arrowhead.common.collector.ICollectorDriver;
import eu.arrowhead.serviceregistry.collector.DatabaseCollectorDriver;

@Configuration
public class BeanConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Primary
	@Bean
	ICollectorDriver getCollectorDriver() {
		return new DatabaseCollectorDriver();
	}
}