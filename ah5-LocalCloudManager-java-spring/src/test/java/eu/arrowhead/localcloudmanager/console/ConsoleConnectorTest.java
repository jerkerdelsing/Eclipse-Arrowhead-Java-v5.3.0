package eu.arrowhead.localcloudmanager.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;

import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import eu.arrowhead.common.testhelper.StandaloneModeInTests;
import eu.arrowhead.localcloudmanager.LocalCloudManagerMain;
import eu.arrowhead.localcloudmanager.client.DockerGateway;
import eu.arrowhead.localcloudmanager.client.ProviderGateway;

// Real Tomcat: the console connector listens on the loopback interface only, serves the UI, and answers /api only after login.
@SpringBootTest(classes = { LocalCloudManagerMain.class, ConsoleConnectorTest.Fixtures.class }, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
		properties = { "spring.config.location=classpath:/application.properties", "arrowhead.security.profile=development", "console.address=127.0.0.1" })
public class ConsoleConnectorTest {

	private static final String PASSWORD = "correct horse battery";
	private static int consolePort;

	@Autowired
	private ServletWebServerApplicationContext context;

	@MockitoBean
	private ProviderGateway providers;

	@MockitoBean
	private DockerGateway docker;

	private final HttpClient http = HttpClient.newHttpClient();

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	@DynamicPropertySource
	static void ports(final DynamicPropertyRegistry registry) throws IOException {
		consolePort = freePort();
		final int apiPort = freePort();
		registry.add("console.port", () -> consolePort);
		registry.add("server.port", () -> apiPort);
		registry.add("console.password.hash", () -> ConsoleAuth.hash(PASSWORD));
	}

	private HttpResponse<String> send(final HttpRequest.Builder request) throws Exception {
		return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	public void consoleIsOnLoopbackOnlyAndApiNeedsALogin() throws Exception {
		final TomcatWebServer server = (TomcatWebServer) context.getWebServer();
		Connector console = null;
		for (final Connector connector : server.getTomcat().getService().findConnectors()) {
			if (connector.getLocalPort() == consolePort) {
				console = connector;
			}
		}
		assertEquals("127.0.0.1", String.valueOf(console.getProperty("address")).replace("/", ""));

		// not reachable on a non-loopback address of this host
		for (final NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
			for (final InetAddress address : Collections.list(nif.getInetAddresses())) {
				if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
					assertThrows(IOException.class, () -> {
						try (Socket socket = new Socket()) {
							socket.connect(new java.net.InetSocketAddress(address, consolePort), 1000);
						}
					});
				}
			}
		}

		final String base = "http://127.0.0.1:" + consolePort;
		assertEquals(200, send(HttpRequest.newBuilder(URI.create(base + "/"))).statusCode());
		assertEquals(200, send(HttpRequest.newBuilder(URI.create(base + "/health"))).statusCode());
		assertEquals(401, send(HttpRequest.newBuilder(URI.create(base + "/api/features"))).statusCode());
		final HttpResponse<String> login = send(HttpRequest.newBuilder(URI.create(base + "/api/session")).header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString("{\"password\":\"" + PASSWORD + "\"}")));
		assertEquals(204, login.statusCode());
		final String cookie = login.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
		final HttpResponse<String> features = send(HttpRequest.newBuilder(URI.create(base + "/api/features")).header("Cookie", cookie));
		assertEquals(200, features.statusCode());
		assertTrue(features.body().contains("\"managerName\":\"LocalCloudManager\""));
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
