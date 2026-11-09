/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;

import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;

// Acknowledgement rules of the intake: stored observations are acknowledged, invalid ones rejected without requeue, and a
// storage failure is requeued.
public class AmqpIntakeTest {

	//=================================================================================================
	// members

	private MonitorDbService store;
	private Channel channel;
	private AmqpIntake intake;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		store = mock(MonitorDbService.class);
		channel = mock(Channel.class);
		when(channel.isOpen()).thenReturn(true);
		intake = new AmqpIntake();
		ReflectionTestUtils.setField(intake, "store", store);
		ReflectionTestUtils.setField(intake, "state", new SysMonitorState());
		ReflectionTestUtils.setField(intake, "cloud", "TestCloud");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void validObservationIsStoredThenAcknowledged() throws Exception {
		when(store.enqueue(any())).thenReturn(true);
		intake.deliver(channel, delivery(7, "TestCloud.sysmonitor.Pump.pump.echo", "{\"sourceTime\":\"2026-10-15T12:00:00Z\",\"value\":{\"status\":\"alive\"}}"));
		verify(store).enqueue(any());
		verify(channel).basicAck(7, false);
		assertEquals("SysMonitor.observations", intake.queue());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void invalidObservationIsRejectedWithoutRequeue() throws Exception {
		intake.deliver(channel, delivery(8, "TestCloud.sysmonitor.Pump.pump.echo", "{\"sourceTime\":\"2026-10-15T12:00:00Z\",\"value\":{\"status\":\"fiction\"}}"));
		intake.deliver(channel, delivery(9, "TestCloud.sysmonitor.Pump.pump.echo", "not json"));
		verify(channel).basicReject(8, false);
		verify(channel).basicReject(9, false);
		verify(store, never()).enqueue(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void storageFailureIsRequeued() throws Exception {
		when(store.enqueue(any())).thenThrow(new IllegalStateException("database down"));
		intake.deliver(channel, delivery(10, "TestCloud.sysmonitor.Pump.pump.echo", "{\"sourceTime\":\"2026-10-15T12:00:00Z\",\"value\":{\"status\":\"alive\"}}"));
		verify(channel).basicNack(10, false, true);
		verify(channel, never()).basicAck(anyLong(), anyBoolean());
		assertEquals("Observation persistence failed", intake.getError());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static Delivery delivery(final long tag, final String routingKey, final String body) {
		return new Delivery(new Envelope(tag, false, "arrowhead", routingKey), new AMQP.BasicProperties(), body.getBytes(StandardCharsets.UTF_8));
	}
}
