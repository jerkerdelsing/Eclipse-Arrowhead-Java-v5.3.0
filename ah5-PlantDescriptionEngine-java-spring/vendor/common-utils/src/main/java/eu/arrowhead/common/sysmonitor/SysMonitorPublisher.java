/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.common.sysmonitor;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.amqp.model.AmqpInterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

// Native publisher of the sysMonitor service: echo, power and status observations over AMQP 0-9-1 with mutual TLS.
// The broker authenticates the client certificate (SASL EXTERNAL); its CN is the broker user name.
// The ApplicationInitListener registers the offer returned by getServiceModel().
@Component
@ConditionalOnProperty(name = Constants.SYS_MONITOR_ENABLED, havingValue = "true")
public class SysMonitorPublisher {

	//=================================================================================================
	// members

	private static final long CONFIRM_TIMEOUT_MILLIS = 5000;
	private static final String CONTENT_TYPE = "application/json";

	private static final String FALLBACK_LOCAL_CLOUD = "TestCloud";

	private final Logger logger = LogManager.getLogger(getClass());

	private final SystemInfo sysInfo;
	private final SSLProperties serverSsl;
	private volatile boolean stopped = false;
	private final ObjectProvider<SysMonitorHealthIndicator> healthIndicator;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Instant startedAt = Instant.now();

	@Value(Constants.$SYS_MONITOR_BROKER_ADDRESS_WD)
	private String brokerAddress;

	@Value(Constants.$SYS_MONITOR_BROKER_PORT_WD)
	private int brokerPort;

	@Value(Constants.$SYS_MONITOR_EXCHANGE_WD)
	private String exchange;

	@Value(Constants.$SYS_MONITOR_LOCAL_CLOUD_WD)
	private String localCloud;

	@Value(Constants.$SYS_MONITOR_SERVICE_INSTANCE_WD)
	private String serviceInstance;

	@Value(Constants.$SYS_MONITOR_PUBLISH_INTERVAL_WD)
	private int publishInterval;

	@Value(Constants.$SYS_MONITOR_SSL_ENABLED_WD)
	private boolean sslEnabled;

	@Value(Constants.$SYS_MONITOR_SSL_KEY_STORE_WD)
	private String keyStore;

	@Value(Constants.$SYS_MONITOR_SSL_KEY_STORE_PASSWORD_WD)
	private String keyStorePassword;

	@Value(Constants.$SYS_MONITOR_SSL_TRUST_STORE_WD)
	private String trustStore;

	@Value(Constants.$SYS_MONITOR_SSL_TRUST_STORE_PASSWORD_WD)
	private String trustStorePassword;

	private SysMonitorObservations observations;
	private ScheduledExecutorService scheduler;
	private Connection connection;
	private Channel channel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SysMonitorPublisher(final SystemInfo sysInfo, final SSLProperties serverSsl, final ObjectProvider<SysMonitorHealthIndicator> healthIndicator) {
		this.sysInfo = sysInfo;
		this.serverSsl = serverSsl;
		this.healthIndicator = healthIndicator;
	}

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	public void start() {
		final String instance = Utilities.isEmpty(serviceInstance) ? sysInfo.getSystemName() : serviceInstance.trim();
		final String cloud = Utilities.isEmpty(localCloud) ? cloudFromCertificate() : localCloud.trim();
		observations = new SysMonitorObservations(sysInfo.getSystemName(), cloud, instance);
		scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "sysMonitor-publisher");
			thread.setDaemon(true);
			return thread;
		});
		scheduler.scheduleWithFixedDelay(this::publishCycle, 0, publishInterval, TimeUnit.SECONDS);
		logger.info("sysMonitor publisher enabled on {} {}", exchange, observations.baseRoutingKey());
	}

	//-------------------------------------------------------------------------------------------------
	// Always releases the AMQP connection: its threads are not daemon threads and would otherwise keep the JVM alive
	// when the context closes (for example after a failed start-up).
	@PreDestroy
	public void stop() {
		stopped = true;
		if (scheduler != null) {
			scheduler.shutdownNow();
		}

		closeConnection();
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceModel getServiceModel() {
		final String templateName = sslEnabled ? Constants.GENERIC_AMQPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_AMQP_INTERFACE_TEMPLATE_NAME;
		final AmqpInterfaceModel amqpInterface = new AmqpInterfaceModel(templateName, List.of(brokerAddress), brokerPort, exchange,
				observations.baseRoutingKey(), new LinkedHashSet<>(SysMonitorObservations.TYPES));

		return new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_SYS_MONITOR)
				.version(Constants.SYS_MONITOR_VERSION)
				.serviceInterface(amqpInterface)
				.metadata("contract", Constants.SYS_MONITOR_CONTRACT)
				.metadata("schemaVersion", SysMonitorObservations.SCHEMA_VERSION)
				.metadata("publisherMode", SysMonitorObservations.PUBLISHER_MODE)
				.metadata("observationTypes", SysMonitorObservations.TYPES)
				.metadata("delivery", "persistent, publisher-confirmed")
				.build();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	void publishCycle() {
		try {
			ensureConnected();
			final Instant now = Instant.now();
			final SysMonitorHealth health = currentHealth();
			final int serviceCount = sysInfo.getServices() == null ? 0 : sysInfo.getServices().size();
			publish(SysMonitorObservations.TYPE_ECHO, observations.echo(now, now.getEpochSecond() - startedAt.getEpochSecond(), health));
			publish(SysMonitorObservations.TYPE_POWER, observations.power(now));
			publish(SysMonitorObservations.TYPE_STATUS, observations.status(now, health, serviceCount));
		} catch (final Exception ex) {
			logger.debug(ex);
			logger.warn("sysMonitor publication failed: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private SysMonitorHealth currentHealth() {
		final SysMonitorHealthIndicator indicator = healthIndicator.getIfAvailable();
		if (indicator == null) {
			return SysMonitorHealth.alive(sysInfo.getSystemName() + " is running");
		}

		try {
			return indicator.health();
		} catch (final RuntimeException ex) {
			logger.debug(ex);
			return SysMonitorHealth.degraded("health check failed: " + ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void publish(final String observationType, final Map<String, Object> observation) throws Exception {
		final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
				.contentType(CONTENT_TYPE)
				.deliveryMode(2)
				.messageId(String.valueOf(observation.get("observationId")))
				.timestamp(new java.util.Date())
				.build();
		channel.basicPublish(exchange, observations.routingKey(observationType), properties, objectMapper.writeValueAsString(observation).getBytes(StandardCharsets.UTF_8));
		channel.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MILLIS);
	}

	//-------------------------------------------------------------------------------------------------
	private void ensureConnected() throws Exception {
		if (channel != null && channel.isOpen()) {
			return;
		}

		if (stopped) {
			throw new IllegalStateException("sysMonitor publisher is stopped");
		}

		closeConnection();
		final ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(brokerAddress);
		factory.setPort(brokerPort);
		factory.setAutomaticRecoveryEnabled(false); // each publish cycle reconnects when needed
		factory.setConnectionTimeout(5000);
		if (sslEnabled) {
			factory.useSslProtocol(sslContext());
			factory.enableHostnameVerification();
			factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
		}

		connection = factory.newConnection(sysInfo.getSystemName() + "-sysMonitor");
		channel = connection.createChannel();
		channel.confirmSelect();
		logger.info("sysMonitor publisher connected to amqps://" + brokerAddress + ":" + brokerPort);
	}

	//-------------------------------------------------------------------------------------------------
	private void closeConnection() {
		final Connection current = connection;
		connection = null;
		channel = null;
		if (current != null) {
			try {
				current.close(1000);
			} catch (final Exception ex) {
				logger.debug("sysMonitor publisher close: {}", ex.getMessage());
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Uses the sysmonitor.ssl.* stores when given, otherwise the server.ssl.* stores of the system.
	// The local cloud is the second label of the publisher's certificate CN (<System>.<Cloud>.<Organization>.arrowhead.eu).
	String cloudFromCertificate() {
		try {
			final KeyStore keys = loadKeyStore();
			for (final java.util.Enumeration<String> aliases = keys.aliases(); aliases.hasMoreElements();) {
				final String alias = aliases.nextElement();
				if (keys.isKeyEntry(alias) && keys.getCertificate(alias) instanceof java.security.cert.X509Certificate cert) {
					final String commonName = new javax.naming.ldap.LdapName(cert.getSubjectX500Principal().getName()).getRdns().stream()
							.filter(rdn -> "CN".equalsIgnoreCase(rdn.getType())).map(rdn -> rdn.getValue().toString()).findFirst().orElse("");
					final String[] parts = commonName.split("\\.");
					if (parts.length >= 2 && !parts[1].isBlank()) {
						return parts[1];
					}
				}
			}
		} catch (final Exception ex) {
			logger.debug(ex);
		}
		logger.warn("sysmonitor.local.cloud is not set and the certificate CN gives no cloud name; using {}", FALLBACK_LOCAL_CLOUD);
		return FALLBACK_LOCAL_CLOUD;
	}

	//-------------------------------------------------------------------------------------------------
	private KeyStore loadKeyStore() throws Exception {
		final Resource keyStoreResource = Utilities.isEmpty(keyStore) ? serverSsl.getKeyStore() : new DefaultResourceLoader().getResource(keyStore);
		final char[] keyPassword = (Utilities.isEmpty(keyStorePassword) ? serverSsl.getKeyStorePassword() : keyStorePassword).toCharArray();
		final KeyStore keys = KeyStore.getInstance(Utilities.isEmpty(serverSsl.getKeyStoreType()) ? "PKCS12" : serverSsl.getKeyStoreType());
		try (java.io.InputStream in = keyStoreResource.getInputStream()) {
			keys.load(in, keyPassword);
		}
		return keys;
	}

	//-------------------------------------------------------------------------------------------------
	private SSLContext sslContext() throws Exception {
		final DefaultResourceLoader loader = new DefaultResourceLoader();
		final Resource keyStoreResource = Utilities.isEmpty(keyStore) ? serverSsl.getKeyStore() : loader.getResource(keyStore);
		final Resource trustStoreResource = Utilities.isEmpty(trustStore) ? serverSsl.getTrustStore() : loader.getResource(trustStore);
		final char[] keyPassword = (Utilities.isEmpty(keyStorePassword) ? serverSsl.getKeyStorePassword() : keyStorePassword).toCharArray();
		final char[] trustPassword = (Utilities.isEmpty(trustStorePassword) ? serverSsl.getTrustStorePassword() : trustStorePassword).toCharArray();
		final String storeType = Utilities.isEmpty(serverSsl.getKeyStoreType()) ? "PKCS12" : serverSsl.getKeyStoreType();

		final KeyStore keys = KeyStore.getInstance(storeType);
		keys.load(keyStoreResource.getInputStream(), keyPassword);
		final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keys, keyPassword);

		final KeyStore trust = KeyStore.getInstance(storeType);
		trust.load(trustStoreResource.getInputStream(), trustPassword);
		final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trust);

		final SSLContext sslContext = SSLContext.getInstance("TLS");
		sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

		return sslContext;
	}
}
