package eu.arrowhead.localcloudmanager.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.common.testhelper.StandaloneModeInTests;
import eu.arrowhead.localcloudmanager.LocalCloudManagerMain;
import eu.arrowhead.localcloudmanager.client.DockerGateway;
import eu.arrowhead.localcloudmanager.client.ProviderGateway;
import eu.arrowhead.localcloudmanager.console.ConsoleAuth;
import eu.arrowhead.localcloudmanager.security.ManagerAuthorizationService;
import eu.arrowhead.localcloudmanager.service.Json;

// The whole application: the console connector (password session in Spring Security, Host/Origin checks, the static UI) and the
// Arrowhead connector (common declared-identity filter, management filter, localCloudManagement access filter), in front of every
// /api route of the native console API. Providers, the ServiceRegistry and the Docker proxy are stubs.
@SpringBootTest(classes = { LocalCloudManagerMain.class, ConsoleApiTest.Fixtures.class },
		properties = {
				"spring.config.location=classpath:/application.properties",
				"arrowhead.security.profile=development",
				"service.authorization.enabled=true",
				"console.port=8490",
				"server.port=8491",
				"lcm.compose-project=cloud-a",
				"lcm.managed-services=sysmonitor,datamanager"
		})
@AutoConfigureMockMvc
public class ConsoleApiTest {

	private static final String PASSWORD = "correct horse battery";
	static final String HASH = ConsoleAuth.hash(PASSWORD);
	private static final String UUID = "123e4567-e89b-12d3-a456-426614174000";
	private static final String CONTAINER_ID = "abcdefabcdef0123456789";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ProviderGateway providers;

	@MockitoBean
	private DockerGateway docker;

	@MockitoBean
	private ManagerAuthorizationService authorization;

	private final List<String> calls = new CopyOnWriteArrayList<>();
	private final List<String> dockerCalls = new CopyOnWriteArrayList<>();

	private static JsonNode j(final String json) {
		try {
			return Json.MAPPER.readTree(json.replace('\'', '"'));
		} catch (final Exception ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	@org.springframework.test.context.DynamicPropertySource
	static void consolePassword(final org.springframework.test.context.DynamicPropertyRegistry registry) {
		registry.add("console.password.hash", () -> HASH);
	}

	@BeforeEach
	public void stubs() throws Exception {
		when(authorization.isAuthorized(eq("LocalCloudManagerProbe"), anyString(), anyString())).thenReturn(true);
		when(providers.serviceRegistry(anyString(), any())).thenAnswer(call -> {
			final String path = call.getArgument(0);
			if (path.contains("devices")) {
				return j("{'entries':[{'name':'Device1','addresses':['10.0.0.1']}],'count':1}");
			}
			if (path.contains("systems")) {
				return j("{'entries':[{'name':'ServiceRegistry','version':'5.2.2','deviceName':'Device1','metadata':{'x509PublicKey':'k'}},{'name':'Consumer','metadata':{'x509PublicKey':'c','consumerInterfaceTemplates':'generic_https'}}],'count':2}");
			}
			return j("{'entries':[{'instanceId':'ServiceRegistry|temperature|1.0.0','provider':{'name':'ServiceRegistry'},'serviceDefinition':{'name':'temperature'},'interfaces':[{'templateName':'generic_https','protocol':'https','policy':'CERT_AUTH'}]}],'count':1}");
		});
		when(providers.call(anyString(), anyString(), any(), any(), any(), any())).thenAnswer(call -> {
			final String def = call.getArgument(0);
			final String op = call.getArgument(1);
			final String path = call.getArgument(3);
			calls.add(def + "." + op + (path == null ? "" : " " + path));
			return switch (def + "." + op) {
			case "dataHistorian.list-systems" -> j("{'systems':['RandomDataProducer']}");
			case "dataHistorian.list-services" -> j("{'services':['randomNumberProducer']}");
			case "dataHistorian.get-data" -> j("[{'bn':'urn:arrowhead:rdp:','bt':1000,'n':'value','v':42}]");
			case "sysMonitorHistory.list-systems" -> j("{'systems':[{'systemName':'DataManager','observationCount':3}],'retentionDays':7}");
			case "sysMonitorHistory.latest" -> j("{'latest':{'echo':{'status':'alive'}}}");
			case "sysMonitorHistory.query" -> j("{'observations':[{'observationType':'echo'}]}");
			case "sysMonitorManagement.providers" -> j("{'defaultSubscription':['echo'],'list':[{'systemName':'DataManager'}]}");
			case "sysMonitorManagement.subscriptions" -> j("{'list':[]}");
			case "sysMonitorManagement.status" -> j("{'amqp':{'connected':true}}");
			case "sysMonitorManagement.subscribe" -> j("{'systemName':'DataManager','enabled':true}");
			case "sysMonitorManagement.unsubscribe" -> null;
			case "gatekeeperManagement.status" -> j("{'relay':{'connected':true}}");
			case "gatekeeperManagement.clouds" -> j("{'entries':[{'name':'GoCloud','operator':'Company'}]}");
			case "gateTunnelManagement.status" -> j("{'relay':{'connected':true},'interCloud':{'sessions':[]}}");
			case "randomNumberProducer.latest" -> j("{'interCloud':{'status':'publishing'}}");
			case "plantDescriptionManagement.query" -> j("{'entries':[{'id':1,'updatedAt':'v1','plantDescription':'test','active':false,'include':[],'systems':[],'connections':[]}],'count':1}");
			case "plantDescriptionManagement.create" -> j("{'id':2}");
			case "plantDescriptionManagement.replace", "plantDescriptionManagement.update" -> j("{'id':1}");
			case "plantDescriptionManagement.remove" -> null;
			case "serviceOrchestrationSimpleStoreManagement.query" -> j("{'entries':[{'id':'" + UUID + "','consumer':'Consumer','serviceInstanceId':'ServiceRegistry|temperature|1.0.0','priority':1}],'count':1}");
			case "serviceOrchestrationSimpleStoreManagement.create" -> j("{'entries':[{'id':'new'}],'count':1}");
			case "serviceOrchestrationSimpleStoreManagement.remove" -> null;
			case "serviceOrchestrationSimpleStoreManagement.modify-priorities" -> j("{'entries':[{'id':'" + UUID + "','priority':2}]}");
			case "serviceOrchestrationHistoryManagement.query" -> j("{'entries':[{'requesterSystem':'Consumer','serviceDefinition':'temperature','message':'1 local result: ServiceRegistry','status':'DONE','createdAt':'2026-11-01T00:00:00Z'}]}");
			case "serviceOrchestrationPlantManagement.query" -> j("{'rules':[]}");
			case "authorizationManagement.query-policies" -> j("{'entries':[]}");
			case "authorizationManagement.grant-policies" -> j("{'entries':[{'instanceId':'MGMT|LOCAL|ServiceRegistry|SERVICE_DEF|temperature'}]}");
			default -> throw new IllegalStateException("unexpected " + def + "." + op);
			};
		});
		when(providers.exchange(eq("localCloudConfiguration"), anyString(), eq("LocalCloudConfigurator"), anyString(), anyString(), any())).thenAnswer(call -> {
			calls.add("localCloudConfiguration." + call.getArgument(1) + " " + call.getArgument(3));
			return "submit-deployment".equals(call.getArgument(1)) ? new ProviderGateway.Response(409, j("{'error':'Plan or cloud state changed. Review a fresh plan before applying.'}"))
					: new ProviderGateway.Response(200, j("{'ok':true}"));
		});
		when(docker.request(anyString(), anyString())).thenAnswer(call -> {
			final String path = call.getArgument(0);
			dockerCalls.add(call.getArgument(1) + " " + path);
			if (path.startsWith("/containers/json")) {
				return ("[{'Id':'" + CONTAINER_ID + "','Names':['/cloud-a-sysmonitor-1'],'Image':'img','State':'running','Status':'Up 1 hour (healthy)','Created':1700000000,"
						+ "'Labels':{'com.docker.compose.service':'sysmonitor'}}]").replace('\'', '"').getBytes(StandardCharsets.UTF_8);
			}
			if (path.contains("/logs")) {
				return "line one\nline two\n".getBytes(StandardCharsets.UTF_8);
			}
			return new byte[0];
		});
	}

	// ----------------------------------------------------------------------------------------------- request helpers

	private static MockHttpServletRequestBuilder console(final HttpMethod method, final String path) {
		return console(method, path, "127.0.0.1:8490");
	}

	private static MockHttpServletRequestBuilder console(final HttpMethod method, final String path, final String host) {
		return request(method, path).with(r -> {
			r.setLocalPort(8490);
			r.setServerPort(8490);
			r.setRemoteAddr("127.0.0.1");
			return r;
		}).header("Host", host);
	}

	private static MockHttpServletRequestBuilder arrowhead(final HttpMethod method, final String path) {
		return request(method, path).with(r -> {
			r.setLocalPort(8491);
			r.setServerPort(8491);
			return r;
		});
	}

	private String login() throws Exception {
		final MvcResult result = mvc.perform(console(HttpMethod.POST, "/api/session").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isNoContent())
				.andExpect(header().string("Set-Cookie", containsString("HttpOnly; SameSite=Strict")))
				.andReturn();
		final String cookie = result.getResponse().getHeader("Set-Cookie");
		return cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';'));
	}

	private MockHttpServletRequestBuilder as(final String session, final HttpMethod method, final String path) {
		return console(method, path).cookie(new jakarta.servlet.http.Cookie(ConsoleAuth.COOKIE, session));
	}

	private MockHttpServletRequestBuilder confirmed(final String session, final HttpMethod method, final String path, final String body) {
		return as(session, method, path).header("X-Arrowhead-Action", "confirm").contentType(MediaType.APPLICATION_JSON).content(body);
	}

	// ----------------------------------------------------------------------------------------------- console login

	@Test
	public void consoleServesTheUiAndHealthButApiOnlyAfterLogin() throws Exception {
		mvc.perform(console(HttpMethod.GET, "/")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(header().string("X-Frame-Options", "DENY"));
		mvc.perform(console(HttpMethod.GET, "/app.js")).andExpect(status().isOk()).andExpect(header().string("Content-Type", containsString("javascript")));
		mvc.perform(console(HttpMethod.GET, "/some/client/route")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
		mvc.perform(console(HttpMethod.GET, "/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"))
				.andExpect(jsonPath("$.system").value("LocalCloudManager")).andExpect(jsonPath("$.registration.registered").value(false));

		mvc.perform(console(HttpMethod.GET, "/api/overview")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Console login required"));
		mvc.perform(console(HttpMethod.GET, "/api/session")).andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(false))
				.andExpect(jsonPath("$.configured").value(true));
		mvc.perform(console(HttpMethod.POST, "/api/session").contentType(MediaType.TEXT_PLAIN).content("password=x")).andExpect(status().isUnsupportedMediaType());
		mvc.perform(console(HttpMethod.POST, "/api/session").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"wrong\"}"))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("Wrong password"));
		mvc.perform(console(HttpMethod.PUT, "/api/session")).andExpect(status().isMethodNotAllowed());

		final String session = login();
		mvc.perform(as(session, HttpMethod.GET, "/api/session")).andExpect(jsonPath("$.authenticated").value(true));
		mvc.perform(as(session, HttpMethod.GET, "/api/features")).andExpect(status().isOk()).andExpect(jsonPath("$.localCloudConfiguration").value(true))
				.andExpect(jsonPath("$.chipletFloorplans").value(false)).andExpect(jsonPath("$.managerName").value("LocalCloudManager"));
		mvc.perform(as(session, HttpMethod.DELETE, "/api/session")).andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(false))
				.andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
		mvc.perform(as(session, HttpMethod.GET, "/api/overview")).andExpect(status().isUnauthorized());
		mvc.perform(as("forged", HttpMethod.GET, "/api/overview")).andExpect(status().isUnauthorized());
	}

	@Test
	public void foreignHostOrOriginIsRefusedOnTheConsole() throws Exception {
		final String session = login();
		mvc.perform(console(HttpMethod.GET, "/api/overview", "attacker.example:8490").cookie(new jakarta.servlet.http.Cookie(ConsoleAuth.COOKIE, session))).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("Host or Origin is not an allowed console address"));
		mvc.perform(as(session, HttpMethod.GET, "/api/overview").header("Origin", "http://attacker.example")).andExpect(status().isForbidden());
		mvc.perform(console(HttpMethod.GET, "/", "rebound.example")).andExpect(status().isForbidden());
		mvc.perform(console(HttpMethod.POST, "/api/session").header("Origin", "null").contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"" + PASSWORD + "\"}"))
				.andExpect(status().isForbidden());
	}

	// ----------------------------------------------------------------------------------------------- every /api route

	@Test
	public void readRoutesKeepTheirShapes() throws Exception {
		final String s = login();
		mvc.perform(as(s, HttpMethod.GET, "/api/overview")).andExpect(status().isOk())
				.andExpect(jsonPath("$.cloud.name").value("cloud-a")).andExpect(jsonPath("$.cloud.registryReachable").value(true))
				.andExpect(jsonPath("$.cloud.dockerReachable").value(true)).andExpect(jsonPath("$.summary.running").value(1))
				.andExpect(jsonPath("$.systems[0].composeService").value("serviceregistry")).andExpect(jsonPath("$.containers[0].service").value("sysmonitor"))
				.andExpect(jsonPath("$.containers[0].managed").value(true)).andExpect(jsonPath("$.containers[0].health").value("healthy"))
				.andExpect(jsonPath("$.registryManagement.summary.registeredDevices").value(1)).andExpect(jsonPath("$.naming.valid").value(true))
				.andExpect(jsonPath("$.revocation.state").value("unavailable")).andExpect(jsonPath("$.orchestrations").isArray())
				.andExpect(jsonPath("$.history").isArray()).andExpect(jsonPath("$.errors.docker").doesNotExist());
		mvc.perform(as(s, HttpMethod.GET, "/api/service-registry-management")).andExpect(status().isOk()).andExpect(jsonPath("$.devices").isArray())
				.andExpect(jsonPath("$.summary.registeredSystems").value(2)).andExpect(jsonPath("$.cloud.name").value("cloud-a"));
		mvc.perform(as(s, HttpMethod.GET, "/api/datamanager?limit=10")).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.latest[0].name").value("urn:arrowhead:rdp:value")).andExpect(jsonPath("$.series[0].points[0].value").value(42))
				.andExpect(jsonPath("$.series[0].label").value("RandomDataProducer · value — local"));
		mvc.perform(as(s, HttpMethod.GET, "/api/consumers")).andExpect(status().isOk()).andExpect(jsonPath("$.localCloud").value("TestCloud"))
				.andExpect(jsonPath("$.rows[0].orchestrated").isArray());
		mvc.perform(as(s, HttpMethod.GET, "/api/intercloud")).andExpect(status().isOk()).andExpect(jsonPath("$.neighbourClouds[0]").value("GoCloud|Company"))
				.andExpect(jsonPath("$.relay.gatekeeper.connected").value(true)).andExpect(jsonPath("$.producer.status").value("publishing"));
		mvc.perform(as(s, HttpMethod.GET, "/api/sysmonitor?system=DataManager&limit=5")).andExpect(status().isOk())
				.andExpect(jsonPath("$.selectedSystem").value("DataManager")).andExpect(jsonPath("$.storedObservations").value(3))
				.andExpect(jsonPath("$.systems[0].latest.echo.status").value("alive")).andExpect(jsonPath("$.observations", hasSize(1)));
		assertTrue(calls.contains("sysMonitorHistory.query /systems/DataManager/observations?count=5"));
		mvc.perform(as(s, HttpMethod.GET, "/api/sysmonitor/subscriptions")).andExpect(status().isOk()).andExpect(jsonPath("$.providers[0].systemName").value("DataManager"))
				.andExpect(jsonPath("$.intake.connected").value(true));
		mvc.perform(as(s, HttpMethod.GET, "/api/plantdescriptions")).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.systems[0]").value("Consumer")).andExpect(jsonPath("$.services[0].interfaces[0]").value("generic_https"));
		mvc.perform(as(s, HttpMethod.GET, "/api/orchestration/rules")).andExpect(status().isOk()).andExpect(jsonPath("$.rules[0].id").value(UUID))
				.andExpect(jsonPath("$.catalog.producers[0].compatibleConsumers[0]").value("Consumer")).andExpect(jsonPath("$.dynamic[0].providers[0].provider").value("ServiceRegistry"));
		mvc.perform(as(s, HttpMethod.GET, "/api/orchestration/policies")).andExpect(status().isOk()).andExpect(jsonPath("$.entries").isArray())
				.andExpect(jsonPath("$.readOnlyEntries").isArray());
		mvc.perform(as(s, HttpMethod.GET, "/api/logs?service=sysmonitor&tail=1")).andExpect(status().isOk()).andExpect(jsonPath("$.lines", hasSize(1)))
				.andExpect(jsonPath("$.lines[0]").value("line two"));
		mvc.perform(as(s, HttpMethod.GET, "/api/logs?service=unknown")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Unknown project service"));
		mvc.perform(as(s, HttpMethod.GET, "/api/sos")).andExpect(status().isOk()).andExpect(jsonPath("$.entries[0].id").value(1));
		mvc.perform(as(s, HttpMethod.GET, "/api/sos/1")).andExpect(status().isOk()).andExpect(jsonPath("$.plantDescription").value("test"));
		mvc.perform(as(s, HttpMethod.GET, "/api/sos/9")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("SoS not found"));
		mvc.perform(as(s, HttpMethod.GET, "/api/sos/abc")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Unknown SoS operation"));
		mvc.perform(as(s, HttpMethod.GET, "/api/chiplet/floorplans")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Chiplet floorplan editing is not enabled"));
		mvc.perform(as(s, HttpMethod.GET, "/api/nothing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").value("Not found"));
	}

	@Test
	public void writeRoutesNeedTheActionConfirmation() throws Exception {
		final String s = login();
		for (final Object[] r : List.of(new Object[] { HttpMethod.PUT, "/api/sysmonitor/subscriptions/DataManager" }, new Object[] { HttpMethod.DELETE, "/api/sysmonitor/subscriptions/DataManager" },
				new Object[] { HttpMethod.POST, "/api/plantdescriptions" }, new Object[] { HttpMethod.PUT, "/api/plantdescriptions/1" },
				new Object[] { HttpMethod.PATCH, "/api/plantdescriptions/1" }, new Object[] { HttpMethod.DELETE, "/api/plantdescriptions/1" },
				new Object[] { HttpMethod.POST, "/api/orchestration/rules" }, new Object[] { HttpMethod.PUT, "/api/orchestration/rules/" + UUID },
				new Object[] { HttpMethod.DELETE, "/api/orchestration/rules/" + UUID }, new Object[] { HttpMethod.POST, "/api/orchestration/policies" },
				new Object[] { HttpMethod.DELETE, "/api/orchestration/policies/x" }, new Object[] { HttpMethod.POST, "/api/containers/sysmonitor/restart" },
				new Object[] { HttpMethod.POST, "/api/localcloud/plan" }, new Object[] { HttpMethod.POST, "/api/sos" }, new Object[] { HttpMethod.DELETE, "/api/sos/1" })) {
			mvc.perform(as(s, (HttpMethod) r[0], (String) r[1]).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().is(428))
					.andExpect(jsonPath("$.error").value(containsString("onfirm")));
		}
		assertTrue(calls.stream().noneMatch(c -> c.contains("create") || c.contains("remove") || c.contains("subscribe") || c.contains("replace")), calls.toString());
		assertTrue(dockerCalls.stream().noneMatch(c -> c.startsWith("POST")));
	}

	@Test
	public void confirmedWritesReachOnlyTheIdentifiedProviderOperations() throws Exception {
		final String s = login();
		mvc.perform(confirmed(s, HttpMethod.PUT, "/api/sysmonitor/subscriptions/DataManager", "{\"observationTypes\":[\"echo\"]}")).andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(true));
		mvc.perform(confirmed(s, HttpMethod.DELETE, "/api/sysmonitor/subscriptions/DataManager", "")).andExpect(status().isOk()).andExpect(jsonPath("$.removed").value("DataManager"));
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/plantdescriptions", "{\"plantDescription\":\"x\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(2));
		mvc.perform(confirmed(s, HttpMethod.PUT, "/api/plantdescriptions/1", "{}")).andExpect(status().isOk());
		mvc.perform(confirmed(s, HttpMethod.PATCH, "/api/plantdescriptions/1", "{}")).andExpect(status().isOk());
		mvc.perform(confirmed(s, HttpMethod.DELETE, "/api/plantdescriptions/1", "")).andExpect(status().isOk()).andExpect(jsonPath("$.removed").value(1));
		mvc.perform(confirmed(s, HttpMethod.PUT, "/api/plantdescriptions", "{}")).andExpect(status().isNotFound());
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/orchestration/rules", "{\"consumer\":\"Consumer\",\"serviceInstanceId\":\"ServiceRegistry|temperature|1.0.0\",\"priority\":3}"))
				.andExpect(status().isCreated());
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/orchestration/rules", "{\"consumer\":\"Nobody\",\"serviceInstanceId\":\"ServiceRegistry|temperature|1.0.0\",\"priority\":3}"))
				.andExpect(status().isConflict());
		mvc.perform(confirmed(s, HttpMethod.PUT, "/api/orchestration/rules/" + UUID,
				"{\"priority\":2,\"expected\":{\"consumer\":\"Consumer\",\"serviceInstanceId\":\"ServiceRegistry|temperature|1.0.0\",\"priority\":1}}")).andExpect(status().isOk());
		mvc.perform(confirmed(s, HttpMethod.PUT, "/api/orchestration/rules/" + UUID, "{\"priority\":2}")).andExpect(status().is(428));
		mvc.perform(confirmed(s, HttpMethod.DELETE, "/api/orchestration/rules/" + UUID, "")).andExpect(status().isOk()).andExpect(jsonPath("$.removed").value(true));
		mvc.perform(confirmed(s, HttpMethod.DELETE, "/api/orchestration/rules/0000-bad", "")).andExpect(status().isBadRequest());
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/orchestration/policies", "{\"policy\":{\"provider\":\"ServiceRegistry\",\"targetType\":\"SERVICE_DEF\",\"target\":\"temperature\","
				+ "\"defaultPolicy\":{\"policyType\":\"WHITELIST\",\"policyList\":[\"Consumer\"]}},\"expected\":null}")).andExpect(status().isCreated());
		mvc.perform(confirmed(s, HttpMethod.DELETE, "/api/orchestration/policies/none", "{\"expected\":{}}")).andExpect(status().isNotFound());
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/containers/sysmonitor/restart", "")).andExpect(status().isAccepted()).andExpect(jsonPath("$.accepted").value(true));
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/containers/authentication/stop", "")).andExpect(status().isForbidden());
		assertTrue(dockerCalls.contains("POST /containers/" + CONTAINER_ID.substring(0, 12) + "/restart?t=20"), dockerCalls.toString());

		final String base = "{\"plantDescription\":\"test\",\"active\":false,\"include\":[],\"systems\":[],\"connections\":[]}";
		final String expected = "{\"id\":1,\"updatedAt\":\"v1\",\"plantDescription\":\"test\",\"active\":false,\"include\":[],\"systems\":[],\"connections\":[]}";
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/sos", "{\"configuration\":" + base + "}")).andExpect(status().isCreated());
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/sos", "{\"configuration\":" + base.replace("false", "true") + "}")).andExpect(status().is(428));
		mvc.perform(confirmed(s, HttpMethod.PUT, "/api/sos/1", "{\"configuration\":" + base + ",\"expected\":" + expected.replace("v1", "old") + "}")).andExpect(status().isConflict());
		mvc.perform(confirmed(s, HttpMethod.PUT, "/api/sos/1", "{\"configuration\":" + base.replace("[],\"systems\"", "[1],\"systems\"") + ",\"expected\":" + expected + "}"))
				.andExpect(status().isBadRequest());
		mvc.perform(confirmed(s, HttpMethod.PUT, "/api/sos/1", "{\"configuration\":" + base + ",\"expected\":" + expected + "}")).andExpect(status().isOk());
		mvc.perform(confirmed(s, HttpMethod.DELETE, "/api/sos/1", "{\"expected\":" + expected + "}")).andExpect(status().isOk()).andExpect(jsonPath("$.removed").value(1));
		assertTrue(calls.contains("plantDescriptionManagement.remove /plant-descriptions/1"));
	}

	@Test
	public void localCloudConfigurationIsConsumedThroughOrchestrationAndPassedThrough() throws Exception {
		final String s = login();
		mvc.perform(as(s, HttpMethod.GET, "/api/localcloud/catalog")).andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
		mvc.perform(as(s, HttpMethod.GET, "/api/localcloud/status")).andExpect(status().isOk());
		mvc.perform(as(s, HttpMethod.GET, "/api/localcloud/configurations")).andExpect(status().isOk());
		mvc.perform(as(s, HttpMethod.GET, "/api/localcloud/configurations/" + UUID)).andExpect(status().isOk());
		mvc.perform(as(s, HttpMethod.GET, "/api/localcloud/deployments/local-1")).andExpect(status().isOk());
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/localcloud/plan", "{\"systems\":[]}")).andExpect(status().isOk());
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/localcloud/deployments", "{\"expectedPlanHash\":\"stale\"}")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Plan or cloud state changed. Review a fresh plan before applying."));
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/localcloud/configurations", "{\"name\":\"d\"}")).andExpect(status().isOk());
		mvc.perform(confirmed(s, HttpMethod.PUT, "/api/localcloud/configurations/" + UUID, "{\"expectedRevision\":1}")).andExpect(status().isOk());
		mvc.perform(confirmed(s, HttpMethod.DELETE, "/api/localcloud/configurations/" + UUID, "{\"expectedRevision\":2}")).andExpect(status().isOk());
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/localcloud/executor", "{}")).andExpect(status().isNotFound());
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/localcloud/configurations/not-valid", "{}")).andExpect(status().isNotFound());
		assertEquals(List.of("localCloudConfiguration.catalog /catalog", "localCloudConfiguration.status /status", "localCloudConfiguration.list-configurations /configurations",
				"localCloudConfiguration.get-configuration /configurations/" + UUID, "localCloudConfiguration.get-deployment /deployments/local-1",
				"localCloudConfiguration.plan /plan", "localCloudConfiguration.submit-deployment /deployments", "localCloudConfiguration.create-configuration /configurations",
				"localCloudConfiguration.update-configuration /configurations/" + UUID, "localCloudConfiguration.remove-configuration /configurations/" + UUID),
				calls.stream().filter(c -> c.startsWith("localCloudConfiguration")).toList());
	}

	@Test
	public void oversizedAndInvalidBodiesAreRefused() throws Exception {
		final String s = login();
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/plantdescriptions", "{\"x\":\"" + "a".repeat(70000) + "\"}")).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error").value("Request body is too large"));
		mvc.perform(confirmed(s, HttpMethod.POST, "/api/plantdescriptions", "{not json")).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error").value("Invalid JSON body"));
	}

	// ----------------------------------------------------------------------------------------------- Arrowhead connector

	@Test
	public void arrowheadConnectorHasNoConsoleAndNeedsAnIdentityAndAGrant() throws Exception {
		for (final String path : List.of("/api/session", "/health", "/app.js", "/index.html")) {
			mvc.perform(arrowhead(HttpMethod.GET, path)).andExpect(status().isNotFound());
		}
		mvc.perform(arrowhead(HttpMethod.GET, "/api/overview")).andExpect(status().isUnauthorized());
		mvc.perform(arrowhead(HttpMethod.GET, "/api/overview").header("Authorization", "Bearer SYSTEM//DataManager")).andExpect(status().isForbidden());
		mvc.perform(arrowhead(HttpMethod.GET, "/api/overview").header("Authorization", "Bearer SYSTEM//Sysop")).andExpect(status().isOk())
				.andExpect(jsonPath("$.cloud.name").value("cloud-a"));
		mvc.perform(arrowhead(HttpMethod.GET, "/api/sysmonitor/subscriptions").header("Authorization", "Bearer SYSTEM//LocalCloudManagerProbe")).andExpect(status().isOk());
		// a path that is no registered operation is refused for everybody but Sysop
		mvc.perform(arrowhead(HttpMethod.GET, "/api/features").header("Authorization", "Bearer SYSTEM//LocalCloudManagerProbe")).andExpect(status().isForbidden());
		mvc.perform(arrowhead(HttpMethod.GET, "/localcloudmanager/general/mgmt/get-config").param("keys", "console.port").header("Authorization", "Bearer SYSTEM//Sysop"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.map['console.port']").value("8490"));
		mvc.perform(arrowhead(HttpMethod.GET, "/localcloudmanager/general/mgmt/get-config").header("Authorization", "Bearer SYSTEM//DataManager")).andExpect(status().isForbidden());
	}

	@TestConfiguration
	static class Fixtures {

		@Bean
		StandaloneModeInTests standaloneMode() {
			return new StandaloneModeInTests() {
			};
		}
	}

}
