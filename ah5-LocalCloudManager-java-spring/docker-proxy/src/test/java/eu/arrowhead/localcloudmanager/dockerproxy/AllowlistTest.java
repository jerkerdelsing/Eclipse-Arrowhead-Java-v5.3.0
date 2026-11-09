package eu.arrowhead.localcloudmanager.dockerproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

// The proxy decisions of the native proxy.test.js, plus refusals of further methods, paths and containers.
public class AllowlistTest {

	private static final String ID = "a".repeat(64);
	private final Allowlist allowlist = new Allowlist("cloud-a", "sysmonitor,datamanager");

	private static Function<String, Map<String, String>> labels(final String project, final String service) {
		return id -> Map.of("com.docker.compose.project", project, "com.docker.compose.service", service);
	}

	private static String path(final String url) {
		final int q = url.indexOf('?');
		return q < 0 ? url : url.substring(0, q);
	}

	private static String query(final String url) {
		final int q = url.indexOf('?');
		return q < 0 ? null : url.substring(q + 1);
	}

	private Decision decide(final String method, final String url, final Function<String, Map<String, String>> labels) {
		return allowlist.decide(method, path(url), query(url), labels);
	}

	@Test
	public void containerListIsAlwaysFilteredToTheOwnProject() {
		final Decision d = decide("GET", "/v1.47/containers/json?all=1&filters=%7B%7D", id -> null);
		assertTrue(URLDecoder.decode(d.path(), StandardCharsets.UTF_8).contains("com.docker.compose.project=cloud-a"));
		assertTrue(d.path().startsWith("/containers/json?all=1&filters="));
	}

	@Test
	public void logsOfAnOwnContainerPassWithSanitizedQuery() {
		final Decision d = decide("GET", "/containers/" + ID + "/logs?stdout=1&tail=150&follow=1", labels("cloud-a", "authentication"));
		assertEquals("/containers/" + ID + "/logs?stdout=1&tail=150", d.path());
	}

	@Test
	public void lifecycleOnlyForManagedServicesOfTheOwnProject() {
		assertEquals("/containers/" + ID + "/restart?t=20", decide("POST", "/containers/" + ID + "/restart?t=20", labels("cloud-a", "sysmonitor")).path());
		assertEquals(403, decide("POST", "/containers/" + ID + "/stop", labels("cloud-a", "docker-proxy")).status());
		assertEquals(403, decide("POST", "/containers/" + ID + "/stop", labels("cloud-b", "sysmonitor")).status());
		assertEquals(403, decide("POST", "/containers/" + ID + "/stop", id -> null).status());
		assertEquals(403, decide("GET", "/containers/" + ID + "/logs", labels("cloud-b", "sysmonitor")).status());
	}

	@Test
	public void everythingElseIsRefused() {
		for (final String[] request : List.of(new String[] { "POST", "/containers/" + ID + "/exec" }, new String[] { "POST", "/containers/create" },
				new String[] { "GET", "/images/json" }, new String[] { "DELETE", "/containers/" + ID }, new String[] { "GET", "/containers/" + ID + "/start" },
				new String[] { "POST", "/containers/" + ID + "/logs" }, new String[] { "GET", "/containers/../images/json" },
				new String[] { "POST", "/containers/name-not-id/restart" }, new String[] { "POST", "/containers/json" }, new String[] { "PUT", "/containers/json" },
				new String[] { "DELETE", "/containers/json" }, new String[] { "GET", "/volumes" }, new String[] { "GET", "/networks" },
				new String[] { "POST", "/containers/" + ID + "/kill" }, new String[] { "GET", "/containers/" + ID + "/json" }, new String[] { "GET", "/info" },
				new String[] { "POST", "/build" }, new String[] { "GET", "/containers/" + ID + "/logs/../../images" })) {
			assertEquals(403, decide(request[0], request[1], labels("cloud-a", "sysmonitor")).status(), request[0] + " " + request[1]);
		}
	}

	@Test
	public void aProjectIsRequired() {
		assertThrows(IllegalArgumentException.class, () -> new Allowlist("", "sysmonitor"));
	}
}
