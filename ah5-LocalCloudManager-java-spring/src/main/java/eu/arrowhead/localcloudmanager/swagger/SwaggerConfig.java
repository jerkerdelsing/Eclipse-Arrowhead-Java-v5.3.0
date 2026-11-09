package eu.arrowhead.localcloudmanager.swagger;

import org.springframework.context.annotation.Configuration;

import eu.arrowhead.common.swagger.DefaultSwaggerConfig;

@Configuration
public class SwaggerConfig extends DefaultSwaggerConfig {

	public SwaggerConfig() {
		super("Arrowhead LocalCloudManager");
	}
}
