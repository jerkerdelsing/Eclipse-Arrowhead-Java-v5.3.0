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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.solocalcloudconfigurator.Refused;
import eu.arrowhead.solocalcloudconfigurator.SoLocalCloudConfiguratorConstants;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign;
import eu.arrowhead.solocalcloudconfigurator.jpa.entity.SolcPackageEntity;
import eu.arrowhead.solocalcloudconfigurator.jpa.repository.SolcPackageRepository;
import eu.arrowhead.solocalcloudconfigurator.packaging.KitResources;
import eu.arrowhead.solocalcloudconfigurator.packaging.PackageBuilder;
import eu.arrowhead.solocalcloudconfigurator.policy.PolicyEngine;
import eu.arrowhead.solocalcloudconfigurator.service.dto.PackageRecord;
import eu.arrowhead.solocalcloudconfigurator.service.dto.PackageRequest;

/** Builds and stores deployment packages of stored SoLC designs; only an allowed plan with current LC design pins is packaged. */
@Service
public class PackageService {

	public record Download(String fileName, String sha256, byte[] content) {
	}

	private final SolcPackageRepository repository;
	private final DesignService designs;
	private final LcDesignClient lcc;
	private final PolicyEngine engine;
	private final String sourceRepository;
	private final String sourceCommit;

	public PackageService(final SolcPackageRepository repository, final DesignService designs, final LcDesignClient lcc, final PolicyEngine engine,
			@Value(SoLocalCloudConfiguratorConstants.$SOURCE_REPOSITORY_WD) final String sourceRepository,
			@Value(SoLocalCloudConfiguratorConstants.$SOURCE_COMMIT_WD) final String sourceCommit) {
		this.repository = repository;
		this.designs = designs;
		this.lcc = lcc;
		this.engine = engine;
		this.sourceRepository = sourceRepository;
		this.sourceCommit = sourceCommit;
	}

	@Transactional(readOnly = true)
	public Map<String, Object> list() {
		final List<PackageRecord> rows = new ArrayList<>();
		for (final Object[] r : repository.listMetadata()) {
			rows.add(new PackageRecord((String) r[0], (String) r[1], (String) r[2], ((Number) r[3]).longValue(), (String) r[4],
					Arrays.asList(((String) r[5]).split(",")), (String) r[6], (String) r[7], ((Number) r[8]).longValue(), (String) r[9], (String) r[10]));
		}
		return Map.of("entries", rows);
	}

	@Transactional
	public synchronized PackageRecord build(final PackageRequest request, final String caller) throws Exception {
		if (request == null || request.designId() == null) {
			throw new IllegalArgumentException("designId required");
		}
		if (repository.count() >= SoLocalCloudConfiguratorConstants.MAX_PACKAGES) {
			throw new Refused(409, "At most " + SoLocalCloudConfiguratorConstants.MAX_PACKAGES + " packages; remove old ones first");
		}
		final long revision = designs.revision(request.designId());
		if (request.expectedRevision() == null || request.expectedRevision() != revision) {
			throw new Refused(409, "SoLC design changed; reload before building a package");
		}
		final SolcDesign d = designs.design(request.designId());
		final Map<String, Object> plan = designs.plan(d);
		if (!Boolean.TRUE.equals(plan.get("allowed"))) {
			throw new Refused(422, "The SoLC plan is not allowed; validate it and resolve its problems and violations first");
		}
		final Object stale = plan.get("stale");
		if (stale == null) {
			throw new Refused(503, "LocalCloudConfigurator is unavailable; the pinned local-cloud designs cannot be checked");
		}
		if (!((List<?>) stale).isEmpty()) {
			throw new Refused(409, "Local-cloud designs changed since they were pinned; choose them again in the SoLC view: " + stale);
		}
		if ("build".equals(d.packaging().imageMode()) && (sourceCommit == null || !sourceCommit.matches("[0-9a-f]{40}"))) {
			throw new Refused(409, "A build-on-target package needs the source commit of this system (solc.source.commit)");
		}
		final JsonNode lcCatalog = lcc.catalog();
		final String now = Instant.now().toString();
		final PackageBuilder.Result r = new PackageBuilder(KitResources.load()).build(new PackageBuilder.Input(request.designId(), revision, d,
				engine.evaluate(d), lcCatalog, sourceRepository, sourceCommit == null || sourceCommit.isBlank() ? null : sourceCommit, caller, now));
		final SolcPackageEntity e = new SolcPackageEntity(UUID.randomUUID().toString(), request.designId(), d.name(), revision, d.packaging().imageMode(),
				String.join(",", d.packaging().architectures()), r.fileName(), r.sha256(), r.content(), caller, now);
		repository.saveAndFlush(e);
		return record(e);
	}

	@Transactional(readOnly = true)
	public Download get(final String id) {
		final SolcPackageEntity e = find(id);
		return new Download(e.getFileName(), e.getSha256(), e.getContent());
	}

	@Transactional
	public synchronized Map<String, Object> remove(final String id) {
		repository.delete(find(id));
		repository.flush();
		return Map.of("removed", id);
	}

	private SolcPackageEntity find(final String id) {
		return repository.findById(id).orElseThrow(() -> new Refused(404, "Package not found"));
	}

	private static PackageRecord record(final SolcPackageEntity e) {
		return new PackageRecord(e.getId(), e.getDesignId(), e.getDesignName(), e.getDesignRevision(), e.getImageMode(), Arrays.asList(e.getArchitectures().split(",")),
				e.getFileName(), e.getSha256(), e.getSizeBytes(), e.getCreatedBy(), e.getCreatedAt());
	}
}
