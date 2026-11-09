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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.SchedulerException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.jpa.entity.Device;
import eu.arrowhead.deviceqosevaluator.jpa.entity.System;
import eu.arrowhead.deviceqosevaluator.jpa.service.DeviceDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.StatDbService;
import eu.arrowhead.deviceqosevaluator.jpa.service.SystemDbService;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.AugmentedMeasurementJobScheduler;
import eu.arrowhead.deviceqosevaluator.quartz.scheduler.RttMeasurementJobScheduler;

@ExtendWith(MockitoExtension.class)
public class CleaningJobTest {

	//=================================================================================================
	// members

	@InjectMocks
	private CleaningJob job;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Mock
	private DeviceDbService deviceDbService;

	@Mock
	private SystemDbService systemDbService;

	@Mock
	private StatDbService statDbService;

	@Mock
	private RttMeasurementJobScheduler rttMeasurementJobScheduler;

	@Mock
	private AugmentedMeasurementJobScheduler augmentedMeasurementJobScheduler;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteInternalExceptionHandling() {
		when(sysInfo.getRawMeasurementDataMaxAge()).thenReturn(10);
		doThrow(InternalServerError.class).when(statDbService).removeBeforeTimestamp(any(ZonedDateTime.class));

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(sysInfo).getRawMeasurementDataMaxAge();
		verify(statDbService).removeBeforeTimestamp(any(ZonedDateTime.class));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteInternalOk() throws SchedulerException {
		final UUID d1Id = UUID.fromString("fcd92344-e643-4c3f-9dc1-9f5cdff6262a");
		final Device d1 = new Device(d1Id, "10.0.0.1", 12345, true, true);
		d1.setUpdatedAt(ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of(Constants.UTC)));

		final UUID d2Id = UUID.fromString("37afcc60-e8c6-45ea-9fce-17f281e67a56");
		final Device d2 = new Device(d2Id, "10.0.0.2", 12345, true, true);
		d2.setUpdatedAt(ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of(Constants.UTC)));

		final UUID d3Id = UUID.fromString("1fa30526-a3c5-45fb-aced-98bda87c70de");
		final Device d3 = new Device(d3Id, "10.0.0.3", 12345, true, true);
		d3.setUpdatedAt(Utilities.utcNow());

		final UUID d4Id = UUID.fromString("bd8bc9fc-71cd-42c1-9ad3-a90db83e1048");
		final Device d4 = new Device(d4Id, "10.0.0.4", 12345, true, false);

		when(sysInfo.getRawMeasurementDataMaxAge()).thenReturn(10);
		doNothing().when(statDbService).removeBeforeTimestamp(any(ZonedDateTime.class));
		when(sysInfo.getMaxPageSize()).thenReturn(3);
		when(deviceDbService.getPage(PageRequest.of(0, 3, Direction.ASC, Device.DEFAULT_SORT_FIELD))).thenReturn(new PageImpl<>(
				List.of(d1, d2, d3),
				PageRequest.of(0, 3, Direction.ASC, Device.DEFAULT_SORT_FIELD),
				4));
		when(deviceDbService.getPage(PageRequest.of(1, 3, Direction.ASC, Device.DEFAULT_SORT_FIELD))).thenReturn(new PageImpl<>(List.of(d4)));
		when(sysInfo.getInactiveDeviceMaxAge()).thenReturn(10);
		when(statDbService.hasAny(d1Id)).thenReturn(false);
		when(rttMeasurementJobScheduler.isScheduled(d1)).thenReturn(false);
		when(augmentedMeasurementJobScheduler.isScheduled(d1)).thenReturn(false);
		when(statDbService.hasAny(d2Id)).thenReturn(true);
		when(systemDbService.findByDeviceId(d2Id)).thenReturn(List.of());
		when(rttMeasurementJobScheduler.isScheduled(d2)).thenReturn(true);
		when(augmentedMeasurementJobScheduler.isScheduled(d2)).thenReturn(true);
		when(systemDbService.findByDeviceId(d3Id)).thenReturn(List.of(new System()));
		when(systemDbService.findByDeviceId(d4Id)).thenReturn(List.of(new System()));
		doNothing().when(deviceDbService).update(List.of(d2));
		doNothing().when(deviceDbService).update(List.of());
		doNothing().when(deviceDbService).remove(List.of(d1));

		assertDoesNotThrow(() -> job.executeInternal(null));

		verify(sysInfo).getRawMeasurementDataMaxAge();
		verify(statDbService).removeBeforeTimestamp(any(ZonedDateTime.class));
		verify(sysInfo, times(2)).getMaxPageSize();
		verify(deviceDbService).getPage(PageRequest.of(0, 3, Direction.ASC, Device.DEFAULT_SORT_FIELD));
		verify(deviceDbService).getPage(PageRequest.of(1, 3, Direction.ASC, Device.DEFAULT_SORT_FIELD));
		verify(sysInfo, times(3)).getInactiveDeviceMaxAge();
		verify(statDbService).hasAny(d1Id);
		verify(rttMeasurementJobScheduler).isScheduled(d1);
		verify(rttMeasurementJobScheduler, never()).stop(List.of(d1));
		verify(augmentedMeasurementJobScheduler).isScheduled(d1);
		verify(augmentedMeasurementJobScheduler, never()).stop(List.of(d1));
		verify(statDbService).hasAny(d2Id);
		verify(systemDbService).findByDeviceId(d2Id);
		verify(rttMeasurementJobScheduler).isScheduled(d2);
		verify(rttMeasurementJobScheduler).stop(List.of(d2));
		verify(augmentedMeasurementJobScheduler).isScheduled(d2);
		verify(augmentedMeasurementJobScheduler).stop(List.of(d2));
		verify(systemDbService).findByDeviceId(d3Id);
		verify(systemDbService).findByDeviceId(d4Id);
		verify(deviceDbService).update(List.of(d2));
		verify(deviceDbService).update(List.of());
		verify(deviceDbService).remove(List.of(d1));
	}
}