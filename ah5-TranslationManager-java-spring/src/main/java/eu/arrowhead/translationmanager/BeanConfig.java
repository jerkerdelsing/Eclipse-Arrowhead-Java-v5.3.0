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
package eu.arrowhead.translationmanager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.arrowhead.translationmanager.service.matchmaking.DataModelTranslatorMatchmaker;
import eu.arrowhead.translationmanager.service.matchmaking.DefaultDataModelTranslatorMatchmaker;
import eu.arrowhead.translationmanager.service.matchmaking.DefaultInterfaceTranslatorMatchmaker;
import eu.arrowhead.translationmanager.service.matchmaking.InterfaceTranslatorMatchmaker;

@Configuration
public class BeanConfig {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	InterfaceTranslatorMatchmaker initInterfaceTranslatorMatchmaker() {
		return new DefaultInterfaceTranslatorMatchmaker();
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	DataModelTranslatorMatchmaker initDataModelTranslatorMatchmaker() {
		return new DefaultDataModelTranslatorMatchmaker();
	}
}