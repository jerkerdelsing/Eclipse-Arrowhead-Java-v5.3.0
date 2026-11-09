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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.CryptographerAuxiliary;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.service.EncryptionKeyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.authorization.service.engine.TokenEngine;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.authorization.service.validation.AuthorizationTokenManagementValidation;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyListResponseDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyRegistrationRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@ExtendWith(MockitoExtension.class)
public class AuthorizationTokenManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationTokenManagementService service;

	@Mock
	private AuthorizationSystemInfo sysInfo;

	@Mock
	private AuthorizationTokenManagementValidation validator;

	@Mock
	private AuthorizationPolicyEngine policyEngine;

	@Mock
	private TokenEngine tokenEngine;

	@Mock
	private EncryptionKeyDbService encryptionKeyDbService;

	@Mock
	private SecretCryptographer secretCryptographer;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private PageService pageService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokensOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.generateTokensOperation(null, null, false, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokensOperationEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.generateTokensOperation(null, null, false, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokensOperationForbiddenException() {
		final AuthorizationTokenGenerationMgmtRequestDTO request = new AuthorizationTokenGenerationMgmtRequestDTO(
				"USAGE_LIMITED_TOKEN_AUTH",
				"SERVICE_DEF",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				"serviceDef",
				"operation",
				null,
				17);
		final AuthorizationTokenGenerationMgmtListRequestDTO dto = new AuthorizationTokenGenerationMgmtListRequestDTO(List.of(request));

		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeGenerateTokenRequests(dto, "origin")).thenReturn(dto);
		when(sysInfo.hasSystemUnboundedTokenGenerationRight("Requester")).thenReturn(false);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> service.generateTokensOperation("Requester", dto, true, "origin"));

		assertEquals("Requester has no permission for unbounded token generation request", ex.getMessage());

		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(validator).validateAndNormalizeGenerateTokenRequests(dto, "origin");
		verify(sysInfo).hasSystemUnboundedTokenGenerationRight("Requester");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGenerateTokensOperationSkipAuth() {
		final AuthorizationTokenGenerationMgmtRequestDTO request = new AuthorizationTokenGenerationMgmtRequestDTO(
				"USAGE_LIMITED_TOKEN_AUTH",
				"SERVICE_DEF",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				"serviceDef",
				"operation",
				null,
				17);
		final AuthorizationTokenGenerationMgmtListRequestDTO dto = new AuthorizationTokenGenerationMgmtListRequestDTO(List.of(request));

		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeGenerateTokenRequests(dto, "origin")).thenReturn(dto);
		when(sysInfo.hasSystemUnboundedTokenGenerationRight("Requester")).thenReturn(true);
		when(tokenEngine.produce(
				"Requester",
				"TestConsumer",
				"LOCAL",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				17,
				null,
				"origin")).thenThrow(new InternalServerError("test", "origin"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.generateTokensOperation("Requester", dto, true, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(validator).validateAndNormalizeGenerateTokenRequests(dto, "origin");
		verify(sysInfo).hasSystemUnboundedTokenGenerationRight("Requester");
		verify(policyEngine, never()).isAccessGranted(any(NormalizedVerifyRequest.class));
		verify(tokenEngine).produce(
				"Requester",
				"TestConsumer",
				"LOCAL",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				17,
				null,
				"origin");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGenerateTokensOperationNoAuth() {
		final AuthorizationTokenGenerationMgmtRequestDTO request = new AuthorizationTokenGenerationMgmtRequestDTO(
				"USAGE_LIMITED_TOKEN_AUTH",
				"SERVICE_DEF",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				"serviceDef",
				"operation",
				null,
				17);
		final AuthorizationTokenGenerationMgmtListRequestDTO dto = new AuthorizationTokenGenerationMgmtListRequestDTO(List.of(request));

		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeGenerateTokenRequests(dto, "origin")).thenReturn(dto);
		when(policyEngine.isAccessGranted(any(NormalizedVerifyRequest.class))).thenReturn(false);

		AuthorizationTokenMgmtListResponseDTO result = service.generateTokensOperation("Requester", dto, false, "origin");

		assertNotNull(result);
		assertTrue(result.entries().isEmpty());
		assertEquals(0, result.count());

		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(validator).validateAndNormalizeGenerateTokenRequests(dto, "origin");
		verify(sysInfo, never()).hasSystemUnboundedTokenGenerationRight("Requester");
		verify(policyEngine).isAccessGranted(any(NormalizedVerifyRequest.class));
		verify(tokenEngine, never()).produce(
				anyString(),
				anyString(),
				anyString(),
				any(ServiceInterfacePolicy.class),
				anyString(),
				any(AuthorizationTargetType.class),
				anyString(),
				anyString(),
				anyInt(),
				isNull(),
				anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGenerateTokensOperationNoEncryption() {
		final AuthorizationTokenGenerationMgmtRequestDTO request = new AuthorizationTokenGenerationMgmtRequestDTO(
				"USAGE_LIMITED_TOKEN_AUTH",
				"SERVICE_DEF",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				"serviceDef",
				null,
				null,
				17);
		final AuthorizationTokenGenerationMgmtListRequestDTO dto = new AuthorizationTokenGenerationMgmtListRequestDTO(List.of(request));
		final TokenHeader usageLimitedToken = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);
		final TokenModel token = new TokenModel(usageLimitedToken);
		final AuthorizationTokenResponseDTO converted = new AuthorizationTokenResponseDTO(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH.name(),
				"token",
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				"2025-10-20T12:00:01Z",
				17,
				17,
				null);

		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeGenerateTokenRequests(dto, "origin")).thenReturn(dto);
		when(policyEngine.isAccessGranted(any(NormalizedVerifyRequest.class))).thenReturn(true);
		when(tokenEngine.produce(
				"Requester",
				"TestConsumer",
				"LOCAL",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				17,
				null,
				"origin")).thenReturn(token);
		when(dtoConverter.convertTokenModelToMgmtResponse(token)).thenReturn(converted);

		assertDoesNotThrow(() -> service.generateTokensOperation("Requester", dto, false, "origin"));

		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(validator).validateAndNormalizeGenerateTokenRequests(dto, "origin");
		verify(sysInfo, never()).hasSystemUnboundedTokenGenerationRight("Requester");
		verify(policyEngine).isAccessGranted(any(NormalizedVerifyRequest.class));
		verify(tokenEngine).produce(
				"Requester",
				"TestConsumer",
				"LOCAL",
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				17,
				null,
				"origin");
		verify(dtoConverter).convertTokenModelToMgmtResponse(token);
		verify(tokenEngine, never()).encryptTokenIfNeeded(token, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testGenerateTokensOperationEncryption() {
		final AuthorizationTokenGenerationMgmtRequestDTO request = new AuthorizationTokenGenerationMgmtRequestDTO(
				"BASE64_SELF_CONTAINED_TOKEN_AUTH",
				"SERVICE_DEF",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				"serviceDef",
				null,
				null,
				null);
		final AuthorizationTokenGenerationMgmtListRequestDTO dto = new AuthorizationTokenGenerationMgmtListRequestDTO(List.of(request));
		final TokenHeader tokenHeader = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);
		final TokenModel token = new TokenModel(tokenHeader);
		final AuthorizationTokenResponseDTO converted = new AuthorizationTokenResponseDTO(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH.name(),
				"token",
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				"2025-10-20T12:00:01Z",
				null,
				null,
				null);

		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeGenerateTokenRequests(dto, "origin")).thenReturn(dto);
		when(policyEngine.isAccessGranted(any(NormalizedVerifyRequest.class))).thenReturn(true);
		when(tokenEngine.produce(
				"Requester",
				"TestConsumer",
				"LOCAL",
				ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				null,
				null,
				"origin")).thenReturn(token);
		doNothing().when(tokenEngine).encryptTokenIfNeeded(token, "origin");
		when(dtoConverter.convertTokenModelToMgmtResponse(token)).thenReturn(converted);

		assertDoesNotThrow(() -> service.generateTokensOperation("Requester", dto, false, "origin"));

		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(validator).validateAndNormalizeGenerateTokenRequests(dto, "origin");
		verify(sysInfo, never()).hasSystemUnboundedTokenGenerationRight("Requester");
		verify(policyEngine).isAccessGranted(any(NormalizedVerifyRequest.class));
		verify(tokenEngine).produce(
				"Requester",
				"TestConsumer",
				"LOCAL",
				ServiceInterfacePolicy.BASE64_SELF_CONTAINED_TOKEN_AUTH,
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				null,
				null,
				"origin");
		verify(tokenEngine).encryptTokenIfNeeded(token, "origin");
		verify(dtoConverter).convertTokenModelToMgmtResponse(token);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTokensOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryTokensOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTokensOperationEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryTokensOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTokensOperationOk1() {
		final AuthorizationTokenQueryRequestDTO dto = new AuthorizationTokenQueryRequestDTO(
				null,
				"Requester",
				null,
				null,
				null,
				null,
				null,
				null);

		final PageRequest pageRequest = PageRequest.of(0, 1);
		final TokenHeader usageLimitedToken = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);
		final TokenModel token = new TokenModel(usageLimitedToken);
		final Page<TokenModel> tokens = new PageImpl<>(List.of(token));

		final AuthorizationTokenResponseDTO converted = new AuthorizationTokenResponseDTO(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH.name(),
				"token",
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				"2025-10-20T12:00:01Z",
				17,
				17,
				null);

		when(validator.validateAndNormalizeQueryTokensRequest(dto, "origin")).thenReturn(dto);
		when(pageService.getPageRequest(null, Direction.ASC, TokenHeader.SORTABLE_FIELDS_BY, TokenHeader.DEFAULT_SORT_FIELD, "origin")).thenReturn(pageRequest);
		when(tokenEngine.query(pageRequest, "Requester", null, null, null, null, null, null, "origin")).thenReturn(tokens);
		when(dtoConverter.convertTokenModelToMgmtResponse(token)).thenReturn(converted);

		assertDoesNotThrow(() -> service.queryTokensOperation(dto, "origin"));

		verify(validator).validateAndNormalizeQueryTokensRequest(dto, "origin");
		verify(pageService).getPageRequest(null, Direction.ASC, TokenHeader.SORTABLE_FIELDS_BY, TokenHeader.DEFAULT_SORT_FIELD, "origin");
		verify(tokenEngine).query(pageRequest, "Requester", null, null, null, null, null, null, "origin");
		verify(dtoConverter).convertTokenModelToMgmtResponse(token);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTokensOperationOk2() {
		final AuthorizationTokenQueryRequestDTO dto = new AuthorizationTokenQueryRequestDTO(
				null,
				"Requester",
				"USAGE_LIMITED_TOKEN",
				null,
				null,
				null,
				"SERVICE_DEF",
				null);

		final PageRequest pageRequest = PageRequest.of(0, 1);
		final TokenHeader usageLimitedToken = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);
		final TokenModel token = new TokenModel(usageLimitedToken);
		final Page<TokenModel> tokens = new PageImpl<>(List.of(token));

		final AuthorizationTokenResponseDTO converted = new AuthorizationTokenResponseDTO(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH.name(),
				"token",
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null,
				"2025-10-20T12:00:01Z",
				17,
				17,
				null);

		when(validator.validateAndNormalizeQueryTokensRequest(dto, "origin")).thenReturn(dto);
		when(pageService.getPageRequest(null, Direction.ASC, TokenHeader.SORTABLE_FIELDS_BY, TokenHeader.DEFAULT_SORT_FIELD, "origin")).thenReturn(pageRequest);
		when(tokenEngine.query(pageRequest, "Requester", AuthorizationTokenType.USAGE_LIMITED_TOKEN, null, null, null, AuthorizationTargetType.SERVICE_DEF, null, "origin")).thenReturn(tokens);
		when(dtoConverter.convertTokenModelToMgmtResponse(token)).thenReturn(converted);

		assertDoesNotThrow(() -> service.queryTokensOperation(dto, "origin"));

		verify(validator).validateAndNormalizeQueryTokensRequest(dto, "origin");
		verify(pageService).getPageRequest(null, Direction.ASC, TokenHeader.SORTABLE_FIELDS_BY, TokenHeader.DEFAULT_SORT_FIELD, "origin");
		verify(tokenEngine).query(pageRequest, "Requester", AuthorizationTokenType.USAGE_LIMITED_TOKEN, null, null, null, AuthorizationTargetType.SERVICE_DEF, null, "origin");
		verify(dtoConverter).convertTokenModelToMgmtResponse(token);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeTokensOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeTokensOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeTokensOperationEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokeTokensOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokeTokensOperationOk() {
		final List<String> refs = List.of("tokenRef");

		doNothing().when(validator).validateTokenReferences(refs, "origin");
		doNothing().when(tokenEngine).revoke(refs, "origin");

		assertDoesNotThrow(() -> service.revokeTokensOperation(refs, "origin"));

		verify(validator).validateTokenReferences(refs, "origin");
		verify(tokenEngine).revoke(refs, "origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAddEncryptionKeysOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.addEncryptionKeysOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAddEncryptionKeysOperationEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.addEncryptionKeysOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAddEncryptionKeysOperationInternalServerErrorDuringEncryption() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO(
				"TestProvider",
				"encryptionKey",
				SecretCryptographer.AES_CBC_ALGORITHM_IV_BASED);
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(List.of(request));

		when(validator.validateAndNormalizeAddEncryptionKeysRequest(dto, "origin")).thenReturn(dto);
		when(secretCryptographer.generateInitializationVectorBase64()).thenReturn("externalInitVector");
		when(sysInfo.getSecretCryptographerKey()).thenReturn("secretKey");
		when(secretCryptographer.encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey")).thenThrow(new InvalidKeyException("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.addEncryptionKeysOperation(dto, "origin"));

		assertEquals("Secret encryption failed", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeAddEncryptionKeysRequest(dto, "origin");
		verify(secretCryptographer).generateInitializationVectorBase64();
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAddEncryptionKeysOperationInternalServerErrorDuringSave() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO(
				"TestProvider",
				"encryptionKey",
				SecretCryptographer.AES_ECB_ALGORITHM);
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(List.of(request));
		final Pair<String, String> pair = Pair.of("encryptedEncryptionKey", "internalInitVector");

		when(validator.validateAndNormalizeAddEncryptionKeysRequest(dto, "origin")).thenReturn(dto);
		when(sysInfo.getSecretCryptographerKey()).thenReturn("secretKey");
		when(secretCryptographer.encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey")).thenReturn(pair);
		when(encryptionKeyDbService.save(anyList())).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.addEncryptionKeysOperation(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeAddEncryptionKeysRequest(dto, "origin");
		verify(secretCryptographer, never()).generateInitializationVectorBase64();
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey");
		verify(encryptionKeyDbService).save(anyList());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAddEncryptionKeysOperationOk() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final AuthorizationMgmtEncryptionKeyRegistrationRequestDTO request = new AuthorizationMgmtEncryptionKeyRegistrationRequestDTO(
				"TestProvider",
				"encryptionKey",
				SecretCryptographer.AES_ECB_ALGORITHM);
		final AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO dto = new AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO(List.of(request));
		final Pair<String, String> pair = Pair.of("encryptedEncryptionKey", "internalInitVector");
		final EncryptionKey saved = new EncryptionKey(
				"TestProvider",
				"encryptionKey",
				SecretCryptographer.AES_ECB_ALGORITHM,
				new CryptographerAuxiliary("internalInitVector"),
				null);
		final List<EncryptionKey> savedList = List.of(saved);

		when(validator.validateAndNormalizeAddEncryptionKeysRequest(dto, "origin")).thenReturn(dto);
		when(sysInfo.getSecretCryptographerKey()).thenReturn("secretKey");
		when(secretCryptographer.encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey")).thenReturn(pair);
		when(encryptionKeyDbService.save(anyList())).thenReturn(savedList);
		when(dtoConverter.convertEncryptionKeyListToResponse(savedList, 1)).thenReturn(new AuthorizationMgmtEncryptionKeyListResponseDTO(List.of(), 0));

		assertDoesNotThrow(() -> service.addEncryptionKeysOperation(dto, "origin"));

		verify(validator).validateAndNormalizeAddEncryptionKeysRequest(dto, "origin");
		verify(secretCryptographer, never()).generateInitializationVectorBase64();
		verify(sysInfo).getSecretCryptographerKey();
		verify(secretCryptographer).encrypt_AES_CBC_PKCS5P_IV("encryptionKey", "secretKey");
		verify(encryptionKeyDbService).save(anyList());
		verify(dtoConverter).convertEncryptionKeyListToResponse(savedList, 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveEncryptionKeyOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeEncryptionKeysOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveEncryptionKeyOperationEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.removeEncryptionKeysOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveEncryptionKeyOperationInternalServerError() {
		final List<String> param = List.of("TestProvider");

		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		doThrow(new InternalServerError("test")).when(encryptionKeyDbService).delete(param);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.removeEncryptionKeysOperation(param, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(encryptionKeyDbService).delete(param);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveEncryptionKeyOperationOk() {
		final List<String> param = List.of("TestProvider");

		when(validator.validateAndNormalizeSystemName("TestProvider", "origin")).thenReturn("TestProvider");
		doNothing().when(encryptionKeyDbService).delete(param);

		assertDoesNotThrow(() -> service.removeEncryptionKeysOperation(param, "origin"));

		verify(validator).validateAndNormalizeSystemName("TestProvider", "origin");
		verify(encryptionKeyDbService).delete(param);
	}
}