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
package eu.arrowhead.authorization.service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.CryptographerAuxiliary;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyListResponseDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenVerifyResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

public class DTOConverterTest {

	//=================================================================================================
	// members

	private final DTOConverter converter = new DTOConverter();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertAuthPolicyToDTONullPolicy() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertAuthPolicyToDTO(null));

		assertEquals("policy is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertAuthPolicyToDTOPolicyTypeNull() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertAuthPolicyToDTO(policy));

		assertEquals("policy type is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertAuthPolicyToDTOPolicyTypeAll() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		final AuthorizationPolicyDTO result = converter.convertAuthPolicyToDTO(policy);

		assertNotNull(result);
		assertEquals(AuthorizationPolicyType.ALL, result.policyType());
		assertNull(result.policyList());
		assertNull(result.policyMetadataRequirement());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertAuthPolicyToDTOPolicyTypeBlacklist() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.BLACKLIST,
				"Consumer1,   Consumer2 ");

		final AuthorizationPolicyDTO result = converter.convertAuthPolicyToDTO(policy);

		assertNotNull(result);
		assertEquals(AuthorizationPolicyType.BLACKLIST, result.policyType());
		assertNotNull(result.policyList());
		assertEquals(2, result.policyList().size());
		assertEquals("Consumer1", result.policyList().get(0));
		assertEquals("Consumer2", result.policyList().get(1));
		assertNull(result.policyMetadataRequirement());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertAuthPolicyToDTOPolicyTypeWhitelist() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.WHITELIST,
				"Consumer1");

		final AuthorizationPolicyDTO result = converter.convertAuthPolicyToDTO(policy);

		assertNotNull(result);
		assertEquals(AuthorizationPolicyType.WHITELIST, result.policyType());
		assertNotNull(result.policyList());
		assertEquals(1, result.policyList().size());
		assertEquals("Consumer1", result.policyList().get(0));
		assertNull(result.policyMetadataRequirement());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertAuthPolicyToDTOPolicyTypeSysMetadata() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.SYS_METADATA,
				"{ \"system.type\": \"test\" }");

		final AuthorizationPolicyDTO result = converter.convertAuthPolicyToDTO(policy);

		assertNotNull(result);
		assertEquals(AuthorizationPolicyType.SYS_METADATA, result.policyType());
		assertNull(result.policyList());
		assertNotNull(result.policyMetadataRequirement());
		assertTrue(result.policyMetadataRequirement().containsKey("system.type"));
		assertEquals("test", result.policyMetadataRequirement().get("system.type"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseLevelNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertPolicyToResponse(null, null));

		assertEquals("level is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseEntitiesNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertPolicyToResponse(AuthorizationLevel.PROVIDER, null));

		assertEquals("entities is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseListEmpty() {
		final Pair<? extends AuthPolicyHeader, List<AuthPolicy>> pair = Pair.of(new AuthProviderPolicyHeader(), List.of());

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertPolicyToResponse(AuthorizationLevel.PROVIDER, pair));

		assertEquals("list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseListContainsNullElement() {
		final List<AuthPolicy> list = new ArrayList<>(1);
		list.add(null);
		final Pair<? extends AuthPolicyHeader, List<AuthPolicy>> pair = Pair.of(new AuthProviderPolicyHeader(), list);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertPolicyToResponse(AuthorizationLevel.PROVIDER, pair));

		assertEquals("list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseProviderLevelNoScopedPolicy() {
		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"description");

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		final Pair<? extends AuthPolicyHeader, List<AuthPolicy>> pair = Pair.of(header, List.of(policy));

		final AuthorizationPolicyResponseDTO result = converter.convertPolicyToResponse(AuthorizationLevel.PROVIDER, pair);

		assertNotNull(result);
		assertEquals("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef", result.instanceId());
		assertEquals(AuthorizationLevel.PROVIDER, result.level());
		assertEquals("LOCAL", result.cloud());
		assertEquals("TestProvider", result.provider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
		assertEquals("serviceDef", result.target());
		assertEquals("description", result.description());
		assertNotNull(result.defaultPolicy());
		assertEquals(AuthorizationPolicyType.ALL, result.defaultPolicy().policyType());
		assertNull(result.scopedPolicies());
		assertEquals("TestProvider", result.createdBy());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertPolicyToResponseMgmtLevelWithScopedPolicy() {
		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				null,
				"CreatorSystem");

		final AuthPolicy defaultPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		final AuthPolicy scopedPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"operation",
				AuthorizationPolicyType.WHITELIST,
				"ImportantConsumer");
		final Pair<? extends AuthPolicyHeader, List<AuthPolicy>> pair = Pair.of(header, List.of(scopedPolicy, defaultPolicy));

		final AuthorizationPolicyResponseDTO result = converter.convertPolicyToResponse(AuthorizationLevel.MGMT, pair);

		assertNotNull(result);
		assertEquals("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef", result.instanceId());
		assertEquals(AuthorizationLevel.MGMT, result.level());
		assertEquals("LOCAL", result.cloud());
		assertEquals("TestProvider", result.provider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
		assertEquals("serviceDef", result.target());
		assertNull(result.description());
		assertNotNull(result.defaultPolicy());
		assertEquals(AuthorizationPolicyType.ALL, result.defaultPolicy().policyType());
		assertNotNull(result.scopedPolicies());
		assertTrue(result.scopedPolicies().containsKey("operation"));
		final AuthorizationPolicyDTO scopedDTO = result.scopedPolicies().get("operation");
		assertEquals(AuthorizationPolicyType.WHITELIST, scopedDTO.policyType());
		assertNotNull(scopedDTO.policyList());
		assertEquals("ImportantConsumer", scopedDTO.policyList().get(0));
		assertEquals("CreatorSystem", result.createdBy());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertProviderLevelPolicyPageToResponseNullPage() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertProviderLevelPolicyPageToResponse(null));

		assertEquals("page is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertProviderLevelPolicyPageToResponseOk() {
		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"description");

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		final Pair<AuthProviderPolicyHeader, List<AuthPolicy>> pair = Pair.of(header, List.of(policy));

		final AuthorizationPolicyListResponseDTO result = converter.convertProviderLevelPolicyPageToResponse(new PageImpl<>(List.of(pair)));

		assertNotNull(result);
		assertEquals(1, result.count());
		assertEquals("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef", result.entries().get(0).instanceId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertMgmtLevelPolicyListToResponseNullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertMgmtLevelPolicyListToResponse(null));

		assertEquals("policies is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertMgmtLevelPolicyListToResponseEmptyList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertMgmtLevelPolicyListToResponse(List.of()));

		assertEquals("policies is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertMgmtLevelPolicyListToResponseListContainsNullElement() {
		final List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> {
					converter.convertMgmtLevelPolicyListToResponse(list);
				});

		assertEquals("policies list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertMgmtLevelPolicyListToResponseOk() {
		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				null,
				"CreatorSystem");
		final AuthPolicy defaultPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		final Pair<AuthMgmtPolicyHeader, List<AuthPolicy>> pair = Pair.of(header, List.of(defaultPolicy));

		final AuthorizationPolicyListResponseDTO result = converter.convertMgmtLevelPolicyListToResponse(List.of(pair));

		assertNotNull(result);
		assertEquals(1, result.count());
		assertEquals("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef", result.entries().get(0).instanceId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertMgmtLevelPolicyPageToResponseNullPage() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertMgmtLevelPolicyPageToResponse(null));

		assertEquals("page is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertMgmtLevelPolicyPageToResponseOk() {
		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				null,
				"CreatorSystem");
		final AuthPolicy defaultPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		final Pair<AuthMgmtPolicyHeader, List<AuthPolicy>> pair = Pair.of(header, List.of(defaultPolicy));

		final AuthorizationPolicyListResponseDTO result = converter.convertMgmtLevelPolicyPageToResponse(new PageImpl<>(List.of(pair)));

		assertNotNull(result);
		assertEquals(1, result.count());
		assertEquals("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef", result.entries().get(0).instanceId());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertCheckResultListToResponseResultNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertCheckResultListToResponse(null));

		assertEquals("result is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertCheckResultListToResponseOk() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final AuthorizationVerifyListResponseDTO response = converter.convertCheckResultListToResponse(List.of(Pair.of(request, true)));

		assertNotNull(response);
		assertEquals(1, response.count());
		final AuthorizationVerifyResponseDTO dto = response.entries().get(0);
		assertEquals("TestProvider", dto.provider());
		assertEquals("TestConsumer", dto.consumer());
		assertEquals("LOCAL", dto.cloud());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, dto.targetType());
		assertEquals("serviceDef", dto.target());
		assertEquals("operation", dto.scope());
		assertTrue(dto.granted());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertTokenModelToResponseNullModel() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertTokenModelToResponse(null));

		assertEquals("TokenModel is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertTokenModelToResponseOk1() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		final UsageLimitedToken token = new UsageLimitedToken(header, 7);
		final TokenModel model = new TokenModel(token, "rawToken");
		model.setEncryptedToken("encryptedToken");

		final AuthorizationTokenGenerationResponseDTO result = converter.convertTokenModelToResponse(model);

		assertNotNull(result);
		assertEquals(AuthorizationTokenType.USAGE_LIMITED_TOKEN, result.tokenType());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
		assertEquals("encryptedToken", result.token());
		assertEquals(7, result.usageLimit());
		assertNull(result.expiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertTokenModelToResponseOk2() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		final ZonedDateTime expiry = ZonedDateTime.of(2125, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC)); // this test only works for 100 years, sorry future developers :)
		final TimeLimitedToken token = new TimeLimitedToken(header, expiry);
		final TokenModel model = new TokenModel(token, "rawToken");

		final AuthorizationTokenGenerationResponseDTO result = converter.convertTokenModelToResponse(model);

		assertNotNull(result);
		assertEquals(AuthorizationTokenType.TIME_LIMITED_TOKEN, result.tokenType());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
		assertEquals("rawToken", result.token());
		assertNull(result.usageLimit());
		assertEquals("2125-10-14T10:12:12Z", result.expiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertTokenModelToMgmtResponseNullModel() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertTokenModelToMgmtResponse(null));

		assertEquals("TokenModel is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertTokenModelToMgmtResponseOk1() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setCreatedAt(ZonedDateTime.of(2025, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC)));
		final UsageLimitedToken token = new UsageLimitedToken(header, 7);
		token.setUsageLeft(6);
		final TokenModel model = new TokenModel(token, "rawToken");
		model.setEncryptedToken("encryptedToken");

		final AuthorizationTokenResponseDTO result = converter.convertTokenModelToMgmtResponse(model);

		assertNotNull(result);
		assertEquals(AuthorizationTokenType.USAGE_LIMITED_TOKEN, result.tokenType());
		assertEquals(ServiceInterfacePolicy.USAGE_LIMITED_TOKEN_AUTH.name(), result.variant());
		assertEquals("encryptedToken", result.token());
		assertEquals("tokenHash", result.tokenReference());
		assertEquals("Requester", result.requester());
		assertEquals("LOCAL", result.consumerCloud());
		assertEquals("TestConsumer", result.consumer());
		assertEquals("TestProvider", result.provider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
		assertEquals("serviceDef", result.target());
		assertEquals("operation", result.scope());
		assertEquals("2025-10-14T10:12:12Z", result.createdAt());
		assertEquals(7, result.usageLimit());
		assertEquals(6, result.usageLeft());
		assertNull(result.expiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertTokenModelToMgmtResponseOk2() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setCreatedAt(ZonedDateTime.of(2025, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC)));
		final ZonedDateTime expiry = ZonedDateTime.of(2125, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC)); // this test only works for 100 years, sorry future developers :)
		final TimeLimitedToken token = new TimeLimitedToken(header, expiry);
		final TokenModel model = new TokenModel(token, "rawToken");

		final AuthorizationTokenResponseDTO result = converter.convertTokenModelToMgmtResponse(model);

		assertNotNull(result);
		assertEquals(AuthorizationTokenType.TIME_LIMITED_TOKEN, result.tokenType());
		assertEquals(ServiceInterfacePolicy.TIME_LIMITED_TOKEN_AUTH.name(), result.variant());
		assertEquals("rawToken", result.token());
		assertEquals("tokenHash", result.tokenReference());
		assertEquals("Requester", result.requester());
		assertEquals("LOCAL", result.consumerCloud());
		assertEquals("TestConsumer", result.consumer());
		assertEquals("TestProvider", result.provider());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
		assertEquals("serviceDef", result.target());
		assertEquals("operation", result.scope());
		assertEquals("2025-10-14T10:12:12Z", result.createdAt());
		assertNull(result.usageLimit());
		assertNull(result.usageLeft());
		assertEquals("2125-10-14T10:12:12Z", result.expiresAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertTokenVerificationResultToResponseNullPair() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertTokenVerificationResultToResponse(null));

		assertEquals("pair is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertTokenVerificationResultToResponseNotVerified() {
		final AuthorizationTokenVerifyResponseDTO result = converter.convertTokenVerificationResultToResponse(Pair.of(false, Optional.empty()));

		assertNotNull(result);
		assertFalse(result.verified());
		assertNull(result.consumerCloud());
		assertNull(result.consumer());
		assertNull(result.targetType());
		assertNull(result.target());
		assertNull(result.scope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertTokenVerificationResultToResponseVerified() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		final UsageLimitedToken token = new UsageLimitedToken(header, 7);
		final TokenModel model = new TokenModel(token, "rawToken");

		final AuthorizationTokenVerifyResponseDTO result = converter.convertTokenVerificationResultToResponse(Pair.of(true, Optional.of(model)));

		assertNotNull(result);
		assertTrue(result.verified());
		assertEquals("LOCAL", result.consumerCloud());
		assertEquals("TestConsumer", result.consumer());
		assertEquals(AuthorizationTargetType.SERVICE_DEF, result.targetType());
		assertEquals("serviceDef", result.target());
		assertEquals("operation", result.scope());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertEncryptionKeyToResponseNullKey() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertEncryptionKeyToResponse(null));

		assertEquals("EncryptionKey is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertEncryptionKeyToResponseOk1() {
		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"theKey",
				"alg",
				new CryptographerAuxiliary("internal"),
				null);
		key.setCreatedAt(ZonedDateTime.of(2025, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC)));

		final AuthorizationMgmtEncryptionKeyResponseDTO result = converter.convertEncryptionKeyToResponse(key);

		assertNotNull(result);
		assertEquals("TestProvider", result.systemName());
		assertEquals("theKey", result.rawKey());
		assertEquals("alg", result.algorithm());
		assertNull(result.keyAdditive());
		assertEquals("2025-10-14T10:12:12Z", result.createdAt());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertEncryptionKeyToResponseOk2() {
		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"theKey",
				"alg",
				new CryptographerAuxiliary("internal"),
				new CryptographerAuxiliary("external"));
		key.setCreatedAt(ZonedDateTime.of(2025, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC)));

		final AuthorizationMgmtEncryptionKeyResponseDTO result = converter.convertEncryptionKeyToResponse(key);

		assertNotNull(result);
		assertEquals("TestProvider", result.systemName());
		assertEquals("theKey", result.rawKey());
		assertEquals("alg", result.algorithm());
		assertEquals("external", result.keyAdditive());
		assertEquals("2025-10-14T10:12:12Z", result.createdAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertEncryptionKeyListToResponseNullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertEncryptionKeyListToResponse(null, 0));

		assertEquals("EncryptionKey list is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testConvertEncryptionKeyListToResponseOk() {
		final EncryptionKey key = new EncryptionKey(
				"TestProvider",
				"theKey",
				"alg",
				new CryptographerAuxiliary("internal"),
				null);
		key.setCreatedAt(ZonedDateTime.of(2025, 10, 14, 10, 12, 12, 0, ZoneId.of(Constants.UTC)));

		final AuthorizationMgmtEncryptionKeyListResponseDTO result = converter.convertEncryptionKeyListToResponse(List.of(key), 1);

		assertNotNull(result);
		assertEquals(1, result.count());
		final AuthorizationMgmtEncryptionKeyResponseDTO keyResult = result.entries().get(0);
		assertEquals("TestProvider", keyResult.systemName());
		assertEquals("theKey", keyResult.rawKey());
		assertEquals("alg", keyResult.algorithm());
		assertNull(keyResult.keyAdditive());
		assertEquals("2025-10-14T10:12:12Z", keyResult.createdAt());
	}
}