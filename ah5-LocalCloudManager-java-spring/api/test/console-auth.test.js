'use strict';
const test = require('node:test');
const assert = require('node:assert');
const crypto = require('node:crypto');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { ConsoleAuth, hostName, secretEnv } = require('../src/console-auth');

const salt = crypto.randomBytes(16);
const hash = `scrypt:${salt.toString('base64')}:${crypto.scryptSync('correct horse', salt, 32).toString('base64')}`;
const request = (headers) => ({ headers });

test('host names are compared without port', () => {
  assert.strictEqual(hostName('127.0.0.1:8490'), '127.0.0.1');
  assert.strictEqual(hostName('[::1]:8490'), '[::1]');
});

test('foreign Host or Origin is refused (DNS rebinding)', () => {
  const auth = new ConsoleAuth({ passwordHash: hash });
  assert.ok(auth.originAllowed(request({ host: '127.0.0.1:8490' })));
  assert.ok(auth.originAllowed(request({ host: 'localhost', origin: 'http://localhost:8490' })));
  assert.ok(!auth.originAllowed(request({ host: 'attacker.example:8490' })));
  assert.ok(!auth.originAllowed(request({ host: '127.0.0.1', origin: 'http://attacker.example' })));
  assert.ok(!auth.originAllowed(request({ host: '127.0.0.1', origin: 'null' })));
});

test('login issues a strict session cookie; wrong passwords lock', () => {
  let now = 1_000_000;
  const auth = new ConsoleAuth({ passwordHash: hash, now: () => now });
  assert.strictEqual(auth.login('wrong').status, 401);
  const ok = auth.login('correct horse');
  assert.strictEqual(ok.status, 204);
  assert.match(ok.cookie, /HttpOnly; SameSite=Strict/);
  const cookie = ok.cookie.split(';')[0];
  assert.ok(auth.authenticated(request({ cookie })));
  for (let i = 0; i < 5; i += 1) auth.login('wrong');
  assert.strictEqual(auth.login('correct horse').status, 429);
  now += 61_000;
  assert.strictEqual(auth.login('correct horse').status, 204);
  now += 9 * 3600 * 1000;
  assert.ok(!auth.authenticated(request({ cookie })));
});

test('unconfigured console refuses every password', () => {
  const auth = new ConsoleAuth({ passwordHash: '' });
  assert.ok(!auth.configured);
  assert.strictEqual(auth.login('').status, 401);
});

test('a lockout is per client, so another client cannot lock the operator out (J6)', () => {
  let now = 2_000_000;
  const auth = new ConsoleAuth({ passwordHash: hash, now: () => now });
  for (let i = 0; i < 5; i += 1) auth.login('wrong', '172.19.0.30');
  assert.strictEqual(auth.login('correct horse', '172.19.0.30').status, 429);
  assert.strictEqual(auth.login('correct horse', '192.168.65.1').status, 204);
  // each further lock of the same client doubles
  now += 61_000;
  for (let i = 0; i < 5; i += 1) auth.login('wrong', '172.19.0.30');
  now += 61_000;
  assert.strictEqual(auth.login('correct horse', '172.19.0.30').status, 429);
  now += 60_000;
  assert.strictEqual(auth.login('correct horse', '172.19.0.30').status, 204);
});

test('with a proxy secret only requests forwarded by the console nginx are served (J6)', () => {
  const auth = new ConsoleAuth({ passwordHash: hash, proxySecret: 's3cret-value' });
  assert.ok(!auth.fromProxy(request({ host: '127.0.0.1' })));
  assert.ok(!auth.fromProxy(request({ host: '127.0.0.1', 'x-console-proxy': 'wrong' })));
  assert.ok(auth.fromProxy(request({ host: '127.0.0.1', 'x-console-proxy': 's3cret-value' })));
  assert.strictEqual(auth.clientOf({ headers: { 'x-real-ip': '192.168.65.1' }, socket: { remoteAddress: '172.19.0.5' } }), '192.168.65.1');
  const open = new ConsoleAuth({ passwordHash: hash });
  assert.ok(open.fromProxy(request({ host: '127.0.0.1' })));
  // without a proxy secret X-Real-IP is not trusted
  assert.strictEqual(open.clientOf({ headers: { 'x-real-ip': '1.2.3.4' }, socket: { remoteAddress: '172.19.0.5' } }), '172.19.0.5');
});

test('secrets come from NAME, else from the file named by NAME_FILE (J7)', () => {
  const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'secret-')), 'value');
  fs.writeFileSync(file, 'from-file\n');
  assert.strictEqual(secretEnv('S', { S_FILE: file }), 'from-file');
  assert.strictEqual(secretEnv('S', { S: 'direct', S_FILE: file }), 'direct');
  assert.strictEqual(secretEnv('S', {}), undefined);
  assert.throws(() => secretEnv('S', { S_FILE: `${file}.absent` }));
});
