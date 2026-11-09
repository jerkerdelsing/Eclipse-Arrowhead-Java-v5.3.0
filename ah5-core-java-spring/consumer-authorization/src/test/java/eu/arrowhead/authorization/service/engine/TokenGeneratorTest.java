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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;

import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.service.dto.SelfContainedTokenPayload;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@ExtendWith(MockitoExtension.class)
public class TokenGeneratorTest {

	//=================================================================================================
	// members

	@SuppressWarnings("checkstyle:nowhitespaceafter")
	private static final byte[] TEST_RANDOM_BYTES = new byte[] { 20, -72, -62, -39, -28, 123, -35, 115, -87, 68, -82, -49, -61, 105, 31, 31 };

	@InjectMocks
	private TokenGenerator generator;

	@Mock
	private AuthorizationSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGenerateSimpleTokenShortToken() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateSimpleToken(10));

		assertEquals("Minimum length for token is 16 bytes", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGenerateSimpleTokenOk() {
		final SecureRandom mockSc = Mockito.mock(SecureRandom.class);
		doAnswer(invocation -> {
			final byte[] array = (byte[]) invocation.getArgument(0);
			System.arraycopy(TEST_RANDOM_BYTES, 0, array, 0, TEST_RANDOM_BYTES.length);
			return null;
		}).when(mockSc).nextBytes(any(byte[].class));
		final SecureRandom original = (SecureRandom) ReflectionTestUtils.getField(generator, "secureRandom");
		ReflectionTestUtils.setField(generator, "secureRandom", mockSc);

		assertArrayEquals(TEST_RANDOM_BYTES, Base64.getUrlDecoder().decode(generator.generateSimpleToken(16)));
		verify(mockSc).nextBytes(any(byte[].class));

		ReflectionTestUtils.setField(generator, "secureRandom", original);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenPayloadNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, null));

		assertEquals("payload is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenProviderNull() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				null,
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, payload));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenProviderEmpty() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				" ",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, payload));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenConsumerNull() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				null,
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, payload));

		assertEquals("consumer is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenConsumerEmpty() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, payload));

		assertEquals("consumer is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenCloudNull() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				null,
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, payload));

		assertEquals("cloud is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenCloudEmpty() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"  ",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, payload));

		assertEquals("cloud is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenTargetTypeNull() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				null,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, payload));

		assertEquals("targetType is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenTargetNull() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				null,
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, payload));

		assertEquals("target is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenTargetEmpty() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				" ",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateBas64SelfContainedToken(null, payload));

		assertEquals("target is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenok1() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		final ZonedDateTime expiry = ZonedDateTime.of(2025, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC));

		final String expectedContent = "LOCAL|Consumer|Provider|serviceDef|operation|SERVICE_DEF|2025-10-14T10:12:12Z";

		final String token = generator.generateBas64SelfContainedToken(expiry, payload);

		assertEquals(expectedContent, new String(Base64.getUrlDecoder().decode(token)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateBas64SelfContainedTokenok2() {
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);
		final String expectedContent = "LOCAL|Consumer|Provider|serviceDef||SERVICE_DEF|";

		final String token = generator.generateBas64SelfContainedToken(null, payload);

		assertEquals(expectedContent, new String(Base64.getUrlDecoder().decode(token)));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenSignAlgorithmNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken(null, null, null, null));

		assertEquals("signAlgorithm is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenSignAlgorithmEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("", null, null, null));

		assertEquals("signAlgorithm is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenPrivateKeyNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", null, null, null));

		assertEquals("privateKey is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenPayloadNull() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, null));

		assertEquals("JWT payload is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenProviderNull() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				null,
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, payload));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenProviderEmpty() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, payload));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenConsumerNull() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				null,
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, payload));

		assertEquals("consumer is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenConsumerEmpty() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				" ",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, payload));

		assertEquals("consumer is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenCloudNull() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				null,
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, payload));

		assertEquals("cloud is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenCloudEmpty() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, payload));

		assertEquals("cloud is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenTargetTypeNull() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				null,
				"serviceDef",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, payload));

		assertEquals("targetType is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenTargetNull() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				null,
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, payload));

		assertEquals("target is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenTargetEmpty() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"",
				"operation");

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> generator.generateJsonWebToken("algorithm", privateKey, null, payload));

		assertEquals("target is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenUnsupportedAlgorithm() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");

		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");

		final Throwable ex = assertThrows(
				JoseException.class,
				() -> generator.generateJsonWebToken("invalid", privateKey, null, payload));

		assertEquals("Unsupported sign algorithm: invalid", ex.getMessage());

		verify(sysInfo).getSystemName();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenOk1() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, JoseException, InvalidJwtException, MalformedClaimException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");
		final PublicKey publicKey = SecurityUtilities.getCertificateFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu").getPublicKey();

		final ZonedDateTime expiry = ZonedDateTime.of(2125, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC)); // this test only works for 100 years, sorry future developers :)
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");

		final String jwt = generator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA512, privateKey, expiry, payload);

		final AlgorithmConstraints algConstraint = new AlgorithmConstraints(ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA512);
		final JwtConsumer jwtConsumer = new JwtConsumerBuilder()
				.setRequireJwtId()
				.setExpectedIssuer("ConsumerAuthorization")
				.setVerificationKey(publicKey)
				.setJwsAlgorithmConstraints(algConstraint)
				.build();

		final JwtClaims claims = jwtConsumer.processToClaims(jwt);

		verify(sysInfo).getSystemName();
		assertEquals("Provider", claims.getStringClaimValue("psn"));
		assertEquals("Consumer", claims.getStringClaimValue("csn"));
		assertEquals("LOCAL", claims.getStringClaimValue("ccn"));
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), claims.getStringClaimValue("tat"));
		assertEquals("serviceDef", claims.getStringClaimValue("tan"));
		assertEquals("operation", claims.getStringClaimValue("sco"));
		assertEquals(expiry.toEpochSecond(), claims.getExpirationTime().getValue());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateJsonWebTokenOk2() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, JoseException, InvalidJwtException, MalformedClaimException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");
		final PublicKey publicKey = SecurityUtilities.getCertificateFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu").getPublicKey();

		final ZonedDateTime expiry = ZonedDateTime.of(2125, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC)); // this test only works for 100 years, sorry future developers :)
		final SelfContainedTokenPayload payload = new SelfContainedTokenPayload(
				"Provider",
				"Consumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");

		final String jwt = generator.generateJsonWebToken(AlgorithmIdentifiers.RSA_USING_SHA256, privateKey, expiry, payload);

		final AlgorithmConstraints algConstraint = new AlgorithmConstraints(ConstraintType.PERMIT, AlgorithmIdentifiers.RSA_USING_SHA256);
		final JwtConsumer jwtConsumer = new JwtConsumerBuilder()
				.setRequireJwtId()
				.setExpectedIssuer("ConsumerAuthorization")
				.setVerificationKey(publicKey)
				.setJwsAlgorithmConstraints(algConstraint)
				.build();

		final JwtClaims claims = jwtConsumer.processToClaims(jwt);

		verify(sysInfo).getSystemName();
		assertEquals("Provider", claims.getStringClaimValue("psn"));
		assertEquals("Consumer", claims.getStringClaimValue("csn"));
		assertEquals("LOCAL", claims.getStringClaimValue("ccn"));
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), claims.getStringClaimValue("tat"));
		assertEquals("serviceDef", claims.getStringClaimValue("tan"));
		assertEquals("operation", claims.getStringClaimValue("sco"));
		assertEquals(expiry.toEpochSecond(), claims.getExpirationTime().getValue());
	}
}