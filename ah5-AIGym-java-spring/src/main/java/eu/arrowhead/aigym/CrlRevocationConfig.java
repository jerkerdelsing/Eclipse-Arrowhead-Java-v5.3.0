package eu.arrowhead.aigym;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

// Same as eu.arrowhead.common.security.CrlRevocationConfig of the common library, which AIGym does not use.
// Certificate revocation for the HTTPS server (security finding J4). With arrowhead.ssl.crl-file set (environment
// ARROWHEAD_SSL_CRL_FILE), Tomcat checks every client certificate against that CRL of the cloud CA during the TLS
// handshake, and the CRL is reloaded when the file changes. Fail closed, as the brokers: a revoked certificate, or a CRL
// that is missing, unreadable or past its nextUpdate, refuses the client.
@Configuration
@ConditionalOnProperty(name = CrlRevocationConfig.CRL_FILE_PROPERTY)
public class CrlRevocationConfig {

	//=================================================================================================
	// members

	public static final String CRL_FILE_PROPERTY = "arrowhead.ssl.crl-file";
	public static final String CRL_POLL_PROPERTY = "arrowhead.ssl.crl-poll-seconds";

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> crlRevocationCustomizer(final Environment environment) {
		final Path crlFile = Path.of(environment.getRequiredProperty(CRL_FILE_PROPERTY));
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
			logger.info("Client certificates are checked against the CRL {}", crlFile);
			watch(connector, crlFile, pollSeconds);
		});
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void watch(final Connector connector, final Path crlFile, final long pollSeconds) {
		final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "crl-watch");
			thread.setDaemon(true);
			return thread;
		});
		final FileTime[] last = { modified(crlFile) };

		scheduler.scheduleWithFixedDelay(() -> {
			final FileTime current = modified(crlFile);
			if (current == null || current.equals(last[0])) {
				return;
			}

			if (connector.getProtocolHandler() instanceof final AbstractHttp11Protocol<?> protocol) {
				try {
					protocol.reloadSslHostConfigs();
					last[0] = current;
					logger.info("CRL {} changed; TLS configuration reloaded", crlFile);
				} catch (final RuntimeException ex) {
					logger.error("Reloading the TLS configuration after a CRL change failed: {}", ex.getMessage());
				}
			}
		}, pollSeconds, pollSeconds, TimeUnit.SECONDS);
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
