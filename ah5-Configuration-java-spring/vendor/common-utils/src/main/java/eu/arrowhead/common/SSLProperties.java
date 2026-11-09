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
package eu.arrowhead.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.exception.InvalidParameterException;
import jakarta.annotation.PostConstruct;

@Component
public class SSLProperties {

	//=================================================================================================
	// members

	@Value(Constants.$SERVER_SSL_ENABLED_WD)
	private boolean sslEnabled;

	@Value(Constants.$SERVER_SSL_KEY__STORE_TYPE_WD)
	private String keyStoreType;

	@Value(Constants.$SERVER_SSL_KEY__STORE_WD)
	private Resource keyStore;

	@Value(Constants.$SERVER_SSL_KEY__STORE__PASSWORD_WD)
	private String keyStorePassword;

	@Value(Constants.$SERVER_SSL_KEY__PASSWORD_WD)
	private String keyPassword;

	@Value(Constants.$SERVER_SSL_KEY__ALIAS_WD)
	private String keyAlias;

	@Value(Constants.$SERVER_SSL_TRUST__STORE_WD)
	private Resource trustStore;

	@Value(Constants.$SERVER_SSL_TRUST__STORE__PASSWORD_WD)
	private String trustStorePassword;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void validate() {
		if (sslEnabled) {
			if (Utilities.isEmpty(keyStoreType)) {
				throw new InvalidParameterException("keyStoreType is missing");
			}

			if (keyStore == null) {
				throw new InvalidParameterException("keyStore is missing");
			}

			if (Utilities.isEmpty(keyStorePassword)) {
				throw new InvalidParameterException("keyStorePassword is missing");
			}

			if (Utilities.isEmpty(keyPassword)) {
				throw new InvalidParameterException("keyPassword is missing");
			}

			if (Utilities.isEmpty(keyAlias)) {
				throw new InvalidParameterException("keyAlias is missing");
			}

			if (trustStore == null) {
				throw new InvalidParameterException("trustStore is missing");
			}

			if (Utilities.isEmpty(trustStorePassword)) {
				throw new InvalidParameterException("trustStorePassword is missing");
			}
		}
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public boolean isSslEnabled() {
		return sslEnabled;
	}

	//-------------------------------------------------------------------------------------------------
	public String getKeyStoreType() {
		return keyStoreType;
	}

	//-------------------------------------------------------------------------------------------------
	public Resource getKeyStore() {
		return keyStore;
	}

	//-------------------------------------------------------------------------------------------------
	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	//-------------------------------------------------------------------------------------------------
	public String getKeyPassword() {
		return keyPassword;
	}

	//-------------------------------------------------------------------------------------------------
	public String getKeyAlias() {
		return keyAlias;
	}

	//-------------------------------------------------------------------------------------------------
	public Resource getTrustStore() {
		return trustStore;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTrustStorePassword() {
		return trustStorePassword;
	}
}