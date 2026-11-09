package eu.arrowhead.sosconfigurator.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.policy.PolicyEngine.CloudConfig;
import eu.arrowhead.sosconfigurator.policy.PolicyEngine.Evaluation;
import eu.arrowhead.sosconfigurator.policy.SosRequest.Exchange;

class PolicyEngineTest {

	static Catalog catalog() throws Exception {
		return Catalog.load(Path.of("catalogs/sos.json"));
	}

	static Exchange x(final String cc, final String consumer, final String pc, final String provider, final String service, final String... ops) {
		return new Exchange(cc, consumer, pc, provider, service, List.of(ops));
	}

	static Set<String> rules(final Evaluation e) {
		return e.violations().stream().map(PolicyEngine.Violation::rule).collect(Collectors.toSet());
	}

	@Test
	void bidirectionalExchangeDerivesLeastPrivilegeConfiguration() throws Exception {
		final Evaluation e = new PolicyEngine(catalog()).evaluate(Json.MAPPER.readValue(Files.readString(Path.of("requests/testcloud-gocloud.json")),
				SosRequest.class));
		assertTrue(e.allowed(), e.violations().toString());
		final CloudConfig test = e.clouds().get("TestCloud");
		assertEquals(List.of(new PolicyEngine.Neighbour("GoCloud", "Company")), test.neighbours());
		assertEquals(1, test.grants().size());
		assertEquals("MGMT|GoCloud|Company|DataManager|SERVICE_DEF|dataHistorian", test.grants().get(0).instanceId());
		assertEquals(List.of("RandomDataProducer"), test.grants().get(0).consumers());
		assertTrue(test.revokes().isEmpty(), "RustDev is of another organization; its grants are never touched");
		assertTrue(test.preconditions().stream().anyMatch(c -> c.consumer().equals("Gatekeeper") && "check-policies".equals(c.scope())));
		assertTrue(test.preconditions().stream().anyMatch(c -> c.consumer().equals("GateTunnel") && "put-data".equals(c.scope())));
		assertEquals(1, e.clouds().get("GoCloud").discovery().size());
		assertEquals(4, e.relay().subjects().size());
		assertTrue(e.relay().subjects().contains("dnQualifier=sy,CN=GateTunnel.GoCloud.Company.arrowhead.eu,OU=GoCloud,O=Company,C=SE"));
	}

	@Test
	void oneWayExchangeRevokesTheOtherDirection() throws Exception {
		final Evaluation e = new PolicyEngine(catalog()).evaluate(Json.MAPPER.readValue(
				Files.readString(Path.of("requests/gocloud-to-testcloud-only.json")), SosRequest.class));
		assertTrue(e.allowed());
		final CloudConfig go = e.clouds().get("GoCloud");
		assertTrue(go.grants().isEmpty());
		assertEquals(List.of("MGMT|TestCloud|Company|DataManager|SERVICE_DEF|dataHistorian"), go.revokes());
		assertTrue(go.verify().stream().anyMatch(c -> !c.expected() && c.cloud().equals("TestCloud|Company")),
				"the revocation is verified with a negative check");
		assertEquals(List.of(new PolicyEngine.Neighbour("TestCloud", "Company")), go.neighbours(), "the consumer cloud needs the provider as neighbour");
	}

	@Test
	void releaseWithdrawsNeighboursAndGrants() throws Exception {
		final Evaluation e = new PolicyEngine(catalog()).evaluate(new SosRequest("Release", List.of("TestCloud", "GoCloud"), List.of(), false));
		assertTrue(e.allowed());
		assertTrue(e.clouds().get("TestCloud").neighbours().isEmpty());
		assertEquals(List.of(new PolicyEngine.Neighbour("GoCloud", "Company")), e.clouds().get("TestCloud").removeNeighbours());
		assertEquals(1, e.clouds().get("TestCloud").revokes().size());
	}

	@Test
	void securityRulesRefuseForeignOrganizationAndUnsupportedImplementation() throws Exception {
		final Evaluation e = new PolicyEngine(catalog()).evaluate(Json.MAPPER.readValue(Files.readString(Path.of("requests/refused-rustdev.json")),
				SosRequest.class));
		assertFalse(e.allowed());
		assertTrue(rules(e).containsAll(Set.of("S3", "S4", "S5")), rules(e).toString());
	}

	@Test
	void authorizationRulesRefuseUncataloguedAndPrivilegedConsumersAndServices() throws Exception {
		final PolicyEngine pe = new PolicyEngine(catalog());
		final List<String> m = List.of("TestCloud", "GoCloud");
		assertEquals(Set.of("A2"), rules(pe.evaluate(new SosRequest("s", m, List.of(x("GoCloud", "Sysop", "TestCloud", "DataManager", "dataHistorian", "put-data")), false))));
		assertEquals(Set.of("A2"), rules(pe.evaluate(new SosRequest("s", m, List.of(x("GoCloud", "DemoSensor", "TestCloud", "DataManager", "dataHistorian", "put-data")), false))));
		assertEquals(Set.of("A3"), rules(pe.evaluate(new SosRequest("s", m, List.of(x("GoCloud", "RandomDataProducer", "TestCloud", "ServiceRegistry", "serviceRegistryManagement", "query")), false))));
		assertEquals(Set.of("A4"), rules(pe.evaluate(new SosRequest("s", m, List.of(x("GoCloud", "RandomDataProducer", "TestCloud", "DataManager", "dataHistorian", "get-data")), false))));
		assertEquals(Set.of("A4"), rules(pe.evaluate(new SosRequest("s", m, List.of(x("GoCloud", "RandomDataProducer", "TestCloud", "DataManager", "dataHistorian")), false))));
		assertEquals(Set.of("A1"), rules(pe.evaluate(new SosRequest("s", m, List.of(x("GoCloud", "RandomDataProducer", "GoCloud", "DataManager", "dataHistorian", "put-data")), false))));
		assertEquals(Set.of("A1", "S2"), rules(pe.evaluate(new SosRequest("s", List.of("GoCloud"), List.of(x("GoCloud", "RandomDataProducer", "TestCloud", "DataManager", "dataHistorian", "put-data")), false))));
		assertEquals(Set.of("S0"), rules(pe.evaluate(new SosRequest("bad name;", m, List.of(), false))));
		assertEquals(Set.of("S1"), rules(pe.evaluate(new SosRequest("s", List.of("TestCloud", "Nowhere"), List.of(), false))));
	}

	@Test
	void catalogRefusesExportOfManagementServices() throws Exception {
		final String bad = Files.readString(Path.of("catalogs/sos.json")).replace("\"service\": \"dataHistorian\", \"operations\": [\"put-data\"], \"services\": [\"datamanager\", \"datamanager-rust\"]}\n      ]\n    },\n    {\n      \"name\": \"GoCloud\"",
				"\"service\": \"dataManagement\", \"operations\": [\"put-data\"], \"services\": [\"datamanager\"]}\n      ]\n    },\n    {\n      \"name\": \"GoCloud\"");
		final Path f = Files.createTempFile("catalog", ".json");
		Files.writeString(f, bad);
		assertThrows(IllegalArgumentException.class, () -> Catalog.load(f));
	}
}
