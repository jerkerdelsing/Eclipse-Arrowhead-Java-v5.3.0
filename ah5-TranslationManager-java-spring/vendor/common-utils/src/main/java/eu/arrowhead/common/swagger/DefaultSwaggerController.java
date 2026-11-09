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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import eu.arrowhead.common.Constants;

@Controller
public class DefaultSwaggerController {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	/*
	 * Necessary controller due to Swagger UI default path is hard coded and can't
	 * be configured.
	 */
	@GetMapping(path = "/")
	public String redirectDefaultSwaggerUI() {
		return "redirect:" + Constants.SWAGGER_UI_INDEX_HTML;
	}
}