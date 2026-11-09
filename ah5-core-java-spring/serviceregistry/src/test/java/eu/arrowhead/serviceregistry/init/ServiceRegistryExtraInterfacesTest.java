/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.serviceregistry.init;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;

public class ServiceRegistryExtraInterfacesTest {

	private static final String JSON = "[{\"serviceDefinition\":\"systemDiscovery\",\"templateName\":\"generic_https\",\"protocol\":\"https\","
			+ "\"policy\":\"CERT_AUTH\",\"address\":\"authentication-registry-mtls\",\"port\":8443},"
			+ "{\"serviceDefinition\":\"generalManagement\",\"templateName\":\"generic_https\",\"address\":\"registry-bootstrap-proxy\",\"port\":8501}]";

	private static ServiceModel model(final String definition, final String basePath) {
		return new ServiceModel.Builder()
				.serviceDefinition(definition)
				.version("1.0.0")
				.serviceInterface(new HttpInterfaceModel.Builder("generic_http", "serviceregistry", 8443)
						.basePath(basePath)
						.operation("lookup", new HttpOperationModel.Builder().method("POST").path("/lookup").build())
						.build())
				.build();
	}

	private static List<ServiceInstanceInterfaceRequestDTO> nativeOf(final ServiceModel model) {
		return model.interfaces().stream().map(i -> new ServiceInstanceInterfaceRequestDTO(i.templateName(), i.protocol(), "NONE", i.properties())).toList();
	}

	@Test
	public void emptyOrMissingConfigurationAddsNothing() {
		assertTrue(ServiceRegistryApplicationInitListener.parseExtraInterfaces("[]").isEmpty());
		assertTrue(ServiceRegistryApplicationInitListener.parseExtraInterfaces(" ").isEmpty());
		final ServiceModel model = model("systemDiscovery", "/serviceregistry/system-discovery");
		assertTrue(ServiceRegistryApplicationInitListener.extraInterfacesFor(model, nativeOf(model), List.of()).isEmpty());
	}

	@Test
	public void addsConfiguredIngressWithNativeBasePathAndOperations() {
		final var extras = ServiceRegistryApplicationInitListener.parseExtraInterfaces(JSON);
		final ServiceModel model = model("systemDiscovery", "/serviceregistry/system-discovery");
		final List<ServiceInstanceInterfaceRequestDTO> added = ServiceRegistryApplicationInitListener.extraInterfacesFor(model, nativeOf(model), extras);
		assertEquals(1, added.size());
		final ServiceInstanceInterfaceRequestDTO extra = added.get(0);
		assertEquals("generic_https", extra.templateName());
		assertEquals("https", extra.protocol());
		assertEquals("CERT_AUTH", extra.policy());
		assertEquals(List.of("authentication-registry-mtls"), extra.properties().get("accessAddresses"));
		assertEquals(8443, extra.properties().get("accessPort"));
		assertEquals("/serviceregistry/system-discovery", extra.properties().get("basePath"));
		assertTrue(((Map<?, ?>) extra.properties().get("operations")).containsKey("lookup"));
		// the native interface is not modified
		assertEquals(List.of("serviceregistry"), nativeOf(model).get(0).properties().get("accessAddresses"));
	}

	@Test
	public void derivesProtocolAndPolicyDefaultsAndIgnoresOtherServices() {
		final var extras = ServiceRegistryApplicationInitListener.parseExtraInterfaces(JSON);
		final ServiceModel gm = model("generalManagement", "/serviceregistry/general/mgmt");
		final ServiceInstanceInterfaceRequestDTO extra = ServiceRegistryApplicationInitListener.extraInterfacesFor(gm, nativeOf(gm), extras).get(0);
		assertEquals("https", extra.protocol());
		assertEquals("NONE", extra.policy());
		assertEquals(8501, extra.properties().get("accessPort"));
		final ServiceModel other = model("deviceDiscovery", "/serviceregistry/device-discovery");
		assertTrue(ServiceRegistryApplicationInitListener.extraInterfacesFor(other, nativeOf(other), extras).isEmpty());
	}

	@Test
	public void rejectsInvalidConfiguration() {
		assertThrows(IllegalArgumentException.class, () -> ServiceRegistryApplicationInitListener.parseExtraInterfaces("not json"));
		assertThrows(IllegalArgumentException.class, () -> ServiceRegistryApplicationInitListener.parseExtraInterfaces("[{\"serviceDefinition\":\"systemDiscovery\"}]"));
	}
}
