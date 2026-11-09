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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.thirdparty.MultiReadRequestWrapper;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class BlacklistFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private BlacklistFilterTestHelper filter; // this is the trick

	@Mock
	protected SystemInfo sysInfo;

	@Mock
	protected ArrowheadHttpService arrowheadHttpService;

	@Mock
	private ServiceCollector collector;

	@Mock
	private FilterChain chain;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalSysop() throws IOException, ServletException {
		final HttpServletRequest request = new MultiReadRequestWrapper(new MockHttpServletRequest());
		request.setAttribute("arrowhead.sysop.request", true);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthenticationLookup() throws IOException, ServletException {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName("identity")
				.build();

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setMethod("POST");
		request.setContent(Utilities.toJson(payload).getBytes(StandardCharsets.UTF_8));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklist() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "Blacklist");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalExcluded() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_https", "ServiceRegistry")).thenReturn(null);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of("RequesterSystem"));

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_https", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerAuthException() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();
		serviceDiscoverySM.interfaces().clear();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenThrow(new AuthException("test auth"));

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("test auth", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerTrue() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_https", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenReturn(true);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("RequesterSystem system is blacklisted", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerConnectionIssueForceTrue() throws IOException, ServletException {
		ReflectionTestUtils.setField(filter, "force", true);
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new DummyHttpInterfaceModel("generic_http"))
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenThrow(ArrowheadException.class);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Blacklist system is not available, the system might be blacklisted", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerConnectionIssueForceFalse() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerFalse() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/register");
		request.setMethod("POST");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerFalseWrongMethod() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setMethod("GET");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerFalseNotAcceptablePayload() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setMethod("POST");
		request.setContent("payload".getBytes(StandardCharsets.UTF_8));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerFalseNoPayload() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setMethod("POST");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenThrow(ArrowheadException.class);

		try (MockedStatic<Utilities> utilMock = Mockito.mockStatic(Utilities.class)) {
			utilMock.when(() -> Utilities.stripEndSlash(anyString())).thenCallRealMethod();
			utilMock.when(() -> Utilities.isEmpty(any(Collection.class))).thenReturn(false);
			utilMock.when(() -> Utilities.fromJson(anyString(), eq(ServiceInstanceLookupRequestDTO.class))).thenReturn(null);

			assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

			utilMock.verify(() -> Utilities.stripEndSlash(anyString()));
			utilMock.verify(() -> Utilities.isEmpty(any(Collection.class)));
			utilMock.verify(() -> Utilities.fromJson(anyString(), eq(ServiceInstanceLookupRequestDTO.class)));
		}

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerFalseNoServiceDefinition() throws IOException, ServletException {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.providerName("Authentication")
				.build();

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setMethod("POST");
		request.setContent(Utilities.toJson(payload).getBytes(StandardCharsets.UTF_8));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerFalseMultipleServiceDefinitions() throws IOException, ServletException {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionNames(List.of("identity", "identityManagement"))
				.build();

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setMethod("POST");
		request.setContent(Utilities.toJson(payload).getBytes(StandardCharsets.UTF_8));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalBlacklistServerFalseWrongServiceDefinitions() throws IOException, ServletException {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName("identityManagement")
				.build();

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setMethod("POST");
		request.setContent(Utilities.toJson(payload).getBytes(StandardCharsets.UTF_8));

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "localhost", 8443)
						.basePath("/serviceregistry/service-discovery")
						.operations(Map.of(
								"register", new HttpOperationModel.Builder()
										.path("/register")
										.method("POST")
										.build(),
								"lookup", new HttpOperationModel.Builder()
										.path("/lookup")
										.method("POST")
										.build(),
								"revoke", new HttpOperationModel.Builder()
										.path("/revoke")
										.method("DELETE")
										.build()))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"))).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, List.of("RequesterSystem"));
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static class DummyHttpInterfaceModel implements InterfaceModel {
		private String name;

		//-------------------------------------------------------------------------------------------------
		public DummyHttpInterfaceModel(final String name) {
			this.name = name;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String templateName() {
			return name;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String protocol() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Map<String, Object> properties() {
			return Map.of();
		}
	}
}