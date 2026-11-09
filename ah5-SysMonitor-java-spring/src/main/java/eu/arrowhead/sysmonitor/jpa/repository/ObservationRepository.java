/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.jpa.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.sysmonitor.jpa.entity.Observation;

@Repository
public interface ObservationRepository extends RefreshableRepository<Observation, String> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// systemName, observationCount, firstObservationTime, lastObservationTime
	@Query("SELECT o.providerSystem, COUNT(o), MIN(o.sourceTime), MAX(o.sourceTime) FROM Observation o GROUP BY o.providerSystem ORDER BY o.providerSystem")
	List<Object[]> systems();

	//-------------------------------------------------------------------------------------------------
	// the newest observation of every type of one system
	@Query("SELECT o FROM Observation o WHERE o.providerSystem = ?1 AND o.sourceEpoch = "
			+ "(SELECT MAX(m.sourceEpoch) FROM Observation m WHERE m.providerSystem = ?1 AND m.observationType = o.observationType)")
	List<Observation> latest(final String providerSystem);

	//-------------------------------------------------------------------------------------------------
	@Modifying
	@Query("DELETE FROM Observation o WHERE o.receivedEpoch < ?1")
	int deleteReceivedBefore(final double epochSeconds);
}
