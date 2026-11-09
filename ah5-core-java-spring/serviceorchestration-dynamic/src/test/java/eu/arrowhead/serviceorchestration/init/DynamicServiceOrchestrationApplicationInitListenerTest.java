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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.dto.KeyValuesDTO;
import eu.arrowhead.serviceorchestration.service.thread.PushOrchestrationThread;

@ExtendWith(MockitoExtension.class)
public class DynamicServiceOrchestrationApplicationInitListenerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DynamicServiceOrchestrationApplicationInitListener listener;

	@Mock
	private PushOrchestrationThread pushOrchestrationThread;

	@Mock
	private ServiceInterfaceAddressPropertyProcessor serviceInterfaceAddressPropertyProcessor;

	@Mock
	private ArrowheadHttpService arrowheadHttpService;

	@Captor
	private ArgumentCaptor<MultiValueMap<String, String>> queryParamCaptor;

	@Captor
	private ArgumentCaptor<List<String>> stringListCaptor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testCustomInit() {
		final KeyValuesDTO configDTO = new KeyValuesDTO(Map.of(Constants.SERVICE_ADDRESS_ALIAS, "foo,bar"));
		when(arrowheadHttpService.consumeService(eq(Constants.SERVICE_DEF_GENERAL_MANAGEMENT), eq(Constants.SERVICE_OP_GET_CONFIG), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(KeyValuesDTO.class), any(MultiValueMap.class)))
				.thenReturn(configDTO);

		assertDoesNotThrow(() -> listener.customInit(Mockito.mock(ContextRefreshedEvent.class)));

		verify(pushOrchestrationThread).start();
		verify(arrowheadHttpService).consumeService(eq(Constants.SERVICE_DEF_GENERAL_MANAGEMENT), eq(Constants.SERVICE_OP_GET_CONFIG), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(KeyValuesDTO.class), queryParamCaptor.capture());
		verify(serviceInterfaceAddressPropertyProcessor).setAddressAliasNames(stringListCaptor.capture());

		assertTrue(queryParamCaptor.getValue().size() == 1);
		assertTrue(queryParamCaptor.getValue().get(Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM).size() == 1);
		assertTrue(queryParamCaptor.getValue().get(Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM).get(0).equals(Constants.SERVICE_ADDRESS_ALIAS));
		assertTrue(stringListCaptor.getValue().size() == 2);
		assertTrue(stringListCaptor.getValue().contains("foo"));
		assertTrue(stringListCaptor.getValue().contains("bar"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testCustomInitNoAddressAliases() {
		final KeyValuesDTO configDTO = new KeyValuesDTO(Map.of());
		when(arrowheadHttpService.consumeService(eq(Constants.SERVICE_DEF_GENERAL_MANAGEMENT), eq(Constants.SERVICE_OP_GET_CONFIG), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(KeyValuesDTO.class), any(MultiValueMap.class)))
				.thenReturn(configDTO);

		assertDoesNotThrow(() -> listener.customInit(Mockito.mock(ContextRefreshedEvent.class)));

		verify(pushOrchestrationThread).start();
		verify(arrowheadHttpService).consumeService(eq(Constants.SERVICE_DEF_GENERAL_MANAGEMENT), eq(Constants.SERVICE_OP_GET_CONFIG), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(KeyValuesDTO.class), queryParamCaptor.capture());
		verify(serviceInterfaceAddressPropertyProcessor, never()).setAddressAliasNames(anyList());

		assertTrue(queryParamCaptor.getValue().size() == 1);
		assertTrue(queryParamCaptor.getValue().get(Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM).size() == 1);
		assertTrue(queryParamCaptor.getValue().get(Constants.SERVICE_OP_GET_CONFIG_REQ_PARAM).get(0).equals(Constants.SERVICE_ADDRESS_ALIAS));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testCustomInitStandalone() {
		ReflectionTestUtils.setField(listener, "standaloneMode", true);

		assertDoesNotThrow(() -> listener.customInit(Mockito.mock(ContextRefreshedEvent.class)));

		verify(pushOrchestrationThread).start();
		verify(arrowheadHttpService, never()).consumeService(anyString(), anyString(), anyString(), any(), any(MultiValueMap.class));
		verify(serviceInterfaceAddressPropertyProcessor, never()).setAddressAliasNames(anyList());
	}
}
