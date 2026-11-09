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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.common.intf.properties.validators.MinMaxValidator;
import eu.arrowhead.common.intf.properties.validators.NotEmptyAddressListValidator;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor.AddressData;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;

@ExtendWith(MockitoExtension.class)
public class InterfaceValidatorTest {

	//=================================================================================================
	// members

	@InjectMocks
	private InterfaceValidator intfValidator;

	@Mock
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Mock
	private AddressNormalizer addressNormalizer;

	@Mock
	private AddressValidator addressValidator;

    @Mock
    private ServiceInterfaceTemplateDbService serviceInterfaceTemplateDbService;

    @Mock
    private PropertyValidators propertyValidators;

    @Mock
    private ServiceInterfaceAddressPropertyProcessor interfaceAddressPropertyProcessor;

    // property validation related
    @Mock
    private MinMaxValidator minMaxValidator;

    @Mock
    private NotEmptyAddressListValidator notEmptyAddressListValidator;

    // expected error messages
    private static final String EMPTY_TEMPLATE_LIST = "Interface template list is empty";
    private static final String TOO_LONG_PROTOCOL_NAME = "Interface protocol is too long";
    private static final String TOO_LONG_INTF_PROPERTY_NAME = "Interface property name is too long";
    private static final String UNKNOWN_VALIDATOR_PREFIX = "Unknown property validator: ";
    private static final String EMPTY_INSTANCE_LIST = "Interface instance list is empty";
    private static final String MISSING_PROTOCOL = "Interface protocol is missing";
    private static final String INVALID_PROTOCOL = " protocol is invalid for ";
    private static final String MISSING_PROPERTY = " interface property is missing for ";


	//=================================================================================================
	// methods

    // INTERFACE TEMPLATES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceTemplatesOk() {

		// properties
		final ServiceInterfaceTemplatePropertyDTO propAddress = new ServiceInterfaceTemplatePropertyDTO(
				"accessAddresses",
				true,
				"NOT_EMPTY_ADDRESS_LIST", // validator not null
				List.of());
		final ServiceInterfaceTemplatePropertyDTO propOperations = new ServiceInterfaceTemplatePropertyDTO(
				"operations",
				true,
				null, // validator null
				List.of());

		// templates
		final ServiceInterfaceTemplateRequestDTO httpTemplate = new ServiceInterfaceTemplateRequestDTO("generic_http", "http", List.of(propAddress, propOperations));
		final ServiceInterfaceTemplateRequestDTO mqttTemplate = new ServiceInterfaceTemplateRequestDTO("generic_mqtt", "mqtt", List.of(propAddress));

		assertDoesNotThrow(() -> {
			intfValidator.validateNormalizedInterfaceTemplates(List.of(httpTemplate, mqttTemplate));
		});
		verify(interfaceTemplateNameValidator, times(2)).validateInterfaceTemplateName(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceTemplatesEmptyTemplateList() {

		final Exception ex = assertThrows(IllegalArgumentException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(List.of()));

		assertEquals(EMPTY_TEMPLATE_LIST, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceTemplatesIsTemplateNameValidated() {

		intfValidator.validateNormalizedInterfaceTemplates(List.of(new ServiceInterfaceTemplateRequestDTO("generic_mqtt", "http", List.of())));

		verify(interfaceTemplateNameValidator, times(1)).validateInterfaceTemplateName(eq("generic_mqtt"));

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceTemplatesTooLongProtocolName() {

		final Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
				List.of(new ServiceInterfaceTemplateRequestDTO(
						"generic_mqtt",
						"very very very very very very very very very very very long mqtt",
						List.of()))));

		assertEquals(TOO_LONG_PROTOCOL_NAME, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceTemplatesTooLongIntfPropName() {

		final ServiceInterfaceTemplatePropertyDTO propWithLongName = new ServiceInterfaceTemplatePropertyDTO(
				"very very very very very very very very very very long address list",
				false,
				"NOT_EMPTY_ADDRESS_LIST",
				List.of());

		final Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
				List.of(new ServiceInterfaceTemplateRequestDTO(
						"generic_mqtt",
						"mqtt",
						List.of(propWithLongName)))));

		assertEquals(TOO_LONG_INTF_PROPERTY_NAME, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceTemplatesUnknownValidator() {

		final String unknownValidator = "NOT_EMPTY_MQTT_ADDRESS_LIST";

		final ServiceInterfaceTemplatePropertyDTO propWithUnknownValidator = new ServiceInterfaceTemplatePropertyDTO(
				"mqtt addresses",
				false,
				unknownValidator,
				List.of());

		final Exception ex = assertThrows(InvalidParameterException.class, () -> intfValidator.validateNormalizedInterfaceTemplates(
				List.of(new ServiceInterfaceTemplateRequestDTO(
						"generic_mqtt",
						"mqtt",
						List.of(propWithUnknownValidator)))));

		assertEquals(UNKNOWN_VALIDATOR_PREFIX + unknownValidator, ex.getMessage());
	}

	// INTERFACE INSTANCES

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesEmptyInstanceList() {

		final Exception ex = assertThrows(
				IllegalArgumentException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(List.of()));

		assertEquals(EMPTY_INSTANCE_LIST, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesMissingIntfProtocol() {

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());


		final Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", null, "BASE64_SELF_CONTAINED_TOKEN_AUTH", Map.of()))));

		assertEquals(MISSING_PROTOCOL, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesTooLongIntfProtocol() {

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());


		final Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "toooooooooooooooooooooooooooooooooooooooooooo long protocol name", "NONE", Map.of()))));

		assertEquals(TOO_LONG_PROTOCOL_NAME, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesTooLongPropertyName() {

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());


		final Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("tooooooooooooooooooooooooooooooooooooooooooooo long property key", "value")))));

		assertEquals(TOO_LONG_INTF_PROPERTY_NAME, ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesDiscoverAddress() {

		final String address = "192.168.0.3\n";
		final String addressKey = "address";

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());
		// mock validation and normalization
		when(addressNormalizer.normalize("192.168.0.3\n")).thenReturn("192.168.0.3");
		when(addressValidator.detectType("192.168.0.3")).thenReturn(AddressType.IPV4);

		// create properties to test
		final Map<String, Object> properties = new HashMap<String, Object>(2);
		properties.put(
				"operations",
				Map.of("query-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/query", HttpOperationModel.PROP_NAME_METHOD, "GET"),
						"set-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/set", HttpOperationModel.PROP_NAME_METHOD, "PUT")));
		properties.put(addressKey, address);

		// mock address preprocessor
		when(interfaceAddressPropertyProcessor.findAddresses(properties)).thenReturn(new AddressData(List.of(address), addressKey, false));

		final ServiceInstanceInterfaceRequestDTO normalizedInstance = intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties))).get(0);

		// check if the address was validated and normalized
		verify(addressNormalizer, times(1)).normalize(address);
		verify(addressValidator, times(1)).validateNormalizedAddress(any(AddressType.class), anyString());

		assertTrue(normalizedInstance.properties().containsKey(addressKey));
		assertEquals(String.class, normalizedInstance.properties().get(addressKey).getClass());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesDiscoverAddressList() {

		// addresses to normalize
		final String address1 = "192.168.0.3\n";
		final String address2 = "GREENHOUSE.EU";
		// normalized addresses
		final String normalizedAddress1 = "192.168.0.3";
		final String normalizedAddress2 = "greenhouse.eu";
		// address properties
		final String addressListKey = "addresses";
		final List<String> addressListToNormalize = List.of(address1, address2);

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());
		// mock validation and normalization
		when(addressNormalizer.normalize(address1)).thenReturn(normalizedAddress1);
		when(addressNormalizer.normalize(address2)).thenReturn(normalizedAddress2);
		when(addressValidator.detectType(normalizedAddress1)).thenReturn(AddressType.IPV4);
		when(addressValidator.detectType(normalizedAddress2)).thenReturn(AddressType.HOSTNAME);

		// create property set to test
		final Map<String, Object> properties = new HashMap<String, Object>(2);
		properties.put(
				"operations",
				Map.of("query-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/query", HttpOperationModel.PROP_NAME_METHOD, "GET"),
						"set-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/set", HttpOperationModel.PROP_NAME_METHOD, "PUT")));
		properties.put(addressListKey, addressListToNormalize);

		// mock address preprocessor
		when(interfaceAddressPropertyProcessor.findAddresses(properties)).thenReturn(new AddressData(addressListToNormalize, addressListKey, true));

		final ServiceInstanceInterfaceRequestDTO normalizedInstance = intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties))).get(0);

		// check if the addresses were validated and normalized
		verify(addressValidator, times(2)).validateNormalizedAddress(any(AddressType.class), anyString());
		verify(addressNormalizer, times(2)).normalize(anyString());

		assertTrue(normalizedInstance.properties().containsKey(addressListKey));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesDiscoverEmptyAddress() {

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());

		// create property set to test
		final Map<String, Object> properties = new HashMap<String, Object>(1);
		properties.put(
				"operations",
				Map.of("query-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/query", HttpOperationModel.PROP_NAME_METHOD, "GET"),
						"set-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/set", HttpOperationModel.PROP_NAME_METHOD, "PUT")));

		// mock address preprocessor
		when(interfaceAddressPropertyProcessor.findAddresses(properties)).thenReturn((new AddressData(new ArrayList<>(), null, false)));

		final ServiceInstanceInterfaceRequestDTO normalizedInstance = intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties))).get(0);

		// check if no addresses were validated and normalized
		verify(addressValidator, never()).validateNormalizedAddress(any(AddressType.class), anyString());
		verify(addressNormalizer, never()).normalize(anyString());
		assertEquals(1, normalizedInstance.properties().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesAddressesNotDiscovered() {

		// existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.of(new ServiceInterfaceTemplate("generic_http", "http")));

		// create property set to test
		final Map<String, Object> properties = new HashMap<String, Object>(1);
		properties.put("accessAddresses", List.of("provider1.temperature.com", "192.168.0.10"));
		// mock required properties
		final List<ServiceInterfaceTemplateProperty> dummyPropRequirements = new ArrayList<>(1);
		dummyPropRequirements.add(new ServiceInterfaceTemplateProperty(new ServiceInterfaceTemplate("generic_http", "http"), "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST"));
		when(serviceInterfaceTemplateDbService.getPropertiesByTemplateName(anyString())).thenReturn(dummyPropRequirements);
		// in case of NotEmptyAddressListValidator, address discovery should not happen
		when(propertyValidators.getValidator(any())).thenReturn(notEmptyAddressListValidator);
		when(notEmptyAddressListValidator.validateAndNormalize(any())).thenReturn(List.of());

		intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties))).get(0);

		// address validation should happen in the property validator instead of the preprocessor
		verify(notEmptyAddressListValidator, times(1)).validateAndNormalize(any());
		verify(interfaceAddressPropertyProcessor, never()).findAddresses(any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesInvalidProtocolForTemplate() {

		// existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.of(new ServiceInterfaceTemplate("generic_http", "http")));

		final Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "https", "NONE", Map.of()))));

		assertEquals("https" + INVALID_PROTOCOL + "generic_http", ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesMissingProtocol() {

		// existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.of(new ServiceInterfaceTemplate("generic_http", "http")));

		assertDoesNotThrow(
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", null, "NONE", Map.of()))));

	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateNormalizedInterfaceInstancesMandatoryPropertyMissing() {

		// existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.of(new ServiceInterfaceTemplate("generic_http", "http")));
		// mock required properties
		final List<ServiceInterfaceTemplateProperty> dummyPropRequirements = new ArrayList<>(2);
		dummyPropRequirements.add(new ServiceInterfaceTemplateProperty(null, "accessPort", true, null));
		dummyPropRequirements.add(new ServiceInterfaceTemplateProperty(new ServiceInterfaceTemplate("generic_http", "http"), "accessAddresses", true, null));
		when(serviceInterfaceTemplateDbService.getPropertiesByTemplateName(anyString())).thenReturn(dummyPropRequirements);

		final Exception ex = assertThrows(
				InvalidParameterException.class,
				() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080)))));

		assertEquals("accessAddresses" + MISSING_PROPERTY + "generic_http", ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateNormalizedInterfaceInstancesNotMandatoryPropertyMissing() {

		// existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.of(new ServiceInterfaceTemplate("generic_http", "http")));
		// mock required properties
		final List<ServiceInterfaceTemplateProperty> dummyPropRequirements = new ArrayList<>(2);
		dummyPropRequirements.add(new ServiceInterfaceTemplateProperty(null, "accessPort", true, null));
		dummyPropRequirements.add(new ServiceInterfaceTemplateProperty(new ServiceInterfaceTemplate("generic_http", "http"), "accessAddresses", false, null));
		when(serviceInterfaceTemplateDbService.getPropertiesByTemplateName(anyString())).thenReturn(dummyPropRequirements);

		assertDoesNotThrow(() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(
						List.of(new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080)))));


	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesValidatorNull() {

		// existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.of(new ServiceInterfaceTemplate("generic_http", "http")));
		// mock mandatory property
		final ServiceInterfaceTemplateProperty propRequirement = new ServiceInterfaceTemplateProperty(new ServiceInterfaceTemplate("generic_http", "http"), "port", true, "PORT");
		when(serviceInterfaceTemplateDbService.getPropertiesByTemplateName(anyString())).thenReturn(List.of(propRequirement));
		// mock null validator
		when(propertyValidators.getValidator(any())).thenReturn(null);
		// mock address preprocessor
		when(interfaceAddressPropertyProcessor.findAddresses(any())).thenReturn(new AddressData(new ArrayList<>(), null, false));

		// create property set to test
		final Map<String, Object> properties = Map.of("port", 600);

		final ServiceInstanceInterfaceRequestDTO dto = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties);

		final List<ServiceInstanceInterfaceRequestDTO> normalized = assertDoesNotThrow(() -> intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(List.of(dto)));
		assertEquals(List.of(dto), normalized);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:magicnumber")
	@Test
	public void testValidateNormalizedInterfaceInstancesNoExistingTemplateOk() {

		// no existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.empty());
		// mock address preprocessor
		when(interfaceAddressPropertyProcessor.findAddresses(any())).thenReturn(new AddressData(new ArrayList<>(), null, false));

		// create property set to test
		final Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(
				"operations",
				Map.of("query-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/query", HttpOperationModel.PROP_NAME_METHOD, "GET"),
						"set-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/set", HttpOperationModel.PROP_NAME_METHOD, "PUT")));
		properties.put("accessPort", 1444);

		final ServiceInstanceInterfaceRequestDTO dto = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties);

		final List<ServiceInstanceInterfaceRequestDTO> normalized = intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(List.of(dto));

		verify(interfaceAddressPropertyProcessor, times(1)).findAddresses(any());
		verify(interfaceTemplateNameValidator, times(1)).validateInterfaceTemplateName(any());

		assertEquals(1, normalized.size());
		assertEquals(2, normalized.get(0).properties().size());
		assertEquals(List.of(dto), normalized);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedInterfaceInstancesExistingTemplateOk() {

		// existing template in the DB
		when(serviceInterfaceTemplateDbService.getByName(anyString())).thenReturn(Optional.of(new ServiceInterfaceTemplate("generic_http", "http")));
		// mock mandatory property
		final ServiceInterfaceTemplateProperty propRequirement = new ServiceInterfaceTemplateProperty(new ServiceInterfaceTemplate("generic_http", "http"), "accessPort", true, "MINMAX|1|65535");
		when(serviceInterfaceTemplateDbService.getPropertiesByTemplateName(anyString())).thenReturn(List.of(propRequirement));
		// mock port validator
		final int testPort = 4040;
		when(propertyValidators.getValidator(any())).thenReturn(minMaxValidator);
		when(minMaxValidator.validateAndNormalize(any(), any(), any())).thenReturn(testPort);
		// mock address preprocessor
		when(interfaceAddressPropertyProcessor.findAddresses(any())).thenReturn(new AddressData(new ArrayList<>(), null, false));

		// create property set to test
		final Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(
				"operations",
				Map.of("query-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/query", HttpOperationModel.PROP_NAME_METHOD, "GET"),
						"set-temperature", Map.of(HttpOperationModel.PROP_NAME_PATH, "/set", HttpOperationModel.PROP_NAME_METHOD, "PUT")));
		properties.put("accessPort", testPort);

		final ServiceInstanceInterfaceRequestDTO dto = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", properties);

		final List<ServiceInstanceInterfaceRequestDTO> normalized = intfValidator.validateNormalizedInterfaceInstancesWithPropsNormalization(List.of(dto));

		verify(interfaceAddressPropertyProcessor, times(1)).findAddresses(any());
		verify(minMaxValidator, times(1)).validateAndNormalize(testPort, "1", "65535");
		verify(interfaceTemplateNameValidator, times(1)).validateInterfaceTemplateName(any());

		assertEquals(1, normalized.size());
		assertEquals(2, normalized.get(0).properties().size());
		assertEquals(List.of(dto), normalized);

	}

}
