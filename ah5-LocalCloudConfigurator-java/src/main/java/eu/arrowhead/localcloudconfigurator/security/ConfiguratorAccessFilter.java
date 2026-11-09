package eu.arrowhead.localcloudconfigurator.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.localcloudconfigurator.LocalCloudConfiguratorConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs after the authentication filter. A localCloudConfiguration operation changes the runtime of the cloud, so only Sysop, the
// systems of lcc.api.whitelist and systems with a ConsumerAuthorization grant of the operation (LocalCloudManager) may call it.
@Component
@Order(LocalCloudConfiguratorConstants.REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION)
public class ConfiguratorAccessFilter extends ArrowheadFilter {

	@Autowired
	private ConfiguratorAuthorizationService authorizationService;

	@Value(LocalCloudConfiguratorConstants.$SERVICE_AUTHORIZATION_ENABLED_WD)
	private boolean authorizationEnabled;

	private final Set<String> whitelist;

	public ConfiguratorAccessFilter(@Value(LocalCloudConfiguratorConstants.$API_WHITELIST_WD) final String whitelist) {
		this.whitelist = Arrays.stream(whitelist.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
	}

	// the localCloudConfiguration operation of a request; "" for any other /api path (refused like an operation); null outside /api
	static String operation(final String method, final String path) {
		final String base = LocalCloudConfiguratorConstants.HTTP_API_BASE_PATH;
		if (!path.equals(base) && !path.startsWith(base + "/")) {
			return null;
		}

		final String rest = path.substring(base.length());
		final boolean get = "GET".equals(method);
		final boolean post = "POST".equals(method);
		if (get && LocalCloudConfiguratorConstants.HTTP_API_OP_CATALOG_PATH.equals(rest)) {
			return LocalCloudConfiguratorConstants.SERVICE_OP_CATALOG;
		}
		if (get && LocalCloudConfiguratorConstants.HTTP_API_OP_STATUS_PATH.equals(rest)) {
			return LocalCloudConfiguratorConstants.SERVICE_OP_STATUS;
		}
		if (post && LocalCloudConfiguratorConstants.HTTP_API_OP_PLAN_PATH.equals(rest)) {
			return LocalCloudConfiguratorConstants.SERVICE_OP_PLAN;
		}
		if (post && LocalCloudConfiguratorConstants.HTTP_API_OP_DEPLOYMENTS_PATH.equals(rest)) {
			return LocalCloudConfiguratorConstants.SERVICE_OP_SUBMIT_DEPLOYMENT;
		}
		if (get && rest.matches("/deployments/" + LocalCloudConfiguratorConstants.DEPLOYMENT_ID_PATTERN)) {
			return LocalCloudConfiguratorConstants.SERVICE_OP_GET_DEPLOYMENT;
		}
		if (LocalCloudConfiguratorConstants.HTTP_API_OP_CONFIGURATIONS_PATH.equals(rest)) {
			return get ? LocalCloudConfiguratorConstants.SERVICE_OP_LIST_CONFIGURATIONS : post ? LocalCloudConfiguratorConstants.SERVICE_OP_CREATE_CONFIGURATION : "";
		}
		if (rest.matches("/configurations/" + LocalCloudConfiguratorConstants.CONFIGURATION_ID_PATTERN)) {
			return switch (method) {
			case "GET" -> LocalCloudConfiguratorConstants.SERVICE_OP_GET_CONFIGURATION;
			case "PUT" -> LocalCloudConfiguratorConstants.SERVICE_OP_UPDATE_CONFIGURATION;
			case "DELETE" -> LocalCloudConfiguratorConstants.SERVICE_OP_REMOVE_CONFIGURATION;
			default -> "";
			};
		}

		return "";
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			final String operation = operation(request.getMethod(), request.getRequestURI());
			if (operation != null) {
				check(request, operation);
			}

			chain.doFilter(request, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	private void check(final HttpServletRequest request, final String operation) {
		final String origin = request.getMethod() + " " + request.getRequestURI();
		final Object system = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (system == null || Constants.UNKNOWN.equals(system)) {
			throw new AuthException("No authenticated Arrowhead system", origin);
		}

		final String consumer = system.toString();
		if (HttpUtilities.isSysop(request, origin) || whitelist.contains(consumer)) {
			return;
		}

		if (!authorizationEnabled) {
			// development profile only: the security profile guard refuses service.authorization.enabled=false otherwise
			return;
		}

		if (operation.isEmpty() || !authorizationService.isAuthorized(consumer, LocalCloudConfiguratorConstants.SERVICE_DEF_LOCAL_CLOUD_CONFIGURATION, operation)) {
			throw new ForbiddenException(consumer + " is not authorized for " + LocalCloudConfiguratorConstants.SERVICE_DEF_LOCAL_CLOUD_CONFIGURATION
					+ (operation.isEmpty() ? "" : "." + operation), origin);
		}
	}
}
