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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;

import eu.arrowhead.solocalcloudconfigurator.Json;
import eu.arrowhead.solocalcloudconfigurator.SoLocalCloudConfiguratorConstants;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign;
import eu.arrowhead.solocalcloudconfigurator.design.SolcDesign.Member;
import eu.arrowhead.solocalcloudconfigurator.policy.PolicyEngine;
import eu.arrowhead.solocalcloudconfigurator.policy.PolicyEngine.CloudConfig;

/**
 * Renders the deployment package of a System of LocalClouds (tar.gz, first stage): the manifest, the organisation and
 * per-cloud configuration derived from the design and its plan, the portable deployment kit, the documentation and the
 * checksums. It holds no secret: every key, certificate request and password is generated on the host that uses it. Source
 * and image archives are added by the host assembler (package-assemble), which has Docker; this system has not.
 *
 * <p>Layout: solc-NAME-rREV/{README.md, manifest.json, SHA256SUMS, docs/, kit/, org/, clouds/CLOUD/, source/, images/}.
 */
public final class PackageBuilder {

	public static final String FORMAT = "solc-package/1";
	private static final Pattern SAFE_PATH = Pattern.compile("[A-Za-z0-9._/-]{1,200}");
	private static final long MTIME = 1_767_225_600_000L; // 2026-01-01T00:00:00Z: reproducible archives

	/** What the package is built from. */
	public record Input(String designId, long designRevision, SolcDesign design, PolicyEngine.Evaluation evaluation, JsonNode lcCatalog,
			String sourceRepository, String sourceCommit, String createdBy, String createdAt) {
	}

	public record Result(String root, String fileName, byte[] content, String sha256, Map<String, String> files) {
	}

	// clouds/<Cloud>/<script> -> kit/bin/<script>: name, kit script, when (always | federation), what it does
	private static final String[][] WRAPPERS = {
		{ "install.sh", "install-cloud.sh", "always", "install and start this cloud on this host" },
		{ "check.sh", "check.sh", "always", "smoke test and security checks" },
		{ "uninstall.sh", "uninstall-cloud.sh", "always", "stop this cloud (--purge also deletes its data)" },
		{ "request-federation.sh", "request-federation.sh", "federation", "write the federation request for the organisation host" },
		{ "import-federation.sh", "import-federation.sh", "federation", "install the organisation's federation bundle" },
		{ "intercloud.sh", "intercloud.sh", "federation", "apply and verify this cloud's neighbours and inter-cloud grants" },
		{ "export-crl.sh", "export-crl.sh", "federation", "export this cloud's CRL for the organisation host" },
	};

	private final Map<String, byte[]> kit;

	public PackageBuilder(final Map<String, byte[]> kit) {
		this.kit = kit;
	}

	public static String root(final SolcDesign d, final long revision) {
		return "solc-" + d.name().toLowerCase() + "-r" + revision;
	}

	public Result build(final Input in) throws IOException {
		final SolcDesign d = in.design();
		final String root = root(d, in.designRevision());
		final Map<String, byte[]> files = new TreeMap<>();

		// the kit: kit/cloud -> kit/, kit/bin -> kit/bin, kit/org -> org/, kit/docs -> docs/
		kit.forEach((path, content) -> {
			final String target;
			if (path.startsWith("cloud/")) {
				target = "kit/" + path.substring("cloud/".length());
			} else if (path.startsWith("bin/")) {
				target = "kit/" + path;
			} else if (path.startsWith("org/") || path.startsWith("docs/")) {
				target = path;
			} else {
				return;
			}
			files.put(target, content);
		});
		files.put("kit/lc-catalog.json", pretty(in.lcCatalog()));

		final String imageTag = "solc-" + d.name().toLowerCase() + "-r" + in.designRevision();
		final boolean federation = d.membersOrEmpty().size() >= 2;
		final Map<String, Object> org = new LinkedHashMap<>();
		org.put("organization", d.organization());
		org.put("solc", d.name());
		org.put("relay", d.relay());
		org.put("clouds", in.evaluation().relay().clouds());
		org.put("subjects", in.evaluation().relay().subjects());
		files.put("org/org.json", pretty(org));

		for (final Member m : d.membersOrEmpty()) {
			final String dir = "clouds/" + m.cloud() + "/";
			final CloudConfig config = in.evaluation().clouds().get(m.cloud());
			final Map<String, Object> cloud = new LinkedHashMap<>();
			cloud.put("cloud", m.cloud());
			cloud.put("organization", d.organization());
			cloud.put("solc", d.name());
			cloud.put("composeProject", m.composeProject());
			cloud.put("host", m.host());
			cloud.put("architecture", m.architecture());
			cloud.put("portOffset", m.portOffset());
			cloud.put("components", m.components());
			cloud.put("intercloudInfrastructure", m.intercloudOrEmpty());
			cloud.put("lcDesign", Map.of("id", m.lcDesign().id(), "name", m.lcDesign().name(), "revision", m.lcDesign().revision()));
			cloud.put("federation", federation && config != null);
			cloud.put("relay", d.relay());
			cloud.put("peers", config == null ? List.of() : config.neighbours().stream().map(PolicyEngine.Neighbour::name).toList());
			cloud.put("intercloudConsumers", d.exchangesOrEmpty().stream().filter(x -> x.consumerCloud().equals(m.cloud())).map(SolcDesign.Exchange::consumer)
					.distinct().sorted().toList());
			cloud.put("imageMode", d.packaging().imageMode());
			cloud.put("imageTag", imageTag);
			files.put(dir + "cloud.json", pretty(cloud));
			if (config != null) {
				final Map<String, Object> spec = new LinkedHashMap<>();
				spec.put("cloud", m.cloud());
				spec.put("organization", d.organization());
				spec.put("sos", d.name());
				spec.put("gatekeeperService", "gatekeeper");
				spec.put("gatetunnelService", "gatetunnel");
				spec.put("subjects", Map.of(
						"Gatekeeper", PolicyEngine.relaySubject("Gatekeeper", m.cloud(), d.organization()),
						"GateTunnel", PolicyEngine.relaySubject("GateTunnel", m.cloud(), d.organization())));
				spec.put("config", config);
				files.put(dir + "federation-spec.json", pretty(spec));
			}
			files.put(dir + "README.md", text(cloudReadme(d, m, config != null && federation)));
			for (final String[] w : WRAPPERS) {
				if (config != null && federation || !w[2].equals("federation")) {
					files.put(dir + w[0], text(wrapper(w[1], w[3])));
				}
			}
		}

		final Map<String, Object> source = new LinkedHashMap<>();
		source.put("repository", in.sourceRepository());
		source.put("commit", in.sourceCommit());
		files.put("source/SOURCE.json", pretty(source));
		files.put("images/README.md", text(imagesReadme(d)));
		files.put("README.md", text(readme(d, in, federation)));

		final Map<String, Object> manifest = new LinkedHashMap<>();
		manifest.put("format", FORMAT);
		manifest.put("generator", SoLocalCloudConfiguratorConstants.SYSTEM_NAME + " " + SoLocalCloudConfiguratorConstants.SYSTEM_VERSION);
		manifest.put("name", d.name());
		manifest.put("designId", in.designId());
		manifest.put("designRevision", in.designRevision());
		manifest.put("createdBy", in.createdBy());
		manifest.put("createdAt", in.createdAt());
		manifest.put("source", source);
		manifest.put("imageTag", imageTag);
		manifest.put("assembled", Map.of("source", false, "images", List.of()));
		manifest.put("design", d);
		manifest.put("plan", in.evaluation());
		files.put("manifest.json", pretty(manifest));

		final Map<String, String> sums = new TreeMap<>();
		final StringBuilder sumFile = new StringBuilder();
		for (final Map.Entry<String, byte[]> e : files.entrySet()) {
			if (!SAFE_PATH.matcher(e.getKey()).matches() || e.getKey().contains("..")) {
				throw new IllegalStateException("unsafe package path " + e.getKey());
			}
			final String sha = sha256(e.getValue());
			sums.put(e.getKey(), sha);
			sumFile.append(sha).append("  ").append(e.getKey()).append('\n');
		}
		files.put("SHA256SUMS", text(sumFile.toString()));

		final byte[] archive = tarGz(root, files);
		if (archive.length > SoLocalCloudConfiguratorConstants.MAX_PACKAGE_BYTES) {
			throw new IllegalStateException("package exceeds " + (SoLocalCloudConfiguratorConstants.MAX_PACKAGE_BYTES >> 20) + " MiB");
		}
		return new Result(root, root + ".tar.gz", archive, sha256(archive), sums);
	}

	static boolean executable(final String path) {
		return path.endsWith(".sh") || path.endsWith(".py") || path.startsWith("kit/bin/");
	}

	static byte[] tarGz(final String root, final Map<String, byte[]> files) throws IOException {
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		final GzipParameters gz = new GzipParameters();
		gz.setModificationTime(MTIME);
		try (TarArchiveOutputStream tar = new TarArchiveOutputStream(new GzipCompressorOutputStream(bytes, gz))) {
			tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			final List<String> dirs = new ArrayList<>();
			for (final String path : files.keySet()) {
				String p = path;
				while (p.contains("/")) {
					p = p.substring(0, p.lastIndexOf('/'));
					if (!dirs.contains(p)) {
						dirs.add(p);
					}
				}
			}
			dirs.add("");
			dirs.sort(String::compareTo);
			for (final String dir : dirs) {
				final TarArchiveEntry e = new TarArchiveEntry(root + "/" + (dir.isEmpty() ? "" : dir + "/"));
				e.setMode(0040755);
				e.setModTime(MTIME);
				tar.putArchiveEntry(e);
				tar.closeArchiveEntry();
			}
			for (final Map.Entry<String, byte[]> f : files.entrySet()) {
				final TarArchiveEntry e = new TarArchiveEntry(root + "/" + f.getKey());
				e.setSize(f.getValue().length);
				e.setMode(executable(f.getKey()) ? 0100755 : 0100644);
				e.setModTime(MTIME);
				tar.putArchiveEntry(e);
				tar.write(f.getValue());
				tar.closeArchiveEntry();
			}
			tar.finish();
		}
		return bytes.toByteArray();
	}

	public static String sha256(final byte[] b) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static byte[] pretty(final Object o) {
		try {
			return (Json.MAPPER.copy().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(o) + "\n").getBytes(StandardCharsets.UTF_8);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static byte[] text(final String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}

	private static String hostsTable(final SolcDesign d) {
		final StringBuilder b = new StringBuilder("| Cloud | Host | Architecture | Port offset | Local-cloud design | Components |\n|---|---|---|---|---|---|\n");
		for (final Member m : d.membersOrEmpty()) {
			b.append("| ").append(m.cloud()).append(" | ").append(m.host()).append(" | ").append(m.architecture()).append(" | ").append(m.portOffset())
					.append(" | ").append(m.lcDesign().name()).append(" (revision ").append(m.lcDesign().revision()).append(") | ")
					.append(String.join(", ", m.components())).append(" |\n");
		}
		return b.toString();
	}

	private static String readme(final SolcDesign d, final Input in, final boolean federation) {
		final boolean prebuilt = "prebuilt".equals(d.packaging().imageMode());
		final StringBuilder b = new StringBuilder();
		b.append("# System of LocalClouds ").append(d.name()).append(" (revision ").append(in.designRevision()).append(")\n\n");
		b.append("Deployment package built by ").append(SoLocalCloudConfiguratorConstants.SYSTEM_NAME).append(" on ").append(in.createdAt())
				.append(" for ").append(in.createdBy()).append(".\nOrganisation: ").append(d.organization()).append(". Relay: ")
				.append(d.relay().host()).append(':').append(d.relay().port()).append(".\n\n");
		b.append("## Member clouds\n\n").append(hostsTable(d)).append('\n');
		if (!d.exchangesOrEmpty().isEmpty()) {
			b.append("## Inter-cloud exchanges\n\n| Consumer | Provider service | Operations |\n|---|---|---|\n");
			d.exchangesOrEmpty().forEach(x -> b.append("| ").append(x.consumerCloud()).append('/').append(x.consumer()).append(" | ")
					.append(x.providerCloud()).append('/').append(x.provider()).append('.').append(x.service()).append(" | ")
					.append(String.join(", ", x.operations())).append(" |\n"));
			b.append('\n');
		}
		b.append("## Before you start\n\n");
		b.append("1. Check the package: `sha256sum -c SHA256SUMS` (macOS: `shasum -a 256 -c SHA256SUMS`).\n");
		b.append("2. Images: ").append(prebuilt
				? "this package uses prebuilt image archives in `images/<arch>/`. If `images/` holds only this README, complete the package first on a machine with Docker: `kit/bin/assemble-package.sh . --arch "
						+ String.join(",", d.packaging().architectures()) + "` (see images/README.md)."
				: "every host builds its images from the pinned source (`source/SOURCE.json`). If `source/source.tar.gz` is absent, `kit/bin/fetch-source.sh` clones the repository at the pinned commit; "
						+ "for hosts without network access add it beforehand with `kit/bin/assemble-package.sh . --source <checkout>`.")
				.append("\n");
		b.append("3. Read the installation guide of each host's platform: docs/INSTALL-linux-amd64.md, docs/INSTALL-linux-arm64.md, docs/INSTALL-macos.md.\n\n");
		b.append("## Order of operations\n\n");
		if (federation) {
			b.append("1. **Organisation host** (holds the organisation CA and runs the relay; see docs/FEDERATION.md): copy the package, then `org/init-org.sh` and `org/relay.sh up`.\n");
			b.append("2. **Each member host**: copy the package, then `clouds/<Cloud>/install.sh` (keys, certificates and passwords are generated there; the cloud CA key never leaves the host). It ends with a federation request `clouds/<Cloud>/out/<Cloud>-federation-request.tar.gz`.\n");
			b.append("3. **Organisation host**: `org/sign-cloud.sh <request>` for each member; it returns `<Cloud>-federation.tar.gz` (cross-certificate, relay client certificates, organisation CRL).\n");
			b.append("4. **Each member host**: `clouds/<Cloud>/import-federation.sh <Cloud>-federation.tar.gz`, then `clouds/<Cloud>/intercloud.sh` (neighbours, grants, verification).\n");
			b.append("5. **Each member host**: `clouds/<Cloud>/check.sh` (smoke test and security checks).\n\n");
		} else {
			b.append("1. **The member host**: copy the package, then `clouds/<Cloud>/install.sh` and `clouds/<Cloud>/check.sh`.\n\n");
		}
		b.append("Operations (CRL distribution, backups, secret rotation, removal): docs/OPERATIONS.md. Security model: docs/SECURITY.md. Problems: docs/TROUBLESHOOTING.md.\n\n");
		b.append("This package contains no keys, certificates or passwords. Never copy a host's `security/` or `secrets/` folder to another host.\n");
		return b.toString();
	}

	private static String cloudReadme(final SolcDesign d, final Member m, final boolean federation) {
		return "# Local cloud " + m.cloud() + "\n\nHost " + m.host() + " (" + m.architecture() + "), Compose project " + m.composeProject() + ", port offset "
				+ m.portOffset() + ".\nBuilt from local-cloud design " + m.lcDesign().name() + " (revision " + m.lcDesign().revision() + ")"
				+ (m.intercloudOrEmpty().isEmpty() ? "" : ", with inter-cloud infrastructure added by the SoLC: " + String.join(", ", m.intercloudOrEmpty()))
				+ ".\n\n"
				+ "1. `./install.sh` - checks the host, provides the images, generates this cloud's CA, certificates and passwords, starts and provisions the cloud.\n"
				+ (federation
						? "2. Send `out/" + m.cloud() + "-federation-request.tar.gz` to the organisation host and run `org/sign-cloud.sh` there.\n"
								+ "3. `./import-federation.sh <" + m.cloud() + "-federation.tar.gz>` - installs the cross-certificate, relay certificates and organisation CRL and restarts Gatekeeper and GateTunnel.\n"
								+ "4. `./intercloud.sh` - registers the neighbour clouds and the inter-cloud grants of " + d.name() + " and verifies them.\n"
								+ "5. `./check.sh` - smoke test and security checks.\n"
						: "2. `./check.sh` - smoke test and security checks.\n")
				+ "\n`cloud.json` holds this cloud's parameters; `federation-spec.json` the exact neighbours, grants and checks the SoLC plan derived.\n";
	}

	private static String wrapper(final String script, final String what) {
		return "#!/bin/sh\n# SPDX-License-Identifier: EPL-2.0\n# " + what + " (generated by " + SoLocalCloudConfiguratorConstants.SYSTEM_NAME + ")\n"
				+ "here=$(CDPATH= cd -- \"$(dirname -- \"$0\")\" && pwd)\n"
				+ "exec \"$here/../../kit/bin/" + script + "\" \"$here/cloud.json\" \"$@\"\n";
	}

	private static String imagesReadme(final SolcDesign d) {
		return "# Images\n\nImage mode: " + d.packaging().imageMode() + "; architectures: " + String.join(", ", d.packaging().architectures()) + ".\n\n"
				+ ("prebuilt".equals(d.packaging().imageMode())
						? "The host assembler adds `images/<arch>/images.tar` (docker save of every image of the package, for linux/<arch>) and\n"
								+ "`images/<arch>/images.sha256`. On a machine with Docker and the source checkout:\n\n"
								+ "    kit/bin/assemble-package.sh <package-dir-or-tar.gz> --source <checkout> --arch " + String.join(",", d.packaging().architectures()) + "\n\n"
								+ "`clouds/<Cloud>/install.sh` loads the archive of its host's architecture.\n"
						: "Build-on-target package: each host builds its images from the pinned source; this folder stays empty.\n");
	}
}
