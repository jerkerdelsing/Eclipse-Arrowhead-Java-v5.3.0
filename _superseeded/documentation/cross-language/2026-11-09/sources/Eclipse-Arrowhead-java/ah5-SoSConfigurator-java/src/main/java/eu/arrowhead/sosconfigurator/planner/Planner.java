package eu.arrowhead.sosconfigurator.planner;

import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.policy.PolicyEngine;
import eu.arrowhead.sosconfigurator.policy.PolicyEngine.CloudConfig;
import eu.arrowhead.sosconfigurator.policy.PolicyEngine.Evaluation;
import eu.arrowhead.sosconfigurator.policy.SosRequest;

/**
 * Turns an SoS request into an ordered list of steps. All checks run before anything is changed, so a cloud or relay
 * that does not meet the security preconditions stops the workflow with no configuration applied.
 */
public final class Planner {

	/** Step kinds; each is one ColonyOS process for the executor of its role. */
	public static final String RELAY_CHECK = "relay-check";
	public static final String CLOUD_CHECK = "cloud-check";
	public static final String NEIGHBOURS = "neighbours";
	public static final String GRANTS = "grants";
	public static final String VERIFY = "verify";
	public static final String DISCOVER = "discover";

	public record Step(String kind, String role, String target) {
	}

	public record Plan(String sos, List<String> members, boolean allowed, List<PolicyEngine.Violation> violations, List<Step> steps,
			List<String> notes, Evaluation evaluation) {
	}

	private final Catalog catalog;
	private final PolicyEngine engine;

	public Planner(final Catalog catalog) {
		this.catalog = catalog;
		this.engine = new PolicyEngine(catalog);
	}

	public Plan build(final SosRequest req) {
		final Evaluation ev = engine.evaluate(req);
		final List<String> notes = new ArrayList<>();
		if (!ev.allowed()) {
			return new Plan(ev.sos(), ev.members(), false, ev.violations(), List.of(), notes, ev);
		}
		final List<Step> steps = new ArrayList<>();
		steps.add(new Step(RELAY_CHECK, "relay", catalog.relay().name()));
		for (final String c : ev.clouds().keySet()) {
			steps.add(new Step(CLOUD_CHECK, "cloud:" + c, c));
		}
		for (final String c : ev.clouds().keySet()) {
			steps.add(new Step(NEIGHBOURS, "cloud:" + c, c));
		}
		for (final String c : ev.clouds().keySet()) {
			steps.add(new Step(GRANTS, "cloud:" + c, c));
		}
		for (final String c : ev.clouds().keySet()) {
			steps.add(new Step(VERIFY, "cloud:" + c, c));
		}
		for (final CloudConfig cc : ev.clouds().values()) {
			if (!cc.discovery().isEmpty()) {
				steps.add(new Step(DISCOVER, "cloud:" + cc.cloud(), cc.cloud()));
			}
			if (cc.grants().isEmpty() && cc.neighbours().isEmpty()) {
				notes.add(cc.cloud() + ": no exchanges; its inter-cloud neighbours and grants of catalogued clouds are withdrawn");
			}
		}
		catalog.clouds().stream().filter(c -> !ev.members().contains(c.name()))
				.forEach(c -> notes.add(c.name() + ": not a member; left unchanged"));
		return new Plan(ev.sos(), ev.members(), true, List.of(), steps, notes, ev);
	}
}
