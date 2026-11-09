package eu.arrowhead.localcloudconfigurator.jpa.repository;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.localcloudconfigurator.jpa.entity.ActiveDeployment;

@Repository
public interface ActiveDeploymentRepository extends RefreshableRepository<ActiveDeployment, Integer> {
}
