package eu.arrowhead.localcloudconfigurator.api.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

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
import eu.arrowhead.localcloudconfigurator.Catalog;
import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.LocalCloudConfiguratorConstants;
import eu.arrowhead.localcloudconfigurator.Planner;
import eu.arrowhead.localcloudconfigurator.Refused;
import eu.arrowhead.localcloudconfigurator.service.ConfigurationService;
import eu.arrowhead.localcloudconfigurator.service.DeploymentCoordinator;
import eu.arrowhead.localcloudconfigurator.service.dto.ApplyRequest;
import eu.arrowhead.localcloudconfigurator.service.dto.ConfigurationDraft;
import eu.arrowhead.localcloudconfigurator.service.dto.ConfigurationRecord;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

/**
 * localCloudConfiguration: the paths, JSON shapes and status codes of the native 5.2.2 operator API. Request bodies are read with the
 * strict mapper (unknown fields are errors) and limited to 64 KiB.
 */
@RestController
@RequestMapping(path = LocalCloudConfiguratorConstants.HTTP_API_BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class LocalCloudConfigurationAPI {

	private static final String DEPLOYMENT = "{id:" + LocalCloudConfiguratorConstants.DEPLOYMENT_ID_PATTERN + "}";
	private static final String CONFIGURATION = "{id:" + LocalCloudConfiguratorConstants.CONFIGURATION_ID_PATTERN + "}";

	private final Catalog catalog;
	private final DeploymentCoordinator coordinator;
	private final ConfigurationService configurations;

	public LocalCloudConfigurationAPI(final Catalog catalog, final DeploymentCoordinator coordinator, final ConfigurationService configurations) {
		this.catalog = catalog;
		this.coordinator = coordinator;
		this.configurations = configurations;
	}

	@GetMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_CATALOG_PATH)
	public Catalog catalog() {
		return catalog;
	}

	@GetMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_STATUS_PATH)
	public Map<String, Object> status() throws Exception {
		return coordinator.status();
	}

	@PostMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_PLAN_PATH)
	public Map<String, Object> plan(final HttpServletRequest request) throws Exception {
		return coordinator.plan(read(request, Planner.Request.class));
	}

	@PostMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_DEPLOYMENTS_PATH)
	public ResponseEntity<Map<String, Object>> deploy(final HttpServletRequest request) throws Exception {
		final DeploymentCoordinator.Result result = coordinator.apply(read(request, ApplyRequest.class));
		return ResponseEntity.status(result.status()).body(result.body());
	}

	@GetMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_DEPLOYMENTS_PATH + "/" + DEPLOYMENT)
	public Object deployment(@PathVariable final String id) throws Exception {
		return coordinator.deployment(id);
	}

	@GetMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_CONFIGURATIONS_PATH)
	public Map<String, Object> configurations() {
		return configurations.list();
	}

	@PostMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_CONFIGURATIONS_PATH)
	public ResponseEntity<ConfigurationRecord> create(final HttpServletRequest request) throws IOException {
		return ResponseEntity.status(201).body(configurations.create(read(request, ConfigurationDraft.class)));
	}

	@GetMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_CONFIGURATIONS_PATH + "/" + CONFIGURATION)
	public ConfigurationRecord configuration(@PathVariable final String id) {
		return configurations.get(id);
	}

	@PutMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_CONFIGURATIONS_PATH + "/" + CONFIGURATION)
	public ConfigurationRecord update(@PathVariable final String id, final HttpServletRequest request) throws IOException {
		final ConfigurationDraft draft = read(request, ConfigurationDraft.class);
		return configurations.update(id, draft);
	}

	@DeleteMapping(LocalCloudConfiguratorConstants.HTTP_API_OP_CONFIGURATIONS_PATH + "/" + CONFIGURATION)
	public Map<String, Object> remove(@PathVariable final String id, final HttpServletRequest request) throws IOException {
		return configurations.remove(id, read(request, JsonNode.class));
	}

	static <T> T read(final HttpServletRequest request, final Class<T> type) throws IOException {
		final byte[] body;
		try (InputStream in = request.getInputStream()) {
			body = in.readNBytes(LocalCloudConfiguratorConstants.MAX_BODY_BYTES + 1);
		}
		if (body.length > LocalCloudConfiguratorConstants.MAX_BODY_BYTES) {
			throw new Refused(413, "Request exceeds 64 KiB");
		}

		return Json.MAPPER.readValue(body, type);
	}
}
