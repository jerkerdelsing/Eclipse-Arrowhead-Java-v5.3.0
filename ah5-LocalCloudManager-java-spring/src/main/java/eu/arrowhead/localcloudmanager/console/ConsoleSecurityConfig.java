package eu.arrowhead.localcloudmanager.console;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import eu.arrowhead.common.Constants;
import eu.arrowhead.localcloudmanager.LocalCloudManagerConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Spring Security for both connectors (replaces the common DefaultSecurityConfig).
 * <ul>
 * <li>Console connector: the console session cookie is the authentication (ROLE_OPERATOR); every /api route except /api/session needs
 * it (401 {"error":"Console login required"}). Stateless (no HTTP session); CSRF is covered by SameSite=Strict cookies, the Host/Origin
 * check and JSON-only login. Headers: no framing, no sniffing, no referrer.</li>
 * <li>Arrowhead connector: HTTPS only; authentication and authorization by the common Arrowhead filters.</li>
 * </ul>
 */
@Configuration
public class ConsoleSecurityConfig {

	public static final String ROLE_OPERATOR = "ROLE_OPERATOR";

	@Value(Constants.$SERVER_SSL_ENABLED_WD)
	private boolean sslEnabled;

	@Value(LocalCloudManagerConstants.$CONSOLE_PORT_WD)
	private int consolePort;

	@Bean
	@Order(1)
	SecurityFilterChain consoleChain(final HttpSecurity http, final ConsoleAuth auth) throws Exception {
		http.securityMatcher(request -> request.getLocalPort() == consolePort)
				.httpBasic(basic -> basic.disable())
				.formLogin(form -> form.disable())
				.logout(logout -> logout.disable())
				.csrf(csrf -> csrf.disable())
				.requestCache(cache -> cache.disable())
				.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(headers -> headers
						.frameOptions(frame -> frame.deny())
						.contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'none'"))
						.referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
				.addFilterBefore(new ConsoleSessionFilter(auth), AnonymousAuthenticationFilter.class)
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(request -> LocalCloudManagerConstants.HTTP_HEALTH_PATH.equals(request.getRequestURI())
								|| LocalCloudManagerConstants.HTTP_API_SESSION_PATH.equals(request.getRequestURI())).permitAll()
						.requestMatchers(request -> ConsoleFilter.isApi(request.getRequestURI())).hasAuthority(ROLE_OPERATOR)
						.anyRequest().permitAll())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, ex) -> unauthorized(response))
						.accessDeniedHandler((request, response, ex) -> unauthorized(response)));
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain arrowheadChain(final HttpSecurity http) throws Exception {
		http.httpBasic(basic -> basic.disable())
				.csrf(csrf -> csrf.disable())
				.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.NEVER));
		if (sslEnabled) {
			http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
		}

		return http.build();
	}

	static void unauthorized(final HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setHeader("Cache-Control", "no-store");
		response.getWriter().print("{\"error\":\"Console login required\"}");
	}

	// the console session cookie as Spring Security authentication
	static final class ConsoleSessionFilter extends OncePerRequestFilter {

		private final ConsoleAuth auth;

		ConsoleSessionFilter(final ConsoleAuth auth) {
			this.auth = auth;
		}

		@Override
		protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws ServletException, IOException {
			if (auth.authenticated(request)) {
				SecurityContextHolder.getContext()
						.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("operator", null, List.of(new SimpleGrantedAuthority(ROLE_OPERATOR))));
			}

			chain.doFilter(request, response);
		}
	}
}
