'use strict';
const fail=(status,message)=>Object.assign(new Error(message),{status});
const name=/^[A-Za-z][A-Za-z0-9_]{0,62}$/;
function stable(v){if(Array.isArray(v))return v.map(stable);if(v&&typeof v==='object')return Object.fromEntries(Object.keys(v).sort().map(k=>[k,stable(v[k])]));return v;}
function clean(row){return Object.fromEntries(['provider','targetType','target','cloud','description','defaultPolicy','scopedPolicies'].map(k=>[k,k==='cloud'?'LOCAL':k==='scopedPolicies'?row[k]||{}:k==='description'?row[k]||'':row[k]]));}
function local(row){return (!row.cloud||row.cloud==='LOCAL')&&(!row.level||row.level==='MGMT');}
function validate(row,systems,services){
 if(!row||typeof row!=='object'||Array.isArray(row))throw fail(400,'A policy is required');
 if(row.cloud&&row.cloud!=='LOCAL')throw fail(400,'Only local-cloud policies can be edited');
 if(row.level&&row.level!=='MGMT')throw fail(400,'Provider-owned policies are read-only in this editor');
 if(!systems.some(s=>s.name===row.provider))throw fail(409,'Provider is not registered in this local cloud');
 if(!['SERVICE_DEF','EVENT_TYPE'].includes(row.targetType)||!name.test(row.provider)||typeof row.target!=='string'||!name.test(row.target))throw fail(400,'Invalid local policy target');
 if(row.targetType==='SERVICE_DEF'&&!services.some(s=>s.provider?.name===row.provider&&s.serviceDefinition?.name===row.target))throw fail(409,'Service is not registered for this local provider');
 function policy(p){if(!p||!['ALL','WHITELIST','BLACKLIST'].includes(p.policyType)||!Array.isArray(p.policyList||[])||(p.policyList||[]).length>500||(p.policyList||[]).some(n=>typeof n!=='string'||!name.test(n)))throw fail(400,'Use ALL, WHITELIST or BLACKLIST with local system names');return{policyType:p.policyType,policyList:p.policyType==='ALL'?[]:[...new Set(p.policyList||[])]};}
 const scoped=row.scopedPolicies||{};if(typeof scoped!=='object'||Array.isArray(scoped)||Object.keys(scoped).length>100)throw fail(400,'Invalid operation scopes');
 for(const k of Object.keys(scoped))if(!/^[A-Za-z0-9_.:-]{1,100}$/.test(k))throw fail(400,'Invalid operation scope');
 if(typeof(row.description||'')!=='string'||(row.description||'').length>1000)throw fail(400,'Description must be at most 1000 characters');
 return{...clean(row),defaultPolicy:policy(row.defaultPolicy),scopedPolicies:Object.fromEntries(Object.entries(scoped).map(([k,v])=>[k,policy(v)]))};
}
function checkExpected(rows,expected,policy){
 const matches=rows.filter(r=>r.provider===policy.provider&&r.targetType===policy.targetType&&r.target===policy.target);
 if(matches.length>1)throw fail(409,'Multiple policies share this target; resolve them before editing');
 const old=matches[0];
 if(!expected&&old)throw fail(409,'Policy already exists; reload before editing');
 if(expected&&(!old||old.instanceId!==expected.instanceId||JSON.stringify(stable(clean(old)))!==JSON.stringify(stable(clean(expected)))))throw fail(409,'Policy changed; reload before saving');
 return old;
}
function createPolicyRoutes({inventory,query,grant,revoke,readJson,jsonResponse}){
 return async function(req,res,url){if(!url.pathname.startsWith('/api/orchestration/policies'))return false;
 const match=url.pathname.match(/^\/api\/orchestration\/policies(?:\/([^/]+))?$/);if(!match){jsonResponse(res,404,{error:'Not found'});return true;}
 const id=match[1]?decodeURIComponent(match[1]):null;
 if(req.method==='GET'&&!id){const entries=await query();jsonResponse(res,200,{entries:entries.filter(local),readOnlyEntries:entries.filter(p=>!local(p)&&(!p.cloud||p.cloud==='LOCAL')),collectedAt:new Date().toISOString()});return true;}
 if(!['POST','DELETE'].includes(req.method)||req.method==='DELETE'&&!id||req.method==='POST'&&id){jsonResponse(res,405,{error:'Method not allowed'});return true;}
 if(req.headers['x-arrowhead-action']!=='confirm')throw fail(428,'Missing action confirmation header');
 const body=await readJson(req),entries=(await query()).filter(local);
 if(req.method==='DELETE'){const row=entries.find(r=>r.instanceId===id);if(!row)throw fail(404,'Local management policy not found');if(!body.expected)throw fail(428,'Expected policy snapshot required');if(body.expected.instanceId!==row.instanceId||JSON.stringify(stable(clean(row)))!==JSON.stringify(stable(clean(body.expected))))throw fail(409,'Policy changed; reload before deleting');await revoke(id);jsonResponse(res,200,{removed:true,id});return true;}
 const inv=await inventory(),policy=validate(body.policy,inv.systems.entries||[],inv.services.entries||[]),old=checkExpected(entries,body.expected,policy);
 let result;
 try{result=await grant(policy);}catch(error){
  // Java's management API rejects an existing target instead of upserting it.
  // Use its documented revoke/grant protocol only for that explicit duplicate error.
  if(!old||![400,409].includes(error.status)||!/policy.*already exist|already exist.*policy/i.test(error.message))throw error;
  checkExpected((await query()).filter(local),body.expected,policy);
  await revoke(old.instanceId);
  try{result=await grant(policy);}catch(replacementError){
   try{await grant(clean(old));}catch{throw fail(502,'Replacement failed and the old policy could not be restored. Reload and recover this policy before retrying');}
   throw fail(502,'Replacement failed; the previous policy was restored');
  }
 }
 const created=(result.entries||[result])[0];if(!created?.instanceId)throw fail(502,'Authority did not return a policy identifier; reload to inspect the result');
 if(old&&old.instanceId!==created.instanceId){try{await revoke(old.instanceId);}catch(error){try{await revoke(created.instanceId);}catch{}throw fail(502,'Policy replacement could not finish; reload policies before retrying');}}
 jsonResponse(res,old?200:201,created);return true;
 };}
module.exports={createPolicyRoutes,validate,checkExpected,local,clean};
