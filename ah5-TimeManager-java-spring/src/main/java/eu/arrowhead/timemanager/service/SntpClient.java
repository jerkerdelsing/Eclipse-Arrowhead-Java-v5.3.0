/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

import org.springframework.stereotype.Component;

// Minimal SNTP client (RFC 4330 / RFC 5905 client mode); replaces the Apache commons-net client of v4.6.
@Component
public class SntpClient {

	public static final int NTP_PORT = 123;
	static final long NTP_EPOCH_OFFSET_SECONDS = 2208988800L; // 1900-01-01 to 1970-01-01
	private static final int PACKET_SIZE = 48;
	private static final int ORIGINATE_OFFSET = 24;
	private static final int RECEIVE_OFFSET = 32;
	private static final int TRANSMIT_OFFSET = 40;

	public record Measurement(String server, long offsetMs, long delayMs, int stratum) {
	}

	public Measurement query(final String host, final int port, final int timeoutMs) throws IOException {
		final byte[] request = new byte[PACKET_SIZE];
		request[0] = 0x23; // LI 0, version 4, mode 3 (client)
		final long t1 = System.currentTimeMillis();
		writeTimestamp(request, TRANSMIT_OFFSET, t1);

		final byte[] response = new byte[PACKET_SIZE];
		final long t4;
		try (DatagramSocket socket = new DatagramSocket()) {
			socket.setSoTimeout(timeoutMs);
			final InetAddress address = InetAddress.getByName(host);
			socket.send(new DatagramPacket(request, PACKET_SIZE, address, port));
			final DatagramPacket packet = new DatagramPacket(response, PACKET_SIZE);
			socket.receive(packet);
			t4 = System.currentTimeMillis();
			if (packet.getLength() < PACKET_SIZE) {
				throw new IOException("Short NTP response from " + host);
			}
		}

		return evaluate(host, request, response, t1, t4);
	}

	static Measurement evaluate(final String host, final byte[] request, final byte[] response, final long t1, final long t4) throws IOException {
		final int mode = response[0] & 0x07;
		if (mode != 4 && mode != 5) {
			throw new IOException("Unexpected NTP mode " + mode + " from " + host);
		}
		final int stratum = response[1] & 0xFF;
		if (stratum == 0 || stratum > 15) {
			throw new IOException("Unsynchronized or kiss-o'-death NTP response (stratum " + stratum + ") from " + host);
		}
		// The server echoes our transmit timestamp as originate timestamp; a mismatch indicates a stale or forged reply.
		if (!Arrays.equals(response, ORIGINATE_OFFSET, ORIGINATE_OFFSET + 8, request, TRANSMIT_OFFSET, TRANSMIT_OFFSET + 8)) {
			throw new IOException("NTP originate timestamp mismatch from " + host);
		}
		final long t2 = readTimestamp(response, RECEIVE_OFFSET);
		final long t3 = readTimestamp(response, TRANSMIT_OFFSET);

		return new Measurement(host, offset(t1, t2, t3, t4), delay(t1, t2, t3, t4), stratum);
	}

	static long offset(final long t1, final long t2, final long t3, final long t4) {
		return ((t2 - t1) + (t3 - t4)) / 2;
	}

	static long delay(final long t1, final long t2, final long t3, final long t4) {
		return (t4 - t1) - (t3 - t2);
	}

	static void writeTimestamp(final byte[] buffer, final int offset, final long epochMs) {
		final long seconds = epochMs / 1000 + NTP_EPOCH_OFFSET_SECONDS;
		final long fraction = (epochMs % 1000) * 0x100000000L / 1000;
		for (int i = 0; i < 4; i++) {
			buffer[offset + i] = (byte) (seconds >>> (24 - 8 * i));
			buffer[offset + 4 + i] = (byte) (fraction >>> (24 - 8 * i));
		}
	}

	static long readTimestamp(final byte[] buffer, final int offset) {
		long seconds = 0;
		long fraction = 0;
		for (int i = 0; i < 4; i++) {
			seconds = (seconds << 8) | (buffer[offset + i] & 0xFF);
			fraction = (fraction << 8) | (buffer[offset + 4 + i] & 0xFF);
		}
		return (seconds - NTP_EPOCH_OFFSET_SECONDS) * 1000 + fraction * 1000 / 0x100000000L;
	}
}
