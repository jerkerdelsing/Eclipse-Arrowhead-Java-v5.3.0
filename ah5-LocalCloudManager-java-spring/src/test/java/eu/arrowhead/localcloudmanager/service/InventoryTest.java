package eu.arrowhead.localcloudmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// Port of the native server.test.js (pure functions of the console API).
public class InventoryTest {

	private static JsonNode j(final String json) throws Exception {
		return Json.MAPPER.readTree(json.replace('\'', '"'));
	}

	private static List<JsonNode> l(final String json) throws Exception {
		return Json.list(j(json));
	}

	@Test
	public void demuxDockerLogsDecodesDockerStreamFrames() {
		final byte[] payload = "hello\n".getBytes();
		final byte[] frame = new byte[8 + payload.length];
		frame[0] = 1;
		frame[7] = (byte) payload.length;
		System.arraycopy(payload, 0, frame, 8, payload.length);
		assertEquals("hello\n", Inventory.demuxDockerLogs(frame));
	}

	@Test
	public void systemStatusAssociatesAnArrowheadSystemWithItsComposeService() throws Exception {
		final ObjectNode result = Inventory.systemStatus(j("{'name':'ServiceRegistry','version':'5.2.2'}"),
				l("[{'service':'serviceregistry','state':'running','status':'Up','health':'healthy','managed':true}]"), Map.of("ServiceRegistry", 5));
		assertEquals("serviceregistry", result.path("composeService").asText());
		assertEquals(5, result.path("serviceCount").asInt());
		assertEquals("running", result.path("runtime").path("state").asText());
	}

	@Test
	public void registryManagementLinksDevicesSystemsServicesAndDeploymentState() throws Exception {
		final List<JsonNode> systems = List.of(
				Inventory.systemStatus(j("{'name':'ServiceRegistry','deviceName':'FactoryFloor','version':'1.0.0'}"),
						l("[{'service':'serviceregistry','displayName':'ServiceRegistry','state':'running','managed':true}]"), Map.of("ServiceRegistry", 1)),
				j("{'name':'ExternalConsumer','deviceName':null,'runtime':null,'composeService':null}"));
		final List<JsonNode> services = l("[{'instanceId':'ServiceRegistry|temperature|1.0.0','provider':{'name':'ServiceRegistry'},'serviceDefinition':{'name':'temperature'}}]");
		final ObjectNode result = Inventory.buildRegistryManagement(l("[{'name':'FactoryFloor','addresses':['10.0.0.5']}]"), systems, services,
				l("[{'service':'serviceregistry','displayName':'ServiceRegistry','state':'running','managed':true},{'service':'worker','displayName':'Worker','state':'running','managed':true}]"));
		assertEquals(1, result.path("summary").path("registeredDevices").asInt());
		assertEquals(1, result.path("summary").path("deployedDevices").asInt());
		assertEquals(1, result.path("summary").path("deployedServices").asInt());
		assertEquals("temperature", result.path("devices").get(0).path("systems").get(0).path("services").get(0).path("serviceDefinition").path("name").asText());
		assertEquals("No registered device", result.path("devices").get(result.path("devices").size() - 1).path("name").asText());
		assertEquals("worker", result.path("runtimeOnly").get(0).path("composeService").asText());
		assertEquals(1, result.path("runtimeOnly").size());
	}

	@Test
	public void databaseServicesAreExcludedFromLifecycleManagement() {
		assertTrue(Inventory.DEFAULT_MANAGED_SERVICES.contains("serviceregistry"));
		assertTrue(!Inventory.DEFAULT_MANAGED_SERVICES.contains("serviceregistry-db"));
	}

	@Test
	public void runtimeIdentifiersHaveCanonicalArrowheadDisplayNames() {
		assertEquals("GraphDBManager", Inventory.RUNTIME_DISPLAY_NAMES.get("graphdbmanager"));
		assertEquals("ServiceRegistry", Inventory.RUNTIME_DISPLAY_NAMES.get("serviceregistry"));
		assertEquals("DynamicServiceOrchestration", Inventory.RUNTIME_DISPLAY_NAMES.get("serviceorchestration-dynamic"));
		assertEquals("SysMonitorCompatibilityAdapter", Inventory.RUNTIME_DISPLAY_NAMES.get("system-monitor-publisher"));
	}

	@Test
	public void inventoryNamingFollowsPascalCaseCamelCaseAndKebabCase() throws Exception {
		assertEquals(List.of(), Inventory.validateInventoryNames(l("[{'name':'CertificateAuthority'}]"),
				l("[{'serviceDefinition':{'name':'certificateSigning'},'interfaces':[{'properties':{'operations':{'check-certificate':{}}}}]}]")));
		assertEquals(List.of("legacy-service-definition:monitor", "legacy-system:SystemMonitorPublisher", "operation:checkCertificate", "service-definition:CertificateSigning",
				"system:certificate-authority"),
				Inventory.validateInventoryNames(l("[{'name':'certificate-authority'},{'name':'SystemMonitorPublisher'}]"),
						l("[{'serviceDefinition':{'name':'CertificateSigning'},'interfaces':[{'properties':{'operations':['checkCertificate']}}]},"
								+ "{'serviceDefinition':{'name':'monitor'},'interfaces':[{'properties':{'operations':['echo']}}]}]")));
	}

	@Test
	public void numericValueExtractsNumericSenmlValuesOnly() throws Exception {
		assertEquals(42.0, Inventory.numericValue(j("{'value':{'v':42}}")));
		assertEquals(7.0, Inventory.numericValue(j("{'value':{'s':7}}")));
		assertEquals(null, Inventory.numericValue(j("{'value':{'vs':'online'}}")));
	}

	@Test
	public void messagingOverviewExposesBrokerAndActiveSecuredFlows() throws Exception {
		final StringBuilder containers = new StringBuilder("[");
		for (final String s : List.of("random-data-producer", "mqtt-broker", "amqp-broker", "sysmonitor", "serviceorchestration-dynamic", "datamanager", "aigym")) {
			containers.append(containers.length() > 1 ? "," : "").append("{'service':'").append(s).append("','state':'running','status':'Up (healthy)','health':'healthy','managed':true}");
		}
		final ObjectNode result = Inventory.buildMessagingOverview(l(containers + "]"),
				l("[{'instanceId':'SysMonitor|sysMonitorData|1.0.0'},{'instanceId':'SysMonitor|sysMonitorHistory|1.0.0'},{'instanceId':'DataManager|dataHistorian|1.0.0'},"
						+ "{'instanceId':'DataManager|sysMonitor|1.0.0'},{'instanceId':'AIGym|sysMonitor|1.0.0'}]"));
		assertEquals("amqps://amqp-broker:5671 · exchange arrowhead", result.path("infrastructure").get(0).path("endpoint").asText());
		assertEquals("mqtts://mqtt-broker:8883", result.path("infrastructure").get(1).path("endpoint").asText());
		final List<JsonNode> flows = Json.list(result.path("orchestrations"));
		assertEquals(6, flows.size());
		assertTrue(flows.stream().allMatch(f -> f.path("active").asBoolean()));
		assertEquals(List.of("sysmonitor-publish-aigym", "aigym-sysmonitor-data"), flows.stream().map(f -> f.path("id").asText()).filter(id -> id.contains("aigym")).toList());
		assertEquals("sysMonitor.observe · native", flows.stream().filter(f -> f.path("id").asText().equals("sysmonitor-publish-datamanager")).findFirst().orElseThrow().path("label").asText());
		assertTrue(flows.stream().noneMatch(f -> "DataManager".equals(f.path("target").asText()) && "SysMonitor".equals(f.path("source").asText())));
		assertEquals("LocalCloudManager", flows.stream().filter(f -> f.path("id").asText().equals("sysmonitor-history")).findFirst().orElseThrow().path("target").asText());
	}

	@Test
	public void orchestrationCatalogKeepsOnlyInterfaceCompatibleConsumers() throws Exception {
		final ObjectNode catalog = Inventory.buildOrchestrationCatalog(
				l("[{'name':'Producer','metadata':{'x509PublicKey':'producer-key'}},{'name':'SecureConsumer','metadata':{'x509PublicKey':'consumer-key'}},{'name':'HttpConsumer','metadata':{}}]"),
				l("[{'instanceId':'Producer|temperature|1.0.0','provider':{'name':'Producer'},'serviceDefinition':{'name':'temperature'},'interfaces':[{'templateName':'generic_https','protocol':'https','policy':'CERT_AUTH'}]},"
						+ "{'instanceId':'SecureConsumer|status|1.0.0','provider':{'name':'SecureConsumer'},'serviceDefinition':{'name':'status'},'interfaces':[{'templateName':'generic_https','protocol':'https','policy':'CERT_AUTH'}]},"
						+ "{'instanceId':'HttpConsumer|status|1.0.0','provider':{'name':'HttpConsumer'},'serviceDefinition':{'name':'status'},'interfaces':[{'templateName':'generic_http','protocol':'http','policy':'NONE'}]}]"));
		final JsonNode temperature = Json.list(catalog, "producers").stream().filter(p -> p.path("instanceId").asText().equals("Producer|temperature|1.0.0")).findFirst().orElseThrow();
		assertEquals("[\"SecureConsumer\"]", temperature.path("compatibleConsumers").toString());
	}

	@Test
	public void ruleValidationRejectsAStaleOrIncompatibleSelection() throws Exception {
		final JsonNode catalog = j("{'producers':[{'instanceId':'Provider|service|1.0.0','compatibleConsumers':['Consumer']}]}");
		assertEquals("{\"consumer\":\"Consumer\",\"serviceInstanceId\":\"Provider|service|1.0.0\",\"priority\":1}",
				Inventory.validateRuleCandidate(catalog, j("{'consumer':'Consumer','serviceInstanceId':'Provider|service|1.0.0','priority':1}")).toString());
		final ApiException ex = assertThrows(ApiException.class, () -> Inventory.validateRuleCandidate(catalog, j("{'consumer':'Other','serviceInstanceId':'Provider|service|1.0.0','priority':1}")));
		assertTrue(ex.getMessage().contains("not interoperable"));
		assertEquals(400, assertThrows(ApiException.class, () -> Inventory.validateRuleCandidate(catalog, j("{'consumer':'Consumer','serviceInstanceId':'x','priority':0}"))).status());
	}

	@Test
	public void resolveSenmlRecordsExpandsDataHistorianPacks() throws Exception {
		final List<ObjectNode> entries = Inventory.resolveSenMLRecords(l("[{'bn':'urn:arrowhead:random-data-producer:','bt':1000,'n':'random-number','u':'1','v':42},"
				+ "{'bn':'urn:arrowhead:sysmonitor:DataManager:','bt':2000,'n':'echo','vs':'{\\\"status\\\":\\\"alive\\\"}'},{'n':'power','t':5,'vs':'{}'}]"),
				"RandomDataProducer", "randomNumberProducer");
		assertEquals(3, entries.size());
		assertEquals("urn:arrowhead:random-data-producer:random-number", entries.get(0).path("name").asText());
		assertEquals(1000, entries.get(0).path("time").asLong());
		assertEquals("urn:arrowhead:sysmonitor:DataManager:echo", entries.get(1).path("name").asText());
		assertEquals(2000, entries.get(1).path("time").asLong());
		assertEquals("urn:arrowhead:sysmonitor:DataManager:power", entries.get(2).path("name").asText());
		assertEquals(2005, entries.get(2).path("time").asLong());
		assertEquals(42.0, Inventory.numericValue(entries.get(0)));
		assertEquals("1", entries.get(0).path("unit").asText());
	}

	@Test
	public void aRegisteredGraphDbManagerIsMatchedToItsRunningContainer() throws Exception {
		final List<JsonNode> containers = l("[{'service':'graphdbmanager','displayName':'GraphDBManager','state':'running','managed':true}]");
		final ObjectNode result = Inventory.buildRegistryManagement(List.of(), List.of(Inventory.systemStatus(j("{'name':'GraphDBManager'}"), containers, Map.of())),
				l("[{'instanceId':'GraphDBManager|graphDataHistorian|1.0.0','provider':{'name':'GraphDBManager'},'serviceDefinition':{'name':'graphDataHistorian'}}]"), containers);
		assertEquals(0, result.path("runtimeOnly").size());
		assertTrue(Set.copyOf(Inventory.DEFAULT_MANAGED_SERVICES).contains("graphdbmanager"));
	}
}
