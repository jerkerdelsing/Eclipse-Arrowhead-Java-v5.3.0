/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.api.http;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import eu.arrowhead.common.security.CertificateProfileType;
import eu.arrowhead.onboarding.api.dto.OnboardingDtos.*;
import eu.arrowhead.onboarding.security.BootstrapAuthentication;
import eu.arrowhead.onboarding.security.CloudCertificateAuthentication;
import eu.arrowhead.onboarding.service.OnboardingService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
/** The onboarding service: one operation per step, each authenticated by the credential of the previous step. */
@RestController
@RequestMapping("/onboarding")
public class OnboardingAPI {
    private final BootstrapAuthentication bootstrap;
    private final CloudCertificateAuthentication certificates;
    private final OnboardingService service;
    public OnboardingAPI(BootstrapAuthentication bootstrap, CloudCertificateAuthentication certificates, OnboardingService service) {
        this.bootstrap = bootstrap; this.certificates = certificates; this.service = service;
    }
    @GetMapping(value="/echo", produces="text/plain")
    public String echo() { return "OK"; }
    @Operation(summary="Step 1: enroll with an approved bootstrap certificate; Onboarding generates the key of the onboarding certificate")
    @PostMapping(value="/certificate/name", consumes="application/json", produces="application/json")
    public ResponseEntity<Response> certificateName(HttpServletRequest http, @RequestBody NameRequest request) {
        return result(service.withName(request, bootstrap.authenticate(http), http.getRemoteAddr()));
    }
    @Operation(summary="Step 1: enroll with an approved bootstrap certificate and a PKCS#10 CSR for the onboarding certificate")
    @PostMapping(value="/certificate/csr", consumes="application/json", produces="application/json")
    public ResponseEntity<Response> certificateCsr(HttpServletRequest http, @RequestBody CsrRequest request) {
        return result(service.withCsr(request, bootstrap.authenticate(http), http.getRemoteAddr()));
    }
    @Operation(summary="Step 2: with the onboarding certificate, obtain the device certificate and register the device")
    @PostMapping(value="/device", consumes="application/json", produces="application/json")
    public ResponseEntity<DeviceResponse> device(HttpServletRequest http, @RequestBody(required=false) DeviceRequest request) {
        return result(service.device(certificates.authenticate(http, CertificateProfileType.ON_BOARDING), request, http.getRemoteAddr()));
    }
    @Operation(summary="Step 3: with the device certificate, obtain the certificate and identity of one approved hosted system")
    @PostMapping(value="/system", consumes="application/json", produces="application/json")
    public ResponseEntity<SystemResponse> system(HttpServletRequest http, @RequestBody SystemRequest request) {
        return result(service.system(certificates.authenticate(http, CertificateProfileType.DEVICE), request));
    }
    @Operation(summary="Off-boarding requested by the device: withdraw its systems, identities, registry entries and certificates")
    @PostMapping(value="/device/offboard", produces="application/json")
    public ResponseEntity<OffboardingReport> offboard(HttpServletRequest http) {
        return result(service.offboard(certificates.authenticate(http, CertificateProfileType.DEVICE)));
    }
    private static <T> ResponseEntity<T> result(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache").body(body);
    }
}
