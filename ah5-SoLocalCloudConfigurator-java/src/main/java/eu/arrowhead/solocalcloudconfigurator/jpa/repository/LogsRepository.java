package eu.arrowhead.solocalcloudconfigurator.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.LogEntityRepository;
import eu.arrowhead.solocalcloudconfigurator.jpa.entity.Logs;

@Repository
public interface LogsRepository extends LogEntityRepository<Logs> {
}
