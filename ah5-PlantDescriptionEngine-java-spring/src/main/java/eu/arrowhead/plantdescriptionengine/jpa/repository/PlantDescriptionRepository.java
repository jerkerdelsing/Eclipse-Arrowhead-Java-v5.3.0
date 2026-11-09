/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.jpa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import eu.arrowhead.plantdescriptionengine.jpa.entity.PlantDescription;

@Repository
public interface PlantDescriptionRepository extends JpaRepository<PlantDescription, Long> {

	Optional<PlantDescription> findFirstByActiveTrue();

	List<PlantDescription> findAllByActiveTrue();

	Page<PlantDescription> findAllByActive(boolean active, Pageable pageable);
}
