/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.init;

import org.springframework.stereotype.Component;

import eu.arrowhead.common.init.ApplicationInitListener;

// Registration in the ServiceRegistry (with CERT_AUTH interfaces) and sysMonitor are done by the common listener.
@Component
public class SoSConfiguratorApplicationInitListener extends ApplicationInitListener {
}
