package eu.arrowhead.localcloudmanager.dockerproxy;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * The proxy forwards exactly what the console needs, for its Compose project only (unchanged from the native proxy.js):
 * <ul>
 * <li>GET /containers/json: the project's containers (the label filter is set here, whatever the client asked);</li>
 * <li>GET /containers/&lt;id&gt;/logs: logs of a project container (stdout, stderr, timestamps, tail only);</li>
 * <li>POST /containers/&lt;id&gt;/(start|stop|restart): lifecycle of a project container whose service is in MANAGED_SERVICES (t only).</li>
 * </ul>
 * Everything else (exec, create, images, volumes, networks, other projects, ...) is refused with 403.
 */
public class Allowlist {

	private static final Pattern VERSION = Pattern.compile("^/v\\d+\\.\\d+(?=/)");
	private static final Pattern CONTAINER_OPERATION = Pattern.compile("^/containers/([^/]+)/(logs|start|stop|restart)$");
	private static final Pattern CONTAINER_ID = Pattern.compile("^[a-f0-9]{12,64}$");
	private static final String NOT_ALLOWED = "operation not allowed by the console proxy";

	private final String project;
	private final Set<String> managedServices;

	public Allowlist(final String project, final String managedServices) {
		if (project == null || project.isBlank()) {
			throw new IllegalArgumentException("COMPOSE_PROJECT_NAME is required");
		}
		this.project = project.trim();
		this.managedServices = managedServices == null ? Set.of()
				: java.util.Arrays.stream(managedServices.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
	}

	/** labelsOf: the Compose labels of a container id, or null when Docker does not know it. */
	public Decision decide(final String method, final String rawPath, final String rawQuery, final Function<String, Map<String, String>> labelsOf) {
		final String path = VERSION.matcher(rawPath == null ? "" : rawPath).replaceFirst("");
		if ("GET".equals(method) && "/containers/json".equals(path)) {
			final String filters = URLEncoder.encode("{\"label\":[\"com.docker.compose.project=" + project + "\"]}", StandardCharsets.UTF_8).replace("+", "%20");
			return Decision.forward(method, "/containers/json?all=1&filters=" + filters);
		}

		final Matcher match = CONTAINER_OPERATION.matcher(path);
		if (!match.matches() || !CONTAINER_ID.matcher(match.group(1)).matches()) {
			return Decision.refuse(403, NOT_ALLOWED);
		}
		final String id = match.group(1);
		final String operation = match.group(2);
		if ("logs".equals(operation) != "GET".equals(method) || !"logs".equals(operation) && !"POST".equals(method)) {
			return Decision.refuse(403, NOT_ALLOWED);
		}

		final Map<String, String> labels = labelsOf.apply(id);
		if (labels == null || !project.equals(labels.get("com.docker.compose.project"))) {
			return Decision.refuse(403, "container is not part of this local cloud");
		}

		final Map<String, String> query = parse(rawQuery);
		if ("logs".equals(operation)) {
			final Map<String, Pattern> allowed = new LinkedHashMap<>();
			allowed.put("stdout", Pattern.compile("^[01]$"));
			allowed.put("stderr", Pattern.compile("^[01]$"));
			allowed.put("timestamps", Pattern.compile("^[01]$"));
			allowed.put("tail", Pattern.compile("^\\d{1,4}$"));
			return Decision.forward(method, "/containers/" + id + "/logs?" + pick(query, allowed));
		}

		if (!managedServices.contains(labels.get("com.docker.compose.service"))) {
			return Decision.refuse(403, "service is not in the management allowlist");
		}
		final String picked = pick(query, Map.of("t", Pattern.compile("^\\d{1,3}$")));
		return Decision.forward(method, "/containers/" + id + "/" + operation + (picked.isEmpty() ? "" : "?" + picked));
	}

	private static Map<String, String> parse(final String query) {
		final Map<String, String> out = new LinkedHashMap<>();
		if (query == null || query.isEmpty()) {
			return out;
		}
		for (final String pair : query.split("&")) {
			final int eq = pair.indexOf('=');
			final String name = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
			final String value = eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
			out.putIfAbsent(name, value);
		}
		return out;
	}

	private static String pick(final Map<String, String> query, final Map<String, Pattern> allowed) {
		return allowed.entrySet().stream()
				.filter(e -> query.containsKey(e.getKey()) && e.getValue().matcher(query.get(e.getKey())).matches())
				.map(e -> e.getKey() + "=" + query.get(e.getKey()))
				.collect(Collectors.joining("&"));
	}
}
