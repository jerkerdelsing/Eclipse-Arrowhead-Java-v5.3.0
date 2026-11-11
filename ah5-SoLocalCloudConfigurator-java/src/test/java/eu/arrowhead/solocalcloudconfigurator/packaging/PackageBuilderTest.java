/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.solocalcloudconfigurator.packaging;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import eu.arrowhead.solocalcloudconfigurator.Json;
import eu.arrowhead.solocalcloudconfigurator.TestDesigns;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign;
import eu.arrowhead.solocalcloudconfigurator.policy.PolicyEngine;

public class PackageBuilderTest {

	private record Entry(int mode, byte[] content) {
	}

	private static PackageBuilder.Result build(final Map<String, byte[]> kit) throws Exception {
		final SolcDesign d = TestDesigns.twoClouds();
		final JsonNode lcCatalog = Json.LENIENT.readTree("{\"cloud\":\"TestCloud\",\"systems\":[]}");
		return new PackageBuilder(kit).build(new PackageBuilder.Input("33333333-3333-3333-3333-333333333333", 7, d,
				new PolicyEngine(TestDesigns.catalog()).evaluate(d), lcCatalog, "https://example.org/arrowhead.git", "a".repeat(40), "LocalCloudManager",
				"2026-11-11T00:00:00Z"));
	}

	private static Map<String, Entry> untar(final byte[] archive) throws Exception {
		final Map<String, Entry> out = new TreeMap<>();
		try (TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(new ByteArrayInputStream(archive)))) {
			TarArchiveEntry e;
			while ((e = tar.getNextEntry()) != null) {
				out.put(e.getName(), new Entry(e.getMode(), e.isDirectory() ? new byte[0] : tar.readAllBytes()));
			}
		}
		return out;
	}

	private static Map<String, byte[]> kit() {
		final Map<String, byte[]> kit = new TreeMap<>();
		kit.put("cloud/deploy.sh", "#!/bin/sh\necho deploy\n".getBytes(StandardCharsets.UTF_8));
		kit.put("cloud/templates/base.json", "{}".getBytes(StandardCharsets.UTF_8));
		kit.put("bin/assemble-package.sh", "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
		kit.put("org/init-org.sh", "#!/bin/sh\n".getBytes(StandardCharsets.UTF_8));
		kit.put("docs/INSTALL-macos.md", "# macOS\n".getBytes(StandardCharsets.UTF_8));
		kit.put("selftest-only.txt", "not packaged".getBytes(StandardCharsets.UTF_8));
		return kit;
	}

	@Test
	public void packageHasTheDocumentedLayout() throws Exception {
		final PackageBuilder.Result r = build(kit());
		assertEquals("solc-factory-r7.tar.gz", r.fileName());
		final Map<String, Entry> files = untar(r.content());
		final String root = "solc-factory-r7/";
		for (final String f : new String[] { "README.md", "manifest.json", "SHA256SUMS", "kit/deploy.sh", "kit/templates/base.json", "kit/bin/assemble-package.sh",
				"kit/lc-catalog.json", "org/init-org.sh", "org/org.json", "docs/INSTALL-macos.md", "clouds/Plant/cloud.json", "clouds/Plant/federation-spec.json",
				"clouds/Office/cloud.json", "clouds/Office/README.md", "source/SOURCE.json", "images/README.md" }) {
			assertTrue(files.containsKey(root + f), f);
		}
		assertFalse(files.keySet().stream().anyMatch(f -> f.contains("selftest-only")));
		assertEquals(0755, files.get(root + "kit/deploy.sh").mode() & 0777);
		assertEquals(0755, files.get(root + "org/init-org.sh").mode() & 0777);
		assertEquals(0644, files.get(root + "manifest.json").mode() & 0777);

		final JsonNode cloud = Json.LENIENT.readTree(files.get(root + "clouds/Plant/cloud.json").content());
		assertEquals("arrowhead-plant", cloud.path("composeProject").asText());
		assertTrue(cloud.path("federation").asBoolean());
		assertEquals("Office", cloud.path("peers").get(0).asText());
		final JsonNode spec = Json.LENIENT.readTree(files.get(root + "clouds/Office/federation-spec.json").content());
		assertEquals("RandomDataProducer", spec.path("config").path("grants").get(0).path("consumers").get(0).asText());
		assertEquals("dnQualifier=sy,CN=Gatekeeper.Office.Company.arrowhead.eu,OU=Office,O=Company,C=SE", spec.path("subjects").path("Gatekeeper").asText());
	}

	@Test
	public void checksumsCoverEveryFileAndBuildsAreReproducible() throws Exception {
		final PackageBuilder.Result r = build(kit());
		final Map<String, Entry> files = untar(r.content());
		final String sums = new String(files.get("solc-factory-r7/SHA256SUMS").content(), StandardCharsets.UTF_8);
		int n = 0;
		for (final String line : sums.split("\n")) {
			final String[] p = line.split("  ", 2);
			assertEquals(p[0], PackageBuilder.sha256(files.get("solc-factory-r7/" + p[1]).content()), p[1]);
			n++;
		}
		assertEquals(files.values().stream().filter(e -> (e.mode() & 0040000) == 0).count() - 1, n);
		assertArrayEquals(r.content(), build(kit()).content());
	}

	@Test
	public void bundledKitIsPackagedWithItsScriptsExecutable() throws Exception {
		final Map<String, Entry> files = untar(build(KitResources.load()).content());
		final String root = "solc-factory-r7/";
		for (final String f : new String[] { "kit/bin/install-cloud.sh", "kit/bin/configure.py", "kit/bin/assemble-package.sh", "kit/bin/shims/keytool",
				"kit/templates/compose-template.json", "kit/scripts/generate-x509.sh", "org/init-org.sh", "org/relay/mosquitto.conf", "docs/FEDERATION.md",
				"docs/INSTALL-linux-arm64.md", "clouds/Plant/install.sh", "clouds/Plant/intercloud.sh" }) {
			assertTrue(files.containsKey(root + f), f);
		}
		assertEquals(0755, files.get(root + "kit/bin/shims/keytool").mode() & 0777);
		assertEquals(0755, files.get(root + "clouds/Plant/install.sh").mode() & 0777);
		assertEquals(0644, files.get(root + "org/relay/mosquitto.conf").mode() & 0777);
		final String wrapper = new String(files.get(root + "clouds/Office/import-federation.sh").content(), StandardCharsets.UTF_8);
		assertTrue(wrapper.contains("kit/bin/import-federation.sh"), wrapper);
	}

	@Test
	public void packageHoldsNoSecretsOrHostPaths() throws Exception {
		for (final Entry e : untar(build(kit()).content()).values()) {
			final String s = new String(e.content(), StandardCharsets.UTF_8);
			assertFalse(s.contains("PRIVATE KEY"));
			assertFalse(s.contains("/Users/"));
			assertFalse(s.contains("/home/"));
			assertFalse(s.toLowerCase().contains("password\":"));
		}
	}
}
