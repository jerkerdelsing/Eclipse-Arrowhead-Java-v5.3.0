/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.api.dto;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
public final class OnboardingDtos {
    private OnboardingDtos() { }
    // step 1: bootstrap certificate -> onboarding (on) certificate
    public record NameRequest(String commonName) { }
    public record CsrRequest(String certificateSigningRequest) { }
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CertificateResult(String certificate, String certificateFormat, String certificateType,
        String publicKey, String privateKey, String expiresAt) { }
    public record Response(List<String> certificateChain, CertificateResult onboardingCertificate,
        List<ServiceInstanceResponseDTO> services, String nextStep) { }
    // step 2: onboarding certificate -> device (de) certificate and DeviceRegistry entry
    public record DeviceRequest(String certificateSigningRequest, List<String> addresses, Map<String, Object> metadata) { }
    public record DeviceResponse(String deviceName, List<String> certificateChain, CertificateResult deviceCertificate,
        List<String> approvedSystems, String nextStep) { }
    // step 3: device certificate -> system (sy) certificate and Authentication identity per hosted system
    public record SystemRequest(String systemName, String certificateSigningRequest) { }
    public record SystemResponse(String systemName, String deviceName, List<String> certificateChain,
        CertificateResult systemCertificate, String authenticationMethod, Map<String, String> credentials,
        List<ServiceInstanceResponseDTO> services, String nextStep) { }
    // off-boarding
    public record OffboardingReport(String deviceName, List<String> completed, List<String> failed) { }
    // operator management
    public record EnrollmentRequest(String bootstrapFingerprint, List<String> systems) { }
    public record SystemEnrollmentView(String systemName, long certificateId, String certificateExpiresAt, String issuedAt) { }
    public record EnrollmentView(String deviceName, String bootstrapFingerprint, List<String> approvedSystems, String state,
        Long deviceCertificateId, String deviceCertificateExpiresAt, List<SystemEnrollmentView> systems,
        String approvedBy, String approvedAt, String updatedAt, String lastMessage) { }
    public record EnrollmentList(List<EnrollmentView> entries, int count) { }
}
