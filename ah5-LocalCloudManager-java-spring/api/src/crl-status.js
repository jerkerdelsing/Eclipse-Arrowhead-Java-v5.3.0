'use strict';

// Remaining validity of the cloud CA's CRL (security finding J16). Every relying party refuses all peers once the CRL is
// past its nextUpdate, so an expiring CRL is a cloud-wide incident that the console reports before it happens.
const fs = require('node:fs');

const WARN_HOURS = 12;

// one DER TLV at offset: { tag, start (of the value), end }
function tlv(buffer, offset) {
  const tag = buffer[offset];
  let length = buffer[offset + 1];
  let start = offset + 2;
  if (length & 0x80) {
    const bytes = length & 0x7f;
    if (bytes < 1 || bytes > 4) throw new Error('unsupported DER length');
    length = 0;
    for (let i = 0; i < bytes; i += 1) length = length * 256 + buffer[start + i];
    start += bytes;
  }
  if (start + length > buffer.length) throw new Error('truncated DER');
  return { tag, start, end: start + length };
}

// UTCTime (0x17) YYMMDDHHMMSSZ or GeneralizedTime (0x18) YYYYMMDDHHMMSSZ
function asn1Time(buffer, item) {
  const text = buffer.subarray(item.start, item.end).toString('ascii');
  const match = item.tag === 0x17 ? /^(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})Z$/.exec(text)
    : item.tag === 0x18 ? /^(\d{4})(\d{2})(\d{2})(\d{2})(\d{2})(\d{2})Z$/.exec(text) : null;
  if (!match) throw new Error('unsupported CRL time');
  let year = Number(match[1]);
  if (item.tag === 0x17) year += year < 50 ? 2000 : 1900;
  return new Date(Date.UTC(year, Number(match[2]) - 1, Number(match[3]), Number(match[4]), Number(match[5]), Number(match[6])));
}

// thisUpdate and nextUpdate of a PEM or DER encoded X.509 CRL
function parseCrlDates(data) {
  let der = Buffer.from(data);
  const pem = /-----BEGIN X509 CRL-----([\s\S]*?)-----END X509 CRL-----/.exec(der.toString('latin1'));
  if (pem) der = Buffer.from(pem[1].replace(/\s+/g, ''), 'base64');
  const list = tlv(der, 0);
  const tbs = tlv(der, list.start);
  if (list.tag !== 0x30 || tbs.tag !== 0x30) throw new Error('not a CRL');
  let item = tlv(der, tbs.start);
  if (item.tag === 0x02) item = tlv(der, item.end); // version
  item = tlv(der, item.end); // skip signature algorithm, now at the issuer
  const thisUpdate = tlv(der, item.end);
  const next = thisUpdate.end < tbs.end ? tlv(der, thisUpdate.end) : null;
  return {
    thisUpdate: asn1Time(der, thisUpdate),
    nextUpdate: next && (next.tag === 0x17 || next.tag === 0x18) ? asn1Time(der, next) : null
  };
}

// { state: ok | warning | expired | unavailable, nextUpdate, hoursRemaining, message }
function crlStatus(file, now = Date.now(), warnHours = WARN_HOURS) {
  if (!file) return { state: 'unavailable', message: 'No CRL configured (TLS_CRL_FILE)' };
  let dates;
  try {
    dates = parseCrlDates(fs.readFileSync(file));
  } catch (error) {
    return { state: 'unavailable', message: `CRL ${file} unreadable: ${error.message}` };
  }
  if (!dates.nextUpdate) return { state: 'ok', thisUpdate: dates.thisUpdate.toISOString(), nextUpdate: null, hoursRemaining: null };
  const hoursRemaining = Math.floor((dates.nextUpdate.getTime() - now) / 3600000);
  const base = { thisUpdate: dates.thisUpdate.toISOString(), nextUpdate: dates.nextUpdate.toISOString(), hoursRemaining };
  if (dates.nextUpdate.getTime() <= now) {
    return { ...base, state: 'expired', message: `CRL expired at ${base.nextUpdate}: every system refuses every peer; start the CertificateAuthority` };
  }
  if (hoursRemaining < warnHours) {
    return { ...base, state: 'warning', message: `CRL expires in ${hoursRemaining} h (${base.nextUpdate}); is the CertificateAuthority running?` };
  }
  return { ...base, state: 'ok' };
}

module.exports = { crlStatus, parseCrlDates, WARN_HOURS };
