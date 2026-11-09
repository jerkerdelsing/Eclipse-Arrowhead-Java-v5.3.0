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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jose4j.lang.JoseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.CryptographerAuxiliary;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.entity.SelfContainedToken;
import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.jpa.service.SelfContainedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TimeLimitedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TokenHeaderDbService;
import eu.arrowhead.authorization.jpa.service.UsageLimitedTokenDbService;
import eu.arrowhead.authorization.service.dto.SelfContainedTokenPayload;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class TokenEngineTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TokenEngine engine;

	@Mock
	private AuthorizationSystemInfo sysInfo;

	@Mock
	private TokenGenerator tokenGenerator;

	@Mock
	private SecretCryptographer secretCryptographer;

	@Mock
	private EncryptionKeyDbService encryptionKeyDbService;

	@Mock
	private TokenHeaderDbService tokenHeaderDbService;

	@Mock
	private UsageLimitedTokenDbService usageLimitedTokenDbService;

	@Mock
	private TimeLimitedTokenDbService timeLimitedTokenDbService;

	@Mock
	private SelfContainedTokenDbService selfContainedTokenDbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce8UsageOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final Pair<UsageLimitedToken, Boolean> dbResult = Pair.of(
				new UsageLimitedToken(
						new TokenHeader(
								AuthorizationTokenType.USAGE_LIMITED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						7),
				true);

		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
		when(sysInfo.getSimpleTokenUsageLimit()).thenReturn(7);
		when(usageLimitedTokenDbService.save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7)).thenReturn(dbResult);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"testOrigin");

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
		verify(tokenHeaderDbService).find("hashedToken");
		verify(sysInfo).getSimpleTokenUsageLimit();
		verify(usageLimitedTokenDbService).save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.USAGE_LIMITED_TOKEN, result.getTokenType());
		assertEquals("0123456789abcdef", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("USAGE_LIMITED_TOKEN_AUTH", result.getVariant());
		assertEquals(7, result.getUsageLimit());
		assertEquals(7, result.getUsageLeft());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce8TimeOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 8, 6, 12, 0, 10, 0, ZoneId.of("UTC"));
		final Pair<TimeLimitedToken, Boolean> dbResult = Pair.of(
				new TimeLimitedToken(
						new TokenHeader(
								AuthorizationTokenType.TIME_LIMITED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						expiresAt),
				true);

		try (MockedStatic<Utilities> utilMock = Mockito.mockStatic(Utilities.class)) {
			utilMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);
			when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
			when(tokenGenerator.generateSimpleToken(16)).thenReturn("aaaaaaaaaaaaaaaa", "0123456789abcdef");
			when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
			when(secretCryptographer.encrypt_HMAC_SHA256("aaaaaaaaaaaaaaaa", "aVerySecretKey")).thenReturn("existingToken");
			when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
			when(tokenHeaderDbService.find("existingToken")).thenReturn(Optional.of(new TokenHeader()));
			when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
			utilMock.when(() -> Utilities.utcNow()).thenReturn(ZonedDateTime.of(2025, 8, 6, 12, 0, 0, 0, ZoneId.of("UTC")));
			when(sysInfo.getTokenTimeLimit()).thenReturn(10);
			when(timeLimitedTokenDbService.save(
					AuthorizationTokenType.TIME_LIMITED_TOKEN,
					"hashedToken",
					"ConsumerName",
					"LOCAL",
					"ConsumerName",
					"ProviderName",
					AuthorizationTargetType.SERVICE_DEF,
					"testService",
					"op",
					expiresAt)).thenReturn(dbResult);

			final TokenModel result = engine.produce(
					"ConsumerName",
					"ConsumerName",
					ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH,
					"ProviderName",
					AuthorizationTargetType.SERVICE_DEF,
					"testService",
					"op",
					"testOrigin");

			utilMock.verify(() -> Utilities.isEmpty((String) null));
			verify(sysInfo, times(2)).getSimpleTokenByteSize();
			verify(tokenGenerator, times(2)).generateSimpleToken(16);
			verify(sysInfo, times(2)).getSecretCryptographerKey();
			verify(secretCryptographer).encrypt_HMAC_SHA256("aaaaaaaaaaaaaaaa", "aVerySecretKey");
			verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
			verify(tokenHeaderDbService).find("existingToken");
			verify(tokenHeaderDbService).find("hashedToken");
			utilMock.verify(() -> Utilities.utcNow());
			verify(sysInfo).getTokenTimeLimit();
			verify(timeLimitedTokenDbService).save(
					AuthorizationTokenType.TIME_LIMITED_TOKEN,
					"hashedToken",
					"ConsumerName",
					"LOCAL",
					"ConsumerName",
					"ProviderName",
					AuthorizationTargetType.SERVICE_DEF,
					"testService",
					"op",
					expiresAt);

			assertFalse(result.isEncrypted());
			assertEquals(AuthorizationTokenType.TIME_LIMITED_TOKEN, result.getTokenType());
			assertEquals("0123456789abcdef", result.getRawToken());
			assertEquals("hashedToken", result.getHashedToken());
			assertEquals("ConsumerName", result.getRequester());
			assertEquals("LOCAL", result.getConsumerCloud());
			assertEquals("ConsumerName", result.getConsumer());
			assertEquals("ProviderName", result.getProvider());
			assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
			assertEquals("testService", result.getTarget());
			assertEquals("op", result.getScope());
			assertEquals("TIME_LIMITED_TOKEN_AUTH", result.getVariant());
			assertEquals(expiresAt, result.getExpiresAt());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11UsageGeneralException() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenThrow(InvalidKeyException.class);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> engine.produce(
						"ConsumerName",
						"ConsumerName",
						"LOCAL",
						ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
						"ProviderName",
						AuthorizationTargetType.SERVICE_DEF,
						"testService",
						"op",
						7,
						null,
						"testOrigin"));

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");

		assertEquals("Token generation failed", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11UsageInternalServerError() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
		when(usageLimitedTokenDbService.save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(InternalServerError.class,
				() -> engine.produce(
						"ConsumerName",
						"ConsumerName",
						"LOCAL",
						ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
						"ProviderName",
						AuthorizationTargetType.SERVICE_DEF,
						"testService",
						"op",
						7,
						null,
						"testOrigin"));

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
		verify(tokenHeaderDbService).find("hashedToken");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11UsageOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final Pair<UsageLimitedToken, Boolean> dbResult = Pair.of(
				new UsageLimitedToken(
						new TokenHeader(
								AuthorizationTokenType.USAGE_LIMITED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						7),
				true);

		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
		when(usageLimitedTokenDbService.save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7)).thenReturn(dbResult);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7,
				null,
				"testOrigin");

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
		verify(tokenHeaderDbService).find("hashedToken");
		verify(sysInfo, never()).getSimpleTokenUsageLimit();
		verify(usageLimitedTokenDbService).save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				7);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.USAGE_LIMITED_TOKEN, result.getTokenType());
		assertEquals("0123456789abcdef", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("USAGE_LIMITED_TOKEN_AUTH", result.getVariant());
		assertEquals(7, result.getUsageLimit());
		assertEquals(7, result.getUsageLeft());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11TimeOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 12, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final Pair<TimeLimitedToken, Boolean> dbResult = Pair.of(
				new TimeLimitedToken(
						new TokenHeader(
								AuthorizationTokenType.TIME_LIMITED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						expiresAt),
				true);

		when(sysInfo.getSimpleTokenByteSize()).thenReturn(16);
		when(tokenGenerator.generateSimpleToken(16)).thenReturn("0123456789abcdef");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("hashedToken")).thenReturn(Optional.empty());
		when(timeLimitedTokenDbService.save(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				expiresAt)).thenReturn(dbResult);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				null,
				expiresAt,
				"testOrigin");

		verify(sysInfo).getSimpleTokenByteSize();
		verify(tokenGenerator).generateSimpleToken(16);
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("0123456789abcdef", "aVerySecretKey");
		verify(tokenHeaderDbService).find("hashedToken");
		verify(sysInfo, never()).getTokenTimeLimit();
		verify(timeLimitedTokenDbService).save(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				expiresAt);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.TIME_LIMITED_TOKEN, result.getTokenType());
		assertEquals("0123456789abcdef", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("TIME_LIMITED_TOKEN_AUTH", result.getVariant());
		assertEquals(expiresAt, result.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11UnhandledTokenType() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getTokenTimeLimit()).thenReturn(300);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> engine.produce(
						"ConsumerName",
						"ConsumerName",
						"LOCAL",
						ServiceInterfacePolicy.CERT_AUTH,
						"ProviderName",
						AuthorizationTargetType.SERVICE_DEF,
						"testService",
						"op",
						null,
						null,
						"testOrigin"));

		verify(sysInfo).getTokenTimeLimit();

		assertEquals("Token generation failed", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11Base64SelfContainedTokenOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 12, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final Pair<SelfContainedToken, Boolean> dbToken = Pair.of(
				new SelfContainedToken(
						new TokenHeader(
								AuthorizationTokenType.SELF_CONTAINED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						"BASE64_SELF_CONTAINED_TOKEN_AUTH",
						expiresAt),
				true);

		when(tokenGenerator.generateBas64SelfContainedToken(eq(expiresAt), any(SelfContainedTokenPayload.class))).thenReturn("rawToken");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aVerySecretKey")).thenReturn("hashedToken");
		when(selfContainedTokenDbService.save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"BASE64_SELF_CONTAINED_TOKEN_AUTH",
				expiresAt)).thenReturn(dbToken);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				null,
				expiresAt,
				"testOrigin");

		verify(sysInfo, never()).getTokenTimeLimit();
		verify(tokenGenerator).generateBas64SelfContainedToken(eq(expiresAt), any(SelfContainedTokenPayload.class));
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aVerySecretKey");
		verify(selfContainedTokenDbService).save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"BASE64_SELF_CONTAINED_TOKEN_AUTH",
				expiresAt);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.SELF_CONTAINED_TOKEN, result.getTokenType());
		assertEquals("rawToken", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("BASE64_SELF_CONTAINED_TOKEN_AUTH", result.getVariant());
		assertEquals(expiresAt, result.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11JWTNoSSL() throws InvalidKeyException, NoSuchAlgorithmException {
		ReflectionTestUtils.setField(engine, "arrowheadContext", Map.of());

		when(sysInfo.getTokenTimeLimit()).thenReturn(300);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> engine.produce(
						"ConsumerName",
						"ConsumerName",
						"LOCAL",
						ServiceInterfacePolicy.RSA_SHA256_JSON_WEB_TOKEN_AUTH,
						"ProviderName",
						AuthorizationTargetType.SERVICE_DEF,
						"testService",
						"op",
						null,
						null,
						"testOrigin"));

		verify(sysInfo).getTokenTimeLimit();

		assertEquals("JWT is supported only when SSL is enabled", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11JWTRSA512Ok() throws InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, JoseException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keyStore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");
		final Map<String, Object> context = Map.of("server.private.key", privateKey);
		ReflectionTestUtils.setField(engine, "arrowheadContext", context);

		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 12, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final Pair<SelfContainedToken, Boolean> dbToken = Pair.of(
				new SelfContainedToken(
						new TokenHeader(
								AuthorizationTokenType.SELF_CONTAINED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
						expiresAt),
				true);

		when(tokenGenerator.generateJsonWebToken(eq("RS512"), eq(privateKey), eq(expiresAt), any(SelfContainedTokenPayload.class))).thenReturn("rawToken");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aVerySecretKey")).thenReturn("hashedToken");
		when(selfContainedTokenDbService.save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
				expiresAt)).thenReturn(dbToken);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.RSA_SHA512_JSON_WEB_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				null,
				expiresAt,
				"testOrigin");

		verify(sysInfo, never()).getTokenTimeLimit();
		verify(tokenGenerator).generateJsonWebToken(eq("RS512"), eq(privateKey), eq(expiresAt), any(SelfContainedTokenPayload.class));
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aVerySecretKey");
		verify(selfContainedTokenDbService).save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"RSA_SHA512_JSON_WEB_TOKEN_AUTH",
				expiresAt);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.SELF_CONTAINED_TOKEN, result.getTokenType());
		assertEquals("rawToken", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("RSA_SHA512_JSON_WEB_TOKEN_AUTH", result.getVariant());
		assertEquals(expiresAt, result.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testProduce11JWTRSA256Ok() throws InvalidKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, JoseException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keyStore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu", "123456");
		final Map<String, Object> context = Map.of("server.private.key", privateKey);
		ReflectionTestUtils.setField(engine, "arrowheadContext", context);

		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 12, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final Pair<SelfContainedToken, Boolean> dbToken = Pair.of(
				new SelfContainedToken(
						new TokenHeader(
								AuthorizationTokenType.SELF_CONTAINED_TOKEN,
								"hashedToken",
								"ConsumerName",
								"LOCAL",
								"ConsumerName",
								"ProviderName",
								AuthorizationTargetType.SERVICE_DEF,
								"testService",
								"op"),
						"RSA_SHA256_JSON_WEB_TOKEN_AUTH",
						expiresAt),
				true);

		when(tokenGenerator.generateJsonWebToken(eq("RS256"), eq(privateKey), eq(expiresAt), any(SelfContainedTokenPayload.class))).thenReturn("rawToken");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aVerySecretKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aVerySecretKey")).thenReturn("hashedToken");
		when(selfContainedTokenDbService.save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"RSA_SHA256_JSON_WEB_TOKEN_AUTH",
				expiresAt)).thenReturn(dbToken);

		final TokenModel result = engine.produce(
				"ConsumerName",
				"ConsumerName",
				"LOCAL",
				ServiceInterfacePolicy.RSA_SHA256_JSON_WEB_TOKEN_AUTH,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				null,
				expiresAt,
				"testOrigin");

		verify(sysInfo, never()).getTokenTimeLimit();
		verify(tokenGenerator).generateJsonWebToken(eq("RS256"), eq(privateKey), eq(expiresAt), any(SelfContainedTokenPayload.class));
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aVerySecretKey");
		verify(selfContainedTokenDbService).save(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op",
				"RSA_SHA256_JSON_WEB_TOKEN_AUTH",
				expiresAt);

		assertFalse(result.isEncrypted());
		assertEquals(AuthorizationTokenType.SELF_CONTAINED_TOKEN, result.getTokenType());
		assertEquals("rawToken", result.getRawToken());
		assertEquals("hashedToken", result.getHashedToken());
		assertEquals("ConsumerName", result.getRequester());
		assertEquals("LOCAL", result.getConsumerCloud());
		assertEquals("ConsumerName", result.getConsumer());
		assertEquals("ProviderName", result.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.getTargetType());
		assertEquals("testService", result.getTarget());
		assertEquals("op", result.getScope());
		assertEquals("RSA_SHA256_JSON_WEB_TOKEN_AUTH", result.getVariant());
		assertEquals(expiresAt, result.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyHashCalculationProblem() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenThrow(InvalidKeyException.class);

		final Throwable ex = assertThrows(InternalServerError.class,
				() -> engine.verify("ConsumerName", "rawToken", "testOrigin"));

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");

		assertEquals("Token verification failed", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyDbProblem() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(InternalServerError.class,
				() -> engine.verify("ConsumerName", "rawToken", "testOrigin"));

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyHashedTokenNotFound() throws InvalidKeyException, NoSuchAlgorithmException {
		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenReturn(Optional.empty());

		final Pair<Boolean, Optional<TokenModel>> result = engine.verify("ConsumerName", "rawToken", "testOrigin");

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");

		assertFalse(result.getFirst());
		assertTrue(result.getSecond().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifySelfContainedToken() throws InvalidKeyException, NoSuchAlgorithmException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");

		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenReturn(Optional.of(tokenHeader));

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> engine.verify("ConsumerName", "rawToken", "testOrigin"));

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");

		assertEquals("Self contained tokens can't be verified this way", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyUsageLimitedTokenNoDetails() throws InvalidKeyException, NoSuchAlgorithmException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");

		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenReturn(Optional.of(tokenHeader));
		when(usageLimitedTokenDbService.decrease(tokenHeader)).thenReturn(Optional.empty());

		final Pair<Boolean, Optional<TokenModel>> result = engine.verify("ConsumerName", "rawToken", "testOrigin");

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");
		verify(usageLimitedTokenDbService).decrease(tokenHeader);

		assertFalse(result.getFirst());
		assertTrue(result.getSecond().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyUsageLimitedTokenNoUsageLeft() throws InvalidKeyException, NoSuchAlgorithmException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");

		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenReturn(Optional.of(tokenHeader));
		when(usageLimitedTokenDbService.decrease(tokenHeader)).thenReturn(Optional.of(Pair.of(0, -1)));

		final Pair<Boolean, Optional<TokenModel>> result = engine.verify("ConsumerName", "rawToken", "testOrigin");

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");
		verify(usageLimitedTokenDbService).decrease(tokenHeader);

		assertFalse(result.getFirst());
		assertTrue(result.getSecond().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyUsageLimitedTokenVerified() throws InvalidKeyException, NoSuchAlgorithmException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");

		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenReturn(Optional.of(tokenHeader));
		when(usageLimitedTokenDbService.decrease(tokenHeader)).thenReturn(Optional.of(Pair.of(2, 1)));

		final Pair<Boolean, Optional<TokenModel>> result = engine.verify("ConsumerName", "rawToken", "testOrigin");

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");
		verify(usageLimitedTokenDbService).decrease(tokenHeader);

		assertTrue(result.getFirst());
		final TokenModel resultModel = result.getSecond().get();
		assertFalse(resultModel.isEncrypted());
		assertEquals(AuthorizationTokenType.USAGE_LIMITED_TOKEN, resultModel.getTokenType());
		assertNull(resultModel.getRawToken());
		assertEquals("hashedToken", resultModel.getHashedToken());
		assertEquals("ConsumerName", resultModel.getRequester());
		assertEquals("LOCAL", resultModel.getConsumerCloud());
		assertEquals("ConsumerName", resultModel.getConsumer());
		assertEquals("ProviderName", resultModel.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, resultModel.getTargetType());
		assertEquals("testService", resultModel.getTarget());
		assertEquals("op", resultModel.getScope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyTimeLimitedTokenFalse() throws InvalidKeyException, NoSuchAlgorithmException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");

		final ZonedDateTime expiresAt = ZonedDateTime.of(2025, 05, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final TimeLimitedToken token = new TimeLimitedToken(tokenHeader, expiresAt);

		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenReturn(Optional.of(tokenHeader));
		when(timeLimitedTokenDbService.getByHeader(tokenHeader)).thenReturn(Optional.of(token));

		final Pair<Boolean, Optional<TokenModel>> result = engine.verify("ConsumerName", "rawToken", "testOrigin");

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");
		verify(timeLimitedTokenDbService).getByHeader(tokenHeader);

		assertFalse(result.getFirst());
		assertTrue(result.getSecond().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyTimeLimitedTokenTrue() throws InvalidKeyException, NoSuchAlgorithmException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");

		final ZonedDateTime expiresAt = ZonedDateTime.of(2125, 05, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final TimeLimitedToken token = new TimeLimitedToken(tokenHeader, expiresAt);

		when(sysInfo.getSecretCryptographerKey()).thenReturn("aKey");
		when(secretCryptographer.encrypt_HMAC_SHA256("rawToken", "aKey")).thenReturn("hashedToken");
		when(tokenHeaderDbService.find("ConsumerName", "hashedToken")).thenReturn(Optional.of(tokenHeader));
		when(timeLimitedTokenDbService.getByHeader(tokenHeader)).thenReturn(Optional.of(token));

		final Pair<Boolean, Optional<TokenModel>> result = engine.verify("ConsumerName", "rawToken", "testOrigin");

		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_HMAC_SHA256("rawToken", "aKey");
		verify(tokenHeaderDbService).find("ConsumerName", "hashedToken");
		verify(timeLimitedTokenDbService).getByHeader(tokenHeader);

		assertTrue(result.getFirst());
		final TokenModel resultModel = result.getSecond().get();
		assertFalse(resultModel.isEncrypted());
		assertEquals(AuthorizationTokenType.TIME_LIMITED_TOKEN, resultModel.getTokenType());
		assertNull(resultModel.getRawToken());
		assertEquals("hashedToken", resultModel.getHashedToken());
		assertEquals("ConsumerName", resultModel.getRequester());
		assertEquals("LOCAL", resultModel.getConsumerCloud());
		assertEquals("ConsumerName", resultModel.getConsumer());
		assertEquals("ProviderName", resultModel.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, resultModel.getTargetType());
		assertEquals("testService", resultModel.getTarget());
		assertEquals("op", resultModel.getScope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeEmptyList() {
		assertDoesNotThrow(() -> engine.revoke(List.of(), "testOrigin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeExceptionInDb() {
		final List<String> list = List.of("hashedToken");

		when(tokenHeaderDbService.findByTokenHashList(list)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(InternalServerError.class,
				() -> engine.revoke(list, "testOrigin"));

		verify(tokenHeaderDbService).findByTokenHashList(list);

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeOk() {
		final List<String> list = List.of("hashedToken");
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		tokenHeader.setId(1L);

		when(tokenHeaderDbService.findByTokenHashList(list)).thenReturn(List.of(tokenHeader));
		doNothing().when(tokenHeaderDbService).deleteById(List.of(1L));

		assertDoesNotThrow(() -> engine.revoke(list, "testOrigin"));

		verify(tokenHeaderDbService).findByTokenHashList(list);
		verify(tokenHeaderDbService).deleteById(List.of(1L));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryDbException() {
		final Pageable page = PageRequest.of(0, 10);
		when(tokenHeaderDbService.query(
				page,
				null,
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(InternalServerError.class,
				() -> engine.query(page, null, AuthorizationTokenType.USAGE_LIMITED_TOKEN, null, null, "ProviderName", AuthorizationTargetType.SERVICE_DEF, "testService", "testOrigin"));

		verify(tokenHeaderDbService).query(page,
				null,
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF);

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryUsageLimitedNoDetails() {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final Pageable page = PageRequest.of(0, 10);

		when(tokenHeaderDbService.query(
				page,
				null,
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF)).thenReturn(new PageImpl<>(List.of(tokenHeader), page, 1));
		when(usageLimitedTokenDbService.getByHeader(tokenHeader)).thenReturn(Optional.empty());

		final Page<TokenModel> resultPage = engine.query(
				page,
				null,
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"testOrigin");

		verify(tokenHeaderDbService).query(page,
				null,
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF);
		verify(usageLimitedTokenDbService).getByHeader(tokenHeader);

		assertEquals(1, resultPage.getContent().size());
		final TokenModel resultModel = resultPage.getContent().get(0);
		assertFalse(resultModel.isEncrypted());
		assertEquals(AuthorizationTokenType.USAGE_LIMITED_TOKEN, resultModel.getTokenType());
		assertNull(resultModel.getRawToken());
		assertEquals("hashedToken", resultModel.getHashedToken());
		assertEquals("ConsumerName", resultModel.getRequester());
		assertEquals("LOCAL", resultModel.getConsumerCloud());
		assertEquals("ConsumerName", resultModel.getConsumer());
		assertEquals("ProviderName", resultModel.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, resultModel.getTargetType());
		assertEquals("testService", resultModel.getTarget());
		assertEquals("op", resultModel.getScope());
		assertNull(resultModel.getVariant());
		assertNull(resultModel.getUsageLimit());
		assertNull(resultModel.getUsageLeft());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryUsageLimitedWithDetails() {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final Pageable page = PageRequest.of(0, 10);
		final UsageLimitedToken details = new UsageLimitedToken(tokenHeader, 10);
		details.setUsageLeft(6);

		when(tokenHeaderDbService.query(
				page,
				null,
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF)).thenReturn(new PageImpl<>(List.of(tokenHeader), page, 1));
		when(usageLimitedTokenDbService.getByHeader(tokenHeader)).thenReturn(Optional.of(details));

		final Page<TokenModel> resultPage = engine.query(
				page,
				null,
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"testOrigin");

		verify(tokenHeaderDbService).query(page,
				null,
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF);
		verify(usageLimitedTokenDbService).getByHeader(tokenHeader);

		assertEquals(1, resultPage.getContent().size());
		final TokenModel resultModel = resultPage.getContent().get(0);
		assertFalse(resultModel.isEncrypted());
		assertEquals(AuthorizationTokenType.USAGE_LIMITED_TOKEN, resultModel.getTokenType());
		assertNull(resultModel.getRawToken());
		assertEquals("hashedToken", resultModel.getHashedToken());
		assertEquals("ConsumerName", resultModel.getRequester());
		assertEquals("LOCAL", resultModel.getConsumerCloud());
		assertEquals("ConsumerName", resultModel.getConsumer());
		assertEquals("ProviderName", resultModel.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, resultModel.getTargetType());
		assertEquals("testService", resultModel.getTarget());
		assertEquals("op", resultModel.getScope());
		assertEquals("USAGE_LIMITED_TOKEN_AUTH", resultModel.getVariant());
		assertEquals(10, resultModel.getUsageLimit());
		assertEquals(6, resultModel.getUsageLeft());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTimeLimitedNoDetails() {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final Pageable page = PageRequest.of(0, 10);

		when(tokenHeaderDbService.query(
				page,
				null,
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF)).thenReturn(new PageImpl<>(List.of(tokenHeader), page, 1));
		when(timeLimitedTokenDbService.getByHeader(tokenHeader)).thenReturn(Optional.empty());

		final Page<TokenModel> resultPage = engine.query(
				page,
				null,
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"testOrigin");

		verify(tokenHeaderDbService).query(page,
				null,
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF);
		verify(timeLimitedTokenDbService).getByHeader(tokenHeader);

		assertEquals(1, resultPage.getContent().size());
		final TokenModel resultModel = resultPage.getContent().get(0);
		assertFalse(resultModel.isEncrypted());
		assertEquals(AuthorizationTokenType.TIME_LIMITED_TOKEN, resultModel.getTokenType());
		assertNull(resultModel.getRawToken());
		assertEquals("hashedToken", resultModel.getHashedToken());
		assertEquals("ConsumerName", resultModel.getRequester());
		assertEquals("LOCAL", resultModel.getConsumerCloud());
		assertEquals("ConsumerName", resultModel.getConsumer());
		assertEquals("ProviderName", resultModel.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, resultModel.getTargetType());
		assertEquals("testService", resultModel.getTarget());
		assertEquals("op", resultModel.getScope());
		assertNull(resultModel.getVariant());
		assertNull(resultModel.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTimeLimitedWithDetails() {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final Pageable page = PageRequest.of(0, 10);
		final ZonedDateTime expiresAt = ZonedDateTime.of(2125, 05, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final TimeLimitedToken details = new TimeLimitedToken(tokenHeader, expiresAt);

		when(tokenHeaderDbService.query(
				page,
				null,
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF)).thenReturn(new PageImpl<>(List.of(tokenHeader), page, 1));
		when(timeLimitedTokenDbService.getByHeader(tokenHeader)).thenReturn(Optional.of(details));

		final Page<TokenModel> resultPage = engine.query(
				page,
				null,
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"testOrigin");

		verify(tokenHeaderDbService).query(page,
				null,
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF);
		verify(timeLimitedTokenDbService).getByHeader(tokenHeader);

		assertEquals(1, resultPage.getContent().size());
		final TokenModel resultModel = resultPage.getContent().get(0);
		assertFalse(resultModel.isEncrypted());
		assertEquals(AuthorizationTokenType.TIME_LIMITED_TOKEN, resultModel.getTokenType());
		assertNull(resultModel.getRawToken());
		assertEquals("hashedToken", resultModel.getHashedToken());
		assertEquals("ConsumerName", resultModel.getRequester());
		assertEquals("LOCAL", resultModel.getConsumerCloud());
		assertEquals("ConsumerName", resultModel.getConsumer());
		assertEquals("ProviderName", resultModel.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, resultModel.getTargetType());
		assertEquals("testService", resultModel.getTarget());
		assertEquals("op", resultModel.getScope());
		assertEquals("TIME_LIMITED_TOKEN_AUTH", resultModel.getVariant());
		assertEquals(expiresAt, resultModel.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySelfContainedNoDetails() {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final Pageable page = PageRequest.of(0, 10);

		when(tokenHeaderDbService.query(
				page,
				null,
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF)).thenReturn(new PageImpl<>(List.of(tokenHeader), page, 1));
		when(selfContainedTokenDbService.getByHeader(tokenHeader)).thenReturn(Optional.empty());

		final Page<TokenModel> resultPage = engine.query(
				page,
				null,
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				null,
				null,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"testOrigin");

		verify(tokenHeaderDbService).query(page,
				null,
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF);
		verify(selfContainedTokenDbService).getByHeader(tokenHeader);

		assertEquals(1, resultPage.getContent().size());
		final TokenModel resultModel = resultPage.getContent().get(0);
		assertFalse(resultModel.isEncrypted());
		assertEquals(AuthorizationTokenType.SELF_CONTAINED_TOKEN, resultModel.getTokenType());
		assertNull(resultModel.getRawToken());
		assertEquals("hashedToken", resultModel.getHashedToken());
		assertEquals("ConsumerName", resultModel.getRequester());
		assertEquals("LOCAL", resultModel.getConsumerCloud());
		assertEquals("ConsumerName", resultModel.getConsumer());
		assertEquals("ProviderName", resultModel.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, resultModel.getTargetType());
		assertEquals("testService", resultModel.getTarget());
		assertEquals("op", resultModel.getScope());
		assertNull(resultModel.getVariant());
		assertNull(resultModel.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySelfContainedWithDetails() {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final Pageable page = PageRequest.of(0, 10);
		final ZonedDateTime expiresAt = ZonedDateTime.of(2125, 05, 10, 12, 0, 0, 0, ZoneId.of("UTC"));
		final SelfContainedToken details = new SelfContainedToken(tokenHeader, "RSA_SHA512_JSON_WEB_TOKEN_AUTH", expiresAt);

		when(tokenHeaderDbService.query(
				page,
				null,
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF)).thenReturn(new PageImpl<>(List.of(tokenHeader), page, 1));
		when(selfContainedTokenDbService.getByHeader(tokenHeader)).thenReturn(Optional.of(details));

		final Page<TokenModel> resultPage = engine.query(
				page,
				null,
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				null,
				null,
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"testOrigin");

		verify(tokenHeaderDbService).query(page,
				null,
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				null,
				null,
				"ProviderName",
				"testService",
				AuthorizationTargetType.SERVICE_DEF);
		verify(selfContainedTokenDbService).getByHeader(tokenHeader);

		assertEquals(1, resultPage.getContent().size());
		final TokenModel resultModel = resultPage.getContent().get(0);
		assertFalse(resultModel.isEncrypted());
		assertEquals(AuthorizationTokenType.SELF_CONTAINED_TOKEN, resultModel.getTokenType());
		assertNull(resultModel.getRawToken());
		assertEquals("hashedToken", resultModel.getHashedToken());
		assertEquals("ConsumerName", resultModel.getRequester());
		assertEquals("LOCAL", resultModel.getConsumerCloud());
		assertEquals("ConsumerName", resultModel.getConsumer());
		assertEquals("ProviderName", resultModel.getProvider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, resultModel.getTargetType());
		assertEquals("testService", resultModel.getTarget());
		assertEquals("op", resultModel.getScope());
		assertEquals("RSA_SHA512_JSON_WEB_TOKEN_AUTH", resultModel.getVariant());
		assertEquals(expiresAt, resultModel.getExpiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncryptTokenIfNeededNotNeeded() {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final TokenModel model = new TokenModel(tokenHeader, "rawToken");

		when(encryptionKeyDbService.get("ProviderName")).thenReturn(Optional.empty());

		assertFalse(model.isEncrypted());
		assertDoesNotThrow(() -> engine.encryptTokenIfNeeded(model, "testOrigin"));

		verify(encryptionKeyDbService).get("ProviderName");

		assertFalse(model.isEncrypted());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncryptTokenIfNeededDbException() {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final TokenModel model = new TokenModel(tokenHeader, "rawToken");

		when(encryptionKeyDbService.get("ProviderName")).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(InternalServerError.class,
				() -> engine.encryptTokenIfNeeded(model, "testOrigin"));

		verify(encryptionKeyDbService).get("ProviderName");

		assertEquals("test", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncryptTokenIfNeededOtherException() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
			BadPaddingException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final TokenModel model = new TokenModel(tokenHeader, "rawToken");
		final EncryptionKey keyRecord = new EncryptionKey(
				"ProviderName",
				"encryptedKey",
				"unsupported",
				new CryptographerAuxiliary("internalAux"),
				new CryptographerAuxiliary("externalAux"));

		when(encryptionKeyDbService.get("ProviderName")).thenReturn(Optional.of(keyRecord));
		when(sysInfo.getSecretCryptographerKey()).thenReturn("secretKey");
		when(secretCryptographer.decrypt_AES_CBC_PKCS5P_IV("encryptedKey", "internalAux", "secretKey")).thenReturn("plainProviderKey");

		final ArrowheadException ex = assertThrows(InternalServerError.class,
				() -> engine.encryptTokenIfNeeded(model, "testOrigin"));

		verify(encryptionKeyDbService).get("ProviderName");
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).decrypt_AES_CBC_PKCS5P_IV("encryptedKey", "internalAux", "secretKey");

		assertEquals("Token encryption failed", ex.getMessage());
		assertEquals("testOrigin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:methodname")
	@Test
	public void testEncryptTokenIfNeeded_AES_ECB_PKCS5Padding_Ok() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final TokenModel model = new TokenModel(tokenHeader, "rawToken");
		final EncryptionKey keyRecord = new EncryptionKey(
				"ProviderName",
				"encryptedKey",
				"AES/ECB/PKCS5Padding",
				new CryptographerAuxiliary("internalAux"),
				new CryptographerAuxiliary("externalAux"));

		when(encryptionKeyDbService.get("ProviderName")).thenReturn(Optional.of(keyRecord));
		when(sysInfo.getSecretCryptographerKey()).thenReturn("secretKey");
		when(secretCryptographer.decrypt_AES_CBC_PKCS5P_IV("encryptedKey", "internalAux", "secretKey")).thenReturn("plainProviderKey");
		when(secretCryptographer.encrypt_AES_ECB_PKCS5P("rawToken", "plainProviderKey")).thenReturn("encryptedToken");

		assertFalse(model.isEncrypted());

		assertDoesNotThrow(() -> engine.encryptTokenIfNeeded(model, "testOrigin"));

		verify(encryptionKeyDbService).get("ProviderName");
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).decrypt_AES_CBC_PKCS5P_IV("encryptedKey", "internalAux", "secretKey");
		verify(secretCryptographer).encrypt_AES_ECB_PKCS5P("rawToken", "plainProviderKey");

		assertTrue(model.isEncrypted());
		assertEquals("encryptedToken", model.getEncryptedToken());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:methodname")
	@Test
	public void testEncryptTokenIfNeeded_AES_CBC_PKCS5Padding_Ok() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"ConsumerName",
				"LOCAL",
				"ConsumerName",
				"ProviderName",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final TokenModel model = new TokenModel(tokenHeader, "rawToken");
		final EncryptionKey keyRecord = new EncryptionKey(
				"ProviderName",
				"encryptedKey",
				"AES/CBC/PKCS5Padding",
				new CryptographerAuxiliary("internalAux"),
				new CryptographerAuxiliary("externalAux"));

		when(encryptionKeyDbService.get("ProviderName")).thenReturn(Optional.of(keyRecord));
		when(sysInfo.getSecretCryptographerKey()).thenReturn("secretKey");
		when(secretCryptographer.decrypt_AES_CBC_PKCS5P_IV("encryptedKey", "internalAux", "secretKey")).thenReturn("plainProviderKey");
		when(secretCryptographer.encrypt_AES_CBC_PKCS5P_IV("rawToken", "plainProviderKey", "externalAux")).thenReturn(Pair.of("encryptedToken", "externalAux"));

		assertFalse(model.isEncrypted());

		assertDoesNotThrow(() -> engine.encryptTokenIfNeeded(model, "testOrigin"));

		verify(encryptionKeyDbService).get("ProviderName");
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).decrypt_AES_CBC_PKCS5P_IV("encryptedKey", "internalAux", "secretKey");
		verify(secretCryptographer).encrypt_AES_CBC_PKCS5P_IV("rawToken", "plainProviderKey", "externalAux");

		assertTrue(model.isEncrypted());
		assertEquals("encryptedToken", model.getEncryptedToken());
	}
}