/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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
package eu.arrowhead.blacklist.init;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.blacklist.service.WhitelistService;

@ExtendWith(MockitoExtension.class)
public class BlacklistApplicationInitListenerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private BlacklistApplicationInitListener listener;

	@Mock
	private WhitelistService whitelistService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCustomInit() {
		doNothing().when(whitelistService).cleanDatabase();

		assertDoesNotThrow(() -> listener.customInit(null));

		verify(whitelistService).cleanDatabase();
	}
}