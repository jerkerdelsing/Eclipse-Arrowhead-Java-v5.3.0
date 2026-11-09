package eu.arrowhead.localcloudconfigurator.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.arrowhead.localcloudconfigurator.Catalog;
import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.Planner;
import eu.arrowhead.localcloudconfigurator.Refused;
import eu.arrowhead.localcloudconfigurator.service.dto.ApplyRequest;

/**
 * Status, plan review and serialized deployment of reviewed plans (the deployment part of the native 5.2.2 ApiServer). Used by the
 * Arrowhead API (records in MySQL, execution by the host worker) and by the host worker itself (file records, DirectBackend).
 */
public class DeploymentCoordinator {

	public record Result(int status, Map<String, Object> body) {
	}

	private final Catalog catalog;
	private final ConfiguratorBackend backend;
	private final DeploymentStore store;
	private final Object lock = new Object();
	private List<String> cached;
	private long cachedAt;

	public DeploymentCoordinator(final Catalog catalog, final ConfiguratorBackend backend, final DeploymentStore store) {
		this.catalog = catalog;
		this.backend = backend;
		this.store = store;
		final String active = store.activeDeployment();
		if ("direct".equals(backend.kind()) && active != null && !active.startsWith("local-")) {
			// a ColonyOS workflow of the previous backend: its record stays, it no longer blocks direct deployments
			store.setActiveDeployment(null);
		}
	}

	public Catalog catalog() {
		return catalog;
	}

	public String backendKind() {
		return backend.kind();
	}

	public Map<String, Object> status() throws Exception {
		final List<String> run = running(false);
		final List<Map<String, Object>> systems = new ArrayList<>();
		for (final Catalog.Component c : catalog.systems()) {
			final long n = c.services().stream().filter(run::contains).count();
			systems.add(Map.of("id", c.id(), "state", n == 0 ? "stopped" : n == c.services().size() ? "running" : "partial"));
		}

		final Map<String, Object> out = new LinkedHashMap<>();
		out.put("cloud", catalog.cloud());
		out.put("running", run);
		out.put("systems", systems);
		return out;
	}

	public Map<String, Object> plan(final Planner.Request request) throws Exception {
		final Planner.Plan p = Planner.build(catalog, request, running(true));
		return Map.of("plan", p, "hash", p.hash());
	}

	public Result apply(final ApplyRequest r) throws Exception {
		synchronized (lock) {
			final String active = store.activeDeployment();
			if (active != null) {
				final String phase = Json.LENIENT.valueToTree(backend.deployment(active)).path("state").asText();
				if (!Set.of("success", "failed").contains(phase)) {
					throw new Refused(409, "A deployment is still running. Wait for it before applying another plan.");
				}

				store.setActiveDeployment(null);
			}

			final Planner.Plan p = Planner.build(catalog, r.request(), running(true));
			if (r.expectedPlanHash() == null || !p.hash().equals(r.expectedPlanHash())) {
				throw new Refused(409, "Plan or cloud state changed. Review a fresh plan before applying.");
			}

			if (p.steps().isEmpty()) {
				return new Result(200, Map.of("id", "", "plan", p, "state", "success"));
			}

			final String id = backend.submit(p, r.request());
			if (id == null || !id.matches("[A-Za-z0-9-]{1,128}")) {
				throw new IllegalStateException("Unexpected workflow ID");
			}

			store.submitted(id, p, r.request());
			store.setActiveDeployment(id);
			return new Result(202, Map.of("id", id, "plan", p, "state", "submitted"));
		}
	}

	public Object deployment(final String id) throws Exception {
		if (!store.exists(id)) {
			throw new Refused(404, "Deployment not found");
		}

		final Object result = backend.deployment(id);
		store.observed(id, result);
		return result;
	}

	private synchronized List<String> running(final boolean fresh) throws Exception {
		final long now = System.nanoTime();
		if (fresh || cached == null || now - cachedAt > 2_000_000_000L) {
			cached = List.copyOf(backend.status());
			cachedAt = System.nanoTime();
		}

		return cached;
	}
}
