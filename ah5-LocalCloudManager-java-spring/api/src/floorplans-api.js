'use strict';
// Optional authenticated BFF for ChipletFloorplanGenerator; never exposes delivery/hardware operations.
const fields=['name','description','interposer','spacing','chiplets','links','network','generator','active'];
function failure(status,message){return Object.assign(new Error(message),{status});}
function payload(body){
 const plan=body?.floorplan;if(!plan||typeof plan!=='object'||Array.isArray(plan))throw failure(400,'floorplan object required');
 const out=Object.fromEntries(fields.filter(k=>Object.hasOwn(plan,k)).map(k=>[k,plan[k]]));
 if(typeof out.name!=='string'||!out.name.trim()||out.name.length>200)throw failure(400,'A name of 1-200 characters is required');
 if(!Array.isArray(out.chiplets)||out.chiplets.length<1||out.chiplets.length>64)throw failure(400,'A floorplan must contain 1-64 chiplets');
 if(out.links!==undefined&&(!Array.isArray(out.links)||out.links.length>1024))throw failure(400,'Invalid links');
 return out;
}
function createFloorplanRoutes({enabled,call,readJson,jsonResponse}){
 return async function(req,res,url){
  if(!url.pathname.startsWith('/api/chiplet/floorplans'))return false;
  if(!enabled){jsonResponse(res,404,{error:'Chiplet floorplan editing is not enabled'});return true;}
  if(url.pathname==='/api/chiplet/floorplans/library' && req.method==='GET'){jsonResponse(res,200,await call('floorplanDiscovery','query-floorplans',{query:'library=true'}));return true;}
  const match=url.pathname.match(/^\/api\/chiplet\/floorplans(?:\/(\d+))?(?:\/(check-network))?$/);
  if(!match){jsonResponse(res,404,{error:'Unknown floorplan operation'});return true;}
  const id=match[1],check=match[2];
  if(req.method==='GET'&&!check){
   if(id){jsonResponse(res,200,await call('floorplanDiscovery','get-floorplan',{id}));return true;}
   const page=Number(url.searchParams.get('page')||0),size=Number(url.searchParams.get('size')||200);
   if(!Number.isSafeInteger(page)||page<0||page>1000000||!Number.isInteger(size)||size<1||size>200)throw failure(400,'Invalid page/size');
   jsonResponse(res,200,await call('floorplanDiscovery','query-floorplans',{query:`page=${page}&size=${size}`}));return true;
  }
  if(!['POST','PUT','DELETE'].includes(req.method)){jsonResponse(res,405,{error:'Method not allowed'});return true;}
  if(req.headers['x-arrowhead-action']!=='confirm')throw failure(428,'Missing action confirmation header');
  if(check&&req.method==='POST'&&!id){const body=await readJson(req);const plan=payload(body);if(!plan.network)throw failure(400,'No network configuration to validate');jsonResponse(res,200,await call('floorplanManagement','check-network',{body:plan}));return true;}
  if(req.method==='POST'&&!id&&!check){const body=payload(await readJson(req));body.active=false;jsonResponse(res,201,await call('floorplanManagement','create',{body}));return true;}
  if(id&&req.method==='PUT'&&!check){
   const body=await readJson(req);const plan=payload(body);
   // Best-effort stale-edit protection; the upstream service remains the authoritative writer/validator.
   const current=await call('floorplanDiscovery','get-floorplan',{id});
   if(!body.expectedUpdatedAt||body.expectedUpdatedAt!==current.updatedAt)throw failure(409,'This floorplan changed since you opened it. Reload it before saving.');
   jsonResponse(res,200,await call('floorplanManagement','replace',{id,body:plan}));return true;
  }
  if(id&&req.method==='DELETE'&&!check){await call('floorplanManagement','remove',{id});jsonResponse(res,200,{removed:Number(id)});return true;}
  jsonResponse(res,405,{error:'Method not allowed'});return true;
 };
}
module.exports={createFloorplanRoutes,payload};
