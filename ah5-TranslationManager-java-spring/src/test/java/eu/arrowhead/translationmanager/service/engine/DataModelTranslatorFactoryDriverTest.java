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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.intf.properties.IPropertyValidator;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.dto.DataModelFactoryTranslatorInitiaizationResponseDTO;
import eu.arrowhead.dto.DataModelTranslatorFactoryRequestDTO;
import eu.arrowhead.dto.TranslationDataModelTranslatorInitializationResponseDTO;

@ExtendWith(MockitoExtension.class)
public class DataModelTranslatorFactoryDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DataModelTranslatorFactoryDriver driver;

	@Mock
	private SystemInfo sysInfo;

	@Mock
	private HttpService httpService;

	@Mock
	private PropertyValidators validators;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationNameNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.isFactorySupportsTranslation(null, null, null, null));

		assertEquals("Factory name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationNameEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.isFactorySupportsTranslation("", null, null, null));

		assertEquals("Factory name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationPropertiesNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.isFactorySupportsTranslation("Factory", null, null, null));

		assertEquals("Factory interface properties is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationPropertiesEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.isFactorySupportsTranslation("Factory", Map.of(), null, null));

		assertEquals("Factory interface properties is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationFromNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.isFactorySupportsTranslation("Factory", Map.of("a", "b"), null, null));

		assertEquals("From data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationFromEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.isFactorySupportsTranslation("Factory", Map.of("a", "b"), "", null));

		assertEquals("From data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationToNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.isFactorySupportsTranslation("Factory", Map.of("a", "b"), "from", null));

		assertEquals("To data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationToEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.isFactorySupportsTranslation("Factory", Map.of("a", "b"), "from", ""));

		assertEquals("To data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationExceptionInWebService() {
		final Map<String, Object> properties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test/check");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("from", "to");

		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(httpService.sendRequest(uri, HttpMethod.GET, Boolean.class, payload)).thenThrow(new ExternalServerError("test"));

		final boolean result = driver.isFactorySupportsTranslation("Factory", properties, "from", "to");

		assertFalse(result);

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.GET, Boolean.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationNullResponse() {
		final Map<String, Object> properties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test",
				"operations", "notAMap");

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/check");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("from", "to");

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.GET, Boolean.class, payload)).thenReturn(null);

		final boolean result = driver.isFactorySupportsTranslation("Factory", properties, "from", "to");

		assertFalse(result);

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.GET, Boolean.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationFalseResponse() {
		final Map<String, Object> properties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test",
				"operations", Map.of());

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/check");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("from", "to");

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.GET, Boolean.class, payload)).thenReturn(false);

		final boolean result = driver.isFactorySupportsTranslation("Factory", properties, "from", "to");

		assertFalse(result);

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.GET, Boolean.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationTrueResponse() {
		final Map<String, Object> properties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test",
				"operations", Map.of("check", "notAModel"));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/check");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("from", "to");

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.GET, Boolean.class, payload)).thenReturn(true);

		final boolean result = driver.isFactorySupportsTranslation("Factory", properties, "from", "to");

		assertTrue(result);

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.GET, Boolean.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsFactorySupportsTranslationTrueResponse2() {
		final Map<String, Object> properties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test",
				"operations", Map.of("check", Map.of("method", "POST", "path", "/analyze")));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/analyze");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("from", "to");

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, Boolean.class, payload)).thenReturn(true);

		final boolean result = driver.isFactorySupportsTranslation("Factory", properties, "from", "to");

		assertTrue(result);

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, Boolean.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorNameNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator(null, null, null, null));

		assertEquals("Factory name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorNameEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator("", null, null, null));

		assertEquals("Factory name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorPropertiesNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator("Factory", null, null, null));

		assertEquals("Factory interface properties is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorPropertiesEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator("Factory", Map.of(), null, null));

		assertEquals("Factory interface properties is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorFromNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator("Factory", Map.of("a", "b"), null, null));

		assertEquals("From data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorFromEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator("Factory", Map.of("a", "b"), "", null));

		assertEquals("From data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorToNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator("Factory", Map.of("a", "b"), "testXml", null));

		assertEquals("To data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorToEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> driver.initializeDataModelTranslator("Factory", Map.of("a", "b"), "testXml", ""));

		assertEquals("To data model identifier is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorResponseNull() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");

		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(null);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("missing response", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorDataModelTranslatorNull() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test",
				"operations", "notAMap");

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO(null, null);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("data model translator name is missing", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorDataModelTranslatorEmpty() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test",
				"operations", Map.of());

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("", null);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("data model translator name is missing", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorInterfacePropertiesNull() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test",
				"operations", Map.of("initialize", "notAModel"));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", null);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("data model translator interface properties is missing", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorInterfacePropertiesEmpty() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test",
				"operations", Map.of("initialize", Map.of("method", "GET", "path", "/init")));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/init");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", Map.of());

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.GET, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("data model translator interface properties is missing", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.GET, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorNoAddress() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", Map.of("a", "b"));

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("Missing property: accessAddresses", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorInvalidAddress() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final Map<String, Object> dmtProperties = Map.of(
				"accessAddresses", List.of());

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", dmtProperties);

		final IPropertyValidator addressListValidatorMock = Mockito.mock(IPropertyValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST)).thenReturn(addressListValidatorMock);
		when(addressListValidatorMock.validateAndNormalize(List.of())).thenThrow(new InvalidParameterException("test"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("test", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
		verify(addressListValidatorMock).validateAndNormalize(List.of());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitializeDataModelTranslatorNoAccessPort() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final Map<String, Object> dmtProperties = Map.of(
				"accessAddresses", List.of("localhost"));

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", dmtProperties);

		final IPropertyValidator addressListValidatorMock = Mockito.mock(IPropertyValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST)).thenReturn(addressListValidatorMock);
		when(addressListValidatorMock.validateAndNormalize(List.of("localhost"))).thenReturn(List.of("localhost"));

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("Missing property: accessPort", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
		verify(addressListValidatorMock).validateAndNormalize(List.of("localhost"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeDataModelTranslatorNoBasePath() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final Map<String, Object> dmtProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 23456);

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", dmtProperties);

		final IPropertyValidator addressListValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator portValidatorMock = Mockito.mock(IPropertyValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST)).thenReturn(addressListValidatorMock);
		when(addressListValidatorMock.validateAndNormalize(List.of("localhost"))).thenReturn(List.of("localhost"));
		when(validators.getValidator(PropertyValidatorType.PORT)).thenReturn(portValidatorMock);
		when(portValidatorMock.validateAndNormalize(23456)).thenReturn(23456);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("Missing property: basePath", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
		verify(addressListValidatorMock).validateAndNormalize(List.of("localhost"));
		verify(validators).getValidator(PropertyValidatorType.PORT);
		verify(portValidatorMock).validateAndNormalize(23456);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeDataModelTranslatorInvalidBasePath() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final Map<String, Object> dmtProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 23456,
				"basePath", List.of());

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", dmtProperties);

		final IPropertyValidator addressListValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator portValidatorMock = Mockito.mock(IPropertyValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST)).thenReturn(addressListValidatorMock);
		when(addressListValidatorMock.validateAndNormalize(List.of("localhost"))).thenReturn(List.of("localhost"));
		when(validators.getValidator(PropertyValidatorType.PORT)).thenReturn(portValidatorMock);
		when(portValidatorMock.validateAndNormalize(23456)).thenReturn(23456);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("Invalid property: basePath", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
		verify(addressListValidatorMock).validateAndNormalize(List.of("localhost"));
		verify(validators).getValidator(PropertyValidatorType.PORT);
		verify(portValidatorMock).validateAndNormalize(23456);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeDataModelTranslatorNoOperations() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final Map<String, Object> dmtProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 23456,
				"basePath", "/test");

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", dmtProperties);

		final IPropertyValidator addressListValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator portValidatorMock = Mockito.mock(IPropertyValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST)).thenReturn(addressListValidatorMock);
		when(addressListValidatorMock.validateAndNormalize(List.of("localhost"))).thenReturn(List.of("localhost"));
		when(validators.getValidator(PropertyValidatorType.PORT)).thenReturn(portValidatorMock);
		when(portValidatorMock.validateAndNormalize(23456)).thenReturn(23456);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("Missing property: operations", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
		verify(addressListValidatorMock).validateAndNormalize(List.of("localhost"));
		verify(validators).getValidator(PropertyValidatorType.PORT);
		verify(portValidatorMock).validateAndNormalize(23456);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeDataModelTranslatorNoInitOperation() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final Map<String, HttpOperationModel> ops = Map.of("test", new HttpOperationModel("/a", "GET"));
		final Map<String, Object> dmtProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 23456,
				"basePath", "/test",
				"operations", ops);

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", dmtProperties);

		final IPropertyValidator addressListValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator portValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator operationsValidatorMock = Mockito.mock(IPropertyValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST)).thenReturn(addressListValidatorMock);
		when(addressListValidatorMock.validateAndNormalize(List.of("localhost"))).thenReturn(List.of("localhost"));
		when(validators.getValidator(PropertyValidatorType.PORT)).thenReturn(portValidatorMock);
		when(portValidatorMock.validateAndNormalize(23456)).thenReturn(23456);
		when(validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)).thenReturn(operationsValidatorMock);
		when(operationsValidatorMock.validateAndNormalize(ops)).thenReturn(ops);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("Missing operation: init-translation", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
		verify(addressListValidatorMock).validateAndNormalize(List.of("localhost"));
		verify(validators).getValidator(PropertyValidatorType.PORT);
		verify(portValidatorMock).validateAndNormalize(23456);
		verify(validators).getValidator(PropertyValidatorType.HTTP_OPERATIONS);
		verify(operationsValidatorMock).validateAndNormalize(ops);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeDataModelTranslatorNoGetOperation() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final Map<String, HttpOperationModel> ops = Map.of("init-translation", new HttpOperationModel("/init", "POST"));
		final Map<String, Object> dmtProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 23456,
				"basePath", "/test",
				"operations", ops);

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", dmtProperties);

		final IPropertyValidator addressListValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator portValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator operationsValidatorMock = Mockito.mock(IPropertyValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST)).thenReturn(addressListValidatorMock);
		when(addressListValidatorMock.validateAndNormalize(List.of("localhost"))).thenReturn(List.of("localhost"));
		when(validators.getValidator(PropertyValidatorType.PORT)).thenReturn(portValidatorMock);
		when(portValidatorMock.validateAndNormalize(23456)).thenReturn(23456);
		when(validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)).thenReturn(operationsValidatorMock);
		when(operationsValidatorMock.validateAndNormalize(ops)).thenReturn(ops);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("Missing operation: get-translation-result", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
		verify(addressListValidatorMock).validateAndNormalize(List.of("localhost"));
		verify(validators).getValidator(PropertyValidatorType.PORT);
		verify(portValidatorMock).validateAndNormalize(23456);
		verify(validators).getValidator(PropertyValidatorType.HTTP_OPERATIONS);
		verify(operationsValidatorMock).validateAndNormalize(ops);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeDataModelTranslatorNoAbortOperation() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final Map<String, HttpOperationModel> ops = Map.of(
				"init-translation", new HttpOperationModel("/init", "POST"),
				"get-translation-result", new HttpOperationModel("/get", "GET"));
		final Map<String, Object> dmtProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 23456,
				"basePath", "/test",
				"operations", ops);

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", dmtProperties);

		final IPropertyValidator addressListValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator portValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator operationsValidatorMock = Mockito.mock(IPropertyValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST)).thenReturn(addressListValidatorMock);
		when(addressListValidatorMock.validateAndNormalize(List.of("localhost"))).thenReturn(List.of("localhost"));
		when(validators.getValidator(PropertyValidatorType.PORT)).thenReturn(portValidatorMock);
		when(portValidatorMock.validateAndNormalize(23456)).thenReturn(23456);
		when(validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)).thenReturn(operationsValidatorMock);
		when(operationsValidatorMock.validateAndNormalize(ops)).thenReturn(ops);

		final ArrowheadException ex = assertThrows(
				ExternalServerError.class,
				() -> driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson"));

		assertEquals("Missing operation: abort-translation", ex.getMessage());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
		verify(addressListValidatorMock).validateAndNormalize(List.of("localhost"));
		verify(validators).getValidator(PropertyValidatorType.PORT);
		verify(portValidatorMock).validateAndNormalize(23456);
		verify(validators).getValidator(PropertyValidatorType.HTTP_OPERATIONS);
		verify(operationsValidatorMock).validateAndNormalize(ops);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitializeDataModelTranslatorOk() {
		final Map<String, Object> factoryProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 12345,
				"basePath", "/test");

		final Map<String, HttpOperationModel> ops = Map.of(
				"init-translation", new HttpOperationModel("/init", "POST"),
				"get-translation-result", new HttpOperationModel("/get", "GET"),
				"abort-translation", new HttpOperationModel("/abort", "DELETE"));
		final Map<String, Object> dmtProperties = Map.of(
				"accessAddresses", List.of("localhost"),
				"accessPort", 23456,
				"basePath", "/test",
				"operations", ops);

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test/initialize-translator");
		final DataModelTranslatorFactoryRequestDTO payload = new DataModelTranslatorFactoryRequestDTO("testXml", "testJson");
		final DataModelFactoryTranslatorInitiaizationResponseDTO response = new DataModelFactoryTranslatorInitiaizationResponseDTO("GeneratedDataModel", dmtProperties);

		final IPropertyValidator addressListValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator portValidatorMock = Mockito.mock(IPropertyValidator.class);
		final IPropertyValidator operationsValidatorMock = Mockito.mock(IPropertyValidator.class);

		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(httpService.sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload)).thenReturn(response);
		when(validators.getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST)).thenReturn(addressListValidatorMock);
		when(addressListValidatorMock.validateAndNormalize(List.of("localhost"))).thenReturn(List.of("localhost"));
		when(validators.getValidator(PropertyValidatorType.PORT)).thenReturn(portValidatorMock);
		when(portValidatorMock.validateAndNormalize(23456)).thenReturn(23456);
		when(validators.getValidator(PropertyValidatorType.HTTP_OPERATIONS)).thenReturn(operationsValidatorMock);
		when(operationsValidatorMock.validateAndNormalize(ops)).thenReturn(ops);

		final TranslationDataModelTranslatorInitializationResponseDTO result = driver.initializeDataModelTranslator("Factory", factoryProperties, "testXml", "testJson");

		assertEquals("GeneratedDataModel", result.dataModelTranslatorName());
		assertEquals(dmtProperties, result.interfaceProperties());

		verify(sysInfo).isSslEnabled();
		verify(httpService).sendRequest(uri, HttpMethod.POST, DataModelFactoryTranslatorInitiaizationResponseDTO.class, payload);
		verify(validators).getValidator(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST);
		verify(addressListValidatorMock).validateAndNormalize(List.of("localhost"));
		verify(validators).getValidator(PropertyValidatorType.PORT);
		verify(portValidatorMock).validateAndNormalize(23456);
		verify(validators).getValidator(PropertyValidatorType.HTTP_OPERATIONS);
		verify(operationsValidatorMock).validateAndNormalize(ops);
	}
}