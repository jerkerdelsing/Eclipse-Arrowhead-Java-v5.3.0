/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.service;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.onboarding.api.dto.OnboardingDtos.*;
import eu.arrowhead.onboarding.jpa.entity.DeviceEnrollment;
import eu.arrowhead.onboarding.jpa.entity.SystemEnrollment;
import eu.arrowhead.onboarding.jpa.repository.DeviceEnrollmentRepository;
import eu.arrowhead.onboarding.jpa.repository.SystemEnrollmentRepository;
import eu.arrowhead.onboarding.security.CloudCertificateAuthentication.Authenticated;
import eu.arrowhead.onboarding.security.CloudIdentity;
/**
 * Device side of the Arrowhead 5.2.2 onboarding procedure. Each step returns the credential for the next one:
 * bootstrap certificate -> onboarding (on) certificate -> device (de) certificate and DeviceRegistry entry ->
 * per approved hosted system a system (sy) certificate and an Authentication identity. With those the system
 * registers itself and its services in the ServiceRegistry like every other system.
 */
@Service
public class OnboardingService {
    private static final Pattern MAC = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}");
    private static final Pattern IP_OR_HOST = Pattern.compile("[0-9A-Fa-f:.]+|[A-Za-z0-9]([A-Za-z0-9.-]{0,251}[A-Za-z0-9])?");
    private final Logger logger = LogManager.getLogger(getClass());
    private final SecureRandom random = new SecureRandom();
    private final CloudIdentity cloud;
    private final CsrSupport csrs;
    private final CertificateAuthorityClient authority;
    private final CloudClient registry;
    private final DiscoveryService discovery;
    private final DeviceEnrollmentRepository devices;
    private final SystemEnrollmentRepository systems;
    private final EnrollmentService enrollments;
    public OnboardingService(CloudIdentity cloud, CsrSupport csrs, CertificateAuthorityClient authority, CloudClient registry,
            DiscoveryService discovery, DeviceEnrollmentRepository devices, SystemEnrollmentRepository systems, EnrollmentService enrollments) {
        this.cloud = cloud; this.csrs = csrs; this.authority = authority; this.registry = registry; this.discovery = discovery;
        this.devices = devices; this.systems = systems; this.enrollments = enrollments;
    }

    // ---- step 1: bootstrap certificate -> onboarding certificate ----
    public Response withName(NameRequest request, DeviceEnrollment enrollment, String address) {
        if (request == null || request.commonName() == null || request.commonName().isBlank()) { throw new InvalidParameterException("commonName is required"); }
        String expected = cloud.commonName(enrollment.getDeviceName());
        if (!expected.equals(request.commonName())) { throw new ForbiddenException("Requested name does not match enrollment identity"); }
        return onboardingCertificate(enrollment, csrs.generate(expected), address);
    }
    public Response withCsr(CsrRequest request, DeviceEnrollment enrollment, String address) {
        if (request == null) { throw new InvalidParameterException("certificateSigningRequest is required"); }
        String expected = cloud.commonName(enrollment.getDeviceName());
        String publicKey = csrs.check(request.certificateSigningRequest(), expected);
        return onboardingCertificate(enrollment, new CsrSupport.Csr(request.certificateSigningRequest(), publicKey, null), address);
    }
    private Response onboardingCertificate(DeviceEnrollment enrollment, CsrSupport.Csr csr, String address) {
        var services = discovery.discover(); // Resolve dependencies before issuing any certificate.
        var signed = authority.sign(csr.encoded(), "on", addresses(List.of(address)));
        if (enrollment.getOnboardingCertificateId() != null) {
            long previous = enrollment.getOnboardingCertificateId();
            quietly("revoke previous onboarding certificate " + previous, () -> authority.revoke(previous));
        }
        enrollment.onboardingIssued(signed.id(), signed.serialNumber());
        devices.save(enrollment);
        return new Response(signed.chain(), result(signed, "ON_BOARDING", csr), services,
            "POST /onboarding/device with this onboarding certificate to obtain the device certificate and register the device.");
    }

    // ---- step 2: onboarding certificate -> device certificate and DeviceRegistry entry ----
    @Transactional
    public DeviceResponse device(Authenticated caller, DeviceRequest request, String address) {
        String deviceName = caller.name();
        DeviceEnrollment enrollment = enrollments.find(deviceName);
        if (enrollment.getState() == DeviceEnrollment.State.OFFBOARDED) { throw new ForbiddenException("Device " + deviceName + " was off-boarded"); }
        if (!caller.serialNumber().equals(enrollment.getOnboardingCertificateSerial())) {
            throw new ForbiddenException("Only the onboarding certificate issued last for " + deviceName + " is accepted");
        }
        String expected = cloud.commonName(deviceName);
        var csr = csr(request == null ? null : request.certificateSigningRequest(), expected);
        List<String> addresses = request == null || request.addresses() == null || request.addresses().isEmpty() ? List.of(address) : request.addresses();
        var signed = authority.sign(csr.encoded(), "de", addresses(addresses));
        if (enrollment.getDeviceCertificateId() != null) {
            long previous = enrollment.getDeviceCertificateId();
            quietly("revoke previous device certificate " + previous, () -> authority.revoke(previous));
        }
        Map<String, Object> metadata = new HashMap<>(request == null || request.metadata() == null ? Map.of() : request.metadata());
        metadata.put("onboarding", Map.of("bootstrapFingerprint", enrollment.getBootstrapFingerprint(), "deviceCertificateId", signed.id()));
        try {
            registry.createDevice(deviceName, addresses, metadata);
        } catch (RuntimeException first) {
            // an earlier run registered the device already (it may host systems): update the entry
            try { registry.updateDevice(deviceName, addresses, metadata); }
            catch (RuntimeException ex) { quietly("revoke device certificate", () -> authority.revoke(signed.id())); throw ex; }
        }
        enrollment.deviceRegistered(signed.id(), signed.serialNumber(), signed.expiresAt());
        devices.save(enrollment);
        logger.info("Device {} onboarded (device certificate {})", deviceName, signed.id());
        return new DeviceResponse(deviceName, signed.chain(), result(signed, "DEVICE", csr), enrollment.approvedSystemList(),
            "POST /onboarding/system with this device certificate once per approved system to obtain its system certificate and identity.");
    }

    // ---- step 3: device certificate -> system certificate and Authentication identity ----
    @Transactional
    public SystemResponse system(Authenticated caller, SystemRequest request) {
        String deviceName = caller.name();
        DeviceEnrollment enrollment = currentDevice(caller);
        if (request == null) { throw new InvalidParameterException("systemName is required"); }
        String systemName = CloudIdentity.requireName(request.systemName());
        if (!enrollment.approvedSystemList().contains(systemName)) { throw new ForbiddenException(systemName + " is not approved for device " + deviceName); }
        var existing = systems.findById(systemName);
        if (existing.isPresent() && !existing.get().getDeviceName().equals(deviceName)) { throw new ForbiddenException(systemName + " is hosted by another device"); }
        String expected = cloud.commonName(systemName);
        var csr = csr(request.certificateSigningRequest(), expected);
        var signed = authority.sign(csr.encoded(), "sy", null);
        existing.ifPresent(previous -> {  // re-issue: the earlier certificate and password are withdrawn
            quietly("revoke previous certificate of " + systemName, () -> authority.revoke(previous.getCertificateId()));
            quietly("remove previous identity of " + systemName, () -> registry.removeIdentity(systemName));
        });
        String password = password();
        try { registry.createIdentity(systemName, password); }
        catch (RuntimeException ex) { quietly("revoke system certificate", () -> authority.revoke(signed.id())); throw ex; }
        systems.save(new SystemEnrollment(systemName, deviceName, signed.id(), signed.expiresAt()));
        logger.info("System {} on device {} onboarded (system certificate {})", systemName, deviceName, signed.id());
        return new SystemResponse(systemName, deviceName, signed.chain(), result(signed, "SYSTEM", csr), "PASSWORD",
            Map.of("password", password), discovery.discover(),
            "Log in at Authentication identity-login as " + systemName + " with these credentials and the system certificate, register the "
            + "system with systemDiscovery register (deviceName " + deviceName + ") and its services with serviceDiscovery register, "
            + "then consume services through orchestration. Store the password securely; Onboarding does not keep it.");
    }

    // ---- off-boarding requested by the device itself ----
    public OffboardingReport offboard(Authenticated caller) {
        currentDevice(caller);
        return enrollments.offboard(caller.name(), caller.name());
    }
    private DeviceEnrollment currentDevice(Authenticated caller) {
        DeviceEnrollment enrollment = enrollments.find(caller.name());
        if (enrollment.getState() != DeviceEnrollment.State.DEVICE_REGISTERED) { throw new ForbiddenException("Device " + caller.name() + " is not onboarded"); }
        if (!caller.serialNumber().equals(enrollment.getDeviceCertificateSerial())) {
            throw new ForbiddenException("Only the device certificate issued last for " + caller.name() + " is accepted");
        }
        return enrollment;
    }

    private CsrSupport.Csr csr(String encoded, String expected) {
        if (encoded == null || encoded.isBlank()) { return csrs.generate(expected); }
        return new CsrSupport.Csr(encoded, csrs.check(encoded, expected), null);
    }
    private static CertificateResult result(CertificateAuthorityClient.Signed signed, String type, CsrSupport.Csr csr) {
        return new CertificateResult(signed.leaf(), "X.509", type, csr.publicKey(), csr.privateKey(), signed.expiresAt());
    }
    /** Addresses that can go into the certificate (IP addresses and host names); others, such as MAC addresses, only into the registry. */
    private static List<String> addresses(List<String> addresses) {
        return addresses.stream().filter(a -> a != null && !MAC.matcher(a).matches() && IP_OR_HOST.matcher(a).matches()).toList();
    }
    private String password() {
        byte[] bytes = new byte[24]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private void quietly(String step, Runnable action) {
        try { action.run(); } catch (RuntimeException ex) { logger.warn("{} failed: {}", step, ex.getMessage()); }
    }
}
