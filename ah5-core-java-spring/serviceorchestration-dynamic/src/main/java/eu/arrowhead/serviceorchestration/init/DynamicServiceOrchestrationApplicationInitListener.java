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
package eu.arrowhead.serviceorchestration.init;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.serviceorchestration.service.thread.PushOrchestrationThread;

@Component
public class DynamicServiceOrchestrationApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	@Autowired
	private PushOrchestrationThread pushOrchestrationThread;

	@Autowired
	private ServiceInterfaceAddressPropertyProcessor serviceInterfaceAddressPropertyProcessor;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException {
		logger.debug("customInit started...");

		pushOrchestrationThread.start();

		if (standaloneMode) {
			return;
		}

		initServiceInterfaceAddressTypeFilter();

	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void initServiceInterfaceAddressTypeFilter() {
		logger.debug("initServiceInterfaceAddressTypeFilter started...");

		final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(Map.of(Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM, List.of(Constants.SERVICE_ADDRESS_ALIAS)));
		final KeyValuesDTO srConfigDTO = arrowheadHttpService.consumeService(
				Constants.SERVICE_DEF_GENERAL_MANAGEMENT,
				Constants.SERVICE_OP_GET_CONFIG,
				Constants.SYS_NAME_SERVICE_REGISTRY,
				KeyValuesDTO.class,
				queryParams);
		final String serviceAddressAliasListStr = srConfigDTO.map().get(Constants.SERVICE_ADDRESS_ALIAS);

		if (!Utilities.isEmpty(serviceAddressAliasListStr)) {
			final List<String> serviceAddressAliasList = Arrays.asList(serviceAddressAliasListStr.split(Constants.COMMA));
			serviceInterfaceAddressPropertyProcessor.setAddressAliasNames(serviceAddressAliasList.stream().map(alias -> alias.trim()).toList());
		}
	}
}