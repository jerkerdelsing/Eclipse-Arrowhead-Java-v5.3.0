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
package eu.arrowhead.authorization.http.filter.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

import eu.arrowhead.authorization.AuthorizationConstants;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.filter.authorization.ManagementPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class InternalManagementServiceFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private InternalManagementServiceFilterTestHelper filter; // this is the trick

	@Mock
	private SystemInfo sysInfo;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private ServiceOperationNameNormalizer serviceOpNameNormalizer;

	@Mock
	private AuthorizationPolicyEngine policyEngine;

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
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/grant");

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
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

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
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.SYSOP_ONLY);

		final Throwable ex = assertThrows(
				ForbiddenException.class,
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
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

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
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("RequesterSystem"));

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("RequesterSystem");
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
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("OtherSystem"));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("RequesterSystem");
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
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

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
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("RequesterSystem"));

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(chain).doFilter(request, null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorizedServiceDefNotFoundNoServices() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("OtherSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of());

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorizedServiceDefNotFoundInterfaceDoesNotMatch() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("https");
		request.setServerName("localhost");
		request.setServerPort(8445);
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		final InterfaceModel serviceInterfaceModel = new HttpInterfaceModel.Builder("generic_http", "localhost", 8445)
				.basePath("/consumerauthorization/authorization/mgmt")
				.operation("grant-policies", new HttpOperationModel.Builder()
						.method(HttpMethod.POST.name())
						.path(AuthorizationConstants.HTTP_API_OP_GRANT_PATH)
						.build())
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("authorizationManagement")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("OtherSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorizedServiceDefNotFoundMethodDoesNotMatch() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8445);
		request.setMethod(HttpMethod.GET.name());
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		final InterfaceModel serviceInterfaceModel = new HttpInterfaceModel.Builder("generic_http", "localhost", 8445)
				.basePath("/consumerauthorization/authorization/mgmt")
				.operation("grant-policies", new HttpOperationModel.Builder()
						.method(HttpMethod.POST.name())
						.path(AuthorizationConstants.HTTP_API_OP_GRANT_PATH)
						.build())
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("authorizationManagement")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("OtherSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorizedServiceDefNotFoundPathDoesNotMatch() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8445);
		request.setMethod(HttpMethod.POST.name());
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		final InterfaceModel serviceInterfaceModel = new HttpInterfaceModel.Builder("generic_http", "localhost", 8445)
				.basePath("/consumerauthorization/auth/mgmt")
				.operation("grant-policies", new HttpOperationModel.Builder()
						.method(HttpMethod.POST.name())
						.path(AuthorizationConstants.HTTP_API_OP_GRANT_PATH)
						.build())
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("authorizationManagement")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("OtherSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorized() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8445);
		request.setMethod(HttpMethod.POST.name());
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		final InterfaceModel serviceInterfaceModel = new HttpInterfaceModel.Builder("generic_http", "localhost", 8445)
				.basePath("/consumerauthorization/authorization/mgmt")
				.operation("grant-policies", new HttpOperationModel.Builder()
						.method(HttpMethod.POST.name())
						.path(AuthorizationConstants.HTTP_API_OP_GRANT_PATH)
						.build())
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("authorizationManagement")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		final NormalizedVerifyRequest verifyRequest = new NormalizedVerifyRequest(
				"ConsumerAuthorization",
				"RequesterSystem",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"authorizationManagement",
				"grant-policies");

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("OtherSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(systemNameNormalizer.normalize("ConsumerAuthorization")).thenReturn("ConsumerAuthorization");
		when(serviceDefNameNormalizer.normalize("authorizationManagement")).thenReturn("authorizationManagement");
		when(serviceOpNameNormalizer.normalize("grant-policies")).thenReturn("grant-policies");
		when(policyEngine.isAccessGranted(verifyRequest)).thenReturn(false);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(sysInfo).getSystemName();
		verify(systemNameNormalizer).normalize("ConsumerAuthorization");
		verify(serviceDefNameNormalizer).normalize("authorizationManagement");
		verify(serviceOpNameNormalizer).normalize("grant-policies");
		verify(policyEngine).isAccessGranted(verifyRequest);
		verify(chain, never()).doFilter(request, null);

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationAuthorized() throws IOException, ServletException {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");
		request.setAttribute("arrowhead.sysop.request", false);
		request.setScheme("http");
		request.setServerName("localhost");
		request.setServerPort(8445);
		request.setMethod(HttpMethod.POST.name());
		request.setRequestURI("/consumerauthorization/authorization/mgmt/grant");

		final InterfaceModel serviceInterfaceModel = new HttpInterfaceModel.Builder("generic_http", "localhost", 8445)
				.basePath("/consumerauthorization/authorization/mgmt")
				.operation("grant-policies", new HttpOperationModel.Builder()
						.method(HttpMethod.POST.name())
						.path(AuthorizationConstants.HTTP_API_OP_GRANT_PATH)
						.build())
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("authorizationManagement")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		final NormalizedVerifyRequest verifyRequest = new NormalizedVerifyRequest(
				"ConsumerAuthorization",
				"RequesterSystem",
				"LOCAL",
				AuthorizationTargetType.SERVICE_DEF,
				"authorizationManagement",
				"grant-policies");

		when(systemNameNormalizer.normalize("RequesterSystem")).thenReturn("RequesterSystem");
		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("OtherSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");
		when(systemNameNormalizer.normalize("ConsumerAuthorization")).thenReturn("ConsumerAuthorization");
		when(serviceDefNameNormalizer.normalize("authorizationManagement")).thenReturn("authorizationManagement");
		when(serviceOpNameNormalizer.normalize("grant-policies")).thenReturn("grant-policies");
		when(policyEngine.isAccessGranted(verifyRequest)).thenReturn(true);

		assertDoesNotThrow(() -> filter.doFilterInternal(request, null, chain));

		verify(systemNameNormalizer).normalize("RequesterSystem");
		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(sysInfo).getSystemName();
		verify(systemNameNormalizer).normalize("ConsumerAuthorization");
		verify(serviceDefNameNormalizer).normalize("authorizationManagement");
		verify(serviceOpNameNormalizer).normalize("grant-policies");
		verify(policyEngine).isAccessGranted(verifyRequest);
		verify(chain).doFilter(request, null);
	}
}