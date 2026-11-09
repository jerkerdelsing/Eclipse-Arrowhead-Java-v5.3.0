/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.common.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

// Deployment-specific additions to a system's own service registrations, applied at every start-up so that they
// survive restarts:
//   service.interface.policy  - policy of the native interfaces (e.g. NONE), instead of the framework default: CERT_AUTH
//                               for every system that verifies its requesters behind TLS (certificate, outsourced and
//                               internal authentication), NONE for declared identities and for systems without TLS
//   service.extra.interfaces  - JSON list of additional ingresses, e.g. a second mTLS address:
//     [{"serviceDefinition":"identity","templateName":"generic_https","protocol":"https","policy":"CERT_AUTH",
//       "address":"authentication-internal","port":8444}]
//     Base path and operations are copied from the service's first native interface.
public final class ServiceInterfaceExtensions {

	public static final String INTERFACE_POLICY = "service.interface.policy";
	public static final String $INTERFACE_POLICY_WD = "${" + INTERFACE_POLICY + ":}";
	public static final String EXTRA_INTERFACES = "service.extra.interfaces";
	public static final String $EXTRA_INTERFACES_WD = "${" + EXTRA_INTERFACES + ":[]}";

	public record ExtraInterface(String serviceDefinition, String templateName, String protocol, String policy, String address, Integer port) {
	}

	//-------------------------------------------------------------------------------------------------
	public static ServiceInterfacePolicy nativePolicy(final String override, final SystemInfo sysInfo) {
		if (!Utilities.isEmpty(override)) {
			return ServiceInterfacePolicy.valueOf(override.trim().toUpperCase());
		}

		return switch (sysInfo.getAuthenticationPolicy()) {
		case CERTIFICATE -> ServiceInterfacePolicy.CERT_AUTH;
		case DECLARED -> ServiceInterfacePolicy.NONE;
		case OUTSOURCED, INTERNAL -> sysInfo.isSslEnabled() ? ServiceInterfacePolicy.CERT_AUTH : ServiceInterfacePolicy.NONE;
		};
	}

	//-------------------------------------------------------------------------------------------------
	public static List<ExtraInterface> parse(final String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}

		try {
			final List<ExtraInterface> result = new ObjectMapper().readValue(json, new TypeReference<List<ExtraInterface>>() { });
			for (final ExtraInterface extra : result) {
				if (extra.serviceDefinition() == null || extra.templateName() == null || extra.address() == null || extra.port() == null) {
					throw new IllegalArgumentException("serviceDefinition, templateName, address and port are mandatory");
				}
			}

			return result;
		} catch (final Exception ex) {
			throw new IllegalArgumentException("Invalid " + EXTRA_INTERFACES + ": " + ex.getMessage(), ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public static List<ServiceInstanceInterfaceRequestDTO> extrasFor(final ServiceModel model, final List<ServiceInstanceInterfaceRequestDTO> nativeInterfaces, final List<ExtraInterface> extras) {
		if (extras == null || extras.isEmpty() || nativeInterfaces.isEmpty()) {
			return List.of();
		}

		final List<ServiceInstanceInterfaceRequestDTO> result = new ArrayList<>();
		for (final ExtraInterface extra : extras) {
			if (!extra.serviceDefinition().equals(model.serviceDefinition())) {
				continue;
			}

			final Map<String, Object> properties = new HashMap<>(nativeInterfaces.get(0).properties());
			properties.put("accessAddresses", List.of(extra.address()));
			properties.put("accessPort", extra.port());
			final String protocol = extra.protocol() != null ? extra.protocol() : (extra.templateName().endsWith("s") ? "https" : "http");
			final String policy = extra.policy() != null ? extra.policy() : "NONE";
			result.add(new ServiceInstanceInterfaceRequestDTO(extra.templateName(), protocol, policy, properties));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceInterfaceExtensions() {
		throw new UnsupportedOperationException();
	}
}
