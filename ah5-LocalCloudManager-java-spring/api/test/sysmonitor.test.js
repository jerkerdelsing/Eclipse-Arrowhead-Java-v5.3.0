'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { SysMonitorPublisher } = require('../src/sysmonitor');

test('registration and observations follow the sysMonitor contract', async () => {
  const publisher = new SysMonitorPublisher({ systemName: 'LocalCloudManager', health: async () => [false, 'docker down'], serviceCount: () => 2 });
  const registration = publisher.serviceRegistration();
  assert.equal(registration.metadata.publisherMode, 'native');
  assert.equal(registration.interfaces[0].policy, 'CERT_AUTH');
  assert.equal(registration.interfaces[0].templateName, 'generic_amqps');
  assert.equal(registration.interfaces[0].properties.exchange, 'arrowhead');
  assert.equal(registration.interfaces[0].properties.baseRoutingKey, 'TestCloud.sysmonitor.LocalCloudManager.LocalCloudManager');
  const [echo, power, status] = await publisher.observations(Date.parse('2026-10-11T10:00:00.250Z'));
  assert.equal(echo.value.status, 'degraded');
  assert.equal(echo.quality, 'uncertain');
  assert.equal(echo.sourceTime, '2026-10-11T10:00:00.250Z');
  assert.equal(power.unit, '1');
  assert.equal(status.value.serviceCount, 2);
  assert.deepEqual([echo.sequence, power.sequence, status.sequence], [1, 2, 3]);
});

test('connect negotiates a frame_max the broker accepts, with EXTERNAL auth over mTLS', async (t) => {
  const amqp = require('amqplib');
  const { FRAME_MAX } = require('../src/sysmonitor');
  let captured;
  const emitter = { on() {} };
  t.mock.method(amqp, 'connect', async (url, socketOptions) => {
    captured = { url, socketOptions };
    return { ...emitter, createConfirmChannel: async () => emitter, close: async () => {} };
  });
  const publisher = new SysMonitorPublisher({ systemName: 'LocalCloudManager', ca: 'ca', cert: 'cert', key: 'key' });
  await publisher.connect();
  assert.ok(captured.url.frameMax >= 8192);
  assert.equal(captured.url.frameMax, FRAME_MAX);
  assert.equal(captured.url.protocol, 'amqps');
  assert.equal(captured.socketOptions.credentials.mechanism, 'EXTERNAL');
  assert.deepEqual([captured.socketOptions.cert, captured.socketOptions.key], ['cert', 'key']);
});
