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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Which clients may use the console connector (security finding J6, 2026-11-09).
 * <p>
 * In a container the console listens on every interface, because Docker forwards a published port to an arbitrary one of the
 * container's networks. A connection forwarded from the host's published port arrives from the gateway of that network (the Docker
 * bridge); another container on a shared network arrives from its own address, and no container can hold a gateway address. So the
 * console answers only:
 * <ul>
 * <li>the loopback address,</li>
 * <li>the gateway of one of the container's IPv4 networks, which Docker assigns as the first address of the subnet,</li>
 * <li>the addresses and CIDR ranges of console.allowed.clients (e.g. a reverse proxy, or a network with another gateway).</li>
 * </ul>
 */
@Component
public class ConsoleClients {

	//=================================================================================================
	// members

	/** An IPv4 network of this host: its address and prefix length. */
	record Subnet(byte[] address, int prefix) {
	}

	private static final long REFRESH_MILLIS = 10_000;

	private final List<Subnet> configured;
	private volatile List<Subnet> local = List.of();
	private volatile long localAt;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ConsoleClients(@Value("${console.allowed.clients:}") final String allowedClients) {
		this.configured = parse(allowedClients);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean allowed(final String remoteAddress) {
		final InetAddress remote;
		try {
			remote = InetAddress.getByName(remoteAddress); // a literal address: no name lookup
		} catch (final UnknownHostException ex) {
			return false;
		}

		return allowed(remote, localSubnets(), configured);
	}

	//-------------------------------------------------------------------------------------------------
	static boolean allowed(final InetAddress remote, final List<Subnet> local, final List<Subnet> configured) {
		final InetAddress address = unmapped(remote);
		if (address.isLoopbackAddress()) {
			return true;
		}

		if (!(address instanceof Inet4Address)) {
			return contains(configured, address.getAddress());
		}

		final byte[] bytes = address.getAddress();
		for (final Subnet subnet : local) {
			if (subnet.prefix() < 31 && Arrays.equals(bytes, gateway(subnet))) {
				return true;
			}
		}

		return contains(configured, bytes);
	}

	//-------------------------------------------------------------------------------------------------
	static List<Subnet> parse(final String list) {
		final List<Subnet> result = new ArrayList<>();
		if (list == null) {
			return result;
		}

		for (final String raw : list.split(",")) {
			final String entry = raw.trim();
			if (entry.isEmpty()) {
				continue;
			}

			final int slash = entry.indexOf('/');
			final String host = slash < 0 ? entry : entry.substring(0, slash);
			if (!host.matches("[0-9.]+|[0-9A-Fa-f:]+")) {
				throw new IllegalArgumentException("console.allowed.clients: " + entry + " is not an IP address or CIDR range");
			}

			try {
				final byte[] address = unmapped(InetAddress.getByName(host)).getAddress();
				final int prefix = slash < 0 ? address.length * 8 : Integer.parseInt(entry.substring(slash + 1));
				if (prefix < 0 || prefix > address.length * 8) {
					throw new IllegalArgumentException("console.allowed.clients: invalid prefix in " + entry);
				}

				result.add(new Subnet(address, prefix));
			} catch (final UnknownHostException | NumberFormatException ex) {
				throw new IllegalArgumentException("console.allowed.clients: " + entry + " is not an IP address or CIDR range", ex);
			}
		}

		return List.copyOf(result);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// the container's IPv4 networks, re-read every few seconds (a network may be connected while it runs)
	private List<Subnet> localSubnets() {
		final long now = System.currentTimeMillis();
		if (now - localAt > REFRESH_MILLIS) {
			final List<Subnet> subnets = new ArrayList<>();
			try {
				for (final NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
					for (final InterfaceAddress address : nif.getInterfaceAddresses()) {
						if (address.getAddress() instanceof Inet4Address && !address.getAddress().isLoopbackAddress()) {
							subnets.add(new Subnet(address.getAddress().getAddress(), address.getNetworkPrefixLength()));
						}
					}
				}
			} catch (final SocketException ex) {
				// keep the previous list: fail closed for gateways, configured clients still apply
				return local;
			}

			local = List.copyOf(subnets);
			localAt = now;
		}

		return local;
	}

	//-------------------------------------------------------------------------------------------------
	// first address of the subnet: Docker's default gateway
	private static byte[] gateway(final Subnet subnet) {
		final byte[] result = mask(subnet.address(), subnet.prefix());
		for (int i = result.length - 1; i >= 0; i--) {
			result[i]++;
			if (result[i] != 0) {
				break;
			}
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private static boolean contains(final List<Subnet> subnets, final byte[] address) {
		for (final Subnet subnet : subnets) {
			if (subnet.address().length == address.length
					&& Arrays.equals(mask(subnet.address(), subnet.prefix()), mask(address, subnet.prefix()))) {
				return true;
			}
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	private static byte[] mask(final byte[] address, final int prefix) {
		final byte[] result = address.clone();
		for (int i = 0; i < result.length; i++) {
			final int bits = Math.max(0, Math.min(8, prefix - i * 8));
			result[i] &= (byte) (0xFF << (8 - bits));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	// ::ffff:a.b.c.d as a.b.c.d (Tomcat reports IPv4 clients of a dual-stack socket that way)
	private static InetAddress unmapped(final InetAddress address) {
		if (address instanceof Inet6Address) {
			final byte[] b = address.getAddress();
			boolean mapped = b[10] == (byte) 0xFF && b[11] == (byte) 0xFF;
			for (int i = 0; i < 10 && mapped; i++) {
				mapped = b[i] == 0;
			}

			if (mapped) {
				try {
					return InetAddress.getByAddress(Arrays.copyOfRange(b, 12, 16));
				} catch (final UnknownHostException ex) {
					return address;
				}
			}
		}

		return address;
	}
}
