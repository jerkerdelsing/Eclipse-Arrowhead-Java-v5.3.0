/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sosconfigurator.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.SoSConfiguratorConstants;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.colony.ColonyClient;
import eu.arrowhead.sosconfigurator.jpa.entity.SosDeployment;
import eu.arrowhead.sosconfigurator.jpa.repository.SosDeploymentRepository;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;

// sosConfiguration: catalog, executor status, policy-checked plans and their submission as workflows.
@Service
public class SosConfigurationService {

	//=================================================================================================
	// members

	private static final ExecutorService WORKERS = Executors.newVirtualThreadPerTaskExecutor();

	private final Catalog catalog;
	private final Planner planner;
	private final SosBackend backend;
	private final SosDeploymentRepository deployments;
	private final Duration statusTimeout;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public SosConfigurationService(final Catalog catalog, final Planner planner, final SosBackend backend, final SosDeploymentRepository deployments,
			@Value(SoSConfiguratorConstants.$STATUS_TIMEOUT_SECONDS_WD) final long statusTimeoutSeconds) {
		this.catalog = catalog;
		this.planner = planner;
		this.backend = backend;
		this.deployments = deployments;
		this.statusTimeout = Duration.ofSeconds(statusTimeoutSeconds);
	}

	//-------------------------------------------------------------------------------------------------
	public Catalog catalog() {
		return catalog;
	}

	//-------------------------------------------------------------------------------------------------
	public Planner.Plan plan(final SosRequest request) {
		return planner.build(request);
	}

	//-------------------------------------------------------------------------------------------------
	// the workflow id; the submission is recorded with the submitter, the request and the plan
	public String submit(final Planner.Plan plan, final SosRequest request, final String submittedBy) throws IOException {
		final String id = backend.submit(plan, request);
		deployments.saveAndFlush(new SosDeployment(id, plan.sos(), submittedBy, Instant.now(), Json.write(request), Json.write(plan)));
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public ColonyClient.Deployment deployment(final String id) throws IOException {
		return backend.deployment(id);
	}

	//-------------------------------------------------------------------------------------------------
	// the most recent submissions, newest first
	public List<Map<String, Object>> submissions() {
		return deployments.findTop100ByOrderBySubmittedAtDesc().stream().map(d -> {
			final Map<String, Object> view = new LinkedHashMap<>();
			view.put("id", d.getId());
			view.put("sos", d.getSosName());
			view.put("submittedBy", d.getSubmittedBy());
			view.put("submittedAt", d.getSubmittedAt().toString());
			return view;
		}).toList();
	}

	//-------------------------------------------------------------------------------------------------
	// status of the relay and every catalogued cloud, asked in parallel; an executor that does not answer is offline
	public Map<String, Object> status() {
		final Map<String, CompletableFuture<Object>> futures = new LinkedHashMap<>();
		for (final String role : catalog.roles()) {
			futures.put(role, CompletableFuture.supplyAsync(() -> {
				try {
					return (Object) Json.LENIENT.readTree(backend.status(role, statusTimeout));
				} catch (final IOException ex) {
					return Map.of("state", "offline", "error", String.valueOf(ex.getMessage()));
				}
			}, WORKERS));
		}

		final Map<String, Object> clouds = new LinkedHashMap<>();
		final Map<String, Object> result = new LinkedHashMap<>();
		futures.forEach((role, future) -> {
			final Object value = future.join();
			if ("relay".equals(role)) {
				result.put("relay", value);
			} else {
				clouds.put(role.substring("cloud:".length()), value);
			}
		});
		result.put("clouds", clouds);

		return result;
	}
}
