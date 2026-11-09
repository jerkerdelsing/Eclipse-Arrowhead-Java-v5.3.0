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
package eu.arrowhead.authentication.quartz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authentication.jpa.service.IdentityDbService;
import eu.arrowhead.common.exception.ArrowheadException;

@ExtendWith(MockitoExtension.class)
public class CleanerJobTest {

	//=================================================================================================
	// members

	@InjectMocks
	private CleanerJob job;

	@Mock
	private IdentityDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteExceptionHandled() {
		doThrow(ArrowheadException.class).when(dbService).removeExpiredSessions();

		assertDoesNotThrow(() -> job.execute(null));

		verify(dbService).removeExpiredSessions();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteOk() {
		doNothing().when(dbService).removeExpiredSessions();

		assertDoesNotThrow(() -> job.execute(null));

		verify(dbService).removeExpiredSessions();
	}
}