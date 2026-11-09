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
package eu.arrowhead.serviceregistry.init;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.init.ServiceInterfaceExtensions;
import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemRequestDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.ServiceRegistrySystemInfo;
import eu.arrowhead.serviceregistry.service.ServiceDiscoveryService;
import eu.arrowhead.serviceregistry.service.SystemDiscoveryService;

@Component
public class ServiceRegistryApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	private static final String INIT_ORIGIN = "INIT";

	@Autowired
	private SystemDiscoveryService sysdService;

	@Autowired
	private ServiceDiscoveryService sdService;

	@Autowired
	private ServiceInterfaceAddressPropertyProcessor serviceInterfaceAddressPropertyProcessor;

	@Value(ServiceInterfaceExtensions.$INTERFACE_POLICY_WD)
	private String interfacePolicyOverride;

	@Value(ServiceRegistryConstants.$EXTRA_INTERFACES_WD)
	private String extraInterfacesJson;

	private List<ExtraInterface> extraInterfaces;

	// An additional ingress of one of the ServiceRegistry's own services, e.g. a private mTLS gateway.
	record ExtraInterface(String serviceDefinition, String templateName, String protocol, String policy, String address, Integer port) {
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException {
		logger.debug("customInit started...");

		if (standaloneMode) {
			return;
		}

		extraInterfaces = parseExtraInterfaces(extraInterfacesJson);

		// revoke system (if any)
		sysdService.revokeSystem(sysInfo.getSystemName(), INIT_ORIGIN);

		// register system
		final SystemModel model = sysInfo.getSystemModel();
		final SystemRequestDTO dto = new SystemRequestDTO(sysInfo.getSystemName(), model.metadata(), model.version(), model.addresses(), model.deviceName());
		sysdService.registerSystem(dto, INIT_ORIGIN);

		// register services
		if (sysInfo.getServices() != null) {
			for (final ServiceModel serviceModel : sysInfo.getServices()) {
				registerService(serviceModel);
			}
		}

		// the native sysMonitor offer always requires the X.509 identity at the broker
		if (sysMonitorPublisher != null) {
			registerService(sysMonitorPublisher.getServiceModel(), ServiceInterfacePolicy.CERT_AUTH);
		}

		// Init service interface address type filter
		final ServiceRegistrySystemInfo srSysInfo = (ServiceRegistrySystemInfo) sysInfo;
		serviceInterfaceAddressPropertyProcessor.setAddressAliasNames(srSysInfo.getServiceAddressAliases());

		logger.info("System {} published {} service(s)", sysInfo.getSystemName(), registeredServices.size());
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customDestroy() {
		logger.debug("customDestroy started...");

		if (standaloneMode) {
			return;
		}

		try {
			for (final String serviceInstanceId : registeredServices) {
				sdService.revokeService(sysInfo.getSystemName(), serviceInstanceId, INIT_ORIGIN);
			}

			logger.info("Core system {} revoked {} service(s)", sysInfo, registeredServices.size());
			registeredServices.clear();
		} catch (final Throwable t) {
			logger.error(t.getMessage());
			logger.debug(t);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void registerService(final ServiceModel model) {
		// service.interface.policy overrides the framework default (CERT_AUTH behind TLS unless identities are declared)
		final ServiceInterfacePolicy policy = ServiceInterfaceExtensions.nativePolicy(interfacePolicyOverride, sysInfo);
		registerService(model, policy);
	}

	//-------------------------------------------------------------------------------------------------
	private void registerService(final ServiceModel model, final ServiceInterfacePolicy interfacePolicy) {
		logger.debug("registerService started...");

		final List<ServiceInstanceInterfaceRequestDTO> interfaces = model
				.interfaces()
				.stream()
				.map(i -> new ServiceInstanceInterfaceRequestDTO(i.templateName(), i.protocol(), interfacePolicy.name(), i.properties()))
				.collect(Collectors.toCollection(ArrayList::new));
		interfaces.addAll(extraInterfacesFor(model, interfaces));
		final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO(sysInfo.getSystemName(), model.serviceDefinition(), model.version(), null, model.metadata(), interfaces);
		final ServiceInstanceResponseDTO result = sdService.registerService(dto, INIT_ORIGIN);
		registeredServices.add(result.instanceId());
	}

	//-------------------------------------------------------------------------------------------------
	static List<ExtraInterface> parseExtraInterfaces(final String json) {
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
			throw new IllegalArgumentException("Invalid " + ServiceRegistryConstants.EXTRA_INTERFACES + ": " + ex.getMessage(), ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// The extra interface copies base path and operations of the service's first native interface and
	// replaces the access address and port.
	static List<ServiceInstanceInterfaceRequestDTO> extraInterfacesFor(final ServiceModel model, final List<ServiceInstanceInterfaceRequestDTO> nativeInterfaces, final List<ExtraInterface> extras) {
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
			final String policy = extra.policy() != null ? extra.policy() : ServiceInterfacePolicy.NONE.name();
			result.add(new ServiceInstanceInterfaceRequestDTO(extra.templateName(), protocol, policy, properties));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private List<ServiceInstanceInterfaceRequestDTO> extraInterfacesFor(final ServiceModel model, final List<ServiceInstanceInterfaceRequestDTO> nativeInterfaces) {
		return extraInterfacesFor(model, nativeInterfaces, extraInterfaces);
	}
}
