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

import static eu.arrowhead.sysmonitor.service.Json.now;
import static eu.arrowhead.sysmonitor.service.Json.parse;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;
import com.rabbitmq.client.Delivery;

import eu.arrowhead.common.Constants;
import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;
import jakarta.annotation.PreDestroy;

// sysMonitor intake (SysMonitor-AMQP-IDD-5.2.2): the durable queue SysMonitor.observations on the "arrowhead" topic exchange,
// bound to <cloud>.sysmonitor.<provider>.<instance>.<type> for every subscribed type of every orchestrated provider. AMQPS
// with the system certificate (SASL EXTERNAL). Valid observations are stored and queued for MQTT before they are acknowledged;
// invalid ones are rejected without requeue; a storage failure requeues.
@Component
public class AmqpIntake {

	//=================================================================================================
	// members

	private static final int HEARTBEAT_SECONDS = 30;
	private static final int CONNECTION_TIMEOUT_MILLIS = 10000;
	private static final int PREFETCH = 100;

	private final Logger logger = LogManager.getLogger(getClass());

	private final Object lock = new Object();

	@Autowired
	private BrokerTls tls;

	@Autowired
	private MonitorDbService store;

	@Autowired
	private SysMonitorState state;

	@Value(Constants.$SYS_MONITOR_BROKER_ADDRESS_WD)
	private String address;

	@Value(Constants.$SYS_MONITOR_BROKER_PORT_WD)
	private int port;

	@Value(SysMonitorConstants.$CLOUD_WD)
	private String cloud;

	private volatile Connection connection;
	private volatile Channel intake;
	private volatile Set<String> applied;
	private volatile String lastMessageAt;
	private volatile String error;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String queue() {
		return SysMonitorConstants.SYSTEM_NAME + ".observations";
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isConnected() {
		final Connection current = connection;
		final Channel channel = intake;
		return current != null && current.isOpen() && channel != null && channel.isOpen();
	}

	//-------------------------------------------------------------------------------------------------
	// true when a new connection was made (the caller then re-applies the bindings)
	public boolean connect() throws Exception {
		synchronized (lock) {
			if (isConnected()) {
				return false;
			}

			closeQuietly();
			final ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(address);
			factory.setPort(port);
			if (tls.isEnabled()) {
				factory.useSslProtocol(tls.context());
				factory.enableHostnameVerification();
				factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
			}

			factory.setRequestedHeartbeat(HEARTBEAT_SECONDS);
			factory.setConnectionTimeout(CONNECTION_TIMEOUT_MILLIS);
			factory.setAutomaticRecoveryEnabled(false);
			connection = factory.newConnection(SysMonitorConstants.SYSTEM_NAME + "-java-intake");
			intake = connection.createChannel();
			intake.queueDeclare(queue(), true, false, false, null);
			intake.basicQos(PREFETCH);
			final Channel deliveryChannel = intake;
			intake.basicConsume(queue(), false, (tag, delivery) -> deliver(deliveryChannel, delivery), tag -> {
			});
			error = null;
			logger.info("sysMonitor intake connected to {}:{}", address, port);
			return true;
		}
	}

	//-------------------------------------------------------------------------------------------------
	// binds every desired key (the broker can lose bindings that the store still lists, and binding an existing binding is a
	// no-op) and unbinds keys that are no longer desired
	public void apply(final Set<String> desired) throws Exception {
		synchronized (lock) {
			if (!isConnected()) {
				return;
			}

			for (final String key : applied()) {
				if (!desired.contains(key)) {
					intake.queueUnbind(queue(), SysMonitorConstants.EXCHANGE, key);
				}
			}

			for (final String key : desired) {
				intake.queueBind(queue(), SysMonitorConstants.EXCHANGE, key);
			}

			store.bindings(desired);
			applied = new TreeSet<>(desired);
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> applied() {
		Set<String> current = applied;
		if (current == null) {
			current = store.bindings();
			applied = current;
		}

		return current;
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
	public String getLastMessageAt() {
		return lastMessageAt;
	}

	//-------------------------------------------------------------------------------------------------
	public String getError() {
		return error;
	}

	//-------------------------------------------------------------------------------------------------
	@PreDestroy
	public void close() {
		synchronized (lock) {
			closeQuietly();
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	void deliver(final Channel channel, final Delivery delivery) {
		final long tag = delivery.getEnvelope().getDeliveryTag();
		try {
			if (delivery.getBody().length > SysMonitorConstants.MAX_OBSERVATION_BYTES) {
				throw new IllegalArgumentException("Observation too large");
			}

			final Map<String, Object> observation = ObservationFormat.normalize(parse(delivery.getBody()), delivery.getEnvelope().getRoutingKey(), cloud);
			if (store.enqueue(observation)) {
				lastMessageAt = now();
			}

			acknowledge(channel, () -> channel.basicAck(tag, false));
		} catch (final IllegalArgumentException ex) {
			logger.debug("Rejected observation on {}: {}", delivery.getEnvelope().getRoutingKey(), ex.getMessage());
			state.setLastError("Rejected observation");
			acknowledge(channel, () -> channel.basicReject(tag, false));
		} catch (final Exception ex) {
			logger.error("Observation persistence failed: {}", ex.getMessage());
			logger.debug(ex);
			error = "Observation persistence failed";
			acknowledge(channel, () -> channel.basicNack(tag, false, true));
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void acknowledge(final Channel channel, final ChannelAction action) {
		synchronized (lock) {
			if (channel.isOpen()) {
				try {
					action.run();
				} catch (final Exception ex) {
					logger.debug("AMQP acknowledgement failed: {}", ex.getMessage());
				}
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	private void closeQuietly() {
		final Connection current = connection;
		connection = null;
		intake = null;
		if (current != null) {
			try {
				current.close(1000);
			} catch (final Exception ex) {
				logger.debug("AMQP close: {}", ex.getMessage());
			}
		}
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	@FunctionalInterface
	private interface ChannelAction {
		void run() throws Exception;
	}
}
