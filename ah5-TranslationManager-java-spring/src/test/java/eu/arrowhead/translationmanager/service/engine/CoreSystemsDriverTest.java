/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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
package eu.arrowhead.translationmanager.service.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyResponseDTO;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.translationmanager.TranslationManagerSystemInfo;
import eu.arrowhead.translationmanager.service.dto.NormalizedServiceInstanceDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;

@ExtendWith(MockitoExtension.class)
public class CoreSystemsDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private CoreSystemsDriver driver;

	@Mock
	private TranslationManagerSystemInfo sysInfo;

	@Mock
	private ArrowheadHttpService ahHttpService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsBlacklistedNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.isBlacklisted(null));

		assertEquals("systemName is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsBlacklistedEmptyInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.isBlacklisted(""));

		assertEquals("systemName is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsBlacklistedOk() {
		// checking that filterOutBlacklistedSystems() is called (that sysInfo.getBlacklistCheckExcludeList() is called actually)

		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of("TestConsumer"));

		final boolean result = driver.isBlacklisted("TestConsumer");

		assertFalse(result);

		verify(sysInfo).getBlacklistCheckExcludeList();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsListNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutBlacklistedSystems(null));

		assertEquals("systemNames is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsListEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutBlacklistedSystems(List.of()));

		assertEquals("systemNames is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutBlacklistedSystems(list));

		assertEquals("systemNames list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsListContainsEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutBlacklistedSystems(List.of("")));

		assertEquals("systemNames list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsForbiddenException() {
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenThrow(new ForbiddenException("test", "origin"));

		final ArrowheadException ex = assertThrows(ForbiddenException.class,
				() -> driver.filterOutBlacklistedSystems(List.of("TestConsumer")));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsAuthException() {
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenThrow(new AuthException("test", "origin"));

		final ArrowheadException ex = assertThrows(AuthException.class,
				() -> driver.filterOutBlacklistedSystems(List.of("TestConsumer")));

		assertEquals("test", ex.getMessage());
		assertEquals("origin", ex.getOrigin());

		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsAnyOtherArrowheadExceptionForcedBlacklist() {
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenThrow(ExternalServerError.class);
		when(sysInfo.isBlacklistForced()).thenReturn(true);

		final List<String> result = driver.filterOutBlacklistedSystems(List.of("TestConsumer"));

		assertTrue(result.isEmpty());

		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
		verify(sysInfo).isBlacklistForced();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutBlacklistedSystemsAnyOtherArrowheadExceptionNotForcedBlacklist() {
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenThrow(ExternalServerError.class);
		when(sysInfo.isBlacklistForced()).thenReturn(false);

		final List<String> result = driver.filterOutBlacklistedSystems(List.of("TestConsumer"));

		assertEquals(List.of("TestConsumer"), result);

		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
		verify(sysInfo).isBlacklistForced();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testFilterOutBlacklistedSystemsOk() {
		final BlacklistEntryDTO entry1 = new BlacklistEntryDTO("BadSystem", null, null, null, null, null, null, true);
		final BlacklistEntryDTO entry2 = new BlacklistEntryDTO("BadSystem", null, null, null, null, null, null, true);
		final BlacklistEntryDTO entry3 = new BlacklistEntryDTO("BadSystem", null, null, null, null, null, null, true);

		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(ahHttpService.consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class)))
				.thenReturn(
						new BlacklistEntryListResponseDTO(List.of(entry1, entry2), 3),
						new BlacklistEntryListResponseDTO(List.of(entry3), 3));

		final List<String> result = driver.filterOutBlacklistedSystems(List.of("TestConsumer", "BadSystem"));

		assertEquals(List.of("TestConsumer"), result);

		verify(sysInfo, times(2)).getBlacklistCheckExcludeList();
		verify(ahHttpService, times(2)).consumeService(eq("blacklistManagement"), eq("query"), eq("Blacklist"), eq(BlacklistEntryListResponseDTO.class), any(BlacklistQueryRequestDTO.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationCandidateListNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(null, null, null, null));

		assertEquals("candidates list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationCandidateListEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of(), null, null, null));

		assertEquals("candidates list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationCandidateListContainsNull() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(list, null, null, null));

		assertEquals("candidates list contains null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationCandidateListContainsEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of(""), null, null, null));

		assertEquals("candidates list contains null or empty value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationConsumerNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), null, null, null));

		assertEquals("consumer is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationConsumerEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "", null, null));

		assertEquals("consumer is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationServiceDefinitionNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", null, null));

		assertEquals("serviceDefinition is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationServiceDefinitionEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "", null));

		assertEquals("serviceDefinition is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationOk1() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"TestProvider",
				"TestConsumer",
				null,
				"SERVICE_DEF",
				"testService",
				"test-operation");
		final AuthorizationVerifyListRequestDTO requestList = new AuthorizationVerifyListRequestDTO(List.of(request));

		when(ahHttpService.consumeService("authorizationManagement", "check-policies", "ConsumerAuthorization", AuthorizationVerifyListResponseDTO.class, requestList))
				.thenReturn(new AuthorizationVerifyListResponseDTO(List.of(), 0));

		final List<String> result = driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider"), "TestConsumer", "testService", "test-operation");

		assertTrue(result.isEmpty());

		verify(ahHttpService).consumeService("authorizationManagement", "check-policies", "ConsumerAuthorization", AuthorizationVerifyListResponseDTO.class, requestList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFilterOutProvidersBecauseOfUnauthorizationOk2() {
		final AuthorizationVerifyRequestDTO request = new AuthorizationVerifyRequestDTO(
				"TestProvider",
				"TestConsumer",
				null,
				"SERVICE_DEF",
				"testService",
				"test-operation");
		final AuthorizationVerifyRequestDTO request2 = new AuthorizationVerifyRequestDTO(
				"TestProvider2",
				"TestConsumer",
				null,
				"SERVICE_DEF",
				"testService",
				"test-operation");
		final AuthorizationVerifyListRequestDTO requestList = new AuthorizationVerifyListRequestDTO(List.of(request, request2));
		final AuthorizationVerifyResponseDTO response1 = new AuthorizationVerifyResponseDTO(
				"TestProvider",
				"TestConsumer",
				null,
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"test-operation",
				true);
		final AuthorizationVerifyResponseDTO response2 = new AuthorizationVerifyResponseDTO(
				"TestProvider2",
				"TestConsumer",
				null,
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"test-operation",
				false);

		when(ahHttpService.consumeService("authorizationManagement", "check-policies", "ConsumerAuthorization", AuthorizationVerifyListResponseDTO.class, requestList))
				.thenReturn(new AuthorizationVerifyListResponseDTO(List.of(response1, response2), 2));

		final List<String> result = driver.filterOutProvidersBecauseOfUnauthorization(List.of("TestProvider", "TestProvider2"), "TestConsumer", "testService", "test-operation");

		assertEquals(List.of("TestProvider"), result);

		verify(ahHttpService).consumeService("authorizationManagement", "check-policies", "ConsumerAuthorization", AuthorizationVerifyListResponseDTO.class, requestList);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForManagerToInterfaceBridgeManagementServiceInputListNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.generateTokenForManagerToInterfaceBridgeManagementService(null));

		assertEquals("interfaceTranslators list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForManagerToInterfaceBridgeManagementServiceInputListEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.generateTokenForManagerToInterfaceBridgeManagementService(List.of()));

		assertEquals("interfaceTranslators list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForManagerToInterfaceBridgeManagementServiceInputListContainsNull() {
		final List<ServiceInstanceResponseDTO> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> driver.generateTokenForManagerToInterfaceBridgeManagementService(list));

		assertEquals("interfaceTranslators list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForManagerToInterfaceBridgeManagementServiceNoTokensNeeded() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"NONE",
				Map.of());

		final ServiceInstanceResponseDTO dto = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(intf),
				null,
				null);

		final Map<String, String> result = driver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(dto));

		assertTrue(result.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForManagerToInterfaceBridgeManagementServiceOk() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_http",
				"http",
				"USAGE_LIMITED_TOKEN_AUTH",
				Map.of());

		final ServiceInstanceResponseDTO dto = new ServiceInstanceResponseDTO(
				"InterfaceTranslator|interfaceBridgeManagement|1.0.0",
				new SystemResponseDTO("InterfaceTranslator", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("interfaceBridgeManagement", null, null),
				"1.0.0",
				null,
				null,
				List.of(intf),
				null,
				null);

		final MultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>(Map.of(Constants.UNBOUND, List.of(Boolean.TRUE.toString())));

		final AuthorizationTokenResponseDTO responseEntry = new AuthorizationTokenResponseDTO(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"variant",
				"token",
				null,
				"TranslationManager",
				"LOCAL",
				"TranslationManager",
				"InterfaceTranslator",
				AuthorizationTargetType.SERVICE_DEF,
				"interfaceBridgeManagement",
				null,
				null,
				100,
				100,
				null);

		when(sysInfo.getSystemName()).thenReturn("TranslationManager");
		when(ahHttpService.consumeService(
				eq("authorizationTokenManagement"),
				eq("generate-tokens"),
				eq("ConsumerAuthorization"),
				eq(AuthorizationTokenMgmtListResponseDTO.class),
				any(AuthorizationTokenGenerationMgmtListRequestDTO.class),
				eq(queryMap))).thenReturn(new AuthorizationTokenMgmtListResponseDTO(List.of(responseEntry), 1));

		final Map<String, String> result = driver.generateTokenForManagerToInterfaceBridgeManagementService(List.of(dto));

		assertNotNull(result);
		assertTrue(result.containsKey("InterfaceTranslator"));
		assertEquals("token", result.get("InterfaceTranslator"));

		verify(sysInfo).getSystemName();
		verify(ahHttpService).consumeService(
				eq("authorizationTokenManagement"),
				eq("generate-tokens"),
				eq("ConsumerAuthorization"),
				eq(AuthorizationTokenMgmtListResponseDTO.class),
				any(AuthorizationTokenGenerationMgmtListRequestDTO.class),
				eq(queryMap));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationPolicyNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation(null, null, null, null, null));

		assertEquals("policy is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationPolicyEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation("", null, null, null, null));

		assertEquals("policy is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationInterfaceTranslatorNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation("USAGE_LIMITED_TOKEN_AUTH", null, null, null, null));

		assertEquals("interfaceTranslator is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationInterfaceTranslatorEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation("USAGE_LIMITED_TOKEN_AUTH", "", null, null, null));

		assertEquals("interfaceTranslator is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationTargetProviderNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation("USAGE_LIMITED_TOKEN_AUTH", "InterfaceTranslator", null, null, null));

		assertEquals("targetProvider is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationTargetProviderEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation("USAGE_LIMITED_TOKEN_AUTH", "InterfaceTranslator", "", null, null));

		assertEquals("targetProvider is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationServiceDefinitionNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation("USAGE_LIMITED_TOKEN_AUTH", "InterfaceTranslator", "TestProvider", null, null));

		assertEquals("serviceDefinition is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationServiceDefinitionEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation("USAGE_LIMITED_TOKEN_AUTH", "InterfaceTranslator", "TestProvider", "", null));

		assertEquals("serviceDefinition is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationOperationNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation("USAGE_LIMITED_TOKEN_AUTH", "InterfaceTranslator", "TestProvider", "testService", null));

		assertEquals("operation is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationOperationEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.generateTokenForInterfaceTranslatorToTargetOperation("USAGE_LIMITED_TOKEN_AUTH", "InterfaceTranslator", "TestProvider", "testService", ""));

		assertEquals("operation is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateTokenForInterfaceTranslatorToTargetOperationOk() {
		final AuthorizationTokenGenerationMgmtRequestDTO dto = new AuthorizationTokenGenerationMgmtRequestDTO(
				"USAGE_LIMITED_TOKEN_AUTH",
				"SERVICE_DEF",
				"LOCAL",
				"InterfaceTranslator",
				"TestProvider",
				"testService",
				"test-operation",
				null,
				null);
		final AuthorizationTokenGenerationMgmtListRequestDTO payload = new AuthorizationTokenGenerationMgmtListRequestDTO(List.of(dto));

		final MultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>(Map.of(Constants.UNBOUND, List.of(Boolean.TRUE.toString())));

		final AuthorizationTokenResponseDTO responseEntry = new AuthorizationTokenResponseDTO(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"variant",
				"token",
				null,
				"TranslationManager",
				"LOCAL",
				"InterfaceTranslator",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"testService",
				"test-operation",
				null,
				10,
				10,
				null);

		when(ahHttpService.consumeService(
				"authorizationTokenManagement",
				"generate-tokens",
				"ConsumerAuthorization",
				AuthorizationTokenMgmtListResponseDTO.class,
				payload,
				queryMap)).thenReturn(new AuthorizationTokenMgmtListResponseDTO(List.of(responseEntry), 1));

		final AuthorizationTokenResponseDTO result = driver.generateTokenForInterfaceTranslatorToTargetOperation(
				"USAGE_LIMITED_TOKEN_AUTH",
				"InterfaceTranslator",
				"TestProvider",
				"testService",
				"test-operation");

		assertEquals(responseEntry, result);

		verify(ahHttpService).consumeService(
				"authorizationTokenManagement",
				"generate-tokens",
				"ConsumerAuthorization",
				AuthorizationTokenMgmtListResponseDTO.class,
				payload,
				queryMap);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectInterfaceTranslatorCandidatesRequirementsListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectInterfaceTranslatorCandidates(null, null));

		assertEquals("inputInterfaceRequirements list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectInterfaceTranslatorCandidatesRequirementsListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectInterfaceTranslatorCandidates(List.of(), null));

		assertEquals("inputInterfaceRequirements list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectInterfaceTranslatorCandidatesRequirementsListContainsNull() {
		final List<String> reqList = new ArrayList<>(1);
		reqList.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectInterfaceTranslatorCandidates(reqList, null));

		assertEquals("inputInterfaceRequirements list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectInterfaceTranslatorCandidatesRequirementsListContainsEmptyElement() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectInterfaceTranslatorCandidates(List.of(""), null));

		assertEquals("inputInterfaceRequirements list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectInterfaceTranslatorCandidatesTargetsNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectInterfaceTranslatorCandidates(List.of("req"), null));

		assertEquals("targets list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectInterfaceTranslatorCandidatesTargetsEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectInterfaceTranslatorCandidates(List.of("req"), List.of()));

		assertEquals("targets list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectInterfaceTranslatorCandidatesTargetsContainsNull() {
		final List<NormalizedServiceInstanceDTO> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectInterfaceTranslatorCandidates(List.of("req"), list));

		assertEquals("targets list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectInterfaceTranslatorCandidatesTargetsOk1() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_mqtt",
				"tcp",
				"NONE",
				Map.of("accessPort", 12345));

		final NormalizedServiceInstanceDTO target = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				List.of(intf));

		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("interfaceBridge.from", Map.of("op", "CONTAINS_ANY", "value", List.of("generic_http")));
		req.put("interfaceBridge.to", Map.of("op", "IN", "value", List.of("generic_mqtt")));

		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO(
				null,
				null,
				List.of("interfaceBridgeManagement"),
				null,
				null,
				List.of(req),
				null,
				List.of("generic_http"),
				null,
				null);

		final ServiceInstanceResponseDTO response = new ServiceInstanceResponseDTO("InterfaceTranslator|interfaceBridgeManagement|1.0.0", null, null, null, null, req, null, null, null);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(ahHttpService.consumeService(
				"serviceDiscovery",
				"lookup",
				"ServiceRegistry",
				ServiceInstanceListResponseDTO.class,
				payload)).thenReturn(new ServiceInstanceListResponseDTO(List.of(response), 1));

		final List<ServiceInstanceResponseDTO> result = driver.collectInterfaceTranslatorCandidates(List.of("generic_http"), List.of(target));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("InterfaceTranslator|interfaceBridgeManagement|1.0.0", result.get(0).instanceId());

		verify(sysInfo).isSslEnabled();
		verify(ahHttpService).consumeService(
				"serviceDiscovery",
				"lookup",
				"ServiceRegistry",
				ServiceInstanceListResponseDTO.class,
				payload);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCollectInterfaceTranslatorCandidatesTargetsOk2() {
		final ServiceInstanceInterfaceResponseDTO intf = new ServiceInstanceInterfaceResponseDTO(
				"generic_mqtt",
				"tcp",
				"NONE",
				Map.of("accessPort", 12345));

		final NormalizedServiceInstanceDTO target = new NormalizedServiceInstanceDTO(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				List.of(intf));

		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("interfaceBridge.from", Map.of("op", "CONTAINS_ANY", "value", List.of("generic_http")));
		req.put("interfaceBridge.to", Map.of("op", "IN", "value", List.of("generic_mqtt")));

		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO(
				null,
				null,
				List.of("interfaceBridgeManagement"),
				null,
				"2026-1-16T10:05:00Z",
				List.of(req),
				null,
				List.of("generic_https"),
				null,
				null);

		final ServiceInstanceResponseDTO response = new ServiceInstanceResponseDTO("InterfaceTranslator|interfaceBridgeManagement|1.0.0", null, null, null, null, req, null, null, null);

		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(
				"serviceDiscovery",
				"lookup",
				"ServiceRegistry",
				ServiceInstanceListResponseDTO.class,
				payload)).thenReturn(new ServiceInstanceListResponseDTO(List.of(response), 1));

		ReflectionTestUtils.setField(driver, "translatorServiceMinAvailability", 5);

		try (MockedStatic<Utilities> staticMock = Mockito.mockStatic(Utilities.class)) {
			final ZonedDateTime time = ZonedDateTime.of(2026, 1, 16, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
			staticMock.when(() -> Utilities.utcNow()).thenReturn(time);
			staticMock.when(() -> Utilities.convertZonedDateTimeToUTCString(any(ZonedDateTime.class))).thenReturn("2026-1-16T10:05:00Z");

			final List<ServiceInstanceResponseDTO> result = driver.collectInterfaceTranslatorCandidates(List.of("generic_http"), List.of(target));

			assertNotNull(result);
			assertEquals(1, result.size());
			assertEquals("InterfaceTranslator|interfaceBridgeManagement|1.0.0", result.get(0).instanceId());

			verify(sysInfo).isSslEnabled();
			staticMock.verify(() -> Utilities.utcNow());
			staticMock.verify(() -> Utilities.convertZonedDateTimeToUTCString(any(ZonedDateTime.class)));
			verify(ahHttpService).consumeService(
					"serviceDiscovery",
					"lookup",
					"ServiceRegistry",
					ServiceInstanceListResponseDTO.class,
					payload);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectDataModelTranslatorCandidatesModelsNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectDataModelTranslatorCandidates(null));

		assertEquals("models list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectDataModelTranslatorCandidatesModelsEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectDataModelTranslatorCandidates(List.of()));

		assertEquals("models list is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectDataModelTranslatorCandidatesModelsContainsNull() {
		final List<TranslationDiscoveryModel> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.collectDataModelTranslatorCandidates(list));

		assertEquals("models list contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectDataModelTranslatorCandidatesOk1() {
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				"testXml",
				null);
		model.setTargetInputDataModelId("testJson");

		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("dataModelIds", Map.of("op", "CONTAINS", "value", List.of("testXml", "testJson")));

		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO(
				null,
				null,
				List.of("dataModelTranslation"),
				null,
				null,
				List.of(req),
				null,
				List.of("generic_http"),
				null,
				List.of("NONE"));

		final ServiceInstanceResponseDTO response = new ServiceInstanceResponseDTO("DataModelTranslator|dataModelTranslation|1.0.0", null, null, null, null, req, null, null, null);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(ahHttpService.consumeService(
				"serviceDiscovery",
				"lookup",
				"ServiceRegistry",
				ServiceInstanceListResponseDTO.class,
				payload)).thenReturn(new ServiceInstanceListResponseDTO(List.of(response), 1));

		final List<ServiceInstanceResponseDTO> result = driver.collectDataModelTranslatorCandidates(List.of(model));

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("DataModelTranslator|dataModelTranslation|1.0.0", result.get(0).instanceId());

		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(ahHttpService).consumeService(
				"serviceDiscovery",
				"lookup",
				"ServiceRegistry",
				ServiceInstanceListResponseDTO.class,
				payload);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCollectDataModelTranslatorCandidatesOk2() {
		final TranslationDiscoveryModel model = new TranslationDiscoveryModel(
				"TestProvider|testService|1.0.0",
				"TestProvider",
				"testService",
				"test-operation",
				"TestConsumer",
				null,
				"testXml");
		model.setTargetOutputDataModelId("testJson");

		final MetadataRequirementDTO req = new MetadataRequirementDTO();
		req.put("dataModelIds", Map.of("op", "CONTAINS", "value", List.of("testJson", "testXml")));

		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO(
				null,
				null,
				List.of("dataModelTranslation"),
				null,
				"2026-1-16T10:05:00Z",
				List.of(req),
				null,
				List.of("generic_https"),
				null,
				List.of("CERT_AUTH", "NONE"));

		final ServiceInstanceResponseDTO response = new ServiceInstanceResponseDTO("DataModelTranslator|dataModelTranslation|1.0.0", null, null, null, null, req, null, null, null);

		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(ahHttpService.consumeService(
				"serviceDiscovery",
				"lookup",
				"ServiceRegistry",
				ServiceInstanceListResponseDTO.class,
				payload)).thenReturn(new ServiceInstanceListResponseDTO(List.of(response), 1));

		ReflectionTestUtils.setField(driver, "translatorServiceMinAvailability", 5);

		try (MockedStatic<Utilities> staticMock = Mockito.mockStatic(Utilities.class)) {
			final ZonedDateTime time = ZonedDateTime.of(2026, 1, 16, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
			staticMock.when(() -> Utilities.utcNow()).thenReturn(time);
			staticMock.when(() -> Utilities.convertZonedDateTimeToUTCString(any(ZonedDateTime.class))).thenReturn("2026-1-16T10:05:00Z");

			final List<ServiceInstanceResponseDTO> result = driver.collectDataModelTranslatorCandidates(List.of(model));

			assertNotNull(result);
			assertEquals(1, result.size());
			assertEquals("DataModelTranslator|dataModelTranslation|1.0.0", result.get(0).instanceId());

			verify(sysInfo).isSslEnabled();
			verify(sysInfo).getAuthenticationPolicy();
			staticMock.verify(() -> Utilities.utcNow());
			staticMock.verify(() -> Utilities.convertZonedDateTimeToUTCString(any(ZonedDateTime.class)));
			verify(ahHttpService).consumeService(
					"serviceDiscovery",
					"lookup",
					"ServiceRegistry",
					ServiceInstanceListResponseDTO.class,
					payload);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetConfigurationForSystemNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.getConfigurationForSystem(null));

		assertEquals("system name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetConfigurationForSystemEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.getConfigurationForSystem(""));

		assertEquals("system name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCollectDataModelTranslatorFactoryCandidatesOk1() {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO(
				null,
				null,
				List.of("dataModelTranslatorFactoryControl"),
				null,
				null,
				null,
				null,
				List.of("generic_http"),
				null,
				List.of("NONE"));

		final ServiceInstanceResponseDTO resultElement = new ServiceInstanceResponseDTO("dataManagerTranslatorFactoryInstance", null, null, null, null, null, null, null, null);
		final ServiceInstanceListResponseDTO result = new ServiceInstanceListResponseDTO(List.of(resultElement), 1);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(ahHttpService.consumeService(
				"serviceDiscovery",
				"lookup",
				"ServiceRegistry",
				ServiceInstanceListResponseDTO.class,
				payload)).thenReturn(result);

		final List<ServiceInstanceResponseDTO> response = driver.collectDataModelTranslatorFactoryCandidates();

		assertNotNull(response);
		assertEquals(1, response.size());
		assertEquals("dataManagerTranslatorFactoryInstance", response.get(0).instanceId());

		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getAuthenticationPolicy();
		verify(ahHttpService).consumeService(
				"serviceDiscovery",
				"lookup",
				"ServiceRegistry",
				ServiceInstanceListResponseDTO.class,
				payload);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCollectDataModelTranslatorFactoryCandidatesOk2() {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO(
				null,
				null,
				List.of("dataModelTranslatorFactoryControl"),
				null,
				"2026-2-12T10:05:00Z",
				null,
				null,
				List.of("generic_https"),
				null,
				List.of("CERT_AUTH", "NONE"));

		final ServiceInstanceResponseDTO resultElement = new ServiceInstanceResponseDTO("dataManagerTranslatorFactoryInstance", null, null, null, null, null, null, null, null);
		final ServiceInstanceListResponseDTO result = new ServiceInstanceListResponseDTO(List.of(resultElement), 1);

		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(ahHttpService.consumeService(
				"serviceDiscovery",
				"lookup",
				"ServiceRegistry",
				ServiceInstanceListResponseDTO.class,
				payload)).thenReturn(result);

		ReflectionTestUtils.setField(driver, "translatorServiceMinAvailability", 5);

		try (MockedStatic<Utilities> staticMock = Mockito.mockStatic(Utilities.class)) {
			final ZonedDateTime time = ZonedDateTime.of(2026, 2, 12, 10, 0, 0, 0, ZoneId.of(Constants.UTC));
			staticMock.when(() -> Utilities.utcNow()).thenReturn(time);
			staticMock.when(() -> Utilities.convertZonedDateTimeToUTCString(any(ZonedDateTime.class))).thenReturn("2026-2-12T10:05:00Z");

			final List<ServiceInstanceResponseDTO> response = driver.collectDataModelTranslatorFactoryCandidates();

			assertNotNull(response);
			assertEquals(1, response.size());
			assertEquals("dataManagerTranslatorFactoryInstance", response.get(0).instanceId());

			verify(sysInfo).isSslEnabled();
			verify(sysInfo).getAuthenticationPolicy();
			staticMock.verify(() -> Utilities.utcNow());
			staticMock.verify(() -> Utilities.convertZonedDateTimeToUTCString(any(ZonedDateTime.class)));
			verify(ahHttpService).consumeService(
					"serviceDiscovery",
					"lookup",
					"ServiceRegistry",
					ServiceInstanceListResponseDTO.class,
					payload);
		}
	}
}