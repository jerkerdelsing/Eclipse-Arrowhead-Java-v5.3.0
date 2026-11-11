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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.solocalcloudconfigurator.Json;
import eu.arrowhead.solocalcloudconfigurator.Refused;
import eu.arrowhead.solocalcloudconfigurator.SoLocalCloudConfiguratorConstants;
import eu.arrowhead.solocalcloudconfigurator.design.DesignValidator;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.LcDesignRef;

/**
 * Reads the stored local-cloud designs of LocalCloudConfigurator through orchestration (localCloudConfiguration catalog,
 * list-configurations, get-configuration; each needs a ConsumerAuthorization grant). A design's components are the catalog
 * closure of its selection (required components included), which is what a member cloud of the SoLC runs.
 */
@Service
public class LcDesignClient {

	@Autowired
	private ArrowheadHttpService http;

	/** The LocalCloudConfigurator catalog: component ids, names, kinds, Compose services and requirements. */
	public JsonNode catalog() {
		return call(SoLocalCloudConfiguratorConstants.LCC_OP_CATALOG, null);
	}

	/** The stored designs with their components: {cloud, components: [{id, name, kind}], entries: [{id, name, revision, updatedAt, components}]}. */
	public Map<String, Object> list() {
		final JsonNode catalog = catalog();
		final JsonNode list = call(SoLocalCloudConfiguratorConstants.LCC_OP_LIST_CONFIGURATIONS, null);
		final List<Map<String, Object>> entries = new ArrayList<>();
		for (final JsonNode e : list.path("entries")) {
			final Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", e.path("id").asText());
			m.put("name", e.path("name").asText());
			m.put("revision", e.path("revision").asLong());
			m.put("updatedAt", e.path("updatedAt").asText());
			m.put("components", closure(catalog, e.path("systems")));
			entries.add(m);
		}
		final List<Map<String, Object>> components = new ArrayList<>();
		for (final JsonNode c : catalog.path("systems")) {
			final Map<String, Object> m = new LinkedHashMap<>();
			m.put("id", c.path("id").asText());
			m.put("name", c.path("name").asText());
			m.put("kind", c.path("kind").asText());
			components.add(m);
		}
		final Map<String, Object> out = new LinkedHashMap<>();
		out.put("cloud", catalog.path("cloud").asText());
		out.put("components", components);
		out.put("entries", entries);
		return out;
	}

	/** Snapshot of one stored design at its current revision. */
	public LcDesignRef snapshot(final String id) {
		if (id == null || !DesignValidator.ID.matcher(id).matches()) {
			throw new IllegalArgumentException("Invalid local-cloud design id");
		}
		final JsonNode catalog = catalog();
		final JsonNode d = call(SoLocalCloudConfiguratorConstants.LCC_OP_GET_CONFIGURATION, List.of(id));
		return new LcDesignRef(d.path("id").asText(), d.path("name").asText(), d.path("revision").asLong(), closure(catalog, d.path("systems")));
	}

	/** Current revision of each stored design (id -> revision). */
	public Map<String, Long> revisions() {
		final Map<String, Long> out = new LinkedHashMap<>();
		for (final JsonNode e : call(SoLocalCloudConfiguratorConstants.LCC_OP_LIST_CONFIGURATIONS, null).path("entries")) {
			out.put(e.path("id").asText(), e.path("revision").asLong());
		}
		return out;
	}

	static List<String> closure(final JsonNode catalog, final JsonNode selected) {
		final Map<String, List<String>> requires = new LinkedHashMap<>();
		for (final JsonNode c : catalog.path("systems")) {
			final List<String> r = new ArrayList<>();
			c.path("requires").forEach(x -> r.add(x.asText()));
			requires.put(c.path("id").asText(), r);
		}
		final Set<String> out = new LinkedHashSet<>();
		for (final JsonNode s : selected) {
			visit(s.asText(), requires, out, new HashSet<>());
		}
		return List.copyOf(out);
	}

	private static void visit(final String id, final Map<String, List<String>> requires, final Set<String> out, final Set<String> path) {
		if (out.contains(id)) {
			return;
		}
		if (!requires.containsKey(id)) {
			throw new Refused(409, "The LocalCloudConfigurator catalog has no component " + id + "; the stored design is out of date");
		}
		if (!path.add(id)) {
			throw new Refused(502, "Dependency cycle in the LocalCloudConfigurator catalog: " + id);
		}
		for (final String r : requires.get(id)) {
			visit(r, requires, out, path);
		}
		path.remove(id);
		out.add(id);
	}

	private JsonNode call(final String operation, final List<String> pathParams) {
		final String body;
		try {
			body = http.consumeService(SoLocalCloudConfiguratorConstants.SERVICE_DEF_LOCAL_CLOUD_CONFIGURATION, operation,
					SoLocalCloudConfiguratorConstants.LCC_SYSTEM_NAME, String.class, null, null, pathParams, null);
		} catch (final DataNotFoundException ex) {
			throw new Refused(pathParams == null ? 503 : 404, pathParams == null
					? "Orchestration found no authorized provider of localCloudConfiguration (" + operation + ")"
					: "Local-cloud design not found in LocalCloudConfigurator");
		} catch (final AuthException | ForbiddenException ex) {
			throw new Refused(503, "SoLocalCloudConfigurator may not use localCloudConfiguration (" + operation + "): " + ex.getMessage());
		} catch (final ArrowheadException ex) {
			throw new Refused(502, "LocalCloudConfigurator " + operation + " failed: " + ex.getMessage());
		}
		try {
			return Json.LENIENT.readTree(body == null ? "{}" : body);
		} catch (final Exception ex) {
			throw new Refused(502, "LocalCloudConfigurator returned invalid JSON");
		}
	}
}
