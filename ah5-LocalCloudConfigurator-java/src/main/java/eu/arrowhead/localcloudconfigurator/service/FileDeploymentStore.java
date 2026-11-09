package eu.arrowhead.localcloudconfigurator.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;

import eu.arrowhead.localcloudconfigurator.Json;
import eu.arrowhead.localcloudconfigurator.Planner;

/**
 * The host worker's file state (unchanged layout of the native worker): active-deployment.json and deployments/&lt;id&gt;.json under the
 * state directory. scripts/install-direct.py reads active-deployment.json to refuse a reinstall during a deployment.
 */
public class FileDeploymentStore implements DeploymentStore {

	private final Path state;

	public FileDeploymentStore(final Path state) throws IOException {
		this.state = state;
		Files.createDirectories(state.resolve("deployments"));
	}

	@Override
	public synchronized String activeDeployment() {
		final Path active = state.resolve("active-deployment.json");
		if (!Files.exists(active)) {
			return null;
		}

		try {
			return Json.LENIENT.readTree(Files.readString(active)).path("id").asText(null);
		} catch (final IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public synchronized void setActiveDeployment(final String id) {
		try {
			final Path active = state.resolve("active-deployment.json");
			if (id == null) {
				Files.deleteIfExists(active);
			} else {
				write(active, Map.of("id", id));
			}
		} catch (final IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public boolean exists(final String id) {
		return Files.exists(state.resolve("deployments").resolve(id + ".json"));
	}

	@Override
	public synchronized void submitted(final String id, final Planner.Plan plan, final Planner.Request request) {
		try {
			write(state.resolve("deployments").resolve(id + ".json"), Map.of("id", id, "plan", plan, "submittedAt", Instant.now().toString()));
		} catch (final IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static void write(final Path p, final Object v) throws IOException {
		final Path tmp = Files.createTempFile(p.getParent(), "lcc-", ".tmp");
		try {
			Files.writeString(tmp, Json.write(v));
			try {
				Files.move(tmp, p, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (final AtomicMoveNotSupportedException ex) {
				Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(tmp);
		}
	}
}
