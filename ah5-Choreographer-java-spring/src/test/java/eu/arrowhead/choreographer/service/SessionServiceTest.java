/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SessionServiceTest {

	@Test
	public void notifyUriIsBuiltFromParts() {
		final List<String> errors = new ArrayList<>();
		assertEquals("https://10.0.0.5:9000/cb", SessionService.notifyUri("HTTPS", "10.0.0.5", 9000, "cb", errors));
		assertEquals("http://host:80", SessionService.notifyUri("http", "host", 80, null, errors));
		assertTrue(errors.isEmpty());
	}

	@Test
	public void noAddressMeansNoNotification() {
		final List<String> errors = new ArrayList<>();
		assertNull(SessionService.notifyUri(null, null, null, null, errors));
		assertTrue(errors.isEmpty());
	}

	@Test
	public void invalidProtocolAndPortAreReported() {
		final List<String> errors = new ArrayList<>();
		assertNull(SessionService.notifyUri("ftp", "host", 70000, "/x", errors));
		assertEquals(2, errors.size());
	}
}
