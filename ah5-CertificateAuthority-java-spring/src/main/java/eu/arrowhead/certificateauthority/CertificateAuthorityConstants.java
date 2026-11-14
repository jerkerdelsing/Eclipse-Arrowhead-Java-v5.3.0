/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority;

import java.util.Map;
import java.util.Set;

public final class CertificateAuthorityConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "CertificateAuthority";
	public static final String SYSTEM_VERSION = "5.4.0";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.certificateauthority.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.certificateauthority.jpa.repository";

	// service definitions and operations (unchanged from the native 5.2.2 implementation and the SD/IDD documents)
	public static final String SERVICE_DEF_CERTIFICATE_SIGNING = "certificateSigning";
	public static final String SERVICE_DEF_CERTIFICATE_VALIDATION = "certificateValidation";
	public static final String SERVICE_DEF_CERTIFICATE_AUTHORITY_MANAGEMENT = "certificateAuthorityManagement";
	public static final String SERVICE_VERSION = "1.0.0";

	public static final String SERVICE_OP_SIGN = "sign";
	public static final String SERVICE_OP_CHECK_CERTIFICATE = "check-certificate";
	public static final String SERVICE_OP_CHECK_TRUSTED_KEY = "check-trusted-key";
	public static final String SERVICE_OP_GET_CRL = "get-crl";
	public static final String SERVICE_OP_LIST_CERTIFICATES = "list-certificates";
	public static final String SERVICE_OP_REVOKE_CERTIFICATE = "revoke-certificate";
	public static final String SERVICE_OP_REVOKE_CERTIFICATE_BY_CONTENT = "revoke-certificate-by-content";
	public static final String SERVICE_OP_LIST_KEYS = "list-keys";
	public static final String SERVICE_OP_ADD_KEY = "add-key";
	public static final String SERVICE_OP_DELETE_KEY = "delete-key";
	public static final String SERVICE_OP_STATUS = "status";

	// HTTP paths; every operation is also served without the base path, as the native implementation did
	public static final String HTTP_API_BASE_PATH = "/certificate-authority";
	public static final String HTTP_API_OP_SIGN_PATH = "/sign";
	public static final String HTTP_API_OP_CHECK_CERTIFICATE_PATH = "/checkCertificate";
	public static final String HTTP_API_OP_CHECK_TRUSTED_KEY_PATH = "/checkTrustedKey";
	public static final String HTTP_API_OP_CRL_PATH = "/crl";
	public static final String HTTP_API_OP_CERTIFICATES_PATH = "/mgmt/certificates";
	public static final String HTTP_API_OP_CERTIFICATE_PATH = "/mgmt/certificates/{id}";
	public static final String HTTP_API_OP_REVOCATIONS_PATH = "/mgmt/revocations";
	public static final String HTTP_API_OP_KEYS_PATH = "/mgmt/keys";
	public static final String HTTP_API_OP_KEY_PATH = "/mgmt/keys/{id}";
	public static final String HTTP_API_OP_HEALTH_PATH = "/health";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt/general";
	public static final String HTTP_API_OP_STATUS_PATH = "/status";

	// identities (J6: LocalCloudManager neither signs certificates nor manages the CA)
	public static final String SYSOP = "Sysop";
	public static final String ONBOARDING = "Onboarding";

	// caller -> certificate profiles it may request
	public static final Map<String, Set<String>> PROFILES = Map.of(
			ONBOARDING, Set.of("on", "de", "sy"),
			SYSOP, Set.of("on", "de", "sy", "op"),
			SYSTEM_NAME, Set.of("sy"));

	public static final Map<String, String> PROFILE_NAMES = Map.of("on", "ON_BOARDING", "de", "DEVICE", "sy", "SYSTEM", "op", "OPERATOR");

	// callers of the certificateAuthorityManagement operations and the status operation
	public static final Set<String> MANAGERS = Set.of(SYSOP, SYSTEM_NAME);

	// certificate profiles (DN qualifier) that may call any service
	public static final Set<String> CALLER_PROFILES = Set.of("sy", "op");

	// configuration
	public static final String CA_CERTIFICATE_FILE = "ca.certificate.file";
	public static final String $CA_CERTIFICATE_FILE_WD = "${" + CA_CERTIFICATE_FILE + ":" + CertificateAuthorityDefaults.CA_CERTIFICATE_FILE_DEFAULT + "}";
	public static final String CA_KEY_FILE = "ca.key.file";
	public static final String $CA_KEY_FILE_WD = "${" + CA_KEY_FILE + ":" + CertificateAuthorityDefaults.CA_KEY_FILE_DEFAULT + "}";
	public static final String CRL_DIR = "crl.dir";
	public static final String $CRL_DIR_WD = "${" + CRL_DIR + ":" + CertificateAuthorityDefaults.CRL_DIR_DEFAULT + "}";
	public static final String CRL_VALIDITY_HOURS = "crl.validity.hours";
	public static final String $CRL_VALIDITY_HOURS_WD = "${" + CRL_VALIDITY_HOURS + ":" + CertificateAuthorityDefaults.CRL_VALIDITY_HOURS_DEFAULT + "}";
	public static final String CRL_REFRESH_SECONDS = "crl.refresh.seconds";
	public static final String $CRL_REFRESH_MILLIS_WD = "#{${" + CRL_REFRESH_SECONDS + ":" + CertificateAuthorityDefaults.CRL_REFRESH_SECONDS_DEFAULT + "} * 1000}";
	public static final String CERT_VALIDITY_DAYS = "cert.validity.days";
	public static final String $CERT_VALIDITY_DAYS_WD = "${" + CERT_VALIDITY_DAYS + ":" + CertificateAuthorityDefaults.CERT_VALIDITY_DAYS_DEFAULT + "}";
	public static final String ONBOARDING_CERT_VALIDITY_HOURS = "onboarding.cert.validity.hours";
	public static final String $ONBOARDING_CERT_VALIDITY_HOURS_WD = "${" + ONBOARDING_CERT_VALIDITY_HOURS + ":" + CertificateAuthorityDefaults.ONBOARDING_CERT_VALIDITY_HOURS_DEFAULT + "}";
	public static final String RESERVED_NAMES = "reserved.names";
	public static final String $RESERVED_NAMES_WD = "${" + RESERVED_NAMES + ":" + CertificateAuthorityDefaults.RESERVED_NAMES_DEFAULT + "}";

	public static final int REQUEST_FILTER_ORDER_CALLER_PROFILE = 16;
	public static final int MAX_ADDRESSES = 16;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private CertificateAuthorityConstants() {
		throw new UnsupportedOperationException();
	}
}
