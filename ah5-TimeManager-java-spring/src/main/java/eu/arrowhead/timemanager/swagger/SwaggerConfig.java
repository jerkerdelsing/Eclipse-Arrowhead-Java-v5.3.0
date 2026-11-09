/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.swagger;

import org.springframework.context.annotation.Configuration;

import eu.arrowhead.common.swagger.DefaultSwaggerConfig;

@Configuration
public class SwaggerConfig extends DefaultSwaggerConfig {

	public SwaggerConfig() {
		super("Arrowhead TimeManager");
	}
}
