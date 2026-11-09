package eu.arrowhead.localcloudmanager.client;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.X509CRL;
import java.security.cert.X509CertSelector;
import java.util.EnumSet;
import java.util.List;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.core.io.ResourceLoader;

/** The client TLS context of LocalCloudManager: its system certificate, the cloud truststore and the cloud CRL (J4, fail closed). */
public final class TlsContextFactory {

	private TlsContextFactory() {
		throw new UnsupportedOperationException();
	}

	public static SSLContext create(final ResourceLoader resources, final String keyStore, final String keyStorePassword, final String keyPassword, final String trustStore,
			final String trustStorePassword, final String crlFile) throws Exception {
		final KeyStore keys = KeyStore.getInstance("PKCS12");
		try (InputStream in = resources.getResource(keyStore).getInputStream()) {
			keys.load(in, keyStorePassword.toCharArray());
		}
		final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keys, keyPassword.toCharArray());

		final KeyStore trust = KeyStore.getInstance("PKCS12");
		try (InputStream in = resources.getResource(trustStore).getInputStream()) {
			trust.load(in, trustStorePassword.toCharArray());
		}
		final PKIXBuilderParameters parameters = new PKIXBuilderParameters(trust, new X509CertSelector());
		parameters.setRevocationEnabled(false);
		if (crlFile != null && !crlFile.isBlank()) {
			final X509CRL crl;
			try (InputStream in = Files.newInputStream(Path.of(crlFile))) {
				crl = (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(in);
			}
			parameters.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(List.of(crl))));
			final PKIXRevocationChecker checker = (PKIXRevocationChecker) CertPathBuilder.getInstance("PKIX").getRevocationChecker();
			checker.setOptions(EnumSet.of(PKIXRevocationChecker.Option.PREFER_CRLS, PKIXRevocationChecker.Option.NO_FALLBACK));
			parameters.addCertPathChecker(checker);
			parameters.setRevocationEnabled(true);
		}

		final TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
		tmf.init(new CertPathTrustManagerParameters(parameters));
		final SSLContext context = SSLContext.getInstance("TLS");
		context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return context;
	}
}
