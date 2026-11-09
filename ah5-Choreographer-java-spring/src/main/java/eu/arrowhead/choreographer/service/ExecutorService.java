/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import eu.arrowhead.choreographer.api.dto.ExecutedStepResultDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorCreateRequestDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorListResponseDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorRegistrationRequestDTO;
import eu.arrowhead.choreographer.api.dto.ExecutorResponseDTO;
import eu.arrowhead.choreographer.engine.ChoreographerEngine;
import eu.arrowhead.choreographer.jpa.entity.Executor;
import eu.arrowhead.choreographer.jpa.entity.SessionStep;
import eu.arrowhead.choreographer.jpa.service.ExecutorDbService;
import eu.arrowhead.choreographer.jpa.service.SessionDbService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.SystemResponseDTO;

// Executors are Arrowhead systems already registered in ServiceRegistry (v5 systems register themselves);
// the Choreographer only records which step service definitions they execute and where their executor API is.
@Service
public class ExecutorService {

	@Autowired
	private ExecutorDbService executorDb;

	@Autowired
	private SessionDbService sessionDb;

	@Autowired
	private ChoreographerDriver driver;

	@Autowired
	private ChoreographerEngine engine;

	@Autowired
	private DTOConverter converter;

	@Autowired
	private PageService pageService;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefinitionNameNormalizer;

	@Autowired
	private ServiceDefinitionNameValidator serviceDefinitionNameValidator;

	// ---------------------------------------------------------------------------------------- choreographerExecutor

	public ExecutorResponseDTO register(final String requester, final ExecutorRegistrationRequestDTO request, final String origin) {
		if (request == null) {
			throw new InvalidParameterException("Request is null", origin);
		}
		return createOrUpdate(requester, request.address(), request.port(), request.basePath(), request.serviceDefinitionName(), request.minVersion(), request.maxVersion(), origin);
	}

	public void unregister(final String requester) {
		executorDb.findByName(requester).ifPresent(executorDb::remove);
	}

	// Executor reports the result of a step; only the executor that runs the step may report it.
	public void notifyStepDone(final String requester, final ExecutedStepResultDTO result, final String origin) {
		if (result == null) {
			throw new InvalidParameterException("Payload is null", origin);
		}
		if (result.sessionId() == null || result.sessionId() <= 0) {
			throw new InvalidParameterException("Invalid session id", origin);
		}
		if (result.sessionStepId() == null || result.sessionStepId() <= 0) {
			throw new InvalidParameterException("Invalid session step id", origin);
		}
		if (result.status() == null) {
			throw new InvalidParameterException("Status is null", origin);
		}
		if (result.status().isError() && Utilities.isEmpty(result.message())) {
			throw new InvalidParameterException("Message is null or blank", origin);
		}
		final SessionStep step;
		try {
			step = sessionDb.getSessionStep(result.sessionStepId());
		} catch (final DataNotFoundException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
		if (step.getSession().getId() != result.sessionId()) {
			throw new InvalidParameterException("Session step " + result.sessionStepId() + " does not belong to session " + result.sessionId(), origin);
		}
		if (step.getExecutor() == null || !step.getExecutor().getName().equals(requester)) {
			throw new ForbiddenException(requester + " is not the executor of session step " + result.sessionStepId(), origin);
		}
		engine.stepDoneAsync(result);
	}

	// ---------------------------------------------------------------------------------------- choreographerManagement

	public ExecutorResponseDTO create(final ExecutorCreateRequestDTO request, final String origin) {
		if (request == null || Utilities.isEmpty(request.systemName())) {
			throw new InvalidParameterException("System name is empty", origin);
		}
		return createOrUpdate(request.systemName().trim(), request.address(), request.port(), request.basePath(), request.serviceDefinitionName(),
				request.minVersion(), request.maxVersion(), origin);
	}

	public void remove(final long id) {
		executorDb.findById(id).ifPresent(executorDb::remove);
	}

	public ExecutorResponseDTO get(final long id) {
		return converter.executor(executorDb.findById(id).orElseThrow(() -> new DataNotFoundException("Executor not found: " + id)));
	}

	public ExecutorListResponseDTO query(final PageDTO page, final String origin) {
		final Page<Executor> executors = executorDb.getExecutors(pageService.getPageRequest(page, Executor.SORTABLE_FIELDS_BY, "id", origin));
		return new ExecutorListResponseDTO(executors.getContent().stream().map(converter::executor).toList(), executors.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	private ExecutorResponseDTO createOrUpdate(final String systemName, final String address, final Integer port, final String basePath, final String serviceDefinitionName,
			final Integer minVersion, final Integer maxVersion, final String origin) {
		if (port == null || port < Constants.MIN_PORT || port > Constants.MAX_PORT) {
			throw new InvalidParameterException("Port must be between " + Constants.MIN_PORT + " and " + Constants.MAX_PORT, origin);
		}
		if (Utilities.isEmpty(serviceDefinitionName)) {
			throw new InvalidParameterException("Service definition is empty", origin);
		}
		final String serviceDefinition = serviceDefinitionNameNormalizer.normalize(serviceDefinitionName);
		try {
			serviceDefinitionNameValidator.validateServiceDefinitionName(serviceDefinition);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
		if (minVersion == null || maxVersion == null) {
			throw new InvalidParameterException("minVersion and maxVersion are required", origin);
		}
		if (minVersion < 0 || minVersion > maxVersion) {
			throw new InvalidParameterException("minVersion cannot be negative or greater than maxVersion", origin);
		}
		final String path = Utilities.isEmpty(basePath) ? "" : Utilities.stripEndSlash(basePath.trim().startsWith("/") ? basePath.trim() : "/" + basePath.trim());

		// the executor must be a system known to ServiceRegistry, and may only claim one of its registered addresses
		final Optional<SystemResponseDTO> system = driver.lookupSystem(systemName);
		if (system.isEmpty()) {
			throw new InvalidParameterException("System " + systemName + " is not registered in ServiceRegistry", origin);
		}
		final List<String> addresses = system.get().addresses() == null ? List.of() : system.get().addresses().stream().map(AddressDTO::address).toList();
		final String chosen = Utilities.isEmpty(address) ? (addresses.isEmpty() ? null : addresses.get(0)) : address.trim();
		if (chosen == null || !addresses.contains(chosen)) {
			throw new InvalidParameterException("Address " + chosen + " is not a registered address of " + systemName, origin);
		}

		return converter.executor(executorDb.createOrUpdate(systemName, chosen, port, path, serviceDefinition, minVersion, maxVersion));
	}
}
