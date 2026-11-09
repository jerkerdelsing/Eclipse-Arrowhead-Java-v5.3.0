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
package eu.arrowhead.serviceorchestration.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

@ExtendWith(MockitoExtension.class)
public class OrchestrationFormContextValidationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationFromContextValidation validator;

	@Mock
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOnlyIntercloudButNotEnabled() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder().orchestrationFlag(OrchestrationFlag.ONLY_INTERCLOUD.name(), true).build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);
		when(sysInfo.isIntercloudEnabled()).thenReturn(false);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		verify(sysInfo).isIntercloudEnabled();

		assertEquals("ONLY_INTERCLOUD flag is present, but intercloud orchestration is not enabled", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOnlyIntercloudAndAllowTranslation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ONLY_INTERCLOUD.name(), true)
				.orchestrationFlag(OrchestrationFlag.ALLOW_TRANSLATION.name(), true)
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		verify(sysInfo).isIntercloudEnabled();

		assertEquals("ONLY_INTERCLOUD and ALLOW_TRANSLATION flags cannot be present at the same time", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOnlyIntercloudButNoOperation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder().orchestrationFlag(OrchestrationFlag.ONLY_INTERCLOUD.name(), true).build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		verify(sysInfo).isIntercloudEnabled();

		assertEquals("Exactly one operation must be defined when only inter-cloud orchestration is required", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOnlyIntercloudButMoreThanOneOperation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ONLY_INTERCLOUD.name(), true)
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder().operations(List.of("op1", "op2")).build())
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		verify(sysInfo).isIntercloudEnabled();

		assertEquals("Exactly one operation must be defined when only inter-cloud orchestration is required", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOnlyIntercloudAndExactlyOneOperation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ONLY_INTERCLOUD.name(), true)
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder().operations(List.of("op1")).build())
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);
		when(sysInfo.isIntercloudEnabled()).thenReturn(true);

		assertDoesNotThrow(() -> validator.validate(orchestrationForm, "test.origin"));

		verify(sysInfo).isIntercloudEnabled();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAllowIntercloudButNoOperation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder().orchestrationFlag(OrchestrationFlag.ALLOW_INTERCLOUD.name(), true).build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		assertEquals("Exactly one operation must be defined when only inter-cloud orchestration is allowed", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAllowIntercloudButMoreThanOneOperation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ALLOW_INTERCLOUD.name(), true)
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder().operations(List.of("op1", "op2")).build())
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		assertEquals("Exactly one operation must be defined when only inter-cloud orchestration is allowed", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAllowIntercloudAndExactlyOneOperation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ALLOW_INTERCLOUD.name(), true)
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder().operations(List.of("op1")).build())
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);

		assertDoesNotThrow(() -> validator.validate(orchestrationForm, "test.origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAllowTranslationButNoInterfaceTemplateName() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ALLOW_TRANSLATION.name(), true)
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder().operations(List.of("op1")).build())
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		assertEquals("Interface template name(s) must be defined when translation is allowed", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAllowTranslationWithInterfaceNameButNoOperation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder().orchestrationFlag(OrchestrationFlag.ALLOW_TRANSLATION.name(), true)
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder().interfaceTemplateName("interface1").build())
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		assertEquals("Exactly one operation must be defined when translation is allowed", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAllowTranslationWithInterfaceNameButMoreThanOneOperation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ALLOW_TRANSLATION.name(), true)
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder().interfaceTemplateName("interface1").operations(List.of("op1", "op2")).build())
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		assertEquals("Exactly one operation must be defined when translation is allowed", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAllowTranslationWithInterfaceNameAndExactlyOneOperation() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ALLOW_TRANSLATION.name(), true)
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder().interfaceTemplateName("interface1").operations(List.of("op1")).build())
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);

		assertDoesNotThrow(() -> validator.validate(orchestrationForm, "test.origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOnlyPreferredButNoPreferredProviders() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ONLY_PREFERRED.name(), true)
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);

		final Throwable ex = assertThrows(InvalidParameterException.class, () -> validator.validate(orchestrationForm, "test.origin"));

		assertEquals("ONLY_PREFERRED flag is present, but no preferred provider is defined", ex.getMessage());
		assertEquals("test.origin", ((InvalidParameterException) ex).getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOnlyPreferredAndHasPreferredProviders() {
		final OrchestrationRequestDTO orchestrationRequest = new OrchestrationRequestDTO.Builder()
				.orchestrationFlag(OrchestrationFlag.ONLY_PREFERRED.name(), true)
				.serviceRequirement(new OrchestrationServiceRequirementDTO.Builder().preferredProvider("pp").build())
				.build();
		final OrchestrationForm orchestrationForm = new OrchestrationForm("TestConsumer", orchestrationRequest);

		assertDoesNotThrow(() -> validator.validate(orchestrationForm, "test.origin"));
	}
}
