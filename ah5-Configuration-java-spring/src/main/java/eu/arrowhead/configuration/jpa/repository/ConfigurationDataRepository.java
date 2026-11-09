/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.configuration.jpa.entity.ConfigurationData;

@Repository
public interface ConfigurationDataRepository extends RefreshableRepository<ConfigurationData, Long> {

	Optional<ConfigurationData> findBySystemName(final String systemName);
}
