/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.solocalcloudconfigurator.Json;
import eu.arrowhead.solocalcloudconfigurator.Refused;
import eu.arrowhead.solocalcloudconfigurator.SoLocalCloudConfiguratorConstants;
import eu.arrowhead.solocalcloudconfigurator.catalog.PolicyCatalog;
import eu.arrowhead.solocalcloudconfigurator.design.DesignValidator;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.LcDesignRef;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Member;
import eu.arrowhead.solocalcloudconfigurator.jpa.entity.SolcDesignEntity;
import eu.arrowhead.solocalcloudconfigurator.jpa.repository.SolcDesignRepository;
import eu.arrowhead.solocalcloudconfigurator.policy.PolicyEngine;
import eu.arrowhead.solocalcloudconfigurator.service.dto.DesignDraft;
import eu.arrowhead.solocalcloudconfigurator.service.dto.DesignRecord;

/**
 * Stored SoLC designs with optimistic revisions, and their plans. A member's local-cloud design snapshot (components at a
 * revision) always comes from LocalCloudConfigurator, never from the request: a member keeps the snapshot already stored
 * for the same design id and revision, and otherwise the design is read from LocalCloudConfigurator, which must still be at
 * the revision the operator chose. The inter-cloud infrastructure a member adds on top of its design (Gatekeeper, GateTunnel
 * and what they require) is resolved here from the LocalCloudConfigurator catalog in the same way.
 */
@Service
public class DesignService {

	private final SolcDesignRepository repository;
	private final LcDesignClient lcc;
	private final PolicyCatalog catalog;
	private final PolicyEngine engine;

	public DesignService(final SolcDesignRepository repository, final LcDesignClient lcc, final PolicyCatalog catalog, final PolicyEngine engine) {
		this.repository = repository;
		this.lcc = lcc;
		this.catalog = catalog;
		this.engine = engine;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> list() {
		final List<Map<String, Object>> rows = new ArrayList<>();
		for (final SolcDesignEntity e : repository.findAll()) {
			final SolcDesign d = design(e);
			final Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", e.getId());
			m.put("name", e.getName());
			m.put("revision", e.getRevision());
			m.put("updatedBy", e.getUpdatedBy());
			m.put("updatedAt", e.getUpdatedAt());
			m.put("members", d.membersOrEmpty().stream().map(Member::cloud).toList());
			m.put("exchanges", d.exchangesOrEmpty().size());
			rows.add(m);
		}
		rows.sort(Comparator.comparing(m -> m.get("name").toString()));
		return Map.of("entries", rows);
	}

	@Transactional(readOnly = true)
	public DesignRecord get(final String id) {
		return record(find(id));
	}

	@Transactional
	public synchronized DesignRecord create(final DesignDraft draft, final String caller) {
		if (repository.count() >= SoLocalCloudConfiguratorConstants.MAX_DESIGNS) {
			throw new Refused(409, "At most " + SoLocalCloudConfiguratorConstants.MAX_DESIGNS + " SoLC designs");
		}
		final SolcDesign d = resolve(draft == null ? null : draft.design(), null);
		requireValid(d);
		if (repository.findByName(d.name()).isPresent()) {
			throw new Refused(409, "An SoLC design named " + d.name() + " exists");
		}
		final SolcDesignEntity e = new SolcDesignEntity(UUID.randomUUID().toString(), d.name(), Json.write(d), 1, caller, Instant.now().toString());
		repository.saveAndFlush(e);
		return record(e);
	}

	@Transactional
	public synchronized DesignRecord update(final String id, final DesignDraft draft, final String caller) {
		final SolcDesignEntity old = find(id);
		if (draft == null || draft.expectedRevision() == null || draft.expectedRevision() != old.getRevision()) {
			throw new Refused(409, "SoLC design changed; reload before saving");
		}
		final SolcDesign d = resolve(draft.design(), design(old));
		requireValid(d);
		repository.findByName(d.name()).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
			throw new Refused(409, "An SoLC design named " + d.name() + " exists");
		});
		old.update(d.name(), Json.write(d), old.getRevision() + 1, caller, Instant.now().toString());
		repository.saveAndFlush(old);
		return record(old);
	}

	@Transactional
	public synchronized Map<String, Object> remove(final String id, final JsonNode body) {
		final SolcDesignEntity old = find(id);
		if (body == null || body.path("expectedRevision").asLong(-1) != old.getRevision()) {
			throw new Refused(409, "SoLC design changed; reload before deleting");
		}
		repository.delete(old);
		repository.flush();
		return Map.of("removed", id);
	}

	/**
	 * Plan of a draft (or of the stored design designId): structural problems, policy evaluation and per-cloud
	 * configuration, and the members whose local-cloud design changed in LocalCloudConfigurator since it was pinned.
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> plan(final DesignDraft draft) {
		final SolcDesign previous = draft != null && draft.designId() != null ? design(find(draft.designId())) : null;
		final SolcDesign d = draft != null && draft.design() != null ? resolve(draft.design(), previous) : previous;
		if (d == null) {
			throw new IllegalArgumentException("design or designId required");
		}
		return plan(d);
	}

	public Map<String, Object> plan(final SolcDesign d) {
		final Map<String, Object> out = new LinkedHashMap<>();
		final List<String> problems = DesignValidator.problems(d, catalog);
		out.put("problems", problems);
		final PolicyEngine.Evaluation evaluation = engine.evaluate(d);
		out.put("allowed", problems.isEmpty() && evaluation.allowed());
		out.put("evaluation", evaluation);
		out.put("stale", stale(d));
		return out;
	}

	/** Members pinned to an older revision than LocalCloudConfigurator now holds; null if LocalCloudConfigurator is unavailable. */
	List<Map<String, Object>> stale(final SolcDesign d) {
		final Map<String, Long> current;
		try {
			current = lcc.revisions();
		} catch (final Refused ex) {
			return null;
		}
		final List<Map<String, Object>> out = new ArrayList<>();
		for (final Member m : d.membersOrEmpty()) {
			if (m != null && m.lcDesign() != null) {
				final Long now = current.get(m.lcDesign().id());
				if (now == null || now != m.lcDesign().revision()) {
					final Map<String, Object> s = new LinkedHashMap<>();
					s.put("cloud", m.cloud());
					s.put("lcDesign", m.lcDesign().name());
					s.put("pinned", m.lcDesign().revision());
					s.put("current", now);
					out.add(s);
				}
			}
		}
		return out;
	}

	public SolcDesign design(final String id) {
		return design(find(id));
	}

	public long revision(final String id) {
		return find(id).getRevision();
	}

	// members take their LC design snapshot from the previous version (same id and revision) or from LocalCloudConfigurator
	SolcDesign resolve(final SolcDesign d, final SolcDesign previous) {
		if (d == null) {
			throw new IllegalArgumentException("design required");
		}
		if (d.membersOrEmpty().size() > DesignValidator.MAX_MEMBERS) {
			throw new IllegalArgumentException("a System of LocalClouds has at most " + DesignValidator.MAX_MEMBERS + " member clouds");
		}
		final Map<String, LcDesignRef> pinned = new LinkedHashMap<>();
		if (previous != null) {
			previous.membersOrEmpty().stream().filter(m -> m.lcDesign() != null)
					.forEach(m -> pinned.put(m.lcDesign().id() + "@" + m.lcDesign().revision(), m.lcDesign()));
		}
		// inter-cloud infrastructure already resolved for a pinned design and the same request
		final Map<String, List<String>> added = new LinkedHashMap<>();
		if (previous != null) {
			previous.membersOrEmpty().stream().filter(m -> m.lcDesign() != null && !requested(m).isEmpty())
					.forEach(m -> added.put(intercloudKey(m.lcDesign(), requested(m)), m.intercloudOrEmpty()));
		}
		final JsonNode[] lcCatalog = new JsonNode[1];
		final Map<String, LcDesignRef> fetched = new LinkedHashMap<>();
		final List<Member> members = new ArrayList<>();
		for (final Member m : d.membersOrEmpty()) {
			if (m == null || m.lcDesign() == null || m.lcDesign().id() == null) {
				members.add(m);
				continue;
			}
			final LcDesignRef want = m.lcDesign();
			LcDesignRef ref = pinned.get(want.id() + "@" + want.revision());
			if (ref == null) {
				ref = fetched.computeIfAbsent(want.id(), lcc::snapshot);
				if (want.revision() > 0 && want.revision() != ref.revision()) {
					throw new Refused(409, (m.cloud() == null ? "member" : m.cloud()) + ": local-cloud design " + ref.name() + " is now at revision "
							+ ref.revision() + " (requested " + want.revision() + "); choose it again");
				}
			}
			members.add(new Member(m.cloud(), ref, m.host(), m.architecture(), m.portOffset(), intercloud(m, ref, added, lcCatalog)));
		}
		return new SolcDesign(d.name(), d.organization(), d.relay(), members, d.exchanges(), d.verifyDiscovery(), d.packaging(), d.layout());
	}

	// the policy's inter-cloud components among those asked for; what they require is derived, never taken from the request
	private List<String> requested(final Member m) {
		return m.intercloudOrEmpty().stream().filter(c -> c != null && catalog.intercloudComponents().contains(c)).distinct().sorted().toList();
	}

	private static String intercloudKey(final LcDesignRef ref, final List<String> requested) {
		return ref.id() + "@" + ref.revision() + "+" + String.join(",", requested);
	}

	/**
	 * The inter-cloud infrastructure a member adds to its local-cloud design: the requested inter-cloud components and
	 * their requirements in the LocalCloudConfigurator catalog, without what the design already has. Null when nothing is added.
	 */
	private List<String> intercloud(final Member m, final LcDesignRef ref, final Map<String, List<String>> resolved, final JsonNode[] lcCatalog) {
		final List<String> requested = requested(m);
		if (requested.isEmpty()) {
			return null;
		}
		final List<String> add = resolved.computeIfAbsent(intercloudKey(ref, requested), k -> {
			if (lcCatalog[0] == null) {
				lcCatalog[0] = lcc.catalog();
			}
			return LcDesignClient.closure(lcCatalog[0], requested).stream().filter(c -> !ref.components().contains(c)).toList();
		});
		return add.isEmpty() ? null : add;
	}

	private void requireValid(final SolcDesign d) {
		final List<String> problems = DesignValidator.problems(d, catalog);
		if (!problems.isEmpty()) {
			throw new IllegalArgumentException(String.join("; ", problems));
		}
	}

	private SolcDesignEntity find(final String id) {
		return repository.findById(id).orElseThrow(() -> new Refused(404, "SoLC design not found"));
	}

	private static SolcDesign design(final SolcDesignEntity e) {
		try {
			return Json.LENIENT.readValue(e.getDesign(), SolcDesign.class);
		} catch (final Exception ex) {
			throw new IllegalStateException("Stored SoLC design " + e.getId() + " is unreadable", ex);
		}
	}

	private static DesignRecord record(final SolcDesignEntity e) {
		return new DesignRecord(e.getId(), e.getRevision(), e.getUpdatedBy(), e.getUpdatedAt(), design(e));
	}
}
