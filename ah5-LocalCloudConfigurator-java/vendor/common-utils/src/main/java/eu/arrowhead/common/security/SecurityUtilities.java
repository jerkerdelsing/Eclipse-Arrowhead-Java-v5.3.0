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
package eu.arrowhead.common.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.util.UriUtils;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

public final class SecurityUtilities {

	//=================================================================================================
	// members

	private static final String COMMON_NAME_FIELD_NAME = "CN";
	private static final String DN_QUALIFIER_FIELD_NAME = "2.5.4.46";
	private static final String X509_CN_DELIMITER = "\\.";

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private static final Logger logger = LogManager.getLogger(SecurityUtilities.class);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static X509Certificate getCertificateFromKeyStore(final KeyStore keystore, final String alias) {
		Assert.notNull(keystore, "Key store is not defined");
		Assert.isTrue(!Utilities.isEmpty(alias), "Alias is not defined");

		try {
			final Certificate[] chain = keystore.getCertificateChain(alias);

			return chain == null ? null : (X509Certificate) chain[0];
		} catch (final KeyStoreException ex) {
			logger.error("Accessing certificate from key store failed...", ex);
			throw new ServiceConfigurationError("Accessing certificate from key store failed...", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static PrivateKey getPrivateKeyFromKeyStore(final KeyStore keystore, final String alias, final String keyPass) {
		Assert.notNull(keystore, "Key store is not defined");
		Assert.isTrue(!Utilities.isEmpty(alias), "Alias is not defined");
		Assert.notNull(keyPass, "Password is not defined");

		try {
			return (PrivateKey) keystore.getKey(alias, keyPass.toCharArray());
		} catch (final KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException ex) {
			logger.error("Getting the private key from key store failed...", ex);
			throw new ServiceConfigurationError("Getting the private key from key store failed...", ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static PublicKey getPublicKeyFromTrustStore(final KeyStore trustStore, final String cn) {
		Assert.notNull(trustStore, "Key store is not defined");
		Assert.isTrue(!Utilities.isEmpty(cn), "CN is not defined");

		try {
			for (final String alias : Collections.list(trustStore.aliases())) {
				final Certificate certificate = trustStore.getCertificate(alias);
				if (certificate != null) {
					final X509Certificate cert = (X509Certificate) certificate;
					final String commonName = getCommonNameFromSubjectDN(cert.getSubjectX500Principal().getName(X500Principal.RFC2253));
					if (commonName.equals(cn)) {
						return cert.getPublicKey();
					}
				}
			}
		} catch (final KeyStoreException ex) {
			logger.error("Accessing certificate from key store failed...", ex);
			throw new ServiceConfigurationError("Accessing certificate from key store failed...", ex);
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String getCommonNameFromSubjectDN(final String dn) {
		if (Utilities.isEmpty(dn)) {
			return null;
		}

		try {
			// DN is in LDAP format, we can use the LdapName object for parsing
			final LdapName ldapname = new LdapName(dn);
			for (final Rdn rdn : ldapname.getRdns()) {
				// Find the data after the CN and DN_QUALIFIER fields
				if (COMMON_NAME_FIELD_NAME.equalsIgnoreCase(rdn.getType())) {
					return (String) rdn.getValue();
				}
			}

		} catch (final InvalidNameException ex) {
			logger.warn("InvalidNameException in getCommonNameFromSubjectDN: {}", ex.getMessage());
			logger.debug("Exception", ex);
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static CommonNameAndType getIdentificationDataFromSubjectDN(final String dn) {
		if (Utilities.isEmpty(dn)) {
			return null;
		}

		String commonName = null;
		final List<String> dnQualifiers = new ArrayList<>();
		try {
			// DN is in LDAP format, we can use the LdapName object for parsing
			final LdapName ldapname = new LdapName(dn);
			for (final Rdn rdn : ldapname.getRdns()) {
				// Find the data after the CN and DN_QUALIFIER fields
				if (COMMON_NAME_FIELD_NAME.equalsIgnoreCase(rdn.getType())) {
					commonName = (String) rdn.getValue();
				} else if (DN_QUALIFIER_FIELD_NAME.equalsIgnoreCase(rdn.getType())) {
					final String dnQualifier = new String((byte[]) rdn.getValue(), StandardCharsets.UTF_8);
					dnQualifiers.add(dnQualifier.trim()); // important, there were white spaces in the test
				}
			}

			if (commonName != null) {
				for (final String qualifier : dnQualifiers) {
					final CertificateProfileType type = CertificateProfileType.fromCode(qualifier);
					if (type != null) {
						return new CommonNameAndType(commonName, type);
					}
				}
			}
		} catch (final InvalidNameException ex) {
			logger.warn("InvalidNameException in getIdentificationDataFromSubjectDN: {}", ex.getMessage());
			logger.debug("Exception", ex);
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static CommonNameAndType getIdentificationDataFromRequest(final HttpServletRequest request) {
		Assert.notNull(request, "request must not be null");
		final X509Certificate[] certificates = (X509Certificate[]) request.getAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE);
		if (certificates != null && certificates.length > 0) {
			for (final X509Certificate cert : certificates) {
				final CommonNameAndType requesterData = getIdentificationDataFromSubjectDN(cert.getSubjectX500Principal().getName(X500Principal.RFC2253));
				if (requesterData == null || !isValidSystemCommonName(requesterData.commonName())) {
					continue;
				}

				return requesterData;
			}
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static CommonNameAndType getIdentificationDataFromCertificate(final X509Certificate certificate) {
		Assert.notNull(certificate, "certificate must not be null");
		final CommonNameAndType requesterData = getIdentificationDataFromSubjectDN(certificate.getSubjectX500Principal().getName(X500Principal.RFC2253));

		if (requesterData == null || !isValidSystemCommonName(requesterData.commonName())) {
			return null;
		}

		return requesterData;
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isValidSystemCommonName(final String commonName) {
		if (Utilities.isEmpty(commonName)) {
			return false;
		}

		final String[] cnFields = commonName.split(X509_CN_DELIMITER, 0);
		return cnFields.length == Constants.SYSTEM_CERT_CN_LENGTH;
	}

	//-------------------------------------------------------------------------------------------------
	public static String getCloudCN(final String clientCn) {
		final String[] fields = clientCn.split(X509_CN_DELIMITER, 2); // fields contains: clientName, <cloudName>.<organization>.<two parts of the master certificate, eg. arrowhead.eu>
		Assert.isTrue(fields.length >= 2, "Client common name is invalid: " + clientCn);

		return fields[1];
	}

	//-------------------------------------------------------------------------------------------------
	@Nullable
	public static String getClientNameFromClientCN(final String clientCN) {
		if (clientCN == null) {
			return null;
		}

		return clientCN.split(X509_CN_DELIMITER, 2)[0];
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isClientInTheLocalCloudByCNs(final ApplicationContext appContext, final String clientCN, final String cloudCN) {
		if (appContext == null || Utilities.isEmpty(clientCN) || Utilities.isEmpty(cloudCN)) {
			return false;
		}

		final CloudIdentifierNormalizer cloudIdentifierNormalizer = appContext.getBean(CloudIdentifierNormalizer.class);
		if (cloudIdentifierNormalizer == null) {
			return false;
		}

		String[] fields = clientCN.split(X509_CN_DELIMITER); // valid clientCN contains <clientName>.<cloudName>.<organization>.<two parts of the master certificate, eg. arrowhead.eu>
		if (fields.length != Constants.SYSTEM_CERT_CN_LENGTH) {
			return false;
		}
		final String clientCloudId = cloudIdentifierNormalizer.normalize(fields[1] + Constants.COMPOSITE_ID_DELIMITER + fields[2]);

		fields = cloudCN.split(X509_CN_DELIMITER); // valid cloudCN contains <cloudName>.<organization>.<two parts of the master certificate, eg. arrowhead.eu>
		if (fields.length != Constants.CLOUD_CERT_CN_LENGTH) {
			return false;
		}
		final String cloudId = cloudIdentifierNormalizer.normalize(fields[0] + Constants.COMPOSITE_ID_DELIMITER + fields[1]);

		return cloudId.equals(clientCloudId);
	}

	//-------------------------------------------------------------------------------------------------
	public static String hashWithSecretKey(final String data, final String secretKey) throws NoSuchAlgorithmException, InvalidKeyException {
		Assert.isTrue(!Utilities.isEmpty(data), "data is missing");
		Assert.isTrue(!Utilities.isEmpty(secretKey), "secretKey is missing");

		final SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), HMAC_ALGORITHM);
		final Mac mac = Mac.getInstance(HMAC_ALGORITHM);
		mac.init(keySpec);

		return Utilities.bytesToHex(mac.doFinal(data.getBytes()));
	}

	//-------------------------------------------------------------------------------------------------
	public static String getDecodedUri(final String rawUri) {
		if (Utilities.isEmpty(rawUri)) {
			return rawUri;
		}

		return UriUtils.decode(rawUri.trim(), StandardCharsets.UTF_8);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SecurityUtilities() {
		throw new UnsupportedOperationException();
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	public record CommonNameAndType(String commonName, CertificateProfileType profileType) {
	}
}