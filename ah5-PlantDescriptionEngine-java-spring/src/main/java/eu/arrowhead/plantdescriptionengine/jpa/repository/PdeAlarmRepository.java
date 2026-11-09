/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import eu.arrowhead.plantdescriptionengine.jpa.entity.PdeAlarm;

@Repository
public interface PdeAlarmRepository extends JpaRepository<PdeAlarm, Long>, JpaSpecificationExecutor<PdeAlarm> {

	List<PdeAlarm> findAllByClearedAtIsNull();
}
