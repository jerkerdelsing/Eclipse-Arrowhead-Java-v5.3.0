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

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;

import eu.arrowhead.serviceorchestration.service.thread.PushOrchestrationThread;

@ExtendWith(MockitoExtension.class)
public class SimpleStoreServiceOrchestrationApplicationInitListenerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private SimpleStoreServiceOrchestrationApplicationInitListener initListener;

	@Mock
	private PushOrchestrationThread pushOrchestrationThread;

	@Mock
	private ContextRefreshedEvent contextRefreshedEvent;

	//=================================================================================================
	// Tests for customInit

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInitStartsPushOrchestrationThread() throws InterruptedException {
		initListener.customInit(contextRefreshedEvent);

		verify(pushOrchestrationThread).start();
	}
}
