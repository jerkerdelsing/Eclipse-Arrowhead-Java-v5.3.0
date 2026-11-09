/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.security;
import java.security.cert.X509Certificate;
import java.util.Base64;
import org.springframework.stereotype.Component;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.security.CertificateProfileType;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.onboarding.service.CertificateAuthorityClient;
import jakarta.servlet.http.HttpServletRequest;
/**
 * Steps 2 and 3 of the onboarding procedure: the device presents the onboarding (on) or device (de) certificate that the
 * local-cloud CertificateAuthority issued to it earlier in the procedure. The certificate must be issued by the cloud CA,
 * carry the expected profile and a name of this cloud, and still be GOOD in the CertificateAuthority's register.
 */
@Component
public class CloudCertificateAuthentication {
    private final CloudIdentity cloud;
    private final CertificateAuthorityClient authority;
    public CloudCertificateAuthentication(CloudIdentity cloud, CertificateAuthorityClient authority) {
        this.cloud = cloud; this.authority = authority;
    }
    /** The device name of the client certificate and its serial number (decimal, as registered by the CertificateAuthority). */
    public record Authenticated(String name, String serialNumber) { }
    public Authenticated authenticate(HttpServletRequest request, CertificateProfileType expected) {
        X509Certificate leaf = ClientCertificates.leaf(request);
        String name = verify(leaf, expected);
        try {
            String status = authority.status(Base64.getEncoder().encodeToString(leaf.getEncoded()));
            if (!"GOOD".equals(status)) { throw new ForbiddenException("Certificate is " + status + " at the CertificateAuthority"); }
        } catch (ForbiddenException ex) { throw ex; }
        catch (Exception ex) { throw new AuthException("Certificate status unavailable: " + ex.getMessage()); }
        return new Authenticated(name, leaf.getSerialNumber().toString());
    }
    String verify(X509Certificate leaf, CertificateProfileType expected) {
        try { leaf.checkValidity(); } catch (Exception ex) { throw new AuthException("Certificate is not valid now"); }
        if (!cloud.cloudCa().equals(leaf.getIssuerX500Principal())) { throw new ForbiddenException("Certificate is not issued by the local-cloud CA"); }
        var identity = SecurityUtilities.getIdentificationDataFromCertificate(leaf);
        if (identity == null || identity.profileType() != expected) {
            throw new ForbiddenException("A " + expected + " certificate is required for this step");
        }
        if (!cloud.cloudCommonName().equals(SecurityUtilities.getCloudCN(identity.commonName()))) {
            throw new ForbiddenException("Certificate belongs to another cloud");
        }
        return SecurityUtilities.getClientNameFromClientCN(identity.commonName());
    }
}
