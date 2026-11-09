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
package eu.arrowhead.common.service.validation.name;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class InterfaceTemplateNameValidator {

	//=================================================================================================
	// members

	// snake-case naming convention, only allowed characters are lower-case ASCII letters and numbers and underscore
	private static final String INTERFACE_TEMPLATE_NAME_REGEX_STRING = "([a-z]{1})|(^[a-z][0-9a-z_]*[0-9a-z]$)";
	private static final Pattern INTERFACE_TEMPLATE_NAME_REGEX_PATTERN = Pattern.compile(INTERFACE_TEMPLATE_NAME_REGEX_STRING);

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validateInterfaceTemplateName(final String name) {
		logger.debug("validateIntefaceTemplateName started: {}", name);

		if (Utilities.isEmpty(name)
				|| !INTERFACE_TEMPLATE_NAME_REGEX_PATTERN.matcher(name).matches()
				|| name.length() > Constants.INTERFACE_TEMPLATE_NAME_MAX_LENGTH) {
			throw new InvalidParameterException("The specified interface template name does not match the naming convention: " + name);
		}
	}
}