/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.jpa.service;

import static eu.arrowhead.sysmonitor.service.Json.map;
import static eu.arrowhead.sysmonitor.service.Json.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import eu.arrowhead.common.jpa.RefreshableRepositoryImpl;
import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.jpa.repository.ObservationRepository;
import eu.arrowhead.sysmonitor.service.ObservationFormat;
import eu.arrowhead.sysmonitor.service.SubscriptionConflictException;

// Ported from the native ContractTest (store part), on H2 in MySQL mode. The MySQL schema itself is validated by
// MySqlEndToEndTest against a container built from deployment/database.
// the deployment config/ folder of the working directory is not used: only the packaged development sample
@DataJpaTest(properties = {
		"spring.config.location=classpath:/application.properties",
		"spring.datasource.url=jdbc:h2:mem:sysmonitor;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop" })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = MonitorDbServiceTest.StoreConfig.class)
public class MonitorDbServiceTest {

	//=================================================================================================
	// members

	@Autowired
	private MonitorDbService store;

	@Autowired
	private ObservationRepository observations;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void historyOutboxDeduplicates() {
		final Map<String, Object> observation = ObservationFormat.normalize(map("observationId", "fixture-1", "sourceTime", "2026-10-15T12:00:00Z", "observationType", "status",
				"value", map("state", "running")), "TestCloud.sysmonitor.Provider.instance.status", "TestCloud");
		assertTrue(store.enqueue(observation));
		assertFalse(store.enqueue(observation));
		assertEquals(1, store.pending().size());
		assertEquals(1, store.systems().size());
		assertEquals(Map.of("systemName", "Provider", "observationCount", 1L, "firstObservationTime", "2026-10-15T12:00:00Z", "lastObservationTime", "2026-10-15T12:00:00Z"),
				store.systems().get(0));
		assertEquals(1, store.history("Provider", List.of("status"), null, null, 100).size());
		assertTrue(store.history("Other", List.of(), null, null, 100).isEmpty());
		assertTrue(store.latest("Provider").containsKey("status"));
		assertEquals(observation.get("observationId"), ((Map<?, ?>) store.history("Provider", List.of(), null, null, 100).get(0)).get("observationId"));

		store.published("fixture-1");
		assertTrue(store.pending().isEmpty());
		assertEquals(1, store.history("Provider", List.of(), null, null, 100).size());
		assertEquals(Map.of("pending", 0L, "stored", 1L), store.counts());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void failedPublicationIsRetriedLater() {
		store.enqueue(ObservationFormat.normalize(map("observationId", "retry-1", "sourceTime", now(), "value", map("status", "alive")), "TestCloud.sysmonitor.Provider.i.echo", "TestCloud"));
		store.failed("retry-1");
		assertTrue(store.pending().isEmpty(), "not due before the retry delay");
		assertEquals(Map.of("pending", 1L, "stored", 1L), store.counts());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void historyFiltersOrderAndLatest() {
		for (int i = 0; i < 5; ++i) {
			final String type = i % 2 == 0 ? "echo" : "status";
			final Map<String, Object> value = type.equals("echo") ? map("status", "alive") : map("state", "running");
			store.enqueue(ObservationFormat.normalize(map("observationId", "h-" + i, "sourceTime", "2026-10-15T12:00:0" + i + "Z", "value", value),
					"TestCloud.sysmonitor.Provider.i." + type, "TestCloud"));
		}

		final List<Object> all = store.history("Provider", List.of(), null, null, 100);
		assertEquals(List.of("h-4", "h-3", "h-2", "h-1", "h-0"), all.stream().map(o -> ((Map<?, ?>) o).get("observationId")).toList());
		assertEquals(3, store.history("Provider", List.of("echo"), null, null, 100).size());
		assertEquals(2, store.history("Provider", List.of(), null, null, 2).size());
		final double from = Instant.parse("2026-10-15T12:00:01Z").getEpochSecond();
		final double to = Instant.parse("2026-10-15T12:00:03Z").getEpochSecond();
		assertEquals(3, store.history("Provider", List.of(), from, to, 100).size());
		final Map<String, Object> latest = store.latest("Provider");
		assertEquals(Set.of("echo", "status"), latest.keySet());
		assertEquals("h-4", ((Map<?, ?>) latest.get("echo")).get("observationId"));
		assertEquals("h-3", ((Map<?, ?>) latest.get("status")).get("observationId"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void subscriptionRevisionConflictsAndWildcardValidation() {
		final Map<String, Object> first = store.put("Provider", List.of("status", "echo"), true, "LocalCloudManager", null);
		assertEquals(1, first.get("revision"));
		assertEquals(List.of("echo", "status"), first.get("observationTypes"));
		assertEquals(List.of("providerSystem", "observationTypes", "enabled", "revision", "updatedAt", "updatedBy"), List.copyOf(first.keySet()));
		final Map<String, Object> second = store.put("Provider", List.of("*"), false, "LocalCloudManager", 1);
		assertEquals(2, second.get("revision"));
		assertFalse((Boolean) second.get("enabled"));
		assertThrows(SubscriptionConflictException.class, () -> store.put("Provider", List.of("echo"), true, "LocalCloudManager", 1));
		assertThrows(SubscriptionConflictException.class, () -> store.put("Unknown", List.of("echo"), true, "LocalCloudManager", 1));

		assertThrows(IllegalArgumentException.class, () -> store.put("Bad.Provider", List.of("echo"), true, "LocalCloudManager", null));
		assertThrows(IllegalArgumentException.class, () -> store.put("Provider", List.of("*", "echo"), true, "LocalCloudManager", null));
		assertThrows(IllegalArgumentException.class, () -> store.put("Provider", List.of(), true, "LocalCloudManager", null));
		assertThrows(IllegalArgumentException.class, () -> store.put("Provider", List.of("a/b"), true, "LocalCloudManager", null));

		assertTrue(store.delete("Provider"));
		assertFalse(store.delete("Provider"));
		assertTrue(store.subscriptions().isEmpty());

		store.bindings(Set.of("TestCloud.sysmonitor.Provider.instance.echo"));
		assertEquals(Set.of("TestCloud.sysmonitor.Provider.instance.echo"), store.bindings());
		store.bindings(Set.of());
		assertTrue(store.bindings().isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void filterCannotSelectAnotherSystem() {
		store.enqueue(ObservationFormat.normalize(map("sourceTime", now(), "value", map("status", "alive")), "TestCloud.sysmonitor.Provider.instance.echo", "TestCloud"));
		assertTrue(store.history("' OR 1=1 --", List.of(), null, null, 10).isEmpty());
		assertTrue(store.latest("' OR 1=1 --").isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void pruneRemovesOnlyExpiredHistory() {
		store.enqueue(ObservationFormat.normalize(map("observationId", "fresh", "sourceTime", now(), "value", map("status", "alive")), "TestCloud.sysmonitor.P.i.echo", "TestCloud"));
		assertEquals(0, store.prune(1));
		assertEquals(1, store.prune(-1), "everything received before now + 1 day");
		assertEquals(0, observations.count());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void auditIsStored() {
		store.audit("Sysop", "POST", "reconcile", "accepted");
		store.audit("Sysop", "subscribe", "P", "x".repeat(5000));
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	// not a @Configuration: SysMonitorMain scans eu.arrowhead, test classes included, and must not pick this up
	@EntityScan(SysMonitorConstants.DATABASE_ENTITY_PACKAGE)
	@EnableJpaRepositories(basePackages = SysMonitorConstants.DATABASE_REPOSITORY_PACKAGE, repositoryBaseClass = RefreshableRepositoryImpl.class)
	@Import(MonitorDbService.class)
	static class StoreConfig {
	}
}
