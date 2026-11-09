package eu.arrowhead.graphdbmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.sysmonitor.SysMonitorHealth;

@ExtendWith(MockitoExtension.class)
class GraphDBManagerHealthIndicatorTest {

	@Mock
	private ProxyService proxyService;

	@Mock
	private HistorianService historianService;

	@Test
	void aliveWhileHistorianAnswers() {
		when(proxyService.getSystems()).thenReturn(List.of());
		when(historianService.getSystems()).thenReturn(List.of("RandomDataProducer"));

		final SysMonitorHealth health = new GraphDBManagerHealthIndicator(proxyService, historianService).health();

		assertTrue(health.healthy());
		assertEquals("dataProxy systems: 0, dataHistorian systems: 1", health.detail());
	}

	@Test
	void degradedWhenHistorianFails() {
		when(proxyService.getSystems()).thenReturn(List.of());
		when(historianService.getSystems()).thenThrow(new IllegalStateException("database down"));

		assertFalse(new GraphDBManagerHealthIndicator(proxyService, historianService).health().healthy());
	}
}
