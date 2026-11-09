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
package eu.arrowhead.serviceorchestration.service.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.MultiValueMap;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.DataModelIdentifierValidator;
import eu.arrowhead.dto.AuthorizationTokenGenerationMgmtListRequestDTO;
import eu.arrowhead.dto.AuthorizationTokenMgmtListResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyListRequestDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyResponseDTO;
import eu.arrowhead.dto.BlacklistEntryDTO;
import eu.arrowhead.dto.BlacklistEntryListResponseDTO;
import eu.arrowhead.dto.BlacklistQueryRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.QoSRequirementDTO;
import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceListResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.TranslationBridgeCandidateDTO;
import eu.arrowhead.dto.TranslationDiscoveryMgmtRequestDTO;
import eu.arrowhead.dto.TranslationDiscoveryResponseDTO;
import eu.arrowhead.dto.TranslationNegotiationMgmtRequestDTO;
import eu.arrowhead.dto.TranslationNegotiationResponseDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.dto.enums.TranslationDiscoveryFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationConstants;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationLock;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationLockDbService;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.utils.matchmaker.ServiceInstanceMatchmaker;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:magicnumber")
public class LocalServiceOrchestrationTest {

    @Mock
    private eu.arrowhead.serviceorchestration.service.PlantRuleService plantRules;

	//=================================================================================================
	// members

	@InjectMocks
	private LocalServiceOrchestration orchestration;

	@Mock
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	@Mock
	private InterCloudServiceOrchestration interCloudOrch;

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private OrchestrationLockDbService orchLockDbService;

	@Mock
	private ArrowheadHttpService ahHttpService;

	@Mock(name = DynamicServiceOrchestrationConstants.SERVICE_INSTANCE_MATCHMAKER)
	private ServiceInstanceMatchmaker matchmaker;

	@Mock
	private ServiceInterfaceAddressPropertyProcessor interfaceAddressPropertyProcessor;

	@Spy
	private DataModelIdentifierNormalizer dataModelIDNormalizer;

	@Spy
	private DataModelIdentifierValidator dataModelIDValidator;

	@Captor
	private ArgumentCaptor<String> stringCaptor;

	@Captor
	private ArgumentCaptor<Set<Long>> longSetCaptor;

	@Captor
	private ArgumentCaptor<Collection<Long>> longCollectionCaptor;

	@Captor
	private ArgumentCaptor<List<String>> stringListCaptor;

	@Captor
	private ArgumentCaptor<ServiceInstanceLookupRequestDTO> serviceInstanceLookupRequestCaptor;

	@Captor
	private ArgumentCaptor<List<OrchestrationLock>> orchestrationLockListCaptor;

	@Captor
	private ArgumentCaptor<BlacklistQueryRequestDTO> blacklistQueryRequestCaptor;

	@Captor
	private ArgumentCaptor<AuthorizationVerifyListRequestDTO> authorizationVerifyRequestCaptor;

	@Captor
	private ArgumentCaptor<AuthorizationTokenGenerationMgmtListRequestDTO> authorizationTokenGenerationMgmtListRequestCaptor;

	@Captor
	private ArgumentCaptor<MultiValueMap<String, String>> queryParamCaptor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> stringObjectMapCaptor;

	@Captor
	private ArgumentCaptor<TranslationDiscoveryMgmtRequestDTO> translationDiscoveryReqCaptor;

	@Captor
	private ArgumentCaptor<TranslationNegotiationMgmtRequestDTO> translationNegotiationCaptor;

	private static final String testSerfviceDef = "testService";

	//=================================================================================================
	// assistant methods

    @Test
    public void emptyPlantScopeRejectsUnrelatedDiscoveredProviderAndIntercloudFallback() {
        final var requirement = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
        final var form = new OrchestrationForm("RequesterSystem", new OrchestrationRequestDTO(requirement, Map.of("ALLOW_INTERCLOUD", true), null, null));
        when(plantRules.find("RequesterSystem", testSerfviceDef)).thenReturn(new eu.arrowhead.dto.OrchestrationPlantRuleDTO("RequesterSystem", testSerfviceDef, List.of()));
        final var instance = new ServiceInstanceResponseDTO("Unrelated|testService|1.0.0", null, null, null, null, null, List.of(), null, null);
        when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
                .thenReturn(new ServiceInstanceListResponseDTO(List.of(instance), 1));
        assertTrue(orchestration.doLocalServiceOrchestration(UUID.randomUUID(), form).results().isEmpty());
        assertFalse(form.getFlag(OrchestrationFlag.ALLOW_INTERCLOUD));
        org.mockito.Mockito.verifyNoInteractions(interCloudOrch, matchmaker);
    }

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationResultlessServiceDiscoveryIntercloudNotEnabledAndNotAllowed() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().get(0));
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationResultlessServiceDiscoveryIntercloudIsEnabledButNotAllowed() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().get(0));
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationResultlessServiceDiscoveryIntercloudIsEnabledAndAllowed() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_INTERCLOUD.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);
		when(interCloudOrch.doInterCloudServiceOrchestration(eq(jobId), eq(form))).thenReturn(new OrchestrationResponseDTO(List.of(), List.of()));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch).doInterCloudServiceOrchestration(eq(jobId), eq(form));
		verify(orchJobDbService, never()).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), anyString());

		assertTrue(serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, serviceInstanceLookupRequestCaptor.getValue().serviceDefinitionNames().get(0));
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithoutInterfacesBecauseTranslationIsAllowedAndEnabled() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfacePropReq = new MetadataRequirementDTO();
		interfacePropReq.put("basePath", "/kelvin");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, List.of("generic_http"), List.of("IPV4"), List.of(interfacePropReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(sysInfo.isTranslationEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.interfaceTemplateNames() == null);
		assertTrue(lookupRequestDTO.addressTypes() == null);
		assertTrue(lookupRequestDTO.interfacePropertyRequirementsList() == null);
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithInterfacesBecauseTranslationIsAllowedAndButNotEnabled() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfacePropReq = new MetadataRequirementDTO();
		interfacePropReq.put("basePath", "/kelvin");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, List.of("generic_http"), List.of("IPV4"), List.of(interfacePropReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));
		when(sysInfo.isTranslationEnabled()).thenReturn(false);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.interfaceTemplateNames().size() == 1);
		assertEquals("generic_http", lookupRequestDTO.interfaceTemplateNames().get(0));
		assertTrue(lookupRequestDTO.addressTypes().size() == 1);
		assertEquals("IPV4", lookupRequestDTO.addressTypes().get(0));
		assertTrue(lookupRequestDTO.interfacePropertyRequirementsList().size() == 1);
		assertEquals("/kelvin", lookupRequestDTO.interfacePropertyRequirementsList().get(0).get("basePath"));
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithOnlyPreferredProviders() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, List.of("PreferredProvider"));
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_PREFERRED.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.providerNames().size() == 1);
		assertEquals("PreferredProvider", lookupRequestDTO.providerNames().get(0));
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithOnlyPreferredProvidersAndMatchmaking() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, List.of("TestProvider1", "TestProvider2"));
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_PREFERRED.name(), true, OrchestrationFlag.MATCHMAKING.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO prefCandidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO prefCandidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(prefCandidate2, true, false);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(prefCandidate1, prefCandidate2), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return candidateMatch;
		});

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.providerNames().size() == 2);
		assertEquals("TestProvider1", lookupRequestDTO.providerNames().get(0));
		assertEquals("TestProvider2", lookupRequestDTO.providerNames().get(1));

		assertTrue(matchmakingInput.size() == 2);
		assertEquals(prefCandidate1.instanceId(), matchmakingInput.get(0).getServiceInstance().instanceId());
		assertEquals(prefCandidate2.instanceId(), matchmakingInput.get(1).getServiceInstance().instanceId());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertEquals(candidateMatch.getServiceInstance().instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(!result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithPreferredProvidersButNotOnly() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, List.of("PreferredProvider"));
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(), 0));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, never()).getByServiceInstanceId(anyList());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.providerNames() == null);
		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithPreferredProvidersButNotOnlyWithMatchmaking() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, List.of("TestProvider2", "TestProvider3"));
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO notPrefCandidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO prefCandidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final ServiceInstanceResponseDTO prefCandidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(prefCandidate2, true, false);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(notPrefCandidate1, prefCandidate2, prefCandidate3), 3));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return candidateMatch;
		});

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.providerNames() == null);

		assertTrue(matchmakingInput.size() == 2);
		assertEquals(prefCandidate2.instanceId(), matchmakingInput.get(0).getServiceInstance().instanceId());
		assertEquals(prefCandidate3.instanceId(), matchmakingInput.get(1).getServiceInstance().instanceId());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 1);
		assertEquals(candidateMatch.getServiceInstance().instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(!result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationServiceDiscoveryWithPreferredProvidersButNotOnlyButPreferredProvidersAreLocked() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, List.of("TestProvider2", "TestProvider3"));
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO notPrefCandidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO prefCandidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final ServiceInstanceResponseDTO prefCandidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final OrchestrationLock tempLockRecord = new OrchestrationLock(UUID.randomUUID().toString(), prefCandidate2.instanceId(), requester, Utilities.utcNow().plusSeconds(60), true);
		tempLockRecord.setId(1);
		final OrchestrationLock mgmtLockRecord = new OrchestrationLock(UUID.randomUUID().toString(), prefCandidate3.instanceId(), requester, null, true);
		mgmtLockRecord.setId(2);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(notPrefCandidate1, prefCandidate2, prefCandidate2), 3));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of()).thenReturn(List.of(tempLockRecord, mgmtLockRecord));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lookupRequestDTO.providerNames() == null);

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 1);
		assertEquals(notPrefCandidate1.instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(!result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationOneOfTheCandidateIsLocked() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO lockedCandidate = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO notLockedCandidate = serviceInstanceResponseDTO("TestProvider2", true);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(lockedCandidate, notLockedCandidate), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of(new OrchestrationLock(UUID.randomUUID().toString(), lockedCandidate.instanceId(), null, Utilities.utcNow().plusMinutes(10), false)));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(stringListCaptor.capture());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		final List<String> lockCheckIdsFirst = stringListCaptor.getAllValues().get(0);
		final List<String> lockCheckIdsSecond = stringListCaptor.getAllValues().get(1);

		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lockCheckIdsFirst.size() == 2);
		assertTrue(lockCheckIdsFirst.contains(lockedCandidate.instanceId()));
		assertTrue(lockCheckIdsFirst.contains(notLockedCandidate.instanceId()));
		assertTrue(lockCheckIdsSecond.size() == 1);
		assertTrue(lockCheckIdsSecond.contains(notLockedCandidate.instanceId()));
		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals(notLockedCandidate.instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationOneOfTheCandidateIsLockedButTheLockIsExpired() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO lockExpiredCandidate = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO notLockedCandidate = serviceInstanceResponseDTO("TestProvider2", true);
		final OrchestrationLock lockRecord = new OrchestrationLock(UUID.randomUUID().toString(), lockExpiredCandidate.instanceId(), null, Utilities.utcNow().minusMinutes(1), false);
		lockRecord.setId(78);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(lockExpiredCandidate, notLockedCandidate), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList()))
				.thenReturn(List.of(lockRecord))
				.thenReturn(List.of());

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				serviceInstanceLookupRequestCaptor.capture(), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(stringListCaptor.capture());
		verify(orchLockDbService).deleteInBatch(longSetCaptor.capture());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final ServiceInstanceLookupRequestDTO lookupRequestDTO = serviceInstanceLookupRequestCaptor.getValue();
		final List<String> lockCheckIdsFirst = stringListCaptor.getAllValues().get(0);
		final List<String> lockCheckIdsSecond = stringListCaptor.getAllValues().get(1);
		final Set<Long> deleteLockIds = longSetCaptor.getValue();

		assertTrue(lookupRequestDTO.serviceDefinitionNames().size() == 1);
		assertEquals(testSerfviceDef, lookupRequestDTO.serviceDefinitionNames().get(0));
		assertTrue(lockCheckIdsFirst.size() == 2);
		assertTrue(lockCheckIdsFirst.contains(lockExpiredCandidate.instanceId()));
		assertTrue(lockCheckIdsFirst.contains(notLockedCandidate.instanceId()));
		assertTrue(deleteLockIds.size() == 1);
		assertTrue(deleteLockIds.contains(78L));
		assertTrue(lockCheckIdsSecond.size() == 2);
		assertTrue(lockCheckIdsSecond.contains(lockExpiredCandidate.instanceId()));
		assertTrue(lockCheckIdsSecond.contains(notLockedCandidate.instanceId()));
		assertTrue(stringCaptor.getValue().startsWith("2 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 2);
		assertEquals(lockExpiredCandidate.instanceId(), result.results().get(0).serviceInstanceId());
		assertEquals(notLockedCandidate.instanceId(), result.results().get(1).serviceInstanceId());
		assertTrue(result.warnings().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationExclusivityIsPreferred() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", 110, true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", 105, true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final OrchestrationLock tempLockRecord1 = new OrchestrationLock(jobId.toString(), candidate1.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord1.setId(1);
		final OrchestrationLock tempLockRecord2 = new OrchestrationLock(jobId.toString(), candidate2.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord2.setId(2);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(candidate2, true, false);
		candidateMatch.setCanBeExclusive(true);
		candidateMatch.setLocked(true);
		candidateMatch.setExclusivityDuration(105);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3), 3));
		when(orchLockDbService.create(anyList())).thenReturn(List.of(tempLockRecord1, tempLockRecord2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of()).thenReturn(List.of()).thenReturn(List.of(tempLockRecord1));
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return candidateMatch;
		});
		when(orchLockDbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate2.instanceId()), any(), eq(false))).thenReturn(Optional.of(tempLockRecord2));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(3)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService).create(orchestrationLockListCaptor.capture());
		verify(matchmaker).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate2.instanceId()), any(), eq(false));
		verify(orchLockDbService).deleteInBatch(longCollectionCaptor.capture());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<OrchestrationLock> tempLockCreateInput = orchestrationLockListCaptor.getValue();
		assertTrue(tempLockCreateInput.size() == 2);
		assertEquals(candidate1.instanceId(), tempLockCreateInput.get(0).getServiceInstanceId());
		assertEquals(candidate2.instanceId(), tempLockCreateInput.get(1).getServiceInstanceId());

		assertTrue(matchmakingInput.size() == 2);
		assertEquals(candidate1.instanceId(), matchmakingInput.get(0).getServiceInstance().instanceId());
		assertEquals(candidate2.instanceId(), matchmakingInput.get(1).getServiceInstance().instanceId());

		final Collection<Long> releaseLockInput = longCollectionCaptor.getValue();
		assertTrue(releaseLockInput.size() == 1);
		assertTrue(releaseLockInput.contains(tempLockRecord1.getId()));

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 1);
		assertEquals(candidateMatch.getServiceInstance().instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationExclusivityIsPreferredButPartlyFulfilled() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", 90, true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final OrchestrationLock tempLockRecord1 = new OrchestrationLock(jobId.toString(), candidate1.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord1.setId(1);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(candidate1, true, false);
		candidateMatch.setCanBeExclusive(true);
		candidateMatch.setLocked(true);
		candidateMatch.setExclusivityDuration(90);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.create(anyList())).thenReturn(List.of(tempLockRecord1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return candidateMatch;
		});
		when(orchLockDbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate1.instanceId()), any(), eq(false))).thenReturn(Optional.of(tempLockRecord1));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService).create(orchestrationLockListCaptor.capture());
		verify(matchmaker).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate1.instanceId()), any(), eq(false));
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<OrchestrationLock> tempLockCreateInput = orchestrationLockListCaptor.getValue();
		assertTrue(tempLockCreateInput.size() == 1);
		assertEquals(candidate1.instanceId(), tempLockCreateInput.get(0).getServiceInstanceId());

		assertTrue(matchmakingInput.size() == 1);
		assertEquals(candidate1.instanceId(), matchmakingInput.get(0).getServiceInstance().instanceId());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 1);
		assertEquals(candidateMatch.getServiceInstance().instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
		assertTrue(result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_PART_TIME_EXCLUSIVITY));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationExclusivityIsPreferredButPartlyFulfilledWithExplicitMatchmaking() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", 90, true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final OrchestrationLock tempLockRecord1 = new OrchestrationLock(jobId.toString(), candidate1.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord1.setId(1);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(candidate1, true, false);
		candidateMatch.setCanBeExclusive(true);
		candidateMatch.setLocked(true);
		candidateMatch.setExclusivityDuration(90);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.create(anyList())).thenReturn(List.of(tempLockRecord1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return candidateMatch;
		});
		when(orchLockDbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate1.instanceId()), any(), eq(false))).thenReturn(Optional.of(tempLockRecord1));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService).create(orchestrationLockListCaptor.capture());
		verify(matchmaker).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate1.instanceId()), any(), eq(false));
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<OrchestrationLock> tempLockCreateInput = orchestrationLockListCaptor.getValue();
		assertTrue(tempLockCreateInput.size() == 1);
		assertEquals(candidate1.instanceId(), tempLockCreateInput.get(0).getServiceInstanceId());

		assertTrue(matchmakingInput.size() == 1);
		assertEquals(candidate1.instanceId(), matchmakingInput.get(0).getServiceInstance().instanceId());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 1);
		assertEquals(candidateMatch.getServiceInstance().instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(!result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
		assertTrue(result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_PART_TIME_EXCLUSIVITY));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationExclusivityIsPreferredButNotFulfilled() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(candidate1, true, false);
		candidateMatch.setCanBeExclusive(true);
		candidateMatch.setLocked(true);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(orchestrationLockListCaptor.capture());
		verify(matchmaker, never()).doMatchmaking(any(), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(stringCaptor.getValue().startsWith("2 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 2);
		assertEquals(candidate1.instanceId(), result.results().get(0).serviceInstanceId());
		assertEquals(candidate2.instanceId(), result.results().get(1).serviceInstanceId());
		assertTrue(!result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
		assertTrue(result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_NOT_EXCLUSIVE));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationExclusivityIsPreferredButNotFulfilledWithMatchmaking() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.MATCHMAKING.name(), true), null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(candidate1, true, false);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return candidateMatch;
		});

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(orchestrationLockListCaptor.capture());
		verify(matchmaker).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyList());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 1);
		assertEquals(candidateMatch.getServiceInstance().instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(!result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
		assertTrue(result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_NOT_EXCLUSIVE));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationOnlyExclusive() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_EXCLUSIVE.name(), true), null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", 110, true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", 105, true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final OrchestrationLock tempLockRecord1 = new OrchestrationLock(jobId.toString(), candidate1.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord1.setId(1);
		final OrchestrationLock tempLockRecord2 = new OrchestrationLock(jobId.toString(), candidate2.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord2.setId(2);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(candidate2, true, false);
		candidateMatch.setCanBeExclusive(true);
		candidateMatch.setLocked(true);
		candidateMatch.setExclusivityDuration(105);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3), 3));
		when(orchLockDbService.create(anyList())).thenReturn(List.of(tempLockRecord1, tempLockRecord2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of()).thenReturn(List.of()).thenReturn(List.of(tempLockRecord1));
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return candidateMatch;
		});
		when(orchLockDbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate2.instanceId()), any(), eq(false))).thenReturn(Optional.of(tempLockRecord2));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(3)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService).create(orchestrationLockListCaptor.capture());
		verify(matchmaker).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate2.instanceId()), any(), eq(false));
		verify(orchLockDbService).deleteInBatch(longCollectionCaptor.capture());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<OrchestrationLock> tempLockCreateInput = orchestrationLockListCaptor.getValue();
		assertTrue(tempLockCreateInput.size() == 2);
		assertEquals(candidate1.instanceId(), tempLockCreateInput.get(0).getServiceInstanceId());
		assertEquals(candidate2.instanceId(), tempLockCreateInput.get(1).getServiceInstanceId());

		assertTrue(matchmakingInput.size() == 2);
		assertEquals(candidate1.instanceId(), matchmakingInput.get(0).getServiceInstance().instanceId());
		assertEquals(candidate2.instanceId(), matchmakingInput.get(1).getServiceInstance().instanceId());

		final Collection<Long> releaseLockInput = longCollectionCaptor.getValue();
		assertTrue(releaseLockInput.size() == 1);
		assertTrue(releaseLockInput.contains(tempLockRecord1.getId()));

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 1);
		assertEquals(candidateMatch.getServiceInstance().instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationOnlyExclusiveWithExplicitMatchmaking() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_EXCLUSIVE.name(), true, OrchestrationFlag.MATCHMAKING.name(), true), null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", 110, true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", 105, true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final OrchestrationLock tempLockRecord1 = new OrchestrationLock(jobId.toString(), candidate1.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord1.setId(1);
		final OrchestrationLock tempLockRecord2 = new OrchestrationLock(jobId.toString(), candidate2.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord2.setId(2);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(candidate2, true, false);
		candidateMatch.setCanBeExclusive(true);
		candidateMatch.setLocked(true);
		candidateMatch.setExclusivityDuration(105);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3), 3));
		when(orchLockDbService.create(anyList())).thenReturn(List.of(tempLockRecord1, tempLockRecord2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of()).thenReturn(List.of()).thenReturn(List.of(tempLockRecord1));
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return candidateMatch;
		});
		when(orchLockDbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate2.instanceId()), any(), eq(false))).thenReturn(Optional.of(tempLockRecord2));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(3)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService).create(orchestrationLockListCaptor.capture());
		verify(matchmaker).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate2.instanceId()), any(), eq(false));
		verify(orchLockDbService).deleteInBatch(longCollectionCaptor.capture());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<OrchestrationLock> tempLockCreateInput = orchestrationLockListCaptor.getValue();
		assertTrue(tempLockCreateInput.size() == 2);
		assertEquals(candidate1.instanceId(), tempLockCreateInput.get(0).getServiceInstanceId());
		assertEquals(candidate2.instanceId(), tempLockCreateInput.get(1).getServiceInstanceId());

		assertTrue(matchmakingInput.size() == 2);
		assertEquals(candidate1.instanceId(), matchmakingInput.get(0).getServiceInstance().instanceId());
		assertEquals(candidate2.instanceId(), matchmakingInput.get(1).getServiceInstance().instanceId());

		final Collection<Long> releaseLockInput = longCollectionCaptor.getValue();
		assertTrue(releaseLockInput.size() == 1);
		assertTrue(releaseLockInput.contains(tempLockRecord1.getId()));

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 1);
		assertEquals(candidateMatch.getServiceInstance().instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(!result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationOnlyExclusiveButNotFulfilled() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_EXCLUSIVE.name(), true), null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3), 3));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());

		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationOnlyExclusiveButNotFulfilledBecauseCandidatesBecameLockedMeanwhile() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_EXCLUSIVE.name(), true), null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", 110, true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", 105, true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final OrchestrationLock tempLockRecord = new OrchestrationLock(UUID.randomUUID().toString(), candidate1.instanceId(), requester, Utilities.utcNow().plusSeconds(60), true);
		tempLockRecord.setId(1);
		final OrchestrationLock mgmtLockRecord = new OrchestrationLock(UUID.randomUUID().toString(), candidate2.instanceId(), requester, null, true);
		mgmtLockRecord.setId(2);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3), 3));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of()).thenReturn(List.of(tempLockRecord, mgmtLockRecord));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(any(), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationOnlyExclusiveButAllowedExclusivityIsNotANumber() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_EXCLUSIVE.name(), true), null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = new ServiceInstanceResponseDTO(
				"TestProvider1|testService|1.0.0",
				new SystemResponseDTO("TestProvider1", null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO("testService", null, null),
				null,
				null,
				Map.of(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY, "not-a-number"),
				new ArrayList<>(),
				null,
				null);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());

		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationOnlyExclusiveButNegativeAllowedExclusivity() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_EXCLUSIVE.name(), true), null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", -6, true);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());

		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationOnlyExclusiveTheTemporaryLockHasExpireBeforeFinalReservation() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ONLY_EXCLUSIVE.name(), true), null, 100);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", 110, true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", 105, true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final OrchestrationLock tempLockRecord1 = new OrchestrationLock(jobId.toString(), candidate1.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord1.setId(1);
		final OrchestrationLock tempLockRecord2 = new OrchestrationLock(jobId.toString(), candidate2.instanceId(), requester, Utilities.utcNow().plusSeconds(30), true);
		tempLockRecord2.setId(2);
		final OrchestrationCandidate candidateMatch = new OrchestrationCandidate(candidate2, true, false);
		candidateMatch.setCanBeExclusive(true);
		candidateMatch.setLocked(true);
		candidateMatch.setExclusivityDuration(105);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3), 3));
		when(orchLockDbService.create(anyList())).thenReturn(List.of(tempLockRecord1, tempLockRecord2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of()).thenReturn(List.of()).thenReturn(List.of(tempLockRecord1));
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return candidateMatch;
		});
		when(orchLockDbService.changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate2.instanceId()), any(), eq(false))).thenReturn(Optional.empty());

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(3)).getByServiceInstanceId(anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, times(2)).create(orchestrationLockListCaptor.capture());
		verify(matchmaker).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(eq(jobId.toString()), eq(candidate2.instanceId()), any(), eq(false));
		verify(orchLockDbService).deleteInBatch(longCollectionCaptor.capture());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<List<OrchestrationLock>> tempLockCreateInputList = orchestrationLockListCaptor.getAllValues();
		assertTrue(tempLockCreateInputList.size() == 2);
		List<OrchestrationLock> tempLockCreateInput1 = tempLockCreateInputList.get(0);
		assertTrue(tempLockCreateInput1.size() == 2);
		assertEquals(candidate1.instanceId(), tempLockCreateInput1.get(0).getServiceInstanceId());
		assertEquals(candidate2.instanceId(), tempLockCreateInput1.get(1).getServiceInstanceId());
		assertTrue(tempLockCreateInput1.get(0).isTemporary());
		assertTrue(tempLockCreateInput1.get(1).isTemporary());
		List<OrchestrationLock> tempLockCreateInput2 = tempLockCreateInputList.get(1);
		assertTrue(tempLockCreateInput2.size() == 1);
		assertEquals(candidate2.instanceId(), tempLockCreateInput2.get(0).getServiceInstanceId());
		assertTrue(!tempLockCreateInput2.get(0).isTemporary());

		assertTrue(matchmakingInput.size() == 2);
		assertEquals(candidate1.instanceId(), matchmakingInput.get(0).getServiceInstance().instanceId());
		assertEquals(candidate2.instanceId(), matchmakingInput.get(1).getServiceInstance().instanceId());

		final Collection<Long> releaseLockInput = longCollectionCaptor.getValue();
		assertTrue(releaseLockInput.size() == 1);
		assertTrue(releaseLockInput.contains(tempLockRecord1.getId()));

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());

		assertTrue(result.results().size() == 1);
		assertEquals(candidateMatch.getServiceInstance().instanceId(), result.results().get(0).serviceInstanceId());
		assertTrue(result.warnings().contains(DynamicServiceOrchestrationConstants.ORCH_WARN_AUTO_MATCHMAKING));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationCandidatesAreBlackListed() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final ServiceInstanceResponseDTO candidate4 = serviceInstanceResponseDTO("TestProvider4", true);
		final BlacklistEntryDTO blacklisted1 = new BlacklistEntryDTO(candidate1.provider().name(), null, null, null, null, null, null, true);
		final BlacklistEntryDTO blacklisted2 = new BlacklistEntryDTO(candidate2.provider().name(), null, null, null, null, null, null, true);
		final BlacklistEntryDTO blacklisted3 = new BlacklistEntryDTO(candidate3.provider().name(), null, null, null, null, null, null, true);
		final BlacklistEntryDTO blacklisted4 = new BlacklistEntryDTO(candidate4.provider().name(), null, null, null, null, null, null, true);
		final BlacklistEntryListResponseDTO blacklisResponsePage1 = new BlacklistEntryListResponseDTO(List.of(blacklisted1, blacklisted2), 4);
		final BlacklistEntryListResponseDTO blacklisResponsePage2 = new BlacklistEntryListResponseDTO(List.of(blacklisted3, blacklisted4), 4);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3, candidate4), 4));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isBlacklistEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST), eq(BlacklistEntryListResponseDTO.class),
				blacklistQueryRequestCaptor.capture())).thenReturn(blacklisResponsePage1).thenReturn(blacklisResponsePage2);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(4)).getBlacklistCheckExcludeList();
		verify(ahHttpService, times(2)).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST), eq(BlacklistEntryListResponseDTO.class),
				any(BlacklistQueryRequestDTO.class));
		verify(sysInfo, never()).isAuthorizationEnabled();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<BlacklistQueryRequestDTO> blacklistQueryRequestDTOs = blacklistQueryRequestCaptor.getAllValues();
		assertTrue(blacklistQueryRequestDTOs.size() == 2);
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate1.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate2.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate3.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate4.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().page() == null);
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().size() == null);
		assertTrue(blacklistQueryRequestDTOs.get(1).systemNames().contains(candidate1.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(1).systemNames().contains(candidate2.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(1).systemNames().contains(candidate3.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(1).systemNames().contains(candidate4.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(1).pagination().page() == 1);
		assertTrue(blacklistQueryRequestDTOs.get(1).pagination().size() == 2);

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationCandidatesAreBlackListedButOneIsOnTheExcludeList() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final ServiceInstanceResponseDTO candidate4 = serviceInstanceResponseDTO("TestProvider4", true);
		final BlacklistEntryDTO blacklisted1 = new BlacklistEntryDTO(candidate1.provider().name(), null, null, null, null, null, null, true);
		final BlacklistEntryDTO blacklisted2 = new BlacklistEntryDTO(candidate2.provider().name(), null, null, null, null, null, null, true);
		final BlacklistEntryDTO blacklisted4 = new BlacklistEntryDTO(candidate4.provider().name(), null, null, null, null, null, null, true);
		final BlacklistEntryListResponseDTO blacklisResponsePage1 = new BlacklistEntryListResponseDTO(List.of(blacklisted1, blacklisted2), 3);
		final BlacklistEntryListResponseDTO blacklisResponsePage2 = new BlacklistEntryListResponseDTO(List.of(blacklisted4), 3);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3, candidate4), 4));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isBlacklistEnabled()).thenReturn(true);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of("TestProvider3"));
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST), eq(BlacklistEntryListResponseDTO.class),
				blacklistQueryRequestCaptor.capture())).thenReturn(blacklisResponsePage1).thenReturn(blacklisResponsePage2);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(4)).getBlacklistCheckExcludeList();
		verify(ahHttpService, times(2)).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST), eq(BlacklistEntryListResponseDTO.class),
				any(BlacklistQueryRequestDTO.class));
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<BlacklistQueryRequestDTO> blacklistQueryRequestDTOs = blacklistQueryRequestCaptor.getAllValues();
		assertTrue(blacklistQueryRequestDTOs.size() == 2);
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate1.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate2.provider().name()));
		assertTrue(!blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate3.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate4.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().page() == null);
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().size() == null);
		assertTrue(blacklistQueryRequestDTOs.get(1).systemNames().contains(candidate1.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(1).systemNames().contains(candidate2.provider().name()));
		assertTrue(!blacklistQueryRequestDTOs.get(1).systemNames().contains(candidate3.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(1).systemNames().contains(candidate4.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(1).pagination().page() == 1);
		assertTrue(blacklistQueryRequestDTOs.get(1).pagination().size() == 2);

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationBlacklistIsUnavailableButForced() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final ServiceInstanceResponseDTO candidate4 = serviceInstanceResponseDTO("TestProvider4", true);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3, candidate4), 4));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isBlacklistEnabled()).thenReturn(true);
		doThrow(new ArrowheadException("test")).when(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST),
				eq(BlacklistEntryListResponseDTO.class), blacklistQueryRequestCaptor.capture());
		when(sysInfo.isBlacklistForced()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(4)).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST), eq(BlacklistEntryListResponseDTO.class),
				any(BlacklistQueryRequestDTO.class));
		verify(sysInfo).isBlacklistForced();
		verify(sysInfo, never()).isAuthorizationEnabled();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<BlacklistQueryRequestDTO> blacklistQueryRequestDTOs = blacklistQueryRequestCaptor.getAllValues();
		assertTrue(blacklistQueryRequestDTOs.size() == 1);
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate1.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate2.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate3.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate4.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().page() == null);
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().size() == null);

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationBlacklistIsUnavailableButNotForced() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final ServiceInstanceResponseDTO candidate4 = serviceInstanceResponseDTO("TestProvider4", true);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3, candidate4), 4));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isBlacklistEnabled()).thenReturn(true);
		doThrow(new ArrowheadException("test")).when(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST),
				eq(BlacklistEntryListResponseDTO.class), blacklistQueryRequestCaptor.capture());
		when(sysInfo.isBlacklistForced()).thenReturn(false);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(4)).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST), eq(BlacklistEntryListResponseDTO.class),
				any(BlacklistQueryRequestDTO.class));
		verify(sysInfo).isBlacklistForced();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<BlacklistQueryRequestDTO> blacklistQueryRequestDTOs = blacklistQueryRequestCaptor.getAllValues();
		assertTrue(blacklistQueryRequestDTOs.size() == 1);
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate1.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate2.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate3.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate4.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().page() == null);
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().size() == null);

		assertTrue(stringCaptor.getValue().startsWith("4 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 4);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationBlacklistThrowsForbbidenException() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final ServiceInstanceResponseDTO candidate4 = serviceInstanceResponseDTO("TestProvider4", true);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3, candidate4), 4));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isBlacklistEnabled()).thenReturn(true);
		doThrow(new ForbiddenException("test message", "test.origin")).when(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST),
				eq(BlacklistEntryListResponseDTO.class), blacklistQueryRequestCaptor.capture());

		final Throwable ex = assertThrows(Throwable.class, () -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(4)).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST), eq(BlacklistEntryListResponseDTO.class),
				any(BlacklistQueryRequestDTO.class));
		verify(sysInfo, never()).isBlacklistForced();
		verify(sysInfo, never()).isAuthorizationEnabled();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), stringCaptor.capture());

		final List<BlacklistQueryRequestDTO> blacklistQueryRequestDTOs = blacklistQueryRequestCaptor.getAllValues();
		assertTrue(blacklistQueryRequestDTOs.size() == 1);
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate1.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate2.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate3.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate4.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().page() == null);
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().size() == null);

		assertEquals("test message", stringCaptor.getValue());
		assertEquals("test message", ex.getMessage());
		assertTrue(ex instanceof ForbiddenException);
		assertEquals("test.origin", ((ForbiddenException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationBlacklistThrowsAuthException() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);
		final ServiceInstanceResponseDTO candidate3 = serviceInstanceResponseDTO("TestProvider3", true);
		final ServiceInstanceResponseDTO candidate4 = serviceInstanceResponseDTO("TestProvider4", true);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2, candidate3, candidate4), 4));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isBlacklistEnabled()).thenReturn(true);
		doThrow(new AuthException("test message", "test.origin")).when(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST),
				eq(BlacklistEntryListResponseDTO.class), blacklistQueryRequestCaptor.capture());

		final Throwable ex = assertThrows(Throwable.class, () -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(4)).getBlacklistCheckExcludeList();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_BLACKLIST_MANAGEMENT), eq(Constants.SERVICE_OP_BLACKLIST_QUERY), eq(Constants.SYS_NAME_BLACKLIST), eq(BlacklistEntryListResponseDTO.class),
				any(BlacklistQueryRequestDTO.class));
		verify(sysInfo, never()).isBlacklistForced();
		verify(sysInfo, never()).isAuthorizationEnabled();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), stringCaptor.capture());

		final List<BlacklistQueryRequestDTO> blacklistQueryRequestDTOs = blacklistQueryRequestCaptor.getAllValues();
		assertTrue(blacklistQueryRequestDTOs.size() == 1);
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate1.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate2.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate3.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).systemNames().contains(candidate4.provider().name()));
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().page() == null);
		assertTrue(blacklistQueryRequestDTOs.get(0).pagination().size() == null);

		assertEquals("test message", stringCaptor.getValue());
		assertEquals("test message", ex.getMessage());
		assertTrue(ex instanceof AuthException);
		assertEquals("test.origin", ((AuthException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationAuthorizationIsEnabled() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);

		final AuthorizationVerifyResponseDTO authRespDTO1 = new AuthorizationVerifyResponseDTO(candidate1.provider().name(), "TestConsumer", null, AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, null, true);
		final AuthorizationVerifyResponseDTO authRespDTO2 = new AuthorizationVerifyResponseDTO(candidate2.provider().name(), "TestConsumer", null, AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, null, true);
		final AuthorizationVerifyListResponseDTO authRespList = new AuthorizationVerifyListResponseDTO(List.of(authRespDTO1, authRespDTO2), 2);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isAuthorizationEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION), eq(AuthorizationVerifyListResponseDTO.class),
				authorizationVerifyRequestCaptor.capture())).thenReturn(authRespList);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationVerifyListResponseDTO.class), any(AuthorizationVerifyListRequestDTO.class));
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final AuthorizationVerifyListRequestDTO authCheckRequest = authorizationVerifyRequestCaptor.getValue();
		assertTrue(authCheckRequest.list().size() == 2);
		assertEquals(candidate1.provider().name(), authCheckRequest.list().get(0).provider());
		assertEquals(requester, authCheckRequest.list().get(0).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(0).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(0).targetType());
		assertNull(authCheckRequest.list().get(0).scope());
		assertNull(authCheckRequest.list().get(0).cloud());
		assertEquals(candidate2.provider().name(), authCheckRequest.list().get(1).provider());
		assertEquals(requester, authCheckRequest.list().get(1).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(1).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(1).targetType());
		assertNull(authCheckRequest.list().get(1).scope());
		assertNull(authCheckRequest.list().get(1).cloud());

		assertTrue(stringCaptor.getValue().startsWith("2 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 2);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationAuthorizationIsEnabledWithOperation() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, List.of("test-operation"), null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);

		final AuthorizationVerifyResponseDTO authRespDTO1 = new AuthorizationVerifyResponseDTO(candidate1.provider().name(), "TestConsumer", null, AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, null, true);
		final AuthorizationVerifyResponseDTO authRespDTO2 = new AuthorizationVerifyResponseDTO(candidate2.provider().name(), "TestConsumer", null, AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, null, true);
		final AuthorizationVerifyListResponseDTO authRespList = new AuthorizationVerifyListResponseDTO(List.of(authRespDTO1, authRespDTO2), 2);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isAuthorizationEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION), eq(AuthorizationVerifyListResponseDTO.class),
				authorizationVerifyRequestCaptor.capture())).thenReturn(authRespList);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationVerifyListResponseDTO.class), any(AuthorizationVerifyListRequestDTO.class));
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final AuthorizationVerifyListRequestDTO authCheckRequest = authorizationVerifyRequestCaptor.getValue();
		assertTrue(authCheckRequest.list().size() == 2);
		assertEquals(candidate1.provider().name(), authCheckRequest.list().get(0).provider());
		assertEquals(requester, authCheckRequest.list().get(0).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(0).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(0).targetType());
		assertEquals("test-operation", authCheckRequest.list().get(0).scope());
		assertNull(authCheckRequest.list().get(0).cloud());
		assertEquals(candidate2.provider().name(), authCheckRequest.list().get(1).provider());
		assertEquals(requester, authCheckRequest.list().get(1).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(1).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(1).targetType());
		assertEquals("test-operation", authCheckRequest.list().get(0).scope());
		assertNull(authCheckRequest.list().get(1).cloud());

		assertTrue(stringCaptor.getValue().startsWith("2 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 2);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationAuthorizationIsEnabledEmptyResponse() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isAuthorizationEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION), eq(AuthorizationVerifyListResponseDTO.class),
				authorizationVerifyRequestCaptor.capture())).thenReturn(new AuthorizationVerifyListResponseDTO(List.of(), 0));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo).isAuthorizationEnabled();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationVerifyListResponseDTO.class), any(AuthorizationVerifyListRequestDTO.class));
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final AuthorizationVerifyListRequestDTO authCheckRequest = authorizationVerifyRequestCaptor.getValue();
		assertTrue(authCheckRequest.list().size() == 2);
		assertEquals(candidate1.provider().name(), authCheckRequest.list().get(0).provider());
		assertEquals(requester, authCheckRequest.list().get(0).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(0).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(0).targetType());
		assertNull(authCheckRequest.list().get(0).scope());
		assertNull(authCheckRequest.list().get(0).cloud());
		assertEquals(candidate2.provider().name(), authCheckRequest.list().get(1).provider());
		assertEquals(requester, authCheckRequest.list().get(1).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(1).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(1).targetType());
		assertNull(authCheckRequest.list().get(1).scope());
		assertNull(authCheckRequest.list().get(1).cloud());

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationAuthorizationIsEnabledOneIsNotGranted() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", true);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", true);

		final AuthorizationVerifyResponseDTO authRespDTO1 = new AuthorizationVerifyResponseDTO(candidate1.provider().name(), "TestConsumer", null, AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, null, true);
		final AuthorizationVerifyResponseDTO authRespDTO2 = new AuthorizationVerifyResponseDTO(candidate2.provider().name(), "TestConsumer", null, AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, null, false);
		final AuthorizationVerifyListResponseDTO authRespList = new AuthorizationVerifyListResponseDTO(List.of(authRespDTO1, authRespDTO2), 2);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isAuthorizationEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION), eq(AuthorizationVerifyListResponseDTO.class),
				authorizationVerifyRequestCaptor.capture())).thenReturn(authRespList);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationVerifyListResponseDTO.class), any(AuthorizationVerifyListRequestDTO.class));
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final AuthorizationVerifyListRequestDTO authCheckRequest = authorizationVerifyRequestCaptor.getValue();
		assertTrue(authCheckRequest.list().size() == 2);
		assertEquals(candidate1.provider().name(), authCheckRequest.list().get(0).provider());
		assertEquals(requester, authCheckRequest.list().get(0).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(0).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(0).targetType());
		assertNull(authCheckRequest.list().get(0).scope());
		assertNull(authCheckRequest.list().get(0).cloud());
		assertEquals(candidate2.provider().name(), authCheckRequest.list().get(1).provider());
		assertEquals(requester, authCheckRequest.list().get(1).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(1).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(1).targetType());
		assertNull(authCheckRequest.list().get(1).scope());
		assertNull(authCheckRequest.list().get(1).cloud());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals(candidate1.provider().name(), result.results().get(0).providerName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationAuthorizationIsEnabledAndAuthTokenIsRequired() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidate1Interface = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", null);
		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", List.of(candidate1Interface));
		final ServiceInstanceInterfaceResponseDTO candidate2Interface = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "CERTIFICATE", null);
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider1", List.of(candidate2Interface));

		final AuthorizationVerifyResponseDTO authRespDTO1 = new AuthorizationVerifyResponseDTO(candidate1.provider().name(), "TestConsumer", null, AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, null, true);
		final AuthorizationVerifyListResponseDTO authRespList = new AuthorizationVerifyListResponseDTO(List.of(authRespDTO1), 1);

		final AuthorizationTokenResponseDTO tokenResponseDTO1 = new AuthorizationTokenResponseDTO(AuthorizationTokenType.TIME_LIMITED_TOKEN, "TIME_LIMITED_TOKEN_AUTH", "thisistherawtoken", "thisisthetokereference",
				"DynamicServiceOrchestration", "LOCAL", requester, candidate1.provider().name(), AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, null, null, null, null, null);
		final AuthorizationTokenMgmtListResponseDTO tokenListResponseDTO = new AuthorizationTokenMgmtListResponseDTO(List.of(tokenResponseDTO1), 1);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isAuthorizationEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION), eq(AuthorizationVerifyListResponseDTO.class),
				authorizationVerifyRequestCaptor.capture())).thenReturn(authRespList);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationTokenMgmtListResponseDTO.class), authorizationTokenGenerationMgmtListRequestCaptor.capture(), queryParamCaptor.capture())).thenReturn(tokenListResponseDTO);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationVerifyListResponseDTO.class), any(AuthorizationVerifyListRequestDTO.class));
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationTokenMgmtListResponseDTO.class), any(AuthorizationTokenGenerationMgmtListRequestDTO.class), any());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final AuthorizationVerifyListRequestDTO authCheckRequest = authorizationVerifyRequestCaptor.getValue();
		assertTrue(authCheckRequest.list().size() == 2);
		assertEquals(candidate1.provider().name(), authCheckRequest.list().get(0).provider());
		assertEquals(requester, authCheckRequest.list().get(0).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(0).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(0).targetType());
		assertNull(authCheckRequest.list().get(0).scope());
		assertNull(authCheckRequest.list().get(0).cloud());
		assertEquals(candidate2.provider().name(), authCheckRequest.list().get(1).provider());
		assertEquals(requester, authCheckRequest.list().get(1).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(1).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(1).targetType());
		assertNull(authCheckRequest.list().get(1).scope());
		assertNull(authCheckRequest.list().get(1).cloud());

		final AuthorizationTokenGenerationMgmtListRequestDTO tokenRequest = authorizationTokenGenerationMgmtListRequestCaptor.getValue();
		assertTrue(tokenRequest.list().size() == 1);
		assertEquals("TIME_LIMITED_TOKEN_AUTH", tokenRequest.list().get(0).tokenVariant());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), tokenRequest.list().get(0).targetType());
		assertEquals("LOCAL", tokenRequest.list().get(0).consumerCloud());
		assertEquals(requester, tokenRequest.list().get(0).consumer());
		assertEquals(candidate1.provider().name(), tokenRequest.list().get(0).provider());
		assertEquals(testSerfviceDef, tokenRequest.list().get(0).target());
		assertEquals("", tokenRequest.list().get(0).scope());
		assertNull(tokenRequest.list().get(0).expiresAt());
		assertNull(tokenRequest.list().get(0).usageLimit());

		final MultiValueMap<String, String> tokenRequestqueryParams = queryParamCaptor.getValue();
		assertTrue(tokenRequestqueryParams.containsKey(Constants.UNBOUND));
		assertEquals("true", tokenRequestqueryParams.get(Constants.UNBOUND).get(0));

		assertTrue(stringCaptor.getValue().startsWith("2 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 2);
		assertEquals("thisistherawtoken", result.results().get(0).authorizationTokens().get("TIME_LIMITED_TOKEN_AUTH").get(testSerfviceDef).token());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationAuthorizationIsEnabledAndAuthTokenIsRequiredWithOperation() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, List.of("test-operation"), null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidate1Interface = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", null);
		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", List.of(candidate1Interface));

		final AuthorizationVerifyResponseDTO authRespDTO1 = new AuthorizationVerifyResponseDTO(candidate1.provider().name(), "TestConsumer", null, AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, "test-operation", true);
		final AuthorizationVerifyListResponseDTO authRespList = new AuthorizationVerifyListResponseDTO(List.of(authRespDTO1), 1);

		final AuthorizationTokenResponseDTO tokenResponseDTO1 = new AuthorizationTokenResponseDTO(AuthorizationTokenType.TIME_LIMITED_TOKEN, "TIME_LIMITED_TOKEN_AUTH", "thisistherawtoken", "thisisthetokereference",
				"DynamicServiceOrchestration", "LOCAL", requester, candidate1.provider().name(), AuthorizationTargetType.SERVICE_DEF, testSerfviceDef, "test-operation", null, null, null, null);
		final AuthorizationTokenMgmtListResponseDTO tokenListResponseDTO = new AuthorizationTokenMgmtListResponseDTO(List.of(tokenResponseDTO1), 1);

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isAuthorizationEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION), eq(AuthorizationVerifyListResponseDTO.class),
				authorizationVerifyRequestCaptor.capture())).thenReturn(authRespList);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationTokenMgmtListResponseDTO.class), authorizationTokenGenerationMgmtListRequestCaptor.capture(), queryParamCaptor.capture())).thenReturn(tokenListResponseDTO);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_CHECK_POLICIES), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationVerifyListResponseDTO.class), any(AuthorizationVerifyListRequestDTO.class));
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION_TOKEN_MANAGEMENT), eq(Constants.SERVICE_OP_AUTHORIZATION_GENERATE_TOKENS), eq(Constants.SYS_NAME_CONSUMER_AUTHORIZATION),
				eq(AuthorizationTokenMgmtListResponseDTO.class), any(AuthorizationTokenGenerationMgmtListRequestDTO.class), any());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final AuthorizationVerifyListRequestDTO authCheckRequest = authorizationVerifyRequestCaptor.getValue();
		assertTrue(authCheckRequest.list().size() == 1);
		assertEquals(candidate1.provider().name(), authCheckRequest.list().get(0).provider());
		assertEquals(requester, authCheckRequest.list().get(0).consumer());
		assertEquals(testSerfviceDef, authCheckRequest.list().get(0).target());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), authCheckRequest.list().get(0).targetType());
		assertEquals("test-operation", authCheckRequest.list().get(0).scope());
		assertNull(authCheckRequest.list().get(0).cloud());

		final AuthorizationTokenGenerationMgmtListRequestDTO tokenRequest = authorizationTokenGenerationMgmtListRequestCaptor.getValue();
		assertTrue(tokenRequest.list().size() == 1);
		assertEquals("TIME_LIMITED_TOKEN_AUTH", tokenRequest.list().get(0).tokenVariant());
		assertEquals(AuthorizationTargetType.SERVICE_DEF.name(), tokenRequest.list().get(0).targetType());
		assertEquals("LOCAL", tokenRequest.list().get(0).consumerCloud());
		assertEquals(requester, tokenRequest.list().get(0).consumer());
		assertEquals(candidate1.provider().name(), tokenRequest.list().get(0).provider());
		assertEquals(testSerfviceDef, tokenRequest.list().get(0).target());
		assertEquals("test-operation", tokenRequest.list().get(0).scope());
		assertNull(tokenRequest.list().get(0).expiresAt());
		assertNull(tokenRequest.list().get(0).usageLimit());

		final MultiValueMap<String, String> tokenRequestqueryParams = queryParamCaptor.getValue();
		assertTrue(tokenRequestqueryParams.containsKey(Constants.UNBOUND));
		assertEquals("true", tokenRequestqueryParams.get(Constants.UNBOUND).get(0));

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals("thisistherawtoken", result.results().get(0).authorizationTokens().get("TIME_LIMITED_TOKEN_AUTH").get("test-operation").token());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationInterfaceRequirementsTemplateName() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, List.of("generic_https"), null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", null);
		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "TIME_LIMITED_TOKEN_AUTH", null);
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", List.of(candidateInterface1, candidateInterface2));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals(candidate.provider().name(), result.results().get(0).providerName());
		assertTrue(result.results().get(0).interfaces().size() == 1);
		assertEquals("generic_https", result.results().get(0).interfaces().get(0).templateName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationInterfaceRequirementsTemplateNameWithTranslationAllowed() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, List.of("generic_https"), null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", null);
		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "TIME_LIMITED_TOKEN_AUTH", null);
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", List.of(candidateInterface1, candidateInterface2));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals(candidate.provider().name(), result.results().get(0).providerName());
		assertTrue(result.results().get(0).interfaces().size() == 1);
		assertEquals("generic_https", result.results().get(0).interfaces().get(0).templateName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationInterfaceRequirementsPropertyRequirements() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfacePropReq = new MetadataRequirementDTO();
		interfacePropReq.put("foo", "bar");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, null, List.of(interfacePropReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, null, null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", null);
		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "TIME_LIMITED_TOKEN_AUTH", Map.of("foo", "bar"));
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", List.of(candidateInterface1, candidateInterface2));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals(candidate.provider().name(), result.results().get(0).providerName());
		assertTrue(result.results().get(0).interfaces().size() == 1);
		assertEquals("generic_https", result.results().get(0).interfaces().get(0).templateName());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationInterfaceRequirementsAddressTypeWhenTranslationAllowed() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, null, List.of("IPV4"), null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", Map.of("accessAddresses", List.of("192.168.56.116")));
		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "TIME_LIMITED_TOKEN_AUTH", Map.of("accessAddresses", List.of("test.com")));
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", List.of(candidateInterface1, candidateInterface2));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);
		when(interfaceAddressPropertyProcessor.filterOnAddressTypes(stringObjectMapCaptor.capture(), stringListCaptor.capture())).thenReturn(true).thenReturn(false);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, times(2)).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		final List<Map<String, Object>> offeredInterfacesInputList = stringObjectMapCaptor.getAllValues();
		assertTrue(offeredInterfacesInputList.size() == 2);
		List<String> accessAddresses1 = (List<String>) offeredInterfacesInputList.get(0).get("accessAddresses");
		assertEquals("192.168.56.116", accessAddresses1.get(0));
		List<String> accessAddresses2 = (List<String>) offeredInterfacesInputList.get(1).get("accessAddresses");
		assertEquals("test.com", accessAddresses2.get(0));

		final List<List<String>> requiredAddressTypesInputList = stringListCaptor.getAllValues();
		assertTrue(requiredAddressTypesInputList.size() == 2);
		assertTrue(requiredAddressTypesInputList.get(0).size() == 1);
		assertEquals("IPV4", requiredAddressTypesInputList.get(0).get(0));
		assertTrue(requiredAddressTypesInputList.get(1).size() == 1);
		assertEquals("IPV4", requiredAddressTypesInputList.get(1).get(0));

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals(candidate.provider().name(), result.results().get(0).providerName());
		assertTrue(result.results().get(0).interfaces().size() == 1);
		assertEquals("generic_http", result.results().get(0).interfaces().get(0).templateName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationWhenTranslationAllowedAndCandidateHasOneNativeAndOneNonNativeInterface() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, null, null, null, null, List.of("generic_http"), null, null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", Map.of());
		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_mqtt", "mqtt", "TIME_LIMITED_TOKEN_AUTH", Map.of());
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", List.of(candidateInterface1, candidateInterface2));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), any(TranslationDiscoveryMgmtRequestDTO.class));
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_NEGOTIATION), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationNegotiationResponseDTO.class), any(TranslationNegotiationMgmtRequestDTO.class));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals(candidate.provider().name(), result.results().get(0).providerName());
		assertTrue(result.results().get(0).interfaces().size() == 1);
		assertEquals("generic_http", result.results().get(0).interfaces().get(0).templateName());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationInterfaceRequirementsNonMatchingTemplateNameAndMatchingAddressTypeWhenTranslationAllowedButNoTranslationDiscoveryResult() {
		final UUID jobId = UUID.randomUUID();
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, List.of("operatio-1"), null, null, null, List.of("something"), List.of("IPV4"), null, null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", Map.of("accessAddresses", List.of("192.168.56.116")));
		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "TIME_LIMITED_TOKEN_AUTH", Map.of("accessAddresses", List.of("test.com")));
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", new ArrayList<ServiceInstanceInterfaceResponseDTO>(List.of(candidateInterface1, candidateInterface2)));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), any(TranslationDiscoveryMgmtRequestDTO.class)))
				.thenReturn(new TranslationDiscoveryResponseDTO(null, List.of()));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo, times(2)).isBlacklistEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(sysInfo, times(2)).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor).filterOnAddressTypes(eq(candidateInterface1.properties()), eq(form.getInterfaceAddressTypes()));
		verify(interfaceAddressPropertyProcessor).filterOnAddressTypes(eq(candidateInterface2.properties()), eq(form.getInterfaceAddressTypes()));
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), any(TranslationDiscoveryMgmtRequestDTO.class));
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_NEGOTIATION), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationNegotiationResponseDTO.class), any(TranslationNegotiationMgmtRequestDTO.class));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationWithOnlyNonNativeCandidatesButTranslationIsNotAnOptionBecauseNoInputModelIdOffered() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfaceProosReq = new MetadataRequirementDTO();
		interfaceProosReq.put("dataModels.operation-1.input", "abc");
		interfaceProosReq.put("dataModels.operation-1.output", "def");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, List.of("operation-1"), null, null, null, List.of("something_else"), null, List.of(interfaceProosReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", Map.of("dataModels", Map.of("operation-1", Map.of("output", "def"))));
		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "TIME_LIMITED_TOKEN_AUTH", Map.of("dataModels", Map.of("operation-1", Map.of())));
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", new ArrayList<ServiceInstanceInterfaceResponseDTO>(List.of(candidateInterface1, candidateInterface2)));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(sysInfo).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(dataModelIDNormalizer).normalize(eq("abc"));
		verify(dataModelIDNormalizer).normalize(eq("def"));
		verify(dataModelIDValidator).validateDataModelIdentifier(eq("abc"));
		verify(dataModelIDValidator).validateDataModelIdentifier(eq("def"));
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), any(TranslationDiscoveryMgmtRequestDTO.class));
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_NEGOTIATION), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationNegotiationResponseDTO.class), any(TranslationNegotiationMgmtRequestDTO.class));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationWithOnlyNonNativeCandidatesButTranslationIsNotAnOptionBecauseNoOutputModelIdOffered() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfaceProosReq = new MetadataRequirementDTO();
		interfaceProosReq.put("dataModels.operation-1.input", "abc");
		interfaceProosReq.put("dataModels.operation-1.output", "def");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, List.of("operation-1"), null, null, null, List.of("something_else"), null, List.of(interfaceProosReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH", Map.of("dataModels", Map.of("operation-1", Map.of("input", "def"))));
		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "TIME_LIMITED_TOKEN_AUTH", Map.of("dataModels", Map.of("operation-1", Map.of())));
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", new ArrayList<ServiceInstanceInterfaceResponseDTO>(List.of(candidateInterface1, candidateInterface2)));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(sysInfo).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(dataModelIDNormalizer).normalize(eq("abc"));
		verify(dataModelIDNormalizer).normalize(eq("def"));
		verify(dataModelIDValidator).validateDataModelIdentifier(eq("abc"));
		verify(dataModelIDValidator).validateDataModelIdentifier(eq("def"));
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), any(TranslationDiscoveryMgmtRequestDTO.class));
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_NEGOTIATION), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationNegotiationResponseDTO.class), any(TranslationNegotiationMgmtRequestDTO.class));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationWithOnlyNonNativeCandidatesButTranslationIsNotAnOptionBecauseNoInputModelIdExpected() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfaceProosReq = new MetadataRequirementDTO();
		interfaceProosReq.put("dataModels.operation-1.output", "def");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, List.of("operation-1"), null, null, null, List.of("something_else"), null, List.of(interfaceProosReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH",
				Map.of("dataModels", Map.of("operation-1", Map.of("input", "abc", "output", "def"))));

		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "TIME_LIMITED_TOKEN_AUTH",
				Map.of("dataModels", Map.of("operation-1", Map.of("input", "abc"))));
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", new ArrayList<ServiceInstanceInterfaceResponseDTO>(List.of(candidateInterface1, candidateInterface2)));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(sysInfo).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(dataModelIDNormalizer).normalize(eq("def"));
		verify(dataModelIDValidator).validateDataModelIdentifier(eq("def"));
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), any(TranslationDiscoveryMgmtRequestDTO.class));
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_NEGOTIATION), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationNegotiationResponseDTO.class), any(TranslationNegotiationMgmtRequestDTO.class));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationWithOnlyNonNativeCandidatesButTranslationIsNotAnOptionBecauseNoOutputModelIdExpected() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfaceProosReq = new MetadataRequirementDTO();
		interfaceProosReq.put("dataModels.operation-1.input", "abc");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, List.of("operation-1"), null, null, null, List.of("something_else"), null, List.of(interfaceProosReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH",
				Map.of("dataModels", Map.of("operation-1", Map.of("input", "abc", "output", "def"))));
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", new ArrayList<ServiceInstanceInterfaceResponseDTO>(List.of(candidateInterface1)));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo, times(1)).isBlacklistEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(sysInfo, times(1)).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(dataModelIDNormalizer).normalize(eq("abc"));
		verify(dataModelIDValidator).validateDataModelIdentifier(eq("abc"));
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), any(TranslationDiscoveryMgmtRequestDTO.class));
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_NEGOTIATION), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationNegotiationResponseDTO.class), any(TranslationNegotiationMgmtRequestDTO.class));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoLocalServiceOrchestrationWithOnlyNonNativeCandidatesButNoTranslationDiscoveryResults() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfaceProosReq = new MetadataRequirementDTO();
		interfaceProosReq.put("dataModels.operation-1.input", "abc");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, List.of("operation-1"), null, null, null, List.of("something_else"), null, List.of(interfaceProosReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidateInterface1 = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH",
				Map.of("dataModels", Map.of("operation-1", Map.of("input", "abc", "output", "def"))));

		final ServiceInstanceInterfaceResponseDTO candidateInterface2 = new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "TIME_LIMITED_TOKEN_AUTH",
				Map.of("dataModels", Map.of("operation-1", Map.of("output", "def"))));
		final ServiceInstanceResponseDTO candidate = serviceInstanceResponseDTO("TestProvider1", new ArrayList<ServiceInstanceInterfaceResponseDTO>(List.of(candidateInterface1, candidateInterface2)));

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate), 1));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo).isBlacklistEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(sysInfo).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(dataModelIDNormalizer).normalize(eq("abc"));
		verify(dataModelIDValidator).validateDataModelIdentifier(eq("abc"));
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), any(TranslationDiscoveryMgmtRequestDTO.class));
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker, never()).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(ahHttpService, never()).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_NEGOTIATION), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationNegotiationResponseDTO.class), any(TranslationNegotiationMgmtRequestDTO.class));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertEquals("No results were found", stringCaptor.getValue());
		assertTrue(result.results().size() == 0);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testDoLocalServiceOrchestrationWithOnlyNonNativeCandidatesAndWithOneTranslationDiscoveryResult() {
		final UUID jobId = UUID.randomUUID();
		final MetadataRequirementDTO interfaceProosReq = new MetadataRequirementDTO();
		interfaceProosReq.put("dataModels.operation-1.input", "abc");
		interfaceProosReq.put("dataModels.operation-1.output", "def");
		final OrchestrationServiceRequirementDTO requirementDTO = new OrchestrationServiceRequirementDTO(testSerfviceDef, List.of("operation-1"), null, null, null, List.of("something_else"), null, List.of(interfaceProosReq), null, null);
		final OrchestrationRequestDTO requestDTO = new OrchestrationRequestDTO(requirementDTO, Map.of(OrchestrationFlag.ALLOW_TRANSLATION.name(), true), null, null);
		final String requester = "RequesterSystem";
		final OrchestrationForm form = new OrchestrationForm(requester, requestDTO);

		final ServiceInstanceInterfaceResponseDTO candidate1Interface = new ServiceInstanceInterfaceResponseDTO("generic_http", "http", "TIME_LIMITED_TOKEN_AUTH",
				Map.of("dataModels", Map.of("operation-1", Map.of("input", "abc", "output", "def"))));
		final ServiceInstanceResponseDTO candidate1 = serviceInstanceResponseDTO("TestProvider1", new ArrayList<ServiceInstanceInterfaceResponseDTO>(List.of(candidate1Interface)));
		final ServiceInstanceInterfaceResponseDTO candidate2Interface = new ServiceInstanceInterfaceResponseDTO("generic_mqtt", "mqtt", "TIME_LIMITED_TOKEN_AUTH",
				Map.of("dataModels", Map.of("operation-1", Map.of("input", "abc", "output", "def"))));
		final ServiceInstanceResponseDTO candidate2 = serviceInstanceResponseDTO("TestProvider2", new ArrayList<ServiceInstanceInterfaceResponseDTO>(List.of(candidate2Interface)));

		final String translationBridgeId = UUID.randomUUID().toString();

		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class), any(), any()))
				.thenReturn(new ServiceInstanceListResponseDTO(List.of(candidate1, candidate2), 2));
		when(orchLockDbService.getByServiceInstanceId(anyList())).thenReturn(List.of());
		when(sysInfo.isTranslationEnabled()).thenReturn(true);
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), any(TranslationDiscoveryMgmtRequestDTO.class)))
				.thenReturn(new TranslationDiscoveryResponseDTO(translationBridgeId, List.of(new TranslationBridgeCandidateDTO(candidate2.instanceId(), candidate2.interfaces().getFirst().templateName()))));
		final List<OrchestrationCandidate> matchmakingInput = new ArrayList<OrchestrationCandidate>();
		when(matchmaker.doMatchmaking(eq(form), anyList())).thenAnswer(invocation -> {
			matchmakingInput.addAll((List<OrchestrationCandidate>) invocation.getArgument(1));
			return matchmakingInput.getFirst();
		});
		when(ahHttpService.consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_NEGOTIATION), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationNegotiationResponseDTO.class), any(TranslationNegotiationMgmtRequestDTO.class)))
				.thenReturn(new TranslationNegotiationResponseDTO(translationBridgeId,
						new ServiceInstanceInterfaceResponseDTO(form.getInterfaceTemplateNames().getFirst(), null, ServiceInterfacePolicy.TRANSLATION_BRIDGE_TOKEN_AUTH.name(), null), null, null));

		final OrchestrationResponseDTO result = assertDoesNotThrow(() -> orchestration.doLocalServiceOrchestration(jobId, form));

		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_SERVICE_DISCOVERY), eq(Constants.SERVICE_OP_LOOKUP), eq(Constants.SYS_NAME_SERVICE_REGISTRY), eq(ServiceInstanceListResponseDTO.class),
				any(ServiceInstanceLookupRequestDTO.class), any());
		verify(orchLockDbService, times(2)).getByServiceInstanceId(anyList());
		verify(sysInfo, times(2)).isBlacklistEnabled();
		verify(sysInfo).isTranslationEnabled();
		verify(sysInfo, times(3)).isAuthorizationEnabled();
		verify(interfaceAddressPropertyProcessor, never()).filterOnAddressTypes(any(), anyList());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_DISCOVERY), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationDiscoveryResponseDTO.class), translationDiscoveryReqCaptor.capture());
		verify(orchLockDbService, never()).create(anyList());
		verify(matchmaker).doMatchmaking(eq(form), anyList());
		verify(orchLockDbService, never()).changeExpiresAtByOrchestrationJobIdAndServiceInstanceId(anyString(), anyString(), any(), anyBoolean());
		verify(orchLockDbService, never()).deleteInBatch(anyCollection());
		verify(ahHttpService).consumeService(eq(Constants.SERVICE_DEF_TRANSLATION_BRIDGE_MANAGEMENT), eq(Constants.SERVICE_OP_NEGOTIATION), eq(Constants.SYS_NAME_TRANSLATION_MANAGER),
				eq(TranslationNegotiationResponseDTO.class), translationNegotiationCaptor.capture());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.DONE), stringCaptor.capture());

		assertTrue(matchmakingInput.size() == 1);
		assertEquals(candidate2.instanceId(), matchmakingInput.get(0).getServiceInstance().instanceId());

		final TranslationDiscoveryMgmtRequestDTO translationDiscReq = translationDiscoveryReqCaptor.getValue();
		assertTrue(candidate1.instanceId().equals(translationDiscReq.candidates().get(0).instanceId()));
		assertTrue(candidate2.instanceId().equals(translationDiscReq.candidates().get(1).instanceId()));
		assertTrue(form.getTargetSystemName().equals(translationDiscReq.consumer()));
		assertTrue(form.getInterfaceTemplateNames().getFirst().equals(translationDiscReq.interfaceTemplateNames().getFirst()));
		assertTrue("abc".equals(translationDiscReq.inputDataModelId()));
		assertTrue("def".equals(translationDiscReq.outputDataModelId()));
		assertFalse(translationDiscReq.flags().get(TranslationDiscoveryFlag.CONSUMER_BLACKLIST_CHECK.toString()));
		assertFalse(translationDiscReq.flags().get(TranslationDiscoveryFlag.CANDIDATES_BLACKLIST_CHECK.toString()));
		assertFalse(translationDiscReq.flags().get(TranslationDiscoveryFlag.CANDIDATES_AUTH_CHECK.toString()));
		assertFalse(translationDiscReq.flags().get(TranslationDiscoveryFlag.TRANSLATORS_BLACKLIST_CHECK.toString()));
		assertFalse(translationDiscReq.flags().get(TranslationDiscoveryFlag.TRANSLATORS_AUTH_CHECK.toString()));

		final TranslationNegotiationMgmtRequestDTO translationNegReq = translationNegotiationCaptor.getValue();
		assertTrue(translationBridgeId.equals(translationNegReq.bridgeId()));
		assertTrue(matchmakingInput.getFirst().getServiceInstance().instanceId().equals(translationNegReq.targetInstanceId()));

		assertTrue(stringCaptor.getValue().startsWith("1 local result"), stringCaptor.getValue());
		assertTrue(result.results().size() == 1);
		assertEquals(candidate2.provider().name(), result.results().get(0).providerName());
		assertTrue(result.results().get(0).interfaces().size() == 1);
		assertEquals(form.getInterfaceTemplateNames().getFirst(), result.results().get(0).interfaces().get(0).templateName());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO serviceInstanceResponseDTO(final String sysName, final boolean withFakeInterface) {
		return new ServiceInstanceResponseDTO(
				sysName + "|" + testSerfviceDef + "|1.0.0",
				new SystemResponseDTO(sysName, null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO(testSerfviceDef, null, null),
				null,
				null,
				null,
				!withFakeInterface ? new ArrayList<>() : List.of(new ServiceInstanceInterfaceResponseDTO("whatever", "xyz", "NONE", null)),
				null,
				null);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO serviceInstanceResponseDTO(final String sysName, final Integer exclusivity, final boolean withFakeInterface) {
		return new ServiceInstanceResponseDTO(
				sysName + "|" + testSerfviceDef + "|1.0.0",
				new SystemResponseDTO(sysName, null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO(testSerfviceDef, null, null),
				null,
				null,
				exclusivity == null ? null : Map.of(Constants.METADATA_KEY_ALLOW_EXCLUSIVITY, exclusivity),
				!withFakeInterface ? new ArrayList<>() : List.of(new ServiceInstanceInterfaceResponseDTO("whatever", "xyz", "NONE", null)),
				null,
				null);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInstanceResponseDTO serviceInstanceResponseDTO(final String sysName, final List<ServiceInstanceInterfaceResponseDTO> interfaces) {
		return new ServiceInstanceResponseDTO(
				sysName + "|" + testSerfviceDef + "|1.0.0",
				new SystemResponseDTO(sysName, null, null, null, null, null, null),
				new ServiceDefinitionResponseDTO(testSerfviceDef, null, null),
				null,
				null,
				null,
				interfaces,
				null,
				null);
	}
}
