/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.localcloudmanager.console;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.tomcat.TomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;

// A second, plain-HTTP Tomcat connector for the browser console, next to the mutual-TLS Arrowhead connector (server.port).
// console.port=0 disables it. Keep it on the loopback interface, or publish it only to the host's loopback interface. In a
// container it listens on every interface (Docker forwards a published port to any of the container's networks); ConsoleFilter
// then answers only clients allowed by ConsoleClients, so other systems on the cloud network cannot use it (J6).
@Configuration
public class ConsoleConnectorConfig implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

	//=================================================================================================
	// members

	@Value(LocalCloudManagerConstants.$CONSOLE_PORT_WD)
	private int port;

	@Value(LocalCloudManagerConstants.$CONSOLE_ADDRESS_WD)
	private String address;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void customize(final TomcatServletWebServerFactory factory) {
		if (port <= 0) {
			return;
		}

		final Connector connector = new Connector(TomcatWebServerFactory.DEFAULT_PROTOCOL);
		connector.setScheme("http");
		connector.setSecure(false);
		connector.setPort(port);
		connector.setProperty("address", address);
		factory.addAdditionalConnectors(connector);
	}

}
