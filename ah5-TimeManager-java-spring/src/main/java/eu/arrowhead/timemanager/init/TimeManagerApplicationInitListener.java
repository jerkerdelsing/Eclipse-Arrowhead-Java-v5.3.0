/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.init;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.dto.ServiceInstanceCreateRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Component
public class TimeManagerApplicationInitListener extends ApplicationInitListener {

	@Value("${server.ssl.client-auth:none}")
	private String clientAuth;

	// The framework registers the interfaces of non-certificate authentication policies with policy NONE. When TLS
	// requires a client certificate, every consumer must present an X.509 identity, so the offers are re-registered
	// as CERT_AUTH. (sysMonitor is registered as CERT_AUTH by the common publisher.)
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		if (standaloneMode || !sysInfo.isSslEnabled() || !"need".equalsIgnoreCase(clientAuth)) {
			return;
		}

		for (final ServiceModel model : sysInfo.getServices()) {
			final List<ServiceInstanceInterfaceRequestDTO> interfaces = model.interfaces()
					.stream()
					.map(i -> new ServiceInstanceInterfaceRequestDTO(i.templateName(), i.protocol(), ServiceInterfacePolicy.CERT_AUTH.name(), i.properties()))
					.toList();
			final ServiceInstanceCreateRequestDTO payload = new ServiceInstanceCreateRequestDTO(model.serviceDefinition(), model.version(), null, model.metadata(), interfaces);
			final ServiceInstanceResponseDTO response = arrowheadHttpService.consumeService(
					Constants.SERVICE_DEF_SERVICE_DISCOVERY,
					Constants.SERVICE_OP_REGISTER,
					Constants.SYS_NAME_SERVICE_REGISTRY,
					ServiceInstanceResponseDTO.class,
					payload);
			registeredServices.add(response.instanceId());
		}

		logger.info("Re-registered {} service(s) with CERT_AUTH interfaces", sysInfo.getServices().size());
	}
}
