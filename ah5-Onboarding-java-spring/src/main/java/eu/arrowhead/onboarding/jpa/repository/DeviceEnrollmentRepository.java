/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.jpa.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import eu.arrowhead.onboarding.jpa.entity.DeviceEnrollment;
@Repository
public interface DeviceEnrollmentRepository extends JpaRepository<DeviceEnrollment, String> {
    Optional<DeviceEnrollment> findByBootstrapFingerprint(String fingerprint);
}
