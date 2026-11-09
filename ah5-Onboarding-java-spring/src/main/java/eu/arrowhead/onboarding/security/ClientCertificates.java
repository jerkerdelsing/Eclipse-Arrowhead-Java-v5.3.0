/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.security;
import java.security.cert.X509Certificate;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
/** The client certificate of a mutual-TLS request; the TLS connector has already verified its chain. */
public final class ClientCertificates {
    private ClientCertificates() { }
    public static X509Certificate leaf(HttpServletRequest request) {
        if (!request.isSecure()) { throw new ForbiddenException("Enrollment requires HTTPS"); }
        Object value = request.getAttribute("jakarta.servlet.request.X509Certificate");
        if (!(value instanceof X509Certificate[] chain) || chain.length == 0) { throw new AuthException("Client certificate required"); }
        return chain[0];
    }
}
