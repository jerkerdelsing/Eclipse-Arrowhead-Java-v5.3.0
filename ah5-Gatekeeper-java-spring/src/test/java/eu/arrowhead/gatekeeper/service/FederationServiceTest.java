/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.service;

import static eu.arrowhead.gatekeeper.service.dto.JsonValues.list;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.m;
import static eu.arrowhead.gatekeeper.service.dto.JsonValues.obj;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.gatekeeper.jpa.entity.Cloud;
import eu.arrowhead.gatekeeper.jpa.service.GatekeeperDbService;
import eu.arrowhead.gatekeeper.relay.RelayClient;

// The provider-side checks of the native SecurityTest (ConsumerAuthorization check-policies for the foreign consumer) and
// the consumer/provider protocol steps of global service discovery and inter-cloud negotiation.
public class FederationServiceTest {

	//=================================================================================================
	// members

	private ArrowheadConsumer consumer;
	private GatekeeperDbService db;
	private RelayClient relay;
	private final List<Map<String, Object>> policyChecks = new ArrayList<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		consumer = mock(ArrowheadConsumer.class);
		db = mock(GatekeeperDbService.class);
		relay = mock(RelayClient.class);
		policyChecks.clear();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void providerSideChecksForeignConsumerWithCheckPolicies() {
		final FederationService service = gatekeeper("legacy-mtls", b -> decisions(true, true));
		assertTrue(service.authorized("ProviderA", "ConsumerB", "OtherCloud|Company", "temperature", List.of("read", "write")));
		final List<Object> checks = list(policyChecks.getFirst().get("list"));
		assertEquals(2, checks.size());
		final Map<String, Object> first = obj(checks.getFirst());
		assertEquals("ProviderA", first.get("provider"));
		assertEquals("ConsumerB", first.get("consumer"));
		assertEquals("OtherCloud|Company", first.get("cloud"), "the foreign cloud is checked, not the local one");
		assertEquals("SERVICE_DEF", first.get("targetType"));
		assertEquals("temperature", first.get("target"));
		assertEquals("read", first.get("scope"));
		assertEquals("write", obj(checks.get(1)).get("scope"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void providerSideDeniesUnlessEveryOperationIsGranted() {
		assertFalse(gatekeeper("legacy-mtls", b -> decisions(true, false)).authorized("ProviderA", "ConsumerB", "OtherCloud|Company", "temperature", List.of("read", "write")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void providerSideDeniesAnIncompleteAnswer() {
		assertFalse(gatekeeper("legacy-mtls", b -> decisions(true)).authorized("ProviderA", "ConsumerB", "OtherCloud|Company", "temperature", List.of("read", "write")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void providerSideFailsClosedWithoutConsumerAuthorization() {
		final FederationService service = gatekeeper("legacy-mtls", b -> {
			throw new ForbiddenException("no grant");
		});
		assertFalse(service.authorized("ProviderA", "ConsumerB", "OtherCloud|Company", "temperature", List.of("read")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void providerSideChecksTheServiceWhenNoOperationIsNamed() {
		final FederationService service = gatekeeper("legacy-mtls", b -> decisions(true));
		assertTrue(service.authorized("ProviderA", "ConsumerB", "OtherCloud|Company", "temperature", List.of()));
		assertFalse(obj(list(policyChecks.getFirst().get("list")).getFirst()).containsKey("scope"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void providerSideRejectsIncompleteRequestsWithoutAsking() {
		final FederationService service = gatekeeper("legacy-mtls", b -> decisions(true));
		assertFalse(service.authorized("", "ConsumerB", "OtherCloud|Company", "temperature", List.of()));
		assertTrue(policyChecks.isEmpty());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void trustedPeersAreConfiguredNeighbourGatekeepersOfTheExpectedOrganisation() {
		final FederationService service = gatekeeper("legacy-mtls", b -> decisions(true));
		when(db.clouds()).thenReturn(List.of(new Cloud("OtherCloud", "Company"), new Cloud("LabCloud", "Lab")));
		assertTrue(service.trustedPeer("Gatekeeper.OtherCloud.Company.arrowhead.eu"));
		assertFalse(service.trustedPeer("GateTunnel.OtherCloud.Company.arrowhead.eu"), "only the Gatekeeper");
		assertFalse(service.trustedPeer("Gatekeeper.LabCloud.Lab.arrowhead.eu"), "a foreign organisation needs an explicit binding");
		assertFalse(service.trustedPeer("Gatekeeper.TestCloud.Company.arrowhead.eu"), "not the own cloud");
		assertFalse(service.trustedPeer("Gatekeeper.UnknownCloud.Company.arrowhead.eu"), "not configured");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void gsdQueriesSameOrganisationNeighboursAndTagsResults() throws Exception {
		final FederationService service = gatekeeper("legacy-mtls", b -> decisions(true));
		when(db.clouds()).thenReturn(List.of(new Cloud("OtherCloud", "Company"), new Cloud("BadCloud", "Company"), new Cloud("LabCloud", "Lab")));
		when(relay.request(eq("OtherCloud"), anyMap())).thenReturn(m("cloud", "OtherCloud|Company", "results", List.of(m("providerName", "P"))));
		when(relay.request(eq("BadCloud"), anyMap())).thenReturn(m("cloud", "Impostor|Company", "results", List.of(m("providerName", "X"))));

		final Map<String, Object> result = service.gsd(m("serviceRequirement", m("serviceDefinition", "temperature")), "Consumer");
		assertEquals(2, result.get("queriedClouds"), "LabCloud belongs to another organisation");
		assertEquals(1, result.get("unsuccessfulRequests"), "a peer identity mismatch is a failure");
		assertEquals(List.of(m("providerName", "P", "cloud", "OtherCloud|Company")), result.get("results"));

		final ArgumentCaptor<Map<String, Object>> message = mapCaptor();
		verify(relay).request(eq("OtherCloud"), message.capture());
		assertEquals(m("type", "gsd-request", "consumer", "Consumer", "serviceRequirement", m("serviceDefinition", "temperature")), message.getValue());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void gsdNeedsAServiceDefinition() {
		assertThrows(IllegalArgumentException.class, () -> gatekeeper("legacy-mtls", b -> decisions(true)).gsd(m(), "Consumer"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void icnRejectsAnotherOrganisationThanConfigured() {
		final ApiError error = assertThrows(ApiError.class,
				() -> gatekeeper("legacy-mtls", b -> decisions(true)).icn(m("targetCloud", "OtherCloud|Lab", "providerName", "P", "serviceDefinition", "temperature"), "Consumer"));
		assertEquals(400, error.getStatus());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void icnNeverDowngradesTheTransport() throws Exception {
		when(relay.request(eq("OtherCloud"), anyMap())).thenReturn(m("accepted", true, "keyExchange", m("k", "v")));
		assertEquals(502, assertThrows(ApiError.class, () -> gatekeeper("legacy-mtls", b -> decisions(true)).icn(icnRequest(), "Consumer")).getStatus());

		when(relay.request(eq("OtherCloud"), anyMap())).thenReturn(m("accepted", true));
		assertEquals(502, assertThrows(ApiError.class, () -> gatekeeper("ah-gt-1", b -> decisions(true)).icn(icnRequest(), "Consumer")).getStatus());
		verify(consumer, never()).consume(eq("gateTunnelConnection"), any(), any(), any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void icnOpensTheConsumerSessionAndReturnsTheTunnelInterface() throws Exception {
		final FederationService service = gatekeeper("ah-gt-1", b -> decisions(true));
		when(relay.request(eq("OtherCloud"), anyMap())).thenReturn(m("accepted", true, "sessionId", "s1", "providerName", "P", "serviceDefinition", "temperature",
				"version", "1.0.0", "metadata", m(), "basePath", "/t", "operations", m("read", m("method", "GET", "path", "/read")), "keyExchange", m("k", "v")));
		when(consumer.consume(eq("gateTunnelConnection"), eq("open-consumer-session"), isNull(), any())).thenReturn(m("address", "gatetunnel", "port", 8000));

		final Map<String, Object> result = service.icn(icnRequest(), "Consumer");
		assertEquals(true, result.get("negotiated"));
		assertEquals("OtherCloud|Company", result.get("providerCloud"));
		assertEquals(m("templateName", "generic_https", "protocol", "https", "policy", "CERT_AUTH", "properties",
				m("accessAddresses", List.of("gatetunnel"), "accessPort", 8000, "basePath", "/t", "operations", m("read", m("method", "GET", "path", "/read")))), result.get("interface"));

		final ArgumentCaptor<Object> session = ArgumentCaptor.forClass(Object.class);
		verify(consumer).consume(eq("gateTunnelConnection"), eq("open-consumer-session"), isNull(), session.capture());
		assertEquals("GateTunnel.OtherCloud.Company.arrowhead.eu", obj(session.getValue()).get("peer"));
		assertEquals(m("k", "v"), obj(session.getValue()).get("keyExchange"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void incomingIcnNegotiatesTheOfferedOperationsAndOpensTheProviderSession() throws Exception {
		final FederationService service = gatekeeper("ah-gt-1", b -> decisions(true));
		when(consumer.lookup(any())).thenReturn(m("entries", List.of(instance("P"))));
		when(consumer.consume(eq("gateTunnelConnection"), eq("open-provider-session"), isNull(), any())).thenReturn(m("sessionId", "s1", "keyExchange", m("k", "v")));

		final Map<String, Object> reply = service.incoming("Gatekeeper.OtherCloud.Company.arrowhead.eu",
				m("type", "icn-request", "consumer", "C", "providerName", "P", "serviceDefinition", "temperature", "operations", List.of("read")));
		assertEquals(true, reply.get("accepted"));
		assertEquals("s1", reply.get("sessionId"));
		assertEquals(m("read", m("method", "GET", "path", "/read")), reply.get("operations"));
		assertEquals(m("k", "v"), reply.get("keyExchange"));
		final Map<String, Object> check = obj(list(policyChecks.getFirst().get("list")).getFirst());
		assertEquals("OtherCloud|Company", check.get("cloud"));

		final ArgumentCaptor<Object> session = ArgumentCaptor.forClass(Object.class);
		verify(consumer).consume(eq("gateTunnelConnection"), eq("open-provider-session"), isNull(), session.capture());
		assertEquals("GateTunnel.OtherCloud.Company.arrowhead.eu", obj(session.getValue()).get("peer"));
		assertEquals(m("address", "10.0.0.5", "port", 9000, "basePath", "/t", "operations", m("read", m("method", "GET", "path", "/read"))), obj(session.getValue()).get("target"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void incomingRequestsAreRefusedWithTheNativeReasons() throws Exception {
		when(consumer.lookup(any())).thenReturn(m("entries", List.of(instance("P"))));
		final String peer = "Gatekeeper.OtherCloud.Company.arrowhead.eu";

		assertEquals("Provider not found", gatekeeper("ah-gt-1", b -> decisions(true)).incoming(peer,
				m("type", "icn-request", "consumer", "C", "providerName", "Q", "serviceDefinition", "temperature")).get("reason"));
		assertEquals("Operation not offered", gatekeeper("ah-gt-1", b -> decisions(true)).incoming(peer,
				m("type", "icn-request", "consumer", "C", "providerName", "P", "serviceDefinition", "temperature", "operations", List.of("write"))).get("reason"));
		assertEquals("Not authorized by provider cloud", gatekeeper("ah-gt-1", b -> decisions(false)).incoming(peer,
				m("type", "icn-request", "consumer", "C", "providerName", "P", "serviceDefinition", "temperature")).get("reason"));
		assertThrows(ApiError.class, () -> gatekeeper("ah-gt-1", b -> decisions(true)).incoming(peer, m("type", "other")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void incomingGsdReturnsOnlyAuthorizedProviders() throws Exception {
		when(consumer.lookup(any())).thenReturn(m("entries", List.of(instance("P"))));
		final Map<String, Object> reply = gatekeeper("ah-gt-1", b -> decisions(true)).incoming("Gatekeeper.OtherCloud.Company.arrowhead.eu",
				m("type", "gsd-request", "consumer", "C", "serviceRequirement", m("serviceDefinition", "temperature")));
		assertEquals("TestCloud|Company", reply.get("cloud"));
		assertEquals(1, list(reply.get("results")).size());
		assertEquals(java.util.Set.of("read"), obj(list(reply.get("results")).getFirst()).get("operations"));

		assertEquals(List.of(), gatekeeper("ah-gt-1", b -> decisions(false)).incoming("Gatekeeper.OtherCloud.Company.arrowhead.eu",
				m("type", "gsd-request", "consumer", "C", "serviceRequirement", m("serviceDefinition", "temperature"))).get("results"));
	}

	//-------------------------------------------------------------------------------------------------
	// SOS-7 (2026-11-10): consumers reach neighbours of another organisation only when this cloud's policy lists them
	@Test
	public void gsdSkipsNeighboursOfAnotherOrganisationUnlessListed() throws Exception {
		when(db.clouds()).thenReturn(List.of(new Cloud("OtherCloud", "Company"), new Cloud("LabCloud", "Lab")));
		when(relay.request(eq("OtherCloud"), anyMap())).thenReturn(m("cloud", "OtherCloud|Company", "results", List.of(m("providerName", "P"))));
		when(relay.request(eq("LabCloud"), anyMap())).thenReturn(m("cloud", "LabCloud|Lab", "results", List.of(m("providerName", "L"))));

		final FederationService ownOnly = gatekeeper("legacy-mtls", "LabCloud=Lab", "");
		Map<String, Object> result = ownOnly.gsd(m("serviceRequirement", m("serviceDefinition", "temperature")), "Consumer");
		assertEquals(1, result.get("queriedClouds"));
		assertEquals(List.of(m("providerName", "P", "cloud", "OtherCloud|Company")), result.get("results"));
		// naming the foreign cloud does not get around the policy
		result = ownOnly.gsd(m("serviceRequirement", m("serviceDefinition", "temperature"), "targetClouds", List.of("LabCloud")), "Consumer");
		assertEquals(List.of("LabCloud|Lab"), result.get("refusedClouds"));
		assertEquals(List.of(), result.get("results"));
		verify(relay, never()).request(eq("LabCloud"), anyMap());

		result = gatekeeper("legacy-mtls", "LabCloud=Lab", "LabCloud|Lab").gsd(m("serviceRequirement", m("serviceDefinition", "temperature")), "Consumer");
		assertEquals(2, result.get("queriedClouds"));
		assertEquals(2, list(result.get("results")).size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void icnRefusesANeighbourOfAnotherOrganisationUnlessListed() throws Exception {
		final ApiError error = assertThrows(ApiError.class, () -> gatekeeper("legacy-mtls", "LabCloud=Lab", "")
				.icn(m("targetCloud", "LabCloud", "providerName", "P", "serviceDefinition", "temperature"), "Consumer"));
		assertEquals(403, error.getStatus());
		verify(relay, never()).request(eq("LabCloud"), anyMap());

		when(relay.request(eq("LabCloud"), anyMap())).thenReturn(m("accepted", false, "reason", "no provider"));
		assertEquals(false, gatekeeper("legacy-mtls", "LabCloud=Lab", "LabCloud|Lab")
				.icn(m("targetCloud", "LabCloud", "providerName", "P", "serviceDefinition", "temperature"), "Consumer").get("negotiated"));
		verify(relay).request(eq("LabCloud"), anyMap());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void foreignTargetsMustBeBoundNeighboursOfAnotherOrganisation() {
		for (final String bad : List.of("LabCloud", "LabCloud|Company", "LabCloud|Other", "TestCloud|Lab", "|Lab", "OtherCloud|Company")) {
			assertThrows(IllegalArgumentException.class, () -> new FederationSettings("TestCloud", "Company", "ah-gt-1", "LabCloud=Lab", bad), bad);
		}
		final FederationSettings settings = new FederationSettings("TestCloud", "Company", "ah-gt-1", "LabCloud=Lab", " LabCloud|Lab ");
		assertTrue(settings.targetAllowed("LabCloud"));
		assertTrue(settings.targetAllowed("OtherCloud"));
		assertFalse(new FederationSettings("TestCloud", "Company", "ah-gt-1", "LabCloud=Lab").targetAllowed("LabCloud"));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private FederationService gatekeeper(final String profile, final String peers, final String foreignTargets) {
		final FederationService service = gatekeeper(profile, b -> decisions(true));
		ReflectionTestUtils.setField(service, "settings", new FederationSettings("TestCloud", "Company", profile, peers, foreignTargets));
		return service;
	}

	//-------------------------------------------------------------------------------------------------
	private FederationService gatekeeper(final String profile, final Function<Map<String, Object>, Map<String, Object>> answer) {
		final FederationService service = new FederationService();
		ReflectionTestUtils.setField(service, "settings", new FederationSettings("TestCloud", "Company", profile, ""));
		ReflectionTestUtils.setField(service, "dbService", db);
		ReflectionTestUtils.setField(service, "consumer", consumer);
		ReflectionTestUtils.setField(service, "relay", relay);
		when(consumer.consume(eq("authorizationManagement"), eq("check-policies"), eq("ConsumerAuthorization"), any())).thenAnswer(invocation -> {
			final Map<String, Object> body = obj(invocation.getArgument(3));
			policyChecks.add(body);
			return answer.apply(body);
		});

		return service;
	}

	//-------------------------------------------------------------------------------------------------
	private static Map<String, Object> decisions(final boolean... granted) {
		final List<Object> entries = new ArrayList<>();
		for (final boolean g : granted) {
			entries.add(m("granted", g));
		}

		return m("entries", entries);
	}

	//-------------------------------------------------------------------------------------------------
	private static Map<String, Object> icnRequest() {
		return m("targetCloud", "OtherCloud", "providerName", "P", "serviceDefinition", "temperature");
	}

	//-------------------------------------------------------------------------------------------------
	private static Map<String, Object> instance(final String provider) {
		return m("provider", m("name", provider), "version", "1.0.0", "metadata", m(),
				"interfaces", List.of(m("templateName", "generic_https", "protocol", "https", "properties",
						m("accessAddresses", List.of("10.0.0.5"), "accessPort", 9000, "basePath", "/t", "operations", m("read", m("method", "GET", "path", "/read"))))));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static ArgumentCaptor<Map<String, Object>> mapCaptor() {
		return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
	}
}
