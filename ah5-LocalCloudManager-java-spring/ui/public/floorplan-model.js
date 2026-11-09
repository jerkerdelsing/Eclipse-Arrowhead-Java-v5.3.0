/* Geometry helpers shared by the graphical editor and its tests. Coordinates and occupied dimensions are mm. */
(function(root,factory){const model=factory();if(typeof module==='object'&&module.exports)module.exports=model;else root.ChipletFloorplanModel=model;})(typeof globalThis==='object'?globalThis:this,function(){
 'use strict';
 const clone=value=>JSON.parse(JSON.stringify(value));

 // Paper 6 Table 7: architecture/profile is distinct from the L1/L2 carrier.
 const networkModels=[
  {id:'UCIe',label:'UCIe',carrier:'UCIe',note:'Die-to-die link technology. Routing, location-independent identity and atomic reconfiguration require higher layers.'},
  {id:'AIB',label:'AIB',carrier:'AIB',note:'Parallel die-to-die interface. Fabric routing and runtime reorganisation require higher layers.'},
  {id:'BoW',label:'BoW',carrier:'BoW',note:'Bunch of Wires die-to-die interface. Fabric routing and runtime reorganisation require higher layers.'},
  {id:'SharedBus',label:'Shared bus',note:'Shared address-mapped bus architecture. Does not itself provide location-independent identity, transition deadlock freedom or versioned atomic configuration.'},
  {id:'StaticNoC',label:'Static NoC',note:'Packet network with routing fixed at design/boot time. Runtime reorganisation requires additional mechanisms.'},
  {id:'Proposed',label:'Proposed reorganisable fabric',note:'Paper 6 layered fabric above a chosen D2D interface. Its proposed capabilities require implementation and verification; selecting it does not establish hardware support.'}
 ];
 const carriers=['UCIe','AIB','BoW','PCIe'];
 function networkChoice(plan){const n=plan.network;return n&&Object.hasOwn(n,'model')?n.model:'Proposed';}
 function selectNetwork(plan,model,fabric){
  const profile=networkModels.find(m=>m.id===model);if(!profile)throw Error('Choose a network model from Paper 6 Table 7.');
  const next=clone(plan),n=next.network||{routers:[],d2dPorts:[],links:[],endpoints:[],bindings:[],flows:[],firewall:[]};
  const old=n.fabric;n.model=model;n.fabric=profile.carrier||fabric||old||'UCIe';
  if(!carriers.includes(n.fabric))throw Error('Choose a supported electrical interface.');
  // The engineer explicitly changes the draft carrier; retain routes/endpoints/contracts.
  for(const port of n.d2dPorts||[])port.standard=n.fabric;
  for(const link of next.links||[])if(!link.type||link.type===old||carriers.includes(link.type))link.type=n.fabric;
  delete n.admittedAt;next.network=n;return next;
 }
 function validateNetworkChoice(plan){
  const n=plan.network;if(n==null)return[];if(typeof n!=='object'||Array.isArray(n))return['Network must be an object.'];
  const profile=networkModels.find(m=>m.id===networkChoice(plan)),errors=[];
  if(!profile)errors.push('Choose a network model from Paper 6 Table 7.');
  if(!carriers.includes(n.fabric))errors.push('Choose UCIe, AIB, BoW or PCIe as the electrical interface.');
  if(profile?.carrier&&profile.carrier!==n.fabric)errors.push(`${profile.label} requires the matching ${profile.carrier} electrical interface.`);
  if(n.d2dPorts!=null&&!Array.isArray(n.d2dPorts))return errors.concat('D2D ports must be an array.');
  for(const port of n.d2dPorts||[])if(!port||port.standard!==n.fabric)errors.push('All D2D ports must match the chosen electrical interface.');
  return errors;
 }
 const id=/^[A-Za-z0-9][A-Za-z0-9_.-]{0,99}$/;
 function validate(plan){
  if(!plan||typeof plan!=="object"||Array.isArray(plan))return["A floorplan must be a JSON object."];
  const errors=[],w=Number(plan?.interposer?.width),h=Number(plan?.interposer?.height),chips=plan?.chiplets;
  if(typeof plan?.name!=='string'||!plan.name.trim()||plan.name.length>200)errors.push('Give the floorplan a name.');
  if(!Number.isFinite(w)||!Number.isFinite(h)||w<=0||h<=0||w>1000||h>1000)errors.push('Interposer dimensions must be in (0, 1000] mm.');
  if(!Array.isArray(chips)||chips.length<1||chips.length>64){errors.push('Add between 1 and 64 chiplets.');return errors;}
  const seen=new Set();
  chips.forEach((c,i)=>{
   if(!c||typeof c!=='object'||Array.isArray(c)){errors.push(`Chiplet ${i+1}: an object is required.`);return;}
   const label=c.chipletId||`Chiplet ${i+1}`;
   if(c.networkProperties!==undefined&&(!Array.isArray(c.networkProperties)||c.networkProperties.some(p=>typeof p!=='string'||!/^IP([1-9]|1[0-9]|2[0-2])$/.test(p))||new Set(c.networkProperties).size!==c.networkProperties.length))errors.push('Choose unique network properties IP1–IP22.');
   if(c.ipBlocks!==undefined){if(!Array.isArray(c.ipBlocks)||c.ipBlocks.length>64)errors.push('IP blocks must be an array with at most 64 entries.');else{const ids=new Set();for(const b of c.ipBlocks){if(!b||typeof b!=='object'||!id.test(b.instanceId)||ids.has(b.instanceId)||typeof b.definitionId!=='string'||typeof b.libraryVersion!=='string'||!Array.isArray(b.interfaces)||b.interfaces.some(p=>!p||typeof p.port!=='string'||typeof p.contractId!=='string'||(p.enabled!==undefined&&typeof p.enabled!=='boolean')||(p.values!==undefined&&(!p.values||typeof p.values!=='object'||Array.isArray(p.values)))))errors.push('Invalid IP block or typed interface selection.');else ids.add(b.instanceId);}}}

   if(typeof c.chipletId!=='string'||!id.test(c.chipletId)||seen.has(c.chipletId))errors.push(`${label}: use a unique chiplet ID (letters, numbers, dot, dash or underscore).`);seen.add(c.chipletId);
   if(typeof c.type!=='string'||!c.type.trim())errors.push(`${label}: a type is required.`);
   if(![c.x,c.y,c.width,c.height].every(Number.isFinite)||c.width<=0||c.height<=0||c.x<0||c.y<0||c.x+c.width>w+1e-9||c.y+c.height>h+1e-9)errors.push(`${label}: the chiplet must fit inside the interposer.`);
   if(c.rotation!==undefined&&![0,90].includes(c.rotation))errors.push(`${label}: rotation must be 0 or 90 degrees.`);
   for(let j=0;j<i;j++){const other=chips[j];if(!other||typeof other!=="object")continue;if(c.x<other.x+other.width-1e-9&&other.x<c.x+c.width-1e-9&&c.y<other.y+other.height-1e-9&&other.y<c.y+c.height-1e-9)errors.push(`${label} overlaps ${other.chipletId}.`);}
  });
  if(plan.links!=null&&!Array.isArray(plan.links)){errors.push('Connections must be an array.');return errors;}
  (plan.links||[]).forEach(l=>{if(!l||typeof l!=='object'){errors.push('Each connection must be an object.');return;}if(!seen.has(l.from)||!seen.has(l.to)||l.from===l.to)errors.push('Each connection must join two existing, different chiplets.');if(l.bandwidthGbps!==undefined&&(!Number.isFinite(l.bandwidthGbps)||l.bandwidthGbps<0))errors.push('Connection bandwidth must be nonnegative.');});
  return errors.concat(validateNetworkChoice(plan));
 }
 function arrange(plan){
  const next=clone(plan),w=Number(next.interposer.width),h=Number(next.interposer.height),spacing=Number(next.spacing??.5);
  if(!Number.isFinite(spacing)||spacing<0||!Number.isFinite(w)||!Number.isFinite(h))throw Error('Invalid interposer or spacing.');
  const chips=[...next.chiplets].sort((a,b)=>Math.max(b.width,b.height)-Math.max(a.width,a.height));let x=spacing,y=spacing,shelf=0;
  for(const c of chips){
   const options=[{w:c.width,h:c.height,rotation:c.rotation||0},{w:c.height,h:c.width,rotation:c.rotation===90?0:90}];let chosen;
   for(const fresh of [false,true]){const px=fresh?spacing:x,py=fresh?y+shelf+spacing:y;const fits=options.filter(o=>px+o.w+spacing<=w+1e-9&&py+o.h+spacing<=h+1e-9).sort((a,b)=>a.h-b.h);if(fits.length){chosen={...fits[0],x:px,y:py,fresh};break;}}
   if(!chosen)throw Error('These chiplets do not fit. Enlarge the interposer or reduce chiplet sizes.');
   if(chosen.fresh)shelf=0;Object.assign(c,{x:chosen.x,y:chosen.y,width:chosen.w,height:chosen.h,rotation:chosen.rotation});x=chosen.x+chosen.w+spacing;y=chosen.y;shelf=Math.max(shelf,chosen.h);
  }
  const errors=validate(next);if(errors.length)throw Error(errors[0]);return next;
 }
 function endpoint(a,b){const dx=b.x+b.width/2-a.x-a.width/2,dy=b.y+b.height/2-a.y-a.height/2;const scale=Math.min(dx?Math.abs(a.width/2/dx):Infinity,dy?Math.abs(a.height/2/dy):Infinity);return{x:a.x+a.width/2+dx*scale,y:a.y+a.height/2+dy*scale};}
 function createIPBlock(library,id,existing=[]){const d=library.ipBlocks.find(b=>b.id===id);if(!d)throw Error('Unknown IP block');if(existing.length>=64)throw Error('At most 64 IP blocks per chiplet.');let n=1;while(existing.some(b=>b.instanceId===id+'-'+n))n++;return{instanceId:id+'-'+n,definitionId:id,libraryVersion:library.version,parameters:{},interfaces:d.ports.map(p=>({port:p.id,contractId:p.contractId,enabled:true,values:{}}))};}
 return{createIPBlock,clone,validate,arrange,endpoint,networkModels,carriers,networkChoice,selectNetwork,validateNetworkChoice};
});
