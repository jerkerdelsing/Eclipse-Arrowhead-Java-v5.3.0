package eu.arrowhead.localcloudmanager.dockerproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// The HTTP behaviour of the proxy with a stub Docker socket: only allowed requests reach Docker, everything else is 403.
public class ProxyControllerTest {

	private static final String ID = "b".repeat(64);
	private final List<String> forwarded = new ArrayList<>();
	private MockMvc mvc;

	@BeforeEach
	public void setUp() {
		final DockerSocket docker = new DockerSocket(Path.of("/nonexistent")) {
			@Override
			public Response request(final String method, final String path) {
				if (path.endsWith("/json") && path.startsWith("/containers/") && !path.startsWith("/containers/json")) {
					final String service = path.contains("c".repeat(12)) ? "authentication" : "sysmonitor";
					return new Response(200, "application/json",
							("{\"Config\":{\"Labels\":{\"com.docker.compose.project\":\"cloud-a\",\"com.docker.compose.service\":\"" + service + "\"}}}").getBytes(StandardCharsets.UTF_8));
				}
				forwarded.add(method + " " + path);
				return new Response(200, "application/json", "[]".getBytes(StandardCharsets.UTF_8));
			}
		};
		mvc = MockMvcBuilders.standaloneSetup(new ProxyController(new Allowlist("cloud-a", "sysmonitor"), docker)).build();
	}

	@Test
	public void allowedRequestsAreForwarded() throws Exception {
		mvc.perform(request(HttpMethod.GET, "/containers/json?all=1")).andExpect(status().isOk()).andExpect(content().string("[]"));
		mvc.perform(request(HttpMethod.GET, "/containers/" + ID + "/logs?tail=10&follow=1")).andExpect(status().isOk());
		mvc.perform(request(HttpMethod.POST, "/v1.47/containers/" + ID + "/restart?t=20")).andExpect(status().isOk());
		assertEquals(3, forwarded.size());
		assertTrue(forwarded.get(0).startsWith("GET /containers/json?all=1&filters="));
		assertEquals("GET /containers/" + ID + "/logs?tail=10", forwarded.get(1));
		assertEquals("POST /containers/" + ID + "/restart?t=20", forwarded.get(2));
	}

	@Test
	public void everythingElseIsRefusedWithoutReachingDocker() throws Exception {
		for (final Object[] r : List.of(new Object[] { HttpMethod.POST, "/containers/" + ID + "/exec" }, new Object[] { HttpMethod.POST, "/containers/create" },
				new Object[] { HttpMethod.GET, "/images/json" }, new Object[] { HttpMethod.DELETE, "/containers/" + ID }, new Object[] { HttpMethod.PUT, "/containers/json" },
				new Object[] { HttpMethod.POST, "/containers/" + "c".repeat(64) + "/stop" }, new Object[] { HttpMethod.GET, "/_ping" },
				new Object[] { HttpMethod.PATCH, "/containers/json" })) {
			mvc.perform(request((HttpMethod) r[0], (String) r[1])).andExpect(status().isForbidden());
		}
		assertEquals(List.of(), forwarded);
	}
}
