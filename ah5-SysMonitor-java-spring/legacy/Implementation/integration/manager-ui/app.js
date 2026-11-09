'use strict';

const state = { data: null, dataManager: null, sysMonitor: null, selectedSmSystem: '', loadingSm: false, orchestration: null, activeView: 'orchestration', filter: '', registryFilter: '', selectedSeries: '', refreshTimer: null, refreshing: false, loadingData: false, loadingOrchestration: false };
const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>'"]/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[char]));
}

function shortName(name) {
  return String(name || '?').replace(/[^A-Z]/g, '').slice(0, 3) || String(name || '?').slice(0, 2).toUpperCase();
}

function formatTime(iso) {
  if (!iso) return '—';
  return new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(new Date(iso));
}

function showToast(message) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.hidden = false;
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => { toast.hidden = true; }, 3800);
}

function setView(view) {
  state.activeView = view;
  $$('.rail-button').forEach((button) => button.classList.toggle('active', button.dataset.view === view));
  $$('.view').forEach((section) => {
    const active = section.id === `view-${view}`;
    section.hidden = !active;
    section.classList.toggle('active', active);
  });
  if (view === 'runtime' && state.data) {
    loadLogs($('#log-service').value);
    requestAnimationFrame(() => renderActivity(state.data.history));
  }
  if (view === 'data') {
    loadDataManager();
    requestAnimationFrame(() => {
      const series = state.dataManager?.series.find((item) => item.name === state.selectedSeries);
      if (series) renderDataChart(series);
    });
  }
  if (view === 'orchestration') loadOrchestration();
  if (view === 'sysmonitor') loadSysMonitor();
}

function serviceEndpoint(service) {
  const intf = service.interfaces?.[0];
  if (!intf) return '—';
  const props = intf.properties || {};
  const address = props.accessAddresses?.[0] || 'unknown';
  return `${intf.protocol || 'http'}://${address}:${props.accessPort || '—'}${props.basePath || ''}`;
}

function addressLabel(address) {
  if (typeof address === 'string') return address;
  return address?.address || address?.host || address?.value || '—';
}

function registryBadge(label, active, kind) {
  return `<span class="registry-badge ${active ? kind : 'inactive'}"><i></i>${escapeHtml(label)}</span>`;
}

function registrySystemCard(system) {
  const services = system.services || [];
  return `<article class="registry-system">
    <div class="registry-node-head">
      <div><span class="registry-kind">System</span><strong>${escapeHtml(system.name)}</strong></div>
      <div class="registry-badges">${registryBadge('Registered', true, 'registered')}${registryBadge(system.deployed ? 'Deployed' : 'Not deployed', system.deployed, 'deployed')}</div>
    </div>
    <div class="registry-system-meta"><span>v${escapeHtml(system.version || '—')}</span><span>${escapeHtml(addressLabel(system.addresses?.[0]))}</span><span>${services.length} service${services.length === 1 ? '' : 's'}</span></div>
    <div class="registry-services">${services.map((service) => `<div class="registry-service">
      <span class="registry-service-line"><i></i><strong>${escapeHtml(service.serviceDefinition?.name || 'Unknown')}</strong></span>
      <code>${escapeHtml(service.instanceId)}</code>
      <span>${escapeHtml(service.interfaces?.[0]?.protocol || '—')} · ${escapeHtml(service.interfaces?.[0]?.policy || '—')}</span>
      ${registryBadge(service.deployed ? 'Deployed' : 'Provider offline', service.deployed, 'deployed')}
    </div>`).join('') || '<p class="registry-empty">No registered service instances.</p>'}</div>
  </article>`;
}

function renderRegistry(registry) {
  const summary = registry?.summary || {};
  $('#registry-device-count').textContent = summary.registeredDevices ?? 0;
  $('#registry-device-deployed').textContent = `${summary.deployedDevices ?? 0} deployed`;
  $('#registry-system-count').textContent = summary.registeredSystems ?? 0;
  $('#registry-system-deployed').textContent = `${summary.deployedSystems ?? 0} deployed`;
  $('#registry-service-count').textContent = summary.registeredServices ?? 0;
  $('#registry-service-deployed').textContent = `${summary.deployedServices ?? 0} deployed`;
  $('#registry-runtime-count').textContent = summary.runtimeOnly ?? 0;

  const query = state.registryFilter.trim().toLowerCase();
  const groups = (registry?.devices || []).map((device) => ({
    ...device,
    systems: (device.systems || []).filter((system) => {
      const haystack = [device.name, system.name, ...(system.services || []).flatMap((service) => [service.serviceDefinition?.name, service.instanceId])].join(' ').toLowerCase();
      return !query || haystack.includes(query);
    })
  })).filter((device) => device.systems.length || (!query && device.registered) || device.name.toLowerCase().includes(query));

  $('#registry-map').innerHTML = groups.map((device) => `<section class="registry-device">
    <div class="registry-device-head">
      <div class="registry-device-title"><span class="registry-device-icon">DV</span><div><span class="registry-kind">Device</span><h3>${escapeHtml(device.name)}</h3><small>${escapeHtml((device.addresses || []).map(addressLabel).join(', ') || 'No address published')}</small></div></div>
      <div class="registry-device-summary">${registryBadge(device.registered ? 'Registered' : 'Unassigned', device.registered, 'registered')}${registryBadge(device.deployed ? 'Deployed' : 'Not deployed', device.deployed, 'deployed')}<span>${device.systems.length} systems · ${device.serviceCount} services</span></div>
    </div>
    <div class="registry-systems">${device.systems.map(registrySystemCard).join('') || '<p class="registry-empty">This device has no registered systems.</p>'}</div>
  </section>`).join('') || '<div class="registry-map-empty"><strong>No matching registry entries</strong><span>Try a different device, system or service name.</span></div>';

  const runtimeOnly = (registry?.runtimeOnly || []).filter((entry) => !query || `${entry.name} ${entry.composeService}`.toLowerCase().includes(query));
  $('#registry-runtime-only').innerHTML = runtimeOnly.map((entry) => `<div class="registry-runtime-item">
    <div><strong>${escapeHtml(entry.name)}</strong><code>${escapeHtml(entry.composeService)}</code></div>
    ${registryBadge(entry.deployed ? 'Running' : escapeHtml(entry.runtime?.state || 'Stopped'), entry.deployed, 'deployed')}
  </div>`).join('') || '<p class="registry-empty">No deployed runtime systems are missing from ServiceRegistry.</p>';
}

function renderMetrics(data) {
  $('#metric-running').textContent = data.summary.running;
  $('#metric-containers').textContent = `of ${data.summary.containers} containers`;
  $('#metric-systems').textContent = data.summary.registeredSystems;
  $('#metric-services').textContent = data.summary.registeredServices;
  $('#metric-incidents').textContent = data.summary.incidents ?? data.summary.unhealthy;
  $('#metric-incidents').parentElement.title = data.summary.unregisteredServices?.length
    ? `Not registered: ${data.summary.unregisteredServices.join(', ')}`
    : '';
  $('#updated-at').textContent = `Updated ${formatTime(data.collectedAt)}`;
  const connection = $('#connection');
  connection.classList.toggle('online', data.cloud.registryReachable && data.cloud.dockerReachable);
  connection.lastElementChild.textContent = data.cloud.registryReachable ? 'Live telemetry' : 'Degraded';
  $('#registration-state').textContent = data.registration.registered
    ? `Manager registered · ${data.registration.instanceId}`
    : `Manager registration pending${data.registration.error ? ` · ${data.registration.error}` : ''}`;
}

function renderTopology(systems) {
  const preferred = ['DynamicServiceOrchestration', 'ConsumerAuthorization', 'ServiceRegistry', 'Onboarding', 'Gatekeeper', 'GateTunnel', 'LocalCloudManager'];
  const byName = new Map(systems.map((system) => [system.name, system]));
  const selected = preferred.map((name) => byName.get(name)).filter(Boolean);
  $('#topology').innerHTML = selected.map((system) => {
    const running = system.runtime?.state === 'running';
    const registryClass = system.name === 'ServiceRegistry' ? ' registry' : '';
    return `<button class="topology-node${registryClass}" data-system="${escapeHtml(system.name)}">
      <span class="node-top"><span class="node-name">${escapeHtml(system.name)}</span><i class="node-state ${running ? 'up' : ''}"></i></span>
      <span class="node-role">${system.serviceCount} services · ${escapeHtml(system.version || 'unknown')}</span>
    </button>`;
  }).join('');
}

function renderMessaging(data) {
  const infrastructure = data.infrastructure || [];
  $('#mqtt-infrastructure').innerHTML = infrastructure.map((item) => {
    const running = item.runtime?.state === 'running';
    return `<article class="infrastructure-card">
      <div class="infra-icon">MQ</div>
      <div class="infra-copy">
        <span class="node-top"><strong>${escapeHtml(item.name)}</strong><i class="node-state ${running ? 'up' : ''}"></i></span>
        <span>${escapeHtml(item.kind)}</span>
        <code>${escapeHtml(item.endpoint)}</code>
        <small>${escapeHtml(item.protocol)} · ${escapeHtml(item.security)}</small>
      </div>
      <div class="infra-actions">
        <button class="text-button" data-log="${escapeHtml(item.runtime?.service || '')}">Logs</button>
        <button class="action" data-action="restart" data-service="${escapeHtml(item.runtime?.service || '')}" ${item.runtime?.managed ? '' : 'disabled'}>Restart</button>
      </div>
    </article>`;
  }).join('') || '<p class="muted">No messaging infrastructure reported.</p>';

  $('#orchestration-flow').innerHTML = (data.orchestrations || []).map((flow) => `<article class="flow-row ${flow.active ? 'active' : ''}">
    <div class="flow-system">${escapeHtml(flow.source)}</div>
    <div class="flow-link"><span>${escapeHtml(flow.label)}</span><i></i><small>${escapeHtml(flow.protocol)}</small></div>
    <div class="flow-system">${escapeHtml(flow.target)}</div>
    <span class="flow-state">${flow.active ? 'Active' : 'Inactive'}</span>
  </article>`).join('') || '<p class="muted">No orchestration flows reported.</p>';
}

function renderActivity(history) {
  const svg = $('#activity-chart');
  const width = Math.max(svg.clientWidth || 320, 280);
  const height = Math.max(svg.clientHeight || 210, 180);
  const pad = { x: 36, y: 24 };
  const values = history.length ? history : [{ running: 0, registeredSystems: 0 }];
  const max = Math.max(1, ...values.flatMap((point) => [point.running, point.registeredSystems]));
  const x = (index) => pad.x + (values.length === 1 ? 0 : index / (values.length - 1)) * (width - pad.x * 2);
  const y = (value) => height - pad.y - (value / max) * (height - pad.y * 2);
  const line = (key) => values.map((point, index) => `${index ? 'L' : 'M'}${x(index).toFixed(1)},${y(point[key]).toFixed(1)}`).join(' ');
  const area = `${line('running')} L${x(values.length - 1)},${height - pad.y} L${x(0)},${height - pad.y} Z`;
  svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
  const firstTime = values[0]?.at;
  const lastTime = values.at(-1)?.at;
  svg.innerHTML = `
    <line class="chart-grid" x1="${pad.x}" y1="${y(max)}" x2="${width - pad.x}" y2="${y(max)}"></line>
    <line class="chart-grid" x1="${pad.x}" y1="${y(0)}" x2="${width - pad.x}" y2="${y(0)}"></line>
    <path class="chart-area" d="${area}"></path>
    <path class="chart-running" d="${line('running')}"></path>
    <path class="chart-systems" d="${line('registeredSystems')}"></path>
    <text class="chart-label" x="${pad.x}" y="${y(max) - 7}">${max}</text>
    <text class="chart-label" x="${pad.x}" y="${height - 5}">${firstTime ? formatTime(firstTime) : '5 min ago'}</text>
    <text class="chart-label" x="${width - pad.x}" y="${height - 5}" text-anchor="end">${lastTime ? formatTime(lastTime) : 'now'}</text>`;
}

function measurementValue(entry) {
  const value = entry?.value || {};
  return value.v ?? value.vs ?? value.vb ?? value.vd ?? value.s ?? '—';
}

function renderDataChart(series, selector = '#data-chart') {
  const svg = $(selector);
  const width = Math.max(svg.clientWidth || 760, 320);
  const height = Math.max(svg.clientHeight || 310, 220);
  const pad = { left: 48, right: 20, top: 18, bottom: 30 };
  const points = series?.points || [];
  svg.setAttribute('viewBox', `0 0 ${width} ${height}`);
  if (!points.length) {
    svg.innerHTML = `<text class="chart-empty" x="${width / 2}" y="${height / 2}" text-anchor="middle">No numeric measurements</text>`;
    return;
  }
  const values = points.map((point) => point.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;
  const x = (index) => pad.left + (points.length === 1 ? 0 : index / (points.length - 1)) * (width - pad.left - pad.right);
  const y = (value) => pad.top + ((max - value) / range) * (height - pad.top - pad.bottom);
  const line = points.map((point, index) => `${index ? 'L' : 'M'}${x(index).toFixed(1)},${y(point.value).toFixed(1)}`).join(' ');
  const dots = points.slice(-80).map((point, offset) => {
    const index = points.length - Math.min(points.length, 80) + offset;
    return `<circle class="data-dot" cx="${x(index)}" cy="${y(point.value)}" r="2.4"><title>${escapeHtml(point.value)} at ${escapeHtml(formatTime(point.time * 1000))}</title></circle>`;
  }).join('');
  svg.innerHTML = `
    <line class="chart-grid" x1="${pad.left}" y1="${y(max)}" x2="${width - pad.right}" y2="${y(max)}"></line>
    <line class="chart-grid" x1="${pad.left}" y1="${y(min)}" x2="${width - pad.right}" y2="${y(min)}"></line>
    <path class="data-line" d="${line}"></path>${dots}
    <text class="chart-label" x="8" y="${y(max) + 4}">${max.toFixed(1)}</text>
    <text class="chart-label" x="8" y="${y(min) + 4}">${min.toFixed(1)}</text>
    <text class="chart-label" x="${pad.left}" y="${height - 5}">${formatTime(points[0].time * 1000)}</text>
    <text class="chart-label" x="${width - pad.right}" y="${height - 5}" text-anchor="end">${formatTime(points.at(-1).time * 1000)}</text>`;
}

function renderDataManager(payload) {
  state.dataManager = payload;
  const select = $('#data-series');
  const names = payload.series.map((series) => series.name);
  if (!names.includes(state.selectedSeries)) state.selectedSeries = names[0] || '';
  select.innerHTML = names.map((name) => `<option value="${escapeHtml(name)}">${escapeHtml(name)}</option>`).join('') || '<option value="">No numeric streams</option>';
  select.value = state.selectedSeries;
  const series = payload.series.find((item) => item.name === state.selectedSeries);
  const latest = series?.points.at(-1);
  $('#data-count').textContent = payload.count;
  $('#data-latest').textContent = latest?.value ?? '—';
  $('#data-unit').textContent = latest?.unit || 'unitless';
  $('#data-last-time').textContent = latest ? formatTime(latest.time * 1000) : '—';
  $('#data-chart-title').textContent = state.selectedSeries || 'No numeric streams';
  $('#data-updated').textContent = `Updated ${formatTime(payload.collectedAt)}`;
  $('#data-table').innerHTML = payload.entries.slice(0, 100).map((entry) => `<tr>
    <td>${escapeHtml(formatTime(entry.time * 1000))}</td><td><code>${escapeHtml(entry.name)}</code></td>
    <td>${escapeHtml(measurementValue(entry))}</td><td>${escapeHtml(entry.unit || '—')}</td>
  </tr>`).join('') || '<tr><td colspan="4" class="muted">DataManager contains no records.</td></tr>';
  renderDataChart(series);
}

const LIVENESS = { alive: 1, degraded: 0.5, failed: 0 };

function observationSummary(observation) {
  const value = observation?.value;
  if (!value || typeof value !== 'object') return value ?? '—';
  const keys = { echo: ['status', 'detail'], power: ['supplyType', 'state'], status: ['state', 'health'] }[observation.observationType];
  const picked = (keys || Object.keys(value)).map((key) => value[key]).filter((item) => item !== undefined && item !== null);
  return picked.length ? picked.join(' · ') : JSON.stringify(value);
}

function renderSysMonitor(payload) {
  state.sysMonitor = payload;
  state.selectedSmSystem = payload.selectedSystem;
  const select = $('#sm-system');
  select.innerHTML = payload.systems.map((item) => `<option value="${escapeHtml(item.systemName)}">${escapeHtml(item.systemName)}</option>`).join('') || '<option value="">No monitored systems</option>';
  select.value = payload.selectedSystem;
  const selected = payload.systems.find((item) => item.systemName === payload.selectedSystem);
  $('#sm-systems').textContent = payload.systems.length;
  $('#sm-stored').textContent = payload.storedObservations.toLocaleString();
  $('#sm-retention').textContent = payload.retentionDays ? `retention ${payload.retentionDays} days` : 'retention';
  $('#sm-echo').textContent = selected?.latest?.echo?.value?.status || '—';
  $('#sm-last-time').textContent = selected?.lastObservationTime ? `latest ${formatTime(selected.lastObservationTime)}` : 'latest echo';
  $('#sm-updated').textContent = `Updated ${formatTime(payload.collectedAt)}`;
  $('#sm-system-table').innerHTML = payload.systems.map((item) => `<tr data-sm-system="${escapeHtml(item.systemName)}" class="${item.systemName === payload.selectedSystem ? 'selected' : ''}">
    <td><strong>${escapeHtml(item.systemName)}</strong></td>
    <td>${escapeHtml(observationSummary(item.latest?.echo))}</td><td>${escapeHtml(observationSummary(item.latest?.power))}</td>
    <td>${escapeHtml(observationSummary(item.latest?.status))}</td>
    <td>${escapeHtml(item.lastObservationTime ? formatTime(item.lastObservationTime) : '—')}</td><td>${escapeHtml(item.observationCount)}</td>
  </tr>`).join('') || '<tr><td colspan="6" class="muted">SysMonitor has stored no observations yet.</td></tr>';
  $('#sm-chart-title').textContent = payload.selectedSystem ? `Liveness · ${payload.selectedSystem}` : 'Liveness';
  $('#sm-table').innerHTML = payload.observations.slice(0, 100).map((item) => `<tr>
    <td>${escapeHtml(formatTime(item.sourceTime))}</td><td><code>${escapeHtml(item.observationType)}</code></td>
    <td>${escapeHtml(observationSummary(item))}</td><td>${escapeHtml(item.quality || '—')}</td>
  </tr>`).join('') || '<tr><td colspan="4" class="muted">No observations stored for this system.</td></tr>';
  renderSysMonitorChart();
}

function renderSysMonitorChart() {
  const points = (state.sysMonitor?.observations || [])
    .filter((item) => item.observationType === 'echo' && item.value?.status in LIVENESS)
    .map((item) => ({ time: Date.parse(item.sourceTime) / 1000, value: LIVENESS[item.value.status], unit: '1' }))
    .reverse();
  renderDataChart({ name: 'liveness', points }, '#sm-chart');
}

async function loadSysMonitor(silent = false) {
  if (state.loadingSm) return;
  state.loadingSm = true;
  if (!silent) $('#sm-updated').textContent = 'Loading…';
  try {
    const query = new URLSearchParams({ limit: '240', system: state.selectedSmSystem || '' });
    const response = await fetch(`/api/sysmonitor?${query}`, { cache: 'no-store', signal: AbortSignal.timeout(15000) });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `SysMonitor request failed (${response.status})`);
    renderSysMonitor(payload);
  } catch (error) {
    $('#sm-updated').textContent = error.message;
    if (!silent) showToast(error.message);
  } finally {
    state.loadingSm = false;
  }
}

async function loadDataManager(silent = false) {
  if (state.loadingData) return;
  state.loadingData = true;
  if (!silent) $('#data-updated').textContent = 'Loading…';
  try {
    const response = await fetch('/api/datamanager?limit=240', { cache: 'no-store', signal: AbortSignal.timeout(15000) });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `DataManager request failed (${response.status})`);
    renderDataManager(payload);
  } catch (error) {
    $('#data-updated').textContent = error.message;
    if (!silent) showToast(error.message);
  } finally {
    state.loadingData = false;
  }
}

function selectedProducer() {
  return state.orchestration?.catalog?.producers.find((producer) => producer.instanceId === $('#rule-producer').value);
}

function updateRuleConsumers() {
  const producer = selectedProducer();
  const consumerSelect = $('#rule-consumer');
  const previous = consumerSelect.value;
  const consumers = producer?.compatibleConsumers || [];
  const systemsByName = new Map((state.orchestration?.catalog?.consumers || []).map((consumer) => [consumer.name, consumer]));
  consumerSelect.innerHTML = consumers.map((name) => `<option value="${escapeHtml(name)}">${escapeHtml(name)} · ${escapeHtml(systemsByName.get(name)?.version || 'unknown')}</option>`).join('') || '<option value="">No compatible consumers</option>';
  if (consumers.includes(previous)) consumerSelect.value = previous;
  consumerSelect.disabled = !consumers.length;
  $('#create-rule').disabled = !producer || !consumers.length;
  $('#compatible-count').textContent = consumers.length;
  const interfaces = producer?.interfaces || [];
  $('#rule-producer-detail').innerHTML = producer
    ? `<strong>${escapeHtml(producer.serviceDefinition)}</strong> from ${escapeHtml(producer.provider)} · ${interfaces.map((item) => `${escapeHtml(item.templateName)} / ${escapeHtml(item.policy || 'NONE')}`).join(' · ') || 'no interfaces'}`
    : 'No eligible service producers are registered.';
}

function renderOrchestration(payload) {
  state.orchestration = payload;
  const producerSelect = $('#rule-producer');
  const previous = producerSelect.value;
  const producers = payload.catalog?.producers || [];
  producerSelect.innerHTML = producers.map((producer) => `<option value="${escapeHtml(producer.instanceId)}">${escapeHtml(producer.serviceDefinition)} — ${escapeHtml(producer.provider)}</option>`).join('') || '<option value="">No interoperable producers</option>';
  if (producers.some((producer) => producer.instanceId === previous)) producerSelect.value = previous;
  producerSelect.disabled = !producers.length;
  updateRuleConsumers();

  const byInstance = new Map(producers.map((producer) => [producer.instanceId, producer]));
  const consumersByName = new Map((payload.catalog?.consumers || []).map((consumer) => [consumer.name, consumer]));
  const rules = payload.rules || [];
  const activeRules = rules.filter((rule) => byInstance.has(rule.serviceInstanceId) && consumersByName.has(rule.consumer));
  $('#active-rule-count').textContent = `${activeRules.length} active`;
  $('#orchestration-map').innerHTML = rules.map((rule) => {
    const producer = byInstance.get(rule.serviceInstanceId);
    const active = Boolean(producer && consumersByName.has(rule.consumer));
    return `<article class="rule-flow-visual ${active ? 'active' : 'stale'}">
      <div class="rule-node consumer-node"><small>Consumer</small><strong>${escapeHtml(rule.consumer)}</strong></div>
      <div class="rule-route"><span>${escapeHtml(producer?.serviceDefinition || rule.serviceDefinition || 'Unavailable service')}</span><i></i><small>Priority ${escapeHtml(rule.priority)}</small></div>
      <div class="rule-node producer-node"><small>Producer</small><strong>${escapeHtml(producer?.provider || 'Not registered')}</strong></div>
      <span class="rule-live"><i></i>${active ? 'Active' : 'Unavailable'}</span>
    </article>`;
  }).join('') || '<div class="rule-map-empty"><strong>No active flows</strong><span>Add a rule to visualize its consumer-to-producer route.</span></div>';
  $('#orchestration-rule-table').innerHTML = rules.map((rule) => {
    const producer = byInstance.get(rule.serviceInstanceId);
    return `<tr>
      <td><strong>${escapeHtml(rule.priority)}</strong></td>
      <td>${escapeHtml(rule.consumer)}</td>
      <td>${escapeHtml(producer?.serviceDefinition || 'Unavailable service')}<br><code>${escapeHtml(rule.serviceInstanceId)}</code></td>
      <td>${escapeHtml(producer?.provider || 'Not registered')}</td>
      <td>${escapeHtml(formatTime(rule.createdAt))}</td>
      <td><button class="rule-remove" data-remove-rule="${escapeHtml(rule.id)}">Remove</button></td>
    </tr>`;
  }).join('') || '<tr><td colspan="6" class="muted">No orchestration rules have been stored.</td></tr>';
  $('#orchestration-updated').textContent = `Updated ${formatTime(payload.collectedAt)}`;
}

async function loadOrchestration(silent = false) {
  if (state.loadingOrchestration) return;
  state.loadingOrchestration = true;
  if (!silent) $('#orchestration-updated').textContent = 'Loading…';
  try {
    const response = await fetch('/api/orchestration/rules', { cache: 'no-store', signal: AbortSignal.timeout(15000) });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Orchestration request failed (${response.status})`);
    renderOrchestration(payload);
  } catch (error) {
    $('#orchestration-updated').textContent = error.message;
    if (!silent) showToast(error.message);
  } finally {
    state.loadingOrchestration = false;
  }
}

async function createOrchestrationRule(event) {
  event.preventDefault();
  const producer = selectedProducer();
  if (!producer) return;
  const button = $('#create-rule');
  button.disabled = true;
  try {
    const response = await fetch('/api/orchestration/rules', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Arrowhead-Action': 'confirm' },
      body: JSON.stringify({ serviceInstanceId: producer.instanceId, consumer: $('#rule-consumer').value, priority: Number($('#rule-priority').value) })
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Could not create rule (${response.status})`);
    showToast('Orchestration rule added');
    await loadOrchestration(true);
  } catch (error) {
    showToast(error.message);
  } finally {
    updateRuleConsumers();
  }
}

async function removeOrchestrationRule(id) {
  if (!confirm('Remove this orchestration rule?')) return;
  try {
    const response = await fetch(`/api/orchestration/rules/${encodeURIComponent(id)}`, {
      method: 'DELETE', headers: { 'X-Arrowhead-Action': 'confirm' }
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Could not remove rule (${response.status})`);
    showToast('Orchestration rule removed');
    await loadOrchestration(true);
  } catch (error) { showToast(error.message); }
}

function systemRow(system) {
  const running = system.runtime?.state === 'running';
  return `<div class="system-row">
    <div class="system-identity"><span class="system-glyph">${shortName(system.name)}</span><span class="system-name">${escapeHtml(system.name)}</span></div>
    <span class="state ${running ? 'running' : ''}">${running ? 'Running' : system.runtime ? escapeHtml(system.runtime.state) : 'Registered'}</span>
    <span class="muted">${system.serviceCount} services</span>
    <span class="muted">v${escapeHtml(system.version || '—')}</span>
  </div>`;
}

function renderSystems(data) {
  $('#overview-systems').innerHTML = data.systems.slice(0, 8).map(systemRow).join('') || '<p class="muted">No registered systems.</p>';
  $('#systems-grid').innerHTML = data.systems.map((system) => {
    const running = system.runtime?.state === 'running';
    const manageable = system.runtime?.managed;
    return `<article class="system-card">
      <div class="system-card-head"><div><p class="eyebrow">${system.composeService ? `Runtime · ${escapeHtml(system.composeService)}` : 'Registry only'}</p><h3>${escapeHtml(system.name)}</h3></div><span class="state ${running ? 'running' : ''}">${running ? 'Running' : system.runtime ? escapeHtml(system.runtime.state) : 'Registered'}</span></div>
      <dl><dt>Version</dt><dd>${escapeHtml(system.version || '—')}</dd><dt>Services</dt><dd>${system.serviceCount}</dd><dt>Address</dt><dd>${escapeHtml(system.addresses?.[0]?.address || '—')}</dd><dt>Health</dt><dd>${escapeHtml(system.runtime?.health || (running ? 'active' : '—'))}</dd></dl>
      <div class="card-actions">
        <button class="action" data-log="${escapeHtml(system.composeService || '')}" ${system.composeService ? '' : 'disabled'}>Logs</button>
        <button class="action" data-action="restart" data-service="${escapeHtml(system.composeService || '')}" ${manageable ? '' : 'disabled'}>Restart</button>
        <button class="action danger" data-action="${running ? 'stop' : 'start'}" data-service="${escapeHtml(system.composeService || '')}" ${manageable ? '' : 'disabled'}>${running ? 'Stop' : 'Start'}</button>
      </div>
    </article>`;
  }).join('') || '<p class="muted">No systems reported by ServiceRegistry.</p>';
}

function renderServices(services) {
  const query = state.filter.toLowerCase();
  const filtered = services.filter((service) => `${service.serviceDefinition?.name} ${service.provider?.name}`.toLowerCase().includes(query));
  $('#service-table').innerHTML = filtered.map((service) => `<tr>
    <td><strong>${escapeHtml(service.serviceDefinition?.name)}</strong><br><code>${escapeHtml(service.instanceId)}</code></td>
    <td>${escapeHtml(service.provider?.name)}</td>
    <td>${escapeHtml(service.version)}</td>
    <td>${escapeHtml(service.interfaces?.[0]?.protocol || '—')} · ${escapeHtml(service.interfaces?.[0]?.policy || '—')}</td>
    <td><code>${escapeHtml(serviceEndpoint(service))}</code></td>
  </tr>`).join('') || '<tr><td colspan="5" class="muted">No matching service instances.</td></tr>';
}

function renderRuntime(containers) {
  $('#container-list').innerHTML = containers.map((container) => `<div class="container-item"><strong>${escapeHtml(container.displayName || container.service)}</strong><span class="state ${container.state === 'running' ? 'running' : ''}">${escapeHtml(container.state)}</span><span class="container-image"><code>${escapeHtml(container.service)}</code> · ${escapeHtml(container.image)}</span><span class="muted">${escapeHtml(container.health || '')}</span></div>`).join('');
  const select = $('#log-service');
  const previous = select.value;
  select.innerHTML = containers.map((container) => `<option value="${escapeHtml(container.service)}">${escapeHtml(container.displayName || container.service)} · ${escapeHtml(container.service)}</option>`).join('');
  if (containers.some((container) => container.service === previous)) select.value = previous;
}

function render(data) {
  state.data = data;
  renderMetrics(data);
  renderTopology(data.systems);
  renderMessaging(data);
  renderActivity(data.history);
  renderSystems(data);
  renderServices(data.services);
  renderRegistry(data.registryManagement);
  renderRuntime(data.containers);
  const errors = Object.values(data.errors || {}).filter(Boolean);
  if (data.summary.unregisteredServices?.length) {
    errors.push(`Running but not registered: ${data.summary.unregisteredServices.join(', ')}`);
  }
  const banner = $('#error-banner');
  banner.hidden = !errors.length;
  banner.textContent = errors.join(' · ');
}

async function refresh(silent = false) {
  if (state.refreshing) return;
  state.refreshing = true;
  const button = $('#refresh-button');
  button.disabled = true;
  try {
    const response = await fetch('/api/overview', { cache: 'no-store', signal: AbortSignal.timeout(15000) });
    if (!response.ok) throw new Error(`Telemetry request failed (${response.status})`);
    render(await response.json());
    if (state.activeView === 'data') loadDataManager(true);
    if (state.activeView === 'sysmonitor') loadSysMonitor(true);
    if (state.activeView === 'orchestration') loadOrchestration(true);
    if (!silent) showToast('Telemetry refreshed');
  } catch (error) {
    $('#connection').classList.remove('online');
    $('#connection').lastElementChild.textContent = 'Disconnected';
    const banner = $('#error-banner');
    banner.hidden = false;
    banner.textContent = error.message;
  } finally {
    button.disabled = false;
    state.refreshing = false;
  }
}

async function loadLogs(service) {
  if (!service) return;
  $('#log-title').textContent = service;
  $('#logs').textContent = 'Loading logs…';
  try {
    const response = await fetch(`/api/logs?service=${encodeURIComponent(service)}&tail=180`, { cache: 'no-store' });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || 'Could not load logs');
    $('#logs').textContent = payload.lines.join('\n') || 'No log output.';
    $('#logs').scrollTop = $('#logs').scrollHeight;
  } catch (error) {
    $('#logs').textContent = error.message;
  }
}

async function lifecycle(service, action) {
  if (!confirm(`${action[0].toUpperCase() + action.slice(1)} ${service}?`)) return;
  try {
    const response = await fetch(`/api/containers/${encodeURIComponent(service)}/${action}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Arrowhead-Action': 'confirm' },
      body: '{}'
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Action failed (${response.status})`);
    showToast(`${service}: ${action} accepted`);
    setTimeout(() => refresh(true), 1800);
  } catch (error) { showToast(error.message); }
}

document.addEventListener('click', (event) => {
  const nav = event.target.closest('[data-view]');
  if (nav) setView(nav.dataset.view);
  const go = event.target.closest('[data-go]');
  if (go) setView(go.dataset.go);
  const node = event.target.closest('[data-system]');
  if (node) { setView('systems'); document.querySelector(`[data-log]`)?.focus(); }
  const action = event.target.closest('[data-action]');
  if (action) lifecycle(action.dataset.service, action.dataset.action);
  const log = event.target.closest('[data-log]');
  if (log?.dataset.log) {
    setView('runtime');
    $('#log-service').value = log.dataset.log;
    loadLogs(log.dataset.log);
  }
  const removeRule = event.target.closest('[data-remove-rule]');
  if (removeRule) removeOrchestrationRule(removeRule.dataset.removeRule);
});

$('#refresh-button').addEventListener('click', () => refresh());
$('#service-filter').addEventListener('input', (event) => { state.filter = event.target.value; if (state.data) renderServices(state.data.services); });
$('#registry-filter').addEventListener('input', (event) => { state.registryFilter = event.target.value; if (state.data) renderRegistry(state.data.registryManagement); });
$('#log-service').addEventListener('change', (event) => loadLogs(event.target.value));
$('#reload-logs').addEventListener('click', () => loadLogs($('#log-service').value));
$('#reload-data').addEventListener('click', () => loadDataManager());
$('#reload-sm').addEventListener('click', () => loadSysMonitor());
$('#sm-system').addEventListener('change', (event) => { state.selectedSmSystem = event.target.value; loadSysMonitor(); });
$('#sm-system-table').addEventListener('click', (event) => {
  const row = event.target.closest('[data-sm-system]');
  if (row) { state.selectedSmSystem = row.dataset.smSystem; loadSysMonitor(); }
});
$('#reload-rules').addEventListener('click', () => loadOrchestration());
$('#rule-producer').addEventListener('change', updateRuleConsumers);
$('#orchestration-form').addEventListener('submit', createOrchestrationRule);
$('#data-series').addEventListener('change', (event) => { state.selectedSeries = event.target.value; if (state.dataManager) renderDataManager(state.dataManager); });
new ResizeObserver(() => {
  if (state.data) renderActivity(state.data.history);
  if (state.dataManager) renderDataChart(state.dataManager.series.find((item) => item.name === state.selectedSeries));
  if (state.sysMonitor) renderSysMonitorChart();
}).observe(document.querySelector('main'));

refresh(true);
state.refreshTimer = setInterval(() => refresh(true), 5000);
