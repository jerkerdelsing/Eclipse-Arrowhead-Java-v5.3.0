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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.service.AuthorizationPolicyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.AuthorizationPolicyDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.SystemListResponseDTO;
import eu.arrowhead.dto.SystemLookupRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@ExtendWith(MockitoExtension.class)
public class AuthorizationPolicyEngineTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationPolicyEngine engine;

	@Mock
	private AuthorizationPolicyDbService dbService;

	@Mock
	private ArrowheadHttpService httpService;

	@Mock
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAccessGrantedNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> engine.isAccessGranted(null));

		assertEquals("request is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAccessGrantedNoRules() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(Optional.empty());
		when(dbService.findRelevantPolicies(AuthorizationLevel.PROVIDER, request)).thenReturn(Optional.empty());

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(dbService).findRelevantPolicies(AuthorizationLevel.PROVIDER, request);

		assertFalse(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAccessGrantedRuleTypeAll() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.ALL, null, null);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertTrue(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAccessGrantedRuleTypeBlacklistTrue() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"op",
				AuthorizationPolicyType.BLACKLIST,
				"BadSystem1,BadSystem2");
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.BLACKLIST, List.of("BadSystem1", "BadSystem2"), null);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertTrue(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAccessGrantedRuleTypeBlacklistFalse() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"op",
				AuthorizationPolicyType.BLACKLIST,
				"BadSystem1,BadSystem2,ConsumerName");
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.BLACKLIST, List.of("BadSystem1", "BadSystem2", "ConsumerName"), null);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertFalse(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAccessGrantedRuleTypeWhitelistTrue() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"op",
				AuthorizationPolicyType.WHITELIST,
				"ConsumerName");
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.WHITELIST, List.of("ConsumerName"), null);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertTrue(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAccessGrantedRuleTypeWhitelistFalse() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"op",
				AuthorizationPolicyType.WHITELIST,
				"TrustedSystem1,TrustedSystem2");
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.WHITELIST, List.of("TrustedSystem1,TrustedSystem2"), null);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertFalse(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testIsAccessGrantedRuleTypeSysMetadataExceptionDuringHTTP() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"op",
				AuthorizationPolicyType.SYS_METADATA,
				"{\"key\": \"value\"}");
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", "value");
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.SYS_METADATA, null, req);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(httpService.consumeService(eq("systemDiscovery"), eq("lookup"), eq(SystemListResponseDTO.class), any(SystemLookupRequestDTO.class), any(MultiValueMap.class)))
				.thenThrow(ArrowheadException.class);
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(httpService).consumeService(eq("systemDiscovery"), eq("lookup"), eq(SystemListResponseDTO.class), any(SystemLookupRequestDTO.class), any(MultiValueMap.class));
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertFalse(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testIsAccessGrantedRuleTypeSysMetadataUnknownConsumer() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"op",
				AuthorizationPolicyType.SYS_METADATA,
				"{\"key\": \"value\"}");
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", "value");
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.SYS_METADATA, null, req);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(httpService.consumeService(eq("systemDiscovery"), eq("lookup"), eq(SystemListResponseDTO.class), any(SystemLookupRequestDTO.class), any(MultiValueMap.class)))
				.thenReturn(new SystemListResponseDTO(List.of(), 0));
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(httpService).consumeService(eq("systemDiscovery"), eq("lookup"), eq(SystemListResponseDTO.class), any(SystemLookupRequestDTO.class), any(MultiValueMap.class));
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertFalse(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testIsAccessGrantedRuleTypeSysMetadataNotMatchingConsumer() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"op",
				AuthorizationPolicyType.SYS_METADATA,
				"{\"key\": \"value\"}");
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", "value");
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.SYS_METADATA, null, req);
		final SystemListResponseDTO response = new SystemListResponseDTO(
				List.of(new SystemResponseDTO(
						"ConsumerName",
						Map.of(),
						"1.0.0",
						List.of(new AddressDTO("HOSTNAME", "localhost")),
						null,
						"2025-08-06T10:00:00Z",
						"2025-08-06T10:00:00Z")),
				1);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(httpService.consumeService(eq("systemDiscovery"), eq("lookup"), eq(SystemListResponseDTO.class), any(SystemLookupRequestDTO.class), any(MultiValueMap.class)))
				.thenReturn(response);
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(httpService).consumeService(eq("systemDiscovery"), eq("lookup"), eq(SystemListResponseDTO.class), any(SystemLookupRequestDTO.class), any(MultiValueMap.class));
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertFalse(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testIsAccessGrantedRuleTypeSysMetadataMatchingConsumer() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"op",
				AuthorizationPolicyType.SYS_METADATA,
				"{\"key\": \"value\"}");
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", "value");
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.SYS_METADATA, null, req);
		final SystemListResponseDTO response = new SystemListResponseDTO(
				List.of(new SystemResponseDTO(
						"ConsumerName",
						Map.of("key", "value"),
						"1.0.0",
						List.of(new AddressDTO("HOSTNAME", "localhost")),
						null,
						"2025-08-06T10:00:00Z",
						"2025-08-06T10:00:00Z")),
				1);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(httpService.consumeService(eq("systemDiscovery"), eq("lookup"), eq(SystemListResponseDTO.class), any(SystemLookupRequestDTO.class), any(MultiValueMap.class)))
				.thenReturn(response);
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final boolean granted = engine.isAccessGranted(request);

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(httpService).consumeService(eq("systemDiscovery"), eq("lookup"), eq(SystemListResponseDTO.class), any(SystemLookupRequestDTO.class), any(MultiValueMap.class));
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertTrue(granted);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckAccessEmptyList() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> engine.checkAccess(List.of()));

		assertEquals("Request list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckAccessNullElement() {
		final List<NormalizedVerifyRequest> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> engine.checkAccess(list));

		assertEquals("Request list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckAccessRuleTypeAll() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"ProviderName",
				"ConsumerName",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"op");
		final AuthPolicy authPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> opt = Optional.of(Pair.of(
				new AuthMgmtPolicyHeader(
						"MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						AuthorizationTargetType.SERVICE_DEF,
						"LOCAL",
						"ProviderName",
						"testService",
						null,
						"TestSystem"),
				List.of(authPolicy)));
		final AuthorizationPolicyDTO authPolicyDTO = new AuthorizationPolicyDTO(AuthorizationPolicyType.ALL, null, null);

		when(dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request)).thenReturn(opt);
		when(dtoConverter.convertAuthPolicyToDTO(authPolicy)).thenReturn(authPolicyDTO);

		final List<Pair<NormalizedVerifyRequest, Boolean>> result = engine.checkAccess(List.of(request));

		verify(dbService).findRelevantPolicies(AuthorizationLevel.MGMT, request);
		verify(dtoConverter).convertAuthPolicyToDTO(authPolicy);
		verify(dbService, never()).findRelevantPolicies(eq(AuthorizationLevel.PROVIDER), any(NormalizedVerifyRequest.class));

		assertEquals(1, result.size());
		assertEquals(request, result.get(0).getFirst());
		assertTrue(result.get(0).getSecond());
	}
}