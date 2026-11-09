/* Chiplet control-plane roles (Paper 1). Role color and observed availability are independent. */
(function(root,factory){const model=factory();if(typeof module==='object'&&module.exports)module.exports=model;else root.ChipletControlPlaneModel=model;})(typeof globalThis==='object'?globalThis:this,function(){
 'use strict';
 const normalize=value=>String(value||'').split('.')[0].toLowerCase().replace(/[^a-z0-9]/g,'');
 const roles=[
  {id:'registry',name:'ResourceRegistry',aliases:['ResourceRegistry'],service:'resource-registry',category:'registry',group:'flow',step:'1 · Discover',description:'Resource identities, electrical addresses, capabilities and microsystem state.'},
  {id:'orchestration',name:'ResourceOrchestration',aliases:['ResourceOrchestration','ResourceOrchestrator'],service:'resource-orchestration',category:'orchestration',group:'flow',step:'2 · Compose',description:'Select resources and coordinate microsystem integration.'},
  {id:'scheduler',name:'ResourceScheduler',aliases:['ResourceScheduler'],service:'resource-scheduler',category:'orchestration',group:'flow',step:'3 · Schedule',description:'Reserve resources for the requested lifetime and priorities.'},
  {id:'network',name:'NetworkManager',aliases:['NetworkManager'],service:'network-manager',category:'orchestration',group:'flow',step:'4 · Connect',description:'Configure communication and expose the instantiated microsystem.'},
  {id:'monitoring',name:'ResourceMonitoring',aliases:['ResourceMonitoring','ResourceMonitor'],service:'resource-monitoring',category:'core',group:'assurance',description:'Observe resource condition, capability and usage.'},
  {id:'security',name:'ResourceSecurity',aliases:['ResourceSecurity'],service:'resource-security',category:'security',group:'assurance',description:'Protect resource access, onboarding and electrical communication.'},
  {id:'debugger',name:'Debugger',aliases:['Debugger','ChipletDebugger'],service:'debugger',category:'core',group:'assurance',description:'Support testing and validation of chiplets and microsystems.'}
 ];
 const named=value=>typeof value==='string'?value:value?.name||value?.displayName||value?.service||value?.composeService;
 function roleFor(value){const key=normalize(named(value));return roles.find(role=>[role.name,role.service,...role.aliases].some(name=>normalize(name)===key));}
 function category(value){return roleFor(value)?.category||null;}
 function inventory(data){
  return roles.map(role=>{
   const system=(data.systems||[]).find(s=>roleFor(s)?.id===role.id);
   const container=(data.containers||[]).find(c=>roleFor(c)?.id===role.id||c.service===role.service);
   const runtime=system?.runtime||container;
   const services=(data.services||[]).filter(s=>roleFor(s.provider?.name||s.instanceId?.split('|')[0])?.id===role.id);
   return{...role,provider:system?.name||role.name,registered:system?true:data.cloud?.registryReachable===true?false:null,runtime:runtime?{state:runtime.state||'unknown',health:runtime.health||'unknown'}:null,services};
  });
 }
 return{roles,roleFor,category,inventory};
});
