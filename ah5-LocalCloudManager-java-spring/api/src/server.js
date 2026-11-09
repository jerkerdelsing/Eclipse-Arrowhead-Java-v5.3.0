'use strict';

const http = require('node:http');
const crypto = require('node:crypto');
const https = require('node:https');
const fs = require('node:fs');
const { URL } = require('node:url');

const PORT = Number(process.env.PORT || 8491);
// Docker is reached only through the whitelisting docker-proxy (manager/docker-proxy); the API never holds the socket.
const DOCKER_API_URL = new URL(process.env.DOCKER_API_URL || 'http://docker-proxy:2375');
const PROJECT_NAME = process.env.COMPOSE_PROJECT_NAME || 'arrowhead-v52-test-cloud';
// The ServiceRegistry is the only configured Arrowhead endpoint. Authentication and the orchestrator are looked up in it
// (an identity token is needed before anything can be orchestrated); every other provider is selected by an
// orchestration request on behalf of LocalCloudManager (orchestratedRequest).
const SERVICE_REGISTRY_URL = process.env.SERVICE_REGISTRY_URL || 'https://serviceregistry:8443';
const ORCHESTRATOR_SYSTEM = process.env.ORCHESTRATOR_SYSTEM || 'DynamicServiceOrchestration';
const ORCHESTRATION_TTL_MS = Number(process.env.ORCHESTRATION_TTL_SECONDS || 300) * 1000;
const { SysMonitorPublisher } = require('./sysmonitor');
const { ConsoleAuth, secretEnv } = require('./console-auth');
const { crlStatus } = require('./crl-status');
const { createCloudConfigurationRoutes } = require('./cloud-config-api');
const LOCAL_CLOUD_CONFIGURATOR_URL = process.env.LOCAL_CLOUD_CONFIGURATOR_URL || '';
const { createSoSRoutes } = require('./sos-api');
const { createFloorplanRoutes } = require('./floorplans-api');
const CHIPLET_FLOORPLANS_ENABLED = process.env.CHIPLET_FLOORPLANS_ENABLED === 'true';
const AUTHENTICATION_PASSWORD = secretEnv('AUTHENTICATION_PASSWORD') || '';
const TLS_CA_CERT = process.env.TLS_CA_CERT || '/certs/ca.crt';
const TLS_CLIENT_CERT = process.env.TLS_CLIENT_CERT || '/certs/local-cloud-manager.crt';
const TLS_CLIENT_KEY = process.env.TLS_CLIENT_KEY || '/certs/local-cloud-manager.key';
// Revocation (J4): the providers' certificates are checked against this CRL of the cloud CA (fail closed when set).
const TLS_CRL_FILE = process.env.TLS_CRL_FILE || '';
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
  'graphdbmanager',
  'timemanager',
  'eventhandler',
  'configuration',
  'choreographer',
  'random-data-producer',
  'sysmonitor',
  'aigym',
  'certificate-authority',
  'mqtt-broker',
  'amqp-broker',
  'gatekeeper',
  'gatetunnel'
].join(',')).split(',').map((value) => value.trim()).filter(Boolean));

const systemToService = {
  ChipletFloorplanGenerator: 'chiplet-floorplan-generator',
  ResourceRegistry: 'resource-registry',
  ResourceOrchestration: 'resource-orchestration',
  ResourceScheduler: 'resource-scheduler',
  NetworkManager: 'network-manager',
  ServiceRegistry: 'serviceregistry',
  DynamicServiceOrchestration: 'serviceorchestration-dynamic',
  SimpleStoreServiceOrchestration: 'serviceorchestration-simple',
  ConsumerAuthorization: 'consumerauthorization',
  Authentication: 'authentication',
  Blacklist: 'blacklist',
  TranslationManager: 'translation-manager',
  TestTranslation: 'test-translation',
  DeviceQoSEvaluator: 'device-qos-evaluator',
  Onboarding: 'onboarding',
  DataManager: 'datamanager',
  GraphDBManager: 'graphdbmanager',
  TimeManager: 'timemanager',
  EventHandler: 'eventhandler',
  PlantDescriptionEngine: 'plantdescriptionengine',
  Configuration: 'configuration',
  Choreographer: 'choreographer',
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
  'amqp-broker': 'AMQPBroker',
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
      host: DOCKER_API_URL.hostname,
      port: DOCKER_API_URL.port || 80,
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

// The agents keep their TLS context; rebuild them when the CRL file changes, so a revocation applies to new connections.
let crlStamp = null;
function refreshAgentsOnCrlChange() {
  if (!TLS_CRL_FILE) return;
  let stamp;
  try { stamp = fs.statSync(TLS_CRL_FILE).mtimeMs; } catch { stamp = 'missing'; }
  if (stamp !== crlStamp) {
    crlStamp = stamp;
    dataManagerAgent?.destroy();
    sysMonitorAgent?.destroy();
    dataManagerAgent = null;
    sysMonitorAgent = null;
  }
}

function crlOption() {
  return TLS_CRL_FILE ? { crl: fs.readFileSync(TLS_CRL_FILE) } : {};
}

function getDataManagerAgent() {
  refreshAgentsOnCrlChange();
  if (!dataManagerAgent) {
    dataManagerAgent = new https.Agent({
      ca: fs.readFileSync(TLS_CA_CERT),
      cert: fs.readFileSync(TLS_CLIENT_CERT),
      key: fs.readFileSync(TLS_CLIENT_KEY),
      ...crlOption(),
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
      timeout: 15000
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
    req.on('timeout', () => req.destroy(new Error(`Arrowhead request to ${new URL(target).hostname} timed out`)));
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
    identityLoginPromise = identityOperation('identity-login').then((login) => httpsJson(login.url, { method: login.method }, {
      systemName: ARROWHEAD_SYSTEM_NAME, credentials: { password: AUTHENTICATION_PASSWORD }
    })).then((result) => {
      identityToken = result.token;
      identityRefreshAt = Date.now() + 240000;
      return identityToken;
    }).catch((error) => {
      if (!error.status) identityService = null; // Authentication unreachable: look it up again
      throw error;
    }).finally(() => {
      identityLoginPromise = null;
    });
  }
  return identityLoginPromise;
}

async function authorizationHeader() {
  return `Bearer IDENTITY-TOKEN//${await getIdentityToken()}`;
}

function httpsProperties(service) {
  return (service?.interfaces || []).find((intf) => intf.templateName === 'generic_https')?.properties || null;
}

// { url, origin, basePath, method } of one operation of a generic_https interface
function serviceOperation(properties, operation, provider) {
  const op = properties?.operations?.[operation];
  const address = properties?.accessAddresses?.[0];
  if (!op || !address) throw Object.assign(new Error(`${provider || 'Provider'} does not offer ${operation}`), { status: 502 });
  const origin = `https://${address}:${properties.accessPort}`;
  const basePath = properties.basePath || '';
  return { url: `${origin}${basePath}${op.path}`, origin, basePath, path: op.path, method: op.method || 'POST', provider };
}

// Authentication's identity service; the ServiceRegistry answers this lookup without a token (unrestrictedDiscovery).
let identityService = null;
async function identityOperation(operation) {
  if (!identityService) {
    const found = await httpsJson(`${SERVICE_REGISTRY_URL}/serviceregistry/service-discovery/lookup`, { method: 'POST' },
      { serviceDefinitionNames: ['identity'], providerNames: ['Authentication'], interfaceTemplateNames: ['generic_https'] });
    identityService = httpsProperties((found?.entries || []).find(httpsProperties));
    if (!identityService) throw Object.assign(new Error('Authentication is not registered in the ServiceRegistry'), { status: 503 });
  }
  return serviceOperation(identityService, operation, 'Authentication');
}

// Authentication keeps one session per system, so another LocalCloudManager login (for example a smoke
// test) invalidates the cached token. A 401 therefore forces one fresh login and retry.
async function withIdentity(call) {
  try {
    return await call(await authorizationHeader());
  } catch (error) {
    if (error.status !== 401) throw error;
    identityToken = null;
    identityRefreshAt = 0;
    return call(await authorizationHeader());
  }
}

let orchestratorPull = null;
async function orchestratorOperation() {
  if (!orchestratorPull) {
    const found = await arrowheadRequest('/serviceregistry/service-discovery/lookup',
      { serviceDefinitionNames: ['serviceOrchestration'], providerNames: [ORCHESTRATOR_SYSTEM], interfaceTemplateNames: ['generic_https'] });
    const properties = httpsProperties((found?.entries || []).find(httpsProperties));
    if (!properties) throw Object.assign(new Error('No orchestrator is registered in the ServiceRegistry'), { status: 503 });
    orchestratorPull = serviceOperation(properties, 'pull', ORCHESTRATOR_SYSTEM);
  }
  return orchestratorPull;
}

// An orchestration request on behalf of LocalCloudManager: the orchestrator returns only providers that LocalCloudManager
// is authorized to consume the operation of. Results are reused for ORCHESTRATION_TTL_SECONDS.
const orchestrations = new Map();
// Concurrent requests for the same service and operation share one orchestration pull (a page such as SysMonitor's
// asks for the same provider once per system at the same time).
const pendingOrchestrations = new Map();
async function orchestrate(serviceDefinition, operation, provider) {
  const key = `${serviceDefinition}|${operation}|${provider || ''}`;
  const known = orchestrations.get(key);
  if (known && known.expiresAt > Date.now()) return known.target;
  let pending = pendingOrchestrations.get(key);
  if (!pending) {
    pending = orchestrateNow(key, serviceDefinition, operation, provider).finally(() => pendingOrchestrations.delete(key));
    pendingOrchestrations.set(key, pending);
  }
  return pending;
}
async function orchestrateNow(key, serviceDefinition, operation, provider) {
  const pull = await orchestratorOperation();
  // no securityPolicies filter: the core systems register their interfaces with policy NONE (they check mTLS and tokens themselves)
  const serviceRequirement = { serviceDefinition, operations: [operation], interfaceTemplateNames: ['generic_https'] };
  const orchestrationFlags = { MATCHMAKING: true, ALLOW_INTERCLOUD: false, ALLOW_TRANSLATION: false };
  if (provider) {
    serviceRequirement.preferredProviders = [provider];
    orchestrationFlags.ONLY_PREFERRED = true;
  }
  let response;
  try {
    response = await withIdentity((authorization) => httpsJson(pull.url, { method: pull.method, headers: { Authorization: authorization } },
      { serviceRequirement, orchestrationFlags }));
  } catch (error) {
    if (!error.status) orchestratorPull = null;
    throw error;
  }
  const result = (response?.results || []).find((entry) => httpsProperties(entry)?.operations?.[operation]);
  if (!result) {
    throw Object.assign(new Error(`Orchestration found no authorized provider of ${serviceDefinition} (${operation})`), { status: 503 });
  }
  const target = serviceOperation(httpsProperties(result), operation, result.providerName || provider);
  orchestrations.set(key, { target, expiresAt: Date.now() + ORCHESTRATION_TTL_MS });
  return target;
}

// Consumes an operation of an orchestrated provider. path is relative to the service's base path (default: the
// operation's own path); path parameters and query strings are filled in by the caller.
async function orchestratedRequest(serviceDefinition, operation, { path, method, body, provider, agent } = {}) {
  const target = await orchestrate(serviceDefinition, operation, provider);
  try {
    return await systemRequest(target.origin, target.provider || serviceDefinition, `${target.basePath}${path ?? target.path}`,
      agent || getDataManagerAgent(), method || target.method, body);
  } catch (error) {
    if (!error.status || error.status >= 500) orchestrations.delete(`${serviceDefinition}|${operation}|${provider || ''}`);
    throw error;
  }
}

// ServiceOrchestration's simple-store rules (serviceOrchestrationSimpleStoreManagement)
async function orchestrationRequest(operation, body, path) {
  return orchestratedRequest('serviceOrchestrationSimpleStoreManagement', operation, { body, path });
}

// dataHistorian of the DataManager
async function dataManagerRequest(operation, path) {
  return orchestratedRequest('dataHistorian', operation, { path });
}

// plantDescriptionManagement of the PlantDescriptionEngine
async function plantDescriptionRequest(operation, path, body) {
  return orchestratedRequest('plantDescriptionManagement', operation, { path, body });
}

// Plant descriptions plus the registered systems and services, which the editor offers as choices.
async function getPlantDescriptions() {
  const [descriptions, inventory] = await Promise.allSettled([
    plantDescriptionRequest('query', '/plant-descriptions?page=0&size=200'),
    getArrowheadInventory()
  ]);
  if (descriptions.status !== 'fulfilled') throw descriptions.reason;
  const registry = inventory.status === 'fulfilled' ? inventory.value : { systems: { entries: [] }, services: { entries: [] } };
  const services = (registry.services.entries || []).map((service) => ({
    provider: service.provider?.name || service.instanceId?.split('|')[0],
    serviceDefinition: service.serviceDefinition?.name || service.instanceId?.split('|')[1],
    interfaces: [...new Set((service.interfaces || []).map((intf) => intf.templateName).filter(Boolean))]
  }));
  return {
    collectedAt: new Date().toISOString(),
    entries: descriptions.value?.entries || [],
    count: descriptions.value?.count ?? descriptions.value?.entries?.length ?? 0,
    systems: (registry.systems.entries || []).map((system) => system.name).sort(),
    services,
    registryError: inventory.status === 'rejected' ? inventory.reason.message : null
  };
}

// sysMonitorManagement and sysMonitorHistory of SysMonitor
async function sysMonitorRequest(serviceDefinition, operation, path, body) {
  return orchestratedRequest(serviceDefinition, operation, { path, body, agent: getSysMonitorAgent() });
}

// sysMonitorManagement: the sysMonitor providers SysMonitor found, and which of their observation types it receives.
async function getSysMonitorSubscriptions() {
  const [providers, subscriptions, status] = await Promise.all([
    sysMonitorRequest('sysMonitorManagement', 'providers'),
    sysMonitorRequest('sysMonitorManagement', 'subscriptions'),
    sysMonitorRequest('sysMonitorManagement', 'status')
  ]);
  return {
    collectedAt: new Date().toISOString(),
    defaultSubscription: providers.defaultSubscription,
    providers: providers.list || [],
    subscriptions: subscriptions.list || [],
    intake: status.amqp || null
  };
}

let sysMonitorAgent = null;
// SysMonitor serves each request on its own connection; a small pool keeps bursts within its capacity.
function getSysMonitorAgent() {
  refreshAgentsOnCrlChange();
  if (!sysMonitorAgent) {
    sysMonitorAgent = new https.Agent({
      ca: fs.readFileSync(TLS_CA_CERT),
      cert: fs.readFileSync(TLS_CLIENT_CERT),
      key: fs.readFileSync(TLS_CLIENT_KEY),
      ...crlOption(),
      minVersion: 'TLSv1.2',
      rejectUnauthorized: true,
      maxSockets: 4
    });
  }
  return sysMonitorAgent;
}

// GET against an Arrowhead provider with the LocalCloudManager identity token and X.509 client certificate.
async function systemRequest(baseUrl, label, path, agent = getDataManagerAgent(), method = 'GET', body) {
  try {
    return await systemRequestOnce(baseUrl, label, path, agent, method, body);
  } catch (error) {
    if (error.status !== 401) throw error;
    identityToken = null;
    identityRefreshAt = 0;
    return systemRequestOnce(baseUrl, label, path, agent, method, body);
  }
}

async function systemRequestOnce(baseUrl, label, path, agent, method = 'GET', body) {
  const authorization = await authorizationHeader();
  const data = body === undefined ? null : Buffer.from(JSON.stringify(body));
  return new Promise((resolve, reject) => {
    const target = new URL(path, baseUrl);
    const req = https.request(target, {
      agent,
      method,
      headers: { Authorization: authorization, ...(data ? { 'Content-Type': 'application/json', 'Content-Length': data.length } : {}) },
      timeout: 15000
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
    req.end(data);
  });
}

function numericValue(entry) {
  for (const field of ['v', 's']) {
    if (typeof entry?.value?.[field] === 'number') return entry.value[field];
  }
  return null;
}


// sysMonitor observations of every microsystem are stored by SysMonitor (sysMonitorHistory), not by DataManager.
async function getSysMonitorHistory(system = '', limit = 120) {
  const safeLimit = Math.min(Math.max(Number(limit) || 120, 1), 1000);
  const { systems = [], retentionDays = null } = await sysMonitorRequest('sysMonitorHistory', 'list-systems');
  const rows = await Promise.all(systems.map(async (item) => {
    try {
      const { latest = {} } = await sysMonitorRequest('sysMonitorHistory', 'latest',
        `/systems/${encodeURIComponent(item.systemName)}/latest`);
      return { ...item, latest };
    } catch (error) {
      // One unreachable system must not hide the others.
      return { ...item, latest: {}, error: error.status === 404 ? null : error.message };
    }
  }));
  const selectedSystem = systems.some((item) => item.systemName === system) ? system : (rows[0]?.systemName || '');
  const { observations = [] } = selectedSystem
    ? await sysMonitorRequest('sysMonitorHistory', 'query', `/systems/${encodeURIComponent(selectedSystem)}/observations?count=${safeLimit}`)
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
  const { systems = [] } = await dataManagerRequest('list-systems');
  const pairs = (await Promise.all(systems.map(async (system) => {
    const { services = [] } = await dataManagerRequest('list-services', `/systems/${encodeURIComponent(system)}/services`);
    return services.map((service) => [system, service]);
  }))).flat();
  const packs = await Promise.all(pairs.map(async ([system, service]) => {
    try {
      const records = await dataManagerRequest('get-data',
        `/systems/${encodeURIComponent(system)}/services/${encodeURIComponent(service)}/data?count=${safeLimit}`);
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
  const meta = new Map();
  for (const entry of [...entries].reverse()) {
    const value = numericValue(entry);
    if (value === null) continue;
    if (!series.has(entry.name)) series.set(entry.name, []);
    if (!meta.has(entry.name)) meta.set(entry.name, seriesLabel(entry));
    series.get(entry.name).push({ time: entry.time, value, unit: entry.unit });
  }
  return {
    collectedAt: new Date().toISOString(),
    count: entries.length,
    latest: [...latestByName.values()].sort((a, b) => a.name.localeCompare(b.name)),
    entries,
    series: [...series].map(([name, points]) => ({ name, points, ...meta.get(name) }))
  };
}

// A stream is labelled by the stored system/service and, for inter-cloud data, the source cloud: inter-cloud
// producers use the SenML base name urn:arrowhead:<SourceCloud>:<producer>: (local data has no cloud segment).
function seriesLabel(entry) {
  const match = /^urn:arrowhead:([A-Z][A-Za-z0-9]*):/.exec(entry.record?.bn || entry.name || '');
  const sourceCloud = match ? match[1] : null;
  const measurement = String(entry.record?.n || entry.name || '').split(':').pop();
  return {
    system: entry.system, service: entry.service, sourceCloud,
    label: `${entry.system} · ${measurement} — ${sourceCloud ? `from ${sourceCloud} (inter-cloud)` : 'local'}`
  };
}

// Relay link and session status: the management status of Gatekeeper / GateTunnel (gatekeeperManagement /
// gateTunnelManagement status, GET /<system>/mgmt/status over mTLS).
async function gatekeeperStatus() {
  return orchestratedRequest('gatekeeperManagement', 'status');
}
async function gateTunnelStatus() {
  return orchestratedRequest('gateTunnelManagement', 'status');
}

async function settled(promise) {
  try { return { ok: true, value: await promise }; } catch (error) { return { ok: false, error: error.message }; }
}


// Inter-cloud panel: local cloud, neighbour clouds, relay links of Gatekeeper and GateTunnel, GateTunnel sessions and
// the inter-cloud publishing of RandomDataProducer.
async function getInterCloudContent() {
  const [gatekeeper, gatetunnel, clouds, producer] = await Promise.all([
    settled(gatekeeperStatus()),
    settled(gateTunnelStatus()),
    settled(orchestratedRequest('gatekeeperManagement', 'clouds')),
    settled(orchestratedRequest('randomNumberProducer', 'latest'))
  ]);
  return {
    collectedAt: new Date().toISOString(),
    localCloud: process.env.SYSMONITOR_LOCAL_CLOUD || 'TestCloud',
    neighbourClouds: clouds.ok ? (clouds.value?.entries || []).map((c) => `${c.name}|${c.operator}`) : [],
    neighbourError: clouds.ok ? null : clouds.error,
    relay: {
      gatekeeper: gatekeeper.ok ? gatekeeper.value.relay : { connected: false, error: gatekeeper.error },
      gatetunnel: gatetunnel.ok ? gatetunnel.value.relay : { connected: false, error: gatetunnel.error }
    },
    sessions: gatetunnel.ok ? gatetunnel.value.interCloud : null,
    producer: producer.ok ? producer.value.interCloud : { status: 'unavailable', lastError: producer.error }
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

// ServiceRegistry requires the dashboard's identity token, bound to its client certificate (mutual TLS).
async function arrowheadRequest(path, body) {
  // a 401 (session ended by Authentication, e.g. an identity update) is retried once with a fresh login
  return withIdentity((authorization) => httpsJson(`${SERVICE_REGISTRY_URL}${path}`,
    { method: body === undefined ? 'GET' : 'POST', headers: { Authorization: authorization } }, body));
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

  for (const system of systems || []) {
    const declared = system.metadata?.consumerInterfaceTemplates;
    const values = typeof declared === 'string' ? declared.split(',').map(s=>s.trim()) : [];
    if (!templatesBySystem.has(system.name)) templatesBySystem.set(system.name,new Set());
    for (const template of values) if (/^[a-z0-9_]+$/.test(template)) templatesBySystem.get(system.name).add(template);
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
    ? await orchestrationRequest('query', { consumerNames })
    : { entries: [], count: 0 };
  const dynamic = await settled(getDynamicOrchestrations());
  return { catalog, rules: rules.entries || [], count: rules.count || 0,
    dynamic: dynamic.ok ? dynamic.value : [], dynamicError: dynamic.ok ? null : dynamic.error, collectedAt: new Date().toISOString() };
}

// DynamicServiceOrchestration history (serviceOrchestrationHistoryManagement.query): the latest jobs aggregated per
// consumer and service. The job message names the providers: "1 local result: DataManager" or
// "1 inter-cloud result: DataManager@GoCloud|Company".
async function getDynamicOrchestrations(size = 500) {
  const history = await orchestratedRequest('serviceOrchestrationHistoryManagement', 'query',
    { body: { pagination: { page: 0, size, direction: 'DESC', sortField: 'createdAt' } }, provider: ORCHESTRATOR_SYSTEM });
  const groups = new Map();
  for (const job of history.entries || []) {
    const consumer = job.targetSystem || job.requesterSystem;
    const jobKind = /inter-cloud/.test(job.message || '') ? 'inter-cloud' : 'local';
    const key = `${consumer}|${job.serviceDefinition}|${jobKind}`;
    if (!groups.has(key)) groups.set(key, { consumer, serviceDefinition: job.serviceDefinition, count: 0, providers: null,
      kind: null, lastAt: job.finishedAt || job.createdAt, lastStatus: job.status, lastMessage: job.message, type: job.type });
    const group = groups.get(key);
    group.count += 1;
    const match = /^(\d+) (local|inter-cloud) result(?:: (.*))?$/.exec(job.message || '');
    if (group.providers === null && match && match[3]) {
      group.kind = match[2];
      group.providers = match[3].split(', ').map((item) => {
        const [provider, cloud] = item.split('@');
        return { provider, cloud: cloud || null };
      });
    }
  }
  // inter-cloud consumers keep their GateTunnel session and orchestrate only when it fails: add the active sessions
  const tunnel = await settled(gateTunnelStatus());
  for (const s of tunnel.ok ? tunnel.value.interCloud?.sessions || [] : []) {
    if (s.side !== 'consumer') continue;
    const key = `${s.consumer}|${s.serviceDefinition}|inter-cloud`;
    const group = groups.get(key) || { consumer: s.consumer, serviceDefinition: s.serviceDefinition, count: 0, type: 'PULL' };
    groups.set(key, { ...group, kind: 'inter-cloud', providers: [{ provider: s.provider, cloud: s.providerCloud }],
      lastAt: group.lastAt || new Date(s.createdAt * 1000).toISOString(), lastStatus: `session active (${s.requests} requests)`,
      lastMessage: group.lastMessage || 'GateTunnel session' });
  }
  return [...groups.values()].map((group) => ({ ...group, providers: group.providers || [] }))
    .sort((a, b) => a.consumer.localeCompare(b.consumer) || a.serviceDefinition.localeCompare(b.serviceDefinition));
}

async function authorizationPolicies(level) {
  const result = await orchestratedRequest('authorizationManagement', 'query-policies',
    { body: { pagination: { page: 0, size: 1000 }, level } });
  return result.entries || [];
}

function policyText(policy) {
  if (!policy) return '—';
  if (policy.policyType === 'ALL') return 'everyone';
  if (policy.policyType === 'WHITELIST') return (policy.policyList || []).join(', ') || 'nobody';
  if (policy.policyType === 'BLACKLIST') return `everyone except ${(policy.policyList || []).join(', ')}`;
  return policy.policyType;
}

// Service consumers: per provider service, who may consume it (ConsumerAuthorization, also consumers of other clouds)
// and who is orchestrated to it (DynamicServiceOrchestration history, SimpleStore rules).
async function getServiceConsumers() {
  const localCloud = process.env.SYSMONITOR_LOCAL_CLOUD || 'TestCloud';
  const [mgmt, provider, dynamic, rules, plant] = await Promise.all([
    settled(authorizationPolicies('MGMT')), settled(authorizationPolicies('PROVIDER')),
    settled(getDynamicOrchestrations()), settled(getOrchestrationRulesOnly()),
    settled(orchestratedRequest("serviceOrchestrationPlantManagement", "query", { provider: "DynamicServiceOrchestration", body: {} }))
  ]);
  const rows = new Map();
  const row = (providerName, service, cloud) => {
    const key = `${providerName}|${service}|${cloud}`;
    if (!rows.has(key)) rows.set(key, { provider: providerName, serviceDefinition: service, providerCloud: cloud,
      authorized: [], orchestrated: [] });
    return rows.get(key);
  };
  for (const policy of [...(mgmt.ok ? mgmt.value : []), ...(provider.ok ? provider.value : [])]) {
    if (policy.targetType !== 'SERVICE_DEF') continue;
    const consumerCloud = !policy.cloud || policy.cloud === 'LOCAL' ? localCloud : policy.cloud.split('|')[0];
    const entry = row(policy.provider, policy.target, localCloud);
    entry.authorized.push({ consumerCloud, level: policy.level, consumers: policyText(policy.defaultPolicy),
      scoped: Object.entries(policy.scopedPolicies || {}).map(([scope, p]) => `${scope}: ${policyText(p)}`) });
  }
  for (const group of dynamic.ok ? dynamic.value : []) {
    for (const p of group.providers) {
      const cloud = p.cloud ? p.cloud.split('|')[0] : localCloud;
      row(p.provider, group.serviceDefinition, cloud).orchestrated.push({ consumer: group.consumer, consumerCloud: localCloud,
        via: p.cloud ? 'DynamicServiceOrchestration (inter-cloud, GateTunnel)' : 'DynamicServiceOrchestration',
        count: group.count, lastAt: group.lastAt });
    }
  }
  // consumers of other clouds that use a local provider through a GateTunnel session
  const tunnel = await settled(gateTunnelStatus());
  for (const s of tunnel.ok ? tunnel.value.interCloud?.sessions || [] : []) {
    if (s.side === 'provider') row(s.provider, s.serviceDefinition, localCloud).orchestrated.push({ consumer: s.consumer,
      consumerCloud: s.consumerCloud.split('|')[0], via: `inter-cloud GateTunnel session (${s.requests} requests)` });
  }
  for (const rule of rules.ok ? rules.value : []) {
    const [providerName, service] = String(rule.serviceInstanceId || '').split('|');
    if (providerName && service) row(providerName, service, localCloud).orchestrated.push({ consumer: rule.consumerName || rule.consumer,
      consumerCloud: localCloud, via: `SimpleStore rule (priority ${rule.priority})` });
  }
  for (const scope of plant.ok ? plant.value.rules || [] : []) {
    for (const p of scope.providers || []) {
      const [providerName] = String(p.serviceInstanceId).split('|');
      row(providerName, scope.serviceDefinition, localCloud).orchestrated.push({ consumer: scope.consumer,
        consumerCloud: localCloud, via: `DSO plant rule (priority ${p.priority})` });
    }
  }
  return {
    collectedAt: new Date().toISOString(), localCloud,
    errors: [mgmt, provider, dynamic, rules, plant].filter((r) => !r.ok).map((r) => r.error),
    rows: [...rows.values()].sort((a, b) => a.provider.localeCompare(b.provider) || a.serviceDefinition.localeCompare(b.serviceDefinition))
  };
}

async function getOrchestrationRulesOnly() {
  const inventory = await getArrowheadInventory();
  const catalog = buildOrchestrationCatalog(inventory.systems.entries || [], inventory.services.entries || []);
  const consumerNames = catalog.consumers.map((consumer) => consumer.name);
  if (!consumerNames.length) return [];
  const rules = await orchestrationRequest('query', { consumerNames });
  return rules.entries || [];
}

async function createOrchestrationRule(candidate) {
  const inventory = await getArrowheadInventory();
  const catalog = buildOrchestrationCatalog(inventory.systems.entries || [], inventory.services.entries || []);
  const normalized = validateRuleCandidate(catalog, candidate);
  return orchestrationRequest('create', { candidates: [normalized] });
}

async function removeOrchestrationRule(id) {
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(id || '')) {
    throw Object.assign(new Error('Invalid orchestration rule id'), { status: 400 });
  }
  await orchestrationRequest('remove', undefined, `/remove?uuids=${encodeURIComponent(id)}`);
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
    .filter((container) => container.managed && !['mqtt-broker', 'amqp-broker', 'chiplet-gateway', 'chiplet-adapters'].includes(container.service) && !registeredComposeServices.has(container.service))
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
        target: 'AMQP Broker',
        label: `sysMonitor.observe · ${delegated ? 'delegated' : 'native'}`,
        protocol: 'AMQPS · CERT_AUTH · confirmed',
        active: running('amqp-broker') && (delegated
          ? running('system-monitor-publisher')
          : (!providerService || running(providerService)))
      };
    })
    .sort((left, right) => left.source.localeCompare(right.source));
  return {
    infrastructure: [{
      id: 'amqp-broker',
      name: 'AMQP Broker',
      kind: 'Messaging infrastructure · sysMonitor',
      endpoint: 'amqps://amqp-broker:5671 · exchange arrowhead',
      protocol: 'AMQP 0-9-1',
      security: 'Mutual X.509 · SASL EXTERNAL · topic permissions',
      runtime: runtime('amqp-broker')
    }, {
      id: 'mqtt-broker',
      name: 'MQTT Broker',
      kind: 'Messaging infrastructure · sysMonitorData',
      endpoint: 'mqtts://mqtt-broker:8883',
      protocol: 'MQTT 5',
      security: 'Mutual X.509 · ACL',
      runtime: runtime('mqtt-broker')
    }],
    orchestrations: [
      ...sysMonitorProducers,
      {
        id: 'sysmonitor-consume', source: 'AMQP Broker', target: 'SysMonitor',
        label: 'sysMonitor observation subscription · selected in sysMonitorManagement', protocol: 'AMQPS · X.509',
        active: running('amqp-broker') && running('sysmonitor')
      },
      {
        id: 'sysmonitor-orchestration', source: 'SysMonitor', target: 'DynamicServiceOrchestration',
        label: 'Pull sysMonitor.observe · 30s', protocol: 'HTTPS · token + mTLS',
        active: running('sysmonitor') && running('serviceorchestration-dynamic') && sysMonitorProducers.length > 0
      },
      {
        id: 'aigym-sysmonitor-data', source: 'MQTT Broker', target: 'AIGym',
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
  // J16: an expired CRL stops every connection in the cloud, so its remaining validity is an incident before it expires
  const revocation = crlStatus(TLS_CRL_FILE);
  const unhealthy = containers.filter((container) => container.health === 'unhealthy').length;
  const registeredComposeServices = new Set(systems.map((system) => system.composeService).filter(Boolean));
  const unregisteredServices = containers
    .filter((container) => container.managed && !['mqtt-broker', 'amqp-broker', 'chiplet-gateway', 'chiplet-adapters'].includes(container.service) &&
      container.state === 'running' && !registeredComposeServices.has(container.service))
    .map((container) => container.service);
  const sample = {
    at: collectedAt,
    running,
    containers: containers.length,
    registeredSystems: systems.length,
    registeredServices: inventory.services.count || 0,
    unhealthy,
    incidents: unhealthy + unregisteredServices.length + namingViolations.length + (revocation.state === 'ok' ? 0 : 1),
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
    revocation,
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
      host: process.env.SYSMONITOR_BROKER_ADDRESS || 'amqp-broker',
      port: Number(process.env.SYSMONITOR_BROKER_PORT || 5671),
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
    const clientCertificate = new crypto.X509Certificate(fs.readFileSync(TLS_CLIENT_CERT));
    if (!clientCertificate.checkPrivateKey(crypto.createPrivateKey(fs.readFileSync(TLS_CLIENT_KEY)))) throw new Error('Client certificate does not match the configured private key');
    const clientPublicKey = clientCertificate.publicKey.export({type:'spki',format:'der'}).toString('base64');
    await arrowheadRequest('/serviceregistry/system-discovery/register', {
      metadata: { role: 'local-cloud-monitor', managedProject: PROJECT_NAME, x509PublicKey: clientPublicKey, consumerInterfaceTemplates: 'generic_https' },
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
          // Path parameters follow the v5 convention: the registered path has no placeholder, the value is appended.
          operations: {
            session: { method: 'GET', path: '/session' },
            login: { method: 'POST', path: '/session' },
            logout: { method: 'DELETE', path: '/session' },
            overview: { method: 'GET', path: '/overview' },
            serviceRegistryManagement: { method: 'GET', path: '/service-registry-management' },
            dataManager: { method: 'GET', path: '/datamanager' },
            consumers: { method: 'GET', path: '/consumers' },
            interCloud: { method: 'GET', path: '/intercloud' },
            sysMonitorHistory: { method: 'GET', path: '/sysmonitor' },
            sysMonitorSubscriptions: { method: 'GET', path: '/sysmonitor/subscriptions' },
            subscribeSysMonitor: { method: 'PUT', path: '/sysmonitor/subscriptions' },
            unsubscribeSysMonitor: { method: 'DELETE', path: '/sysmonitor/subscriptions' },
            plantDescriptions: { method: 'GET', path: '/plantdescriptions' },
            createPlantDescription: { method: 'POST', path: '/plantdescriptions' },
            replacePlantDescription: { method: 'PUT', path: '/plantdescriptions' },
            updatePlantDescription: { method: 'PATCH', path: '/plantdescriptions' },
            removePlantDescription: { method: 'DELETE', path: '/plantdescriptions' },
            orchestrationRules: { method: 'GET', path: '/orchestration/rules' },
            createOrchestrationRule: { method: 'POST', path: '/orchestration/rules' },
            removeOrchestrationRule: { method: 'DELETE', path: '/orchestration/rules' },
            logs: { method: 'GET', path: '/logs' },
            lifecycle: { method: 'POST', path: '/containers' }
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

const consoleAuth = new ConsoleAuth();

// Console session: login, state and logout. Returns true when the request was answered here.
async function routeSession(req, res, url) {
  if (url.pathname !== '/api/session') return false;
  if (req.method === 'GET') {
    jsonResponse(res, 200, { authenticated: consoleAuth.authenticated(req), configured: consoleAuth.configured });
  } else if (req.method === 'POST') {
    // JSON only: a cross-site form post (text/plain) cannot reach the login, so other pages cannot lock it (J6)
    if (!String(req.headers['content-type'] || '').toLowerCase().startsWith('application/json')) {
      jsonResponse(res, 415, { error: 'Content-Type must be application/json' });
      return true;
    }
    const body = await readJson(req);
    const result = consoleAuth.login(body.password, consoleAuth.clientOf(req));
    if (result.cookie) res.setHeader('Set-Cookie', result.cookie);
    if (result.retryAfter) res.setHeader('Retry-After', String(result.retryAfter));
    if (result.status === 204) {
      console.log('Console login');
      res.writeHead(204, { 'Cache-Control': 'no-store' });
      res.end();
    } else {
      console.warn(`Console login refused (${result.status})`);
      jsonResponse(res, result.status, { error: result.status === 429 ? 'Too many failed logins; try again later' : 'Wrong password' });
    }
  } else if (req.method === 'DELETE') {
    res.setHeader('Set-Cookie', consoleAuth.logout(req));
    jsonResponse(res, 200, { authenticated: false });
  } else {
    jsonResponse(res, 405, { error: 'Method not allowed' });
  }
  return true;
}

// Chiplet floorplans use the same authenticated, authorized orchestration boundary as other console services.
async function floorplanCall(definition, operation, { id, query, body } = {}) {
  const target = await orchestrate(definition, operation, 'ChipletFloorplanGenerator');
  let path = target.path;
  if (id !== undefined) path = path.replace(/\{[^}]+\}/g, encodeURIComponent(id));
  if (/\{[^}]+\}/.test(path)) throw Object.assign(new Error('Incomplete floorplan route'), { status: 502 });
  if (query) path += (path.includes('?') ? '&' : '?') + query;
  return systemRequest(target.origin, target.provider || 'ChipletFloorplanGenerator', `${target.basePath}${path}`, getDataManagerAgent(), target.method, body);
}
async function getSoSConfigurations(){const data=await getPlantDescriptions();for(let page=1;data.entries.length<data.count;page++){if(page>=10)throw Object.assign(new Error('SoS view supports up to 2000 stored configurations'),{status:503});const rows=await plantDescriptionRequest('query',`/plant-descriptions?page=${page}&size=200`);if(!rows.entries?.length)throw Object.assign(new Error('SoS catalog changed while reading; reload'),{status:409});data.entries.push(...rows.entries);}return data;}
async function localCloudConfiguratorCall(path,method,body){const token=secretEnv('LOCAL_CLOUD_CONFIGURATOR_TOKEN');if(!token)throw Object.assign(new Error('Local cloud configurator credential is unavailable'),{status:503});const response=await fetch(new URL(path,LOCAL_CLOUD_CONFIGURATOR_URL),{method,headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},...(body===undefined?{}:{body:JSON.stringify(body)}),signal:AbortSignal.timeout(60000)});const result=await response.json();return{status:response.status,body:result};}
const localCloudConfigurationRoutes=createCloudConfigurationRoutes({enabled:Boolean(LOCAL_CLOUD_CONFIGURATOR_URL),call:localCloudConfiguratorCall,readJson,jsonResponse});
const sosRoutes=createSoSRoutes({catalog:getSoSConfigurations,call:plantDescriptionRequest,readJson,jsonResponse});
const floorplanRoutes = createFloorplanRoutes({ enabled: CHIPLET_FLOORPLANS_ENABLED, call: floorplanCall, readJson, jsonResponse });

async function route(req, res) {
  const url = new URL(req.url, 'http://console.invalid');
  if (req.method === 'GET' && url.pathname === '/health') {
    return jsonResponse(res, 200, { status: 'ok', system: ARROWHEAD_SYSTEM_NAME, registration: registrationState });
  }
  // J6: only requests forwarded by the console's own nginx (proxy secret), not other containers of the cloud network
  if (!consoleAuth.fromProxy(req)) {
    return jsonResponse(res, 403, { error: 'Not forwarded by the console' });
  }
  // H1: only the local console names are accepted (DNS rebinding), and every API call needs an operator session.
  if (!consoleAuth.originAllowed(req)) {
    return jsonResponse(res, 403, { error: 'Host or Origin is not an allowed console address' });
  }
  if (!consoleAuth.configured) {
    return jsonResponse(res, 503, { error: 'Console password is not configured (CONSOLE_PASSWORD_HASH)' });
  }
  if (await routeSession(req, res, url)) return;
  if (!consoleAuth.authenticated(req)) {
    return jsonResponse(res, 401, { error: 'Console login required' });
  }
  if (req.method === 'GET' && url.pathname === '/api/features') {
    return jsonResponse(res, 200, { localCloudConfiguration: Boolean(LOCAL_CLOUD_CONFIGURATOR_URL), chipletFloorplans: CHIPLET_FLOORPLANS_ENABLED, managerName: CHIPLET_FLOORPLANS_ENABLED ? 'ChipletLocalCloudManager' : 'LocalCloudManager' });
  }
  if (await localCloudConfigurationRoutes(req,res,url)) return;
  if (await sosRoutes(req,res,url)) return;
  if (await floorplanRoutes(req, res, url)) return;
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
  if (req.method === 'GET' && url.pathname === '/api/consumers') {
    return jsonResponse(res, 200, await getServiceConsumers());
  }
  if (req.method === 'GET' && url.pathname === '/api/intercloud') {
    return jsonResponse(res, 200, await getInterCloudContent());
  }
  if (req.method === 'GET' && url.pathname === '/api/sysmonitor') {
    return jsonResponse(res, 200, await getSysMonitorHistory(url.searchParams.get('system') || '', url.searchParams.get('limit') || 120));
  }
  if (req.method === 'GET' && url.pathname === '/api/sysmonitor/subscriptions') {
    return jsonResponse(res, 200, await getSysMonitorSubscriptions());
  }
  const subscriptionMatch = url.pathname.match(/^\/api\/sysmonitor\/subscriptions\/([A-Za-z0-9_-]+)$/);
  if (subscriptionMatch && (req.method === 'PUT' || req.method === 'DELETE')) {
    if (req.headers['x-arrowhead-action'] !== ACTION_CONFIRMATION) {
      return jsonResponse(res, 428, { error: 'Missing action confirmation header' });
    }
    const target = `/subscriptions/${encodeURIComponent(subscriptionMatch[1])}`;
    if (req.method === 'DELETE') {
      await sysMonitorRequest('sysMonitorManagement', 'unsubscribe', target);
      return jsonResponse(res, 200, { removed: subscriptionMatch[1] });
    }
    const body = await readJson(req);
    return jsonResponse(res, 200, await sysMonitorRequest('sysMonitorManagement', 'subscribe', target, {
      observationTypes: body.observationTypes, enabled: body.enabled !== false
    }));
  }
  if (req.method === 'GET' && url.pathname === '/api/plantdescriptions') {
    return jsonResponse(res, 200, await getPlantDescriptions());
  }
  const plantMatch = url.pathname.match(/^\/api\/plantdescriptions(?:\/(\d+))?$/);
  if (plantMatch && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method)) {
    if (req.headers['x-arrowhead-action'] !== ACTION_CONFIRMATION) {
      return jsonResponse(res, 428, { error: 'Missing action confirmation header' });
    }
    const id = plantMatch[1];
    if (req.method === 'POST' && !id) {
      return jsonResponse(res, 201, await plantDescriptionRequest('create', '/plant-descriptions', await readJson(req)));
    }
    if (!id) return jsonResponse(res, 404, { error: 'Not found' });
    if (req.method === 'DELETE') {
      await plantDescriptionRequest('remove', `/plant-descriptions/${id}`);
      return jsonResponse(res, 200, { removed: Number(id) });
    }
    return jsonResponse(res, 200, await plantDescriptionRequest(req.method === 'PUT' ? 'replace' : 'update', `/plant-descriptions/${id}`, await readJson(req)));
  }
  if (await policyRoutes(req,res,url)) return;
  if (await priorityRoutes(req,res,url)) return;
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

const { createPriorityRoutes, replacePriorityWithExistingOperations } = require('./rule-editor-api');
const priorityRoutes = createPriorityRoutes({query:getOrchestrationRulesOnly,modify:(id,priority,old)=>process.env.ORCHESTRATION_PRIORITY_MODE==='replace' ? replacePriorityWithExistingOperations({query:getOrchestrationRulesOnly,create:createOrchestrationRule,remove:removeOrchestrationRule},old,priority) : orchestrationRequest('modify-priorities',{[id]:priority}),readJson,jsonResponse});
const { createPolicyRoutes } = require('./policies-api');
const policyRoutes = createPolicyRoutes({inventory:getArrowheadInventory,
 query:async()=>[...await authorizationPolicies('MGMT'),...await authorizationPolicies('PROVIDER')],
 grant:policy=>orchestratedRequest('authorizationManagement','grant-policies',{provider:'ConsumerAuthorization',body:{list:[policy]}}),
 revoke:id=>orchestratedRequest('authorizationManagement','revoke-policies',{provider:'ConsumerAuthorization',path:`/revoke?instanceIds=${encodeURIComponent(id)}`}),
 readJson,jsonResponse});


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
  consoleAuth,
  server
};
