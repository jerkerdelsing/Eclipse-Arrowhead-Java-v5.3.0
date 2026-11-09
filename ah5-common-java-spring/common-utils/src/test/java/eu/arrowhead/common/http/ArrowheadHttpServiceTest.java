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
package eu.arrowhead.common.http;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class ArrowheadHttpServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ArrowheadHttpService service;

	@Mock
	private ServiceCollector collector;

	@Mock
	private HttpService httpService;

	@Mock
	private SystemInfo sysInfo;

	@Mock
	private ServiceOperationNameNormalizer operationNameNormalizer;

	//=================================================================================================
	// members

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConsumeService8ServiceDefNull() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> service.consumeService(null, "test-operation", "ProviderName", Void.TYPE, "payload", null, List.of(), Map.of()));

		assertEquals("Service definition is not specified", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConsumeService8ServiceDefEmpty() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> service.consumeService("", "test-operation", "ProviderName", Void.TYPE, "payload", null, List.of(), Map.of()));

		assertEquals("Service definition is not specified", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConsumeService8OperationNull() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> service.consumeService("testService", null, "ProviderName", Void.TYPE, "payload", null, List.of(), Map.of()));

		assertEquals("Service operation is not specified", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConsumeService8OperationEmpty() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> service.consumeService("testService", "", "ProviderName", Void.TYPE, "payload", null, List.of(), Map.of()));

		assertEquals("Service operation is not specified", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConsumeService8ServiceModelNull() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(collector.getServiceModel("testService", "generic_http", "ProviderName", "test-operation")).thenReturn(null);

		final Throwable ex = assertThrows(DataNotFoundException.class,
				() -> service.consumeService("testService", "test-operation", "ProviderName", Void.TYPE, "payload", null, List.of(), Map.of()));

		verify(collector).getServiceModel("testService", "generic_http", "ProviderName", "test-operation");

		assertEquals("Service definition is not found: testService", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConsumeService8OperationModelNull() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("not-test-operation", new HttpOperationModel("/nop", "POST"))
						.build())
				.build();

		when(collector.getServiceModel("testService", "generic_http", "ProviderName", "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");

		final Throwable ex = assertThrows(ExternalServerError.class,
				() -> service.consumeService("testService", "test-operation", "ProviderName", Void.TYPE, "payload", null, List.of(), Map.of()));

		verify(collector).getServiceModel("testService", "generic_http", "ProviderName", "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");

		assertEquals("Service does not define the specified operation", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService8OkNoHeaders() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		when(collector.getServiceModel("testService", "generic_http", "ProviderName", "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", "ProviderName", Void.TYPE, "payload", null, null, null));

		verify(collector).getServiceModel("testService", "generic_http", "ProviderName", "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService8OkHeadersAndPathSegments() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		when(collector.getServiceModel("testService", "generic_http", "ProviderName", "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfo.getSystemName()).thenReturn("ConsumerName");
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", "ProviderName", Void.TYPE, "payload", null, List.of("a", "b"), Map.of("CustomHeader", "1")));

		verify(collector).getServiceModel("testService", "generic_http", "ProviderName", "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();
		verify(sysInfo).getSystemName();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op/a/b", uriCaptor.getValue().toUriString());
		final HashMap<String, String> headers = headerCaptor.getValue();
		assertEquals(2, headers.size());
		assertTrue(headers.containsKey("CustomHeader"));
		assertEquals("1", headers.get("CustomHeader"));
		assertTrue(headers.containsKey("Authorization"));
		assertEquals("Bearer SYSTEM//ConsumerName", headers.get("Authorization"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService5ProviderNameOk() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		when(collector.getServiceModel("testService", "generic_http", "ProviderName", "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", "ProviderName", Void.TYPE, "payload"));

		verify(collector).getServiceModel("testService", "generic_http", "ProviderName", "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService4Ok() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		when(collector.getServiceModel("testService", "generic_http", null, "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", Void.TYPE, "payload"));

		verify(collector).getServiceModel("testService", "generic_http", null, "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService6Ok() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		final LinkedMultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
		queryMap.put("a", List.of("12"));

		when(collector.getServiceModel("testService", "generic_http", "ProviderName", "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", "ProviderName", Void.TYPE, "payload", queryMap));

		verify(collector).getServiceModel("testService", "generic_http", "ProviderName", "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op?a=12", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService5QueryParamsOk() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		final LinkedMultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
		queryMap.put("a", List.of("12"));

		when(collector.getServiceModel("testService", "generic_http", null, "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", Void.TYPE, "payload", queryMap));

		verify(collector).getServiceModel("testService", "generic_http", null, "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), eq("payload"), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op?a=12", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService5NoPayloadOk() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		final LinkedMultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
		queryMap.put("a", List.of("12"));

		when(collector.getServiceModel("testService", "generic_http", "ProviderName", "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", "ProviderName", Void.TYPE, queryMap));

		verify(collector).getServiceModel("testService", "generic_http", "ProviderName", "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op?a=12", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService4NoPayloadAndProviderOk() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		final LinkedMultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
		queryMap.put("a", List.of("12"));

		when(collector.getServiceModel("testService", "generic_http", null, "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", Void.TYPE, queryMap));

		verify(collector).getServiceModel("testService", "generic_http", null, "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op?a=12", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService4NoPayloadOk() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		when(collector.getServiceModel("testService", "generic_http", "ProviderName", "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", "ProviderName", Void.TYPE));

		verify(collector).getServiceModel("testService", "generic_http", "ProviderName", "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService3Ok() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		when(collector.getServiceModel("testService", "generic_http", null, "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", Void.TYPE));

		verify(collector).getServiceModel("testService", "generic_http", null, "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService5PathParamsOk() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		when(collector.getServiceModel("testService", "generic_http", "ProviderName", "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", "ProviderName", Void.TYPE, List.of("b", "a")));

		verify(collector).getServiceModel("testService", "generic_http", "ProviderName", "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op/b/a", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testConsumeService4PathParamsOk() {
		ReflectionTestUtils.setField(service, "templateName", "generic_http");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http")
						.accessAddress("localhost")
						.accessPort(1234)
						.basePath("/test")
						.operation("test-operation", new HttpOperationModel("/op", "POST"))
						.build())
				.build();

		when(collector.getServiceModel("testService", "generic_http", null, "test-operation")).thenReturn(serviceModel);
		when(operationNameNormalizer.normalize("test-operation")).thenReturn("test-operation");
		when(sysInfo.getIdentityToken()).thenReturn(null);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);
		when(httpService.sendRequest(any(UriComponents.class), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), anyMap())).thenReturn(null);

		assertDoesNotThrow(() -> service.consumeService("testService", "test-operation", Void.TYPE, List.of("b", "a")));

		verify(collector).getServiceModel("testService", "generic_http", null, "test-operation");
		verify(operationNameNormalizer).normalize("test-operation");
		verify(sysInfo).getIdentityToken();
		verify(sysInfo).getAuthenticationPolicy();

		final ArgumentCaptor<UriComponents> uriCaptor = ArgumentCaptor.forClass(UriComponents.class);
		final ArgumentCaptor<HashMap<String, String>> headerCaptor = ArgumentCaptor.forClass(HashMap.class);

		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.TYPE), isNull(), isNull(), headerCaptor.capture());

		assertEquals("http://localhost:1234/test/op/b/a", uriCaptor.getValue().toUriString());
		assertTrue(headerCaptor.getValue().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitSSL() {
		assertNull(ReflectionTestUtils.getField(service, "templateName"));

		when(sysInfo.isSslEnabled()).thenReturn(true);

		assertDoesNotThrow(() -> ReflectionTestUtils.invokeGetterMethod(service, "init"));

		verify(sysInfo).isSslEnabled();

		assertEquals("generic_https", ReflectionTestUtils.getField(service, "templateName"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitNoSSL() {
		assertNull(ReflectionTestUtils.getField(service, "templateName"));

		when(sysInfo.isSslEnabled()).thenReturn(false);

		assertDoesNotThrow(() -> ReflectionTestUtils.invokeGetterMethod(service, "init"));

		verify(sysInfo).isSslEnabled();

		assertEquals("generic_http", ReflectionTestUtils.getField(service, "templateName"));
	}
}