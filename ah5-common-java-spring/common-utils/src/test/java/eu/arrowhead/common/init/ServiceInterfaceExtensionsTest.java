/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.common.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

public class ServiceInterfaceExtensionsTest {

	private static final String BRIDGE = "[{\"serviceDefinition\":\"identity\",\"templateName\":\"generic_http\",\"address\":\"authentication-identity-bridge\",\"port\":8444}]";

	private static ServiceModel identity() {
		return new ServiceModel.Builder()
				.serviceDefinition("identity")
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_https", "authentication", 8444)
						.basePath("/authentication/identity")
						.operation("identity-verify", new HttpOperationModel.Builder().method("GET").path("/verify").build())
						.build())
				.build();
	}

	private static List<ServiceInstanceInterfaceRequestDTO> nativeOf(final ServiceModel model) {
		return model.interfaces().stream().map(i -> new ServiceInstanceInterfaceRequestDTO(i.templateName(), i.protocol(), "CERT_AUTH", i.properties())).toList();
	}

	private static SystemInfo sysInfo(final AuthenticationPolicy policy, final boolean ssl) {
		final SystemInfo sysInfo = mock(SystemInfo.class);
		when(sysInfo.getAuthenticationPolicy()).thenReturn(policy);
		when(sysInfo.isSslEnabled()).thenReturn(ssl);
		return sysInfo;
	}

	@Test
	public void nativePolicyIsCertAuthBehindTls() {
		assertEquals(ServiceInterfacePolicy.CERT_AUTH, ServiceInterfaceExtensions.nativePolicy("", sysInfo(AuthenticationPolicy.CERTIFICATE, true)));
		assertEquals(ServiceInterfacePolicy.CERT_AUTH, ServiceInterfaceExtensions.nativePolicy(null, sysInfo(AuthenticationPolicy.OUTSOURCED, true)));
		assertEquals(ServiceInterfacePolicy.CERT_AUTH, ServiceInterfaceExtensions.nativePolicy(" ", sysInfo(AuthenticationPolicy.INTERNAL, true)));
	}

	@Test
	public void nativePolicyIsNoneForDeclaredIdentitiesOrWithoutTls() {
		assertEquals(ServiceInterfacePolicy.NONE, ServiceInterfaceExtensions.nativePolicy("", sysInfo(AuthenticationPolicy.DECLARED, true)));
		assertEquals(ServiceInterfacePolicy.NONE, ServiceInterfaceExtensions.nativePolicy("", sysInfo(AuthenticationPolicy.OUTSOURCED, false)));
		assertEquals(ServiceInterfacePolicy.NONE, ServiceInterfaceExtensions.nativePolicy("", sysInfo(AuthenticationPolicy.INTERNAL, false)));
	}

	@Test
	public void nativePolicyOverrideWins() {
		assertEquals(ServiceInterfacePolicy.NONE, ServiceInterfaceExtensions.nativePolicy(" none ", sysInfo(AuthenticationPolicy.OUTSOURCED, true)));
		assertEquals(ServiceInterfacePolicy.CERT_AUTH, ServiceInterfaceExtensions.nativePolicy("CERT_AUTH", sysInfo(AuthenticationPolicy.DECLARED, false)));
	}

	@Test
	public void addsBridgeWithNativeBasePathAndDefaults() {
		final ServiceModel model = identity();
		final List<ServiceInstanceInterfaceRequestDTO> extras = ServiceInterfaceExtensions.extrasFor(model, nativeOf(model), ServiceInterfaceExtensions.parse(BRIDGE));
		assertEquals(1, extras.size());
		assertEquals("generic_http", extras.get(0).templateName());
		assertEquals("http", extras.get(0).protocol());
		assertEquals("NONE", extras.get(0).policy());
		assertEquals(List.of("authentication-identity-bridge"), extras.get(0).properties().get("accessAddresses"));
		assertEquals("/authentication/identity", extras.get(0).properties().get("basePath"));
	}

	@Test
	public void ignoresOtherServicesAndEmptyConfiguration() {
		final ServiceModel model = identity();
		assertTrue(ServiceInterfaceExtensions.parse("[]").isEmpty());
		assertTrue(ServiceInterfaceExtensions.extrasFor(model, nativeOf(model), ServiceInterfaceExtensions.parse(
				"[{\"serviceDefinition\":\"generalManagement\",\"templateName\":\"generic_http\",\"address\":\"x\",\"port\":1}]")).isEmpty());
	}

	@Test
	public void rejectsInvalidConfiguration() {
		assertThrows(IllegalArgumentException.class, () -> ServiceInterfaceExtensions.parse("{"));
		assertThrows(IllegalArgumentException.class, () -> ServiceInterfaceExtensions.parse("[{\"serviceDefinition\":\"identity\"}]"));
	}
}
