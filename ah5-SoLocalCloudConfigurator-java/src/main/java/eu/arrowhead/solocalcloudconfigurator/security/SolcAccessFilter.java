package eu.arrowhead.solocalcloudconfigurator.security;

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
import eu.arrowhead.solocalcloudconfigurator.SoLocalCloudConfiguratorConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Runs after the authentication filter. soLocalCloudConfiguration designs Systems of LocalClouds and builds their deployment
// packages, so only Sysop, the systems of solc.api.whitelist and systems with a ConsumerAuthorization grant of the operation
// (LocalCloudManager) may call it.
@Component
@Order(SoLocalCloudConfiguratorConstants.REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION)
public class SolcAccessFilter extends ArrowheadFilter {

	@Autowired
	private SolcAuthorizationService authorizationService;

	@Value(SoLocalCloudConfiguratorConstants.$SERVICE_AUTHORIZATION_ENABLED_WD)
	private boolean authorizationEnabled;

	private final Set<String> whitelist;

	public SolcAccessFilter(@Value(SoLocalCloudConfiguratorConstants.$API_WHITELIST_WD) final String whitelist) {
		this.whitelist = Arrays.stream(whitelist.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
	}

	// the soLocalCloudConfiguration operation of a request; "" for any other /api path (refused like an operation); null outside /api
	static String operation(final String method, final String path) {
		final String base = SoLocalCloudConfiguratorConstants.HTTP_API_BASE_PATH;
		if (!path.equals(base) && !path.startsWith(base + "/")) {
			return null;
		}

		final String rest = path.substring(base.length());
		final boolean get = "GET".equals(method);
		final boolean post = "POST".equals(method);
		if (get && SoLocalCloudConfiguratorConstants.HTTP_API_OP_CATALOG_PATH.equals(rest)) {
			return SoLocalCloudConfiguratorConstants.SERVICE_OP_CATALOG;
		}
		if (get && SoLocalCloudConfiguratorConstants.HTTP_API_OP_LC_DESIGNS_PATH.equals(rest)) {
			return SoLocalCloudConfiguratorConstants.SERVICE_OP_LIST_LC_DESIGNS;
		}
		if (post && SoLocalCloudConfiguratorConstants.HTTP_API_OP_PLAN_PATH.equals(rest)) {
			return SoLocalCloudConfiguratorConstants.SERVICE_OP_PLAN;
		}
		if (SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGNS_PATH.equals(rest)) {
			return get ? SoLocalCloudConfiguratorConstants.SERVICE_OP_LIST_DESIGNS : post ? SoLocalCloudConfiguratorConstants.SERVICE_OP_CREATE_DESIGN : "";
		}
		if (rest.matches("/designs/" + SoLocalCloudConfiguratorConstants.ID_PATTERN)) {
			return switch (method) {
			case "GET" -> SoLocalCloudConfiguratorConstants.SERVICE_OP_GET_DESIGN;
			case "PUT" -> SoLocalCloudConfiguratorConstants.SERVICE_OP_UPDATE_DESIGN;
			case "DELETE" -> SoLocalCloudConfiguratorConstants.SERVICE_OP_REMOVE_DESIGN;
			default -> "";
			};
		}
		if (SoLocalCloudConfiguratorConstants.HTTP_API_OP_PACKAGES_PATH.equals(rest)) {
			return get ? SoLocalCloudConfiguratorConstants.SERVICE_OP_LIST_PACKAGES : post ? SoLocalCloudConfiguratorConstants.SERVICE_OP_BUILD_PACKAGE : "";
		}
		if (rest.matches("/packages/" + SoLocalCloudConfiguratorConstants.ID_PATTERN)) {
			return switch (method) {
			case "GET" -> SoLocalCloudConfiguratorConstants.SERVICE_OP_GET_PACKAGE;
			case "DELETE" -> SoLocalCloudConfiguratorConstants.SERVICE_OP_REMOVE_PACKAGE;
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

		if (operation.isEmpty() || !authorizationService.isAuthorized(consumer, SoLocalCloudConfiguratorConstants.SERVICE_DEF_SO_LOCAL_CLOUD_CONFIGURATION, operation)) {
			throw new ForbiddenException(consumer + " is not authorized for " + SoLocalCloudConfiguratorConstants.SERVICE_DEF_SO_LOCAL_CLOUD_CONFIGURATION
					+ (operation.isEmpty() ? "" : "." + operation), origin);
		}
	}
}
