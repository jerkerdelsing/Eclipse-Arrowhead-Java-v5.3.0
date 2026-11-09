'use strict';

// Docker Engine API proxy for the LocalCloudManager console (security finding H1).
// The console API no longer mounts /var/run/docker.sock; it reaches Docker only through this proxy, on an internal
// network that nothing else joins. The proxy forwards exactly what the console needs, for this Compose project only:
//   GET  /containers/json                          list the project's containers (the filter is set here)
//   GET  /containers/<id>/logs                     logs of a project container
//   POST /containers/<id>/(start|stop|restart)     lifecycle of a container in MANAGED_SERVICES
// Everything else (exec, create, images, volumes, networks, other projects, ...) is refused with 403.

const http = require('node:http');

const PORT = Number(process.env.PORT || 2375);
const DOCKER_SOCKET = process.env.DOCKER_SOCKET || '/var/run/docker.sock';
const PROJECT_NAME = process.env.COMPOSE_PROJECT_NAME || '';
const MANAGED_SERVICES = new Set(String(process.env.MANAGED_SERVICES || '').split(',').map((s) => s.trim()).filter(Boolean));
const CONTAINER_ID = /^[a-f0-9]{12,64}$/;

function docker(path, method = 'GET') {
  return new Promise((resolve, reject) => {
    const req = http.request({ socketPath: DOCKER_SOCKET, path, method }, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(chunk));
      res.on('end', () => resolve({ status: res.statusCode, headers: res.headers, body: Buffer.concat(chunks) }));
    });
    req.on('error', reject);
    req.end();
  });
}

function pick(searchParams, allowed) {
  const out = new URLSearchParams();
  for (const [name, pattern] of Object.entries(allowed)) {
    const value = searchParams.get(name);
    if (value !== null && pattern.test(value)) out.set(name, value);
  }
  return out.toString();
}

async function containerLabels(id) {
  const response = await docker(`/containers/${id}/json`);
  if (response.status !== 200) return null;
  return JSON.parse(response.body.toString('utf8')).Config?.Labels || {};
}

// Decides one request; returns the Docker path and method to forward, or { status, error } to refuse.
async function decide(method, rawUrl, labelsOf = containerLabels) {
  const url = new URL(rawUrl, 'http://proxy.invalid');
  const path = url.pathname.replace(/^\/v\d+\.\d+(?=\/)/, '');
  if (method === 'GET' && path === '/containers/json') {
    const filters = encodeURIComponent(JSON.stringify({ label: [`com.docker.compose.project=${PROJECT_NAME}`] }));
    return { method, path: `/containers/json?all=1&filters=${filters}` };
  }
  const match = path.match(/^\/containers\/([^/]+)\/(logs|start|stop|restart)$/);
  if (!match || !CONTAINER_ID.test(match[1])) return { status: 403, error: 'operation not allowed by the console proxy' };
  const [, id, operation] = match;
  if ((operation === 'logs') !== (method === 'GET') || (operation !== 'logs' && method !== 'POST')) {
    return { status: 403, error: 'operation not allowed by the console proxy' };
  }
  const labels = await labelsOf(id);
  if (!labels || labels['com.docker.compose.project'] !== PROJECT_NAME) {
    return { status: 403, error: 'container is not part of this local cloud' };
  }
  if (operation === 'logs') {
    const query = pick(url.searchParams, { stdout: /^[01]$/, stderr: /^[01]$/, timestamps: /^[01]$/, tail: /^\d{1,4}$/ });
    return { method, path: `/containers/${id}/logs?${query}` };
  }
  if (!MANAGED_SERVICES.has(labels['com.docker.compose.service'])) {
    return { status: 403, error: 'service is not in the management allowlist' };
  }
  const query = pick(url.searchParams, { t: /^\d{1,3}$/ });
  return { method, path: `/containers/${id}/${operation}${query ? `?${query}` : ''}` };
}

function refuse(res, status, error) {
  const body = JSON.stringify({ message: error });
  res.writeHead(status, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) });
  res.end(body);
}

const server = http.createServer(async (req, res) => {
  try {
    req.resume();
    const decision = await decide(req.method, req.url);
    if (decision.status) {
      console.warn(`refused ${req.method} ${req.url}: ${decision.error}`);
      return refuse(res, decision.status, decision.error);
    }
    if (decision.method === 'POST') console.log(`${decision.method} ${decision.path}`);
    const response = await docker(decision.path, decision.method);
    res.writeHead(response.status, { 'Content-Type': response.headers['content-type'] || 'application/octet-stream',
      'Content-Length': response.body.length });
    res.end(response.body);
  } catch (error) {
    refuse(res, 502, error.message);
  }
});

if (require.main === module) {
  if (!PROJECT_NAME) throw new Error('COMPOSE_PROJECT_NAME is required');
  server.listen(PORT, '0.0.0.0', () => console.log(`Console Docker proxy for ${PROJECT_NAME} listening on ${PORT}`));
}

module.exports = { decide };
