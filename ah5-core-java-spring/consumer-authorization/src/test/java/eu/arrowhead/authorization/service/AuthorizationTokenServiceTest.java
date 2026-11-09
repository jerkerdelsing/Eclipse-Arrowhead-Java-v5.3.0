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
package eu.arrowhead.authorization.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.authorization.service.engine.TokenEngine;
import eu.arrowhead.authorization.service.model.EncryptionKeyModel;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.validation.AuthorizationTokenValidation;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.dto.AuthorizationEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenVerifyResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@ExtendWith(MockitoExtension.class)
public class AuthorizationTokenServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationTokenService service;

	@Mock
	private AuthorizationSystemInfo sysInfo;

	@Mock
	private AuthorizationPolicyEngine policyEngine;

	@Mock
	private TokenEngine tokenEngine;

	@Mock
	private SecretCryptographer secretCryptographer;

	@Mock
	private EncryptionKeyDbService encryptionKeyDbService;

	@Mock
	private AuthorizationTokenValidation validator;

	@Mock
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.generate(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.generate(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateForbiddenException() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"USAGE_LIMITED_TOKEN_AUTH",
				"TestProvider",
				"SERVICE_DEF",
				"serviceDef",
				"operation");

		when(validator.validateAndNormalizeSystemName("TestConsumer", "origin")).thenReturn("TestConsumer");
		when(validator.validateAndNormalizeGenerateRequest(dto, "origin")).thenReturn(dto);
		when(policyEngine.isAccessGranted(any(NormalizedVerifyRequest.class))).thenReturn(false);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> service.generate("TestConsumer", dto, "origin"));

		assertEquals("Requester has no permisson to the service instance and/or operation", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("TestConsumer", "origin");
		verify(validator).validateAndNormalizeGenerateRequest(dto, "origin");
		verify(policyEngine).isAccessGranted(any(NormalizedVerifyRequest.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateNoEncryption() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"USAGE_LIMITED_TOKEN_AUTH",
				"TestProvider",
				"SERVICE_DEF",
				"serviceDef",
				"operation");

		final TokenHeader usageLimitedToken = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"TestConsumer",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		final TokenModel token = new TokenModel(usageLimitedToken);

		final AuthorizationTokenGenerationResponseDTO converted = new AuthorizationTokenGenerationResponseDTO(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				AuthorizationTargetType.SERVICE_DEF,
				"token",
				10,
				null);

		when(validator.validateAndNormalizeSystemName("TestConsumer", "origin")).thenReturn("TestConsumer");
		when(validator.validateAndNormalizeGenerateRequest(dto, "origin")).thenReturn(dto);
		when(policyEngine.isAccessGranted(any(NormalizedVerifyRequest.class))).thenReturn(true);
		when(tokenEngine.produce(
				"TestConsumer",
				"TestConsumer",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				"origin")).thenReturn(token);
		when(dtoConverter.convertTokenModelToResponse(token)).thenReturn(converted);

		assertDoesNotThrow(() -> service.generate("TestConsumer", dto, "origin"));

		verify(validator).validateAndNormalizeSystemName("TestConsumer", "origin");
		verify(validator).validateAndNormalizeGenerateRequest(dto, "origin");
		verify(policyEngine).isAccessGranted(any(NormalizedVerifyRequest.class));
		verify(tokenEngine).produce(
				"TestConsumer",
				"TestConsumer",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				"origin");
		verify(tokenEngine, never()).encryptTokenIfNeeded(token, "origin");
		verify(dtoConverter).convertTokenModelToResponse(token);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateEncryption() {
		final AuthorizationTokenGenerationRequestDTO dto = new AuthorizationTokenGenerationRequestDTO(
				"BASE64_SELF_CONTAINED_TOKEN_AUTH",
				"TestProvider",
				"SERVICE_DEF",
				"serviceDef",
				null);

		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"TestConsumer",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);
		final TokenModel token = new TokenModel(tokenHeader);

		final AuthorizationTokenGenerationResponseDTO converted = new AuthorizationTokenGenerationResponseDTO(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				AuthorizationTargetType.SERVICE_DEF,
				"token",
				null,
				null);

		when(validator.validateAndNormalizeSystemName("TestConsumer", "origin")).thenReturn("TestConsumer");
		when(validator.validateAndNormalizeGenerateRequest(dto, "origin")).thenReturn(dto);
		when(policyEngine.isAccessGranted(any(NormalizedVerifyRequest.class))).thenReturn(true);
		when(tokenEngine.produce(
				"TestConsumer",
				"TestConsumer",
				ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				"origin")).thenReturn(token);
		doNothing().when(tokenEngine).encryptTokenIfNeeded(token, "origin");
		when(dtoConverter.convertTokenModelToResponse(token)).thenReturn(converted);

		assertDoesNotThrow(() -> service.generate("TestConsumer", dto, "origin"));

		verify(validator).validateAndNormalizeSystemName("TestConsumer", "origin");
		verify(validator).validateAndNormalizeGenerateRequest(dto, "origin");
		verify(policyEngine).isAccessGranted(any(NormalizedVerifyRequest.class));
		verify(tokenEngine).produce(
				"TestConsumer",
				"TestConsumer",
				ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				"origin");
		verify(tokenEngine).encryptTokenIfNeeded(token, "origin");
		verify(dtoConverter).convertTokenModelToResponse(token);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.verify(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.verify(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testVerifyOk() {
		final Pair<Boolean, Optional<TokenModel>> pair = Pair.of(false, Optional.empty());

		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		when(validator.validateAndNormalizeToken("token", "origin")).thenReturn("token");
		when(tokenEngine.verify("TestProvider", "token", "origin")).thenReturn(pair);
		when(dtoConverter.convertTokenVerificationResultToResponse(pair)).thenReturn(new AuthorizationTokenVerifyResponseDTO(false, null, null, null, null, null));

		assertDoesNotThrow(() -> service.verify("TestProvider", "token", "origin"));

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(validator).validateAndNormalizeToken("token", "origin");
		verify(tokenEngine).verify("TestProvider", "token", "origin");
		verify(dtoConverter).convertTokenVerificationResultToResponse(pair);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getPublicKey(null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getPublicKey(""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyNotFound() {
		ReflectionTestUtils.setField(service, "arrowheadContext", Map.of());

		final ArrowheadException ex = assertThrows(
				DataNotFoundException.class,
				() -> service.getPublicKey("origin"));

		assertEquals("Public key is not available", ex.getMessage());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPublicKeyOk() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final Resource keyStoreResource = new ClassPathResource("certificate/ConsumerAuthorization.p12");
		final KeyStore keystore = KeyStore.getInstance(Constants.PKCS12);
		keystore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final PublicKey publicKey = SecurityUtilities.getCertificateFromKeyStore(keystore, "ConsumerAuthorization.TestCloud.Company.arrowhead.eu").getPublicKey();
		ReflectionTestUtils.setField(service, "arrowheadContext", Map.of(Constants.SERVER_PUBLIC_KEY, publicKey));

		final String result = service.getPublicKey("origin");

		assertTrue(!Utilities.isEmpty(result));
		assertArrayEquals(publicKey.getEncoded(), Base64.getDecoder().decode(result));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterEncryptionKeyNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.registerEncryptionKey(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterEncryptionKeyEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.registerEncryptionKey(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterEncryptionKeyInternalServerErrorDuringEncryption() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final AuthorizationEncryptionKeyRegistrationRequestDTO dto = new AuthorizationEncryptionKeyRegistrationRequestDTO(
				"encryptionKey",
				SecretCryptographer.AES_CBC_ALGORITHM_IV_BASED);

		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		when(validator.validateAndNormalizeRegisterEncryptionKeyRequest(dto, "origin")).thenReturn(dto);
		when(secretCryptographer.generateInitializationVectorBase64()).thenReturn("externalInitVector");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("secretKey");
		when(secretCryptographer.encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey")).thenThrow(new InvalidKeyException("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.registerEncryptionKey("TestProvider", dto, "origin"));

		assertEquals("Secret encryption failed", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(validator).validateAndNormalizeRegisterEncryptionKeyRequest(dto, "origin");
		verify(secretCryptographer).generateInitializationVectorBase64();
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterEncryptionKeyInternalServerErrorDuringSave() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final AuthorizationEncryptionKeyRegistrationRequestDTO dto = new AuthorizationEncryptionKeyRegistrationRequestDTO(
				"encryptionKey",
				SecretCryptographer.AES_ECB_ALGORITHM);
		final Pair<String, String> pair = Pair.of("encryptedEncryptionKey", "internalInitVector");

		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		when(validator.validateAndNormalizeRegisterEncryptionKeyRequest(dto, "origin")).thenReturn(dto);
		when(sysInfo.getSecretCryptographerKey()).thenReturn("secretKey");
		when(secretCryptographer.encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey")).thenReturn(pair);
		when(encryptionKeyDbService.save(any(EncryptionKeyModel.class))).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.registerEncryptionKey("TestProvider", dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(validator).validateAndNormalizeRegisterEncryptionKeyRequest(dto, "origin");
		verify(secretCryptographer, never()).generateInitializationVectorBase64();
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey");
		verify(encryptionKeyDbService).save(any(EncryptionKeyModel.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRegisterEncryptionKeyOk() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final AuthorizationEncryptionKeyRegistrationRequestDTO dto = new AuthorizationEncryptionKeyRegistrationRequestDTO(
				"encryptionKey",
				SecretCryptographer.AES_CBC_ALGORITHM_IV_BASED);
		final Pair<String, String> pair = Pair.of("encryptedEncryptionKey", "internalInitVector");

		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		when(validator.validateAndNormalizeRegisterEncryptionKeyRequest(dto, "origin")).thenReturn(dto);
		when(secretCryptographer.generateInitializationVectorBase64()).thenReturn("externalInitVector");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("secretKey");
		when(secretCryptographer.encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey")).thenReturn(pair);
		when(encryptionKeyDbService.save(any(EncryptionKeyModel.class))).thenReturn(Pair.of(new EncryptionKey(), true));

		final String result = service.registerEncryptionKey("TestProvider", dto, "origin");

		assertEquals("externalInitVector", result);

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(validator).validateAndNormalizeRegisterEncryptionKeyRequest(dto, "origin");
		verify(secretCryptographer).generateInitializationVectorBase64();
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey");
		verify(encryptionKeyDbService).save(any(EncryptionKeyModel.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnregisterEncryptionKeyNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.unregisterEncryptionKey(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnregisterEncryptionKeyEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.unregisterEncryptionKey(null, " "));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testUnregisterEncryptionKeyOk() {
		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		when(encryptionKeyDbService.delete("TestProvider")).thenReturn(true);

		final boolean result = service.unregisterEncryptionKey("TestProvider", "origin");

		assertTrue(result);

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(encryptionKeyDbService).delete("TestProvider");
	}
}