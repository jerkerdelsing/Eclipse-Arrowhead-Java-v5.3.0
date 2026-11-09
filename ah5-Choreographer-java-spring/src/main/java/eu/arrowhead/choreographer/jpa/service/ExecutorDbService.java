/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.jpa.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.choreographer.api.dto.enums.SessionStepStatus;
import eu.arrowhead.choreographer.jpa.entity.Executor;
import eu.arrowhead.choreographer.jpa.entity.ExecutorServiceDefinition;
import eu.arrowhead.choreographer.jpa.entity.SessionStep;
import eu.arrowhead.choreographer.jpa.repository.ExecutorRepository;
import eu.arrowhead.choreographer.jpa.repository.ExecutorServiceDefinitionRepository;
import eu.arrowhead.choreographer.jpa.repository.SessionStepRepository;
import eu.arrowhead.choreographer.service.Versions;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;

@Service
public class ExecutorDbService {

	@Autowired
	private ExecutorRepository executorRepository;

	@Autowired
	private ExecutorServiceDefinitionRepository serviceDefinitionRepository;

	@Autowired
	private SessionStepRepository sessionStepRepository;

	// Creates the executor or updates its endpoint, and adds or updates the service definition it executes.
	@Transactional(rollbackFor = Exception.class)
	public Executor createOrUpdate(final String name, final String address, final int port, final String basePath, final String serviceDefinition, final int minVersion, final int maxVersion) {
		final Optional<Executor> existing = executorRepository.findByName(name);
		final Executor executor;
		if (existing.isPresent()) {
			executor = existing.get();
			if (executor.isLocked()) {
				throw new LockedException("Executor " + name + " is being removed");
			}
			executor.setAddress(address);
			executor.setPort(port);
			executor.setBasePath(basePath);
			executorRepository.saveAndFlush(executor);
		} else {
			executor = executorRepository.saveAndFlush(new Executor(name, address, port, basePath));
		}
		final Optional<ExecutorServiceDefinition> definition = serviceDefinitionRepository.findByExecutorAndServiceDefinition(executor, serviceDefinition);
		if (definition.isPresent()) {
			definition.get().setMinVersion(minVersion);
			definition.get().setMaxVersion(maxVersion);
			serviceDefinitionRepository.saveAndFlush(definition.get());
		} else {
			serviceDefinitionRepository.saveAndFlush(new ExecutorServiceDefinition(executor, serviceDefinition, minVersion, maxVersion));
		}
		return executor;
	}

	public Optional<Executor> findById(final long id) {
		return executorRepository.findById(id);
	}

	public Optional<Executor> findByName(final String name) {
		return executorRepository.findByName(name);
	}

	public Page<Executor> getExecutors(final PageRequest page) {
		return executorRepository.findAll(page);
	}

	public List<ExecutorServiceDefinition> getServiceDefinitions(final Executor executor) {
		return serviceDefinitionRepository.findAllByExecutor(executor);
	}

	// Executors (unlocked) that can execute the service definition in the given major-version window.
	public List<Executor> getExecutorsFor(final String serviceDefinition, final Integer minVersion, final Integer maxVersion) {
		return serviceDefinitionRepository.findAllByServiceDefinition(serviceDefinition)
				.stream()
				.filter(d -> Versions.overlaps(minVersion, maxVersion, d.getMinVersion(), d.getMaxVersion()))
				.map(ExecutorServiceDefinition::getExecutor)
				.filter(e -> !e.isLocked())
				.distinct()
				.toList();
	}

	// Removes an executor unless it is working; past session steps keep their history without the executor link.
	@Transactional(rollbackFor = Exception.class)
	public void remove(final Executor executor) {
		if (sessionStepRepository.existsByExecutorAndStatusIn(executor, List.of(SessionStepStatus.WAITING, SessionStepStatus.RUNNING))) {
			throw new InvalidParameterException("Executor " + executor.getName() + " is working");
		}
		executor.setLocked(true);
		executorRepository.saveAndFlush(executor);
		for (final SessionStep step : sessionStepRepository.findAllByExecutor(executor)) {
			step.setExecutor(null);
		}
		sessionStepRepository.flush();
		serviceDefinitionRepository.deleteAll(serviceDefinitionRepository.findAllByExecutor(executor));
		executorRepository.delete(executor);
		executorRepository.flush();
	}
}
