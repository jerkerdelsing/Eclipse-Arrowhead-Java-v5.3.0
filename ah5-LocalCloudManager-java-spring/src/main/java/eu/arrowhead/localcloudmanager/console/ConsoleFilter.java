package eu.arrowhead.localcloudmanager.console;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Separates the two connectors (replaces the console's nginx).
 * <p>
 * Console connector (8490, plain HTTP, publish on host loopback only): the static UI (ui/public), /health, the session and the
 * /api routes. Host and Origin must be allowed local names (DNS rebinding); with a proxy secret, /api requests must carry it; without a
 * console password /api answers 503. Such requests skip the Arrowhead filter chain (identity token, management and authorization
 * filters) because the operator authenticates with the console password, which Spring Security checks (ConsoleSecurityConfig).
 * <p>
 * Arrowhead connector (8491, mutual TLS): only /api (localCloudManagement, through the common filters and the access filter),
 * generalManagement and the API documentation; the console-only paths do not exist there.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ConsoleFilter extends OncePerRequestFilter {

	private static final Map<String, String> TYPES = Map.of(
			"html", "text/html; charset=utf-8",
			"js", "text/javascript; charset=utf-8",
			"css", "text/css; charset=utf-8",
			"json", "application/json",
			"svg", "image/svg+xml",
			"png", "image/png",
			"ico", "image/x-icon");

	private static final Set<String> CONSOLE_ONLY = Set.of(LocalCloudManagerConstants.HTTP_HEALTH_PATH, LocalCloudManagerConstants.HTTP_API_SESSION_PATH);

	private final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private ConsoleAuth auth;

	@Autowired
	private ConsoleClients clients;

	@Autowired
	private ApplicationContext context;

	@Value(LocalCloudManagerConstants.$CONSOLE_PORT_WD)
	private int consolePort;

	@Value(LocalCloudManagerConstants.$CHIPLET_MANAGER_WD)
	private boolean chipletManager;

	private volatile Set<String> arrowheadFilterNames;

	public static boolean isApi(final String path) {
		return path.equals(LocalCloudManagerConstants.HTTP_API_BASE_PATH) || path.startsWith(LocalCloudManagerConstants.HTTP_API_BASE_PATH + "/");
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
		final String path = request.getRequestURI();
		if (request.getLocalPort() != consolePort) {
			final boolean arrowhead = isApi(path) && !CONSOLE_ONLY.contains(path)
					|| path.startsWith(LocalCloudManagerConstants.HTTP_API_GENERAL_MANAGEMENT_PATH + "/")
					|| path.equals("/") || path.startsWith(Constants.SWAGGER_API_DOCS_URI) || path.startsWith(Constants.SWAGGER_UI_URI);
			if (!arrowhead) {
				error(response, HttpServletResponse.SC_NOT_FOUND, "Not found");
				return;
			}

			chain.doFilter(request, response);
			return;
		}

		// J6: only the host's published port (Docker gateway), loopback and configured clients; not other systems of the cloud
		if (!clients.allowed(request.getRemoteAddr())) {
			error(response, HttpServletResponse.SC_FORBIDDEN, "The console is not available to this client");
			return;
		}

		if (!LocalCloudManagerConstants.HTTP_HEALTH_PATH.equals(path)) {
			// J6: with a proxy secret, only requests forwarded by the console's own front end
			if (isApi(path) && !auth.fromProxy(request)) {
				error(response, HttpServletResponse.SC_FORBIDDEN, "Not forwarded by the console");
				return;
			}

			// H1: only the local console names are accepted (DNS rebinding)
			if (!auth.originAllowed(request.getHeader("Host"), request.getHeader("Origin"))) {
				error(response, HttpServletResponse.SC_FORBIDDEN, "Host or Origin is not an allowed console address");
				return;
			}

			if (!isApi(path)) {
				serveUi(request, response, path);
				return;
			}

			if (!auth.configured()) {
				error(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Console password is not configured (CONSOLE_PASSWORD_HASH)");
				return;
			}
		}

		for (final String name : arrowheadFilterNames()) {
			request.setAttribute(name + OncePerRequestFilter.ALREADY_FILTERED_SUFFIX, Boolean.TRUE);
		}

		chain.doFilter(request, response);
	}

	// the static console: the file, or index.html for any other path (single-page application)
	private void serveUi(final HttpServletRequest request, final HttpServletResponse response, final String path) throws IOException {
		if (!"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod())) {
			error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
			return;
		}

		String name = path.substring(1);
		if (name.isEmpty() || name.endsWith("/") || name.contains("..") || name.contains("\\") || name.contains("//") || !name.matches("[A-Za-z0-9._/-]+")) {
			name = "index.html";
		}

		byte[] body = read(name);
		if (body == null) {
			name = "index.html";
			body = read(name);
		}

		if (body == null) {
			error(response, HttpServletResponse.SC_NOT_FOUND, "Not found");
			return;
		}

		if ("index.html".equals(name) && chipletManager) {
			body = new String(body, StandardCharsets.UTF_8).replace("data-chiplet-manager=\"false\"", "data-chiplet-manager=\"true\"").getBytes(StandardCharsets.UTF_8);
		}

		final String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		securityHeaders(response);
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(TYPES.getOrDefault(extension, "application/octet-stream"));
		response.setContentLength(body.length);
		if ("GET".equals(request.getMethod())) {
			response.getOutputStream().write(body);
		}
	}

	private static byte[] read(final String name) throws IOException {
		try (InputStream in = ConsoleFilter.class.getResourceAsStream("/console/" + name)) {
			return in == null ? null : in.readAllBytes();
		}
	}

	// the headers of the native console: no framing, no sniffing, no referrer
	static void securityHeaders(final HttpServletResponse response) {
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Content-Security-Policy", "frame-ancestors 'none'");
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("Referrer-Policy", "no-referrer");
	}

	private Set<String> arrowheadFilterNames() {
		Set<String> names = arrowheadFilterNames;
		if (names == null) {
			names = Set.copyOf(context.getBeansOfType(ArrowheadFilter.class).keySet());
			arrowheadFilterNames = names;
		}

		return names;
	}

	private void error(final HttpServletResponse response, final int status, final String message) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("X-Content-Type-Options", "nosniff");
		response.getOutputStream().write(mapper.writeValueAsBytes(Map.of("error", message)));
	}
}
