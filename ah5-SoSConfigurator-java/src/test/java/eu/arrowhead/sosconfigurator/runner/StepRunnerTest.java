package eu.arrowhead.sosconfigurator.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.sosconfigurator.Json;
import eu.arrowhead.sosconfigurator.catalog.Catalog;
import eu.arrowhead.sosconfigurator.planner.Planner;

class StepRunnerTest {

	/** Records the command and the spec the script would get. */
	static final class Recorder implements StepRunner.Commander {
		final List<List<String>> commands = new ArrayList<>();
		final List<JsonNode> specs = new ArrayList<>();
		final List<Path> dirs = new ArrayList<>();
		String output = "done password=hunter2 Bearer IDENTITY-TOKEN//abc";
		int exit = 0;

		@Override
		public StepRunner.Result run(final Path dir, final Map<String, String> env, final Duration timeout, final List<String> command) throws IOException {
			commands.add(command);
			dirs.add(dir);
			specs.add(Json.LENIENT.readTree(Files.readString(Path.of(command.get(3)))));
			return new StepRunner.Result(exit, output);
		}
	}

	@TempDir
	Path base;

	private StepRunner runner(final String role, final Recorder rec) throws Exception {
		final Catalog c = Catalog.load(Path.of("catalogs/sos.json"));
		// the catalog paths start with ../../, so the base sits two levels inside the temporary directory
		final Path base = this.base.resolve("a/b");
		// the catalog's cloud and relay directories, relative to base
		for (final Catalog.Cloud cloud : c.clouds()) {
			Files.createDirectories(base.resolve(cloud.dir()).normalize());
		}
		Files.createDirectories(base.resolve(c.relay().dir()).normalize());
		return new StepRunner(c, role, base, Path.of("scripts"), rec);
	}

	private static final String SOS = """
			{"name":"DemoSoS","clouds":["TestCloud","GoCloud"],"exchanges":[{"consumerCloud":"GoCloud","consumer":"RandomDataProducer",
			"providerCloud":"TestCloud","provider":"DataManager","service":"dataHistorian","operations":["put-data"]}]}""";

	@Test
	void derivesTheCloudConfigurationItselfAndRedactsOutput() throws Exception {
		final Recorder rec = new Recorder();
		final String out = runner("cloud:TestCloud", rec).step(Planner.GRANTS, SOS);
		assertEquals("grants", rec.commands.get(0).get(2));
		assertTrue(rec.commands.get(0).get(1).endsWith("scripts/sos-cloud.sh"));
		assertTrue(rec.dirs.get(0).endsWith("Arrowhead-test-cloud-java"));
		final JsonNode spec = rec.specs.get(0);
		assertEquals("TestCloud", spec.path("cloud").asText());
		assertEquals("GoCloud|Company", spec.path("config").path("grants").get(0).path("cloud").asText());
		assertFalse(out.contains("hunter2"));
		assertFalse(out.contains("abc"));
	}

	@Test
	void refusesRequestsThatViolateItsOwnPolicyCheck() throws Exception {
		final Recorder rec = new Recorder();
		final String forged = SOS.replace("RandomDataProducer", "Sysop");
		final IOException e = assertThrows(IOException.class, () -> runner("cloud:TestCloud", rec).step(Planner.GRANTS, forged));
		assertTrue(e.getMessage().contains("A2"));
		assertTrue(rec.commands.isEmpty(), "nothing runs for a refused request");
	}

	@Test
	void refusesStepsOfOtherRolesAndNonMembers() throws Exception {
		final Recorder rec = new Recorder();
		assertThrows(IOException.class, () -> runner("relay", rec).step(Planner.GRANTS, SOS));
		assertThrows(IOException.class, () -> runner("cloud:TestCloud", rec).step("rm-rf", SOS));
		final String withoutTest = SOS.replace("\"TestCloud\",", "");
		assertThrows(IOException.class, () -> runner("cloud:TestCloud", rec).step(Planner.CLOUD_CHECK, withoutTest));
		assertTrue(rec.commands.isEmpty());
	}

	@Test
	void relayCheckGetsMemberSubjects() throws Exception {
		final Recorder rec = new Recorder();
		runner("relay", rec).step(Planner.RELAY_CHECK, SOS);
		final JsonNode members = rec.specs.get(0).path("members");
		assertEquals(2, members.size());
		assertEquals(2, members.get(0).path("subjects").size());
	}

	@Test
	void failedScriptCarriesRedactedOutput() throws Exception {
		final Recorder rec = new Recorder();
		rec.exit = 1;
		final StepRunner.StepFailed f = assertThrows(StepRunner.StepFailed.class, () -> runner("cloud:GoCloud", rec).step(Planner.CLOUD_CHECK, SOS));
		assertFalse(f.output().contains("hunter2"));
	}
}
