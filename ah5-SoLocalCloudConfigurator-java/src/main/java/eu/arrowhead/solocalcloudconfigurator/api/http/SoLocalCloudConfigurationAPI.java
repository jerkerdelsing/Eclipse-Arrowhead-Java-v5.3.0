/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.api.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.common.Constants;
import eu.arrowhead.solocalcloudconfigurator.Json;
import eu.arrowhead.solocalcloudconfigurator.Refused;
import eu.arrowhead.solocalcloudconfigurator.SoLocalCloudConfiguratorConstants;
import eu.arrowhead.solocalcloudconfigurator.catalog.PolicyCatalog;
import eu.arrowhead.solocalcloudconfigurator.service.DesignService;
import eu.arrowhead.solocalcloudconfigurator.service.LcDesignClient;
import eu.arrowhead.solocalcloudconfigurator.service.PackageService;
import eu.arrowhead.solocalcloudconfigurator.service.dto.DesignDraft;
import eu.arrowhead.solocalcloudconfigurator.service.dto.DesignRecord;
import eu.arrowhead.solocalcloudconfigurator.service.dto.PackageRecord;
import eu.arrowhead.solocalcloudconfigurator.service.dto.PackageRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

/**
 * soLocalCloudConfiguration: Systems of LocalClouds built from LocalCloudConfigurator's stored local-cloud designs, their
 * policy plans and deployment packages. Request bodies are read with the strict mapper (unknown fields are errors) and
 * limited to 256 KiB.
 */
@RestController
@RequestMapping(path = SoLocalCloudConfiguratorConstants.HTTP_API_BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class SoLocalCloudConfigurationAPI {

	private static final String ID = "{id:" + SoLocalCloudConfiguratorConstants.ID_PATTERN + "}";

	private final PolicyCatalog catalog;
	private final LcDesignClient lcc;
	private final DesignService designs;
	private final PackageService packages;

	public SoLocalCloudConfigurationAPI(final PolicyCatalog catalog, final LcDesignClient lcc, final DesignService designs, final PackageService packages) {
		this.catalog = catalog;
		this.lcc = lcc;
		this.designs = designs;
		this.packages = packages;
	}

	@GetMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_CATALOG_PATH)
	public PolicyCatalog catalog() {
		return catalog;
	}

	@GetMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_LC_DESIGNS_PATH)
	public Map<String, Object> lcDesigns() {
		return lcc.list();
	}

	@GetMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGNS_PATH)
	public Map<String, Object> list() {
		return designs.list();
	}

	@PostMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGNS_PATH)
	public ResponseEntity<DesignRecord> create(final HttpServletRequest request) throws IOException {
		return ResponseEntity.status(201).body(designs.create(read(request, DesignDraft.class), caller(request)));
	}

	@GetMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGNS_PATH + "/" + ID)
	public DesignRecord get(@PathVariable final String id) {
		return designs.get(id);
	}

	@PutMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGNS_PATH + "/" + ID)
	public DesignRecord update(@PathVariable final String id, final HttpServletRequest request) throws IOException {
		return designs.update(id, read(request, DesignDraft.class), caller(request));
	}

	@DeleteMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_DESIGNS_PATH + "/" + ID)
	public Map<String, Object> remove(@PathVariable final String id, final HttpServletRequest request) throws IOException {
		return designs.remove(id, read(request, JsonNode.class));
	}

	@PostMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_PLAN_PATH)
	public Map<String, Object> plan(final HttpServletRequest request) throws IOException {
		return designs.plan(read(request, DesignDraft.class));
	}

	@GetMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_PACKAGES_PATH)
	public Map<String, Object> packages() {
		return packages.list();
	}

	@PostMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_PACKAGES_PATH)
	public ResponseEntity<PackageRecord> build(final HttpServletRequest request) throws Exception {
		return ResponseEntity.status(201).body(packages.build(read(request, PackageRequest.class), caller(request)));
	}

	@GetMapping(path = SoLocalCloudConfiguratorConstants.HTTP_API_OP_PACKAGES_PATH + "/" + ID, produces = { "application/gzip", MediaType.APPLICATION_JSON_VALUE })
	public ResponseEntity<byte[]> download(@PathVariable final String id) {
		final PackageService.Download d = packages.get(id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/gzip"))
				.cacheControl(CacheControl.noStore())
				.header("Content-Disposition", ContentDisposition.attachment().filename(d.fileName()).build().toString())
				.header("X-Content-SHA256", d.sha256())
				.body(d.content());
	}

	@DeleteMapping(SoLocalCloudConfiguratorConstants.HTTP_API_OP_PACKAGES_PATH + "/" + ID)
	public Map<String, Object> removePackage(@PathVariable final String id) {
		return packages.remove(id);
	}

	private static String caller(final HttpServletRequest request) {
		final Object system = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		final String s = system == null ? Constants.UNKNOWN : system.toString();
		return s.length() > 64 ? s.substring(0, 64) : s;
	}

	static <T> T read(final HttpServletRequest request, final Class<T> type) throws IOException {
		final byte[] body;
		try (InputStream in = request.getInputStream()) {
			body = in.readNBytes(SoLocalCloudConfiguratorConstants.MAX_BODY_BYTES + 1);
		}
		if (body.length > SoLocalCloudConfiguratorConstants.MAX_BODY_BYTES) {
			throw new Refused(413, "Request exceeds 256 KiB");
		}
		if (body.length == 0) {
			throw new IllegalArgumentException("Request body required");
		}

		return Json.MAPPER.readValue(body, type);
	}
}
