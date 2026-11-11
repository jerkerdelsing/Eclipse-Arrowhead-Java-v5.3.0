package eu.arrowhead.solocalcloudconfigurator.api.http;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import eu.arrowhead.solocalcloudconfigurator.SoLocalCloudConfiguratorConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Response headers of the native API on /api, 
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ResponseHeaderFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
		final String path = request.getRequestURI();
		if (path.equals(SoLocalCloudConfiguratorConstants.HTTP_API_BASE_PATH) || path.startsWith(SoLocalCloudConfiguratorConstants.HTTP_API_BASE_PATH + "/")) {
			response.setHeader("X-Content-Type-Options", "nosniff");
			response.setHeader("Cache-Control", "no-store");
			response.setHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
		}

		chain.doFilter(request, response);
	}
}
