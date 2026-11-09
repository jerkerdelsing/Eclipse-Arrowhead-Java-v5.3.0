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
package eu.arrowhead.common.mqtt;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;

@Component
@ConditionalOnProperty(name = Constants.MQTT_API_ENABLED, matchIfMissing = false)
public class MqttService {

	//=================================================================================================
	// members

	private static final String SSL_PREFIX = Constants.SSL + "://";
	private static final String TCP_PREFIX = Constants.TCP + "://";
	private static final String SSL_KEY_MANAGER_FACTORY_ALGORITHM = "ssl.KeyManagerFactory.algorithm";
	private static final String SSL_TRUST_MANAGER_FACTORY_ALGORITHM = "ssl.TrustManagerFactory.algorithm";
	private static final String TLS_VERSION = "TLSv1.2";

	@Autowired
	private SSLProperties sslProperties;

	private final Map<String, MqttClient> clientMap = new ConcurrentHashMap<>();

	private final Logger logger = LogManager.getLogger(getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public MqttClient client(final String connectId) {
		logger.debug("client started");
		Assert.isTrue(!Utilities.isEmpty(connectId), "connectId is empty");

		return clientMap.get(connectId);
	}

	//-------------------------------------------------------------------------------------------------
	public void connect(
			final String connectId,
			final String address,
			final int port,
			final String clientId,
			final String username,
			final String password) throws MqttException {
		logger.debug("connect started");

		createConnection(connectId, address, port, null, clientId, username, password);
	}

	//-------------------------------------------------------------------------------------------------
	public void connect(final String connectId, final String address, final int port, final boolean isSSl) throws MqttException {
		logger.debug("connect started");

		createConnection(connectId, address, port, isSSl, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public void disconnect(final String connectId) throws MqttException {
		logger.debug("disconnect started");

		final MqttClient client = clientMap.get(connectId);
		if (client != null) {
			client.disconnect();
			clientMap.remove(connectId);
			logger.info("Disconnected from MQTT broker");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void createConnection(
			final String connectId,
			final String address,
			final int port,
			final Boolean isSSL,
			final String clientId,
			final String username,
			final String password) throws MqttException {
		logger.debug("createConnection started");
		Assert.isTrue(!Utilities.isEmpty(connectId), "connectId is empty");
		Assert.isTrue(!Utilities.isEmpty(address), "address is empty");

		if (clientMap.containsKey(connectId)) {
			disconnect(connectId);
		}

		String serverURI;
		if (isSSL == null) {
			serverURI = sslProperties.isSslEnabled() ? SSL_PREFIX : TCP_PREFIX;
		} else {
			serverURI = isSSL ? SSL_PREFIX : TCP_PREFIX;
		}
		serverURI += address + ":" + port;

		final MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		if (!Utilities.isEmpty(username)) {
			options.setUserName(username);
			if (!Utilities.isEmpty(password)) {
				options.setPassword(password.toCharArray());
			}
		}

		if (sslProperties.isSslEnabled()) {
			try {
				options.setSocketFactory(sslSettings());
			} catch (final Exception ex) {
				logger.debug(ex);
				logger.error("Creating SSL context is failed. Reason: " + ex.getMessage());
				throw new MqttException(MqttException.REASON_CODE_SSL_CONFIG_ERROR, ex);
			}
		}

		// clean sessions only, so no in-flight state has to survive a restart: the default file persistence would need a
		// writable working directory, which read-only containers do not have
		final MqttClient client = new MqttClient(serverURI, !Utilities.isEmpty(clientId) ? clientId : UUID.randomUUID().toString(), new MemoryPersistence());
		client.connect(options);
		clientMap.put(connectId, client);

		logger.info("Connected to MQTT broker: " + client.getServerURI());
	}

	//-------------------------------------------------------------------------------------------------
	private SSLSocketFactory sslSettings() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException, KeyManagementException {
		logger.debug("sslSettings started");

		final String messageNotDefined = " is not defined";
		Assert.isTrue(!Utilities.isEmpty(sslProperties.getKeyStoreType()), Constants.SERVER_SSL_KEY__STORE__TYPE + messageNotDefined);
		Assert.notNull(sslProperties.getKeyStore(), Constants.SERVER_SSL_KEY__STORE + messageNotDefined);
		Assert.isTrue(sslProperties.getKeyStore().exists(), Constants.SERVER_SSL_KEY__STORE + " file is not found");
		Assert.notNull(sslProperties.getKeyStorePassword(), Constants.SERVER_SSL_KEY__STORE__PASSWORD + messageNotDefined);
		Assert.notNull(sslProperties.getKeyPassword(), Constants.SERVER_SSL_KEY__PASSWORD + messageNotDefined);
		Assert.notNull(sslProperties.getTrustStore(), Constants.SERVER_SSL_TRUST__STORE + messageNotDefined);
		Assert.isTrue(sslProperties.getTrustStore().exists(), Constants.SERVER_SSL_TRUST__STORE + " file is not found");
		Assert.notNull(sslProperties.getTrustStorePassword(), Constants.SERVER_SSL_TRUST__STORE__PASSWORD + messageNotDefined);

		final KeyStore keyStore = KeyStore.getInstance(sslProperties.getKeyStoreType());
		keyStore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());
		final String kmfAlgorithm = System.getProperty(SSL_KEY_MANAGER_FACTORY_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
		final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm);
		keyManagerFactory.init(keyStore, sslProperties.getKeyStorePassword().toCharArray());

		final KeyStore trustStore = KeyStore.getInstance(sslProperties.getKeyStoreType());
		trustStore.load(sslProperties.getTrustStore().getInputStream(), sslProperties.getTrustStorePassword().toCharArray());
		final String tmfAlgorithm = System.getProperty(SSL_TRUST_MANAGER_FACTORY_ALGORITHM, TrustManagerFactory.getDefaultAlgorithm());
		final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
		trustManagerFactory.init(trustStore);

		final SSLContext sslContext = SSLContext.getInstance(TLS_VERSION);
		sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

		return sslContext.getSocketFactory();
	}
}