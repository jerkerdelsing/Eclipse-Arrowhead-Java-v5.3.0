/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.jpa.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import eu.arrowhead.onboarding.jpa.entity.SystemEnrollment;
@Repository
public interface SystemEnrollmentRepository extends JpaRepository<SystemEnrollment, String> {
    List<SystemEnrollment> findByDeviceName(String deviceName);
}
