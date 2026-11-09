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
package eu.arrowhead.deviceqosevaluator.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;

@ExtendWith(MockitoExtension.class)
public class RttMeasurementDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private RttMeasurementDriver driver;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testMeasureUnreachableAddress() throws IOException {
		final InetSocketAddress addressObj = new InetSocketAddress("10.0.0.1", 12345);

		try (MockedConstruction<Socket> mockSocket = Mockito.mockConstruction(
				Socket.class,
				(mock, context) -> {
					doThrow(RuntimeException.class).when(mock).connect(addressObj, 5000);
				})) {

			when(sysInfo.getRttMeasurementTimeout()).thenReturn(5000);

			final long result = driver.measure("10.0.0.1", 12345);

			assertEquals(-1L, result);

			verify(sysInfo).getRttMeasurementTimeout();
			verify(mockSocket.constructed().get(0)).connect(addressObj, 5000);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testMeasureClosedPort() throws IOException {
		final InetSocketAddress addressObj = new InetSocketAddress("10.0.0.1", 12345);
		final Instant now1 = Instant.parse("2026-05-08T10:00:00.000Z");
		final Instant now2 = Instant.parse("2026-05-08T10:00:00.100Z");

		try (MockedStatic<Instant> mockedStatic = Mockito.mockStatic(Instant.class);
				MockedConstruction<Socket> mockSocket = Mockito.mockConstruction(
						Socket.class,
						(mock, context) -> {
							doThrow(ConnectException.class).when(mock).connect(addressObj, 5000);
						})) {

			mockedStatic.when(() -> Instant.now()).thenReturn(now1, now2);
			mockedStatic.when(() -> Instant.from(now2)).thenReturn(now2);
			when(sysInfo.getRttMeasurementTimeout()).thenReturn(5000);

			final long result = driver.measure("10.0.0.1", 12345);

			assertEquals(100L, result);

			mockedStatic.verify(() -> Instant.now(), times(2));
			mockedStatic.verify(() -> Instant.from(now2), atLeastOnce());
			verify(sysInfo).getRttMeasurementTimeout();
			verify(mockSocket.constructed().get(0)).connect(addressObj, 5000);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testMeasureOpenPort() throws IOException {
		final InetSocketAddress addressObj = new InetSocketAddress("10.0.0.1", 12345);
		try (MockedConstruction<Socket> mockSocket = Mockito.mockConstruction(
				Socket.class,
				(mock, context) -> {
					doNothing().when(mock).connect(addressObj, 5000);
				})) {

			when(sysInfo.getRttMeasurementTimeout()).thenReturn(5000);

			final Long result = driver.measure("10.0.0.1", 12345);

			assertNull(result);

			verify(sysInfo).getRttMeasurementTimeout();
			verify(mockSocket.constructed().get(0)).connect(addressObj, 5000);
		}
	}
}