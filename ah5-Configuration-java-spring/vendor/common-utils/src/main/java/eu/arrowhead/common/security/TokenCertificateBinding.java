/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.common.security;

import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.security.SecurityUtilities.CommonNameAndType;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import jakarta.servlet.http.HttpServletRequest;

// Binds an identity token (or authenticator key) to the TLS client certificate of the same request: over HTTPS the
// system named by the token must be the system named by the certificate CN, so a stolen token cannot be replayed
// from another system's certificate. Plain HTTP requests carry no TLS identity and are left to the token alone.
public final class TokenCertificateBinding {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static void check(final HttpServletRequest request, final String tokenSystemName, final SystemNameNormalizer normalizer) {
		if (!request.isSecure()) {
			return;
		}

		final CommonNameAndType certificate = SecurityUtilities.getIdentificationDataFromRequest(request);
		if (certificate == null) {
			throw new AuthException("Identity token must be presented with the client certificate of the same system");
		}

		final String certificateSystem = normalizer.normalize(SecurityUtilities.getClientNameFromClientCN(certificate.commonName()));
		if (tokenSystemName == null || !certificateSystem.equals(normalizer.normalize(tokenSystemName))) {
			throw new AuthException("Identity token does not belong to the system of the client certificate");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private TokenCertificateBinding() {
		throw new UnsupportedOperationException();
	}
}
