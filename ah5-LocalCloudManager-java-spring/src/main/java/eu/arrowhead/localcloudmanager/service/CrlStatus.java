package eu.arrowhead.localcloudmanager.service;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.time.Instant;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Remaining validity of the cloud CA's CRL (security finding J16). Every relying party refuses all peers once the CRL is past its
 * nextUpdate, so an expiring CRL is a cloud-wide incident that the console reports before it happens.
 */
public final class CrlStatus {

	public static final int WARN_HOURS = 12;

	private CrlStatus() {
		throw new UnsupportedOperationException();
	}

	public static X509CRL parse(final byte[] data) throws Exception {
		return (X509CRL) CertificateFactory.getInstance("X.509").generateCRL(new ByteArrayInputStream(data));
	}

	/** { state: ok | warning | expired | unavailable, thisUpdate, nextUpdate, hoursRemaining, message } */
	public static ObjectNode status(final String file, final long now) {
		final ObjectNode out = Json.obj();
		if (file == null || file.isBlank()) {
			return out.put("state", "unavailable").put("message", "No CRL configured (TLS_CRL_FILE)");
		}

		final X509CRL crl;
		try {
			crl = parse(Files.readAllBytes(Path.of(file)));
		} catch (final Exception ex) {
			return out.put("state", "unavailable").put("message", "CRL " + file + " unreadable: " + ex.getMessage());
		}

		final String thisUpdate = Json.iso(crl.getThisUpdate().toInstant());
		if (crl.getNextUpdate() == null) {
			out.put("state", "ok").put("thisUpdate", thisUpdate).putNull("nextUpdate").putNull("hoursRemaining");
			return out;
		}

		final Instant next = crl.getNextUpdate().toInstant();
		final long hours = Math.floorDiv(next.toEpochMilli() - now, 3600000L);
		out.put("thisUpdate", thisUpdate).put("nextUpdate", Json.iso(next)).put("hoursRemaining", hours);
		if (next.toEpochMilli() <= now) {
			return out.put("state", "expired").put("message", "CRL expired at " + Json.iso(next) + ": every system refuses every peer; start the CertificateAuthority");
		}
		if (hours < WARN_HOURS) {
			return out.put("state", "warning").put("message", "CRL expires in " + hours + " h (" + Json.iso(next) + "); is the CertificateAuthority running?");
		}
		return out.put("state", "ok");
	}
}
