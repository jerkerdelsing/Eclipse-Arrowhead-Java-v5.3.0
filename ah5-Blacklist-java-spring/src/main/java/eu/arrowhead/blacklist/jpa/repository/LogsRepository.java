/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.blacklist.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.blacklist.jpa.entity.Logs;
import eu.arrowhead.common.jpa.LogEntityRepository;

@Repository
public interface LogsRepository extends LogEntityRepository<Logs> {
}