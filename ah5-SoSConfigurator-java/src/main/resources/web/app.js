"use strict";
const $ = (id) => document.getElementById(id);
let token = "";
try { token = sessionStorage.getItem("sosToken") || ""; } catch (e) {}
let catalog = null, status = {}, members = new Set(), exchanges = [], polling = null;

function el(tag, props, ...kids) {
  const e = document.createElement(tag);
  Object.assign(e, props || {});
  for (const k of kids) if (k !== null && k !== undefined) e.append(k);
  return e;
}

async function call(method, path, body) {
  const r = await fetch(path, { method, headers: { "Authorization": "Bearer " + token, "Content-Type": "application/json" },
    body: body ? JSON.stringify(body) : undefined });
  const data = await r.json().catch(() => ({}));
  if (!r.ok && r.status !== 422) throw new Error(data.error || r.statusText);
  return data;
}

function showError(e) { $("err").textContent = e ? String(e.message || e) : ""; }
const cloudOf = (name) => catalog.clouds.find((c) => c.name === name);

async function connect() {
  showError();
  token = $("token").value || token;
  try {
    catalog = await call("GET", "/api/catalog");
    try { sessionStorage.setItem("sosToken", token); } catch (e) {}
    $("org").textContent = "Organization " + catalog.organization + " — relay " + catalog.relay.name + " — colony " + catalog.colony;
    $("login").hidden = true; $("app").hidden = false;
    renderClouds(); renderExchanges();
    await refresh();
  } catch (e) { showError(e); }
}

async function refresh() {
  showError();
  $("relay").textContent = "Asking the executors…";
  try { status = await call("GET", "/api/status"); } catch (e) { showError(e); status = {}; }
  renderClouds();
}

function renderClouds() {
  const tb = $("clouds"); tb.replaceChildren();
  for (const c of catalog.clouds) {
    const st = (status.clouds || {})[c.name] || {};
    const box = el("input", { type: "checkbox", checked: members.has(c.name) });
    box.onchange = () => { box.checked ? members.add(c.name) : members.delete(c.name); renderExchanges(); };
    const detail = [];
    if (st.neighbours) detail.push("neighbours: " + (st.neighbours.join(", ") || "none"));
    for (const p of st.pkiProblems || []) detail.push(p);
    if (st.error) detail.push(st.error);
    tb.append(el("tr", {},
      el("td", {}, box), el("td", {}, c.name), el("td", {}, c.organization), el("td", {}, c.implementation),
      el("td", {}, c.intercloud.supported ? "supported" : el("span", { className: "bad", title: c.intercloud.reason }, "not supported")),
      el("td", {}, el("span", { className: st.state || "" }, st.state || "?"), detail.length ? el("pre", {}, detail.join("\n")) : null)));
  }
  const r = status.relay || {};
  $("relay").textContent = "Relay: " + (r.state || "?") + ((r.problems || []).length ? " — " + r.problems.join("; ") : "") + (r.error ? " — " + r.error : "");
}

function select(options, value, onchange) {
  const s = el("select", {});
  for (const o of options) s.append(el("option", { value: o.value ?? o, textContent: o.label ?? o, selected: (o.value ?? o) === value }));
  s.onchange = () => onchange(s.value);
  return s;
}

function renderExchanges() {
  const tb = $("exchanges"); tb.replaceChildren();
  const m = [...members];
  exchanges.forEach((x, i) => {
    const cc = cloudOf(x.consumerCloud), pc = cloudOf(x.providerCloud);
    const exp = pc ? pc.exports.find((e) => e.provider === x.provider && e.service === x.service) : null;
    const ops = el("span", {});
    for (const op of exp ? exp.operations : []) {
      const b = el("input", { type: "checkbox", checked: x.operations.includes(op) });
      b.onchange = () => { x.operations = b.checked ? [...x.operations, op] : x.operations.filter((o) => o !== op); };
      ops.append(el("label", {}, b, " " + op + " "));
    }
    const del = el("button", { textContent: "Remove" }); del.onclick = () => { exchanges.splice(i, 1); renderExchanges(); };
    tb.append(el("tr", {},
      el("td", {}, select(["", ...m], x.consumerCloud, (v) => { x.consumerCloud = v; x.consumer = ""; renderExchanges(); })),
      el("td", {}, select(["", ...(cc ? cc.consumers : [])], x.consumer, (v) => { x.consumer = v; })),
      el("td", {}, select(["", ...m.filter((n) => n !== x.consumerCloud)], x.providerCloud, (v) => { x.providerCloud = v; x.provider = ""; x.service = ""; renderExchanges(); })),
      el("td", {}, select([{ value: "", label: "" }, ...(pc ? pc.exports.map((e) => ({ value: e.provider + "." + e.service, label: e.provider + "." + e.service })) : [])],
        x.provider ? x.provider + "." + x.service : "", (v) => { [x.provider, x.service] = v.split("."); x.operations = []; renderExchanges(); })),
      el("td", {}, ops), el("td", {}, del)));
  });
}

function request() {
  return { name: $("name").value.trim(), clouds: [...members], verifyDiscovery: $("discovery").checked,
    exchanges: exchanges.map((x) => ({ ...x })) };
}

function renderPlan(plan) {
  const box = $("plan"); box.replaceChildren();
  if (!plan.allowed) {
    box.append(el("div", { className: "bad" }, "Refused by the security and authorization policies:"),
      el("ul", {}, ...plan.violations.map((v) => el("li", {}, v.rule + ": " + v.message))));
    return;
  }
  for (const [name, c] of Object.entries(plan.evaluation.clouds)) {
    box.append(el("h3", {}, name),
      el("div", { className: "small" }, "Neighbours: " + (c.neighbours.map((n) => n.name + "|" + n.operator).join(", ") || "none") +
        (c.removeNeighbours.length ? " — withdrawn: " + c.removeNeighbours.map((n) => n.name).join(", ") : "")),
      el("div", { className: "small" }, "Grants: " + (c.grants.map((g) => g.cloud + " " + g.consumers.join("/") + " → " + g.provider + "." + g.service).join("; ") || "none")),
      c.revokes.length ? el("div", { className: "small muted" }, "Revoked: " + c.revokes.join(", ")) : null);
  }
  box.append(el("h3", {}, "Steps"), el("ol", { className: "small" }, ...plan.steps.map((s) => el("li", {}, s.kind + " — " + s.target + " (" + s.role + ")"))));
  if (plan.notes.length) box.append(el("pre", {}, plan.notes.join("\n")));
}

async function preview() {
  showError();
  try { renderPlan(await call("POST", "/api/plan", request())); } catch (e) { showError(e); }
}

async function apply() {
  showError();
  try {
    const res = await call("POST", "/api/deployments", request());
    renderPlan(res.plan);
    if (!res.id) { $("deployment").textContent = res.error || "Nothing to do."; return; }
    if (polling) clearInterval(polling);
    const poll = async () => {
      try {
        const d = await call("GET", "/api/deployments/" + res.id);
        renderDeployment(d);
        if (d.state === "success" || d.state === "failed") { clearInterval(polling); polling = null; refresh(); }
      } catch (e) { showError(e); }
    };
    polling = setInterval(poll, 1500); poll();
  } catch (e) { showError(e); }
}

function renderDeployment(d) {
  const box = $("deployment"); box.replaceChildren();
  box.append(el("div", {}, "Workflow ", el("code", {}, d.id.slice(0, 12)), " — ", el("span", { className: d.state }, d.state)));
  const tb = el("tbody", {});
  for (const s of d.steps) {
    tb.append(el("tr", {}, el("td", {}, s.node), el("td", {}, el("span", { className: s.state }, s.state)),
      el("td", {}, (s.errors || []).length ? el("pre", { className: "err" }, s.errors.join("\n")) : s.output ? el("pre", {}, s.output) : "")));
  }
  box.append(el("table", {}, el("thead", {}, el("tr", {}, el("th", {}, "Step"), el("th", {}, "State"), el("th", {}, "Output"))), tb));
}

$("connect").onclick = connect;
$("token").onkeydown = (e) => { if (e.key === "Enter") connect(); };
$("refresh").onclick = refresh;
$("add").onclick = () => { exchanges.push({ consumerCloud: "", consumer: "", providerCloud: "", provider: "", service: "", operations: [] }); renderExchanges(); };
$("preview").onclick = preview;
$("apply").onclick = apply;
if (token) connect();
