'use strict';
process.env.COMPOSE_PROJECT_NAME = 'cloud-a';
process.env.MANAGED_SERVICES = 'sysmonitor,datamanager';
const test = require('node:test');
const assert = require('node:assert');
const { decide } = require('../src/proxy');

const id = 'a'.repeat(64);
const labels = (project, service) => async () => ({ 'com.docker.compose.project': project, 'com.docker.compose.service': service });

test('container list is always filtered to the own project', async () => {
  const d = await decide('GET', '/v1.47/containers/json?all=1&filters=%7B%7D');
  assert.match(decodeURIComponent(d.path), /com\.docker\.compose\.project=cloud-a/);
});

test('logs of an own container pass with sanitized query', async () => {
  const d = await decide('GET', `/containers/${id}/logs?stdout=1&tail=150&follow=1`, labels('cloud-a', 'authentication'));
  assert.strictEqual(d.path, `/containers/${id}/logs?stdout=1&tail=150`);
});

test('lifecycle only for managed services of the own project', async () => {
  assert.strictEqual((await decide('POST', `/containers/${id}/restart?t=20`, labels('cloud-a', 'sysmonitor'))).path,
    `/containers/${id}/restart?t=20`);
  assert.strictEqual((await decide('POST', `/containers/${id}/stop`, labels('cloud-a', 'docker-proxy'))).status, 403);
  assert.strictEqual((await decide('POST', `/containers/${id}/stop`, labels('cloud-b', 'sysmonitor'))).status, 403);
});

test('everything else is refused', async () => {
  for (const [method, url] of [['POST', `/containers/${id}/exec`], ['POST', '/containers/create'], ['GET', '/images/json'],
    ['DELETE', `/containers/${id}`], ['GET', `/containers/${id}/start`], ['POST', `/containers/${id}/logs`],
    ['GET', '/containers/../images/json'], ['POST', '/containers/name-not-id/restart']]) {
    assert.strictEqual((await decide(method, url, labels('cloud-a', 'sysmonitor'))).status, 403, `${method} ${url}`);
  }
});
