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
package eu.arrowhead.authorization.init;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.service.utils.SecretCryptographer;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.init.ApplicationInitListener;

@Component
public class AuthorizationApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException {
		final AuthorizationSystemInfo authSysInfo = (AuthorizationSystemInfo) sysInfo;

		if (Utilities.isEmpty(authSysInfo.getSecretCryptographerKey())) {
			throw new InvalidParameterException("secret.cryptographer.key property is empty");
		}

		final byte[] cryptoKeyBytes = authSysInfo.getSecretCryptographerKey().getBytes();
		if (cryptoKeyBytes.length < SecretCryptographer.AES_KEY_MIN_SIZE) {
			throw new InvalidParameterException("secret.cryptographer.key value must be minimum " + SecretCryptographer.AES_KEY_MIN_SIZE + " bytes long");
		}
	}
}