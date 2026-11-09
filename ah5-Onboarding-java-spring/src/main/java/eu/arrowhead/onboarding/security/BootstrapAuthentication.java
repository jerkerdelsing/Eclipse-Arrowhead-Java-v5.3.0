/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.security;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import org.springframework.stereotype.Component;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.onboarding.jpa.entity.DeviceEnrollment;
import eu.arrowhead.onboarding.jpa.repository.DeviceEnrollmentRepository;
import jakarta.servlet.http.HttpServletRequest;
/**
 * Step 1 of the onboarding procedure: the device presents its bootstrap (manufacturer) certificate. Its SHA-256
 * fingerprint must belong to a device an operator approved (onboardingManagement approve-device) and not off-boarded.
 */
@Component
public class BootstrapAuthentication {
    private final DeviceEnrollmentRepository enrollments;
    public BootstrapAuthentication(DeviceEnrollmentRepository enrollments) { this.enrollments = enrollments; }
    public DeviceEnrollment authenticate(HttpServletRequest request) {
        X509Certificate leaf = ClientCertificates.leaf(request);
        try {
            leaf.checkValidity();
            String fingerprint = fingerprint(leaf);
            DeviceEnrollment enrollment = enrollments.findByBootstrapFingerprint(fingerprint)
                .orElseThrow(() -> new AuthException("Certificate is not approved for enrollment"));
            if (enrollment.getState() == DeviceEnrollment.State.OFFBOARDED) {
                throw new ForbiddenException("Device " + enrollment.getDeviceName() + " was off-boarded; an operator must approve it again");
            }
            return enrollment;
        } catch (AuthException | ForbiddenException ex) { throw ex; }
        catch (Exception ex) { throw new AuthException("Invalid enrollment certificate"); }
    }
    public static String fingerprint(X509Certificate certificate) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
    }
}
