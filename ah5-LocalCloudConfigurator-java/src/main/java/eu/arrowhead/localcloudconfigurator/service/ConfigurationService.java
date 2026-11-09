package eu.arrowhead.localcloudconfigurator.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.localcloudconfigurator.Catalog;
import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.Planner;
import eu.arrowhead.localcloudconfigurator.Refused;
import eu.arrowhead.localcloudconfigurator.jpa.entity.SavedConfiguration;
import eu.arrowhead.localcloudconfigurator.jpa.repository.SavedConfigurationRepository;
import eu.arrowhead.localcloudconfigurator.service.dto.ConfigurationDraft;
import eu.arrowhead.localcloudconfigurator.service.dto.ConfigurationRecord;
import eu.arrowhead.localcloudconfigurator.service.dto.LayoutPoint;

/** Saved designs (selections and graphical positions) with revision checks; saving never changes the runtime. */
@Service
public class ConfigurationService {

	private static final int MAX_ENTRIES = 1000;

	private final Catalog catalog;
	private final SavedConfigurationRepository repository;

	public ConfigurationService(final Catalog catalog, final SavedConfigurationRepository repository) {
		this.catalog = catalog;
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> list() {
		final List<ConfigurationRecord> rows = new ArrayList<>(repository.findAll().stream().limit(MAX_ENTRIES).map(ConfigurationService::record).toList());
		rows.sort(Comparator.comparing(ConfigurationRecord::name));
		return Map.of("entries", rows);
	}

	@Transactional
	public synchronized ConfigurationRecord create(final ConfigurationDraft draft) {
		final ConfigurationRecord c = configuration(UUID.randomUUID().toString(), draft, 1);
		repository.saveAndFlush(entity(c));
		return c;
	}

	@Transactional(readOnly = true)
	public ConfigurationRecord get(final String id) {
		return record(find(id));
	}

	@Transactional
	public synchronized ConfigurationRecord update(final String id, final ConfigurationDraft draft) {
		final SavedConfiguration old = find(id);
		if (draft.expectedRevision() == null || draft.expectedRevision() != old.getRevision()) {
			throw new Refused(409, "Configuration changed; reload before saving");
		}

		final ConfigurationRecord next = configuration(id, draft, old.getRevision() + 1);
		old.update(next.name(), Json.write(next.systems()), Json.write(next.remove()), next.reapply(), next.smoke(), Json.write(next.layout()), next.revision(), next.updatedAt());
		repository.saveAndFlush(old);
		return next;
	}

	@Transactional
	public synchronized Map<String, Object> remove(final String id, final JsonNode body) {
		final SavedConfiguration old = find(id);
		if (body == null || body.path("expectedRevision").asLong(-1) != old.getRevision()) {
			throw new Refused(409, "Configuration changed; reload before deleting");
		}

		repository.delete(old);
		repository.flush();
		return Map.of("removed", id);
	}

	private SavedConfiguration find(final String id) {
		return repository.findById(id).orElseThrow(() -> new Refused(404, "Configuration not found"));
	}

	// validation and normalization of the native 5.2.2 API: the plan must build, defaults are always selected, layout only for catalog ids
	private ConfigurationRecord configuration(final String id, final ConfigurationDraft d, final long revision) {
		if (d.name() == null || d.name().isBlank() || d.name().length() > 200) {
			throw new IllegalArgumentException("Configuration name required (1–200 characters)");
		}

		final Planner.Request req = new Planner.Request(d.systems(), d.remove(), d.reapply(), d.smoke());
		Planner.build(catalog, req, List.of());
		final List<String> selected = new ArrayList<>(new LinkedHashSet<>(catalog.defaults()));
		for (final String raw : req.systems()) {
			final String s = catalog.hostId(raw);
			if (!selected.contains(s)) {
				selected.add(s);
			}
		}

		final Map<String, LayoutPoint> layout = new LinkedHashMap<>();
		if (d.layout() != null) {
			if (d.layout().size() > 200) {
				throw new IllegalArgumentException("Too many layout nodes");
			}

			for (final Map.Entry<String, LayoutPoint> e : d.layout().entrySet()) {
				if (!catalog.hostId(e.getKey()).equals(e.getKey())) {
					continue;
				}

				catalog.entry(e.getKey());
				final LayoutPoint p = e.getValue();
				if (p == null || !Double.isFinite(p.x()) || !Double.isFinite(p.y()) || p.x() < 0 || p.y() < 0 || p.x() > 10000 || p.y() > 10000) {
					throw new IllegalArgumentException("Invalid graphical position");
				}

				layout.put(e.getKey(), p);
			}
		}

		return new ConfigurationRecord(id, d.name().trim(), selected, req.remove(), req.reapply(), req.smoke(), layout, revision, Instant.now().toString());
	}

	private static SavedConfiguration entity(final ConfigurationRecord c) {
		return new SavedConfiguration(c.id(), c.name(), Json.write(c.systems()), Json.write(c.remove()), c.reapply(), c.smoke(), Json.write(c.layout()), c.revision(), c.updatedAt());
	}

	private static ConfigurationRecord record(final SavedConfiguration e) {
		try {
			return new ConfigurationRecord(e.getId(), e.getName(),
					Json.LENIENT.readValue(e.getSystems(), new TypeReference<List<String>>() { }),
					Json.LENIENT.readValue(e.getRemovals(), new TypeReference<List<String>>() { }),
					e.isReapply(), e.isSmoke(),
					Json.LENIENT.readValue(e.getLayout(), new TypeReference<LinkedHashMap<String, LayoutPoint>>() { }),
					e.getRevision(), e.getUpdatedAt());
		} catch (final Exception ex) {
			throw new IllegalStateException("Stored configuration " + e.getId() + " is unreadable", ex);
		}
	}
}
