/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.jpa.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import eu.arrowhead.datamanager.jpa.entity.HistorianRecord;

@Repository
public interface HistorianRecordRepository extends JpaRepository<HistorianRecord, UUID> {

	@Query("select distinct h.systemName from HistorianRecord h order by h.systemName")
	List<String> findDistinctSystemNames();

	@Query("select distinct h.serviceName from HistorianRecord h where h.systemName = :systemName order by h.serviceName")
	List<String> findDistinctServiceNamesBySystemName(@Param("systemName") String systemName);

	@Query("select h from HistorianRecord h where h.systemName = :systemName and h.serviceName = :serviceName "
			+ "and h.maxTime >= :from and h.minTime <= :to order by h.baseTime desc")
	List<HistorianRecord> findInTimeRange(
			@Param("systemName") String systemName,
			@Param("serviceName") String serviceName,
			@Param("from") double from,
			@Param("to") double to,
			Pageable pageable);
}
