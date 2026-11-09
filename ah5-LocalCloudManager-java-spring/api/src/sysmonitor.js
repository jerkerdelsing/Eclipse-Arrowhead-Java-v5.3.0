'use strict';

// Native sysMonitor publisher (SysMonitor-AMQP-IDD-5.2.2): echo, power and status observations about this system,
// published to the local-cloud AMQP broker (topic exchange "arrowhead", routing key
// <Cloud>.sysmonitor.<System>.<serviceInstance>.<observationType>). Mutual TLS with SASL EXTERNAL: the broker user is
// the client certificate CN. Messages are persistent and confirmed by the broker.

const amqp = require('amqplib');

const OBSERVATION_TYPES = ['echo', 'power', 'status'];
const EXCHANGE = 'arrowhead';
// amqplib 0.10.x offers frame_max 4096 by default; RabbitMQ 4.x refuses anything below 8192. 131072 is the broker default.
const FRAME_MAX = 131072;

class SysMonitorPublisher {
  constructor({ systemName, localCloud = 'TestCloud', host = 'amqp-broker', port = 5671, exchange = EXCHANGE, ca, cert, key,
    health = async () => [true, `${systemName} is running`], serviceCount = () => 0, interval = 30000,
    serviceInstance }) {
    Object.assign(this, { systemName, host, port, exchange, ca, cert, key, health, serviceCount, interval });
    this.serviceInstance = serviceInstance || systemName;
    this.baseRoutingKey = `${localCloud}.sysmonitor.${systemName}.${this.serviceInstance}`;
    this.sequence = 0;
    this.started = Date.now();
    this.connection = null;
    this.channel = null;
    this.lastError = null;
  }

  serviceRegistration() {
    return {
      serviceDefinitionName: 'sysMonitor', version: '1.0.0', expiresAt: null,
      metadata: {
        contract: 'SysMonitor-AMQP-IDD-5.2.2', schemaVersion: '1.0', publisherMode: 'native',
        observationTypes: OBSERVATION_TYPES, delivery: 'persistent, publisher-confirmed'
      },
      interfaces: [{
        templateName: 'generic_amqps', protocol: 'amqps', policy: 'CERT_AUTH',
        properties: {
          accessAddresses: [this.host], accessPort: this.port, exchange: this.exchange,
          baseRoutingKey: this.baseRoutingKey, operations: OBSERVATION_TYPES
        }
      }]
    };
  }

  observation(type, value, unit, quality, now) {
    this.sequence += 1;
    const observation = {
      schemaVersion: '1.0',
      observationId: `${this.systemName}-${type}-${now}-${this.sequence}`,
      providerSystem: this.systemName,
      serviceInstance: this.serviceInstance,
      observationType: type,
      sourceTime: new Date(now).toISOString(),
      sequence: this.sequence,
      value,
      quality,
      metadata: { publisherMode: 'native' }
    };
    if (unit !== null) observation.unit = unit;
    return observation;
  }

  async observations(now = Date.now()) {
    const [healthy, detail] = await this.health();
    const quality = healthy ? 'good' : 'uncertain';
    return [
      this.observation('echo', { status: healthy ? 'alive' : 'degraded', uptimeSeconds: Math.floor((now - this.started) / 1000), detail }, null, quality, now),
      this.observation('power', { supplyType: 'line', state: 'available' }, '1', 'good', now),
      this.observation('status', { state: 'running', health: healthy ? 'healthy' : 'degraded', serviceCount: this.serviceCount() }, '1', quality, now)
    ];
  }

  async connect() {
    const connection = await amqp.connect({ protocol: 'amqps', hostname: this.host, port: this.port, heartbeat: 30, frameMax: FRAME_MAX }, {
      ca: [this.ca], cert: this.cert, key: this.key, servername: this.host, credentials: amqp.credentials.external(),
      clientProperties: { connection_name: `${this.systemName}-sysMonitor` }, timeout: 10000
    });
    connection.on('error', () => {});
    connection.on('close', () => { this.connection = null; this.channel = null; });
    this.channel = await connection.createConfirmChannel();
    this.channel.on('error', () => {});
    this.channel.on('close', () => { this.channel = null; });
    this.connection = connection;
  }

  close() {
    const connection = this.connection;
    this.connection = null;
    this.channel = null;
    if (connection) connection.close().catch(() => {});
  }

  async publishOnce() {
    if (!this.channel) {
      this.close();
      await this.connect();
    }
    for (const observation of await this.observations()) {
      this.channel.publish(this.exchange, `${this.baseRoutingKey}.${observation.observationType}`,
        Buffer.from(JSON.stringify(observation)), { contentType: 'application/json', persistent: true, messageId: observation.observationId });
      await this.channel.waitForConfirms();
    }
  }

  start() {
    const cycle = async () => {
      try {
        await this.publishOnce();
        this.lastError = null;
      } catch (error) {
        this.lastError = error.message;
        this.close();
        console.error(`sysMonitor publication failed: ${error.message}`);
      }
    };
    cycle();
    setInterval(cycle, this.interval).unref();
  }
}

module.exports = { FRAME_MAX, SysMonitorPublisher, OBSERVATION_TYPES, EXCHANGE };
