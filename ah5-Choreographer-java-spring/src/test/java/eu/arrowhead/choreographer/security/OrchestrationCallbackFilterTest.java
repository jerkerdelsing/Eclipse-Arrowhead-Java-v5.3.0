/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.choreographer.ChoreographerConstants;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.IAuthenticationPolicyFilter;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

// Release 5.3.0 negative-access review: orchestration results may be pushed only by the DynamicServiceOrchestration of
// the local cloud. A push without a certificate is refused (401); one from a foreign cloud or from another local
// system is forbidden (403). Every other request goes to the configured authentication policy filter.
public class OrchestrationCallbackFilterTest {

	private static final String CALLBACK = ChoreographerConstants.HTTP_API_ORCHESTRATION_RESULT_PATH + "/0123abcd";
	private static final String LOCAL = ".TestCloud.Company.arrowhead.eu";

	private OrchestrationCallbackFilter filter;
	private IAuthenticationPolicyFilter delegate;
	private SystemInfo sysInfo;

	@BeforeEach
	public void setUp() {
		delegate = mock(IAuthenticationPolicyFilter.class);
		sysInfo = mock(SystemInfo.class);
		when(sysInfo.isSslEnabled()).thenReturn(true);
		final CloudIdentifierNormalizer cloudNormalizer = mock(CloudIdentifierNormalizer.class);
		when(cloudNormalizer.normalize(anyString())).thenAnswer(i -> i.getArgument(0));
		final ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(CloudIdentifierNormalizer.class)).thenReturn(cloudNormalizer);
		final Map<String, Object> arrowheadContext = new HashMap<>();
		arrowheadContext.put(Constants.SERVER_COMMON_NAME, "Choreographer" + LOCAL);

		filter = new OrchestrationCallbackFilter(delegate);
		ReflectionTestUtils.setField(filter, "sysInfo", sysInfo);
		ReflectionTestUtils.setField(filter, "appContext", context);
		ReflectionTestUtils.setField(filter, "systemNameNormalizer", new SystemNameNormalizer());
		ReflectionTestUtils.setField(filter, "arrowheadContext", arrowheadContext);
		ReflectionTestUtils.setField(filter, "mapper", new ObjectMapper());
	}

	@Test
	public void localDynamicServiceOrchestrationMayPush() throws Exception {
		final MockHttpServletRequest request = request("POST", CALLBACK, Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION + LOCAL);
		assertEquals(200, run(request));
		assertEquals(Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION, request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM));
	}

	@Test
	public void pushWithoutACertificateIsRefused() throws Exception {
		assertEquals(401, run(request("POST", CALLBACK, null)));
	}

	@Test
	public void pushFromAForeignCloudIsForbidden() throws Exception {
		assertEquals(403, run(request("POST", CALLBACK, Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION + ".OtherCloud.Company.arrowhead.eu")));
		assertEquals(403, run(request("POST", CALLBACK, Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION + ".TestCloud.OtherOrg.arrowhead.eu")));
	}

	@Test
	public void pushFromAnotherLocalSystemIsForbidden() throws Exception {
		assertEquals(403, run(request("POST", CALLBACK, "PlantDescriptionEngine" + LOCAL)));
		assertEquals(403, run(request("POST", CALLBACK, "Sysop" + LOCAL)));
	}

	@Test
	public void withoutSslThePushIsAttributedToTheOrchestrator() throws Exception {
		when(sysInfo.isSslEnabled()).thenReturn(false);
		final MockHttpServletRequest request = request("POST", CALLBACK, null);
		assertEquals(200, run(request));
		assertEquals(Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION, request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM));
	}

	@Test
	public void otherRequestsGoToTheAuthenticationPolicyFilter() throws Exception {
		final MockHttpServletRequest get = request("GET", CALLBACK, "PlantDescriptionEngine" + LOCAL);
		run(get);
		final MockHttpServletRequest other = request("POST", ChoreographerConstants.HTTP_API_BASE_PATH + "/session/start", "PlantDescriptionEngine" + LOCAL);
		run(other);
		verify(delegate, times(2)).doFilter(any(), any(), any());
		assertNull(get.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM));
		assertNull(other.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM));
	}

	@Test
	public void theCallbackPathNeedsACorrelationId() {
		assertEquals(false, OrchestrationCallbackFilter.isCallback(new MockHttpServletRequest("POST", ChoreographerConstants.HTTP_API_ORCHESTRATION_RESULT_PATH)));
		assertEquals(true, OrchestrationCallbackFilter.isCallback(new MockHttpServletRequest("POST", CALLBACK)));
		assertEquals(false, OrchestrationCallbackFilter.isCallback(new MockHttpServletRequest("PUT", CALLBACK)));
	}

	private static MockHttpServletRequest request(final String method, final String path, final String commonName) {
		final MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		if (commonName != null) {
			final X509Certificate certificate = mock(X509Certificate.class);
			when(certificate.getSubjectX500Principal()).thenReturn(new X500Principal("CN=" + commonName + ",O=Company"));
			request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[] { certificate });
		}
		return request;
	}

	private int run(final MockHttpServletRequest request) throws Exception {
		final MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response.getStatus();
	}
}
