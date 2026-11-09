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

import eu.arrowhead.dto.enums.AuthenticationMethod;

public interface IAuthenticationMethod {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthenticationMethod type(); // the supported type

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodInputValidator validator(); // to validate input

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodInputNormalizer normalizer(); // to normalize input

	//----------------------------------------------------------------------------- --------------------
	public IAuthenticationMethodService service(); // calling from the service layer

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethodDbService dbService(); // calling from the database service layer
}