// SPDX-License-Identifier: EPL-2.0
const t = require('node:test'), a = require('node:assert/strict'), M = require('../public/solc-model');
const catalog = {
  organization: 'Company', relayPort: 8893, architectures: ['amd64', 'arm64'], intercloudComponents: ['gatekeeper', 'gatetunnel'],
  forbiddenConsumers: ['Sysop'],
  components: {
    'random-data-producer': { consumers: ['RandomDataProducer'], exports: [] },
    datamanager: { consumers: [], exports: [{ provider: 'DataManager', service: 'dataHistorian', operations: ['put-data', 'get-data'], services: ['datamanager'] }] }
  }
};
const core = ['serviceregistry', 'authentication', 'consumerauthorization', 'serviceorchestration-dynamic', 'gatekeeper', 'gatetunnel'];
const plant = { id: '11111111-1111-1111-1111-111111111111', name: 'Plant design', revision: 3, components: [...core, 'random-data-producer'] };
const office = { id: '22222222-2222-2222-2222-222222222222', name: 'Office', revision: 1, components: [...core, 'datamanager'] };

t('members get unique cloud names and free port offsets per host', () => {
  let d = M.fresh(catalog);
  d = M.addMember(d, plant, 'mac.local', 'arm64');
  d = M.addMember(d, plant, 'mac.local', 'arm64');
  d = M.addMember(d, office, 'server', 'amd64');
  a.deepEqual(d.members.map(m => m.cloud), ['Plantdesign', 'Plantdesign2', 'Office']);
  a.deepEqual(d.members.map(m => m.portOffset), [0, 1000, 0]);
  a.equal(d.members[0].lcDesign.revision, 3);
  a.ok(d.layout.Office);
});

t('only exchanges the catalog allows are offered, and adding one replaces an identical one', () => {
  let d = M.addMember(M.addMember(M.fresh(catalog), plant, 'a', 'arm64'), office, 'b', 'amd64');
  const options = M.exchangeOptions(catalog, d);
  a.equal(options.length, 1);
  a.deepEqual(options[0], { consumerCloud: 'Plantdesign', consumer: 'RandomDataProducer', providerCloud: 'Office', provider: 'DataManager', service: 'dataHistorian', operations: ['put-data', 'get-data'] });
  d = M.addExchange(d, { ...options[0], operations: ['put-data'] });
  d = M.addExchange(d, { ...options[0], operations: ['put-data', 'get-data'] });
  a.equal(d.exchanges.length, 1);
  a.throws(() => M.addExchange(d, { ...options[0], operations: [] }));
});

t('renaming and removing a member keep exchanges and layout consistent', () => {
  let d = M.addMember(M.addMember(M.fresh(catalog), plant, 'a', 'arm64'), office, 'b', 'amd64');
  d = M.addExchange(d, { ...M.exchangeOptions(catalog, d)[0], operations: ['put-data'] });
  d = M.renameMember(d, 'Plantdesign', 'Plant');
  a.equal(d.exchanges[0].consumerCloud, 'Plant');
  a.ok(d.layout.Plant && !d.layout.Plantdesign);
  a.throws(() => M.renameMember(d, 'Plant', 'office'));
  a.throws(() => M.renameMember(d, 'Plant', 'bad name'));
  d = M.removeMember(d, 'Office');
  a.equal(d.exchanges.length, 0);
});

t('quick checks catch clashing offsets, unpackaged architectures and members without inter-cloud components', () => {
  let d = M.addMember(M.addMember(M.fresh(catalog), plant, 'h', 'arm64'), { ...office, components: core.slice(0, 4) }, 'h', 'amd64');
  d.members[1].portOffset = 0;
  d.packaging.architectures = ['arm64'];
  const p = M.problems(d, catalog).join('\n');
  a.match(p, /same port offset/);
  a.match(p, /architecture amd64 is not packaged/);
  a.match(p, /lacks gatekeeper, gatetunnel/);
  a.deepEqual(M.problems(M.addMember(M.fresh(catalog), plant, 'h', 'arm64'), catalog), []);
});

t('requests carry the design, revision and stored design id only when given', () => {
  const d = M.fresh(catalog);
  a.deepEqual(Object.keys(M.draft(d)), ['design']);
  a.deepEqual(M.draft(d, 4, 'x'), { design: d, expectedRevision: 4, designId: 'x' });
});
