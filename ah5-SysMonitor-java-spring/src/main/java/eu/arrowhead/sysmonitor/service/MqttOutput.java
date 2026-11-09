/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import static eu.arrowhead.sysmonitor.service.Json.bytes;
import static eu.arrowhead.sysmonitor.service.Json.now;
import static eu.arrowhead.sysmonitor.service.Json.obj;
import static eu.arrowhead.sysmonitor.service.Json.parse;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.jpa.entity.Outbox;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;
import jakarta.annotation.PreDestroy;

// sysMonitorData output (SysMonitor-Data-MQTT-IDD-5.2.2): MQTT v5 over mutual TLS, QoS 1, not retained, topic
// arrowhead/<cloud>/sysmonitor/data/<observationType>/<providerSystem>. The outbox makes delivery at-least-once.
@Component
public class MqttOutput {

	//=================================================================================================
	// members

	private static final int CONNECTION_TIMEOUT_SECONDS = 10;
	private static final int KEEP_ALIVE_SECONDS = 60;
	private static final long TIME_TO_WAIT_MILLIS = 10000;

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private BrokerTls tls;

	@Autowired
	private MonitorDbService store;

	@Autowired
	private SysMonitorState state;

	@Value(SysMonitorConstants.$MQTT_ADDRESS_WD)
	private String address;

	@Value(SysMonitorConstants.$MQTT_PORT_WD)
	private int port;

	@Value(SysMonitorConstants.$CLOUD_WD)
	private String cloud;

	private MqttClient client;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String output() {
		return "arrowhead/" + cloud + "/sysmonitor/data";
	}

	//-------------------------------------------------------------------------------------------------
	public String getAddress() {
		return address;
	}

	//-------------------------------------------------------------------------------------------------
	public int getPort() {
		return port;
	}

	//-------------------------------------------------------------------------------------------------
	public synchronized boolean isConnected() {
		return client != null && client.isConnected();
	}

	//-------------------------------------------------------------------------------------------------
	public synchronized void connect() throws Exception {
		if (isConnected()) {
			return;
		}

		if (client == null) {
			client = new MqttClient((tls.isEnabled() ? "ssl://" : "tcp://") + address + ":" + port, SysMonitorConstants.SYSTEM_NAME, new MemoryPersistence());
			client.setTimeToWait(TIME_TO_WAIT_MILLIS);
		}

		final MqttConnectionOptions options = new MqttConnectionOptions();
		if (tls.isEnabled()) {
			options.setSocketFactory(tls.context().getSocketFactory());
			options.setHttpsHostnameVerificationEnabled(true);
		}

		options.setCleanStart(true);
		options.setAutomaticReconnect(true);
		options.setConnectionTimeout(CONNECTION_TIMEOUT_SECONDS);
		options.setKeepAliveInterval(KEEP_ALIVE_SECONDS);
		client.connect(options);
		logger.info("sysMonitorData output connected to {}:{}", address, port);
	}

	//-------------------------------------------------------------------------------------------------
	// publishes every due outbox row; a failed row is retried after a delay
	public void publishOutbox() {
		if (!isConnected()) {
			return;
		}

		for (final Outbox row : store.pending()) {
			final String id = row.getObservationId();
			try {
				final Map<String, Object> observation = obj(parse(row.getPayload()));
				observation.put("publishedTime", now());
				publish(ObservationFormat.topic(output(), observation), bytes(observation));
				store.published(id);
				state.setLastError(null);
			} catch (final Exception ex) {
				logger.debug(ex);
				state.setLastError("MQTT output publish failed");
				store.failed(id);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	@PreDestroy
	public synchronized void close() {
		if (client == null) {
			return;
		}

		try {
			if (client.isConnected()) {
				client.disconnect();
			}

			client.close();
		} catch (final Exception ex) {
			logger.debug("MQTT close: {}", ex.getMessage());
		}

		client = null;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private synchronized void publish(final String topic, final byte[] payload) throws Exception {
		if (client == null) {
			throw new IllegalStateException("MQTT output is not connected");
		}

		client.publish(topic, payload, SysMonitorConstants.MQTT_QOS, false);
	}
}
