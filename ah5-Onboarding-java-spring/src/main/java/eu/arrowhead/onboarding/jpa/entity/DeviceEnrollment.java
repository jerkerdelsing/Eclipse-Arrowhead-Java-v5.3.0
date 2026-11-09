/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.jpa.entity;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.*;
/** A device an operator approved for onboarding: its bootstrap certificate and the systems it may host. */
@Entity
@Table(name = "device_enrollment")
public class DeviceEnrollment {
    public enum State { APPROVED, DEVICE_REGISTERED, OFFBOARDED }
    @Id @Column(name = "device_name", length = 63) private String deviceName;
    @Column(name = "bootstrap_fingerprint", columnDefinition = "char(64)", nullable = false, unique = true) private String bootstrapFingerprint;
    @Column(name = "approved_systems", length = 2048, nullable = false) private String approvedSystems;
    @Enumerated(EnumType.STRING) @Column(length = 20, nullable = false) private State state;
    @Column(name = "onboarding_certificate_id") private Long onboardingCertificateId;
    @Column(name = "onboarding_certificate_serial", length = 64) private String onboardingCertificateSerial;
    @Column(name = "device_certificate_id") private Long deviceCertificateId;
    @Column(name = "device_certificate_serial", length = 64) private String deviceCertificateSerial;
    @Column(name = "device_certificate_expires_at", length = 40) private String deviceCertificateExpiresAt;
    @Column(name = "approved_by", length = 63, nullable = false) private String approvedBy;
    @Column(name = "approved_at", nullable = false) private Instant approvedAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "last_message", length = 1024) private String lastMessage;
    protected DeviceEnrollment() { }
    public DeviceEnrollment(String deviceName, String bootstrapFingerprint, List<String> systems, String approvedBy) {
        this.deviceName = deviceName; this.approvedAt = Instant.now();
        approve(bootstrapFingerprint, systems, approvedBy);
    }
    public final void approve(String fingerprint, List<String> systems, String actor) {
        bootstrapFingerprint = fingerprint; approvedSystems = String.join(",", systems); approvedBy = actor;
        state = State.APPROVED; touch("approved by " + actor);
        onboardingCertificateId = null; onboardingCertificateSerial = null;
        deviceCertificateId = null; deviceCertificateSerial = null; deviceCertificateExpiresAt = null;
    }
    public void approveSystems(List<String> systems, String actor) {
        approvedSystems = String.join(",", systems); approvedBy = actor; touch("approved systems updated by " + actor);
    }
    /** Only the onboarding certificate issued last is accepted for the device step. */
    public void onboardingIssued(long certificateId, String serial) {
        onboardingCertificateId = certificateId; onboardingCertificateSerial = serial; touch("onboarding certificate issued");
    }
    /** Only the device certificate issued last is accepted for the system and off-boarding steps. */
    public void deviceRegistered(long certificateId, String serial, String expiresAt) {
        deviceCertificateId = certificateId; deviceCertificateSerial = serial; deviceCertificateExpiresAt = expiresAt;
        state = State.DEVICE_REGISTERED; touch("device registered");
    }
    public void offboarded(String message) {
        state = State.OFFBOARDED; touch(message);
        onboardingCertificateId = null; onboardingCertificateSerial = null; deviceCertificateId = null; deviceCertificateSerial = null;
    }
    public void touch(String message) {
        updatedAt = Instant.now(); lastMessage = message == null ? null : message.substring(0, Math.min(1024, message.length()));
    }
    public List<String> approvedSystemList() {
        return approvedSystems.isBlank() ? List.of() : Arrays.stream(approvedSystems.split(",")).toList();
    }
    public String getDeviceName() { return deviceName; }
    public String getBootstrapFingerprint() { return bootstrapFingerprint; }
    public State getState() { return state; }
    public Long getOnboardingCertificateId() { return onboardingCertificateId; }
    public String getOnboardingCertificateSerial() { return onboardingCertificateSerial; }
    public Long getDeviceCertificateId() { return deviceCertificateId; }
    public String getDeviceCertificateSerial() { return deviceCertificateSerial; }
    public String getDeviceCertificateExpiresAt() { return deviceCertificateExpiresAt; }
    public String getApprovedBy() { return approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getLastMessage() { return lastMessage; }
}
