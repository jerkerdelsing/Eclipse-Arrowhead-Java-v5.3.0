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

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class CertificateAuthoritySystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return CertificateAuthorityConstants.SYSTEM_NAME;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION)
					.metadata("role", "certificate-authority")
					.metadata("security", "X.509/mTLS");

			if (AuthenticationPolicy.CERTIFICATE == getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	// sysMonitor is registered by the common-library SysMonitorPublisher when sysmonitor.enabled=true.
	@Override
	public List<ServiceModel> getServices() {
		final ServiceModel generalManagement = service(Constants.SERVICE_DEF_GENERAL_MANAGEMENT, interfaceBuilder(CertificateAuthorityConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(CertificateAuthorityConstants.SERVICE_OP_STATUS, operation(HttpMethod.GET, CertificateAuthorityConstants.HTTP_API_OP_STATUS_PATH))
				.operation(Constants.SERVICE_OP_GET_LOG, operation(HttpMethod.POST, Constants.HTTP_API_OP_LOGS_PATH))
				.operation(Constants.SERVICE_OP_GET_CONFIG, operation(HttpMethod.GET, Constants.HTTP_API_OP_GET_CONFIG_PATH))
				.build());

		final ServiceModel management = service(CertificateAuthorityConstants.SERVICE_DEF_CERTIFICATE_AUTHORITY_MANAGEMENT, base()
				.operation(CertificateAuthorityConstants.SERVICE_OP_LIST_CERTIFICATES, operation(HttpMethod.GET, CertificateAuthorityConstants.HTTP_API_OP_CERTIFICATES_PATH))
				.operation(CertificateAuthorityConstants.SERVICE_OP_REVOKE_CERTIFICATE, operation(HttpMethod.DELETE, CertificateAuthorityConstants.HTTP_API_OP_CERTIFICATE_PATH))
				.operation(CertificateAuthorityConstants.SERVICE_OP_REVOKE_CERTIFICATE_BY_CONTENT, operation(HttpMethod.POST, CertificateAuthorityConstants.HTTP_API_OP_REVOCATIONS_PATH))
				.operation(CertificateAuthorityConstants.SERVICE_OP_LIST_KEYS, operation(HttpMethod.GET, CertificateAuthorityConstants.HTTP_API_OP_KEYS_PATH))
				.operation(CertificateAuthorityConstants.SERVICE_OP_ADD_KEY, operation(HttpMethod.PUT, CertificateAuthorityConstants.HTTP_API_OP_KEYS_PATH))
				.operation(CertificateAuthorityConstants.SERVICE_OP_DELETE_KEY, operation(HttpMethod.DELETE, CertificateAuthorityConstants.HTTP_API_OP_KEY_PATH))
				.build());

		final ServiceModel signing = service(CertificateAuthorityConstants.SERVICE_DEF_CERTIFICATE_SIGNING, base()
				.operation(CertificateAuthorityConstants.SERVICE_OP_SIGN, operation(HttpMethod.POST, CertificateAuthorityConstants.HTTP_API_OP_SIGN_PATH))
				.build());

		final ServiceModel validation = service(CertificateAuthorityConstants.SERVICE_DEF_CERTIFICATE_VALIDATION, base()
				.operation(CertificateAuthorityConstants.SERVICE_OP_CHECK_CERTIFICATE, operation(HttpMethod.POST, CertificateAuthorityConstants.HTTP_API_OP_CHECK_CERTIFICATE_PATH))
				.operation(CertificateAuthorityConstants.SERVICE_OP_CHECK_TRUSTED_KEY, operation(HttpMethod.POST, CertificateAuthorityConstants.HTTP_API_OP_CHECK_TRUSTED_KEY_PATH))
				.operation(CertificateAuthorityConstants.SERVICE_OP_GET_CRL, operation(HttpMethod.GET, CertificateAuthorityConstants.HTTP_API_OP_CRL_PATH))
				.build());

		// starting with management services speeds up management filters
		return List.of(generalManagement, management, signing, validation);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY,
						Constants.NORMALIZATION_MODE,
						CertificateAuthorityConstants.CRL_DIR,
						CertificateAuthorityConstants.CRL_VALIDITY_HOURS,
						CertificateAuthorityConstants.CRL_REFRESH_SECONDS,
						CertificateAuthorityConstants.CERT_VALIDITY_DAYS,
						CertificateAuthorityConstants.ONBOARDING_CERT_VALIDITY_HOURS,
						CertificateAuthorityConstants.RESERVED_NAMES),
				CertificateAuthorityDefaults.class);
	}

	//-------------------------------------------------------------------------------------------------
	private ServiceModel service(final String definition, final HttpInterfaceModel intf) {
		return new ServiceModel.Builder()
				.serviceDefinition(definition)
				.version(CertificateAuthorityConstants.SERVICE_VERSION)
				.metadata("systemVersion", CertificateAuthorityConstants.SYSTEM_VERSION)
				.serviceInterface(intf)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private HttpInterfaceModel.Builder base() {
		return interfaceBuilder(CertificateAuthorityConstants.HTTP_API_BASE_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private HttpInterfaceModel.Builder interfaceBuilder(final String basePath) {
		final String templateName = getSslProperties().isSslEnabled()
				? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME
				: Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort()).basePath(basePath);
	}

	//-------------------------------------------------------------------------------------------------
	private HttpOperationModel operation(final HttpMethod method, final String path) {
		return new HttpOperationModel.Builder().method(method.name()).path(path).build();
	}
}
