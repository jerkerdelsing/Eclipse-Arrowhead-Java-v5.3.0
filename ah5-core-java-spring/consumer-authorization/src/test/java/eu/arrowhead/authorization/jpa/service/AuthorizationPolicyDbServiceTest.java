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
package eu.arrowhead.authorization.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Pair;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.type.TypeReference;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.repository.AuthMgmtPolicyHeaderRepository;
import eu.arrowhead.authorization.jpa.repository.AuthPolicyRepository;
import eu.arrowhead.authorization.jpa.repository.AuthProviderPolicyHeaderRepository;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedLookupRequest;
import eu.arrowhead.authorization.service.dto.NormalizedQueryRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@ExtendWith(MockitoExtension.class)
public class AuthorizationPolicyDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationPolicyDbService dbService;

	@Mock
	private AuthProviderPolicyHeaderRepository providerHeaderRepository;

	@Mock
	private AuthMgmtPolicyHeaderRepository mgmtHeaderRepository;

	@Mock
	private AuthPolicyRepository authPolicyRepository;

	@Spy
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateProviderLevelPolicyRequestNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createProviderLevelPolicy(null));

		assertEquals("request is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateProviderLevelPolicyInternalServerError() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenThrow(new RuntimeException());

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.createProviderLevelPolicy(request));

		assertEquals("Database operation error", ex.getMessage());

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateProviderLevelPolicyExistingCombinationButNotTheSame1() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");
		request.setDescription("desc");
		request.setPolicies(Map.of(
				"*", new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null),
				"operation", new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.WHITELIST, List.of("SpecialConsumer"), null)));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1)).thenReturn(List.of(policy));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.createProviderLevelPolicy(request));

		assertEquals("A provider level policy is already existing with the specified cloud / provider / target type / target combination, but the policy details are different", ex.getMessage());

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateProviderLevelPolicyExistingCombinationButNotTheSame2() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");
		request.setDescription("desc");
		request.setPolicies(Map.of(
				"*", new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null),
				"operation", new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.WHITELIST, List.of("SpecialConsumer"), null)));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"other desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		final AuthPolicy policy2 = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"operation2",
				AuthorizationPolicyType.BLACKLIST,
				"BadSystem");

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1)).thenReturn(List.of(policy2, policy));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.createProviderLevelPolicy(request));

		assertEquals("A provider level policy is already existing with the specified cloud / provider / target type / target combination, but the policy details are different", ex.getMessage());

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateProviderLevelPolicyExistingCombinationButNotTheSame3() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");
		request.setDescription("desc");
		request.setPolicies(Map.of(
				"*", new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.WHITELIST, List.of("SpecialConsumer"), null)));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"other desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1)).thenReturn(List.of(policy));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.createProviderLevelPolicy(request));

		assertEquals("A provider level policy is already existing with the specified cloud / provider / target type / target combination, but the policy details are different", ex.getMessage());

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateProviderLevelPolicyExistingCombinationNoDescriptionChange() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");
		request.setDescription("desc");
		request.setPolicies(Map.of(
				"*", new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null)));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1)).thenReturn(List.of(policy));

		final Pair<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>, Boolean> result = dbService.createProviderLevelPolicy(request);

		assertEquals(header, result.getFirst().getFirst());
		assertEquals(1, result.getFirst().getSecond().size());
		assertEquals(policy, result.getFirst().getSecond().get(0));
		assertFalse(result.getSecond());

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateProviderLevelPolicyExistingCombinationWithDescriptionChange() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");
		request.setDescription("desc");
		request.setPolicies(Map.of(
				"*", new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null)));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"old desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1)).thenReturn(List.of(policy));
		when(providerHeaderRepository.saveAndFlush(header)).thenReturn(header);

		final Pair<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>, Boolean> result = dbService.createProviderLevelPolicy(request);

		assertEquals(header, result.getFirst().getFirst());
		assertEquals("desc", result.getFirst().getFirst().getDescription());
		assertEquals(1, result.getFirst().getSecond().size());
		assertEquals(policy, result.getFirst().getSecond().get(0));
		assertFalse(result.getSecond());

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		verify(providerHeaderRepository).saveAndFlush(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateProviderLevelPolicyNotExistingCombination() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.PROVIDER);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");
		request.setDescription("desc");
		request.setPolicies(Map.of(
				"*", new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null)));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");

		final AuthProviderPolicyHeader savedHeader = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		savedHeader.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		final AuthPolicy savedPolicy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		savedPolicy.setId(1);

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.empty());
		when(providerHeaderRepository.saveAndFlush(header)).thenReturn(savedHeader);
		when(authPolicyRepository.saveAllAndFlush(List.of(policy))).thenReturn(List.of(savedPolicy));

		final Pair<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>, Boolean> result = dbService.createProviderLevelPolicy(request);

		assertEquals(savedHeader, result.getFirst().getFirst());
		assertEquals(1, result.getFirst().getSecond().size());
		assertEquals(savedPolicy, result.getFirst().getSecond().get(0));
		assertTrue(result.getSecond());

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		verify(providerHeaderRepository).saveAndFlush(header);
		verify(authPolicyRepository).saveAllAndFlush(List.of(policy));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteProviderLevelPolicyByInstanceIdNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deleteProviderLevelPolicyByInstanceId(null));

		assertEquals("instanceId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteProviderLevelPolicyByInstanceIdEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deleteProviderLevelPolicyByInstanceId(" "));

		assertEquals("instanceId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteProviderLevelPolicyByInstanceIdInternalServerError() {
		final String instanceId = "PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef";

		when(providerHeaderRepository.findByInstanceId(instanceId)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.deleteProviderLevelPolicyByInstanceId(instanceId));

		assertEquals("Database operation error", ex.getMessage());

		verify(providerHeaderRepository).findByInstanceId(instanceId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteProviderLevelPolicyByInstanceIdUnknownInstanceId() {
		final String instanceId = "PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef";

		when(providerHeaderRepository.findByInstanceId(instanceId)).thenReturn(Optional.empty());

		final boolean result = dbService.deleteProviderLevelPolicyByInstanceId(instanceId);

		assertFalse(result);

		verify(providerHeaderRepository).findByInstanceId(instanceId);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteProviderLevelPolicyByInstanceIdOk() {
		final String instanceId = "PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef";

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		when(providerHeaderRepository.findByInstanceId(instanceId)).thenReturn(Optional.of(header));
		doNothing().when(providerHeaderRepository).delete(header);
		doNothing().when(providerHeaderRepository).flush();
		doNothing().when(authPolicyRepository).deleteByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		doNothing().when(authPolicyRepository).flush();

		final boolean result = dbService.deleteProviderLevelPolicyByInstanceId(instanceId);

		assertTrue(result);

		verify(providerHeaderRepository).findByInstanceId(instanceId);
		verify(providerHeaderRepository).delete(header);
		verify(providerHeaderRepository).flush();
		verify(authPolicyRepository).deleteByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		verify(authPolicyRepository).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersPaginationNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getProviderLevelPoliciesByFilters(null, null));

		assertEquals("pagination is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersInternalServerError() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		when(providerHeaderRepository.findAll(pageRequest)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getProviderLevelPoliciesByFilters(pageRequest, null));

		assertEquals("Database operation error", ex.getMessage());

		verify(providerHeaderRepository).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersNoFilterOk() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"old desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		when(providerHeaderRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(header)));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1)).thenReturn(List.of(policy));

		final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(pageRequest, new NormalizedLookupRequest());

		assertEquals(1, result.getSize());
		final Pair<AuthProviderPolicyHeader, List<AuthPolicy>> pair = result.getContent().get(0);
		assertEquals(header, pair.getFirst());
		assertEquals(1, pair.getSecond().size());
		assertEquals(policy, pair.getSecond().get(0));

		verify(providerHeaderRepository).findAll(pageRequest);
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersInstanceIdBaseTargetNotMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedLookupRequest request = new NormalizedLookupRequest();
		request.setInstanceIds(List.of("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		request.setTargetNames(List.of("otherServiceDef"));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		when(providerHeaderRepository.findByInstanceIdIn(List.of("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef"))).thenReturn(List.of(header));
		when(providerHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(providerHeaderRepository).findByInstanceIdIn(List.of("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		verify(providerHeaderRepository, never()).findByTargetIn(anyList());
		verify(providerHeaderRepository, never()).findByCloudIn(anyList());
		verify(providerHeaderRepository, never()).findByProviderIn(anyList());
		verify(providerHeaderRepository, never()).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.PROVIDER), anyLong());
		verify(providerHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersTargetBaseCloudNotMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedLookupRequest request = new NormalizedLookupRequest();
		request.setTargetNames(List.of("serviceDef"));
		request.setCloudIdentifiers(List.of("TestCloud|Company"));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		when(providerHeaderRepository.findByTargetIn(List.of("serviceDef"))).thenReturn(List.of(header));
		when(providerHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(providerHeaderRepository, never()).findByInstanceIdIn(anyList());
		verify(providerHeaderRepository).findByTargetIn(List.of("serviceDef"));
		verify(providerHeaderRepository, never()).findByCloudIn(anyList());
		verify(providerHeaderRepository, never()).findByProviderIn(anyList());
		verify(providerHeaderRepository, never()).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.PROVIDER), anyLong());
		verify(providerHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersCloudBaseProviderNotMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedLookupRequest request = new NormalizedLookupRequest();
		request.setCloudIdentifiers(List.of("LOCAL"));
		request.setProviders(List.of("OtherProvider"));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		when(providerHeaderRepository.findByCloudIn(List.of("LOCAL"))).thenReturn(List.of(header));
		when(providerHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(providerHeaderRepository, never()).findByInstanceIdIn(anyList());
		verify(providerHeaderRepository, never()).findByTargetIn(anyList());
		verify(providerHeaderRepository).findByCloudIn(List.of("LOCAL"));
		verify(providerHeaderRepository, never()).findByProviderIn(anyList());
		verify(providerHeaderRepository, never()).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.PROVIDER), anyLong());
		verify(providerHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersProviderBaseTargetTypeNotMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedLookupRequest request = new NormalizedLookupRequest();
		request.setProviders(List.of("TestProvider"));
		request.setTargetType(AuthorizationTargetType.EVENT_TYPE);

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		when(providerHeaderRepository.findByProviderIn(List.of("TestProvider"))).thenReturn(List.of(header));
		when(providerHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(providerHeaderRepository, never()).findByInstanceIdIn(anyList());
		verify(providerHeaderRepository, never()).findByTargetIn(anyList());
		verify(providerHeaderRepository, never()).findByCloudIn(anyList());
		verify(providerHeaderRepository).findByProviderIn(List.of("TestProvider"));
		verify(providerHeaderRepository, never()).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.PROVIDER), anyLong());
		verify(providerHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersNoneBase() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedLookupRequest request = new NormalizedLookupRequest();
		request.setTargetType(AuthorizationTargetType.EVENT_TYPE);

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		when(providerHeaderRepository.findAll()).thenReturn(List.of(header));
		when(providerHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(providerHeaderRepository, never()).findByInstanceIdIn(anyList());
		verify(providerHeaderRepository, never()).findByTargetIn(anyList());
		verify(providerHeaderRepository, never()).findByCloudIn(anyList());
		verify(providerHeaderRepository, never()).findByProviderIn(anyList());
		verify(providerHeaderRepository).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.PROVIDER), anyLong());
		verify(providerHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersAllMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedLookupRequest request = new NormalizedLookupRequest();
		request.setInstanceIds(List.of("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setCloudIdentifiers(List.of("LOCAL"));
		request.setProviders(List.of("TestProvider"));
		request.setTargetNames(List.of("serviceDef"));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		policy.setId(1);

		when(providerHeaderRepository.findByInstanceIdIn(List.of("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef"))).thenReturn(List.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1)).thenReturn(List.of(policy));
		when(providerHeaderRepository.findAllByIdIn(Set.of(1L), pageRequest)).thenReturn(new PageImpl<>(List.of(header)));

		final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(pageRequest, request);

		assertFalse(result.isEmpty());
		final Pair<AuthProviderPolicyHeader, List<AuthPolicy>> pair = result.getContent().get(0);
		assertEquals(header, pair.getFirst());
		assertFalse(pair.getSecond().isEmpty());
		assertEquals(policy, pair.getSecond().get(0));

		verify(providerHeaderRepository).findByInstanceIdIn(List.of("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		verify(providerHeaderRepository, never()).findByTargetIn(anyList());
		verify(providerHeaderRepository, never()).findByCloudIn(anyList());
		verify(providerHeaderRepository, never()).findByProviderIn(anyList());
		verify(providerHeaderRepository, never()).findAll();
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		verify(providerHeaderRepository).findAllByIdIn(Set.of(1L), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetProviderLevelPoliciesByFiltersNoPolicy() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedLookupRequest request = new NormalizedLookupRequest();
		request.setInstanceIds(List.of("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		when(providerHeaderRepository.findByInstanceIdIn(List.of("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef"))).thenReturn(List.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1)).thenReturn(List.of());
		when(providerHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> result = dbService.getProviderLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(providerHeaderRepository).findByInstanceIdIn(List.of("PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		verify(providerHeaderRepository, never()).findByTargetIn(anyList());
		verify(providerHeaderRepository, never()).findByCloudIn(anyList());
		verify(providerHeaderRepository, never()).findByProviderIn(anyList());
		verify(providerHeaderRepository, never()).findAll();
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		verify(providerHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersPaginationNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getMgmtLevelPoliciesByFilters(null, null));

		assertEquals("pagination is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersRequestNull() {
		final PageRequest pageRequest = PageRequest.of(0, 10);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getMgmtLevelPoliciesByFilters(pageRequest, null));

		assertEquals("request is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersProviderLevelRequest() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.PROVIDER);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getMgmtLevelPoliciesByFilters(pageRequest, request));

		assertEquals("Can't use this method for provider level filtering", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersInternalServerError() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);

		when(mgmtHeaderRepository.findAll(pageRequest)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getMgmtLevelPoliciesByFilters(pageRequest, request));

		assertEquals("Database operation error", ex.getMessage());

		verify(mgmtHeaderRepository).findAll(pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersNoFilterOk() {
		final PageRequest pageRequest = PageRequest.of(0, 1);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"old desc",
				"AdminSystem");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		when(mgmtHeaderRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(header)));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1)).thenReturn(List.of(policy));

		final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, request);

		assertEquals(1, result.getSize());
		final Pair<AuthMgmtPolicyHeader, List<AuthPolicy>> pair = result.getContent().get(0);
		assertEquals(header, pair.getFirst());
		assertEquals(1, pair.getSecond().size());
		assertEquals(policy, pair.getSecond().get(0));

		verify(mgmtHeaderRepository).findAll(pageRequest);
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersInstanceIdBaseProviderNotMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);
		request.setInstanceIds(List.of("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		request.setProviders(List.of("OtherTestProvider"));

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		when(mgmtHeaderRepository.findByInstanceIdIn(List.of("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef"))).thenReturn(List.of(header));
		when(mgmtHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(mgmtHeaderRepository).findByInstanceIdIn(List.of("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		verify(mgmtHeaderRepository, never()).findByTargetIn(anyList());
		verify(mgmtHeaderRepository, never()).findByCloudIn(anyList());
		verify(mgmtHeaderRepository, never()).findByProviderIn(anyList());
		verify(mgmtHeaderRepository, never()).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.MGMT), anyLong());
		verify(mgmtHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersTargetBaseCloudNotMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);
		request.setTargetNames(List.of("serviceDef"));
		request.setCloudIdentifiers(List.of("TestCloud|Company"));

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		when(mgmtHeaderRepository.findByTargetIn(List.of("serviceDef"))).thenReturn(List.of(header));
		when(mgmtHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(mgmtHeaderRepository, never()).findByInstanceIdIn(anyList());
		verify(mgmtHeaderRepository).findByTargetIn(List.of("serviceDef"));
		verify(mgmtHeaderRepository, never()).findByCloudIn(anyList());
		verify(mgmtHeaderRepository, never()).findByProviderIn(anyList());
		verify(mgmtHeaderRepository, never()).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.MGMT), anyLong());
		verify(mgmtHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersCloudBaseTargetTypeNotMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);
		request.setCloudIdentifiers(List.of("LOCAL"));
		request.setTargetType(AuthorizationTargetType.EVENT_TYPE);

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		when(mgmtHeaderRepository.findByCloudIn(List.of("LOCAL"))).thenReturn(List.of(header));
		when(mgmtHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(mgmtHeaderRepository, never()).findByInstanceIdIn(anyList());
		verify(mgmtHeaderRepository, never()).findByTargetIn(anyList());
		verify(mgmtHeaderRepository).findByCloudIn(List.of("LOCAL"));
		verify(mgmtHeaderRepository, never()).findByProviderIn(anyList());
		verify(mgmtHeaderRepository, never()).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.MGMT), anyLong());
		verify(mgmtHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersProviderBaseTargetNotMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);
		request.setProviders(List.of("TestProvider"));
		request.setTargetNames(List.of("otherServiceDef"));

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		when(mgmtHeaderRepository.findByProviderIn(List.of("TestProvider"))).thenReturn(List.of(header));
		when(mgmtHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(mgmtHeaderRepository, never()).findByInstanceIdIn(anyList());
		verify(mgmtHeaderRepository, never()).findByTargetIn(anyList());
		verify(mgmtHeaderRepository, never()).findByCloudIn(anyList());
		verify(mgmtHeaderRepository).findByProviderIn(List.of("TestProvider"));
		verify(mgmtHeaderRepository, never()).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.MGMT), anyLong());
		verify(mgmtHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersNoneBase() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);
		request.setTargetType(AuthorizationTargetType.EVENT_TYPE);

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		when(mgmtHeaderRepository.findAll()).thenReturn(List.of(header));
		when(mgmtHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(mgmtHeaderRepository, never()).findByInstanceIdIn(anyList());
		verify(mgmtHeaderRepository, never()).findByTargetIn(anyList());
		verify(mgmtHeaderRepository, never()).findByCloudIn(anyList());
		verify(mgmtHeaderRepository, never()).findByProviderIn(anyList());
		verify(mgmtHeaderRepository).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.MGMT), anyLong());
		verify(mgmtHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersAllMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);
		request.setInstanceIds(List.of("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setCloudIdentifiers(List.of("LOCAL"));
		request.setProviders(List.of("TestProvider"));
		request.setTargetNames(List.of("serviceDef"));

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		policy.setId(1);

		when(mgmtHeaderRepository.findByInstanceIdIn(List.of("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef"))).thenReturn(List.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1)).thenReturn(List.of(policy));
		when(mgmtHeaderRepository.findAllByIdIn(Set.of(1L), pageRequest)).thenReturn(new PageImpl<>(List.of(header)));

		final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, request);

		assertFalse(result.isEmpty());
		final Pair<AuthMgmtPolicyHeader, List<AuthPolicy>> pair = result.getContent().get(0);
		assertEquals(header, pair.getFirst());
		assertFalse(pair.getSecond().isEmpty());
		assertEquals(policy, pair.getSecond().get(0));

		verify(mgmtHeaderRepository).findByInstanceIdIn(List.of("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		verify(mgmtHeaderRepository, never()).findByTargetIn(anyList());
		verify(mgmtHeaderRepository, never()).findByCloudIn(anyList());
		verify(mgmtHeaderRepository, never()).findByProviderIn(anyList());
		verify(mgmtHeaderRepository, never()).findAll();
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1);
		verify(mgmtHeaderRepository).findAllByIdIn(Set.of(1L), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersNoPolicy() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);
		request.setInstanceIds(List.of("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		when(mgmtHeaderRepository.findByInstanceIdIn(List.of("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef"))).thenReturn(List.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1)).thenReturn(List.of());
		when(mgmtHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(mgmtHeaderRepository).findByInstanceIdIn(List.of("MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef"));
		verify(mgmtHeaderRepository, never()).findByTargetIn(anyList());
		verify(mgmtHeaderRepository, never()).findByCloudIn(anyList());
		verify(mgmtHeaderRepository, never()).findByProviderIn(anyList());
		verify(mgmtHeaderRepository, never()).findAll();
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1);
		verify(mgmtHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetMgmtLevelPoliciesByFiltersProviderBaseTargetTypeNotMacth() {
		final PageRequest pageRequest = PageRequest.of(0, 10);
		final NormalizedQueryRequest request = new NormalizedQueryRequest();
		request.setLevel(AuthorizationLevel.MGMT);
		request.setProviders(List.of("TestProvider"));
		request.setTargetType(AuthorizationTargetType.EVENT_TYPE);

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		when(mgmtHeaderRepository.findByProviderIn(List.of("TestProvider"))).thenReturn(List.of(header));
		when(mgmtHeaderRepository.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.getMgmtLevelPoliciesByFilters(pageRequest, request);

		assertTrue(result.isEmpty());

		verify(mgmtHeaderRepository, never()).findByInstanceIdIn(anyList());
		verify(mgmtHeaderRepository, never()).findByTargetIn(anyList());
		verify(mgmtHeaderRepository, never()).findByCloudIn(anyList());
		verify(mgmtHeaderRepository).findByProviderIn(List.of("TestProvider"));
		verify(mgmtHeaderRepository, never()).findAll();
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(eq(AuthorizationLevel.MGMT), anyLong());
		verify(mgmtHeaderRepository).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesLevelNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findRelevantPolicies(null, null));

		assertEquals("level is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesRequestNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findRelevantPolicies(AuthorizationLevel.MGMT, null));

		assertEquals("request is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesInternalServerError() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request));

		assertEquals("Database operation error", ex.getMessage());

		verify(mgmtHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesNoRelevantMgmtPolicies() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.empty());

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request);

		assertTrue(result.isEmpty());

		verify(mgmtHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(providerHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesMgmtNoScopeReq() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		policy.setId(1);

		when(mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1)).thenReturn(List.of(policy));

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request);

		assertFalse(result.isEmpty());
		assertEquals(header, result.get().getFirst());
		assertFalse(result.get().getSecond().isEmpty());
		assertEquals(policy, result.get().getSecond().get(0));

		verify(mgmtHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(providerHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1);
		verify(authPolicyRepository, never()).findByLevelAndHeaderIdAndScopeIn(eq(AuthorizationLevel.MGMT), eq(1L), anySet());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesMgmtScopeReqZombie() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		when(mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.MGMT, 1, Set.of("operation", "*"))).thenReturn(List.of());

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request);

		assertTrue(result.isEmpty());

		verify(mgmtHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(providerHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1);
		verify(authPolicyRepository).findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.MGMT, 1L, Set.of("operation", "*"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesMgmtScopeReqScopePolicy() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		policy.setId(1);

		final AuthPolicy policy2 = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"operation",
				AuthorizationPolicyType.WHITELIST,
				"PrivilegedSystem");
		policy2.setId(2);

		when(mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.MGMT, 1, Set.of("operation", "*"))).thenReturn(List.of(policy, policy2));

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request);

		assertFalse(result.isEmpty());
		assertEquals(header, result.get().getFirst());
		assertFalse(result.get().getSecond().isEmpty());
		assertEquals(policy2, result.get().getSecond().get(0));

		verify(mgmtHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(providerHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1);
		verify(authPolicyRepository).findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.MGMT, 1L, Set.of("operation", "*"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesMgmtScopeReqDefaultPolicy() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		policy.setId(1);

		when(mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.MGMT, 1, Set.of("operation", "*"))).thenReturn(List.of(policy));

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.MGMT, request);

		assertFalse(result.isEmpty());
		assertEquals(header, result.get().getFirst());
		assertFalse(result.get().getSecond().isEmpty());
		assertEquals(policy, result.get().getSecond().get(0));

		verify(mgmtHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(providerHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(AuthorizationLevel.MGMT, 1);
		verify(authPolicyRepository).findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.MGMT, 1L, Set.of("operation", "*"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesNoRelevantProviderPolicies() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.empty());

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.PROVIDER, request);

		assertTrue(result.isEmpty());

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(mgmtHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesProviderNoScopeReq() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				null);

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		policy.setId(1);

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1)).thenReturn(List.of(policy));

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.PROVIDER, request);

		assertFalse(result.isEmpty());
		assertEquals(header, result.get().getFirst());
		assertFalse(result.get().getSecond().isEmpty());
		assertEquals(policy, result.get().getSecond().get(0));

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(mgmtHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		verify(authPolicyRepository, never()).findByLevelAndHeaderIdAndScopeIn(eq(AuthorizationLevel.PROVIDER), eq(1L), anySet());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesProviderScopeReqZombie() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.PROVIDER, 1, Set.of("operation", "*"))).thenReturn(List.of());

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.PROVIDER, request);

		assertTrue(result.isEmpty());

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(mgmtHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		verify(authPolicyRepository).findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.PROVIDER, 1L, Set.of("operation", "*"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesProviderScopeReqScopePolicy() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		policy.setId(1);

		final AuthPolicy policy2 = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"operation",
				AuthorizationPolicyType.WHITELIST,
				"PrivilegedSystem");
		policy2.setId(2);

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.PROVIDER, 1, Set.of("operation", "*"))).thenReturn(List.of(policy, policy2));

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.PROVIDER, request);

		assertFalse(result.isEmpty());
		assertEquals(header, result.get().getFirst());
		assertFalse(result.get().getSecond().isEmpty());
		assertEquals(policy2, result.get().getSecond().get(0));

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(mgmtHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		verify(authPolicyRepository).findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.PROVIDER, 1L, Set.of("operation", "*"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindRelevantPoliciesProviderScopeReqDefaultPolicy() {
		final NormalizedVerifyRequest request = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				"PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		policy.setId(1);

		when(providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));
		when(authPolicyRepository.findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.PROVIDER, 1, Set.of("operation", "*"))).thenReturn(List.of(policy));

		final Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> result = dbService.findRelevantPolicies(AuthorizationLevel.PROVIDER, request);

		assertFalse(result.isEmpty());
		assertEquals(header, result.get().getFirst());
		assertFalse(result.get().getSecond().isEmpty());
		assertEquals(policy, result.get().getSecond().get(0));

		verify(providerHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(mgmtHeaderRepository, never()).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(authPolicyRepository, never()).findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, 1);
		verify(authPolicyRepository).findByLevelAndHeaderIdAndScopeIn(AuthorizationLevel.PROVIDER, 1L, Set.of("operation", "*"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateMgmtLevelPoliciesInBulkRequesterNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createMgmtLevelPoliciesInBulk(null, null));

		assertEquals("requester is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateMgmtLevelPoliciesInBulkRequesterEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createMgmtLevelPoliciesInBulk("", null));

		assertEquals("requester is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateMgmtLevelPoliciesInBulkRequestListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createMgmtLevelPoliciesInBulk("AdminSystem", null));

		assertEquals("requestList is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateMgmtLevelPoliciesInBulkRequestListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createMgmtLevelPoliciesInBulk("AdminSystem", List.of()));

		assertEquals("requestList is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateMgmtLevelPoliciesInBulkRequestListContainsNull() {
		final List<NormalizedGrantRequest> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.createMgmtLevelPoliciesInBulk("AdminSystem", list));

		assertEquals("requestList contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateMgmtLevelPoliciesInBulkInternalServerError() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.MGMT);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");

		when(mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.createMgmtLevelPoliciesInBulk("AdminSystem", List.of(request)));

		assertEquals("Database operation error", ex.getMessage());

		verify(mgmtHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateMgmtLevelPoliciesInBulkExistingCombination() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.MGMT);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");

		when(mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.of(header));

		final Throwable ex = assertThrows(
				InvalidParameterException.class,
				() -> dbService.createMgmtLevelPoliciesInBulk("AdminSystem", List.of(request)));

		assertEquals("A management level policy is already existing for this cloud / provider / target type / target combination: LOCAL / TestProvider / SERVICE_DEF / serviceDef", ex.getMessage());

		verify(mgmtHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateMgmtLevelPoliciesInBulkOk() {
		final NormalizedGrantRequest request = new NormalizedGrantRequest(AuthorizationLevel.MGMT);
		request.setCloud("LOCAL");
		request.setProvider("TestProvider");
		request.setTargetType(AuthorizationTargetType.SERVICE_DEF);
		request.setTarget("serviceDef");
		request.setDescription("desc");
		request.setPolicies(Map.of(
				"*", new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null)));

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");

		final AuthMgmtPolicyHeader savedHeader = new AuthMgmtPolicyHeader(
				"MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef",
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		savedHeader.setId(1);

		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		final AuthPolicy savedPolicy = new AuthPolicy(
				AuthorizationLevel.MGMT,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);
		savedPolicy.setId(1);

		when(mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef")).thenReturn(Optional.empty());
		when(mgmtHeaderRepository.saveAndFlush(header)).thenReturn(savedHeader);
		when(authPolicyRepository.saveAllAndFlush(List.of(policy))).thenReturn(List.of(savedPolicy));

		final List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> result = dbService.createMgmtLevelPoliciesInBulk("AdminSystem", List.of(request));

		assertFalse(result.isEmpty());
		final Pair<AuthMgmtPolicyHeader, List<AuthPolicy>> pair = result.get(0);
		assertEquals(savedHeader, pair.getFirst());
		assertFalse(pair.getSecond().isEmpty());
		assertEquals(savedPolicy, pair.getSecond().get(0));

		verify(mgmtHeaderRepository).findByCloudAndProviderAndTargetTypeAndTarget("LOCAL", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef");
		verify(mgmtHeaderRepository).saveAndFlush(header);
		verify(authPolicyRepository).saveAllAndFlush(List.of(policy));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeletePoliciesByInstanceIdsListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deletePoliciesByInstanceIds(null));

		assertEquals("instanceIds is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeletePoliciesByInstanceIdsListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deletePoliciesByInstanceIds(List.of()));

		assertEquals("instanceIds is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeletePoliciesByInstanceIdsListContainsNullElement() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deletePoliciesByInstanceIds(list));

		assertEquals("instanceIds contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeletePoliciesByInstanceIdsListContainsEmptyElement() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deletePoliciesByInstanceIds(List.of("")));

		assertEquals("instanceIds contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeletePoliciesByInstanceIdsInternalServerError() {
		when(mgmtHeaderRepository.findByInstanceIdIn(List.of("instanceId"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.deletePoliciesByInstanceIds(List.of("instanceId")));

		assertEquals("Database operation error", ex.getMessage());

		verify(mgmtHeaderRepository).findByInstanceIdIn(List.of("instanceId"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeletePoliciesByInstanceIdsFromMgmtLevel() {
		final String instanceId = "MGMT|LOCAL|TestProvider|SERVICE_DEF|serviceDef";

		final AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				instanceId,
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc",
				"AdminSystem");
		header.setId(1);

		when(mgmtHeaderRepository.findByInstanceIdIn(List.of(instanceId))).thenReturn(List.of(header));
		doNothing().when(mgmtHeaderRepository).deleteAll(List.of(header));
		doNothing().when(mgmtHeaderRepository).flush();
		doNothing().when(authPolicyRepository).deleteByLevelAndHeaderIdIn(AuthorizationLevel.MGMT, List.of(1L));
		when(providerHeaderRepository.findByInstanceIdIn(List.of(instanceId))).thenReturn(List.of());
		doNothing().when(authPolicyRepository).flush();

		assertDoesNotThrow(() -> dbService.deletePoliciesByInstanceIds(List.of(instanceId)));

		verify(mgmtHeaderRepository).findByInstanceIdIn(List.of(instanceId));
		verify(mgmtHeaderRepository).deleteAll(List.of(header));
		verify(mgmtHeaderRepository).flush();
		verify(authPolicyRepository).deleteByLevelAndHeaderIdIn(AuthorizationLevel.MGMT, List.of(1L));
		verify(providerHeaderRepository).findByInstanceIdIn(List.of(instanceId));
		verify(providerHeaderRepository, never()).deleteAll(anyList());
		verify(providerHeaderRepository, never()).flush();
		verify(authPolicyRepository, never()).deleteByLevelAndHeaderIdIn(eq(AuthorizationLevel.PROVIDER), anyList());
		verify(authPolicyRepository).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeletePoliciesByInstanceIdsFromProviderLevel() {
		final String instanceId = "PR|LOCAL|TestProvider|SERVICE_DEF|serviceDef";

		final AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
				instanceId,
				AuthorizationTargetType.SERVICE_DEF,
				"LOCAL",
				"TestProvider",
				"serviceDef",
				"desc");
		header.setId(1);

		when(providerHeaderRepository.findByInstanceIdIn(List.of(instanceId))).thenReturn(List.of(header));
		doNothing().when(providerHeaderRepository).deleteAll(List.of(header));
		doNothing().when(providerHeaderRepository).flush();
		doNothing().when(authPolicyRepository).deleteByLevelAndHeaderIdIn(AuthorizationLevel.PROVIDER, List.of(1L));
		when(mgmtHeaderRepository.findByInstanceIdIn(List.of(instanceId))).thenReturn(List.of());
		doNothing().when(authPolicyRepository).flush();

		assertDoesNotThrow(() -> dbService.deletePoliciesByInstanceIds(List.of(instanceId)));

		verify(providerHeaderRepository).findByInstanceIdIn(List.of(instanceId));
		verify(providerHeaderRepository).deleteAll(List.of(header));
		verify(providerHeaderRepository).flush();
		verify(authPolicyRepository).deleteByLevelAndHeaderIdIn(AuthorizationLevel.PROVIDER, List.of(1L));
		verify(mgmtHeaderRepository).findByInstanceIdIn(List.of(instanceId));
		verify(mgmtHeaderRepository, never()).deleteAll(anyList());
		verify(mgmtHeaderRepository, never()).flush();
		verify(authPolicyRepository, never()).deleteByLevelAndHeaderIdIn(eq(AuthorizationLevel.MGMT), anyList());
		verify(authPolicyRepository).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsDifferentTypes() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.WHITELIST, List.of("SpecialConsumer"), null);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsTwoAlls() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.ALL,
				null);

		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertTrue(result);

		verify(dtoConverter).convertAuthPolicyToDTO(policy);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsTwoBlacklistsFalse1() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.BLACKLIST,
				"System1,System2");

		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.BLACKLIST, List.of("System1"), null);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertFalse(result);

		verify(dtoConverter).convertAuthPolicyToDTO(policy);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsTwoBlacklistsFalse2() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.BLACKLIST,
				"System1,System2");

		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.BLACKLIST, List.of("System1", "System3"), null);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertFalse(result);

		verify(dtoConverter).convertAuthPolicyToDTO(policy);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsTwoBlacklistsTrue() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.BLACKLIST,
				"System1,System2");

		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.BLACKLIST, List.of("System2", "System1"), null);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertTrue(result);

		verify(dtoConverter).convertAuthPolicyToDTO(policy);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsTwoWhitelistsFalse1() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.WHITELIST,
				"System1,System2");

		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.WHITELIST, List.of("System1"), null);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertFalse(result);

		verify(dtoConverter).convertAuthPolicyToDTO(policy);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsTwoWhitelistsFalse2() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.WHITELIST,
				"System1,System2");

		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.WHITELIST, List.of("System1", "System3"), null);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertFalse(result);

		verify(dtoConverter).convertAuthPolicyToDTO(policy);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsTwoWhitelistsTrue() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.WHITELIST,
				"System1,System2");

		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.WHITELIST, List.of("System2", "System1"), null);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertTrue(result);

		verify(dtoConverter).convertAuthPolicyToDTO(policy);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsTwoSysMetadatasFalse() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.SYS_METADATA,
				"{ \"key\": \"value\" }");

		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", "othervalue");
		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.SYS_METADATA, null, req);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertFalse(result);

		verify(dtoConverter).convertAuthPolicyToDTO(policy);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testPolicyDetailsEqualsTwoSysMetadatasTrue() {
		final AuthPolicy policy = new AuthPolicy(
				AuthorizationLevel.PROVIDER,
				1,
				"*",
				AuthorizationPolicyType.SYS_METADATA,
				"{ \"key\": \"value\" }");

		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", "value");
		final NormalizedAuthorizationPolicyRequest candidate = new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.SYS_METADATA, null, req);

		final boolean result = (boolean) ReflectionTestUtils.invokeMethod(dbService, "policyDetailsEquals", policy, candidate);
		assertTrue(result);

		verify(dtoConverter).convertAuthPolicyToDTO(policy);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStringifyPolicyAll() {
		final NormalizedAuthorizationPolicyRequest policy = new NormalizedAuthorizationPolicyRequest(
				AuthorizationPolicyType.ALL,
				null,
				null);

		final String result = (String) ReflectionTestUtils.invokeMethod(dbService, "stringifyPolicy", policy);
		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStringifyPolicyBlacklist() {
		final NormalizedAuthorizationPolicyRequest policy = new NormalizedAuthorizationPolicyRequest(
				AuthorizationPolicyType.BLACKLIST,
				List.of("System1", "System2"),
				null);

		final String result = (String) ReflectionTestUtils.invokeMethod(dbService, "stringifyPolicy", policy);
		assertEquals("System1,System2", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStringifyPolicyWhitelist() {
		final NormalizedAuthorizationPolicyRequest policy = new NormalizedAuthorizationPolicyRequest(
				AuthorizationPolicyType.WHITELIST,
				List.of("System1", "System2", "System4"),
				null);

		final String result = (String) ReflectionTestUtils.invokeMethod(dbService, "stringifyPolicy", policy);
		assertEquals("System1,System2,System4", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testStringifyPolicySysMetadata() {
		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("key", "value");
		final NormalizedAuthorizationPolicyRequest policy = new NormalizedAuthorizationPolicyRequest(
				AuthorizationPolicyType.SYS_METADATA,
				null,
				req);

		final String result = (String) ReflectionTestUtils.invokeMethod(dbService, "stringifyPolicy", policy);
		final Map<String, String> resultMap = Utilities.fromJson(result, new TypeReference<Map<String, String>>() {
		});

		assertEquals(1, resultMap.size());
		assertEquals("value", resultMap.get("key"));
	}
}