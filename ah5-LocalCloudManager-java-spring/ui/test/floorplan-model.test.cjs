const test=require('node:test'),assert=require('node:assert/strict');const M=require('../public/floorplan-model.js');
function plan(){return{name:'test',interposer:{width:30,height:20},spacing:.5,chiplets:[{chipletId:'cpu',type:'CPU',x:1,y:1,width:8,height:8,rotation:0},{chipletId:'memory',type:'MEMORY',x:12,y:1,width:6,height:10,rotation:0}],links:[{from:'cpu',to:'memory',type:'UCIe',bandwidthGbps:64}],network:{fabric:'UCIe',routers:[{id:'router1',chipletId:'cpu'}]}};}
test('valid connected geometry is accepted',()=>assert.deepEqual(M.validate(plan()),[]));
test('overlap, bounds and duplicate IDs are detected',()=>{for(const change of [p=>p.chiplets[1].x=2,p=>p.chiplets[0].width=50,p=>p.chiplets[1].chipletId='cpu']){let p=plan();change(p);assert.ok(M.validate(p).length>0)}});
test('unknown links, negative bandwidth and invalid rotation are detected',()=>{for(const change of [p=>p.links[0].to='missing',p=>p.links[0].bandwidthGbps=-1,p=>p.chiplets[0].rotation=45]){let p=plan();change(p);assert.ok(M.validate(p).length>0)}});
test('auto-layout is valid, deterministic and preserves network metadata',()=>{const original=plan(),snapshot=M.clone(original),a=M.arrange(original),b=M.arrange(original);assert.deepEqual(original,snapshot);assert.deepEqual(a,b);assert.deepEqual(M.validate(a),[]);assert.deepEqual(a.network,original.network);assert.deepEqual(a.links,original.links)});
test('auto-layout fails without mutating an impossible plan',()=>{const p=plan();p.interposer={width:4,height:4};const before=M.clone(p);assert.throws(()=>M.arrange(p),/do not fit/);assert.deepEqual(p,before)});
test('connections attach to rectangle boundaries',()=>{const[a,b]=plan().chiplets,pt=M.endpoint(a,b);assert.ok(Math.abs(pt.x-(a.x+a.width))<1e-9);assert.ok(pt.y>=a.y&&pt.y<=a.y+a.height)});
test('malformed imported JSON produces validation messages rather than throwing',()=>{for(const p of [null,[],{name:'bad',interposer:{width:20,height:20},chiplets:[null]}, {...plan(),links:'bad'}])assert.ok(M.validate(p).length)});

test('all six Table 7 models are selectable without mutating geometry, routes or endpoints',()=>{
 assert.deepEqual(M.networkModels.map(m=>m.id),['UCIe','AIB','BoW','SharedBus','StaticNoC','Proposed']);
 for(const profile of M.networkModels){const p=plan();p.network.d2dPorts=[{id:'p1',standard:'UCIe',lanes:16}];p.network.flows=[{id:'f1',route:['l1']}];p.network.endpoints=[{leid:'cpu'}];const before=M.clone(p),next=M.selectNetwork(p,profile.id,'BoW');assert.deepEqual(p,before);assert.equal(next.network.model,profile.id);assert.equal(next.network.fabric,profile.carrier||'BoW');assert.equal(next.network.d2dPorts[0].standard,next.network.fabric);assert.deepEqual(next.chiplets,p.chiplets);assert.deepEqual(next.network.flows,p.network.flows);assert.deepEqual(next.network.endpoints,p.network.endpoints);assert.deepEqual(M.validateNetworkChoice(next),[]);}
});
test('legacy networks are proposed fabrics; invalid models/carriers/mismatches are rejected',()=>{
 assert.equal(M.networkChoice(plan()),'Proposed');assert.deepEqual(M.validateNetworkChoice(plan()),[]);
 for(const n of [{model:'',fabric:'UCIe'},{model:null,fabric:'UCIe'},{model:'unknown',fabric:'UCIe'},{model:'UCIe',fabric:'BoW'},{model:'StaticNoC',fabric:'unknown'},{model:'Proposed',fabric:'UCIe',d2dPorts:{}},{model:'Proposed',fabric:'UCIe',d2dPorts:[null]}])assert(M.validateNetworkChoice({network:n}).length);
 assert.throws(()=>M.selectNetwork(plan(),'unknown'),/Choose a network model/);
});
