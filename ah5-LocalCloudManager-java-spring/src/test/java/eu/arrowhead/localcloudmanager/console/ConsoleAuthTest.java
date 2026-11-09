package eu.arrowhead.localcloudmanager.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import jakarta.servlet.http.Cookie;

// Port of the native console-auth.test.js.
public class ConsoleAuthTest {

	private static final String HASH = ConsoleAuth.hash("correct horse");

	private static MockHttpServletRequest withCookie(final String setCookie) {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		final String pair = setCookie.split(";")[0];
		request.setCookies(new Cookie(pair.substring(0, pair.indexOf('=')), pair.substring(pair.indexOf('=') + 1)));
		return request;
	}

	@Test
	public void hostNamesAreComparedWithoutPort() {
		assertEquals("127.0.0.1", ConsoleAuth.hostName("127.0.0.1:8490"));
		assertEquals("[::1]", ConsoleAuth.hostName("[::1]:8490"));
	}

	@Test
	public void foreignHostOrOriginIsRefused() {
		final ConsoleAuth auth = new ConsoleAuth(HASH, null, null, 28800, System::currentTimeMillis);
		assertTrue(auth.originAllowed("127.0.0.1:8490", null));
		assertTrue(auth.originAllowed("localhost", "http://localhost:8490"));
		assertFalse(auth.originAllowed("attacker.example:8490", null));
		assertFalse(auth.originAllowed("127.0.0.1", "http://attacker.example"));
		assertFalse(auth.originAllowed("127.0.0.1", "null"));
	}

	@Test
	public void loginIssuesAStrictSessionCookieAndWrongPasswordsLock() {
		final AtomicLong now = new AtomicLong(1_000_000);
		final ConsoleAuth auth = new ConsoleAuth(HASH, null, null, 8 * 3600, now::get);
		assertEquals(401, auth.login("wrong", "c").status());
		final ConsoleAuth.Result ok = auth.login("correct horse", "c");
		assertEquals(204, ok.status());
		assertTrue(ok.cookie().contains("HttpOnly; SameSite=Strict"));
		assertTrue(auth.authenticated(withCookie(ok.cookie())));
		for (int i = 0; i < 5; i++) {
			auth.login("wrong", "c");
		}
		final ConsoleAuth.Result locked = auth.login("correct horse", "c");
		assertEquals(429, locked.status());
		assertTrue(locked.retryAfter() > 0);
		now.addAndGet(61_000);
		assertEquals(204, auth.login("correct horse", "c").status());
		now.addAndGet(9 * 3600 * 1000L);
		assertFalse(auth.authenticated(withCookie(ok.cookie())));
	}

	@Test
	public void unconfiguredConsoleRefusesEveryPassword() {
		final ConsoleAuth auth = new ConsoleAuth("", null, null, 28800, System::currentTimeMillis);
		assertFalse(auth.configured());
		assertEquals(401, auth.login("", "c").status());
	}

	@Test
	public void aLockoutIsPerClientAndEachFurtherLockDoubles() {
		final AtomicLong now = new AtomicLong(2_000_000);
		final ConsoleAuth auth = new ConsoleAuth(HASH, null, null, 28800, now::get);
		for (int i = 0; i < 5; i++) {
			auth.login("wrong", "172.19.0.30");
		}
		assertEquals(429, auth.login("correct horse", "172.19.0.30").status());
		assertEquals(204, auth.login("correct horse", "192.168.65.1").status());
		now.addAndGet(61_000);
		for (int i = 0; i < 5; i++) {
			auth.login("wrong", "172.19.0.30");
		}
		now.addAndGet(61_000);
		assertEquals(429, auth.login("correct horse", "172.19.0.30").status());
		now.addAndGet(60_000);
		assertEquals(204, auth.login("correct horse", "172.19.0.30").status());
	}

	@Test
	public void withAProxySecretOnlyForwardedRequestsAreServed() {
		final ConsoleAuth auth = new ConsoleAuth(HASH, null, "s3cret-value", 28800, System::currentTimeMillis);
		final MockHttpServletRequest plain = new MockHttpServletRequest();
		assertFalse(auth.fromProxy(plain));
		plain.addHeader("X-Console-Proxy", "wrong");
		assertFalse(auth.fromProxy(plain));
		final MockHttpServletRequest forwarded = new MockHttpServletRequest();
		forwarded.addHeader("X-Console-Proxy", "s3cret-value");
		forwarded.addHeader("X-Real-IP", "192.168.65.1");
		forwarded.setRemoteAddr("172.19.0.5");
		assertTrue(auth.fromProxy(forwarded));
		assertEquals("192.168.65.1", auth.clientOf(forwarded));
		final ConsoleAuth open = new ConsoleAuth(HASH, null, null, 28800, System::currentTimeMillis);
		assertTrue(open.fromProxy(plain));
		// without a proxy secret X-Real-IP is not trusted
		assertEquals("172.19.0.5", open.clientOf(forwarded));
	}

	@Test
	public void theNativeNodeHashFormatIsAccepted() throws Exception {
		// crypto.scryptSync('correct horse', salt, 32) with the default N=16384, r=8, p=1, as provisioned for the Node console
		final byte[] salt = new byte[16];
		final byte[] hash = org.bouncycastle.crypto.generators.SCrypt.generate("correct horse".getBytes(java.nio.charset.StandardCharsets.UTF_8), salt, 16384, 8, 1, 32);
		final String value = "scrypt:" + java.util.Base64.getEncoder().encodeToString(salt) + ":" + java.util.Base64.getEncoder().encodeToString(hash);
		assertEquals(204, new ConsoleAuth(value, null, null, 60, System::currentTimeMillis).login("correct horse", "x").status());
	}
}
