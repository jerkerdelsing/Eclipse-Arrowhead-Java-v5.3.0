/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.common.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.GeneralSecurityException;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

// Certificate revocation for the HTTPS server (security finding J4). With arrowhead.ssl.crl-file set (environment
// ARROWHEAD_SSL_CRL_FILE), Tomcat checks every client certificate against that CRL of the cloud CA during the TLS
// handshake, and the CRL is reloaded when the file changes. Fail closed, as the brokers: a revoked certificate, or a CRL
// that is missing, unreadable or past its nextUpdate, refuses the client.
// A system whose truststore holds further CAs (Onboarding: the device manufacturers' CAs of the bootstrap certificates)
// lists their CRLs too, comma-separated. Every trusted CA then needs a current CRL; a certificate of a CA without one is
// refused. Tomcat takes one CRL file, so several are merged into a PEM bundle in java.io.tmpdir, re-merged when any of
// them changes.
@Configuration
@ConditionalOnProperty(name = CrlRevocationConfig.CRL_FILE_PROPERTY)
public class CrlRevocationConfig {

	//=================================================================================================
	// members

	public static final String CRL_FILE_PROPERTY = "arrowhead.ssl.crl-file";
	public static final String CRL_POLL_PROPERTY = "arrowhead.ssl.crl-poll-seconds";

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> crlRevocationCustomizer(final Environment environment) {
		final List<Path> sources = Arrays.stream(environment.getRequiredProperty(CRL_FILE_PROPERTY).split(","))
				.map(String::trim)
				.filter(name -> !name.isEmpty())
				.map(Path::of)
				.toList();
		if (sources.isEmpty()) {
			throw new IllegalStateException(CRL_FILE_PROPERTY + " names no CRL file");
		}
		final Path crlFile = sources.size() == 1 ? sources.get(0) : mergedFile(sources);
		final long pollSeconds = environment.getProperty(CRL_POLL_PROPERTY, Long.class, 30L);

		return factory -> factory.addConnectorCustomizers(connector -> {
			final SSLHostConfig[] configs = connector.findSslHostConfigs();
			if (configs.length == 0) {
				logger.warn("{} is set, but the connector has no TLS configuration", CRL_FILE_PROPERTY);
				return;
			}

			for (final SSLHostConfig config : configs) {
				config.setCertificateRevocationListFile(crlFile.toString());
				config.setRevocationEnabled(true);
			}
			logger.info("Client certificates are checked against the CRL {}", sources);
			watch(connector, sources, crlFile, pollSeconds);
		});
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void watch(final Connector connector, final List<Path> sources, final Path crlFile, final long pollSeconds) {
		final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "crl-watch");
			thread.setDaemon(true);
			return thread;
		});
		final List<FileTime> last = modified(sources);

		scheduler.scheduleWithFixedDelay(() -> {
			final List<FileTime> current = modified(sources);
			if (current.contains(null) || current.equals(last)) {
				return;
			}

			if (connector.getProtocolHandler() instanceof final AbstractHttp11Protocol<?> protocol) {
				try {
					if (sources.size() > 1) {
						merge(sources, crlFile);
					}
					protocol.reloadSslHostConfigs();
					last.clear();
					last.addAll(current);
					logger.info("CRL {} changed; TLS configuration reloaded", sources);
				} catch (final IOException | GeneralSecurityException ex) {
					// the previous CRLs stay in force; they refuse every client once past their nextUpdate
					logger.error("Merging the changed CRLs {} failed: {}", sources, ex.getMessage());
				} catch (final RuntimeException ex) {
					logger.error("Reloading the TLS configuration after a CRL change failed: {}", ex.getMessage());
				}
			}
		}, pollSeconds, pollSeconds, TimeUnit.SECONDS);
	}

	//-------------------------------------------------------------------------------------------------
	private static Path mergedFile(final List<Path> sources) {
		try {
			final Path merged = Files.createTempFile("arrowhead-crl-", ".pem");
			merge(sources, merged);
			return merged;
		} catch (final IOException | GeneralSecurityException ex) {
			// fail closed: no TLS without every listed CRL
			throw new IllegalStateException("Cannot merge the CRLs " + sources + ": " + ex.getMessage(), ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Writes every CRL of the sources, PEM-encoded, to target (replaced atomically); fails if a source has none.
	private static void merge(final List<Path> sources, final Path target) throws IOException, GeneralSecurityException {
		final CertificateFactory factory = CertificateFactory.getInstance("X.509");
		final Base64.Encoder encoder = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII));
		final StringBuilder pem = new StringBuilder();
		for (final Path source : sources) {
			final Collection<? extends CRL> crls;
			try (InputStream in = Files.newInputStream(source)) {
				crls = factory.generateCRLs(in);
			}
			if (crls.isEmpty()) {
				throw new GeneralSecurityException("no CRL in " + source);
			}
			for (final CRL crl : crls) {
				pem.append("-----BEGIN X509 CRL-----\n")
						.append(encoder.encodeToString(((X509CRL) crl).getEncoded()))
						.append("\n-----END X509 CRL-----\n");
			}
		}

		final Path temp = Files.createTempFile(target.getParent(), "arrowhead-crl-", ".tmp");
		try {
			Files.writeString(temp, pem, StandardCharsets.US_ASCII);
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} finally {
			Files.deleteIfExists(temp);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static List<FileTime> modified(final List<Path> files) {
		return new ArrayList<>(files.stream().map(CrlRevocationConfig::modified).toList());
	}

	//-------------------------------------------------------------------------------------------------
	private static FileTime modified(final Path file) {
		try {
			return Files.getLastModifiedTime(file);
		} catch (final IOException ex) {
			return null;
		}
	}
}
