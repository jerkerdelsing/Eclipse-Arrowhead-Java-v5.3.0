/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.localcloudmanager.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import eu.arrowhead.localcloudmanager.console.ConsoleClients.Subnet;

// J6: the console answers the host's published port (Docker gateway), loopback and configured clients, not other containers.
public class ConsoleClientsTest {

	// the container: 172.18.0.20/16 on the cloud network, 10.253.100.2/28 on another network
	private static final List<Subnet> LOCAL = List.of(subnet("172.18.0.20", 16), subnet("10.253.100.2", 28));

	private static Subnet subnet(final String address, final int prefix) {
		try {
			return new Subnet(InetAddress.getByName(address).getAddress(), prefix);
		} catch (final IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static boolean allowed(final String remote, final List<Subnet> configured) throws IOException {
		return ConsoleClients.allowed(InetAddress.getByName(remote), LOCAL, configured);
	}

	@Test
	public void gatewaysOfTheContainersNetworksAndLoopbackAreAllowed() throws IOException {
		assertTrue(allowed("172.18.0.1", List.of()));
		assertTrue(allowed("10.253.100.1", List.of()));
		assertTrue(allowed("::ffff:172.18.0.1", List.of()));
		assertTrue(allowed("127.0.0.1", List.of()));
		assertTrue(allowed("::1", List.of()));
	}

	@Test
	public void otherContainersAreRefused() throws IOException {
		assertFalse(allowed("172.18.0.29", List.of())); // another system on the cloud network
		assertFalse(allowed("::ffff:172.18.0.29", List.of()));
		assertFalse(allowed("10.253.100.3", List.of()));
		assertFalse(allowed("172.18.0.20", List.of())); // its own address is no gateway
		assertFalse(allowed("192.168.1.1", List.of())); // gateway of a network the container is not on
		assertFalse(allowed("fe80::1", List.of()));
	}

	@Test
	public void configuredClientsAreAllowed() throws IOException {
		final List<Subnet> configured = ConsoleClients.parse(" 172.18.0.40 , 192.168.7.0/24,fd00::/8 ");
		assertTrue(allowed("172.18.0.40", configured));
		assertFalse(allowed("172.18.0.41", configured));
		assertTrue(allowed("192.168.7.200", configured));
		assertFalse(allowed("192.168.8.1", configured));
		assertTrue(allowed("fd00::5", configured));
	}

	@Test
	public void invalidConfiguredClientsStopTheStart() {
		assertThrows(IllegalArgumentException.class, () -> ConsoleClients.parse("proxy.example"));
		assertThrows(IllegalArgumentException.class, () -> ConsoleClients.parse("10.0.0.0/33"));
		assertThrows(IllegalArgumentException.class, () -> ConsoleClients.parse("10.0.0.0/x"));
		assertEquals(0, ConsoleClients.parse("").size());
	}

	@Test
	public void deploymentConfigurationAllowsConfiguredClients() throws IOException {
		final Properties config = new Properties();
		try (FileInputStream in = new FileInputStream("config/application.properties")) {
			config.load(in);
		}
		assertEquals("${CONSOLE_ALLOWED_CLIENTS:}", config.getProperty("console.allowed.clients"));
	}
}
