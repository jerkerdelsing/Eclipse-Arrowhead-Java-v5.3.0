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
package eu.arrowhead.deviceqosevaluator.driver;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;

@Service
public class RttMeasurementDriver {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Long measure(final String address, final int port) throws IOException {
		Long result = null;
		try (Socket socket = new Socket()) {
			final InetSocketAddress socketAddress = new InetSocketAddress(address, port);

			final Instant start = Instant.now();
			try {
				socket.connect(socketAddress, sysInfo.getRttMeasurementTimeout());

				// we need a closed port for measuring => do nothing
			} catch (final ConnectException ex) {
				// port closed
				result = Duration.between(start, Instant.now()).toMillis();

			} catch (final Exception ex) {
				// unreachable
				result = -1L;
			}
		}
		return result;
	}
}