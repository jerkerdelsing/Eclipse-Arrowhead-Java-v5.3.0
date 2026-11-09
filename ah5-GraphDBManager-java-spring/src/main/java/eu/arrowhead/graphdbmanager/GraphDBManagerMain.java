/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import eu.arrowhead.common.Constants;

@SpringBootApplication
@ComponentScan(Constants.BASE_PACKAGE)
public class GraphDBManagerMain {

	public static void main(final String[] args) {
		SpringApplication.run(GraphDBManagerMain.class, args);
	}

	protected GraphDBManagerMain() {
	}
}
