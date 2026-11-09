'use strict';
const fail=(status,message)=>Object.assign(new Error(message),{status});
function validatePriority(old,body){const p=body?.priority,e=body?.expected;if(!Number.isInteger(p)||p<1||p>2147483647)throw fail(400,'Use a positive integer priority');if(!e)throw fail(428,'Expected rule snapshot required');if(!old||old.consumer!==e.consumer||old.serviceInstanceId!==e.serviceInstanceId||Number(old.priority)!==Number(e.priority))throw fail(409,'The rule changed; reload before saving');return p;}
function createPriorityRoutes({query,modify,readJson,jsonResponse}){return async(req,res,url)=>{const match=url.pathname.match(/^\/api\/orchestration\/rules\/([0-9a-f-]+)$/i);if(req.method!=='PUT'||!match)return false;if(req.headers['x-arrowhead-action']!=='confirm')throw fail(428,'Missing action confirmation header');const id=match[1],body=await readJson(req),old=(await query()).find(r=>String(r.id)===id),p=validatePriority(old,body);jsonResponse(res,200,await modify(id,p,old));return true;};}
module.exports={createPriorityRoutes,validatePriority};
// For deployments that already authorize create/remove but do not delegate native priority management.
async function replacePriorityWithExistingOperations({query,create,remove},old,priority){
 const before=await query();validatePriority(before.find(r=>r.id===old.id),{priority,expected:old});
 if(before.some(r=>r.id!==old.id&&r.consumer===old.consumer&&r.serviceInstanceId===old.serviceInstanceId&&Number(r.priority)===priority))throw fail(409,'This connection and priority already exist');
 const candidate={consumer:old.consumer,serviceInstanceId:old.serviceInstanceId,priority};let created=null,removeAttempted=false;
 try{
  const receipt=await create(candidate);created=(receipt.entries||[]).find(r=>!before.some(b=>b.id===r.id)&&r.consumer===candidate.consumer&&r.serviceInstanceId===candidate.serviceInstanceId&&Number(r.priority)===priority)||null;const current=await query();created=current.find(r=>!before.some(b=>b.id===r.id)&&r.consumer===candidate.consumer&&r.serviceInstanceId===candidate.serviceInstanceId&&Number(r.priority)===priority)||created;
  if(!created)throw fail(502,'New rule was not confirmed. Reload to inspect the result');
  validatePriority(current.find(r=>r.id===old.id),{priority,expected:old});removeAttempted=true;await remove(old.id);return{entries:[created],count:1};
 }catch(error){
  if(created){try{const current=await query();if(removeAttempted&&!current.some(r=>r.id===old.id||r.consumer===old.consumer&&r.serviceInstanceId===old.serviceInstanceId&&Number(r.priority)===Number(old.priority)))await create({consumer:old.consumer,serviceInstanceId:old.serviceInstanceId,priority:Number(old.priority)});const owned=(await query()).find(r=>r.id===created.id);if(owned&&owned.consumer===candidate.consumer&&owned.serviceInstanceId===candidate.serviceInstanceId&&Number(owned.priority)===priority)await remove(created.id);}catch{throw fail(502,'Priority replacement could not finish recovery. Inspect stored rules before retrying');}}
  throw error;
 }
}
module.exports.replacePriorityWithExistingOperations=replacePriorityWithExistingOperations;
