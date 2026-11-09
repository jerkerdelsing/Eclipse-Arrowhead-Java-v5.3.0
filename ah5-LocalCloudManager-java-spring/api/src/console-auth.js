'use strict';

// Operator authentication of the LocalCloudManager console (security finding H1).
// - Every /api request needs a session, obtained with the console password (POST /api/session).
//   The container only knows an scrypt hash of the password (CONSOLE_PASSWORD_HASH = scrypt:<saltB64>:<hashB64>).
// - The session cookie is HttpOnly and SameSite=Strict, so other sites cannot use it.
// - Requests whose Host (or Origin) is not an allowed local name are refused, which defeats DNS rebinding:
//   a rebound page runs under the attacker's host name.
// - Repeated wrong passwords lock the login for a while, per client (J6): another client (e.g. a compromised container)
//   cannot lock the operator out. Each further lock of the same client doubles, up to MAX_LOCK_MS.
// - With CONSOLE_PROXY_SECRET set, only requests forwarded by the console's own nginx (which adds that secret) are
//   served, and nginx's X-Real-IP is the client address; nothing else in the cloud network can call the API (J6).

const crypto = require('node:crypto');
const fs = require('node:fs');

// NAME, else the content of the file named by NAME_FILE (Compose secrets under /run/secrets, security finding J7).
// An unreadable NAME_FILE is an error: the console does not start without its secrets.
function secretEnv(name, env = process.env) {
  if (env[name] !== undefined) return env[name];
  const file = env[`${name}_FILE`];
  return file ? fs.readFileSync(file, 'utf8').replace(/[\r\n]+$/, '') : undefined;
}

const COOKIE = 'ah_console';
const SESSION_SECONDS = Number(process.env.CONSOLE_SESSION_SECONDS || 8 * 3600);
const MAX_FAILURES = 5;
const FAILURE_WINDOW_MS = 5 * 60 * 1000;
const LOCK_MS = 60 * 1000;
const MAX_LOCK_MS = 15 * 60 * 1000;
const MAX_CLIENTS = 1024;

function parsePasswordHash(value) {
  const parts = String(value || '').split(':');
  if (parts.length !== 3 || parts[0] !== 'scrypt') return null;
  const salt = Buffer.from(parts[1], 'base64');
  const hash = Buffer.from(parts[2], 'base64');
  return salt.length >= 16 && hash.length >= 32 ? { salt, hash } : null;
}

function allowedHostNames(value) {
  return new Set(String(value || '127.0.0.1,localhost,[::1]').split(',').map((name) => name.trim().toLowerCase()).filter(Boolean));
}

// Host name without port; IPv6 literals keep their brackets.
function hostName(value) {
  const host = String(value || '').trim().toLowerCase();
  if (host.startsWith('[')) return host.slice(0, host.indexOf(']') + 1);
  return host.split(':')[0];
}

function cookieValue(header, name) {
  for (const part of String(header || '').split(';')) {
    const index = part.indexOf('=');
    if (index > 0 && part.slice(0, index).trim() === name) return part.slice(index + 1).trim();
  }
  return null;
}

class ConsoleAuth {
  constructor({ passwordHash = secretEnv('CONSOLE_PASSWORD_HASH'), allowedHosts = process.env.CONSOLE_ALLOWED_HOSTS,
    proxySecret = secretEnv('CONSOLE_PROXY_SECRET'), now = () => Date.now() } = {}) {
    this.credential = parsePasswordHash(passwordHash);
    this.allowedHosts = allowedHostNames(allowedHosts);
    this.proxySecret = proxySecret ? Buffer.from(String(proxySecret)) : null;
    this.now = now;
    this.sessions = new Map();
    this.clients = new Map(); // client -> { failures: [timestamps], lockedUntil, locks }
  }

  // True when the request was forwarded by the console's nginx (or no proxy secret is configured).
  fromProxy(req) {
    if (!this.proxySecret) return true;
    const given = Buffer.from(String(req.headers['x-console-proxy'] || ''));
    return given.length === this.proxySecret.length && crypto.timingSafeEqual(given, this.proxySecret);
  }

  // The client a failed login is counted against: nginx's X-Real-IP behind the proxy, else the TCP peer.
  clientOf(req) {
    const forwarded = this.proxySecret ? String(req.headers['x-real-ip'] || '') : '';
    return forwarded || req.socket?.remoteAddress || 'unknown';
  }

  get configured() {
    return this.credential !== null;
  }

  // Host and Origin must name this console, not a foreign (for example rebound) site.
  originAllowed(req) {
    if (!this.allowedHosts.has(hostName(req.headers.host))) return false;
    const origin = req.headers.origin;
    if (origin && origin !== 'null') {
      try {
        if (!this.allowedHosts.has(hostName(new URL(origin).host))) return false;
      } catch {
        return false;
      }
    } else if (origin === 'null') {
      return false;
    }
    return true;
  }

  verifyPassword(password) {
    if (!this.credential || typeof password !== 'string' || password.length === 0 || password.length > 256) return false;
    const candidate = crypto.scryptSync(password, this.credential.salt, this.credential.hash.length);
    return crypto.timingSafeEqual(candidate, this.credential.hash);
  }

  // Returns { status, cookie? }: 204 on success, 401 on a wrong password, 429 while this client is locked.
  login(password, client = 'unknown') {
    const now = this.now();
    const state = this.clients.get(client) || { failures: [], lockedUntil: 0, locks: 0 };
    if (now < state.lockedUntil) return { status: 429, retryAfter: Math.ceil((state.lockedUntil - now) / 1000) };
    if (!this.verifyPassword(password)) {
      state.failures = state.failures.filter((at) => now - at < FAILURE_WINDOW_MS).concat(now);
      if (state.failures.length >= MAX_FAILURES) {
        state.lockedUntil = now + Math.min(LOCK_MS * 2 ** state.locks, MAX_LOCK_MS);
        state.locks += 1;
        state.failures = [];
      }
      this.remember(client, state);
      return { status: 401 };
    }
    this.clients.delete(client);
    const token = crypto.randomBytes(32).toString('base64url');
    this.sessions.set(token, now + SESSION_SECONDS * 1000);
    return { status: 204, cookie: `${COOKIE}=${token}; HttpOnly; SameSite=Strict; Path=/; Max-Age=${SESSION_SECONDS}` };
  }

  remember(client, state) {
    this.clients.delete(client);
    this.clients.set(client, state);
    // bounded memory: forget the oldest clients that are not locked
    for (const [key, value] of this.clients) {
      if (this.clients.size <= MAX_CLIENTS) break;
      if (value.lockedUntil <= this.now()) this.clients.delete(key);
    }
  }

  sessionToken(req) {
    const token = cookieValue(req.headers.cookie, COOKIE);
    if (!token) return null;
    const expires = this.sessions.get(token);
    if (!expires) return null;
    if (expires <= this.now()) {
      this.sessions.delete(token);
      return null;
    }
    return token;
  }

  authenticated(req) {
    return this.sessionToken(req) !== null;
  }

  logout(req) {
    const token = this.sessionToken(req);
    if (token) this.sessions.delete(token);
    return `${COOKIE}=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0`;
  }
}

module.exports = { ConsoleAuth, parsePasswordHash, hostName, cookieValue, secretEnv };
