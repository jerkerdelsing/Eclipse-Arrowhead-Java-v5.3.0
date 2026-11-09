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
package eu.arrowhead.authentication.method;

import java.util.Map;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;

public interface IAuthenticationMethodInputValidator {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public default void validateCredentials(final Map<String, String> credentials) throws InvalidParameterException, InternalServerError {
		// intentionally does nothing
	}
}
