/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.gatekeeper.GatekeeperConstants;
import eu.arrowhead.gatekeeper.service.StatusService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Token-free liveness on the mutual-TLS port (J18): /health (and the shared native alias /certificate-authority/health)
// answer {"status","system"} before authentication. The TLS layer still requires a trusted client certificate.
@Component
@Order(GatekeeperConstants.REQUEST_FILTER_ORDER_LIVENESS)
public class LivenessFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Autowired
	private StatusService statusService;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		if (!RequestPaths.isLiveness(RequestPaths.path(request))) {
			chain.doFilter(request, response);
			return;
		}

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().print(mapper.writeValueAsString(statusService.liveness()));
		response.getWriter().flush();
	}
}
