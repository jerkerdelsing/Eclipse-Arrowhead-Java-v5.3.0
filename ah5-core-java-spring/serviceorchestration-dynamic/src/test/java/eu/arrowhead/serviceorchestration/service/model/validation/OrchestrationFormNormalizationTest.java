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
package eu.arrowhead.serviceorchestration.service.model.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.QoSRequirementDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@ExtendWith(MockitoExtension.class)
public class OrchestrationFormNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationFormNormalization normalizator;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private ServiceOperationNameNormalizer serviceOpNameNormalizer;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Mock
	private VersionNormalizer versionNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationFormBasic() {
		final String requester = " RequesterSystem ";
		final String serviceDef = " testService ";
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO(serviceDef, null, null, null, null, null, null, null, null, null);
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, null, null, null);
		final OrchestrationForm orchestrationForm = new OrchestrationForm(requester, orchestrationRequestDTO);

		when(systemNameNormalizer.normalize(eq(requester))).thenReturn(requester.trim());
		when(serviceDefNameNormalizer.normalize(eq(serviceDef))).thenReturn(serviceDef.trim());

		assertDoesNotThrow(() -> normalizator.normalizeOrchestrationForm(orchestrationForm));

		assertEquals(requester.trim(), orchestrationForm.getRequesterSystemName());
		assertEquals(serviceDef.trim(), orchestrationForm.getServiceDefinition());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOrchestrationFormComplex() {
		final String requester = " RequesterSystem ";
		final String target = " TargetSystem ";
		final String serviceDef = " testService ";
		final String serviceOp = " testOp ";
		final String version = " 1.0.0 ";
		final String interfaceName = " generic_http ";
		final String addressType = " ipv4 ";
		final String securityPolicy = " cert_auth ";
		final String preferredProvider = " PreferredProvider ";
		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("foo", "bar");
		final MetadataRequirementDTO interfacePropsReq = new MetadataRequirementDTO();
		interfacePropsReq.put("method", "put");
		final OrchestrationServiceRequirementDTO serviceRequirementDTO = new OrchestrationServiceRequirementDTO(serviceDef, List.of(serviceOp), List.of(version),
				Utilities.convertZonedDateTimeToUTCString(Utilities.utcNow().plusHours(1)), List.of(metadataReq), List.of(interfaceName), List.of(addressType), List.of(interfacePropsReq),
				List.of(securityPolicy), List.of(preferredProvider));
		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(serviceRequirementDTO, Map.of(" matchmaking ", true), List.of(new QoSRequirementDTO(" test-qos ", " filter ", Map.of("something", "xyz"))), 500);
		final OrchestrationForm orchestrationForm = new OrchestrationForm(requester, target, orchestrationRequestDTO);

		when(systemNameNormalizer.normalize(eq(requester))).thenReturn(requester.trim());
		when(systemNameNormalizer.normalize(eq(target))).thenReturn(target.trim());
		when(serviceDefNameNormalizer.normalize(eq(serviceDef))).thenReturn(serviceDef.trim());
		when(serviceOpNameNormalizer.normalize(eq(serviceOp))).thenReturn(serviceOp.trim());
		when(interfaceTemplateNameNormalizer.normalize(eq(interfaceName))).thenReturn(interfaceName.trim());
		when(systemNameNormalizer.normalize(eq(preferredProvider))).thenReturn(preferredProvider.trim());
		when(versionNormalizer.normalize(eq(version))).thenReturn(version.trim());

		assertDoesNotThrow(() -> normalizator.normalizeOrchestrationForm(orchestrationForm));

		assertEquals(requester.trim(), orchestrationForm.getRequesterSystemName());
		assertEquals(target.trim(), orchestrationForm.getTargetSystemName());
		assertEquals(serviceDef.trim(), orchestrationForm.getServiceDefinition());
		assertEquals(serviceOp.trim(), orchestrationForm.getOperations().getFirst());
		assertEquals(version.trim(), orchestrationForm.getVersions().getFirst());
		assertEquals(interfaceName.trim(), orchestrationForm.getInterfaceTemplateNames().getFirst());
		assertEquals(addressType.trim().toUpperCase(), orchestrationForm.getInterfaceAddressTypes().getFirst());
		assertEquals(securityPolicy.trim().toUpperCase(), orchestrationForm.getSecurityPolicies().getFirst());
		assertEquals(preferredProvider.trim(), orchestrationForm.getPreferredProviders().getFirst());
		orchestrationForm.getOrchestrationFlags().keySet().forEach((flag) -> {
			assertEquals(OrchestrationFlag.MATCHMAKING.name(), flag);
		});
		assertEquals("test-qos", orchestrationForm.getQualityRequirements().getFirst().type());
		assertEquals("FILTER", orchestrationForm.getQualityRequirements().getFirst().operation());
	}
}
