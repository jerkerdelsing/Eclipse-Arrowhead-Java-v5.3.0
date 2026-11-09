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
package eu.arrowhead.authentication.method.password;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.authentication.method.IAuthenticationMethod;
import eu.arrowhead.authentication.method.IAuthenticationMethodDbService;
import eu.arrowhead.authentication.method.IAuthenticationMethodInputNormalizer;
import eu.arrowhead.authentication.method.IAuthenticationMethodInputValidator;
import eu.arrowhead.authentication.method.IAuthenticationMethodService;
import eu.arrowhead.dto.enums.AuthenticationMethod;

@Service
public class PasswordAuthenticationMethod implements IAuthenticationMethod {

	//=================================================================================================
	// members

	static final String KEY_PASSWORD = "password";

	@Autowired
	private PasswordAuthenticationMethodInputValidator validator;

	@Autowired
	private PasswordAuthenticationMethodInputNormalizer normalizer;

	@Autowired
	private PasswordAuthenticationMethodService service;

	@Autowired
	private PasswordAuthenticationMethodDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public IAuthenticationMethodInputValidator validator() {
		return validator;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public IAuthenticationMethodInputNormalizer normalizer() {
		return normalizer;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public IAuthenticationMethodService service() {
		return service;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public IAuthenticationMethodDbService dbService() {
		return dbService;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public AuthenticationMethod type() {
		return AuthenticationMethod.PASSWORD;
	}
}