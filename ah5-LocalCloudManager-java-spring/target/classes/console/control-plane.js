/* Read-only live control-plane view, enabled only in the chiplet manager profile. */
(function(){
 'use strict';const M=window.ChipletControlPlaneModel;if(!M)return;let mounted=false;
 const text=value=>escapeHtml(value);
 function card(role){
  const registration=role.registered===true?'Registered':role.registered===false?'Not registered':'Registry unavailable';
  const runtime=role.runtime?`${role.runtime.state} · ${role.runtime.health}`:'Runtime not observed';
  const services=[...new Set(role.services.map(s=>s.serviceDefinition?.name||s.instanceId?.split('|')[1]).filter(Boolean))];
  return `<article class="cp-node ah-${role.category}" data-cp-role="${role.id}"><p class="cp-role">${role.step||'Core · assurance'}</p><h3>${text(role.name)}</h3><p class="cp-description">${text(role.description)}</p><div class="cp-states"><span class="cp-state" data-cp-registration="${role.registered}">${registration}</span><span class="cp-state">${text(runtime)}</span></div><div class="cp-services">${services.map(s=>`<span class="cp-service">${text(s)}</span>`).join('')||'<span class="cp-no-services">No service instances observed</span>'}</div><button type="button" class="action cp-open-services" data-cp-provider="${text(role.provider)}" ${!role.services.length?'disabled':''}>View services (${role.services.length})</button></article>`;
 }
 function render(data){
  if(!mounted)return;const nodes=M.inventory(data);
  document.querySelector('#cp-flow').innerHTML=nodes.filter(n=>n.group==='flow').map(card).join('');
  document.querySelector('#cp-assurance').innerHTML=nodes.filter(n=>n.group==='assurance').map(card).join('');
  const count=nodes.filter(n=>n.registered===true).length;
  document.querySelector('#cp-observation').textContent=`${count} / ${nodes.length} roles registered · Last sample ${formatTime(data.collectedAt)}`;
  const notice=document.querySelector('#cp-notice');notice.hidden=!(data.errors?.arrowhead||data.errors?.docker);notice.textContent=[data.errors?.arrowhead?'Registry observation unavailable.':'',data.errors?.docker?'Runtime observation unavailable.':''].filter(Boolean).join(' ');
 }
 function start(){
  if(document.body.dataset.chipletManager!=='true')return;
  const host=document.querySelector('#chiplet-control-plane');if(!host)return;mounted=true;document.querySelector('[data-view="controlplane"]').hidden=false;
  host.innerHTML=`<div class="section-heading"><div><p class="eyebrow">Chiplet architecture</p><h2>Chiplet control plane</h2></div><span class="updated" id="cp-observation">Waiting for cloud inventory…</span></div><p class="section-note cp-intro">Core systems discover, compose, schedule and connect chiplet resources. Colors show architectural roles; registration and runtime labels show the latest observation.</p><div class="cp-key" aria-label="Chiplet core color key"><span class="ah-category-badge ah-registry">Blue · registry</span><span class="ah-category-badge ah-orchestration">Green · orchestration, scheduling and network control</span><span class="ah-category-badge ah-security">Red · security</span><span class="ah-category-badge ah-core">Blue · monitoring and debugging</span></div><p id="cp-notice" class="error-banner" role="status" hidden></p><section class="panel cp-section" aria-label="Microsystem instantiation"><div class="panel-heading"><h3>Microsystem instantiation</h3><span class="section-note">Architectural sequence · arrows do not indicate measured traffic</span></div><div class="cp-flow" id="cp-flow"></div></section><section class="panel cp-section" aria-label="Control-plane assurance"><div class="panel-heading"><h3>Monitoring, security and validation</h3><span class="section-note">Paper 1 roles · availability shown independently</span></div><div class="cp-assurance" id="cp-assurance"></div></section><section class="panel cp-resource-plane" aria-label="Managed chiplet resources"><div><p class="eyebrow">Resource plane</p><h3>Composed chiplet resources</h3><p class="section-note">Pink resource blocks connect through teal electrical interfaces in the floorplan workspace.</p></div><button type="button" class="button secondary" id="cp-open-floorplans">Open floorplans</button></section>`;
  host.addEventListener('click',event=>{const button=event.target.closest('[data-cp-provider]');if(button){state.filter=button.dataset.cpProvider;document.querySelector('#service-filter').value=state.filter;if(state.data)renderServices(state.data.services);setView('services');}if(event.target.closest('#cp-open-floorplans'))setView('floorplans');});
  if(state.data)render(state.data);
 }
 window.ChipletControlPlane={render};
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start);else start();
})();
