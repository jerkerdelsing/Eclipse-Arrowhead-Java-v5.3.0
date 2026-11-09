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
package eu.arrowhead.common.http.filter.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
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

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.thirdparty.MultiReadRequestWrapper;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class OutsourcedFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OutsourcedFilter filter = new OutsourcedFilterTestHelper(); // this is the trick

	@Mock
	private SystemInfo sysInfo;

	@Mock
	private ServiceCollector collector;

	@Mock
	private ArrowheadHttpService httpService;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private FilterChain chain;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthenticationLookup() throws IOException, ServletException {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName("identity")
				.build();

		final MockHttpServletRequest origRequest = new MockHttpServletRequest();
		origRequest.setScheme("http");
		origRequest.setServerName("localhost");
		origRequest.setServerPort(8443);
		origRequest.setRequestURI("/serviceregistry/service-discovery/lookup");
		origRequest.setMethod("POST");
		origRequest.setContent(Utilities.toJson(payload).getBytes(StandardCharsets.UTF_8));
		final HttpServletRequest request = new MultiReadRequestWrapper(origRequest);

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
	public void testDoFilterInternalNoAuthHeader() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("No authorization header has been provided", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthHeaderTooShort() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "invalidHeader");

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_https", "ServiceRegistry")).thenReturn(null);

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_https", "ServiceRegistry");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthHeaderTooLong() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Too Much Words");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");

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

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthHeaderInvalidSchema() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "InvalidSchema something");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new TestHttpInterfaceModel(
						"generic_http",
						List.of("localhost"),
						8443,
						null,
						Map.of(
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
										.build())))
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry")).thenReturn(serviceDiscoverySM);

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthHeaderInvalidPrefix() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer SYSTEM//RequesterSystem");
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

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthenticatorKeysNotSupported() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer AUTHENTICATOR-KEY//RequesterSystem");
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

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthenticatorKeyPartTooShort() throws IOException, ServletException {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("RequesterSystem", "key"));

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer AUTHENTICATOR-KEY//RequesterSystem");
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8443);
		request.setRequestURI("/serviceregistry/service-discovery/lookup");
		request.setMethod("POST");
		request.setContent("payload".getBytes());

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

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthenticatorKeyPartTooLong() throws IOException, ServletException {
		final Map<String, String> secretKeys = Map.of("RequesterSystem", "key");
		ReflectionTestUtils.setField(filter, "secretKeys", secretKeys);

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer AUTHENTICATOR-KEY//RequesterSystem//hash//other");
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

		try (MockedStatic<Utilities> utilMock = Mockito.mockStatic(Utilities.class)) {
			utilMock.when(() -> Utilities.stripEndSlash(anyString())).thenCallRealMethod();
			utilMock.when(() -> Utilities.fromJson(anyString(), eq(ServiceInstanceLookupRequestDTO.class))).thenReturn(null);
			utilMock.when(() -> Utilities.isEmpty(anyString())).thenReturn(false);
			utilMock.when(() -> Utilities.isEmpty(any(Collection.class))).thenReturn(false);

			final Throwable ex = assertThrows(AuthException.class,
					() -> filter.doFilterInternal(request, null, chain));

			utilMock.verify(() -> Utilities.stripEndSlash(anyString()));
			utilMock.verify(() -> Utilities.fromJson(anyString(), eq(ServiceInstanceLookupRequestDTO.class)));
			utilMock.verify(() -> Utilities.isEmpty("Bearer AUTHENTICATOR-KEY//RequesterSystem//hash//other"));
			utilMock.verify(() -> Utilities.isEmpty(secretKeys));

			assertEquals("Invalid authorization header", ex.getMessage());
		}

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthenticatorKeyNoMatch() throws IOException, ServletException {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("Authentication", "key"));

		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.providerName("Authentication")
				.build();

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer AUTHENTICATOR-KEY//RequesterSystem//hash");
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
		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthenticatorKeyHashesDoesNotMatch() throws IOException, ServletException {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("RequesterSystem", "123456"));

		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionNames(List.of("identity", "identityManagement"))
				.build();

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer AUTHENTICATOR-KEY//RequesterSystem//hash");
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
		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthenticatorKeyInvalidSecretKey() throws IOException, ServletException {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("RequesterSystem", ""));

		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName("identityManagement")
				.build();

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer AUTHENTICATOR-KEY//RequesterSystem//hash");
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
		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_http", "ServiceRegistry");
		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalAuthenticatorKeyOk() throws IOException, ServletException {
		ReflectionTestUtils.setField(filter, "secretKeys", Map.of("RequesterSystem", "123456"));

		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer AUTHENTICATOR-KEY//RequesterSystem//e2f623e35a98ebf7211c0c80851650cfe7cd0af08e62dcfe7517df1c3c41302d");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("RequesterSystem", request.getAttribute("arrowhead.authenticated.system"));
		assertEquals(false, request.getAttribute("arrowhead.sysop.request"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalIdentityTokenPartTooShort() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer IDENTITY-TOKEN");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalIdentityTokenPartTooLong() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer IDENTITY-TOKEN//RequesterSystem//something");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalIdentityTokenFalseResult() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer IDENTITY-TOKEN//token");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(httpService.consumeService("identity", "identity-verify", IdentityVerifyResponseDTO.class, List.of("token"))).thenReturn(new IdentityVerifyResponseDTO(false, null, null, null, null));

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(httpService).consumeService("identity", "identity-verify", IdentityVerifyResponseDTO.class, List.of("token"));
		verify(chain, never()).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("Invalid authorization header", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterInternalIdentityTokenTrueResult() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer IDENTITY-TOKEN//token");

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(httpService.consumeService("identity", "identity-verify", IdentityVerifyResponseDTO.class, List.of("token"))).thenReturn(new IdentityVerifyResponseDTO(
				true,
				"RequesterSystem",
				true,
				"2025-07-25T11:31:00Z",
				"2025-07-25T11:41:00Z"));

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(sysInfo).getSystemName();
		verify(httpService).consumeService("identity", "identity-verify", IdentityVerifyResponseDTO.class, List.of("token"));
		verify(chain).doFilter(any(HttpServletRequest.class), isNull());

		assertEquals("RequesterSystem", request.getAttribute("arrowhead.authenticated.system"));
		assertEquals(true, request.getAttribute("arrowhead.sysop.request"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitNoRawKeys() {
		ReflectionTestUtils.setField(filter, "rawSecretKeys", Map.of());
		ReflectionTestUtils.invokeMethod(filter, "init");

		verify(systemNameNormalizer, never()).normalize(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInitOk() {
		ReflectionTestUtils.setField(filter, "rawSecretKeys", Map.of("System1", " Key1", "System2", "Key2 "));

		when(systemNameNormalizer.normalize("System1")).thenReturn("System1");
		when(systemNameNormalizer.normalize("System2")).thenReturn("System2");

		ReflectionTestUtils.invokeMethod(filter, "init");

		verify(systemNameNormalizer, times(2)).normalize(anyString());

		final Map<String, String> result = (Map<String, String>) ReflectionTestUtils.getField(filter, "secretKeys");
		assertEquals("Key1", result.get("System1"));
		assertEquals("Key2", result.get("System2"));
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public record TestHttpInterfaceModel(
			String templateName,
			List<String> accessAddresses,
			int accessPort,
			String basePath,
			Map<String, HttpOperationModel> operations) implements InterfaceModel {

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public String protocol() {
			return templateName.equals(Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME) ? Constants.HTTP : Constants.HTTPS;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Map<String, Object> properties() {
			final Map<String, Object> result = new HashMap<>(4);
			result.put(HttpInterfaceModel.PROP_NAME_ACCESS_ADDRESSES, accessAddresses);
			result.put(HttpInterfaceModel.PROP_NAME_ACCESS_PORT, accessPort);
			result.put(HttpInterfaceModel.PROP_NAME_BASE_PATH, basePath);
			result.put(HttpInterfaceModel.PROP_NAME_OPERATIONS, operations);

			return result;
		}
	}
}