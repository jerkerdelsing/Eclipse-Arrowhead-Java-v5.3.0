package eu.arrowhead.common.sysmonitor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class SysMonitorObservationsTest {

	//=================================================================================================
	// members

	private static final Instant NOW = Instant.parse("2026-10-11T12:00:00.123456Z");
	private static final long UPTIME = 42;
	private static final int SERVICES = 3;

	private final SysMonitorObservations observations = new SysMonitorObservations("Blacklist", "TestCloud", "Blacklist");

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void topicsFollowTheSysMonitorContract() {
		assertEquals("TestCloud.sysmonitor.Blacklist.Blacklist", observations.baseRoutingKey());
		assertEquals("TestCloud.sysmonitor.Blacklist.Blacklist.status", observations.routingKey(SysMonitorObservations.TYPE_STATUS));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void echoCarriesEnvelopeAndHealth() {
		final Map<String, Object> echo = observations.echo(NOW, UPTIME, SysMonitorHealth.alive("ok"));

		assertEquals("1.0", echo.get("schemaVersion"));
		assertEquals("Blacklist", echo.get("providerSystem"));
		assertEquals("echo", echo.get("observationType"));
		assertEquals("2026-10-11T12:00:00.123Z", echo.get("sourceTime"));
		assertEquals("good", echo.get("quality"));
		assertEquals(SysMonitorObservations.STATUS_ALIVE, value(echo).get("status"));
		assertEquals(UPTIME, value(echo).get("uptimeSeconds"));
		assertEquals(Map.of("publisherMode", "native"), echo.get("metadata"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void degradedHealthLowersQuality() {
		final Map<String, Object> echo = observations.echo(NOW, 1, SysMonitorHealth.degraded("database unavailable"));

		assertEquals(SysMonitorObservations.STATUS_DEGRADED, value(echo).get("status"));
		assertEquals("uncertain", echo.get("quality"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void statusAndPowerHaveUnitsAndUniqueIds() {
		final Map<String, Object> status = observations.status(NOW, SysMonitorHealth.alive(null), SERVICES);
		final Map<String, Object> power = observations.power(NOW);

		assertEquals(SERVICES, value(status).get("serviceCount"));
		assertEquals("1", status.get("unit"));
		assertEquals("line", value(power).get("supplyType"));
		assertNotEquals(status.get("observationId"), power.get("observationId"));
		assertEquals((long) status.get("sequence") + 1, power.get("sequence"));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private static Map<String, Object> value(final Map<String, Object> observation) {
		return (Map<String, Object>) observation.get("value");
	}
}
