package eu.arrowhead.graphdbmanager.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.graphdbmanager.security.GraphDBManagerOperationResolver.ServiceOperation;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;

@ExtendWith(MockitoExtension.class)
class GraphDBManagerAuthorizationTest {

	private static final long CACHE_PLUS_ONE = 30001;

	@Mock
	private ArrowheadHttpService httpService;

	@Test
	void resolvesEveryDataOperation() {
		assertEquals(Optional.of(new ServiceOperation("graphDataHistorian", "list-systems")),
				GraphDBManagerOperationResolver.resolve("GET", "/graphdbmanager/data-historian/systems"));
		assertEquals(Optional.of(new ServiceOperation("graphDataProxy", "list-services")),
				GraphDBManagerOperationResolver.resolve("GET", "/graphdbmanager/data-proxy/systems/WeatherStation/services"));
		assertEquals(Optional.of(new ServiceOperation("graphDataHistorian", "put-data")),
				GraphDBManagerOperationResolver.resolve("PUT", "/graphdbmanager/data-historian/systems/RandomDataProducer/services/randomNumber/data"));
		assertEquals(Optional.of(new ServiceOperation("graphDataProxy", "get-data")),
				GraphDBManagerOperationResolver.resolve("GET", "/graphdbmanager/data-proxy/systems/A/services/b/data"));
	}

	@Test
	void ignoresUnknownPathsAndMethods() {
		assertTrue(GraphDBManagerOperationResolver.resolve("GET", "/graphdbmanager/echo").isEmpty());
		assertTrue(GraphDBManagerOperationResolver.resolve("DELETE", "/graphdbmanager/data-proxy/systems/A/services/b/data").isEmpty());
		assertTrue(GraphDBManagerOperationResolver.resolve("GET", "/swagger-ui.html").isEmpty());
	}

	@Test
	void extractsSystemNameFromCertificateSubject() {
		assertEquals("RandomDataProducer",
				GraphDBManagerAuthorizationFilter.systemNameFromSubject("CN=RandomDataProducer.TestCloud.Company.arrowhead.eu,OU=Arrowhead Systems,O=Company,C=SE"));
	}

	@Test
	void cachesDecisionsAndAsksAgainAfterExpiry() {
		final AtomicLong now = new AtomicLong(1000);
		final GraphDBManagerAuthorizationService service = new GraphDBManagerAuthorizationService(httpService, now::get);
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenReturn(true);

		assertTrue(service.isAuthorized("SysMonitor", "graphDataHistorian", "put-data"));
		assertTrue(service.isAuthorized("SysMonitor", "graphDataHistorian", "put-data"));
		now.addAndGet(CACHE_PLUS_ONE);
		assertTrue(service.isAuthorized("SysMonitor", "graphDataHistorian", "put-data"));

		verify(httpService, times(2)).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
	}

	@Test
	void failsClosedWhenConsumerAuthorizationIsUnreachable() {
		final GraphDBManagerAuthorizationService service = new GraphDBManagerAuthorizationService(httpService, () -> 0L);
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenThrow(new ExternalServerError("down"));

		assertFalse(service.isAuthorized("RandomDataProducer", "graphDataHistorian", "put-data"));
	}
}
