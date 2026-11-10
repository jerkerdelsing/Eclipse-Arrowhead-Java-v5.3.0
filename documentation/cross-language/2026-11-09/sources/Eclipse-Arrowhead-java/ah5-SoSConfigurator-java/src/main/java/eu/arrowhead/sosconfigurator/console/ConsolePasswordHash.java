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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.bouncycastle.crypto.generators.SCrypt;

// "scrypt:<salt base64>:<hash base64>" with N=16384, r=8, p=1: the format of the LocalCloudManager console password hash,
// so both consoles can be provisioned the same way. The process holds only the hash, never the password.
public final class ConsolePasswordHash {

	//=================================================================================================
	// members

	private static final int N = 16384;
	private static final int R = 8;
	private static final int P = 1;
	private static final int SALT_BYTES = 16;
	private static final int HASH_BYTES = 64;

	private final byte[] salt;
	private final byte[] hash;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static ConsolePasswordHash parse(final String value) {
		final String[] parts = value == null ? new String[0] : value.trim().split(":");
		if (parts.length != 3 || !"scrypt".equals(parts[0])) {
			return null;
		}

		try {
			final byte[] salt = Base64.getDecoder().decode(parts[1]);
			final byte[] hash = Base64.getDecoder().decode(parts[2]);
			return salt.length >= SALT_BYTES && hash.length >= 32 ? new ConsolePasswordHash(salt, hash) : null;
		} catch (final IllegalArgumentException ex) {
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static String create(final String password) {
		final byte[] salt = new byte[SALT_BYTES];
		new SecureRandom().nextBytes(salt);
		final byte[] hash = SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), salt, N, R, P, HASH_BYTES);
		return "scrypt:" + Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean matches(final String password) {
		if (password == null || password.isEmpty() || password.length() > 1024) {
			return false;
		}

		final byte[] candidate = SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), salt, N, R, P, hash.length);
		return MessageDigest.isEqual(candidate, hash);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ConsolePasswordHash(final byte[] salt, final byte[] hash) {
		this.salt = salt;
		this.hash = hash;
	}
}
