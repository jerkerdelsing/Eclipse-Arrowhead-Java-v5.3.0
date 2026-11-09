package eu.arrowhead.localcloudconfigurator.it;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.common.testhelper.StandaloneModeInTests;
import eu.arrowhead.localcloudconfigurator.Catalog;
import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.LocalCloudConfiguratorMain;
import eu.arrowhead.localcloudconfigurator.Planner;
import eu.arrowhead.localcloudconfigurator.jpa.repository.ActiveDeploymentRepository;
import eu.arrowhead.localcloudconfigurator.jpa.repository.DeploymentRepository;
import eu.arrowhead.localcloudconfigurator.jpa.repository.SavedConfigurationRepository;
import eu.arrowhead.localcloudconfigurator.security.ConfiguratorAuthorizationService;
import eu.arrowhead.localcloudconfigurator.service.ConfiguratorBackend;

// Against a fresh MySQL schema created by deployment/database/init.sql and restricted by least-privilege.sh (Hibernate validates the
// schema; the account may only read and change rows). Run with -Dlcc.it.db.url=... -Dlcc.it.db.password=...; skipped otherwise.
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "lcc.it.db.url", matches = ".+")
// The whole application (common authentication and management filters, the localCloudConfiguration access filter, controllers, JPA
// on an in-memory database) in the development profile with declared identities ("Bearer SYSTEM//<name>"). Every /api route of the
// native 5.2.2 API with its status codes and JSON shapes; the execution backend is a stub.
@SpringBootTest(classes = { LocalCloudConfiguratorMain.class, MySqlEndToEndTest.Fixtures.class },
		properties = {
				"spring.config.location=classpath:/application.properties",
				"arrowhead.security.profile=development",
				"service.authorization.enabled=true",
				"spring.datasource.url=${lcc.it.db.url}",
				"spring.datasource.username=localcloudconfigurator",
				"spring.datasource.password=${lcc.it.db.password}",
				"spring.jpa.hibernate.ddl-auto=validate"
		})
@AutoConfigureMockMvc
public class MySqlEndToEndTest {

	private static final String SYSOP = "Bearer SYSTEM//Sysop";
	private static final String MANAGER = "Bearer SYSTEM//LocalCloudManager";
	private static final String OTHER = "Bearer SYSTEM//DataManager";

	static final AtomicReference<String> PHASE = new AtomicReference<>("running");
	static final AtomicInteger SUBMISSIONS = new AtomicInteger();

	@Autowired
	private MockMvc mvc;

	@Autowired
	private Catalog catalog;

	@Autowired
	private DeploymentRepository deployments;

	@Autowired
	private ActiveDeploymentRepository active;

	@Autowired
	private SavedConfigurationRepository saved;

	@MockitoBean
	private ConfiguratorAuthorizationService authorization;

	@BeforeEach
	public void reset() {
		deployments.deleteAll();
		active.deleteAll();
		saved.deleteAll();
		PHASE.set("running");
		when(authorization.isAuthorized(eq("LocalCloudManager"), eq("localCloudConfiguration"), org.mockito.ArgumentMatchers.anyString())).thenReturn(true);
		when(authorization.isAuthorized(eq("DataManager"), eq("localCloudConfiguration"), org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
	}

	private static MockHttpServletRequestBuilder json(final MockHttpServletRequestBuilder request, final String identity, final String body) {
		return request.header("Authorization", identity).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	@Test
	public void catalogAndStatus() throws Exception {
		mvc.perform(get("/api/catalog").header("Authorization", MANAGER))
				.andExpect(status().isOk())
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(jsonPath("$.cloud").value(catalog.cloud()))
				.andExpect(jsonPath("$.systems", hasSize(catalog.systems().size())));
		mvc.perform(get("/api/status").header("Authorization", MANAGER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cloud").value(catalog.cloud()))
				.andExpect(jsonPath("$.running", hasSize(2)))
				.andExpect(jsonPath("$.systems[0].state").exists());
	}

	@Test
	public void reviewedDeploymentIsPersistedAndSerialized() throws Exception {
		final MvcResult planned = mvc.perform(json(post("/api/plan"), MANAGER, "{\"systems\":[],\"remove\":[],\"smoke\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.plan.steps").isArray())
				.andReturn();
		final String hash = Json.LENIENT.readTree(planned.getResponse().getContentAsString()).path("hash").asText();

		mvc.perform(json(post("/api/deployments"), MANAGER, "{\"smoke\":true,\"expectedPlanHash\":\"stale\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Plan or cloud state changed. Review a fresh plan before applying."));
		final String body = "{\"smoke\":true,\"expectedPlanHash\":\"" + hash + "\"}";
		final MvcResult submitted = mvc.perform(json(post("/api/deployments"), MANAGER, body))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.state").value("submitted"))
				.andReturn();
		final String id = Json.LENIENT.readTree(submitted.getResponse().getContentAsString()).path("id").asText();
		assertTrue(deployments.existsById(id));
		assertEquals(id, active.findById(1).orElseThrow().getDeploymentId());

		// a second deployment waits for the running one
		mvc.perform(json(post("/api/deployments"), MANAGER, body)).andExpect(status().isConflict());

		mvc.perform(get("/api/deployments/" + id).header("Authorization", MANAGER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.state").value("running"));
		PHASE.set("success");
		mvc.perform(get("/api/deployments/" + id).header("Authorization", MANAGER)).andExpect(jsonPath("$.state").value("success"));
		assertEquals("success", deployments.findById(id).orElseThrow().getState());
		assertTrue(deployments.findById(id).orElseThrow().getResult().contains("success"));

		mvc.perform(json(post("/api/deployments"), MANAGER, body)).andExpect(status().isAccepted());
		mvc.perform(get("/api/deployments/foreign").header("Authorization", MANAGER))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Deployment not found"));
	}

	@Test
	public void nothingToDoIsAnImmediateSuccess() throws Exception {
		final String request = "{\"systems\":[],\"remove\":[],\"smoke\":false}";
		final String hash = Json.LENIENT.readTree(mvc.perform(json(post("/api/plan"), MANAGER, request)).andReturn().getResponse().getContentAsString()).path("hash").asText();
		final JsonNode plan = Json.LENIENT.readTree(mvc.perform(json(post("/api/plan"), MANAGER, request)).andReturn().getResponse().getContentAsString()).path("plan");
		if (plan.path("steps").isEmpty()) {
			mvc.perform(json(post("/api/deployments"), MANAGER, "{\"expectedPlanHash\":\"" + hash + "\"}"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(""))
					.andExpect(jsonPath("$.state").value("success"));
		}
	}

	@Test
	public void savedDesignsWithRevisions() throws Exception {
		final MvcResult created = mvc.perform(json(post("/api/configurations"), MANAGER,
				"{\"name\":\"Test design\",\"systems\":[],\"remove\":[],\"layout\":{\"serviceregistry\":{\"x\":1,\"y\":2}}}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.revision").value(1))
				.andExpect(jsonPath("$.layout.serviceregistry.x").value(1.0))
				.andReturn();
		final JsonNode row = Json.LENIENT.readTree(created.getResponse().getContentAsString());
		final String id = row.path("id").asText();
		assertEquals(4, row.path("systems").size());

		mvc.perform(get("/api/configurations").header("Authorization", MANAGER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.entries", hasSize(1)))
				.andExpect(jsonPath("$.entries[0].name").value("Test design"));
		mvc.perform(get("/api/configurations/" + id).header("Authorization", MANAGER)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
		mvc.perform(json(put("/api/configurations/" + id), MANAGER, "{\"name\":\"stale\",\"expectedRevision\":0}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Configuration changed; reload before saving"));
		mvc.perform(json(put("/api/configurations/" + id), MANAGER, "{\"name\":\"edited\",\"expectedRevision\":1}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("edited"))
				.andExpect(jsonPath("$.revision").value(2));
		mvc.perform(json(delete("/api/configurations/" + id), MANAGER, "{\"expectedRevision\":1}")).andExpect(status().isConflict());
		mvc.perform(json(delete("/api/configurations/" + id), MANAGER, "{\"expectedRevision\":2}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.removed").value(id));
		mvc.perform(get("/api/configurations/" + id).header("Authorization", MANAGER))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Configuration not found"));
	}

	@Test
	public void invalidInputIsRefused() throws Exception {
		mvc.perform(json(post("/api/plan"), MANAGER, "{\"systems\":[\"not-catalogued\"]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Unknown component: not-catalogued"));
		mvc.perform(json(post("/api/plan"), MANAGER, "{\"unknownField\":true}")).andExpect(status().isBadRequest());
		mvc.perform(json(post("/api/configurations"), MANAGER, "{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Configuration name required (1–200 characters)"));
		mvc.perform(json(post("/api/configurations"), MANAGER, "{\"name\":\"x\",\"layout\":{\"serviceregistry\":{\"x\":-1,\"y\":2}}}"))
				.andExpect(status().isBadRequest());
		mvc.perform(json(post("/api/plan"), MANAGER, "{\"systems\":[\"" + "a".repeat(70000) + "\"]}"))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.error").value("Request exceeds 64 KiB"));
	}

	@Test
	public void accessNeedsAnIdentityAndAGrant() throws Exception {
		mvc.perform(get("/api/catalog")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/catalog").header("Authorization", OTHER)).andExpect(status().isForbidden());
		mvc.perform(json(post("/api/plan"), OTHER, "{}")).andExpect(status().isForbidden());
		mvc.perform(get("/api/catalog").header("Authorization", SYSOP)).andExpect(status().isOk());
		// paths that are no operation are refused before any handler
		mvc.perform(get("/api/other").header("Authorization", MANAGER)).andExpect(status().isForbidden());
		// generalManagement: Sysop only
		mvc.perform(get("/localcloudconfigurator/general/mgmt/get-config").param("keys", "lcc.backend").header("Authorization", SYSOP))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.map['lcc.backend']").value("direct"));
		mvc.perform(get("/localcloudconfigurator/general/mgmt/get-config").header("Authorization", MANAGER)).andExpect(status().isForbidden());
	}

	@Autowired
	private javax.sql.DataSource dataSource;

	@Test
	public void accountHasDataManipulationPrivilegesOnly() throws Exception {
		try (var connection = dataSource.getConnection(); var statement = connection.createStatement(); var grants = statement.executeQuery("SHOW GRANTS")) {
			final StringBuilder all = new StringBuilder();
			while (grants.next()) {
				all.append(grants.getString(1)).append('\n');
			}
			final String text = all.toString();
			assertTrue(text.contains("GRANT SELECT, INSERT, UPDATE, DELETE ON `ah\\_localcloudconfigurator`.*") || text.contains("GRANT SELECT, INSERT, UPDATE, DELETE ON `ah_localcloudconfigurator`.*"), text);
			for (final String ddl : java.util.List.of("CREATE,", "DROP", "ALTER", "ALL PRIVILEGES", "INDEX", "REFERENCES")) {
				org.junit.jupiter.api.Assertions.assertFalse(text.contains(ddl), ddl + " in " + text);
			}
			org.junit.jupiter.api.Assertions.assertThrows(java.sql.SQLException.class, () -> statement.execute("CREATE TABLE probe (id int)"));
		}
	}

	@TestConfiguration
	static class Fixtures {

		@Bean
		StandaloneModeInTests standaloneMode() {
			return new StandaloneModeInTests() {
			};
		}

		@Bean
		@Primary
		ConfiguratorBackend stubBackend() {
			return new ConfiguratorBackend() {
				@Override
				public List<String> status() {
					return List.of("serviceregistry", "serviceregistry-db");
				}

				@Override
				public String submit(final Planner.Plan plan) {
					return "local-00000000-0000-0000-0000-00000000000" + SUBMISSIONS.incrementAndGet() % 10;
				}

				@Override
				public Object deployment(final String id) {
					return Map.of("id", id, "state", PHASE.get(), "steps", List.of(Map.of("node", "s1", "state", PHASE.get())));
				}
			};
		}
	}
}
