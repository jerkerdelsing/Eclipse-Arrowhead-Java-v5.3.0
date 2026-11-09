/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.junit.jupiter.api.Test;

public class SntpClientTest {

	@Test
	public void timestampRoundTripKeepsMilliseconds() {
		final byte[] buffer = new byte[48];
		final long epochMs = 1791777123456L;
		SntpClient.writeTimestamp(buffer, 40, epochMs);
		assertEquals(epochMs, SntpClient.readTimestamp(buffer, 40), 1);
	}

	@Test
	public void computesOffsetAndDelayFromFourTimestamps() {
		// server clock 500 ms ahead, 40 ms network delay each way, 10 ms server processing
		final long t1 = 1_000_000L;
		final long t2 = t1 + 40 + 500;
		final long t3 = t2 + 10;
		final long t4 = t1 + 40 + 10 + 40;
		assertEquals(500, SntpClient.offset(t1, t2, t3, t4));
		assertEquals(80, SntpClient.delay(t1, t2, t3, t4));
	}

	@Test
	public void rejectsUnsynchronizedForgedAndWrongModeReplies() {
		final byte[] request = new byte[48];
		SntpClient.writeTimestamp(request, 40, 1000L);
		final byte[] reply = serverReply(request, 2000L, 1);
		reply[1] = 0; // stratum 0: kiss-o'-death
		assertThrows(IOException.class, () -> SntpClient.evaluate("s", request, reply, 1000L, 1100L));
		final byte[] forged = serverReply(request, 2000L, 1);
		forged[24] ^= 0x01; // originate does not echo our transmit timestamp
		assertThrows(IOException.class, () -> SntpClient.evaluate("s", request, forged, 1000L, 1100L));
		final byte[] clientMode = serverReply(request, 2000L, 1);
		clientMode[0] = 0x23;
		assertThrows(IOException.class, () -> SntpClient.evaluate("s", request, clientMode, 1000L, 1100L));
	}

	@Test
	public void queriesLocalNtpServerOverUdp() throws Exception {
		try (DatagramSocket server = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
			final Thread responder = new Thread(() -> {
				try {
					final byte[] buffer = new byte[48];
					final DatagramPacket request = new DatagramPacket(buffer, 48);
					server.receive(request);
					final byte[] reply = serverReply(buffer, System.currentTimeMillis() + 3000, 2);
					server.send(new DatagramPacket(reply, 48, request.getSocketAddress()));
				} catch (final IOException ignored) {
					// test fails on timeout
				}
			});
			responder.start();
			final SntpClient.Measurement measurement = new SntpClient().query("127.0.0.1", server.getLocalPort(), 2000);
			responder.join(2000);
			assertTrue(Math.abs(measurement.offsetMs() - 3000) < 200, "offset " + measurement.offsetMs());
			assertEquals(2, measurement.stratum());
		}
	}

	// A server-mode reply that echoes the request transmit timestamp and stamps receive/transmit with serverTimeMs.
	static byte[] serverReply(final byte[] request, final long serverTimeMs, final int stratum) {
		final byte[] reply = new byte[48];
		reply[0] = 0x24; // version 4, mode 4 (server)
		reply[1] = (byte) stratum;
		System.arraycopy(request, 40, reply, 24, 8);
		SntpClient.writeTimestamp(reply, 32, serverTimeMs);
		SntpClient.writeTimestamp(reply, 40, serverTimeMs);
		return reply;
	}
}
