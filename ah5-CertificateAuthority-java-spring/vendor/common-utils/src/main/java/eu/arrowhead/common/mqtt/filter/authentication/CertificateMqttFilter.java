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
package eu.arrowhead.common.mqtt.filter.authentication;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.security.CertificateProfileType;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.security.SecurityUtilities.CommonNameAndType;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.annotation.Resource;

public class CertificateMqttFilter implements ArrowheadMqttFilter {

	//=================================================================================================
	// members

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	private Map<String, Object> arrowheadContext;

	@Autowired
	private ApplicationContext appContext;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private SSLProperties sslProperties;

	private PublicKey cloudPublicKey;

	private static final String beginCert = "-----BEGIN CERTIFICATE-----";
	private static final String endCert = "-----END CERTIFICATE-----";
	private static final String whitespaceRegexp = "\\s+";

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public int order() {
		return Constants.REQUEST_FILTER_ORDER_AUTHENTICATION;
	}

	//---------------------------------------------------------------------------------------------
	@Override
	public void doFilter(final String authKey, final MqttRequestModel request) {
		logger.debug("Checking access in CertificateMqttFilter...");

		final X509Certificate x509Certificate = decodeAuthorizationKey(authKey);
		checkCertificate(x509Certificate);
		final CommonNameAndType requesterData = SecurityUtilities.getIdentificationDataFromCertificate(x509Certificate);
		if (requesterData == null) {
			logger.error("Unauthenticated access attempt: {}", request.getBaseTopic());
			throw new AuthException("Unauthenticated access attempt: " + request.getBaseTopic());
		}

		checkClientAuthorized(requesterData, request.getBaseTopic() + request.getOperation());
		fillRequestAttributes(request, requesterData);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@EventListener(ApplicationReadyEvent.class)
	private void init() throws ConfigurationException {
		logger.debug("CertificateMqttFilter.init started...");

		// Authentication policy cannot be 'CERTIFICATE' while SSL is disabled
		final String serverCN = (String) arrowheadContext.get(Constants.SERVER_COMMON_NAME);
		final String cloudCN = SecurityUtilities.getCloudCN(serverCN);
		KeyStore trustStore;
		try {
			trustStore = KeyStore.getInstance(sslProperties.getKeyStoreType());
			trustStore.load(sslProperties.getTrustStore().getInputStream(), sslProperties.getTrustStorePassword().toCharArray());
			cloudPublicKey = SecurityUtilities.getPublicKeyFromTrustStore(trustStore, cloudCN);
		} catch (final KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);

			throw new ConfigurationException("Unable to locate cloud's public key");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private X509Certificate decodeAuthorizationKey(final String authKey) {
		logger.debug("CertificateMqttFilter.decodeAuthorizationKey started...");

		if (Utilities.isEmpty(authKey)) {
			throw new AuthException("No authentication key has been provided");
		}

		try {
			// Base64 encoded X.509 certificate PEM format is expected
			String decodedX509PEM = new String(Base64.getDecoder().decode(authKey));
			decodedX509PEM = decodedX509PEM.replace(beginCert, "").replace(endCert, "").replaceAll(whitespaceRegexp, "");

			final byte[] decodedX509RawContent = Base64.getDecoder().decode(decodedX509PEM);

			final CertificateFactory certificateFactory = CertificateFactory.getInstance(Constants.X_509);
			final ByteArrayInputStream certStream = new ByteArrayInputStream(decodedX509RawContent);

			return (X509Certificate) certificateFactory.generateCertificate(certStream);
		} catch (final IllegalArgumentException | CertificateException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new AuthException("Invalid authentication key has been provided");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void checkCertificate(final X509Certificate cert) {
		try {
			cert.checkValidity();
			cert.verify(cloudPublicKey);
		} catch (final CertificateException | InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new AuthException("Invalid authentication key has been provided");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void checkClientAuthorized(final CommonNameAndType requesterData, final String requestTarget) {
		logger.debug("CertificateMqttFilter.checkClientAuthenticated started...");

		if (CertificateProfileType.SYSTEM != requesterData.profileType() && CertificateProfileType.OPERATOR != requesterData.profileType()) {
			logger.error("Unauthorized access: {}, invalid certificate type: {}", requestTarget, requesterData.profileType());
			throw new ForbiddenException("Unauthorized access: " + requestTarget + ", invalid certificate type: " + requesterData.profileType());
		}

		final String serverCN = (String) arrowheadContext.get(Constants.SERVER_COMMON_NAME);
		final String cloudCN = SecurityUtilities.getCloudCN(serverCN);
		if (!SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, requesterData.commonName(), cloudCN)) {
			logger.error("Unauthorized access: {}, from foreign cloud", requestTarget);
			throw new ForbiddenException("Unauthorized access: " + requestTarget + ", from foreign cloud");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void fillRequestAttributes(final MqttRequestModel request, final CommonNameAndType requesterData) {
		logger.debug("CertificateMqttFilter.checkClientAuthenticated started...");

		final String clientName = systemNameNormalizer.normalize(SecurityUtilities.getClientNameFromClientCN(requesterData.commonName()));

		request.setRequester(clientName);
		request.setSysOp(CertificateProfileType.OPERATOR == requesterData.profileType());
	}
}