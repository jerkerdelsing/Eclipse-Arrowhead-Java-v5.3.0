/* System of LocalClouds (SoLC) design model of the LocalCloudManager console. SPDX-License-Identifier: EPL-2.0
 * Pure functions (also used by ui/test/solc-model.test.cjs). The server (SoLocalCloudConfigurator) is authoritative:
 * it pins each member's local-cloud design from LocalCloudConfigurator and evaluates the organisation policy. */
(function (root) {
  'use strict';
  const NAME = /^[A-Za-z][A-Za-z0-9]{0,62}$/;
  const DESIGN_NAME = /^[A-Za-z][A-Za-z0-9_-]{0,62}$/;
  const STEP = 1000;

  const clone = v => JSON.parse(JSON.stringify(v));

  function fresh(catalog) {
    return {
      name: 'NewSoLC', organization: catalog.organization,
      relay: { host: 'host.docker.internal', port: catalog.relayPort },
      members: [], exchanges: [], verifyDiscovery: true,
      packaging: { imageMode: 'prebuilt', architectures: [...catalog.architectures] }, layout: {}
    };
  }

  /** A cloud name from a design name: letters and digits, starting with a letter, unique (case-insensitively). */
  function cloudName(base, taken) {
    let n = String(base || 'Cloud').replace(/[^A-Za-z0-9]/g, '');
    if (!/^[A-Za-z]/.test(n)) n = 'Cloud' + n;
    n = n.slice(0, 50) || 'Cloud';
    const lower = new Set(taken.map(t => t.toLowerCase()));
    let candidate = n, i = 2;
    while (lower.has(candidate.toLowerCase())) candidate = n + i++;
    return candidate;
  }

  /** The next free port offset on a host (multiples of 1000). */
  function nextOffset(design, host) {
    const used = new Set(design.members.filter(m => (m.host || '').toLowerCase() === (host || '').toLowerCase()).map(m => m.portOffset));
    let o = 0;
    while (used.has(o)) o += STEP;
    return o;
  }

  function addMember(design, lcEntry, host, architecture) {
    const d = clone(design);
    const cloud = cloudName(lcEntry.name, d.members.map(m => m.cloud));
    const h = host || 'host.docker.internal';
    d.members.push({
      cloud, host: h, architecture, portOffset: nextOffset(d, h),
      lcDesign: { id: lcEntry.id, name: lcEntry.name, revision: lcEntry.revision, components: [...(lcEntry.components || [])] }
    });
    d.layout[cloud] = { x: 40 + (d.members.length - 1) % 3 * 320, y: 40 + Math.floor((d.members.length - 1) / 3) * 220 };
    return d;
  }

  function removeMember(design, cloud) {
    const d = clone(design);
    d.members = d.members.filter(m => m.cloud !== cloud);
    d.exchanges = d.exchanges.filter(x => x.consumerCloud !== cloud && x.providerCloud !== cloud);
    delete d.layout[cloud];
    return d;
  }

  function renameMember(design, from, to) {
    if (!NAME.test(to)) throw Error('Cloud names are letters and digits, starting with a letter.');
    if (design.members.some(m => m.cloud !== from && m.cloud.toLowerCase() === to.toLowerCase())) throw Error('Another member has that name.');
    const d = clone(design);
    for (const m of d.members) if (m.cloud === from) m.cloud = to;
    for (const x of d.exchanges) {
      if (x.consumerCloud === from) x.consumerCloud = to;
      if (x.providerCloud === from) x.providerCloud = to;
    }
    if (d.layout[from]) { d.layout[to] = d.layout[from]; delete d.layout[from]; }
    return d;
  }

  /** Inter-cloud abilities of a member: consumers it may offer and services it exports, from its components. */
  function capabilities(catalog, member) {
    const consumers = new Set(), exports = [];
    for (const c of (member && member.lcDesign && member.lcDesign.components) || []) {
      const cap = catalog.components[c];
      if (!cap) continue;
      cap.consumers.forEach(s => consumers.add(s));
      cap.exports.forEach(e => exports.push(e));
    }
    const missing = catalog.intercloudComponents.filter(c => !((member && member.lcDesign && member.lcDesign.components) || []).includes(c));
    return { consumers: [...consumers].sort(), exports, intercloud: missing.length === 0, missing };
  }

  /** Every exchange the catalog allows between two different members (consumer system x export). */
  function exchangeOptions(catalog, design) {
    const out = [];
    for (const c of design.members) {
      const cc = capabilities(catalog, c);
      for (const p of design.members) {
        if (p.cloud === c.cloud) continue;
        for (const e of capabilities(catalog, p).exports) {
          for (const consumer of cc.consumers) {
            out.push({ consumerCloud: c.cloud, consumer, providerCloud: p.cloud, provider: e.provider, service: e.service, operations: [...e.operations] });
          }
        }
      }
    }
    return out;
  }

  const key = x => [x.consumerCloud, x.consumer, x.providerCloud, x.provider, x.service].join('|');

  function addExchange(design, exchange) {
    if (!exchange.operations || !exchange.operations.length) throw Error('Choose at least one operation.');
    const d = clone(design);
    d.exchanges = d.exchanges.filter(x => key(x) !== key(exchange));
    d.exchanges.push(clone(exchange));
    return d;
  }

  /** Quick checks before a round trip; the server's plan is authoritative. */
  function problems(design, catalog) {
    const p = [];
    if (!DESIGN_NAME.test(design.name || '')) p.push('The SoLC name is letters, digits, - and _, starting with a letter.');
    if (!NAME.test(design.organization || '')) p.push('The organisation is letters and digits, starting with a letter.');
    if (!design.relay || !design.relay.host || !(design.relay.port >= 1 && design.relay.port <= 65535)) p.push('Give the relay a host and a port.');
    if (!design.members.length) p.push('Add at least one member cloud from the stored local-cloud designs.');
    const seen = {};
    for (const m of design.members) {
      const k = (m.host || '').toLowerCase() + ':' + m.portOffset;
      if (seen[k]) p.push(`${m.cloud} and ${seen[k]} use the same port offset on ${m.host}.`);
      seen[k] = m.cloud;
      if (m.portOffset % STEP || m.portOffset < 0 || m.portOffset > 50000) p.push(`${m.cloud}: the port offset is a multiple of 1000 up to 50000.`);
      if (!design.packaging.architectures.includes(m.architecture)) p.push(`${m.cloud}: its architecture ${m.architecture} is not packaged.`);
      if (design.members.length > 1 && !capabilities(catalog, m).intercloud) p.push(`${m.cloud}: its local-cloud design lacks ${capabilities(catalog, m).missing.join(', ')} for the federation.`);
    }
    if (!design.packaging.architectures.length) p.push('Package at least one architecture.');
    return p;
  }

  /** Request body of create/update/plan. */
  function draft(design, expectedRevision, designId) {
    const body = { design: clone(design) };
    if (expectedRevision != null) body.expectedRevision = expectedRevision;
    if (designId) body.designId = designId;
    return body;
  }

  const api = { NAME, DESIGN_NAME, clone, fresh, cloudName, nextOffset, addMember, removeMember, renameMember, capabilities, exchangeOptions, addExchange, problems, draft, key };
  if (typeof module !== 'undefined' && module.exports) module.exports = api; else root.SoLocalCloudModel = api;
})(typeof window !== 'undefined' ? window : globalThis);
