'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { crlStatus, parseCrlDates } = require('../src/crl-status');

// fixtures: empty CRLs of a throwaway CA, lastUpdate 2026-10-20 12:00 UTC
const fixture = (name) => path.join(__dirname, 'fixtures', name);
const thisUpdate = Date.parse('2026-10-20T12:00:00Z');

test('reads thisUpdate and nextUpdate from PEM and DER, UTCTime and GeneralizedTime', () => {
  for (const name of ['crl-utc.pem', 'crl-utc.der']) {
    const dates = parseCrlDates(fs.readFileSync(fixture(name)));
    assert.equal(dates.thisUpdate.getTime(), thisUpdate);
    assert.equal(dates.nextUpdate.toISOString(), '2026-10-23T12:00:00.000Z');
  }
  assert.equal(parseCrlDates(fs.readFileSync(fixture('crl-generalized.pem'))).nextUpdate.toISOString(), '2051-01-01T00:00:00.000Z');
});

test('ok with margin, warning within 12 hours, expired after nextUpdate (J16)', () => {
  const nextUpdate = Date.parse('2026-10-23T12:00:00Z');
  const ok = crlStatus(fixture('crl-utc.pem'), thisUpdate);
  assert.equal(ok.state, 'ok');
  assert.equal(ok.hoursRemaining, 72);
  const warning = crlStatus(fixture('crl-utc.pem'), nextUpdate - 5 * 3600000);
  assert.equal(warning.state, 'warning');
  assert.equal(warning.hoursRemaining, 5);
  assert.match(warning.message, /CertificateAuthority/);
  assert.equal(crlStatus(fixture('crl-utc.pem'), nextUpdate + 1000).state, 'expired');
});

test('a missing or unreadable CRL is reported, not ignored', () => {
  assert.equal(crlStatus('').state, 'unavailable');
  assert.equal(crlStatus(fixture('does-not-exist.crl')).state, 'unavailable');
  assert.equal(crlStatus(__filename).state, 'unavailable');
});
