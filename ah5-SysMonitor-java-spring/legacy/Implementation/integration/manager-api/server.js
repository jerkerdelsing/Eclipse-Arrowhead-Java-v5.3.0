'use strict';

const http = require('node:http');
const https = require('node:https');
const fs = require('node:fs');
const { URL } = require('node:url');

const PORT = Number(process.env.PORT || 8790);
const DOCKER_SOCKET = process.env.DOCKER_SOCKET || '/var/run/docker.sock';
const PROJECT_NAME = process.env.COMPOSE_PROJECT_NAME || 'arrowhead-v52-test-cloud';
const SERVICE_REGISTRY_URL = process.env.SERVICE_REGISTRY_URL || 'http://serviceregistry:8443';
const { SysMonitorPublisher } = require('./sysmonitor');
const DATAMANAGER_URL = process.env.DATAMANAGER_URL || 'https://datamanager:8461';
const SYSMONITOR_URL = process.env.SYSMONITOR_URL || 'https://sysmonitor:8475';
const AUTHENTICATION_URL = process.env.AUTHENTICATION_URL || 'https://authentication:8444';
const SIMPLE_ORCHESTRATION_URL = process.env.SIMPLE_ORCHESTRATION_URL || 'https://serviceorchestration-simple:8442';
const AUTHENTICATION_PASSWORD = process.env.AUTHENTICATION_PASSWORD || 'LCM-test-52';
const TLS_CA_CERT = process.env.TLS_CA_CERT || '/certs/ca.crt';
const TLS_CLIENT_CERT = process.env.TLS_CLIENT_CERT || '/certs/local-cloud-manager.crt';
const TLS_CLIENT_KEY = process.env.TLS_CLIENT_KEY || '/certs/local-cloud-manager.key';
const ARROWHEAD_SYSTEM_NAME = process.env.ARROWHEAD_SYSTEM_NAME || 'LocalCloudManager';
const ARROWHEAD_ADMIN_SYSTEM = process.env.ARROWHEAD_ADMIN_SYSTEM || 'Sysop';
const ACTION_CONFIRMATION = 'confirm';
const MAX_BODY_BYTES = 64 * 1024;
const HISTORY_LIMIT = 60;

const managedServices = new Set((process.env.MANAGED_SERVICES || [
  'serviceregistry',
  'serviceorchestration-dynamic',
  'serviceorchestration-simple',
  'consumerauthorization',
  'authentication',
  'blacklist',
  'translation-manager',
  'device-qos-evaluator',
  'onboarding',
  'datamanager',
  'random-data-producer',
  'sysmonitor',
  'aigym',
  'certificate-authority',
  'mqtt-broker',
  'gatekeeper',
  'gatetunnel'
].join(',')).split(',').map((value) => value.trim()).filter(Boolean));

const systemToService = {
  ServiceRegistry: 'serviceregistry',
  DynamicServiceOrchestration: 'serviceorchestration-dynamic',
  SimpleStoreServiceOrchestration: 'serviceorchestration-simple',
  ConsumerAuthorization: 'consumerauthorization',
  Authentication: 'authentication',
  Blacklist: 'blacklist',
  TranslationManager: 'translation-manager',
  DeviceQoSEvaluator: 'device-qos-evaluator',
  Onboarding: 'onboarding',
  DataManager: 'datamanager',
  RandomDataProducer: 'random-data-producer',
  SysMonitor: 'sysmonitor',
  AIGym: 'aigym',
  CertificateAuthority: 'certificate-authority',
  Gatekeeper: 'gatekeeper',
  GateTunnel: 'gatetunnel',
  [ARROWHEAD_SYSTEM_NAME]: 'cloud-monitor-api'
};

const runtimeDisplayNames = Object.freeze({
  ...Object.fromEntries(Object.entries(systemToService).map(([system, service]) => [service, system])),
  'mqtt-broker': 'MQTTBroker',
  'cloud-monitor-ui': 'LocalCloudManagerUI',
  'system-monitor-publisher': 'SysMonitorCompatibilityAdapter'
});

const arrowheadNamePatterns = Object.freeze({
  system: /^[A-Z][0-9A-Za-z]*$/,
  serviceDefinition: /^[a-z][0-9A-Za-z]*$/,
  operation: /^[a-z](?:[0-9a-z-]*[0-9a-z])?$/
});

function validateInventoryNames(systems, services) {
  const violations = [];
  for (const system of systems || []) {
    if (!arrowheadNamePatterns.system.test(system.name || '')) violations.push(`system:${system.name || '<missing>'}`);
    if (system.name === 'SystemMonitorPublisher') violations.push('legacy-system:SystemMonitorPublisher');
  }
  for (const service of services || []) {
    const definition = service.serviceDefinition?.name || '';
    if (!arrowheadNamePatterns.serviceDefinition.test(definition)) {
      violations.push(`service-definition:${definition || '<missing>'}`);
    }
    if (definition === 'monitor' || definition === 'monitoring') {
      violations.push(`legacy-service-definition:${definition}`);
    }
    for (const intf of service.interfaces || []) {
      const operations = intf.properties?.operations;
      const names = Array.isArray(operations) ? operations : Object.keys(operations || {});
      for (const operation of names) {
        if (!arrowheadNamePatterns.operation.test(operation)) violations.push(`operation:${operation || '<missing>'}`);
      }
    }
  }
  return [...new Set(violations)].sort();
}

const history = [];
let registrationState = { registered: false, error: null, checkedAt: null };
let dataManagerAgent = null;
let identityToken = null;
let identityRefreshAt = 0;
let identityLoginPromise = null;

function jsonResponse(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff'
  });
  res.end(body);
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    let size = 0;
    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > MAX_BODY_BYTES) {
        reject(new Error('Request body is too large'));
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      if (!chunks.length) return resolve({});
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString('utf8')));
      } catch {
        reject(new Error('Invalid JSON body'));
      }
    });
    req.on('error', reject);
  });
}

function dockerRequest(path, options = {}) {
  return new Promise((resolve, reject) => {
    const req = http.request({
      socketPath: DOCKER_SOCKET,
      path,
      method: options.method || 'GET',
      headers: options.headers || {}
    }, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(chunk));
      res.on('end', () => {
        const body = Buffer.concat(chunks);
        if (res.statusCode >= 400) {
          return reject(new Error(`Docker API ${res.statusCode}: ${body.toString('utf8').slice(0, 300)}`));
        }
        resolve({ status: res.statusCode, body, headers: res.headers });
      });
    });
    req.on('error', reject);
    req.end();
  });
}

function getDataManagerAgent() {
  if (!dataManagerAgent) {
    dataManagerAgent = new https.Agent({
      ca: fs.readFileSync(TLS_CA_CERT),
      cert: fs.readFileSync(TLS_CLIENT_CERT),
      key: fs.readFileSync(TLS_CLIENT_KEY),
      minVersion: 'TLSv1.2',
      rejectUnauthorized: true
    });
  }
  return dataManagerAgent;
}

function httpsJson(target, options = {}, payload) {
  return new Promise((resolve, reject) => {
    const body = payload === undefined ? null : Buffer.from(JSON.stringify(payload));
    const req = https.request(new URL(target), {
      agent: getDataManagerAgent(), method: options.method || 'GET',
      headers: { ...(body ? { 'Content-Type': 'application/json', 'Content-Length': body.length } : {}), ...(options.headers || {}) },
      timeout: 5000
    }, (response) => {
      const chunks = [];
      response.on('data', (chunk) => chunks.push(chunk));
      response.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf8');
        let value;
        try { value = text ? JSON.parse(text) : null; } catch { return reject(new Error('Arrowhead service returned invalid JSON')); }
        if (response.statusCode >= 400) {
          const error = new Error(value?.errorMessage || value?.error || `Arrowhead service returned HTTP ${response.statusCode}`);
          error.status = response.statusCode;
          error.payload = value;
          return reject(error);
        }
        resolve(value);
      });
    });
    req.on('timeout', () => req.destroy(new Error('Authentication request timed out')));
    req.on('error', reject);
    if (body) req.write(body);
    req.end();
  });
}

async function getIdentityToken() {
  if (identityToken && identityRefreshAt > Date.now()) return identityToken;
  // Historian and cache are loaded in parallel. Coalesce their cold-start
  // login so Arrowhead Authentication receives one session creation rather
  // than two concurrent inserts for the same system identity.
  if (!identityLoginPromise) {
    identityLoginPromise = httpsJson(`${AUTHENTICATION_URL}/authentication/identity/login`, { method: 'POST' }, {
      systemName: ARROWHEAD_SYSTEM_NAME, credentials: { password: AUTHENTICATION_PASSWORD }
    }).then((result) => {
      identityToken = result.token;
      identityRefreshAt = Date.now() + 240000;
      return identityToken;
    }).finally(() => {
      identityLoginPromise = null;
    });
  }
  return identityLoginPromise;
}

async function authorizationHeader() {
  return `Bearer IDENTITY-TOKEN//${await getIdentityToken()}`;
}

async function orchestrationRequest(path, options = {}, payload) {
  const authorization = await authorizationHeader();
  return httpsJson(`${SIMPLE_ORCHESTRATION_URL}${path}`, {
    method: options.method || 'POST',
    headers: { Authorization: authorization }
  }, payload);
}

// Authentication keeps one session per system, so another LocalCloudManager login (for example a smoke
// test) invalidates the cached token. A 401 therefore forces one fresh login and retry.
async function dataManagerRequest(path) {
  return systemRequest(DATAMANAGER_URL, 'DataManager', path);
}

async function sysMonitorRequest(path) {
  return systemRequest(SYSMONITOR_URL, 'SysMonitor', path, getSysMonitorAgent());
}

let sysMonitorAgent = null;
// SysMonitor serves each request on its own connection; a small pool keeps bursts within its capacity.
function getSysMonitorAgent() {
  if (!sysMonitorAgent) {
    sysMonitorAgent = new https.Agent({
      ca: fs.readFileSync(TLS_CA_CERT),
      cert: fs.readFileSync(TLS_CLIENT_CERT),
      key: fs.readFileSync(TLS_CLIENT_KEY),
      minVersion: 'TLSv1.2',
      rejectUnauthorized: true,
      maxSockets: 4
    });
  }
  return sysMonitorAgent;
}

// GET against an Arrowhead provider with the LocalCloudManager identity token and X.509 client certificate.
async function systemRequest(baseUrl, label, path, agent = getDataManagerAgent()) {
  try {
    return await systemRequestOnce(baseUrl, label, path, agent);
  } catch (error) {
    if (error.status !== 401) throw error;
    identityToken = null;
    identityRefreshAt = 0;
    return systemRequestOnce(baseUrl, label, path, agent);
  }
}

async function systemRequestOnce(baseUrl, label, path, agent) {
  const authorization = await authorizationHeader();
  return new Promise((resolve, reject) => {
    const target = new URL(path, baseUrl);
    const req = https.get(target, {
      agent,
      headers: { Authorization: authorization },
      timeout: 5000
    }, (response) => {
      const chunks = [];
      response.on('data', (chunk) => chunks.push(chunk));
      response.on('end', () => {
        const text = Buffer.concat(chunks).toString('utf8');
        let payload;
        try { payload = text ? JSON.parse(text) : null; } catch { return reject(new Error(`${label} returned invalid JSON`)); }
        if (response.statusCode >= 400) {
          return reject(Object.assign(new Error(payload?.errorMessage || payload?.error || `${label} API ${response.statusCode}`), { status: response.statusCode }));
        }
        resolve(payload);
      });
    });
    req.on('timeout', () => req.destroy(new Error(`${label} request timed out`)));
    req.on('error', reject);
  });
}

function numericValue(entry) {
  for (const field of ['v', 's']) {
    if (typeof entry?.value?.[field] === 'number') return entry.value[field];
  }
  return null;
}

const DATA_HISTORIAN_BASE = '/datamanager/data-historian';
const SYSMONITOR_HISTORY_BASE = '/sysmonitor/history';

// sysMonitor observations of every microsystem are stored by SysMonitor (sysMonitorHistory), not by DataManager.
async function getSysMonitorHistory(system = '', limit = 120) {
  const safeLimit = Math.min(Math.max(Number(limit) || 120, 1), 1000);
  const { systems = [], retentionDays = null } = await sysMonitorRequest(`${SYSMONITOR_HISTORY_BASE}/systems`);
  const rows = await Promise.all(systems.map(async (item) => {
    try {
      const { latest = {} } = await sysMonitorRequest(
        `${SYSMONITOR_HISTORY_BASE}/systems/${encodeURIComponent(item.systemName)}/latest`);
      return { ...item, latest };
    } catch (error) {
      // One unreachable system must not hide the others.
      return { ...item, latest: {}, error: error.status === 404 ? null : error.message };
    }
  }));
  const selectedSystem = systems.some((item) => item.systemName === system) ? system : (rows[0]?.systemName || '');
  const { observations = [] } = selectedSystem
    ? await sysMonitorRequest(`${SYSMONITOR_HISTORY_BASE}/systems/${encodeURIComponent(selectedSystem)}/observations?count=${safeLimit}`)
    : {};
  return {
    collectedAt: new Date().toISOString(),
    retentionDays,
    storedObservations: rows.reduce((sum, item) => sum + (item.observationCount || 0), 0),
    systems: rows,
    selectedSystem,
    observations
  };
}
const SENML_VALUE_FIELDS = ['v', 'vs', 'vb', 'vd', 's'];
// SenML times below 2^28 are relative to the base time (RFC 8428 section 4.5.3).
const SENML_RELATIVE_TIME_LIMIT = 268435456;

// Resolves the flat record list returned by dataHistorian get-data (packs concatenated, newest pack first)
// into the measurement entries used by the dashboard.
function resolveSenMLRecords(records, systemName, serviceName) {
  const entries = [];
  let baseName = '';
  let baseTime = 0;
  let baseUnit;
  for (const record of records || []) {
    if (record.bn !== undefined && record.bn !== null) baseName = record.bn;
    if (record.bt !== undefined && record.bt !== null) baseTime = record.bt;
    if (record.bu !== undefined && record.bu !== null) baseUnit = record.bu;
    const field = SENML_VALUE_FIELDS.find((name) => record[name] !== undefined && record[name] !== null);
    if (!field) continue;
    const name = `${baseName}${record.n || ''}`;
    const offset = record.t || 0;
    const time = Math.abs(offset) < SENML_RELATIVE_TIME_LIMIT ? baseTime + offset : offset;
    entries.push({
      id: `${systemName}|${serviceName}|${name}|${time}`,
      name, time,
      unit: record.u || baseUnit || null,
      value: { [field]: record[field] },
      record, system: systemName, service: serviceName
    });
  }
  return entries;
}

async function getDataManagerContent(limit = 240) {
  const safeLimit = Math.min(Math.max(Number(limit) || 240, 1), 1000);
  const { systems = [] } = await dataManagerRequest(`${DATA_HISTORIAN_BASE}/systems`);
  const pairs = (await Promise.all(systems.map(async (system) => {
    const { services = [] } = await dataManagerRequest(`${DATA_HISTORIAN_BASE}/systems/${encodeURIComponent(system)}/services`);
    return services.map((service) => [system, service]);
  }))).flat();
  const packs = await Promise.all(pairs.map(async ([system, service]) => {
    try {
      const records = await dataManagerRequest(
        `${DATA_HISTORIAN_BASE}/systems/${encodeURIComponent(system)}/services/${encodeURIComponent(service)}/data?count=${safeLimit}`);
      return resolveSenMLRecords(records, system, service);
    } catch (error) {
      if (error.status === 404) return [];
      throw error;
    }
  }));
  const entries = packs.flat().sort((a, b) => b.time - a.time).slice(0, safeLimit);
  const latestByName = new Map();
  for (const entry of entries) if (!latestByName.has(entry.name)) latestByName.set(entry.name, entry);
  const series = new Map();
  for (const entry of [...entries].reverse()) {
    const value = numericValue(entry);
    if (value === null) continue;
    if (!series.has(entry.name)) series.set(entry.name, []);
    series.get(entry.name).push({ time: entry.time, value, unit: entry.unit });
  }
  return {
    collectedAt: new Date().toISOString(),
    count: entries.length,
    latest: [...latestByName.values()].sort((a, b) => a.name.localeCompare(b.name)),
    entries,
    series: [...series].map(([name, points]) => ({ name, points }))
  };
}

async function listContainers() {
  const filters = encodeURIComponent(JSON.stringify({ label: [`com.docker.compose.project=${PROJECT_NAME}`] }));
  const response = await dockerRequest(`/containers/json?all=1&filters=${filters}`);
  const containers = JSON.parse(response.body.toString('utf8'));
  return containers.map((container) => {
    const service = container.Labels['com.docker.compose.service'] || 'unknown';
    const health = container.Status.match(/\((healthy|unhealthy|starting)\)/)?.[1] || null;
    return {
      id: container.Id.slice(0, 12),
      name: (container.Names[0] || '').replace(/^\//, ''),
      service,
      displayName: runtimeDisplayNames[service] || service,
      image: container.Image,
      state: container.State,
      status: container.Status,
      health,
      created: new Date(container.Created * 1000).toISOString(),
      managed: managedServices.has(service)
    };
  }).sort((a, b) => a.service.localeCompare(b.service));
}

async function arrowheadRequest(path, body) {
  const response = await fetch(`${SERVICE_REGISTRY_URL}${path}`, {
    method: body === undefined ? 'GET' : 'POST',
    headers: {
      Authorization: `Bearer SYSTEM//${ARROWHEAD_SYSTEM_NAME}`,
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' })
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    signal: AbortSignal.timeout(5000)
  });
  const text = await response.text();
  let payload = null;
  try { payload = text ? JSON.parse(text) : null; } catch { payload = { raw: text }; }
  if (!response.ok) {
    const error = new Error(payload?.errorMessage || `Arrowhead API ${response.status}`);
    error.status = response.status;
    error.payload = payload;
    throw error;
  }
  return payload;
}

async function getArrowheadInventory() {
  const [devices, systems] = await Promise.all([
    arrowheadRequest('/serviceregistry/mgmt/devices/query', {}),
    arrowheadRequest('/serviceregistry/mgmt/systems/query', {})
  ]);
  const providerNames = (systems.entries || []).map((entry) => entry.name);
  let services = { entries: [], count: 0 };
  if (providerNames.length) {
    services = await arrowheadRequest('/serviceregistry/mgmt/service-instances/query', { providerNames });
  }
  return { devices, systems, services };
}

function hasX509Identity(system) {
  const metadata = system?.metadata || {};
  return Object.entries(metadata).some(([key, value]) =>
    /x509.*public.*key/i.test(key) && typeof value === 'string' && value.length > 0);
}

function buildOrchestrationCatalog(systems, services) {
  const serviceEntries = services || [];
  const templatesBySystem = new Map();
  for (const service of serviceEntries) {
    const provider = service.provider?.name;
    if (!provider) continue;
    if (!templatesBySystem.has(provider)) templatesBySystem.set(provider, new Set());
    for (const intf of service.interfaces || []) {
      if (intf.templateName) templatesBySystem.get(provider).add(intf.templateName);
    }
  }

  const consumers = (systems || []).map((system) => ({
    name: system.name,
    version: system.version || null,
    x509: hasX509Identity(system),
    interfaceTemplates: [...(templatesBySystem.get(system.name) || [])].sort()
  })).sort((a, b) => a.name.localeCompare(b.name));

  const producers = serviceEntries.map((service) => {
    const provider = service.provider?.name || 'Unknown';
    const interfaces = (service.interfaces || []).map((intf) => ({
      templateName: intf.templateName,
      protocol: intf.protocol,
      policy: intf.policy
    }));
    const compatibleConsumers = consumers.filter((consumer) => {
      if (consumer.name === provider) return false;
      return interfaces.some((intf) => consumer.interfaceTemplates.includes(intf.templateName) &&
        (intf.policy !== 'CERT_AUTH' || consumer.x509));
    }).map((consumer) => consumer.name);
    return {
      instanceId: service.instanceId,
      provider,
      serviceDefinition: service.serviceDefinition?.name || 'Unknown',
      version: service.version,
      interfaces,
      compatibleConsumers
    };
  }).filter((producer) => producer.compatibleConsumers.length > 0)
    .sort((a, b) => a.serviceDefinition.localeCompare(b.serviceDefinition) || a.provider.localeCompare(b.provider));

  return { producers, consumers };
}

function validateRuleCandidate(catalog, candidate) {
  const consumer = typeof candidate?.consumer === 'string' ? candidate.consumer.trim() : '';
  const serviceInstanceId = typeof candidate?.serviceInstanceId === 'string' ? candidate.serviceInstanceId.trim() : '';
  const priority = Number(candidate?.priority);
  if (!consumer || !serviceInstanceId || !Number.isInteger(priority) || priority <= 0) {
    throw Object.assign(new Error('Consumer, service instance and a positive integer priority are required'), { status: 400 });
  }
  const producer = catalog.producers.find((entry) => entry.instanceId === serviceInstanceId);
  if (!producer) throw Object.assign(new Error('The selected producer is no longer available'), { status: 409 });
  if (!producer.compatibleConsumers.includes(consumer)) {
    throw Object.assign(new Error('The selected consumer is not interoperable with this producer'), { status: 409 });
  }
  return { consumer, serviceInstanceId, priority };
}

async function getOrchestrationRules() {
  const inventory = await getArrowheadInventory();
  const catalog = buildOrchestrationCatalog(inventory.systems.entries || [], inventory.services.entries || []);
  const consumerNames = catalog.consumers.map((consumer) => consumer.name);
  const rules = consumerNames.length
    ? await orchestrationRequest('/serviceorchestration/orchestration/mgmt/simple-store/query', {}, { consumerNames })
    : { entries: [], count: 0 };
  return { catalog, rules: rules.entries || [], count: rules.count || 0, collectedAt: new Date().toISOString() };
}

async function createOrchestrationRule(candidate) {
  const inventory = await getArrowheadInventory();
  const catalog = buildOrchestrationCatalog(inventory.systems.entries || [], inventory.services.entries || []);
  const normalized = validateRuleCandidate(catalog, candidate);
  return orchestrationRequest('/serviceorchestration/orchestration/mgmt/simple-store/create', {}, { candidates: [normalized] });
}

async function removeOrchestrationRule(id) {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id || '')) {
    throw Object.assign(new Error('Invalid orchestration rule id'), { status: 400 });
  }
  await orchestrationRequest(`/serviceorchestration/orchestration/mgmt/simple-store/remove?uuids=${encodeURIComponent(id)}`, { method: 'DELETE' });
  return { id, removed: true, at: new Date().toISOString() };
}

function systemStatus(system, containers, serviceCounts) {
  const composeService = systemToService[system.name] || null;
  const container = containers.find((item) => item.service === composeService);
  return {
    ...system,
    composeService,
    serviceCount: serviceCounts.get(system.name) || 0,
    runtime: container ? {
      state: container.state,
      status: container.status,
      health: container.health,
      managed: container.managed
    } : null
  };
}

function buildRegistryManagement(devices, systems, services, containers) {
  const servicesByProvider = new Map();
  for (const service of services) {
    const providerName = service.provider?.name || service.instanceId?.split('|')[0] || 'Unknown';
    if (!servicesByProvider.has(providerName)) servicesByProvider.set(providerName, []);
    servicesByProvider.get(providerName).push(service);
  }

  const deviceGroups = new Map((devices || []).map((device) => [device.name, {
    name: device.name,
    addresses: device.addresses || [],
    metadata: device.metadata || {},
    registered: true,
    systems: []
  }]));
  const unassigned = { name: 'No registered device', addresses: [], metadata: {}, registered: false, systems: [] };

  for (const system of systems) {
    const deviceName = system.device?.name || system.deviceName || null;
    const entry = {
      ...system,
      deviceName,
      deployed: system.runtime?.state === 'running',
      services: (servicesByProvider.get(system.name) || []).map((service) => ({
        ...service,
        deployed: system.runtime?.state === 'running'
      }))
    };
    if (deviceName) {
      if (!deviceGroups.has(deviceName)) {
        deviceGroups.set(deviceName, { name: deviceName, addresses: [], metadata: {}, registered: false, systems: [] });
      }
      deviceGroups.get(deviceName).systems.push(entry);
    } else {
      unassigned.systems.push(entry);
    }
  }

  const registeredComposeServices = new Set(systems.map((system) => system.composeService).filter(Boolean));
  const runtimeOnly = containers
    .filter((container) => container.managed && container.service !== 'mqtt-broker' && !registeredComposeServices.has(container.service))
    .map((container) => ({
      name: container.displayName || container.service,
      composeService: container.service,
      deployed: container.state === 'running',
      runtime: container,
      registered: false,
      services: []
    }));

  const groups = [...deviceGroups.values()]
    .map((device) => ({
      ...device,
      deployed: device.systems.some((system) => system.deployed),
      serviceCount: device.systems.reduce((count, system) => count + system.services.length, 0)
    }))
    .sort((left, right) => left.name.localeCompare(right.name));
  if (unassigned.systems.length) {
    groups.push({
      ...unassigned,
      deployed: unassigned.systems.some((system) => system.deployed),
      serviceCount: unassigned.systems.reduce((count, system) => count + system.services.length, 0)
    });
  }

  return {
    devices: groups,
    runtimeOnly,
    summary: {
      registeredDevices: devices.length,
      deployedDevices: groups.filter((device) => device.registered && device.deployed).length,
      registeredSystems: systems.length,
      deployedSystems: systems.filter((system) => system.runtime?.state === 'running').length,
      registeredServices: services.length,
      deployedServices: services.filter((service) => {
        const providerName = service.provider?.name || service.instanceId?.split('|')[0];
        return systems.some((system) => system.name === providerName && system.runtime?.state === 'running');
      }).length,
      runtimeOnly: runtimeOnly.length
    }
  };
}

function buildMessagingOverview(containers, services) {
  const runtime = (service) => {
    const container = containers.find((item) => item.service === service);
    return container ? {
      service: container.service,
      state: container.state,
      status: container.status,
      health: container.health,
      managed: container.managed
    } : null;
  };
  const serviceInstances = new Set(services.map((service) => service.instanceId));
  const running = (service) => runtime(service)?.state === 'running';
  const registered = (instanceId) => serviceInstances.has(instanceId);
  const definitionName = (service) => service.serviceDefinition?.name || service.instanceId?.split('|')[1];
  const providerName = (service) => service.provider?.name || service.instanceId?.split('|')[0];
  const sysMonitorProducers = services
    .filter((service) => definitionName(service) === 'sysMonitor')
    .map((service) => {
      const provider = providerName(service);
      const delegated = service.metadata?.publisherMode === 'delegated';
      const providerService = systemToService[provider];
      return {
        id: `sysmonitor-publish-${provider.toLowerCase().replace(/[^a-z0-9]+/g, '-')}`,
        source: provider,
        target: 'MQTT Broker',
        label: `sysMonitor.observe · ${delegated ? 'delegated' : 'native'}`,
        protocol: 'MQTTS · CERT_AUTH · QoS 1',
        active: running('mqtt-broker') && (delegated
          ? running('system-monitor-publisher')
          : (!providerService || running(providerService)))
      };
    })
    .sort((left, right) => left.source.localeCompare(right.source));
  return {
    infrastructure: [{
      id: 'mqtt-broker',
      name: 'MQTT Broker',
      kind: 'Messaging infrastructure',
      endpoint: 'mqtts://mqtt-broker:8883',
      protocol: 'MQTT 5',
      security: 'Mutual X.509 · ACL',
      runtime: runtime('mqtt-broker')
    }],
    orchestrations: [
      ...sysMonitorProducers,
      {
        id: 'sysmonitor-consume', source: 'MQTT Broker', target: 'SysMonitor',
        label: 'sysMonitor observation subscription', protocol: 'MQTTS · X.509',
        active: running('mqtt-broker') && running('sysmonitor')
      },
      {
        id: 'sysmonitor-orchestration', source: 'SysMonitor', target: 'DynamicServiceOrchestration',
        label: 'Pull sysMonitor.observe · 30s', protocol: 'HTTPS · token + mTLS',
        active: running('sysmonitor') && running('serviceorchestration-dynamic') && sysMonitorProducers.length > 0
      },
      {
        id: 'aigym-sysmonitor-data', source: 'SysMonitor', target: 'AIGym',
        label: 'sysMonitorData.subscribe', protocol: 'DSO + MQTTS · CERT_AUTH',
        active: running('aigym') && running('sysmonitor') &&
          registered('SysMonitor|sysMonitorData|1.0.0')
      },
      {
        id: 'sysmonitor-history', source: 'SysMonitor', target: 'LocalCloudManager',
        label: 'sysMonitorHistory.query · stored in SysMonitor', protocol: 'HTTPS · token + mTLS',
        active: running('sysmonitor') && registered('SysMonitor|sysMonitorHistory|1.0.0')
      }
    ]
  };
}

async function buildOverview() {
  const collectedAt = new Date().toISOString();
  const [containersResult, arrowheadResult] = await Promise.allSettled([
    listContainers(),
    getArrowheadInventory()
  ]);

  const containers = containersResult.status === 'fulfilled' ? containersResult.value : [];
  const inventory = arrowheadResult.status === 'fulfilled'
    ? arrowheadResult.value
    : { devices: { entries: [], count: 0 }, systems: { entries: [], count: 0 }, services: { entries: [], count: 0 } };
  const serviceCounts = new Map();
  for (const service of inventory.services.entries || []) {
    const providerName = service.provider?.name || 'Unknown';
    serviceCounts.set(providerName, (serviceCounts.get(providerName) || 0) + 1);
  }
  const systems = (inventory.systems.entries || []).map((system) => systemStatus(system, containers, serviceCounts));
  const registryManagement = buildRegistryManagement(
    inventory.devices?.entries || [], systems, inventory.services.entries || [], containers
  );
  const namingViolations = validateInventoryNames(inventory.systems.entries || [], inventory.services.entries || []);
  const messaging = buildMessagingOverview(containers, inventory.services.entries || []);
  const running = containers.filter((container) => container.state === 'running').length;
  const unhealthy = containers.filter((container) => container.health === 'unhealthy').length;
  const registeredComposeServices = new Set(systems.map((system) => system.composeService).filter(Boolean));
  const unregisteredServices = containers
    .filter((container) => container.managed && container.service !== 'mqtt-broker' &&
      container.state === 'running' && !registeredComposeServices.has(container.service))
    .map((container) => container.service);
  const sample = {
    at: collectedAt,
    running,
    containers: containers.length,
    registeredSystems: systems.length,
    registeredServices: inventory.services.count || 0,
    unhealthy,
    incidents: unhealthy + unregisteredServices.length + namingViolations.length,
    unregisteredServices
  };
  history.push(sample);
  if (history.length > HISTORY_LIMIT) history.shift();

  return {
    collectedAt,
    cloud: {
      name: PROJECT_NAME,
      release: process.env.ARROWHEAD_VERSION || '5.2.2',
      registryReachable: arrowheadResult.status === 'fulfilled',
      dockerReachable: containersResult.status === 'fulfilled'
    },
    summary: sample,
    devices: inventory.devices?.entries || [],
    systems,
    services: inventory.services.entries || [],
    registryManagement,
    naming: { valid: namingViolations.length === 0, violations: namingViolations },
    infrastructure: messaging.infrastructure,
    orchestrations: messaging.orchestrations,
    containers,
    history,
    registration: registrationState,
    errors: {
      docker: containersResult.status === 'rejected' ? containersResult.reason.message : null,
      arrowhead: arrowheadResult.status === 'rejected' ? arrowheadResult.reason.message : null,
      naming: namingViolations.length ? `Arrowhead naming violations: ${namingViolations.join(', ')}` : null
    }
  };
}

function demuxDockerLogs(buffer) {
  const lines = [];
  let offset = 0;
  while (offset + 8 <= buffer.length) {
    const length = buffer.readUInt32BE(offset + 4);
    if (offset + 8 + length > buffer.length) break;
    lines.push(buffer.subarray(offset + 8, offset + 8 + length).toString('utf8'));
    offset += 8 + length;
  }
  return (lines.length ? lines.join('') : buffer.toString('utf8')).replace(/\u0000/g, '');
}

async function containerForService(service) {
  const containers = await listContainers();
  return containers.find((container) => container.service === service) || null;
}

async function getLogs(service, tail) {
  const container = await containerForService(service);
  if (!container) throw Object.assign(new Error('Unknown project service'), { status: 404 });
  const response = await dockerRequest(`/containers/${container.id}/logs?stdout=1&stderr=1&timestamps=1&tail=${tail}`);
  return demuxDockerLogs(response.body).split('\n').filter(Boolean).slice(-tail);
}

async function performAction(service, action) {
  if (!managedServices.has(service)) {
    throw Object.assign(new Error('This service is not in the management allowlist'), { status: 403 });
  }
  if (!['start', 'stop', 'restart'].includes(action)) {
    throw Object.assign(new Error('Unsupported lifecycle action'), { status: 400 });
  }
  const container = await containerForService(service);
  if (!container) throw Object.assign(new Error('Unknown project service'), { status: 404 });
  const suffix = action === 'stop' ? '/stop?t=20' : action === 'restart' ? '/restart?t=20' : '/start';
  await dockerRequest(`/containers/${container.id}${suffix}`, { method: 'POST' });
  return { service, action, accepted: true, at: new Date().toISOString() };
}

let sysMonitorPublisher = null;

// Native sysMonitor publisher; liveness is degraded while the Docker API cannot be reached.
function getSysMonitorPublisher() {
  if (!sysMonitorPublisher) {
    sysMonitorPublisher = new SysMonitorPublisher({
      systemName: ARROWHEAD_SYSTEM_NAME,
      localCloud: process.env.SYSMONITOR_LOCAL_CLOUD || 'TestCloud',
      host: process.env.SYSMONITOR_BROKER_ADDRESS || 'mqtt-broker',
      port: Number(process.env.SYSMONITOR_BROKER_PORT || 8883),
      ca: fs.readFileSync(TLS_CA_CERT),
      cert: fs.readFileSync(TLS_CLIENT_CERT),
      key: fs.readFileSync(TLS_CLIENT_KEY),
      health: async () => {
        try {
          const containers = await listContainers();
          return [true, `Docker API reachable; ${containers.length} containers`];
        } catch (error) {
          return [false, `Docker API unavailable: ${error.message}`];
        }
      },
      serviceCount: () => (registrationState.instanceIds || []).length
    });
  }
  return sysMonitorPublisher;
}

async function registerMicrosystem() {
  try {
    await arrowheadRequest('/serviceregistry/system-discovery/register', {
      metadata: { role: 'local-cloud-monitor', managedProject: PROJECT_NAME },
      version: '1.0.0',
      addresses: ['cloud-monitor-api'],
      deviceName: null
    }, ARROWHEAD_SYSTEM_NAME);

    const registered = await arrowheadRequest('/serviceregistry/service-discovery/register', {
      serviceDefinitionName: 'localCloudManagement',
      version: '1.0.0',
      expiresAt: null,
      metadata: { description: 'Local cloud health, inventory, logs and lifecycle management' },
      interfaces: [{
        templateName: 'generic_http',
        protocol: 'http',
        policy: 'NONE',
        properties: {
          accessAddresses: ['cloud-monitor-api'],
          accessPort: PORT,
          basePath: '/api',
          operations: {
            overview: { method: 'GET', path: '/overview' },
            serviceRegistryManagement: { method: 'GET', path: '/service-registry-management' },
            dataManager: { method: 'GET', path: '/datamanager' },
            orchestrationRules: { method: 'GET', path: '/orchestration/rules' },
            createOrchestrationRule: { method: 'POST', path: '/orchestration/rules' },
            removeOrchestrationRule: { method: 'DELETE', path: '/orchestration/rules/{id}' },
            logs: { method: 'GET', path: '/logs' },
            lifecycle: { method: 'POST', path: '/containers/{service}/{action}' }
          }
        }
      }]
    }, ARROWHEAD_SYSTEM_NAME);
    const monitor = await arrowheadRequest('/serviceregistry/service-discovery/register',
      getSysMonitorPublisher().serviceRegistration(), ARROWHEAD_SYSTEM_NAME);
    registrationState = {
      registered: true,
      instanceId: registered.instanceId,
      instanceIds: [registered.instanceId, monitor.instanceId],
      error: null,
      checkedAt: new Date().toISOString()
    };
    console.log(`Registered ${ARROWHEAD_SYSTEM_NAME} as ${registered.instanceId}`);
  } catch (error) {
    registrationState = { registered: false, error: error.message, checkedAt: new Date().toISOString() };
    console.error(`Arrowhead registration failed: ${error.message}`);
    setTimeout(registerMicrosystem, 10000).unref();
  }
}

async function route(req, res) {
  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  if (req.method === 'GET' && url.pathname === '/health') {
    return jsonResponse(res, 200, { status: 'ok', system: ARROWHEAD_SYSTEM_NAME, registration: registrationState });
  }
  if (req.method === 'GET' && url.pathname === '/api/overview') {
    return jsonResponse(res, 200, await buildOverview());
  }
  if (req.method === 'GET' && url.pathname === '/api/service-registry-management') {
    const overview = await buildOverview();
    return jsonResponse(res, 200, {
      collectedAt: overview.collectedAt,
      cloud: overview.cloud,
      ...overview.registryManagement,
      errors: overview.errors
    });
  }
  if (req.method === 'GET' && url.pathname === '/api/datamanager') {
    const limit = url.searchParams.get('limit') || 240;
    return jsonResponse(res, 200, await getDataManagerContent(limit));
  }
  if (req.method === 'GET' && url.pathname === '/api/sysmonitor') {
    return jsonResponse(res, 200, await getSysMonitorHistory(url.searchParams.get('system') || '', url.searchParams.get('limit') || 120));
  }
  if (req.method === 'GET' && url.pathname === '/api/orchestration/rules') {
    return jsonResponse(res, 200, await getOrchestrationRules());
  }
  if (req.method === 'POST' && url.pathname === '/api/orchestration/rules') {
    if (req.headers['x-arrowhead-action'] !== ACTION_CONFIRMATION) {
      return jsonResponse(res, 428, { error: 'Missing action confirmation header' });
    }
    return jsonResponse(res, 201, await createOrchestrationRule(await readJson(req)));
  }
  const ruleMatch = url.pathname.match(/^\/api\/orchestration\/rules\/([0-9a-f-]+)$/i);
  if (req.method === 'DELETE' && ruleMatch) {
    if (req.headers['x-arrowhead-action'] !== ACTION_CONFIRMATION) {
      return jsonResponse(res, 428, { error: 'Missing action confirmation header' });
    }
    return jsonResponse(res, 200, await removeOrchestrationRule(ruleMatch[1]));
  }
  if (req.method === 'GET' && url.pathname === '/api/logs') {
    const service = url.searchParams.get('service') || '';
    const tail = Math.min(Math.max(Number(url.searchParams.get('tail') || 150), 1), 500);
    return jsonResponse(res, 200, { service, lines: await getLogs(service, tail), collectedAt: new Date().toISOString() });
  }
  const actionMatch = url.pathname.match(/^\/api\/containers\/([a-z0-9-]+)\/(start|stop|restart)$/);
  if (req.method === 'POST' && actionMatch) {
    if (req.headers['x-arrowhead-action'] !== ACTION_CONFIRMATION) {
      return jsonResponse(res, 428, { error: 'Missing action confirmation header' });
    }
    await readJson(req);
    return jsonResponse(res, 202, await performAction(actionMatch[1], actionMatch[2]));
  }
  jsonResponse(res, 404, { error: 'Not found' });
}

const server = http.createServer((req, res) => {
  route(req, res).catch((error) => {
    console.error(error);
    if (!res.headersSent) jsonResponse(res, error.status || 500, { error: error.message });
    else res.end();
  });
});

if (require.main === module) {
  server.listen(PORT, '0.0.0.0', () => {
    console.log(`Arrowhead local-cloud manager API listening on ${PORT}`);
    registerMicrosystem();
    getSysMonitorPublisher().start();
  });
}

module.exports = {
  demuxDockerLogs,
  systemStatus,
  managedServices,
  systemToService,
  numericValue,
  resolveSenMLRecords,
  buildMessagingOverview,
  buildRegistryManagement,
  buildOrchestrationCatalog,
  validateRuleCandidate,
  validateInventoryNames,
  runtimeDisplayNames,
  server
};
