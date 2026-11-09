'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const {
  demuxDockerLogs,
  systemStatus,
  managedServices,
  numericValue,
  resolveSenMLRecords,
  buildMessagingOverview,
  buildRegistryManagement,
  buildOrchestrationCatalog,
  validateRuleCandidate,
  validateInventoryNames,
  runtimeDisplayNames
} = require('../src/server');

test('demuxDockerLogs decodes Docker stream frames', () => {
  const payload = Buffer.from('hello\n');
  const frame = Buffer.alloc(8 + payload.length);
  frame[0] = 1;
  frame.writeUInt32BE(payload.length, 4);
  payload.copy(frame, 8);
  assert.equal(demuxDockerLogs(frame), 'hello\n');
});

test('systemStatus associates an Arrowhead system with its Compose service', () => {
  const result = systemStatus(
    { name: 'ServiceRegistry', version: '5.2.2' },
    [{ service: 'serviceregistry', state: 'running', status: 'Up', health: 'healthy', managed: true }],
    new Map([['ServiceRegistry', 5]])
  );
  assert.equal(result.composeService, 'serviceregistry');
  assert.equal(result.serviceCount, 5);
  assert.equal(result.runtime.state, 'running');
});

test('registry management links devices, systems, services and deployment state', () => {
  const systems = [
    systemStatus({ name: 'ServiceRegistry', deviceName: 'FactoryFloor', version: '1.0.0' }, [
      { service: 'serviceregistry', displayName: 'ServiceRegistry', state: 'running', managed: true }
    ], new Map([['ServiceRegistry', 1]])),
    { name: 'ExternalConsumer', deviceName: null, runtime: null, composeService: null }
  ];
  const services = [{
    instanceId: 'ServiceRegistry|temperature|1.0.0',
    provider: { name: 'ServiceRegistry' },
    serviceDefinition: { name: 'temperature' }
  }];
  const result = buildRegistryManagement(
    [{ name: 'FactoryFloor', addresses: ['10.0.0.5'] }], systems, services,
    [
      { service: 'serviceregistry', displayName: 'ServiceRegistry', state: 'running', managed: true },
      { service: 'worker', displayName: 'Worker', state: 'running', managed: true }
    ]
  );
  assert.equal(result.summary.registeredDevices, 1);
  assert.equal(result.summary.deployedDevices, 1);
  assert.equal(result.summary.deployedServices, 1);
  assert.equal(result.devices[0].systems[0].services[0].serviceDefinition.name, 'temperature');
  assert.equal(result.devices.at(-1).name, 'No registered device');
  assert.deepEqual(result.runtimeOnly.map((entry) => entry.composeService), ['worker']);
});

test('database services are excluded from lifecycle management', () => {
  assert.equal(managedServices.has('serviceregistry'), true);
  assert.equal(managedServices.has('serviceregistry-db'), false);
});

test('runtime identifiers have canonical Arrowhead display names', () => {
  assert.equal(runtimeDisplayNames.graphdbmanager, 'GraphDBManager');
  assert.equal(runtimeDisplayNames.serviceregistry, 'ServiceRegistry');
  assert.equal(runtimeDisplayNames['serviceorchestration-dynamic'], 'DynamicServiceOrchestration');
  assert.equal(runtimeDisplayNames['system-monitor-publisher'], 'SysMonitorCompatibilityAdapter');
});

test('inventory naming follows PascalCase, camelCase and kebab-case', () => {
  const valid = validateInventoryNames(
    [{ name: 'CertificateAuthority' }],
    [{ serviceDefinition: { name: 'certificateSigning' }, interfaces: [{ properties: { operations: { 'check-certificate': {} } } }] }]
  );
  assert.deepEqual(valid, []);
  const invalid = validateInventoryNames(
    [{ name: 'certificate-authority' }, { name: 'SystemMonitorPublisher' }],
    [
      { serviceDefinition: { name: 'CertificateSigning' }, interfaces: [{ properties: { operations: ['checkCertificate'] } }] },
      { serviceDefinition: { name: 'monitor' }, interfaces: [{ properties: { operations: ['echo'] } }] }
    ]
  );
  assert.deepEqual(invalid, [
    'legacy-service-definition:monitor',
    'legacy-system:SystemMonitorPublisher',
    'operation:checkCertificate',
    'service-definition:CertificateSigning',
    'system:certificate-authority'
  ]);
});

test('numericValue extracts numeric SenML values only', () => {
  assert.equal(numericValue({ value: { v: 42 } }), 42);
  assert.equal(numericValue({ value: { s: 7 } }), 7);
  assert.equal(numericValue({ value: { vs: 'online' } }), null);
});

test('messaging overview exposes broker and active secured flows', () => {
  const containers = ['random-data-producer', 'mqtt-broker', 'amqp-broker', 'sysmonitor', 'serviceorchestration-dynamic', 'datamanager', 'aigym']
    .map((service) => ({ service, state: 'running', status: 'Up (healthy)', health: 'healthy', managed: true }));
  const services = [
    { instanceId: 'SysMonitor|sysMonitorData|1.0.0' },
    { instanceId: 'SysMonitor|sysMonitorHistory|1.0.0' },
    { instanceId: 'DataManager|dataHistorian|1.0.0' },
    { instanceId: 'DataManager|sysMonitor|1.0.0' },
    { instanceId: 'AIGym|sysMonitor|1.0.0' }
  ];
  const result = buildMessagingOverview(containers, services);
  // sysMonitor runs over the AMQP broker; the MQTT broker carries sysMonitorData.
  assert.equal(result.infrastructure[0].endpoint, 'amqps://amqp-broker:5671 · exchange arrowhead');
  assert.equal(result.infrastructure[1].endpoint, 'mqtts://mqtt-broker:8883');
  assert.equal(result.orchestrations.length, 6);
  assert.equal(result.orchestrations.every((flow) => flow.active), true);
  assert.deepEqual(result.orchestrations.filter((flow) => flow.id.includes('aigym')).map((flow) => flow.id), ['sysmonitor-publish-aigym', 'aigym-sysmonitor-data']);
  assert.equal(result.orchestrations.find((flow) => flow.id === 'sysmonitor-publish-datamanager').label, 'sysMonitor.observe · native');
  // sysMonitor history is stored in SysMonitor and read through sysMonitorHistory, not delivered to DataManager.
  assert.equal(result.orchestrations.some((flow) => flow.target === 'DataManager' && flow.source === 'SysMonitor'), false);
  assert.equal(result.orchestrations.find((flow) => flow.id === 'sysmonitor-history').target, 'LocalCloudManager');
});

test('orchestration catalog keeps only interface-compatible consumers', () => {
  const systems = [
    { name: 'Producer', metadata: { x509PublicKey: 'producer-key' } },
    { name: 'SecureConsumer', metadata: { x509PublicKey: 'consumer-key' } },
    { name: 'HttpConsumer', metadata: {} }
  ];
  const services = [
    { instanceId: 'Producer|temperature|1.0.0', provider: { name: 'Producer' }, serviceDefinition: { name: 'temperature' }, interfaces: [{ templateName: 'generic_https', protocol: 'https', policy: 'CERT_AUTH' }] },
    { instanceId: 'SecureConsumer|status|1.0.0', provider: { name: 'SecureConsumer' }, serviceDefinition: { name: 'status' }, interfaces: [{ templateName: 'generic_https', protocol: 'https', policy: 'CERT_AUTH' }] },
    { instanceId: 'HttpConsumer|status|1.0.0', provider: { name: 'HttpConsumer' }, serviceDefinition: { name: 'status' }, interfaces: [{ templateName: 'generic_http', protocol: 'http', policy: 'NONE' }] }
  ];
  const catalog = buildOrchestrationCatalog(systems, services);
  const temperature = catalog.producers.find((entry) => entry.instanceId === 'Producer|temperature|1.0.0');
  assert.deepEqual(temperature.compatibleConsumers, ['SecureConsumer']);
});

test('rule validation rejects a stale or incompatible selection', () => {
  const catalog = { producers: [{ instanceId: 'Provider|service|1.0.0', compatibleConsumers: ['Consumer'] }] };
  assert.deepEqual(validateRuleCandidate(catalog, { consumer: 'Consumer', serviceInstanceId: 'Provider|service|1.0.0', priority: 1 }), {
    consumer: 'Consumer', serviceInstanceId: 'Provider|service|1.0.0', priority: 1
  });
  assert.throws(() => validateRuleCandidate(catalog, { consumer: 'Other', serviceInstanceId: 'Provider|service|1.0.0', priority: 1 }), /not interoperable/);
});

test('resolveSenMLRecords expands dataHistorian packs into dashboard entries', () => {
  const entries = resolveSenMLRecords([
    { bn: 'urn:arrowhead:random-data-producer:', bt: 1000, n: 'random-number', u: '1', v: 42 },
    { bn: 'urn:arrowhead:sysmonitor:DataManager:', bt: 2000, n: 'echo', vs: '{"status":"alive"}' },
    { n: 'power', t: 5, vs: '{"supplyType":"line"}' }
  ], 'RandomDataProducer', 'randomNumberProducer');
  assert.equal(entries.length, 3);
  assert.deepEqual(entries.map((entry) => [entry.name, entry.time]), [
    ['urn:arrowhead:random-data-producer:random-number', 1000],
    ['urn:arrowhead:sysmonitor:DataManager:echo', 2000],
    ['urn:arrowhead:sysmonitor:DataManager:power', 2005]
  ]);
  assert.equal(numericValue(entries[0]), 42);
  assert.equal(entries[0].unit, '1');
});

test('a registered GraphDBManager is matched to its running container', () => {
  const containers = [{ service: 'graphdbmanager', displayName: 'GraphDBManager', state: 'running', managed: true }];
  const result = buildRegistryManagement([], [systemStatus({ name: 'GraphDBManager' }, containers, new Map())],
    [{ instanceId: 'GraphDBManager|graphDataHistorian|1.0.0', provider: { name: 'GraphDBManager' }, serviceDefinition: { name: 'graphDataHistorian' } }],
    containers);
  assert.deepEqual(result.runtimeOnly, []);
  assert.equal(managedServices.has('graphdbmanager'), true);
});
