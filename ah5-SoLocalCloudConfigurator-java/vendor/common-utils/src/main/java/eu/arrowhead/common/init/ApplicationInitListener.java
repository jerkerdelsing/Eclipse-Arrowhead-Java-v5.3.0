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
package eu.arrowhead.common.init;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.ConfigurationException;
import javax.security.auth.x500.X500Principal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.mqtt.MqttController;
import eu.arrowhead.common.sysmonitor.SysMonitorPublisher;
import eu.arrowhead.common.security.CertificateProfileType;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.security.SecurityUtilities.CommonNameAndType;
import eu.arrowhead.common.quartz.OutsourcedLoginJob;
import eu.arrowhead.dto.IdentityRequestDTO;
import eu.arrowhead.dto.ServiceInstanceCreateRequestDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemRegisterRequestDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;

public abstract class ApplicationInitListener {

	//=================================================================================================
	// members

	private static final int MAX_NUMBER_OF_SERVICEREGISTRY_CONNECTION_RETRIES = 3;
	private static final int WAITING_PERIOD_BETWEEN_RETRIES_IN_SECONDS = 15;

	protected final Logger logger = LogManager.getLogger(getClass());

	@Resource(name = Constants.ARROWHEAD_CONTEXT)
	protected Map<String, Object> arrowheadContext;

	@Autowired
	protected SystemInfo sysInfo;

	@Autowired
	protected ServiceCollector serviceCollector;

	@Autowired
	protected ArrowheadHttpService arrowheadHttpService;

	@Autowired(required = false)
	protected MqttController mqttController;

	@Autowired(required = false)
	protected SysMonitorPublisher sysMonitorPublisher;

	@Autowired(required = false)
	private OutsourcedLoginJob outsourcedLoginJob;

	protected boolean standaloneMode = false;

	protected Set<String> registeredServices = new HashSet<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@EventListener
	@Order(10)
	public void onApplicationEvent(final ContextRefreshedEvent event) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InterruptedException, ConfigurationException {
		logger.debug("Initialization in onApplicationEvent()...");

		logger.info("System name: {}", sysInfo.getSystemName());
		logger.info("System port: {}", sysInfo.getServerPort());
		logger.info("SSL mode: {}", getSSLString());
		logger.info("Authentication policy: {}", sysInfo.getAuthenticationPolicy().name());

		validateServerConfiguration();

		if (arrowheadContext.containsKey(Constants.SERVER_STANDALONE_MODE)) {
			standaloneMode = (boolean) arrowheadContext.get(Constants.SERVER_STANDALONE_MODE);
		}

		if (sysInfo.isSslEnabled()) {
			checkSSLProperties();
			final KeyStore keyStore = initializeKeyStore();
			obtainKeys(keyStore);
			if (sysInfo.getAuthenticationPolicy() == AuthenticationPolicy.CERTIFICATE) {
				// in this case the certificate must be compliant with the Arrowhead Certificate structure
				checkServerCertificate(keyStore);
			}
		}

		registerToServiceRegistry();

		if (sysInfo.isMqttApiEnabled()) {
			subscribeToMqttServiceTopics();
		}

		customInit(event);

		logger.debug("Initialization in onApplicationEvent() is done.");
	}

	//-------------------------------------------------------------------------------------------------
	@PreDestroy
	public void destroy() throws InterruptedException {
		logger.debug("destroy called...");

		revokeServices();

		try {
			customDestroy();
		} catch (final Throwable t) {
			logger.error(t.getMessage());
			logger.debug(t);
		}

		try {
			if (sysInfo.isMqttApiEnabled()) {
				mqttController.disconnect();
			}
		} catch (final Throwable t) {
			logger.error(t.getMessage());
			logger.debug(t);
		}

		try {
			// logout attempt
			if (AuthenticationPolicy.OUTSOURCED == sysInfo.getAuthenticationPolicy()) {
				arrowheadHttpService.consumeService(Constants.SERVICE_DEF_IDENTITY, Constants.SERVICE_OP_IDENTITY_LOGOUT, Void.TYPE, getLogoutPayload());
			}
		} catch (final Throwable t) {
			logger.error(t.getMessage());
			logger.debug(t);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException, ConfigurationException {
	}

	//-------------------------------------------------------------------------------------------------
	protected void customDestroy() {
	}

	//-------------------------------------------------------------------------------------------------
	protected String getSSLString() {
		return sysInfo.isSslEnabled() ? "ENABLED" : "DISABLED";
	}

	//-------------------------------------------------------------------------------------------------
	private void validateServerConfiguration() throws ConfigurationException {
		logger.debug("validateServerConfiguration started...");

		if (!sysInfo.isSslEnabled() && sysInfo.getAuthenticationPolicy() == AuthenticationPolicy.CERTIFICATE) {
			throw new ConfigurationException("Authentication policy cannot be 'CERTIFICATE' while SSL is disabled");
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void checkSSLProperties() {
		logger.debug("checkSSLProperties started...");

		final String messageNotDefined = " is not defined";
		Assert.isTrue(!Utilities.isEmpty(sysInfo.getSslProperties().getKeyStoreType()), Constants.SERVER_SSL_KEY__STORE__TYPE + messageNotDefined);
		Assert.notNull(sysInfo.getSslProperties().getKeyStore(), Constants.SERVER_SSL_KEY__STORE + messageNotDefined);
		Assert.isTrue(sysInfo.getSslProperties().getKeyStore().exists(), Constants.SERVER_SSL_KEY__STORE + " file is not found");
		Assert.notNull(sysInfo.getSslProperties().getKeyStorePassword(), Constants.SERVER_SSL_KEY__STORE__PASSWORD + messageNotDefined);
		Assert.isTrue(!Utilities.isEmpty(sysInfo.getSslProperties().getKeyAlias()), Constants.SERVER_SSL_KEY__ALIAS + messageNotDefined);
		Assert.notNull(sysInfo.getSslProperties().getKeyPassword(), Constants.SERVER_SSL_KEY__PASSWORD + messageNotDefined);
	}

	//-------------------------------------------------------------------------------------------------
	private KeyStore initializeKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		logger.debug("initializeKeyStore started...");

		final KeyStore keystore = KeyStore.getInstance(sysInfo.getSslProperties().getKeyStoreType());
		keystore.load(sysInfo.getSslProperties().getKeyStore().getInputStream(), sysInfo.getSslProperties().getKeyStorePassword().toCharArray());

		return keystore;
	}

	//-------------------------------------------------------------------------------------------------
	private void obtainKeys(final KeyStore keyStore) {
		logger.debug("obtainKeys started...");

		final X509Certificate serverCertificate = SecurityUtilities.getCertificateFromKeyStore(keyStore, sysInfo.getSslProperties().getKeyAlias());
		if (serverCertificate == null) {
			throw new ServiceConfigurationError("Cannot find server certificate in the specified key store");
		}

		arrowheadContext.put(Constants.SERVER_CERTIFICATE, serverCertificate);
		final PublicKey publicKey = serverCertificate.getPublicKey();
		arrowheadContext.put(Constants.SERVER_PUBLIC_KEY, publicKey);

		final PrivateKey privateKey = SecurityUtilities.getPrivateKeyFromKeyStore(keyStore, sysInfo.getSslProperties().getKeyAlias(), sysInfo.getSslProperties().getKeyPassword());
		if (privateKey == null) {
			throw new ServiceConfigurationError("Cannot find private key in the specified key store");
		}
		arrowheadContext.put(Constants.SERVER_PRIVATE_KEY, privateKey);
	}

	//-------------------------------------------------------------------------------------------------
	private void checkServerCertificate(final KeyStore keyStore) {
		logger.debug("checkServerCertificate started...");

		final X509Certificate serverCertificate = (X509Certificate) arrowheadContext.get(Constants.SERVER_CERTIFICATE);
		final CommonNameAndType serverData = SecurityUtilities.getIdentificationDataFromSubjectDN(serverCertificate.getSubjectX500Principal().getName(X500Principal.RFC2253));

		if (serverData == null) {
			throw new AuthException("Server certificate is not compliant with the Arrowhead certificate structure, common name and profile type not found");
		}

		if (CertificateProfileType.SYSTEM != serverData.profileType()) {
			throw new AuthException("Server certificate is not compliant with the Arrowhead certificate structure, invalid profile type: " + serverData.profileType());
		}

		if (!SecurityUtilities.isValidSystemCommonName(serverData.commonName())) {
			logger.error("Server CN ({}) is not compliant with the Arrowhead certificate structure, since it does not have {} parts", serverData.commonName(), Constants.SYSTEM_CERT_CN_LENGTH);
			throw new AuthException("Server CN (" + serverData.commonName() + ") is not compliant with the Arrowhead certificate structure, since it does not have "
					+ Constants.SYSTEM_CERT_CN_LENGTH + " parts");
		}

		logger.info("Server CN: {}", serverData.commonName());

		arrowheadContext.put(Constants.SERVER_COMMON_NAME, serverData.commonName());
	}

	//-------------------------------------------------------------------------------------------------
	private void subscribeToMqttServiceTopics() {
		logger.debug("subscribeToMqttServiceTopics started...");
		Assert.notNull(mqttController, "mqttController is null");

		if (Utilities.isEmpty(sysInfo.getServices())) {
			return;
		}

		for (final ServiceModel serviceModel : sysInfo.getServices()) {
			mqttController.listen(serviceModel);
		}
	}

	@Value(ServiceInterfaceExtensions.$INTERFACE_POLICY_WD)
	private String interfacePolicyOverride;

	@Value(ServiceInterfaceExtensions.$EXTRA_INTERFACES_WD)
	private String extraInterfacesJson;

	//-------------------------------------------------------------------------------------------------
	private void registerToServiceRegistry() throws InterruptedException {
		logger.debug("registerToServiceRegistry started...");

		if (skipRegistration()) {
			return;
		}

		// if authentication is handled by an other system, we have to wait a little to give time to the running login job
		if (AuthenticationPolicy.OUTSOURCED == sysInfo.getAuthenticationPolicy()) {
			Thread.sleep(sysInfo.getAuthenticatorLoginDelay());
			ensureIdentityToken();
		}

		checkServiceRegistryConnection(sysInfo.isSslEnabled(), MAX_NUMBER_OF_SERVICEREGISTRY_CONNECTION_RETRIES, WAITING_PERIOD_BETWEEN_RETRIES_IN_SECONDS);

		// revoke system (if any)
		arrowheadHttpService.consumeService(Constants.SERVICE_DEF_SYSTEM_DISCOVERY, Constants.SERVICE_OP_REVOKE, Constants.SYS_NAME_SERVICE_REGISTRY, Void.class);

		// register system
		final SystemModel model = sysInfo.getSystemModel();
		final SystemRegisterRequestDTO payload = new SystemRegisterRequestDTO(model.metadata(), model.version(), model.addresses(), model.deviceName());
		arrowheadHttpService.consumeService(Constants.SERVICE_DEF_SYSTEM_DISCOVERY, Constants.SERVICE_OP_REGISTER, Constants.SYS_NAME_SERVICE_REGISTRY, SystemResponseDTO.class, payload);

		// register services
		if (sysInfo.getServices() != null) {
			for (final ServiceModel serviceModel : sysInfo.getServices()) {
				registerService(serviceModel);
			}
		}

		// the native sysMonitor offer always requires the X.509 identity at the broker
		if (sysMonitorPublisher != null) {
			registerService(sysMonitorPublisher.getServiceModel(), ServiceInterfacePolicy.CERT_AUTH);
		}

		logger.info("System {} published {} service(s)", sysInfo.getSystemName(), registeredServices.size());
	}

	//-------------------------------------------------------------------------------------------------
	private boolean skipRegistration() {
		logger.debug("skipRegistration started...");

		return standaloneMode || Constants.SYS_NAME_SERVICE_REGISTRY.equals(sysInfo.getSystemName());
	}

	//-------------------------------------------------------------------------------------------------
	// The ServiceRegistry requires an identity token (bound to the client certificate) for registration. Log in directly
	// when the scheduled login job has not run yet, retrying while Authentication or the identity is not available.
	private void ensureIdentityToken() throws InterruptedException {
		for (int i = 0; outsourcedLoginJob != null && sysInfo.getIdentityToken() == null && i < MAX_NUMBER_OF_SERVICEREGISTRY_CONNECTION_RETRIES; ++i) {
			outsourcedLoginJob.loginIfNeeded();
			if (sysInfo.getIdentityToken() == null) {
				logger.info("No identity token yet, retrying the login in {} seconds...", WAITING_PERIOD_BETWEEN_RETRIES_IN_SECONDS);
				Thread.sleep(WAITING_PERIOD_BETWEEN_RETRIES_IN_SECONDS * Constants.CONVERSION_MILLISECOND_TO_SECOND);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void checkServiceRegistryConnection(final boolean secure, final int retries, final int period) throws InterruptedException {
		logger.debug("checkServiceRegistryConnection started...");

		final String templateName = secure ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		for (int i = 0; i <= retries; ++i) {
			try {
				serviceCollector.getServiceModel(Constants.SERVICE_DEF_SYSTEM_DISCOVERY, templateName, Constants.SYS_NAME_SERVICE_REGISTRY);
				logger.info("ServiceRegistry is accessible...");
				break;
			} catch (final ForbiddenException | AuthException ex) {
				// should never happen
				throw ex;
			} catch (final ArrowheadException ex) {
				if (i >= retries) {
					throw ex;
				} else {
					logger.info("ServiceRegistry is unavailable at the moment, retrying in {} seconds...", period);
					Thread.sleep(period * Constants.CONVERSION_MILLISECOND_TO_SECOND);
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void registerService(final ServiceModel model) {
		final ServiceInterfacePolicy policy = ServiceInterfaceExtensions.nativePolicy(interfacePolicyOverride, sysInfo);
		registerService(model, policy, ServiceInterfaceExtensions.parse(extraInterfacesJson));
	}

	//-------------------------------------------------------------------------------------------------
	private void registerService(final ServiceModel model, final ServiceInterfacePolicy interfacePolicy) {
		registerService(model, interfacePolicy, List.of());
	}

	//-------------------------------------------------------------------------------------------------
	private void registerService(final ServiceModel model, final ServiceInterfacePolicy interfacePolicy, final List<ServiceInterfaceExtensions.ExtraInterface> extras) {
		logger.debug("registerService started...");

		final List<ServiceInstanceInterfaceRequestDTO> interfaces = model.interfaces()
				.stream()
				.map(i -> new ServiceInstanceInterfaceRequestDTO(i.templateName(), i.protocol(), interfacePolicy.name(), i.properties()))
				.collect(Collectors.toCollection(ArrayList::new));
		interfaces.addAll(ServiceInterfaceExtensions.extrasFor(model, interfaces, extras));
		final ServiceInstanceCreateRequestDTO payload = new ServiceInstanceCreateRequestDTO(model.serviceDefinition(), model.version(), null, model.metadata(), interfaces);
		final ServiceInstanceResponseDTO response = arrowheadHttpService.consumeService(
				Constants.SERVICE_DEF_SERVICE_DISCOVERY,
				Constants.SERVICE_OP_REGISTER,
				Constants.SYS_NAME_SERVICE_REGISTRY,
				ServiceInstanceResponseDTO.class,
				payload);
		registeredServices.add(response.instanceId());
	}

	//-------------------------------------------------------------------------------------------------
	private void revokeServices() throws InterruptedException {
		logger.debug("revokeServices started...");

		if (skipRegistration()) {
			return;
		}

		try {
			checkServiceRegistryConnection(sysInfo.isSslEnabled(), 0, 1);

			for (final String serviceInstanceId : registeredServices) {
				arrowheadHttpService.consumeService(Constants.SERVICE_DEF_SERVICE_DISCOVERY, Constants.SERVICE_OP_REVOKE, Constants.SYS_NAME_SERVICE_REGISTRY, Void.class, List.of(serviceInstanceId));
			}

			logger.info("Core system {} revoked {} service(s)", sysInfo.getSystemName(), registeredServices.size());
			registeredServices.clear();
		} catch (final Throwable t) {
			logger.error(t.getMessage());
			logger.debug(t);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private IdentityRequestDTO getLogoutPayload() {
		logger.debug("getLogoutPayload started...");

		return new IdentityRequestDTO(
				sysInfo.getSystemName(),
				sysInfo.getAuthenticatorCredentials());
	}
}