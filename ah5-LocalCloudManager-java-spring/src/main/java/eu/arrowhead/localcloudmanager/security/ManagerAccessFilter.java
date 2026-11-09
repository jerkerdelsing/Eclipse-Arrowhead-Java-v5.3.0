package eu.arrowhead.localcloudmanager.security;

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
import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;
import eu.arrowhead.localcloudmanager.LocalCloudManagerSystemInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Arrowhead connector only (console requests skip the Arrowhead filters, see ConsoleFilter). After the authentication filter, a
// localCloudManagement request needs Sysop, a system of lcm.api.whitelist, or a ConsumerAuthorization grant of its operation;
// /api paths that are no registered operation are refused.
@Component
@Order(LocalCloudManagerConstants.REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION)
public class ManagerAccessFilter extends ArrowheadFilter {

	@Autowired
	private ManagerAuthorizationService authorizationService;

	@Value(LocalCloudManagerConstants.$SERVICE_AUTHORIZATION_ENABLED_WD)
	private boolean authorizationEnabled;

	private final Set<String> whitelist;

	public ManagerAccessFilter(@Value(LocalCloudManagerConstants.$API_WHITELIST_WD) final String whitelist) {
		this.whitelist = Arrays.stream(whitelist.split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toUnmodifiableSet());
	}

	// the operation of a request (registered path, path values appended); "" for any other /api path; null outside /api
	static String operation(final String method, final String path) {
		if (!path.equals(LocalCloudManagerConstants.HTTP_API_BASE_PATH) && !path.startsWith(LocalCloudManagerConstants.HTTP_API_BASE_PATH + "/")) {
			return null;
		}
		final String rest = path.substring(LocalCloudManagerConstants.HTTP_API_BASE_PATH.length());
		String best = "";
		int length = -1;
		for (final String[] op : LocalCloudManagerSystemInfo.OPERATIONS) {
			if (op[1].equals(method) && (rest.equals(op[2]) || rest.startsWith(op[2] + "/")) && op[2].length() > length) {
				best = op[0];
				length = op[2].length();
			}
		}
		return best;
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
		if (operation.isEmpty()) {
			throw new ForbiddenException(consumer + " is not authorized for " + LocalCloudManagerConstants.SERVICE_DEF_LOCAL_CLOUD_MANAGEMENT, origin);
		}
		if (!authorizationEnabled) {
			return;
		}
		if (!authorizationService.isAuthorized(consumer, LocalCloudManagerConstants.SERVICE_DEF_LOCAL_CLOUD_MANAGEMENT, operation)) {
			throw new ForbiddenException(consumer + " is not authorized for " + LocalCloudManagerConstants.SERVICE_DEF_LOCAL_CLOUD_MANAGEMENT + "." + operation, origin);
		}
	}
}
