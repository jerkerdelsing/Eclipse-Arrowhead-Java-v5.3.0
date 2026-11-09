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
package eu.arrowhead.deviceqosevaluator.quartz.job;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.springframework.data.util.Pair;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.deviceqosevaluator.engine.MeasurementEngine;

@ExtendWith(MockitoExtension.class)
public class MeasurementOrganizerJobTest {

	//=================================================================================================
	// members

	@InjectMocks
	private MeasurementOrganizerJob job;

	@Mock
	private MeasurementEngine measurementEngine;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalExceptionHandled() throws ArrowheadException, SchedulerException {
		when(measurementEngine.organize()).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(measurementEngine).organize();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteInternalOk() throws ArrowheadException, SchedulerException {
		when(measurementEngine.organize()).thenReturn(Pair.of(1, 1));

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(measurementEngine).organize();
	}
}