/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.authorization.service.engine;

import java.security.PrivateKey;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.service.dto.SelfContainedTokenPayload;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;

@Service
public class TokenGenerator {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationSystemInfo sysInfo;

	private final SecureRandom secureRandom = new SecureRandom();

	private final Encoder base64Encoder = Base64.getUrlEncoder();

	private static final String BASE64_SELF_CONTAINED_TOKEN_DELIMITER = Constants.COMPOSITE_ID_DELIMITER;

	private static final String JWT_HEADER_KEY_TOKEN_TYPE = "typ";
	private static final String JWT_HEADER_VALUE_TOKEN_TYPE = "JWT";
	private static final String JWT_PAYLOAD_KEY_PROVIDER_SYSTEM_NAME = "psn";
	private static final String JWT_PAYLOAD_KEY_CONSUMER_SYSTEM_NAME = "csn";
	private static final String JWT_PAYLOAD_KEY_CONSUMER_CLOUD_NAME = "ccn";
	private static final String JWT_PAYLOAD_KEY_TARGET_TYPE = "tat";
	private static final String JWT_PAYLOAD_KEY_TARGET_NAME = "tan";
	private static final String JWT_PAYLOAD_KEY_SCOPE = "sco";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	public String generateSimpleToken(final int byteLength) {
		Assert.isTrue(byteLength >= 16, "Minimum length for token is 16 bytes");

		final byte[] randomBytes = new byte[byteLength];
		secureRandom.nextBytes(randomBytes);

		return base64Encoder.encodeToString(randomBytes);
	}

	//-------------------------------------------------------------------------------------------------
	public String generateBas64SelfContainedToken(final ZonedDateTime expiry, final SelfContainedTokenPayload payload) {
		Assert.notNull(payload, "payload is null");
		Assert.isTrue(!Utilities.isEmpty(payload.provider()), "provider is empty");
		Assert.isTrue(!Utilities.isEmpty(payload.consumer()), "consumer is empty");
		Assert.isTrue(!Utilities.isEmpty(payload.cloud()), "cloud is empty");
		Assert.notNull(payload.targetType(), "targetType is null");
		Assert.isTrue(!Utilities.isEmpty(payload.target()), "target is empty");

		final String content = payload.cloud() + BASE64_SELF_CONTAINED_TOKEN_DELIMITER
				+ payload.consumer() + BASE64_SELF_CONTAINED_TOKEN_DELIMITER
				+ payload.provider() + BASE64_SELF_CONTAINED_TOKEN_DELIMITER
				+ payload.target() + BASE64_SELF_CONTAINED_TOKEN_DELIMITER
				+ (Utilities.isEmpty(payload.scope()) ? "" : payload.scope()) + BASE64_SELF_CONTAINED_TOKEN_DELIMITER
				+ payload.targetType().name() + BASE64_SELF_CONTAINED_TOKEN_DELIMITER
				+ (expiry == null ? "" : Utilities.convertZonedDateTimeToUTCString(expiry));

		return base64Encoder.encodeToString(content.getBytes());
	}

	//-------------------------------------------------------------------------------------------------
	public String generateJsonWebToken(final String signAlgorithm, final PrivateKey privateKey, final ZonedDateTime expiry, final SelfContainedTokenPayload payload) throws JoseException {
		Assert.isTrue(!Utilities.isEmpty(signAlgorithm), "signAlgorithm is empty");
		Assert.notNull(privateKey, "privateKey is null");
		Assert.notNull(payload, "JWT payload is null");
		Assert.isTrue(!Utilities.isEmpty(payload.provider()), "provider is empty");
		Assert.isTrue(!Utilities.isEmpty(payload.consumer()), "consumer is empty");
		Assert.isTrue(!Utilities.isEmpty(payload.cloud()), "cloud is empty");
		Assert.notNull(payload.targetType(), "targetType is null");
		Assert.isTrue(!Utilities.isEmpty(payload.target()), "target is empty");

		final JwtClaims claims = createJWTClaims(expiry, payload);
		final String jwt = signJWT(signAlgorithm, privateKey, claims);

		return jwt;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private JwtClaims createJWTClaims(final ZonedDateTime expiry, final SelfContainedTokenPayload payload) {
		final JwtClaims claims = new JwtClaims();
		claims.setGeneratedJwtId();
		claims.setIssuer(sysInfo.getSystemName());
		claims.setIssuedAtToNow();
		claims.setNotBeforeMinutesInThePast(1);
		if (expiry != null) {
			claims.setExpirationTime(NumericDate.fromSeconds(expiry.toInstant().getEpochSecond()));
		}
		claims.setStringClaim(JWT_PAYLOAD_KEY_PROVIDER_SYSTEM_NAME, payload.provider());
		claims.setStringClaim(JWT_PAYLOAD_KEY_CONSUMER_SYSTEM_NAME, payload.consumer());
		claims.setStringClaim(JWT_PAYLOAD_KEY_CONSUMER_CLOUD_NAME, payload.cloud());
		claims.setStringClaim(JWT_PAYLOAD_KEY_TARGET_TYPE, payload.targetType().name());
		claims.setStringClaim(JWT_PAYLOAD_KEY_TARGET_NAME, payload.target());

		if (!Utilities.isEmpty(payload.scope())) {
			claims.setStringClaim(JWT_PAYLOAD_KEY_SCOPE, payload.scope());
		}

		return claims;
	}

	//-------------------------------------------------------------------------------------------------
	private String signJWT(final String signAlgorithm, final PrivateKey privateKey, final JwtClaims claims) throws JoseException {
		final JsonWebSignature jws = new JsonWebSignature();
		jws.setHeader(JWT_HEADER_KEY_TOKEN_TYPE, JWT_HEADER_VALUE_TOKEN_TYPE);
		jws.setPayload(claims.toJson());
		jws.setKey(privateKey);

		if (signAlgorithm.equalsIgnoreCase(AlgorithmIdentifiers.RSA_USING_SHA512)
				|| signAlgorithm.equalsIgnoreCase(AlgorithmIdentifiers.RSA_USING_SHA256)) {
			jws.setAlgorithmHeaderValue(signAlgorithm);
		} else {
			throw new JoseException("Unsupported sign algorithm: " + signAlgorithm);
		}

		return jws.getCompactSerialization();
	}
}