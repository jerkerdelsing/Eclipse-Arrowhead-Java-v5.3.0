/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.common.init;

import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

// Spring Boot 4 serialises HTTP bodies with Jackson 3 by default. The Arrowhead code and its DTOs use Jackson 2
// (com.fasterxml ObjectMapper, injected from the spring-boot-jackson2 auto-configuration), and the systems of the
// other implementations expect its wire format, so Jackson 2 stays the mapper for the MVC converters and the WebFlux
// codecs. These are defaults: a configuration file or the environment can still override them.
public class Jackson2Defaults implements EnvironmentPostProcessor {

	//=================================================================================================
	// members

	public static final String SOURCE_NAME = "arrowheadJackson2Defaults";

	public static final Map<String, Object> DEFAULTS = Map.of(
			"spring.http.converters.preferred-json-mapper", "jackson2",
			"spring.http.codecs.preferred-json-mapper", "jackson2");

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
		if (!environment.getPropertySources().contains(SOURCE_NAME)) {
			environment.getPropertySources().addLast(new MapPropertySource(SOURCE_NAME, DEFAULTS));
		}
	}
}
