/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.init;

import java.security.cert.X509Certificate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.gatetunnel.security.TlsSupport;
import eu.arrowhead.gatetunnel.service.FederationSettings;
import eu.arrowhead.gatetunnel.service.GateTunnelStatusService;
import eu.arrowhead.common.init.ApplicationInitListener;

@Component
public class GateTunnelApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	@Autowired
	private FederationSettings settings;

	@Autowired
	private GateTunnelStatusService statusService;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// The system certificate must be GateTunnel.<cloud>.<organisation>.arrowhead.eu with the system profile (as in 5.2.2):
	// the inter-cloud peers pin that name.
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		logger.debug("customInit started...");

		if (sysInfo.isSslEnabled()) {
			final X509Certificate certificate = (X509Certificate) arrowheadContext.get(Constants.SERVER_CERTIFICATE);
			if (certificate == null || !TlsSupport.cn(certificate).equals(settings.ownCommonName()) || !"sy".equals(TlsSupport.attribute(certificate, "DNQ"))) {
				throw new IllegalStateException("Configured certificate does not match the service identity/profile " + settings.ownCommonName());
			}
		}

		statusService.setRegisteredInstances(() -> List.copyOf(registeredServices));
		logger.info("GateTunnel federation profile {}, relay {}:{}", settings.getProfile(), settings.getRelayHost(), settings.getRelayPort());
	}
}
