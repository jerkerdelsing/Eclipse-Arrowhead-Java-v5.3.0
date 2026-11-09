package eu.arrowhead.localcloudmanager.console;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import org.bouncycastle.crypto.generators.SCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Operator authentication of the LocalCloudManager console (port of the native console-auth.js, security finding H1):
 * <ul>
 * <li>every /api request needs a session obtained with the console password (POST /api/session); the process holds only an scrypt
 * hash ("scrypt:&lt;saltB64&gt;:&lt;hashB64&gt;", N=16384, r=8, p=1);</li>
 * <li>the session cookie is HttpOnly and SameSite=Strict;</li>
 * <li>requests whose Host (or Origin) is not an allowed local name are refused (DNS rebinding);</li>
 * <li>repeated wrong passwords lock the login per client (J6), each further lock doubling up to 15 minutes;</li>
 * <li>with a proxy secret, only requests carrying it (X-Console-Proxy) are served and X-Real-IP is the client address (J6).</li>
 * </ul>
 */
@Component
public class ConsoleAuth {

	public static final String COOKIE = "ah_console";

	private static final int MAX_FAILURES = 5;
	private static final long FAILURE_WINDOW_MS = 5 * 60 * 1000L;
	private static final long LOCK_MS = 60 * 1000L;
	private static final long MAX_LOCK_MS = 15 * 60 * 1000L;
	private static final int MAX_CLIENTS = 1024;

	public record Result(int status, String cookie, Long retryAfter) {
	}

	private record Credential(byte[] salt, byte[] hash) {
	}

	private static final class Client {
		private List<Long> failures = new ArrayList<>();
		private long lockedUntil;
		private int locks;
	}

	private final Credential credential;
	private final Set<String> allowedHosts;
	private final byte[] proxySecret;
	private final long sessionSeconds;
	private final LongSupplier now;
	private final SecureRandom random = new SecureRandom();
	private final Map<String, Long> sessions = new ConcurrentHashMap<>();
	private final Map<String, Client> clients = new LinkedHashMap<>();

	@Autowired
	public ConsoleAuth(@Value(LocalCloudManagerConstants.$CONSOLE_PASSWORD_HASH_WD) final String passwordHash,
			@Value(LocalCloudManagerConstants.$CONSOLE_ALLOWED_HOSTS_WD) final String allowedHosts,
			@Value(LocalCloudManagerConstants.$CONSOLE_PROXY_SECRET_WD) final String proxySecret,
			@Value(LocalCloudManagerConstants.$CONSOLE_SESSION_SECONDS_WD) final long sessionSeconds) {
		this(passwordHash, allowedHosts, proxySecret, sessionSeconds, System::currentTimeMillis);
	}

	public ConsoleAuth(final String passwordHash, final String allowedHosts, final String proxySecret, final long sessionSeconds, final LongSupplier now) {
		this.credential = parsePasswordHash(passwordHash);
		this.allowedHosts = Arrays.stream((allowedHosts == null || allowedHosts.isBlank() ? "127.0.0.1,localhost,[::1]" : allowedHosts).split(","))
				.map(h -> h.trim().toLowerCase(Locale.ROOT)).filter(h -> !h.isEmpty()).collect(Collectors.toUnmodifiableSet());
		this.proxySecret = proxySecret == null || proxySecret.isBlank() ? null : proxySecret.trim().getBytes(StandardCharsets.UTF_8);
		this.sessionSeconds = sessionSeconds;
		this.now = now;
	}

	static Credential parsePasswordHash(final String value) {
		final String[] parts = value == null ? new String[0] : value.trim().split(":");
		if (parts.length != 3 || !"scrypt".equals(parts[0])) {
			return null;
		}

		try {
			final byte[] salt = Base64.getDecoder().decode(parts[1]);
			final byte[] hash = Base64.getDecoder().decode(parts[2]);
			return salt.length >= 16 && hash.length >= 32 ? new Credential(salt, hash) : null;
		} catch (final IllegalArgumentException ex) {
			return null;
		}
	}

	/** A hash in the console format, for provisioning and tests. */
	public static String hash(final String password) {
		final byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		final byte[] hash = SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), salt, 16384, 8, 1, 32);
		return "scrypt:" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
	}

	public boolean configured() {
		return credential != null;
	}

	public long sessionSeconds() {
		return sessionSeconds;
	}

	/** True when the request carries the proxy secret, or no proxy secret is configured. */
	public boolean fromProxy(final HttpServletRequest request) {
		if (proxySecret == null) {
			return true;
		}

		final String given = request.getHeader("X-Console-Proxy");
		return given != null && MessageDigest.isEqual(given.getBytes(StandardCharsets.UTF_8), proxySecret);
	}

	/** The client a failed login is counted against: X-Real-IP behind a configured proxy, else the TCP peer. */
	public String clientOf(final HttpServletRequest request) {
		final String forwarded = proxySecret != null ? request.getHeader("X-Real-IP") : null;
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded;
		}

		return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
	}

	/** Host and Origin must name this console, not a foreign (for example rebound) site; Origin "null" is refused. */
	public boolean originAllowed(final String host, final String origin) {
		if (!allowedHosts.contains(hostName(host))) {
			return false;
		}

		if (origin == null) {
			return true;
		}

		if ("null".equals(origin)) {
			return false;
		}

		try {
			final java.net.URI uri = java.net.URI.create(origin);
			if (uri.getRawAuthority() == null) {
				return false;
			}
			return allowedHosts.contains(hostName(uri.getRawAuthority()));
		} catch (final IllegalArgumentException ex) {
			return false;
		}
	}

	/** Host name without port; IPv6 literals keep their brackets. */
	public static String hostName(final String value) {
		final String host = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
		if (host.startsWith("[")) {
			final int end = host.indexOf(']');
			return end < 0 ? host : host.substring(0, end + 1);
		}

		final int colon = host.indexOf(':');
		return colon < 0 ? host : host.substring(0, colon);
	}

	boolean verifyPassword(final String password) {
		if (credential == null || password == null || password.isEmpty() || password.length() > 256) {
			return false;
		}

		final byte[] candidate = SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), credential.salt(), 16384, 8, 1, credential.hash().length);
		return MessageDigest.isEqual(candidate, credential.hash());
	}

	/** 204 with a cookie on success, 401 on a wrong password, 429 (with Retry-After seconds) while this client is locked. */
	public synchronized Result login(final String password, final String client) {
		final long t = now.getAsLong();
		final Client state = clients.getOrDefault(client, new Client());
		if (t < state.lockedUntil) {
			return new Result(429, null, (long) Math.ceil((state.lockedUntil - t) / 1000.0));
		}

		if (!verifyPassword(password)) {
			final List<Long> recent = new ArrayList<>();
			for (final Long at : state.failures) {
				if (t - at < FAILURE_WINDOW_MS) {
					recent.add(at);
				}
			}
			recent.add(t);
			state.failures = recent;
			if (state.failures.size() >= MAX_FAILURES) {
				state.lockedUntil = t + Math.min(LOCK_MS * (1L << Math.min(state.locks, 20)), MAX_LOCK_MS);
				state.locks += 1;
				state.failures = new ArrayList<>();
			}
			remember(client, state);
			return new Result(401, null, null);
		}

		clients.remove(client);
		final byte[] value = new byte[32];
		random.nextBytes(value);
		final String token = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
		sessions.put(token, t + sessionSeconds * 1000L);
		return new Result(204, COOKIE + "=" + token + "; HttpOnly; SameSite=Strict; Path=/; Max-Age=" + sessionSeconds, null);
	}

	private void remember(final String client, final Client state) {
		clients.remove(client);
		clients.put(client, state);
		// bounded memory: forget the oldest clients that are not locked
		final Iterator<Map.Entry<String, Client>> it = clients.entrySet().iterator();
		while (clients.size() > MAX_CLIENTS && it.hasNext()) {
			if (it.next().getValue().lockedUntil <= now.getAsLong()) {
				it.remove();
			}
		}
	}

	public String sessionToken(final String cookieHeaderOrValue) {
		if (cookieHeaderOrValue == null) {
			return null;
		}

		final Long expires = sessions.get(cookieHeaderOrValue);
		if (expires == null) {
			return null;
		}

		if (expires <= now.getAsLong()) {
			sessions.remove(cookieHeaderOrValue);
			return null;
		}

		return cookieHeaderOrValue;
	}

	public static String cookieValue(final HttpServletRequest request) {
		final jakarta.servlet.http.Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		for (final jakarta.servlet.http.Cookie cookie : cookies) {
			if (COOKIE.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}

		return null;
	}

	public boolean authenticated(final HttpServletRequest request) {
		return sessionToken(cookieValue(request)) != null;
	}

	public String logout(final HttpServletRequest request) {
		final String token = sessionToken(cookieValue(request));
		if (token != null) {
			sessions.remove(token);
		}

		return COOKIE + "=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0";
	}
}
