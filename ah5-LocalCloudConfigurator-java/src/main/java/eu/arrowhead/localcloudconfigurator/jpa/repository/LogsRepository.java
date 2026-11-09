package eu.arrowhead.localcloudconfigurator.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.LogEntityRepository;
import eu.arrowhead.localcloudconfigurator.jpa.entity.Logs;

@Repository
public interface LogsRepository extends LogEntityRepository<Logs> {
}
