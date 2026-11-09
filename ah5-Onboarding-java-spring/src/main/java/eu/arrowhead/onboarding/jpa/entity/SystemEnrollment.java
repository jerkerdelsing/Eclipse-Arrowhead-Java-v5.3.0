/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.jpa.entity;
import java.time.Instant;
import jakarta.persistence.*;
/** A system certificate and identity that Onboarding issued for a system hosted on an enrolled device. */
@Entity
@Table(name = "system_enrollment")
public class SystemEnrollment {
    @Id @Column(name = "system_name", length = 63) private String systemName;
    @Column(name = "device_name", length = 63, nullable = false) private String deviceName;
    @Column(name = "certificate_id", nullable = false) private long certificateId;
    @Column(name = "certificate_expires_at", length = 40, nullable = false) private String certificateExpiresAt;
    @Column(name = "issued_at", nullable = false) private Instant issuedAt;
    protected SystemEnrollment() { }
    public SystemEnrollment(String systemName, String deviceName, long certificateId, String certificateExpiresAt) {
        this.systemName = systemName; this.deviceName = deviceName; this.certificateId = certificateId;
        this.certificateExpiresAt = certificateExpiresAt; this.issuedAt = Instant.now();
    }
    public String getSystemName() { return systemName; }
    public String getDeviceName() { return deviceName; }
    public long getCertificateId() { return certificateId; }
    public String getCertificateExpiresAt() { return certificateExpiresAt; }
    public Instant getIssuedAt() { return issuedAt; }
}
