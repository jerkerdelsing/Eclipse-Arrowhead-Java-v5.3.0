/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.security;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.quartz.OutsourcedLoginJob;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import eu.arrowhead.gatetunnel.service.GateTunnelApiException;

// The own identity token (kept by the common OutsourcedLoginJob) for calls to providers, and the verification of a
// caller's token on the inter-cloud tunnel listeners (Authentication's identity-verify, consumed through the common library).
@Component
public class IdentityTokens {

	//=================================================================================================
	// members

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private ArrowheadHttpService httpService;

	@Autowired
	private ObjectProvider<OutsourcedLoginJob> loginJob;

	private final Object renewal = new Object();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String current() {
		String token = sysInfo.getIdentityToken();
		if (token == null) {
			final OutsourcedLoginJob job = loginJob.getIfAvailable();
			if (job != null) {
				job.loginIfNeeded();
			}
			token = sysInfo.getIdentityToken();
		}
		if (token == null || token.isBlank()) {
			throw new GateTunnelApiException(503, "No identity token; the login to Authentication has not succeeded");
		}

		return token;
	}

	//-------------------------------------------------------------------------------------------------
	// A provider refused the token: renew it, but only once for every request that used the same token.
	public void invalidate(final String used) {
		synchronized (renewal) {
			if (!Objects.equals(sysInfo.getIdentityToken(), used)) {
				return;
			}

			sysInfo.getArrowheadContext().remove(Constants.KEY_IDENTITY_RENEWAL_THRESHOLD);
			sysInfo.getArrowheadContext().remove(Constants.KEY_IDENTITY_TOKEN);
			final OutsourcedLoginJob job = loginJob.getIfAvailable();
			if (job != null) {
				job.loginIfNeeded();
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Returns the verified system name of the token, or throws 401.
	public IdentityVerifyResponseDTO verify(final String token) {
		try {
			final IdentityVerifyResponseDTO response = httpService.consumeService(Constants.SERVICE_DEF_IDENTITY, Constants.SERVICE_OP_IDENTITY_VERIFY, IdentityVerifyResponseDTO.class, List.of(token));
			if (response == null || !response.verified()) {
				throw new GateTunnelApiException(401, "Identity token does not match the client certificate");
			}

			return response;
		} catch (final ArrowheadException ex) {
			throw new GateTunnelApiException(401, "Identity token does not match the client certificate");
		}
	}
}
