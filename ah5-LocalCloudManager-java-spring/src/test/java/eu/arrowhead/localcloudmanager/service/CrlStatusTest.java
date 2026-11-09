package eu.arrowhead.localcloudmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

// Port of the native crl-status.test.js (same fixtures: empty CRLs, lastUpdate 2026-10-20 12:00 UTC).
public class CrlStatusTest {

	private static final String FIXTURES = "src/test/resources/fixtures/";
	private static final long THIS_UPDATE = Instant.parse("2026-10-20T12:00:00Z").toEpochMilli();

	@Test
	public void readsThisUpdateAndNextUpdateFromPemAndDerUtcTimeAndGeneralizedTime() throws Exception {
		for (final String name : new String[] { "crl-utc.pem", "crl-utc.der" }) {
			final var crl = CrlStatus.parse(Files.readAllBytes(Path.of(FIXTURES + name)));
			assertEquals(THIS_UPDATE, crl.getThisUpdate().getTime());
			assertEquals("2026-10-23T12:00:00.000Z", Json.iso(crl.getNextUpdate().toInstant()));
		}
		assertEquals("2051-01-01T00:00:00.000Z", Json.iso(CrlStatus.parse(Files.readAllBytes(Path.of(FIXTURES + "crl-generalized.pem"))).getNextUpdate().toInstant()));
	}

	@Test
	public void okWithMarginWarningWithin12HoursExpiredAfterNextUpdate() {
		final long next = Instant.parse("2026-10-23T12:00:00Z").toEpochMilli();
		final ObjectNode ok = CrlStatus.status(FIXTURES + "crl-utc.pem", THIS_UPDATE);
		assertEquals("ok", ok.path("state").asText());
		assertEquals(72, ok.path("hoursRemaining").asInt());
		final ObjectNode warning = CrlStatus.status(FIXTURES + "crl-utc.pem", next - 5 * 3600000L);
		assertEquals("warning", warning.path("state").asText());
		assertEquals(5, warning.path("hoursRemaining").asInt());
		assertTrue(warning.path("message").asText().contains("CertificateAuthority"));
		assertEquals("expired", CrlStatus.status(FIXTURES + "crl-utc.pem", next + 1000).path("state").asText());
	}

	@Test
	public void aMissingOrUnreadableCrlIsReportedNotIgnored() {
		assertEquals("unavailable", CrlStatus.status("", 0).path("state").asText());
		assertEquals("unavailable", CrlStatus.status(FIXTURES + "does-not-exist.crl", 0).path("state").asText());
		assertEquals("unavailable", CrlStatus.status("pom.xml", 0).path("state").asText());
	}
}
