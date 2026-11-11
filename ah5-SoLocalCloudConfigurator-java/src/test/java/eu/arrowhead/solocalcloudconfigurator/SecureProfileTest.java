package eu.arrowhead.solocalcloudconfigurator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import eu.arrowhead.common.init.SecurityProfileGuard;

// Insecure startup is refused: the packaged development sample does not start in the default secure profile, the deployment
// configuration does.
public class SecureProfileTest {

	private static MockEnvironment environment(final String file) throws Exception {
		final Properties properties = new Properties();
		try (Reader in = Files.newBufferedReader(Path.of(file))) {
			properties.load(in);
		}
		final MockEnvironment environment = new MockEnvironment();
		properties.forEach((k, v) -> environment.setProperty(k.toString(), v.toString()));
		return environment;
	}

	private static SecurityProfileGuard guard(final MockEnvironment environment) {
		final SecurityProfileGuard guard = new SecurityProfileGuard();
		guard.setEnvironment(environment);
		return guard;
	}

	@Test
	public void developmentSampleIsRefusedInTheSecureProfile() throws Exception {
		final IllegalStateException refused = assertThrows(IllegalStateException.class, () -> guard(environment("src/main/resources/application.properties")).enforce());
		assertTrue(refused.getMessage().contains("server.ssl.enabled=false"));
		assertTrue(refused.getMessage().contains("authentication.policy=declared"));
		assertTrue(refused.getMessage().contains("service.authorization.enabled=false"));
	}

	@Test
	public void deploymentConfigurationMeetsTheSecureProfile() throws Exception {
		final MockEnvironment deployment = environment("config/application.properties");
		assertDoesNotThrow(() -> guard(deployment).enforce());
		deployment.setProperty("service.authorization.enabled", "false");
		assertThrows(IllegalStateException.class, () -> guard(deployment).enforce());
	}

	@Test
	public void developmentExceptionMustBeSelectedExplicitly() throws Exception {
		final MockEnvironment sample = environment("src/main/resources/application.properties");
		sample.setProperty("arrowhead.security.profile", "development");
		assertDoesNotThrow(() -> guard(sample).enforce());
	}
}
