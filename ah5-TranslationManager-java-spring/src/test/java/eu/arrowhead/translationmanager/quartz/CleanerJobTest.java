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
package eu.arrowhead.translationmanager.quartz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDiscovery;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService;

@ExtendWith(MockitoExtension.class)
public class CleanerJobTest {

	//=================================================================================================
	// members

	@InjectMocks
	private CleanerJob job;

	@Mock
	private BridgeDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteException() throws JobExecutionException {
		ReflectionTestUtils.setField(job, "maxAge", 1);

		when(dbService.getBridgeDiscoveriesCreatedBefore(any(ZonedDateTime.class))).thenThrow(InternalServerError.class);

		assertDoesNotThrow(() -> job.execute(null));

		verify(dbService).getBridgeDiscoveriesCreatedBefore(any(ZonedDateTime.class));
		verify(dbService, never()).handleObsoletedBridgeDiscovery(any(BridgeDiscovery.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteNothingToDo() throws JobExecutionException {
		ReflectionTestUtils.setField(job, "maxAge", 1);

		when(dbService.getBridgeDiscoveriesCreatedBefore(any(ZonedDateTime.class))).thenReturn(List.of());

		assertDoesNotThrow(() -> job.execute(null));

		verify(dbService).getBridgeDiscoveriesCreatedBefore(any(ZonedDateTime.class));
		verify(dbService, never()).handleObsoletedBridgeDiscovery(any(BridgeDiscovery.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecute() throws JobExecutionException {
		ReflectionTestUtils.setField(job, "maxAge", 1);
		final BridgeDiscovery bd = new BridgeDiscovery();
		bd.setId(1L);

		when(dbService.getBridgeDiscoveriesCreatedBefore(any(ZonedDateTime.class))).thenReturn(List.of(bd));
		doNothing().when(dbService).handleObsoletedBridgeDiscovery(bd);

		assertDoesNotThrow(() -> job.execute(null));

		verify(dbService).getBridgeDiscoveriesCreatedBefore(any(ZonedDateTime.class));
		verify(dbService).handleObsoletedBridgeDiscovery(bd);
	}
}