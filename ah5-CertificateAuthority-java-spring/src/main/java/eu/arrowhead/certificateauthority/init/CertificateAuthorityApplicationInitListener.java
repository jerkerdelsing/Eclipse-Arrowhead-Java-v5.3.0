/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.certificateauthority.service.CrlService;
import eu.arrowhead.common.init.ApplicationInitListener;

// Registration in the ServiceRegistry (with CERT_AUTH interfaces) and sysMonitor are done by the common listener.
// The CRL file is published once at startup so that a fresh deployment has a CRL before the first refresh period.
@Component
public class CertificateAuthorityApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	@Autowired
	private CrlService crlService;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		crlService.publishQuietly();
	}
}
