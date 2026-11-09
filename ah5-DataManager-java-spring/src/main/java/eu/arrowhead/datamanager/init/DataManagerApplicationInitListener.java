/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.init;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.init.ApplicationInitListener;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.datamanager.DataManagerConstants;
import eu.arrowhead.dto.ServiceInstanceCreateRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@Component
public class DataManagerApplicationInitListener extends ApplicationInitListener {

	@Value(DataManagerConstants.$SERVICE_AUTHORIZATION_ENABLED_WD)
	private boolean serviceAuthorizationEnabled;

	// The framework registers interfaces of outsourced-authentication systems with policy NONE. With mutual TLS and
	// the certificate-bound, ConsumerAuthorization-checked filter in place, consumers must present the registered
	// X.509 identity, so the offers are re-registered as CERT_AUTH.
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		if (standaloneMode || !sysInfo.isSslEnabled() || !serviceAuthorizationEnabled) {
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
