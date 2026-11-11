/* System of LocalClouds (SoLC) view of the LocalCloudManager console. SPDX-License-Identifier: EPL-2.0
 * Composes local clouds from LocalCloudConfigurator's stored designs, connects them with inter-cloud exchanges, validates
 * them against the organisation policy and builds deployment packages, all through SoLocalCloudConfigurator
 * (/api/solc/* of this console, consumed through orchestration). Nothing here deploys anything. */
(function () {
  'use strict';
  const M = window.SoLocalCloudModel;
  if (!M) return;
  const q = s => document.querySelector(s);
  const esc = v => String(v ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const S = { catalog: null, lc: { entries: [], components: [] }, designs: [], packages: [], record: null, model: null, clean: '', selected: null,
    plan: null, busy: false, undo: [], redo: [], drag: null };

  const html = `<div class="section-heading"><div><p class="eyebrow">SoLocalCloudConfigurator</p><h2>System of LocalClouds</h2></div><span id="solc-state">Loading…</span></div>
<p class="section-note">Compose a System of LocalClouds from the local-cloud designs stored in LocalCloudConfigurator, connect member clouds with inter-cloud service exchanges, validate the result against the organisation policy, and build a deployment package with the scripts and documentation to install it on other hardware. Nothing here changes a running cloud.</p>
<div class="solc-toolbar"><button class="action" data-solc="new">New SoLC</button><button class="action" data-solc="save">Save design</button><button class="action" data-solc="copy">Save a copy</button><button class="action" data-solc="reload">Reload</button><button class="action" data-solc="undo">Undo</button><button class="action" data-solc="redo">Redo</button><button class="action" data-solc="validate">Validate</button><button class="action" data-solc="delete">Delete design</button></div>
<div id="solc-message" class="lcc-message" role="status" hidden></div>
<div class="solc-workspace">
 <aside class="panel solc-palette">
  <h3>Saved SoLC designs</h3><div id="solc-designs"></div>
  <h3>System of LocalClouds</h3>
  <label>Name<input id="solc-name" maxlength="63"></label>
  <label>Organisation<input id="solc-org" maxlength="63"></label>
  <label>Relay host<input id="solc-relay-host" maxlength="253" title="Host name or address of the organisation relay, as the member hosts and their containers reach it"></label>
  <label>Relay port<input id="solc-relay-port" type="number" min="1" max="65535"></label>
  <h3>Stored local-cloud designs</h3><p class="section-note">From LocalCloudConfigurator. Adding one creates a member cloud pinned to its current revision.</p>
  <label>Host of new members<input id="solc-new-host" value="host.docker.internal" maxlength="253"></label>
  <label>Architecture<select id="solc-new-arch"></select></label>
  <div id="solc-lc"></div>
 </aside>
 <article class="panel solc-canvas-panel"><div class="panel-heading"><h3>Member clouds and exchanges</h3><span id="solc-summary"></span></div>
  <div class="lcc-scroll"><svg id="solc-canvas" tabindex="0" role="application" aria-label="Member clouds of the System of LocalClouds"></svg></div>
  <p class="section-note">Drag clouds to arrange them. Arrows run from a consumer cloud to the provider cloud of an exchange.</p></article>
 <aside class="panel solc-inspector"><h3>Properties</h3><div id="solc-properties"></div></aside>
</div>
<article class="panel"><h3>Inter-cloud exchanges</h3><p class="section-note">Only the systems a member's components offer as inter-cloud consumers, and only the services they export, are offered (organisation policy). Grants are derived from these exchanges and nothing more.</p>
 <div class="solc-exchange-form"><select id="solc-x-option" aria-label="Exchange"></select><span id="solc-x-ops"></span><button class="action" data-solc="add-exchange">Add exchange</button></div>
 <div class="table-wrap"><table><thead><tr><th>Consumer</th><th>Provider service</th><th>Operations</th><th></th></tr></thead><tbody id="solc-exchanges"></tbody></table></div></article>
<article class="panel" id="solc-plan-panel" hidden><h3>Validation</h3><p id="solc-plan-summary"></p><ul id="solc-plan-problems"></ul><div class="table-wrap"><table><thead><tr><th>Cloud</th><th>Neighbours</th><th>Grants</th><th>Revocations</th><th>Checks</th></tr></thead><tbody id="solc-plan-clouds"></tbody></table></div></article>
<article class="panel"><h3>Deployment package</h3><p class="section-note">The package holds the member and organisation configuration, the deployment kit and documentation for Linux amd64, Linux arm64 and macOS, and no secret: keys and passwords are generated on each host. Image archives or the source archive are added afterwards on a machine with Docker (<code>kit/bin/assemble-package.sh</code>).</p>
 <div class="solc-package-form"><fieldset><legend>Images</legend><label class="lcc-check"><input type="radio" name="solc-mode" value="prebuilt">Prebuilt archives per architecture</label><label class="lcc-check"><input type="radio" name="solc-mode" value="build">Build on each host from the pinned source</label></fieldset>
 <fieldset><legend>Architectures</legend><span id="solc-archs"></span></fieldset><label class="lcc-check"><input type="checkbox" id="solc-discovery">Verify global service discovery over the relay</label>
 <button class="action" data-solc="build">Build package</button></div>
 <div class="table-wrap"><table><thead><tr><th>Package</th><th>Design</th><th>Images</th><th>Size</th><th>SHA-256</th><th>Built</th><th></th></tr></thead><tbody id="solc-packages"></tbody></table></div></article>`;

  async function api(path, method = 'GET', body) {
    const r = await fetch('/api/solc' + path, { method, headers: { 'Content-Type': 'application/json', ...(method === 'GET' ? {} : { 'X-Arrowhead-Action': 'confirm' }) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }), signal: AbortSignal.timeout(90000) });
    const data = await r.json().catch(() => ({}));
    if (!r.ok) throw Error(data.error || 'SoLocalCloudConfigurator request failed (' + r.status + ')');
    return data;
  }
  function message(t, error = false) { const m = q('#solc-message'); m.textContent = t; m.hidden = !t; m.classList.toggle('error', error); }
  const dirty = () => S.model && JSON.stringify(S.model) !== S.clean;
  const discard = () => !dirty() || confirm('Discard unsaved SoLC design changes?');
  function change(fn) { S.undo.push(M.clone(S.model)); if (S.undo.length > 50) S.undo.shift(); S.redo = []; S.model = fn(S.model) || S.model; S.plan = null; render(); }
  function adopt(record) {
    S.record = record ? { id: record.id, revision: record.revision } : null;
    S.model = record ? record.design : M.fresh(S.catalog);
    S.model.layout = S.model.layout || {}; S.model.exchanges = S.model.exchanges || []; S.model.members = S.model.members || [];
    S.clean = JSON.stringify(S.model); S.undo = []; S.redo = []; S.selected = null; S.plan = null; render();
  }
  const member = cloud => S.model.members.find(m => m.cloud === cloud);
  const lcCurrent = id => S.lc.entries.find(e => e.id === id);
  const componentName = id => (S.lc.components.find(c => c.id === id) || {}).name || id;

  async function load() {
    const [catalog, lc, designs, packages] = await Promise.all([api('/catalog'), api('/lc-designs').catch(e => ({ entries: [], components: [], error: e.message })),
      api('/designs'), api('/packages')]);
    S.catalog = catalog; S.lc = lc; S.designs = designs.entries; S.packages = packages.entries;
    if (!S.model) adopt(null);
    if (lc.error) message('Stored local-cloud designs are unavailable: ' + lc.error, true);
    render();
  }

  function renderCanvas() {
    const svg = q('#solc-canvas'), d = S.model, W = 290, H = 150;
    d.members.forEach((m, i) => { if (!d.layout[m.cloud]) d.layout[m.cloud] = { x: 40 + i % 3 * 320, y: 40 + Math.floor(i / 3) * 220 }; });
    const width = Math.max(700, ...d.members.map(m => d.layout[m.cloud].x + W + 40)), height = Math.max(420, ...d.members.map(m => d.layout[m.cloud].y + H + 40));
    svg.setAttribute('viewBox', `0 0 ${width} ${height}`); svg.style.width = width + 'px'; svg.style.height = height + 'px';
    const edges = d.exchanges.map((x, i) => {
      const a = d.layout[x.consumerCloud], b = d.layout[x.providerCloud];
      if (!a || !b) return '';
      const x1 = a.x + W / 2, y1 = a.y + H / 2, x2 = b.x + W / 2, y2 = b.y + H / 2, mx = (x1 + x2) / 2, my = (y1 + y2) / 2 - 30 - i % 3 * 18;
      return `<path class="solc-edge" d="M${x1} ${y1} Q${mx} ${my} ${x2} ${y2}" marker-end="url(#solc-arrow)"/><text class="solc-edge-label" x="${mx}" y="${my}">${esc(x.consumer + ' → ' + x.provider + '.' + x.service)}</text>`;
    }).join('');
    const nodes = d.members.map(m => {
      const p = d.layout[m.cloud], cap = M.capabilities(S.catalog, m), cur = lcCurrent(m.lcDesign.id), stale = cur && cur.revision !== m.lcDesign.revision;
      const fed = d.members.length > 1 ? (cap.intercloud ? 'federation ready' : 'lacks ' + cap.missing.join(', ')) : 'single cloud';
      return `<g class="solc-node ${S.selected === m.cloud ? 'selected' : ''} ${cap.intercloud || d.members.length < 2 ? '' : 'warn'}" data-solc-node="${esc(m.cloud)}" tabindex="0" role="button" aria-label="${esc('Member cloud ' + m.cloud)}">
<rect x="${p.x}" y="${p.y}" width="${W}" height="${H}" rx="10"/><text x="${p.x + 14}" y="${p.y + 26}" class="solc-node-name">${esc(m.cloud)}</text>
<text x="${p.x + 14}" y="${p.y + 48}">${esc(m.host)} · ${esc(m.architecture)} · offset ${m.portOffset}</text>
<text x="${p.x + 14}" y="${p.y + 70}">${esc(m.lcDesign.name)} r${m.lcDesign.revision}${stale ? ' (stored: r' + cur.revision + ')' : ''}</text>
<text x="${p.x + 14}" y="${p.y + 92}">${m.lcDesign.components.length} components · ${esc(fed)}</text>
<text x="${p.x + 14}" y="${p.y + 114}" class="solc-small">consumers: ${esc(cap.consumers.join(', ') || 'none')}</text>
<text x="${p.x + 14}" y="${p.y + 134}" class="solc-small">exports: ${esc(cap.exports.map(e => e.provider + '.' + e.service).join(', ') || 'none')}</text></g>`;
    }).join('');
    svg.innerHTML = `<defs><marker id="solc-arrow" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto"><path d="M0 0L8 4L0 8Z" fill="#2f6f8f"/></marker></defs>${edges}${nodes}`;
    q('#solc-summary').textContent = `${d.members.length} member clouds · ${d.exchanges.length} exchanges`;
  }

  function renderProperties() {
    const node = q('#solc-properties'), m = member(S.selected);
    if (!m) { node.innerHTML = '<p class="section-note">Select a member cloud to set its name, host, architecture and port offset.</p>'; return; }
    const cur = lcCurrent(m.lcDesign.id), stale = cur && cur.revision !== m.lcDesign.revision;
    node.innerHTML = `<label>Cloud name<input id="solc-m-cloud" value="${esc(m.cloud)}" maxlength="63"></label>
<label>Host<input id="solc-m-host" value="${esc(m.host)}" maxlength="253"></label>
<label>Architecture<select id="solc-m-arch">${S.catalog.architectures.map(a => `<option ${a === m.architecture ? 'selected' : ''}>${esc(a)}</option>`).join('')}</select></label>
<label>Port offset<input id="solc-m-offset" type="number" step="1000" min="0" max="50000" value="${m.portOffset}"></label>
<p class="section-note">Compose project arrowhead-${esc(m.cloud.toLowerCase())}; published ports 5671–8500 plus the offset, on 127.0.0.1 of the host.</p>
<h4>Local-cloud design</h4><p>${esc(m.lcDesign.name)}, revision ${m.lcDesign.revision}${stale ? ` <strong>(LocalCloudConfigurator now holds revision ${cur.revision})</strong>` : !cur ? ' <strong>(no longer stored)</strong>' : ''}</p>
${stale ? '<button class="action" data-solc="repin">Use the stored revision</button>' : ''}
<ul class="solc-components">${m.lcDesign.components.map(c => `<li>${esc(componentName(c))}</li>`).join('')}</ul>
<button class="action danger" data-solc="remove-member">Remove from the SoLC</button>`;
  }

  function renderExchanges() {
    const opts = M.exchangeOptions(S.catalog, S.model), sel = q('#solc-x-option'), prev = sel.value;
    sel.innerHTML = opts.length ? opts.map((o, i) => `<option value="${i}">${esc(o.consumerCloud + '/' + o.consumer + ' → ' + o.providerCloud + '/' + o.provider + '.' + o.service)}</option>`).join('')
      : '<option value="">No exchange possible: add members whose components consume and export inter-cloud services</option>';
    if (prev && prev < opts.length) sel.value = prev;
    const o = opts[+sel.value];
    q('#solc-x-ops').innerHTML = o ? o.operations.map(op => `<label class="lcc-check"><input type="checkbox" class="solc-x-op" value="${esc(op)}" checked>${esc(op)}</label>`).join('') : '';
    q('#solc-exchanges').innerHTML = S.model.exchanges.map((x, i) => `<tr><td>${esc(x.consumerCloud + '/' + x.consumer)}</td><td>${esc(x.providerCloud + '/' + x.provider + '.' + x.service)}</td><td>${esc(x.operations.join(', '))}</td><td><button class="action danger" data-solc-x-remove="${i}">Remove</button></td></tr>`).join('')
      || '<tr><td colspan="4">No exchanges: the members run as separate local clouds.</td></tr>';
  }

  function renderPlan() {
    const panel = q('#solc-plan-panel'), p = S.plan;
    panel.hidden = !p;
    if (!p) return;
    const ev = p.evaluation || {};
    q('#solc-plan-summary').textContent = p.allowed ? 'Allowed by the organisation policy. The package can be built after saving.' : 'Not allowed: resolve the problems below.';
    q('#solc-plan-summary').className = p.allowed ? 'ok' : 'error';
    const items = [...(p.problems || []).map(x => ['design', x]), ...(ev.violations || []).map(v => [v.rule, v.message]),
      ...((p.stale || []).map(s => ['pin', `${s.cloud}: ${s.lcDesign} is pinned at revision ${s.pinned}; LocalCloudConfigurator holds ${s.current ?? 'none'}`]))];
    if (p.stale === null) items.push(['pin', 'LocalCloudConfigurator is unavailable: pinned revisions not checked']);
    q('#solc-plan-problems').innerHTML = items.map(([r, t]) => `<li><strong>${esc(r)}</strong> ${esc(t)}</li>`).join('');
    q('#solc-plan-clouds').innerHTML = Object.values(ev.clouds || {}).map(c => `<tr><td>${esc(c.cloud)}</td><td>${esc(c.neighbours.map(n => n.name).join(', ') || '–')}</td>
<td>${esc(c.grants.map(g => g.cloud.split('|')[0] + '/' + g.consumers.join('+') + ' → ' + g.provider + '.' + g.service).join('; ') || '–')}</td><td>${c.revokes.length}</td><td>${c.verify.length} verify · ${c.discovery.length} discovery</td></tr>`).join('');
  }

  function renderPackages() {
    const pk = S.model.packaging;
    document.querySelectorAll('input[name="solc-mode"]').forEach(r => { r.checked = r.value === pk.imageMode; });
    q('#solc-archs').innerHTML = S.catalog.architectures.map(a => `<label class="lcc-check"><input type="checkbox" class="solc-arch" value="${esc(a)}" ${pk.architectures.includes(a) ? 'checked' : ''}>${esc(a)}</label>`).join('');
    q('#solc-discovery').checked = !!S.model.verifyDiscovery;
    const kib = n => n > 1048576 ? (n / 1048576).toFixed(1) + ' MiB' : Math.ceil(n / 1024) + ' KiB';
    q('#solc-packages').innerHTML = S.packages.map(p => `<tr><td><a href="/api/solc/packages/${esc(p.id)}" download="${esc(p.fileName)}">${esc(p.fileName)}</a></td><td>${esc(p.designName)} r${p.designRevision}</td>
<td>${esc(p.imageMode)} · ${esc(p.architectures.join(', '))}</td><td>${kib(p.sizeBytes)}</td><td><code title="${esc(p.sha256)}">${esc(p.sha256.slice(0, 16))}…</code></td><td>${esc(p.createdAt.replace('T', ' ').slice(0, 19))}<br><small>${esc(p.createdBy)}</small></td>
<td><button class="action danger" data-solc-pkg-remove="${esc(p.id)}">Remove</button></td></tr>`).join('') || '<tr><td colspan="7">No packages yet.</td></tr>';
  }

  function render() {
    if (!S.catalog || !S.model) return;
    const d = S.model;
    q('#solc-state').textContent = (S.record ? `Saved design, revision ${S.record.revision}` : 'New design') + (dirty() ? ' · unsaved changes' : '');
    q('#solc-designs').innerHTML = S.designs.map(e => `<button type="button" class="lcc-card ${S.record && S.record.id === e.id ? 'in-design' : ''}" data-solc-open="${esc(e.id)}"><strong>${esc(e.name)}</strong><small>r${e.revision} · ${esc(e.members.join(', ') || 'no members')} · ${e.exchanges} exchanges</small></button>`).join('')
      || '<p class="section-note">No saved SoLC designs.</p>';
    for (const [id, v] of [['#solc-name', d.name], ['#solc-org', d.organization], ['#solc-relay-host', d.relay.host], ['#solc-relay-port', d.relay.port]]) {
      if (document.activeElement !== q(id)) q(id).value = v;
    }
    q('#solc-new-arch').innerHTML = S.catalog.architectures.map(a => `<option>${esc(a)}</option>`).join('');
    q('#solc-lc').innerHTML = S.lc.entries.map(e => `<button type="button" class="lcc-card" data-solc-add="${esc(e.id)}"><strong>${esc(e.name)}</strong><small>r${e.revision} · ${e.components.length} components · ${esc(M.capabilities(S.catalog, { lcDesign: e }).intercloud ? 'inter-cloud capable' : 'no Gatekeeper/GateTunnel')}</small></button>`).join('')
      || '<p class="section-note">LocalCloudConfigurator holds no stored designs. Save one in the LC view.</p>';
    renderCanvas(); renderProperties(); renderExchanges(); renderPlan(); renderPackages();
    for (const [a, off] of [['undo', !S.undo.length], ['redo', !S.redo.length], ['delete', !S.record], ['build', !S.record || dirty()]]) {
      q(`[data-solc="${a}"]`).disabled = off || S.busy;
    }
  }

  async function save(copy) {
    const local = M.problems(S.model, S.catalog);
    if (local.length) throw Error(local.join(' '));
    const r = S.record && !copy ? await api('/designs/' + S.record.id, 'PUT', M.draft(S.model, S.record.revision))
      : await api('/designs', 'POST', M.draft(copy ? { ...S.model, name: M.cloudName(S.model.name + 'Copy', S.designs.map(e => e.name)) } : S.model));
    adopt(r);
    S.designs = (await api('/designs')).entries;
    render();
    message(`Saved ${r.design.name} (revision ${r.revision}).`);
  }

  async function action(a) {
    switch (a) {
      case 'new': if (discard()) { adopt(null); message(''); } break;
      case 'save': await save(false); break;
      case 'copy': await save(true); break;
      case 'reload': if (discard()) { await load(); if (S.record) adopt(await api('/designs/' + S.record.id)); message('Reloaded.'); } break;
      case 'undo': if (S.undo.length) { S.redo.push(M.clone(S.model)); S.model = S.undo.pop(); S.plan = null; render(); } break;
      case 'redo': if (S.redo.length) { S.undo.push(M.clone(S.model)); S.model = S.redo.pop(); S.plan = null; render(); } break;
      case 'validate': S.plan = await api('/plan', 'POST', M.draft(S.model, null, S.record && S.record.id)); render(); break;
      case 'delete':
        if (S.record && confirm('Delete this SoLC design? Built packages are kept.')) {
          await api('/designs/' + S.record.id, 'DELETE', { expectedRevision: S.record.revision });
          adopt(null); S.designs = (await api('/designs')).entries; render(); message('SoLC design deleted.');
        }
        break;
      case 'remove-member':
        if (confirm(`Remove ${S.selected} and its exchanges from the SoLC?`)) { const c = S.selected; S.selected = null; change(d => M.removeMember(d, c)); }
        break;
      case 'repin': {
        const m = member(S.selected), cur = lcCurrent(m.lcDesign.id);
        change(d => { const x = M.clone(d); const t = x.members.find(y => y.cloud === m.cloud); t.lcDesign = { id: cur.id, name: cur.name, revision: cur.revision, components: [...cur.components] }; return x; });
        break;
      }
      case 'add-exchange': {
        const o = M.exchangeOptions(S.catalog, S.model)[+q('#solc-x-option').value];
        if (!o) throw Error('No exchange selected.');
        const ops = [...document.querySelectorAll('.solc-x-op:checked')].map(i => i.value);
        change(d => M.addExchange(d, { ...o, operations: ops }));
        break;
      }
      case 'build': {
        if (!S.record || dirty()) throw Error('Save the design first.');
        message('Building the package…');
        const p = await api('/packages', 'POST', { designId: S.record.id, expectedRevision: S.record.revision });
        S.packages = (await api('/packages')).entries; render();
        message(`Built ${p.fileName} (${Math.ceil(p.sizeBytes / 1024)} KiB, SHA-256 ${p.sha256}). Download it from the package list.`);
        break;
      }
    }
  }

  function bind() {
    const host = q('#view-solc'), svg = q('#solc-canvas');
    host.addEventListener('click', e => {
      if (S.busy) return;
      const t = e.target, act = t.closest('[data-solc]'), open = t.closest('[data-solc-open]'), add = t.closest('[data-solc-add]'),
        xr = t.closest('[data-solc-x-remove]'), pr = t.closest('[data-solc-pkg-remove]');
      if (!(act || open || add || xr || pr)) return;
      S.busy = true; render();
      Promise.resolve().then(async () => {
        if (act) return action(act.dataset.solc);
        if (open && discard()) return adopt(await api('/designs/' + open.dataset.solcOpen));
        if (add) {
          const e2 = S.lc.entries.find(x => x.id === add.dataset.solcAdd);
          change(d => M.addMember(d, e2, q('#solc-new-host').value.trim(), q('#solc-new-arch').value));
          S.selected = S.model.members[S.model.members.length - 1].cloud; render();
        }
        if (xr) change(d => { const x = M.clone(d); x.exchanges.splice(+xr.dataset.solcXRemove, 1); return x; });
        if (pr && confirm('Remove this package from SoLocalCloudConfigurator? Downloaded copies are not affected.')) {
          await api('/packages/' + pr.dataset.solcPkgRemove, 'DELETE', {}); S.packages = (await api('/packages')).entries;
        }
      }).catch(err => message(err.message, true)).finally(() => { S.busy = false; render(); });
    });
    host.addEventListener('change', e => {
      const n = e.target;
      try {
        if (n.id === 'solc-name') change(d => ({ ...d, name: n.value.trim() }));
        if (n.id === 'solc-org') change(d => ({ ...d, organization: n.value.trim() }));
        if (n.id === 'solc-relay-host') change(d => ({ ...d, relay: { ...d.relay, host: n.value.trim() } }));
        if (n.id === 'solc-relay-port') change(d => ({ ...d, relay: { ...d.relay, port: +n.value } }));
        if (n.id === 'solc-m-cloud') { const from = S.selected; change(d => M.renameMember(d, from, n.value.trim())); S.selected = n.value.trim(); render(); }
        const edit = (f, v) => change(d => { const x = M.clone(d); x.members.find(m => m.cloud === S.selected)[f] = v; return x; });
        if (n.id === 'solc-m-host') edit('host', n.value.trim());
        if (n.id === 'solc-m-arch') edit('architecture', n.value);
        if (n.id === 'solc-m-offset') edit('portOffset', +n.value);
        if (n.name === 'solc-mode') change(d => ({ ...d, packaging: { ...d.packaging, imageMode: n.value } }));
        if (n.classList.contains('solc-arch')) change(d => ({ ...d, packaging: { ...d.packaging, architectures: [...document.querySelectorAll('.solc-arch:checked')].map(i => i.value) } }));
        if (n.id === 'solc-discovery') change(d => ({ ...d, verifyDiscovery: n.checked }));
        if (n.id === 'solc-x-option') renderExchanges();
      } catch (err) { message(err.message, true); render(); }
    });
    svg.addEventListener('pointerdown', e => {
      const node = e.target.closest('[data-solc-node]');
      if (!node || S.busy || e.button !== 0) return;
      const c = node.dataset.solcNode;
      S.selected = c; S.drag = { c, x: e.clientX, y: e.clientY, p: { ...S.model.layout[c] }, before: M.clone(S.model) };
      svg.setPointerCapture(e.pointerId); renderProperties();
    });
    svg.addEventListener('pointermove', e => {
      const d = S.drag;
      if (!d) return;
      S.model.layout[d.c] = { x: Math.min(10000, Math.max(0, Math.round(d.p.x + e.clientX - d.x))), y: Math.min(10000, Math.max(0, Math.round(d.p.y + e.clientY - d.y))) };
      renderCanvas();
    });
    const end = () => { if (!S.drag) return; if (JSON.stringify(S.drag.before.layout) !== JSON.stringify(S.model.layout)) { S.undo.push(S.drag.before); S.redo = []; } S.drag = null; render(); };
    svg.addEventListener('pointerup', end); svg.addEventListener('pointercancel', end);
    svg.addEventListener('keydown', e => { const node = e.target.closest('[data-solc-node]'); if (node && ['Enter', ' '].includes(e.key)) { e.preventDefault(); S.selected = node.dataset.solcNode; render(); } });
    window.addEventListener('beforeunload', e => { if (dirty()) { e.preventDefault(); e.returnValue = ''; } });
  }

  async function start() {
    const features = await fetch('/api/features').then(r => r.ok ? r.json() : {}).catch(() => ({}));
    if (!features.soLocalCloudConfiguration) return;
    const nav = document.createElement('button');
    nav.className = 'rail-button'; nav.dataset.view = 'solc'; nav.setAttribute('aria-label', 'System of LocalClouds'); nav.innerHTML = '<span>SoLC</span>';
    q('[data-view="plant"]').before(nav);
    const section = document.createElement('section');
    section.className = 'view'; section.hidden = true; section.id = 'view-solc'; section.innerHTML = html;
    q('#view-plant').before(section);
    bind();
    nav.addEventListener('click', () => {
      setView('solc'); S.busy = true;
      load().catch(e => message(e.message, true)).finally(() => { S.busy = false; render(); });
    });
  }

  window.SoLocalCloudEditor = { state: S, load };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
})();
