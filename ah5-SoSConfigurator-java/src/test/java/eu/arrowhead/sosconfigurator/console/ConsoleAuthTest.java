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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

public class ConsoleAuthTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void passwordHashMatchesOnlyItsPassword() {
		final ConsolePasswordHash hash = ConsolePasswordHash.parse(ConsolePasswordHash.create("secret password"));
		assertTrue(hash.matches("secret password"));
		assertFalse(hash.matches("secret passwore"));
		assertFalse(hash.matches(""));
		assertNull(ConsolePasswordHash.parse("plain:text"));
		assertNull(ConsolePasswordHash.parse(null));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void repeatedWrongPasswordsLockOnlyThatClient() {
		final AtomicLong now = new AtomicLong(1_000_000);
		final ConsoleAuth auth = new ConsoleAuth(ConsolePasswordHash.create("pw"), "127.0.0.1", 60, now::get);
		for (int i = 0; i < 4; ++i) {
			assertEquals(ConsoleAuth.LoginResult.WRONG, auth.login("10.0.0.9", "x").result());
		}

		assertEquals(ConsoleAuth.LoginResult.LOCKED, auth.login("10.0.0.9", "x").result());
		assertEquals(ConsoleAuth.LoginResult.LOCKED, auth.login("10.0.0.9", "pw").result(), "locked even with the right password");
		assertEquals(ConsoleAuth.LoginResult.OK, auth.login("127.0.0.1", "pw").result(), "another client is not locked out");

		now.addAndGet(61_000);
		assertEquals(ConsoleAuth.LoginResult.OK, auth.login("10.0.0.9", "pw").result());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void sessionsExpireAndEndAtLogout() {
		final AtomicLong now = new AtomicLong(0);
		final ConsoleAuth auth = new ConsoleAuth(ConsolePasswordHash.create("pw"), "127.0.0.1", 60, now::get);
		final String session = auth.login("c", "pw").session();
		assertTrue(auth.valid(session));
		now.set(61_000);
		assertFalse(auth.valid(session));

		final String second = auth.login("c", "pw").session();
		auth.logout(second);
		assertFalse(auth.valid(second));
		assertFalse(auth.valid(null));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void withoutPasswordHashTheConsoleIsClosed() {
		assertEquals(ConsoleAuth.LoginResult.DISABLED, new ConsoleAuth("", "127.0.0.1", 60, () -> 0).login("c", "anything").result());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void onlyLocalHostAndOriginNamesAreAccepted() {
		final ConsoleAuth auth = new ConsoleAuth("", "127.0.0.1,localhost,[::1]", 60, () -> 0);
		assertTrue(auth.hostAllowed("127.0.0.1:8495", null));
		assertTrue(auth.hostAllowed("[::1]:8495", "http://[::1]:8495"));
		assertTrue(auth.hostAllowed("localhost", "http://localhost:8495"));
		assertFalse(auth.hostAllowed("rebound.example:8495", null));
		assertFalse(auth.hostAllowed("127.0.0.1:8495", "http://evil.example"));
		assertFalse(auth.hostAllowed(null, null));
	}
}
