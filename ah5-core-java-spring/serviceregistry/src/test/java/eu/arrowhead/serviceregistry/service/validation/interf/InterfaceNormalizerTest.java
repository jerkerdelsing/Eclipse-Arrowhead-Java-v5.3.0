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
package eu.arrowhead.serviceregistry.service.validation.interf;


import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InterfaceNormalizerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private InterfaceNormalizer intfNormalizer;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;


	//=================================================================================================
	// methods

	// INTERFACE INSTANCES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeInterfaceOk() {

		final ServiceInstanceInterfaceRequestDTO normalized = intfNormalizer.normalizeInterfaceDTO(
				new ServiceInstanceInterfaceRequestDTO(
						"GENERIC HTTPS",
						" HTTPS ",
						"rsa_sha512_json_web_token_auth\n \t",
						Map.of("examplePropertyKey1", true, "examplePropertyKey2", "value")));

		verify(interfaceTemplateNameNormalizer, times(1)).normalize("GENERIC HTTPS");
		assertAll("normalize InterfaceDTO",
				() -> assertEquals("https", normalized.protocol()),
				() -> assertEquals("RSA_SHA512_JSON_WEB_TOKEN_AUTH", normalized.policy()),
				() -> assertEquals(Map.of("examplePropertyKey1", true, "examplePropertyKey2", "value"), normalized.properties()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeInterfaceEmptyProtocol() {

		final ServiceInstanceInterfaceRequestDTO normalized = intfNormalizer.normalizeInterfaceDTO(
				new ServiceInstanceInterfaceRequestDTO(null, null, "NONE", null));

		assertEquals("", normalized.protocol());
	}

	// INTERFACE TEMPLATES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeTemplateNullPropRequirements() {

		final ServiceInterfaceTemplateRequestDTO normalized1 = intfNormalizer.normalizeTemplateDTO(
				new ServiceInterfaceTemplateRequestDTO(
						"generic MQTT",
						"\t MQTT",
						null));

		assertEquals(new ArrayList<>(), normalized1.propertyRequirements());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeTemplateOk() {

		// properties to normalize
		final ServiceInterfaceTemplatePropertyDTO propsToNormalize = new ServiceInterfaceTemplatePropertyDTO(
				" operations \n",
				true,
				"not_empty_string_set \t",
				List.of("\noperation "));

		final ServiceInterfaceTemplatePropertyDTO propsWithNullToNormalize = new ServiceInterfaceTemplatePropertyDTO("accessAddresses", false, null, null);

		// list of the properties
		final List<ServiceInterfaceTemplatePropertyDTO> requirements = List.of(propsToNormalize, propsWithNullToNormalize);

		// normalize dto
		final ServiceInterfaceTemplateRequestDTO normalized = intfNormalizer.normalizeTemplateDTO(
				new ServiceInterfaceTemplateRequestDTO(
						"generic mqtt",
						"\t MQTT",
						requirements));

		// expected properties after the normalization
		final ServiceInterfaceTemplatePropertyDTO propsExpected = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				"NOT_EMPTY_STRING_SET",
				List.of("operation"));

		final ServiceInterfaceTemplatePropertyDTO propsWithNullExpected = new ServiceInterfaceTemplatePropertyDTO("accessAddresses", false, "", new ArrayList<>());

		assertEquals(List.of(propsExpected, propsWithNullExpected), normalized.propertyRequirements());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeTemplateNullPropRequirementName() {

		// requirement with null name
		final List<ServiceInterfaceTemplatePropertyDTO> requirements = List.of(new ServiceInterfaceTemplatePropertyDTO(null, false, null, null));

		assertThrows(IllegalArgumentException.class, () -> {
			intfNormalizer.normalizeTemplateDTO(new ServiceInterfaceTemplateRequestDTO(
													"generic mqtt",
													"\t MQTT",
													requirements));
			});

	}

}
