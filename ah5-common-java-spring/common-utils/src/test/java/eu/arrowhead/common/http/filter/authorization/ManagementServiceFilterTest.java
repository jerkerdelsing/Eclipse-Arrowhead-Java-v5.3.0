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
package eu.arrowhead.common.http.filter.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class ManagementServiceFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementServiceFilterTestHelper filter; // this is the trick

	@Mock
	private SystemInfo sysInfo;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private ServiceOperationNameNormalizer operationNameNormalizer;

	@Mock
	private ArrowheadHttpService httpService;

	@Mock
	private FilterChain chain;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalNotManagementPath() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalSysopOnlySysopTrue() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", true);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.SYSOP_ONLY);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalSysopOnlySysopFalse() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.SYSOP_ONLY);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalWhitelistSysopTrue() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", true);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalWhitelistOnTheWhitelist() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("RequesterSystem"));

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalWhitelistNotOnTheWhitelist() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("OtherSystem"));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthorizationSysopTrue() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", true);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthorizationOnTheWhitelist() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("RequesterSystem"));

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthorizationAuthorized() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");
		request.setMethod("POST");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("serviceRegistryManagement")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/mgmt")
						.operations(Map.of(
								"system-query", new HttpOperationModel.Builder()
										.path("/systems/query")
										.method("POST")
										.build(),
								"system-create", new HttpOperationModel.Builder()
										.path("/systems")
										.method("POST")
										.build(),
								"system-update", new HttpOperationModel.Builder()
										.path("/systems")
										.method("PUT")
										.build(),
								"system-remove", new HttpOperationModel.Builder()
										.path("/systems")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of());
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(serviceDefNameNormalizer.normalize("serviceRegistryManagement")).thenReturn("serviceRegistryManagement");
		when(operationNameNormalizer.normalize("system-create")).thenReturn("system-create");
		when(httpService.consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class))).thenReturn(true);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(serviceDefNameNormalizer).normalize("serviceRegistryManagement");
		verify(operationNameNormalizer).normalize("system-create");
		verify(httpService).consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthorizationNotAuthorized() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");
		request.setMethod("POST");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("serviceRegistryManagement")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_https", "localhost", 8443)
						.basePath("/serviceregistry/mgmt")
						.operations(Map.of(
								"system-query", new HttpOperationModel.Builder()
										.path("/systems/query")
										.method("POST")
										.build(),
								"system-create", new HttpOperationModel.Builder()
										.path("/systems")
										.method("POST")
										.build(),
								"system-update", new HttpOperationModel.Builder()
										.path("/systems")
										.method("PUT")
										.build(),
								"system-remove", new HttpOperationModel.Builder()
										.path("/systems")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of());
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(serviceDefNameNormalizer.normalize("serviceRegistryManagement")).thenReturn("serviceRegistryManagement");
		when(operationNameNormalizer.normalize("system-create")).thenReturn("system-create");
		when(httpService.consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class))).thenReturn(false);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(serviceDefNameNormalizer).normalize("serviceRegistryManagement");
		verify(operationNameNormalizer).normalize("system-create");
		verify(httpService).consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthorizationConnectionProblem() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");
		request.setMethod("POST");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("serviceRegistryManagement")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_https", "localhost", 8443)
						.basePath("/serviceregistry/mgmt")
						.operations(Map.of(
								"system-query", new HttpOperationModel.Builder()
										.path("/systems/query")
										.method("POST")
										.build(),
								"system-create", new HttpOperationModel.Builder()
										.path("/systems")
										.method("POST")
										.build(),
								"system-update", new HttpOperationModel.Builder()
										.path("/systems")
										.method("PUT")
										.build(),
								"system-remove", new HttpOperationModel.Builder()
										.path("/systems")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of());
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(serviceDefNameNormalizer.normalize("serviceRegistryManagement")).thenReturn("serviceRegistryManagement");
		when(operationNameNormalizer.normalize("system-create")).thenReturn("system-create");
		when(httpService.consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class))).thenThrow(ArrowheadException.class);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(serviceDefNameNormalizer).normalize("serviceRegistryManagement");
		verify(operationNameNormalizer).normalize("system-create");
		verify(httpService).consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthorizationNotFoundServiceDefNoModel() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");
		request.setMethod("POST");

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of());
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getServices()).thenReturn(List.of());

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthorizationNotFoundServiceDefNotMatchingInterface() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");
		request.setMethod("POST");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("serviceRegistryManagement")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_https", "localhost", 8443)
						.basePath("/serviceregistry/mgmt")
						.operations(Map.of(
								"system-query", new HttpOperationModel.Builder()
										.path("/systems/query")
										.method("POST")
										.build(),
								"system-create", new HttpOperationModel.Builder()
										.path("/systems")
										.method("POST")
										.build(),
								"system-update", new HttpOperationModel.Builder()
										.path("/systems")
										.method("PUT")
										.build(),
								"system-remove", new HttpOperationModel.Builder()
										.path("/systems")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of());
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthorizationNotFoundServiceDefNoOperations() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");
		request.setMethod("POST");

		final Map<String, HttpOperationModel> operations = new HashMap<>();
		operations.put("system-remove", new HttpOperationModel.Builder()
						.path("/systems")
						.method("DELETE")
						.build());
		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("serviceRegistryManagement")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/mgmt")
						.operations(operations)
						.build())
				.build();
		operations.clear();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of());
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthorizationNotFoundServiceDefNotMatchingOperation() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/mgmt/systems");
		request.setMethod("POST");

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("serviceRegistryManagement")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/mgmt")
						.operations(Map.of(
								"system-query", new HttpOperationModel.Builder()
										.path("/systems/query")
										.method("POST")
										.build(),
								"system-update", new HttpOperationModel.Builder()
										.path("/systems")
										.method("PUT")
										.build(),
								"system-remove", new HttpOperationModel.Builder()
										.path("/systems")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of());
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}
}