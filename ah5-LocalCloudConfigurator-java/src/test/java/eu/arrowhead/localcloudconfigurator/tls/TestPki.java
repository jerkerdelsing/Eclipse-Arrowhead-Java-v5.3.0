package eu.arrowhead.localcloudconfigurator.tls;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

// A throw-away cloud CA (TestCloud.Company.arrowhead.eu) issuing Arrowhead system certificates as PKCS12 key stores.
public final class TestPki {

	public static final char[] PASSWORD = "123456".toCharArray();

	private final KeyPair caKeys;
	private final X509Certificate ca;
	private final String suffix;

	public TestPki(final String suffix) throws Exception {
		this.suffix = suffix;
		caKeys = key();
		final X500Name name = new X500Name("C=SE,O=Company,CN=" + suffix);
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date.from(Instant.now().minusSeconds(3600)),
				Date.from(Instant.now().plus(Duration.ofDays(30))), name, caKeys.getPublic());
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
		builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
		ca = new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(caKeys.getPrivate())));
	}

	public static KeyPair key() throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);
		return generator.generateKeyPair();
	}

	public record Issued(byte[] keyStore, X509Certificate certificate) {
	}

	public Issued issue(final String systemName) throws Exception {
		final KeyPair keys = key();
		final X500Name subject = new X500Name("C=SE,O=Company,OU=Arrowhead Systems,CN=" + systemName + "." + suffix);
		final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(X500Name.getInstance(ca.getSubjectX500Principal().getEncoded()),
				new BigInteger(100, new SecureRandom()), Date.from(Instant.now().minusSeconds(3600)), Date.from(Instant.now().plus(Duration.ofDays(30))), subject,
				keys.getPublic());
		builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
		final X509Certificate certificate = new JcaX509CertificateConverter()
				.getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(caKeys.getPrivate())));
		final KeyStore store = KeyStore.getInstance("PKCS12");
		store.load(null, null);
		store.setKeyEntry(systemName, keys.getPrivate(), PASSWORD, new Certificate[] { certificate, ca });
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		store.store(out, PASSWORD);
		return new Issued(out.toByteArray(), certificate);
	}

	public byte[] trustStore() throws Exception {
		final KeyStore store = KeyStore.getInstance("PKCS12");
		store.load(null, null);
		store.setCertificateEntry("ca", ca);
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		store.store(out, PASSWORD);
		return out.toByteArray();
	}

	public Path crl(final Path file, final X509Certificate... revoked) throws Exception {
		final X509v2CRLBuilder builder = new X509v2CRLBuilder(X500Name.getInstance(ca.getSubjectX500Principal().getEncoded()), Date.from(Instant.now().minusSeconds(60)));
		builder.setNextUpdate(Date.from(Instant.now().plus(Duration.ofDays(1))));
		for (final X509Certificate c : revoked) {
			builder.addCRLEntry(c.getSerialNumber(), new Date(), CRLReason.keyCompromise);
		}
		final X509CRL crl = new JcaX509CRLConverter().getCRL(builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(caKeys.getPrivate())));
		Files.write(file, crl.getEncoded());
		return file;
	}

	public SSLContext context(final Issued identity, final Path crl, final String... acceptedPeers) throws Exception {
		return MutualTls.context(new ByteArrayInputStream(identity.keyStore()), PASSWORD, PASSWORD, new ByteArrayInputStream(trustStore()), PASSWORD, crl,
				Set.of(acceptedPeers));
	}
}
