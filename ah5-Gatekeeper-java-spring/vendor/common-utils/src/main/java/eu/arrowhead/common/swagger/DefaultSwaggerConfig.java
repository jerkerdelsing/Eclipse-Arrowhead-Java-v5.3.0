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
package eu.arrowhead.common.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import eu.arrowhead.common.Constants;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@SecurityScheme(name = "Authorization", type = SecuritySchemeType.HTTP, scheme = "bearer")
public abstract class DefaultSwaggerConfig implements WebMvcConfigurer {

	//=================================================================================================
	// members

	private final String systemName;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public DefaultSwaggerConfig(final String systemName) {
		this.systemName = systemName;
	}

	//-------------------------------------------------------------------------------------------------
	@Bean
	OpenAPI customOpenAPI() {
		return new OpenAPI().info(apiInfo());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	protected Info apiInfo() {
		return new Info()
				.title(systemName + " System API")
				.description("This page shows the REST interfaces offered by the " + systemName + " System.")
				.version(getVersion())
				.contact(apiContact())
				.license(apiLicence());
	}

	//-------------------------------------------------------------------------------------------------
	protected License apiLicence() {
		return new License()
				.name("Eclipse Public License 2.0")
				.url("http://www.eclipse.org/legal/epl-2.0")
				.identifier("EPL-2.0");
	}

	//-------------------------------------------------------------------------------------------------
	protected Contact apiContact() {
		return new Contact()
				.name("AITIA International Inc.")
				.email("iiot[at]aitia.ai")
				.url("https://www.aitia.ai");
	}

	//-------------------------------------------------------------------------------------------------
	protected String getVersion() {
		return Constants.AH_FRAMEWORK_VERSION;
	}
}