package eu.arrowhead.sosconfigurator.colony;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.planner.Planner;
import eu.arrowhead.sosconfigurator.policy.SosRequest;
import eu.arrowhead.sosconfigurator.runner.StepRunner;

/** The configurator's use of ColonyOS: submitting SoS plans and status queries, and the executor loop. */
public class SosColony {

	private static final String LABEL = "sosconfigurator";

	private final ColonyClient client;
	private final Catalog catalog;

	public SosColony(final ColonyClient client, final Catalog catalog) {
		this.client = client;
		this.catalog = catalog;
	}

	/** Submits the plan's steps as one chained workflow; each step goes to the executor type of its role. */
	public String submit(final Planner.Plan plan, final SosRequest req) throws IOException {
		if (!plan.allowed() || plan.steps().isEmpty()) {
			throw new IllegalArgumentException("plan is not allowed or has no steps");
		}
		final String sos = Json.write(req);
		final List<ObjectNode> specs = new ArrayList<>();
		final List<String> nodes = new ArrayList<>();
		int i = 0;
		for (final Planner.Step s : plan.steps()) {
			i++;
			final Map<String, Object> kw = new LinkedHashMap<>();
			kw.put("kind", s.kind());
			kw.put("target", s.target());
			kw.put("role", s.role());
			kw.put("sos", sos);
			specs.add(client.functionSpec(StepRunner.FUNC_STEP, catalog.executorType(s.role()), kw, -1, 600, LABEL));
			nodes.add(String.format("s%02d-%s-%s", i, s.kind(), s.target().toLowerCase()));
		}
		return client.submitChain(specs, nodes);
	}

	public ColonyClient.Deployment deployment(final String id) throws IOException {
		return client.deployment(id);
	}

	/** Asks the executor of one role for its status; the answer is the executor's JSON string. */
	public String status(final String role, final Duration timeout) throws IOException {
		final ObjectNode fs = client.functionSpec(StepRunner.FUNC_STATUS, catalog.executorType(role), Map.of("role", role),
				(int) timeout.toSeconds(), 60, LABEL);
		ColonyClient.Process p = client.submit(fs);
		final Instant deadline = Instant.now().plus(timeout).plusSeconds(5);
		while (Instant.now().isBefore(deadline)) {
			try {
				Thread.sleep(300);
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("interrupted", e);
			}
			p = client.getProcess(p.id());
			if (p.state() == ColonyClient.FAILED) {
				throw new IOException("status failed: " + p.errors());
			}
			if (p.state() == ColonyClient.SUCCESS) {
				return p.output().isEmpty() ? "{}" : String.valueOf(p.output().get(0));
			}
		}
		throw new IOException("no executor of type " + catalog.executorType(role) + " answered");
	}

	/** Executor loop: serves sos.status and sos.step for one role until the thread is interrupted. */
	public void serve(final StepRunner runner, final Consumer<String> log) {
		while (!Thread.currentThread().isInterrupted()) {
			final ColonyClient.Process p;
			try {
				p = client.assign(10);
			} catch (final IOException e) {
				log.accept("assign: " + e.getMessage());
				sleep(3000);
				continue;
			}
			if (p != null) {
				handle(runner, p, log);
			}
		}
	}

	private void handle(final StepRunner runner, final ColonyClient.Process p, final Consumer<String> log) {
		try {
			switch (p.funcName()) {
			case StepRunner.FUNC_STATUS -> client.close(p.id(), List.of(runner.status()));
			case StepRunner.FUNC_STEP -> {
				final String kind = String.valueOf(p.kwargs().get("kind"));
				final Object role = p.kwargs().get("role");
				if (!runner.role().equals(role)) {
					throw new IOException("process for role " + role + " reached the executor of " + runner.role());
				}
				log.accept("step " + kind + " " + p.kwargs().get("target") + " (" + p.id() + ")");
				final String out = runner.step(kind, String.valueOf(p.kwargs().get("sos")));
				if (!out.isBlank()) {
					client.addLog(p.id(), tail(out, 8000));
				}
				client.close(p.id(), List.of(tail(out, 2000)));
			}
			default -> throw new IOException("unknown function " + p.funcName());
			}
		} catch (final Exception e) {
			final String reason = StepRunner.Redact.redact(String.valueOf(e.getMessage()));
			log.accept("process " + p.id() + " failed: " + reason);
			try {
				if (e instanceof StepRunner.StepFailed f && !f.output().isBlank()) {
					client.addLog(p.id(), tail(f.output(), 8000));
					client.fail(p.id(), List.of(reason + ": " + tail(f.output(), 600)));
				} else {
					client.fail(p.id(), List.of(reason));
				}
			} catch (final IOException fe) {
				log.accept("fail " + p.id() + ": " + fe.getMessage());
			}
		}
	}

	private static String tail(final String s, final int n) {
		return s.length() <= n ? s : "…" + s.substring(s.length() - n);
	}

	private static void sleep(final long ms) {
		try {
			Thread.sleep(ms);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
