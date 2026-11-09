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
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import eu.arrowhead.authentication.method.password.PasswordAuthenticationMethod;
import eu.arrowhead.dto.enums.AuthenticationMethod;
import jakarta.annotation.PostConstruct;

@Service
public class AuthenticationMethods {

	//=================================================================================================
	// members

	private final Map<AuthenticationMethod, IAuthenticationMethod> methods = new ConcurrentHashMap<>();

	@Autowired
	private ApplicationContext appContext;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IAuthenticationMethod method(final AuthenticationMethod type) {
		return methods.get(type);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		methods.put(AuthenticationMethod.PASSWORD, appContext.getBean(PasswordAuthenticationMethod.class));
	}
}