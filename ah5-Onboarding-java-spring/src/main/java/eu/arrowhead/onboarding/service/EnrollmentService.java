/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.service;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.onboarding.api.dto.OnboardingDtos.*;
import eu.arrowhead.onboarding.jpa.entity.DeviceEnrollment;
import eu.arrowhead.onboarding.jpa.entity.SystemEnrollment;
import eu.arrowhead.onboarding.jpa.repository.DeviceEnrollmentRepository;
import eu.arrowhead.onboarding.jpa.repository.SystemEnrollmentRepository;
import eu.arrowhead.onboarding.security.CloudIdentity;
/**
 * Operator side of the onboarding procedure: approving a device (its bootstrap certificate and the systems it may host)
 * and off-boarding it again, which withdraws everything the procedure created in the local cloud.
 */
@Service
public class EnrollmentService {
    private static final int MAX_SYSTEMS = 32;
    /** Core and support system and operator names (security finding M1): never a device or an onboarded system. */
    static final Set<String> RESERVED_NAMES = Set.copyOf(List.of((System.getenv().getOrDefault("ONBOARDING_RESERVED_NAMES",
            "ServiceRegistry,Authentication,ConsumerAuthorization,DynamicServiceOrchestration,SimpleStoreServiceOrchestration,"
            + "Blacklist,TranslationManager,DeviceQoSEvaluator,Onboarding,CertificateAuthority,DataManager,TimeManager,"
            + "EventHandler,PlantDescriptionEngine,Configuration,Choreographer,SysMonitor,Gatekeeper,GateTunnel,AIGym,"
            + "LocalCloudManager,Sysop,RandomDataProducer,SmokeClient,Relay")).split(",")));
    private final Logger logger = LogManager.getLogger(getClass());
    private final DeviceEnrollmentRepository devices;
    private final SystemEnrollmentRepository systems;
    private final CertificateAuthorityClient authority;
    private final CloudClient cloud;
    public EnrollmentService(DeviceEnrollmentRepository devices, SystemEnrollmentRepository systems,
            CertificateAuthorityClient authority, CloudClient cloud) {
        this.devices = devices; this.systems = systems; this.authority = authority; this.cloud = cloud;
    }

    @Transactional
    public EnrollmentView approve(String deviceName, EnrollmentRequest request, String actor) {
        CloudIdentity.requireName(deviceName);
        if (request == null || request.bootstrapFingerprint() == null) { throw new InvalidParameterException("bootstrapFingerprint is required"); }
        String fingerprint = request.bootstrapFingerprint().replace(":", "").toLowerCase();
        if (!fingerprint.matches("[0-9a-f]{64}")) { throw new InvalidParameterException("bootstrapFingerprint must be the SHA-256 of the DER certificate (64 hex digits)"); }
        var names = new LinkedHashSet<String>();
        for (String name : request.systems() == null ? List.<String>of() : request.systems()) { names.add(CloudIdentity.requireName(name)); }
        if (names.isEmpty() || names.size() > MAX_SYSTEMS) { throw new InvalidParameterException("Approve 1.." + MAX_SYSTEMS + " systems for the device"); }
        requireUnreservedAndUnique(deviceName, names);
        devices.findByBootstrapFingerprint(fingerprint).filter(other -> !other.getDeviceName().equals(deviceName)).ifPresent(other -> {
            throw new InvalidParameterException("The bootstrap certificate is already approved for device " + other.getDeviceName());
        });
        var existing = devices.findById(deviceName);
        DeviceEnrollment enrollment;
        if (existing.isPresent() && existing.get().getState() == DeviceEnrollment.State.DEVICE_REGISTERED) {
            enrollment = existing.get();
            if (!enrollment.getBootstrapFingerprint().equals(fingerprint)) {
                throw new InvalidParameterException("Device " + deviceName + " is onboarded; off-board it before changing its bootstrap certificate");
            }
            enrollment.approveSystems(List.copyOf(names), actor);   // keeps the device onboarded; adds or withdraws approvals
        } else {
            enrollment = existing.orElseGet(() -> new DeviceEnrollment(deviceName, fingerprint, List.copyOf(names), actor));
            enrollment.approve(fingerprint, List.copyOf(names), actor);
            systems.deleteAll(systems.findByDeviceName(deviceName));
            // a re-approved device may host systems that were blacklisted when it was off-boarded
            best("unblacklist " + names, () -> cloud.unblacklist(List.copyOf(names)), new ArrayList<>(), new ArrayList<>());
        }
        return view(devices.save(enrollment));
    }

    /** Reserved names are refused, and so is a name another enrolled device uses for itself or one of its systems. */
    private void requireUnreservedAndUnique(String deviceName, Set<String> systemNames) {
        var requested = new LinkedHashSet<String>(systemNames);
        requested.add(deviceName);
        for (String name : requested) {
            if (RESERVED_NAMES.contains(name)) { throw new InvalidParameterException(name + " is a reserved system name"); }
        }
        if (systemNames.contains(deviceName)) { throw new InvalidParameterException("A system cannot have the device's name " + deviceName); }
        for (DeviceEnrollment other : devices.findAll()) {
            if (other.getDeviceName().equals(deviceName)) { continue; }
            var taken = new LinkedHashSet<String>(other.approvedSystemList());
            taken.add(other.getDeviceName());
            for (String name : requested) {
                if (taken.contains(name)) { throw new InvalidParameterException(name + " is already used by device " + other.getDeviceName()); }
            }
        }
    }

    public EnrollmentList list() {
        var entries = devices.findAll().stream().map(this::view).toList();
        return new EnrollmentList(entries, entries.size());
    }

    public EnrollmentView get(String deviceName) { return view(find(deviceName)); }

    /** Withdraws everything the procedure created: registry entries, identities, certificates; blacklists the systems. */
    @Transactional
    public OffboardingReport offboard(String deviceName, String actor) {
        DeviceEnrollment enrollment = find(deviceName);
        var completed = new ArrayList<String>(); var failed = new ArrayList<String>();
        for (SystemEnrollment system : systems.findByDeviceName(deviceName)) {
            String name = system.getSystemName();
            best("ServiceRegistry system-remove " + name, () -> cloud.removeSystem(name), completed, failed);
            best("Authentication identity-mgmt-remove " + name, () -> cloud.removeIdentity(name), completed, failed);
            best("CertificateAuthority revoke-certificate " + system.getCertificateId() + " (" + name + ")",
                () -> authority.revoke(system.getCertificateId()), completed, failed);
            best("Blacklist create " + name, () -> cloud.blacklist(name, system.getCertificateExpiresAt(),
                "Off-boarded from device " + deviceName + " by " + actor), completed, failed);
            systems.delete(system);
        }
        best("ServiceRegistry device-remove " + deviceName, () -> cloud.removeDevice(deviceName), completed, failed);
        for (Long id : new Long[] { enrollment.getDeviceCertificateId(), enrollment.getOnboardingCertificateId() }) {
            if (id != null) {
                best("CertificateAuthority revoke-certificate " + id + " (" + deviceName + ")", () -> authority.revoke(id), completed, failed);
            }
        }
        enrollment.offboarded("off-boarded by " + actor + (failed.isEmpty() ? "" : "; failed: " + String.join("; ", failed)));
        devices.save(enrollment);
        logger.info("Off-boarded device {} by {}: {} steps done, {} failed", deviceName, actor, completed.size(), failed.size());
        return new OffboardingReport(deviceName, completed, failed);
    }

    /** Removes the approval; an onboarded device is off-boarded first. */
    @Transactional
    public OffboardingReport remove(String deviceName, String actor) {
        DeviceEnrollment enrollment = find(deviceName);
        OffboardingReport report = enrollment.getState() == DeviceEnrollment.State.DEVICE_REGISTERED || !systems.findByDeviceName(deviceName).isEmpty()
            ? offboard(deviceName, actor) : new OffboardingReport(deviceName, List.of(), List.of());
        devices.delete(enrollment);
        return report;
    }

    DeviceEnrollment find(String deviceName) {
        return devices.findById(CloudIdentity.requireName(deviceName)).orElseThrow(() -> new DataNotFoundException("No enrollment for device " + deviceName));
    }

    private EnrollmentView view(DeviceEnrollment e) {
        var issued = systems.findByDeviceName(e.getDeviceName()).stream().map(s -> new SystemEnrollmentView(s.getSystemName(),
            s.getCertificateId(), s.getCertificateExpiresAt(), s.getIssuedAt().toString())).toList();
        return new EnrollmentView(e.getDeviceName(), e.getBootstrapFingerprint(), e.approvedSystemList(), e.getState().name(),
            e.getDeviceCertificateId(), e.getDeviceCertificateExpiresAt(), issued, e.getApprovedBy(), e.getApprovedAt().toString(),
            e.getUpdatedAt().toString(), e.getLastMessage());
    }

    private void best(String step, Runnable action, List<String> completed, List<String> failed) {
        try { action.run(); completed.add(step); }
        catch (RuntimeException ex) { failed.add(step + ": " + ex.getMessage()); logger.warn("{} failed: {}", step, ex.getMessage()); }
    }
}
