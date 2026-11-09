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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.service.AuthorizationPolicyDbService;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedQueryRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.authorization.service.validation.AuthorizationManagementValidation;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.AuthorizationMgmtGrantListRequestDTO;
import eu.arrowhead.dto.AuthorizationMgmtGrantRequestDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.AuthorizationQueryRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

@ExtendWith(MockitoExtension.class)
public class AuthorizationManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationManagementService service;

	@Mock
	private AuthorizationManagementValidation validator;

	@Mock
	private AuthorizationPolicyDbService dbService;

	@Mock
	private DTOConverter dtoConverter;

	@Mock
	private PageService pageService;

	@Mock
	private AuthorizationPolicyEngine policyEngine;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantPoliciesOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.grantPoliciesOperation(null, null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantPoliciesOperationEmptyOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.grantPoliciesOperation(null, null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantPoliciesOperationInvalidParameter() {
		final AuthorizationMgmtGrantRequestDTO request = new AuthorizationMgmtGrantRequestDTO(
				"LOCAL",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF.name(),
				"serviceDef",
				"description",
				new AuthorizationPolicyRequestDTO(AuthorizationPolicyType.ALL.name(), null, null),
				null);
		final AuthorizationMgmtGrantListRequestDTO dto = new AuthorizationMgmtGrantListRequestDTO(List.of(request));

		final NormalizedGrantRequest normalized = new NormalizedGrantRequest(AuthorizationLevel.MGMT);
		final List<NormalizedGrantRequest> normalizedList = List.of(normalized);

		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeGrantListRequest(dto, "origin")).thenReturn(normalizedList);
		when(dbService.createMgmtLevelPoliciesInBulk("Requester", normalizedList)).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				InvalidParameterException.class,
				() -> service.grantPoliciesOperation("Requester", dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(validator).validateAndNormalizeGrantListRequest(dto, "origin");
		verify(dbService).createMgmtLevelPoliciesInBulk("Requester", normalizedList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantPoliciesOperationInternalServerError() {
		final AuthorizationMgmtGrantRequestDTO request = new AuthorizationMgmtGrantRequestDTO(
				"LOCAL",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF.name(),
				"serviceDef",
				"description",
				new AuthorizationPolicyRequestDTO(AuthorizationPolicyType.ALL.name(), null, null),
				null);
		final AuthorizationMgmtGrantListRequestDTO dto = new AuthorizationMgmtGrantListRequestDTO(List.of(request));

		final NormalizedGrantRequest normalized = new NormalizedGrantRequest(AuthorizationLevel.MGMT);
		final List<NormalizedGrantRequest> normalizedList = List.of(normalized);

		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeGrantListRequest(dto, "origin")).thenReturn(normalizedList);
		when(dbService.createMgmtLevelPoliciesInBulk("Requester", normalizedList)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.grantPoliciesOperation("Requester", dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(validator).validateAndNormalizeGrantListRequest(dto, "origin");
		verify(dbService).createMgmtLevelPoliciesInBulk("Requester", normalizedList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGrantPoliciesOperationOk() {
		final AuthorizationMgmtGrantRequestDTO request = new AuthorizationMgmtGrantRequestDTO(
				"LOCAL",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF.name(),
				"serviceDef",
				"description",
				new AuthorizationPolicyRequestDTO(AuthorizationPolicyType.ALL.name(), null, null),
				null);
		final AuthorizationMgmtGrantListRequestDTO dto = new AuthorizationMgmtGrantListRequestDTO(List.of(request));

		final NormalizedGrantRequest normalized = new NormalizedGrantRequest(AuthorizationLevel.MGMT);
		final List<NormalizedGrantRequest> normalizedList = List.of(normalized);

		final Pair<AuthMgmtPolicyHeader, List<AuthPolicy>> pair = Pair.of(
				new AuthMgmtPolicyHeader(),
				List.of(new AuthPolicy()));
		final List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> dbResult = List.of(pair);

		when(validator.validateAndNormalizeSystemName("Requester", "origin")).thenReturn("Requester");
		when(validator.validateAndNormalizeGrantListRequest(dto, "origin")).thenReturn(normalizedList);
		when(dbService.createMgmtLevelPoliciesInBulk("Requester", normalizedList)).thenReturn(dbResult);
		when(dtoConverter.convertMgmtLevelPolicyListToResponse(dbResult)).thenReturn(new AuthorizationPolicyListResponseDTO(List.of(), 0));

		assertDoesNotThrow(() -> service.grantPoliciesOperation("Requester", dto, "origin"));

		verify(validator).validateAndNormalizeSystemName("Requester", "origin");
		verify(validator).validateAndNormalizeGrantListRequest(dto, "origin");
		verify(dbService).createMgmtLevelPoliciesInBulk("Requester", normalizedList);
		verify(dtoConverter).convertMgmtLevelPolicyListToResponse(dbResult);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokePoliciesOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokePoliciesOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokePoliciesOperationNullEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.revokePoliciesOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokePoliciesOperationInternalServerError() {
		final List<String> input = List.of("instanceId");
		final List<String> normalized = List.of("instanceId");

		when(validator.validateAndNormalizeRevokePoliciesInput(input, "origin")).thenReturn(normalized);
		doThrow(new InternalServerError("test")).when(dbService).deletePoliciesByInstanceIds(normalized);

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.revokePoliciesOperation(input, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeRevokePoliciesInput(input, "origin");
		verify(dbService).deletePoliciesByInstanceIds(normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRevokePoliciesOperationOk() {
		final List<String> input = List.of("instanceId");
		final List<String> normalized = List.of("instanceId");

		when(validator.validateAndNormalizeRevokePoliciesInput(input, "origin")).thenReturn(normalized);
		doNothing().when(dbService).deletePoliciesByInstanceIds(normalized);

		assertDoesNotThrow(() -> service.revokePoliciesOperation(input, "origin"));

		verify(validator).validateAndNormalizeRevokePoliciesInput(input, "origin");
		verify(dbService).deletePoliciesByInstanceIds(normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPoliciesOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryPoliciesOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPoliciesOperationNullEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.queryPoliciesOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPoliciesOperationInternalServerError() {
		final AuthorizationQueryRequestDTO dto = new AuthorizationQueryRequestDTO(null, AuthorizationLevel.MGMT.name(), null, null, null, null, null);
		final NormalizedQueryRequest normalized = new NormalizedQueryRequest();
		normalized.setLevel(AuthorizationLevel.MGMT);

		when(validator.validateAndNormalizeQueryRequest(dto, "origin")).thenReturn(normalized);
		when(pageService.getPageRequest(null, Direction.ASC, AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY, AuthPolicyHeader.DEFAULT_SORT_FIELD, "origin")).thenReturn(
				PageRequest.of(0, 1));
		when(dbService.getMgmtLevelPoliciesByFilters(any(Pageable.class), eq(normalized))).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.queryPoliciesOperation(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeQueryRequest(dto, "origin");
		verify(pageService).getPageRequest(null, Direction.ASC, AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY, AuthPolicyHeader.DEFAULT_SORT_FIELD, "origin");
		verify(dbService).getMgmtLevelPoliciesByFilters(any(Pageable.class), eq(normalized));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPoliciesOperationMgmtOk() {
		final AuthorizationQueryRequestDTO dto = new AuthorizationQueryRequestDTO(null, AuthorizationLevel.MGMT.name(), null, null, null, null, null);
		final NormalizedQueryRequest normalized = new NormalizedQueryRequest();
		normalized.setLevel(AuthorizationLevel.MGMT);

		Pair<AuthMgmtPolicyHeader, List<AuthPolicy>> pair = Pair.of(
				new AuthMgmtPolicyHeader(),
				List.of(new AuthPolicy()));
		Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> dbResult = new PageImpl<>(List.of(pair));

		when(validator.validateAndNormalizeQueryRequest(dto, "origin")).thenReturn(normalized);
		when(pageService.getPageRequest(null, Direction.ASC, AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY, AuthPolicyHeader.DEFAULT_SORT_FIELD, "origin")).thenReturn(
				PageRequest.of(0, 1));
		when(dbService.getMgmtLevelPoliciesByFilters(any(Pageable.class), eq(normalized))).thenReturn(dbResult);
		when(dtoConverter.convertMgmtLevelPolicyPageToResponse(dbResult)).thenReturn(new AuthorizationPolicyListResponseDTO(List.of(), 0));

		assertDoesNotThrow(() -> service.queryPoliciesOperation(dto, "origin"));

		verify(validator).validateAndNormalizeQueryRequest(dto, "origin");
		verify(pageService).getPageRequest(null, Direction.ASC, AuthMgmtPolicyHeader.SORTABLE_FIELDS_BY, AuthPolicyHeader.DEFAULT_SORT_FIELD, "origin");
		verify(dbService).getMgmtLevelPoliciesByFilters(any(Pageable.class), eq(normalized));
		verify(dtoConverter).convertMgmtLevelPolicyPageToResponse(dbResult);
		verify(dbService, never()).getProviderLevelPoliciesByFilters(any(Pageable.class), eq(normalized));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPoliciesOperationProviderOk() {
		final AuthorizationQueryRequestDTO dto = new AuthorizationQueryRequestDTO(null, AuthorizationLevel.PROVIDER.name(), null, null, null, null, null);
		final NormalizedQueryRequest normalized = new NormalizedQueryRequest();
		normalized.setLevel(AuthorizationLevel.PROVIDER);

		Pair<AuthProviderPolicyHeader, List<AuthPolicy>> pair = Pair.of(
				new AuthProviderPolicyHeader(),
				List.of(new AuthPolicy()));
		Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> dbResult = new PageImpl<>(List.of(pair));

		when(validator.validateAndNormalizeQueryRequest(dto, "origin")).thenReturn(normalized);
		when(pageService.getPageRequest(null, Direction.ASC, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, AuthPolicyHeader.DEFAULT_SORT_FIELD, "origin")).thenReturn(
				PageRequest.of(0, 1));
		when(dbService.getProviderLevelPoliciesByFilters(any(Pageable.class), eq(normalized))).thenReturn(dbResult);
		when(dtoConverter.convertProviderLevelPolicyPageToResponse(dbResult)).thenReturn(new AuthorizationPolicyListResponseDTO(List.of(), 0));

		assertDoesNotThrow(() -> service.queryPoliciesOperation(dto, "origin"));

		verify(validator).validateAndNormalizeQueryRequest(dto, "origin");
		verify(pageService).getPageRequest(null, Direction.ASC, AuthProviderPolicyHeader.SORTABLE_FIELDS_BY, AuthPolicyHeader.DEFAULT_SORT_FIELD, "origin");
		verify(dbService, never()).getMgmtLevelPoliciesByFilters(any(Pageable.class), eq(normalized));
		verify(dbService).getProviderLevelPoliciesByFilters(any(Pageable.class), eq(normalized));
		verify(dtoConverter).convertProviderLevelPolicyPageToResponse(dbResult);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckPoliciesOperationNullOrigin() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.checkPoliciesOperation(null, null));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckPoliciesOperationNullEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.checkPoliciesOperation(null, ""));

		assertEquals("origin is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckPoliciesOperationInternalServerError() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF.name(),
				"serviceDef",
				"operation");
		final AuthorizationVerifyListRequestDTO dto = new AuthorizationVerifyListRequestDTO(List.of(request));

		NormalizedVerifyRequest normalized = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		final List<NormalizedVerifyRequest> normalizedList = List.of(normalized);

		when(validator.validateAndNormalizeVerifyListRequest(dto, "origin")).thenReturn(normalizedList);
		when(policyEngine.checkAccess(normalizedList)).thenThrow(new InternalServerError("test"));

		final ArrowheadException ex = assertThrows(
				InternalServerError.class,
				() -> service.checkPoliciesOperation(dto, "origin"));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(validator).validateAndNormalizeVerifyListRequest(dto, "origin");
		verify(policyEngine).checkAccess(normalizedList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCheckPoliciesOperationOk() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF.name(),
				"serviceDef",
				"operation");
		final AuthorizationVerifyListRequestDTO dto = new AuthorizationVerifyListRequestDTO(List.of(request));

		NormalizedVerifyRequest normalized = new NormalizedVerifyRequest(
				"TestProvider",
				"TestConsumer",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		final List<NormalizedVerifyRequest> normalizedList = List.of(normalized);

		final Pair<NormalizedVerifyRequest, Boolean> pair = Pair.of(
				normalized,
				true);
		final List<Pair<NormalizedVerifyRequest, Boolean>> engineResult = List.of(pair);

		when(validator.validateAndNormalizeVerifyListRequest(dto, "origin")).thenReturn(normalizedList);
		when(policyEngine.checkAccess(normalizedList)).thenReturn(engineResult);
		when(dtoConverter.convertCheckResultListToResponse(engineResult)).thenReturn(new AuthorizationVerifyListResponseDTO(List.of(), 0));

		assertDoesNotThrow(() -> service.checkPoliciesOperation(dto, "origin"));

		verify(validator).validateAndNormalizeVerifyListRequest(dto, "origin");
		verify(policyEngine).checkAccess(normalizedList);
		verify(dtoConverter).convertCheckResultListToResponse(engineResult);
	}
}