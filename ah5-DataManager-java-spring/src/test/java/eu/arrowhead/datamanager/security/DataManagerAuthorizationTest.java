package eu.arrowhead.datamanager.security;

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
import eu.arrowhead.datamanager.security.DataManagerOperationResolver.ServiceOperation;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;

@ExtendWith(MockitoExtension.class)
class DataManagerAuthorizationTest {

	private static final long CACHE_PLUS_ONE = 30001;

	@Mock
	private ArrowheadHttpService httpService;

	@Test
	void resolvesEveryDataOperation() {
		assertEquals(Optional.of(new ServiceOperation("dataHistorian", "list-systems")),
				DataManagerOperationResolver.resolve("GET", "/datamanager/data-historian/systems"));
		assertEquals(Optional.of(new ServiceOperation("dataProxy", "list-services")),
				DataManagerOperationResolver.resolve("GET", "/datamanager/data-proxy/systems/WeatherStation/services"));
		assertEquals(Optional.of(new ServiceOperation("dataHistorian", "put-data")),
				DataManagerOperationResolver.resolve("PUT", "/datamanager/data-historian/systems/RandomDataProducer/services/randomNumber/data"));
		assertEquals(Optional.of(new ServiceOperation("dataProxy", "get-data")),
				DataManagerOperationResolver.resolve("GET", "/datamanager/data-proxy/systems/A/services/b/data"));
	}

	@Test
	void ignoresUnknownPathsAndMethods() {
		assertTrue(DataManagerOperationResolver.resolve("GET", "/datamanager/echo").isEmpty());
		assertTrue(DataManagerOperationResolver.resolve("DELETE", "/datamanager/data-proxy/systems/A/services/b/data").isEmpty());
		assertTrue(DataManagerOperationResolver.resolve("GET", "/swagger-ui.html").isEmpty());
	}

	@Test
	void extractsSystemNameFromCertificateSubject() {
		assertEquals("RandomDataProducer",
				DataManagerAuthorizationFilter.systemNameFromSubject("CN=RandomDataProducer.TestCloud.Company.arrowhead.eu,OU=Arrowhead Systems,O=Company,C=SE"));
	}

	@Test
	void cachesDecisionsAndAsksAgainAfterExpiry() {
		final AtomicLong now = new AtomicLong(1000);
		final DataManagerAuthorizationService service = new DataManagerAuthorizationService(httpService, now::get);
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenReturn(true);

		assertTrue(service.isAuthorized("SysMonitor", "dataHistorian", "put-data"));
		assertTrue(service.isAuthorized("SysMonitor", "dataHistorian", "put-data"));
		now.addAndGet(CACHE_PLUS_ONE);
		assertTrue(service.isAuthorized("SysMonitor", "dataHistorian", "put-data"));

		verify(httpService, times(2)).consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
	}

	@Test
	void failsClosedWhenConsumerAuthorizationIsUnreachable() {
		final DataManagerAuthorizationService service = new DataManagerAuthorizationService(httpService, () -> 0L);
		when(httpService.consumeService(eq(Constants.SERVICE_DEF_AUTHORIZATION), eq(Constants.SERVICE_OP_VERIFY), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class)))
				.thenThrow(new ExternalServerError("down"));

		assertFalse(service.isAuthorized("RandomDataProducer", "dataHistorian", "put-data"));
	}
}
