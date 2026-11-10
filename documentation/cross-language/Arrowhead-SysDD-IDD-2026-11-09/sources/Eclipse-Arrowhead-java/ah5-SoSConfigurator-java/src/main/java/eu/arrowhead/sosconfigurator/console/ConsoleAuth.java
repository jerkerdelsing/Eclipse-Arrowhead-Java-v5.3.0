/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.console;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.sosconfigurator.SoSConfiguratorConstants;

// Operator authentication of the console, after the LocalCloudManager console:
// - every /api request needs a session obtained with the console password (POST /api/session); the process holds only an
//   scrypt hash of the password;
// - the session cookie is HttpOnly and SameSite=Strict;
// - requests whose Host or Origin is not an allowed local name are refused (DNS rebinding);
// - repeated wrong passwords lock the login per client, doubling up to 15 minutes, so another client cannot lock the operator out.
@Component
public class ConsoleAuth {

	//=================================================================================================
	// members

	private static final int MAX_FAILURES = 5;
	private static final long FAILURE_WINDOW_MS = 5 * 60 * 1000L;
	private static final long LOCK_MS = 60 * 1000L;
	private static final long MAX_LOCK_MS = 15 * 60 * 1000L;
	private static final int MAX_CLIENTS = 1024;
	private static final int MAX_SESSIONS = 256;

	private final ConsolePasswordHash credential;
	private final Set<String> allowedHosts;
	private final long sessionMillis;
	private final LongSupplier clock;
	private final SecureRandom random = new SecureRandom();
	private final Map<String, Long> sessions = new ConcurrentHashMap<>();
	private final Map<String, Client> clients = new ConcurrentHashMap<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public ConsoleAuth(
			@Value(SoSConfiguratorConstants.$CONSOLE_PASSWORD_HASH_WD) final String passwordHash,
			@Value(SoSConfiguratorConstants.$CONSOLE_ALLOWED_HOSTS_WD) final String allowedHosts,
			@Value(SoSConfiguratorConstants.$CONSOLE_SESSION_SECONDS_WD) final long sessionSeconds) {
		this(passwordHash, allowedHosts, sessionSeconds, System::currentTimeMillis);
	}

	//-------------------------------------------------------------------------------------------------
	public ConsoleAuth(final String passwordHash, final String allowedHosts, final long sessionSeconds, final LongSupplier clock) {
		this.credential = ConsolePasswordHash.parse(passwordHash);
		this.allowedHosts = java.util.Arrays.stream(allowedHosts.split(","))
				.map(h -> h.trim().toLowerCase(Locale.ROOT))
				.filter(h -> !h.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
		this.sessionMillis = sessionSeconds * 1000L;
		this.clock = clock;
	}

	//-------------------------------------------------------------------------------------------------
	// no usable password hash: the console refuses every login
	public boolean enabled() {
		return credential != null;
	}

	//-------------------------------------------------------------------------------------------------
	// Host (and, when present, Origin) must name an allowed local host
	public boolean hostAllowed(final String host, final String origin) {
		if (!allowedHosts.contains(hostName(host))) {
			return false;
		}

		if (origin == null || origin.isBlank()) {
			return true;
		}

		final int scheme = origin.indexOf("://");
		return scheme > 0 && allowedHosts.contains(hostName(origin.substring(scheme + 3)));
	}

	//-------------------------------------------------------------------------------------------------
	public enum LoginResult { OK, WRONG, LOCKED, DISABLED }

	//-------------------------------------------------------------------------------------------------
	public record Login(LoginResult result, String session) {
	}

	//-------------------------------------------------------------------------------------------------
	public Login login(final String client, final String password) {
		if (!enabled()) {
			return new Login(LoginResult.DISABLED, null);
		}

		final long now = clock.getAsLong();
		final Client state = clientState(client);
		synchronized (state) {
			if (state.lockedUntil > now) {
				return new Login(LoginResult.LOCKED, null);
			}

			if (!credential.matches(password)) {
				state.failures.addLast(now);
				while (!state.failures.isEmpty() && state.failures.peekFirst() < now - FAILURE_WINDOW_MS) {
					state.failures.removeFirst();
				}

				if (state.failures.size() >= MAX_FAILURES) {
					state.locks++;
					state.lockedUntil = now + Math.min(MAX_LOCK_MS, LOCK_MS << Math.min(state.locks - 1, 10));
					state.failures.clear();
					return new Login(LoginResult.LOCKED, null);
				}

				return new Login(LoginResult.WRONG, null);
			}

			state.failures.clear();
		}

		sessions.values().removeIf(expiry -> expiry <= now);
		if (sessions.size() >= MAX_SESSIONS) {
			return new Login(LoginResult.LOCKED, null);
		}

		final byte[] value = new byte[32];
		random.nextBytes(value);
		final String session = HexFormat.of().formatHex(value);
		sessions.put(session, now + sessionMillis);

		return new Login(LoginResult.OK, session);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean valid(final String session) {
		if (session == null) {
			return false;
		}

		final Long expiry = sessions.get(session);
		if (expiry == null) {
			return false;
		}

		if (expiry <= clock.getAsLong()) {
			sessions.remove(session);
			return false;
		}

		return true;
	}

	//-------------------------------------------------------------------------------------------------
	public void logout(final String session) {
		if (session != null) {
			sessions.remove(session);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public long sessionSeconds() {
		return sessionMillis / 1000L;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static final class Client {
		private final Deque<Long> failures = new ArrayDeque<>();
		private long lockedUntil;
		private int locks;
	}

	//-------------------------------------------------------------------------------------------------
	private Client clientState(final String client) {
		if (clients.size() >= MAX_CLIENTS && !clients.containsKey(client)) {
			final long now = clock.getAsLong();
			clients.entrySet().removeIf(e -> e.getValue().lockedUntil <= now && e.getValue().failures.isEmpty());
		}

		return clients.computeIfAbsent(client == null ? "" : client, k -> new Client());
	}

	//-------------------------------------------------------------------------------------------------
	// host name without port; IPv6 literals keep their brackets
	static String hostName(final String value) {
		final String host = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
		if (host.startsWith("[")) {
			final int end = host.indexOf(']');
			return end < 0 ? host : host.substring(0, end + 1);
		}

		final int slash = host.indexOf('/');
		final String authority = slash < 0 ? host : host.substring(0, slash);
		final int colon = authority.indexOf(':');
		return colon < 0 ? authority : authority.substring(0, colon);
	}
}
