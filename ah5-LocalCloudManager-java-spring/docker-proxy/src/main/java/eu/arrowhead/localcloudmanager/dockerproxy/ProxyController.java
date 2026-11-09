package eu.arrowhead.localcloudmanager.dockerproxy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

/** Every request is decided by the allowlist; only allowed ones are forwarded to the Docker socket. */
@RestController
public class ProxyController {

	private final Log log = LogFactory.getLog(getClass());
	private final ObjectMapper mapper = new ObjectMapper();
	private final Allowlist allowlist;
	private final DockerSocket docker;

	// the injection constructor: with a second (test) constructor Spring needs it marked, or start-up fails
	@Autowired
	public ProxyController(@Value("${docker.project:}") final String project, @Value("${docker.managed-services:}") final String managed,
			@Value("${docker.socket:/var/run/docker.sock}") final String socket) {
		this(new Allowlist(project, managed), new DockerSocket(Path.of(socket)));
	}

	public ProxyController(final Allowlist allowlist, final DockerSocket docker) {
		this.allowlist = allowlist;
		this.docker = docker;
	}

	@RequestMapping("/**")
	public ResponseEntity<byte[]> proxy(final HttpServletRequest request) {
		try {
			final Decision decision = allowlist.decide(request.getMethod(), request.getRequestURI(), request.getQueryString(), this::labels);
			if (decision.refused()) {
				log.warn("refused " + request.getMethod() + " " + request.getRequestURI() + ": " + decision.error());
				return refuse(decision.status(), decision.error());
			}
			if ("POST".equals(decision.method())) {
				log.info(decision.method() + " " + decision.path());
			}
			final DockerSocket.Response response = docker.request(decision.method(), decision.path());
			return ResponseEntity.status(response.status()).contentType(MediaType.parseMediaType(response.contentType())).body(response.body());
		} catch (final Exception ex) {
			return refuse(502, String.valueOf(ex.getMessage()));
		}
	}

	private Map<String, String> labels(final String id) {
		try {
			final DockerSocket.Response response = docker.request("GET", "/containers/" + id + "/json");
			if (response.status() != 200) {
				return null;
			}
			final JsonNode labels = mapper.readTree(response.body()).path("Config").path("Labels");
			final Map<String, String> out = new LinkedHashMap<>();
			labels.fields().forEachRemaining(e -> out.put(e.getKey(), e.getValue().asText()));
			return out;
		} catch (final IOException ex) {
			return null;
		}
	}

	private ResponseEntity<byte[]> refuse(final int status, final String error) {
		try {
			return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(mapper.writeValueAsBytes(Map.of("message", error)));
		} catch (final IOException ex) {
			return ResponseEntity.status(status).build();
		}
	}
}
