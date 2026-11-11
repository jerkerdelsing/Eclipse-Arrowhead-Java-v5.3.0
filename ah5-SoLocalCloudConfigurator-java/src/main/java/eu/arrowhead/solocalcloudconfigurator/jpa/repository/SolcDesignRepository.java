/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.jpa.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.solocalcloudconfigurator.jpa.entity.SolcDesignEntity;

@Repository
public interface SolcDesignRepository extends RefreshableRepository<SolcDesignEntity, String> {

	Optional<SolcDesignEntity> findByName(String name);
}
