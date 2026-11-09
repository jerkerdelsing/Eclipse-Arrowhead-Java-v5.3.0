/*******************************************************************************
 *
 * Copyright (c) 2026 AITIA
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
package eu.arrowhead.deviceqosevaluator.driver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.dto.AugmentedMeasurementsDTO;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
public class AugmentedMeasurementDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AugmentedMeasurementDriver driver;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInit1() {
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(100L);

		ReflectionTestUtils.invokeMethod(driver, "init");

		assertNotNull(ReflectionTestUtils.getField(driver, "client"));
		assertEquals("30", ReflectionTestUtils.getField(driver, "batchSize"));

		verify(sysInfo).getAugmentedMeasurementJobInterval();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInit2() {
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(10L);

		ReflectionTestUtils.invokeMethod(driver, "init");

		assertNotNull(ReflectionTestUtils.getField(driver, "client"));
		assertEquals("10", ReflectionTestUtils.getField(driver, "batchSize"));

		verify(sysInfo).getAugmentedMeasurementJobInterval();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "checkstyle:MagicNumber" })
	@Test
	public void testFetch() {
		final WebClient clientMock = Mockito.mock(WebClient.class);
		final RequestBodyUriSpec specMock = Mockito.mock(RequestBodyUriSpec.class);
		final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
		final Mono<AugmentedMeasurementsDTO> monoSpec = (Mono<AugmentedMeasurementsDTO>) Mockito.mock(Mono.class);
		final AugmentedMeasurementsDTO blockAnswer = new AugmentedMeasurementsDTO();
		blockAnswer.put("test", List.of(1.));
		ReflectionTestUtils.setField(driver, "client", clientMock);
		ReflectionTestUtils.setField(driver, "batchSize", "10");

		when(clientMock.method(HttpMethod.GET)).thenReturn(specMock);
		when(specMock.uri(any(URI.class))).thenReturn(specMock);
		when(specMock.retrieve()).thenReturn(responseSpecMock);
		when(responseSpecMock.bodyToMono(AugmentedMeasurementsDTO.class)).thenReturn(monoSpec);
		when(monoSpec.block(Duration.ofMillis(5000))).thenReturn(blockAnswer);

		final AugmentedMeasurementsDTO result = driver.fetch("10.0.0.1");

		assertNotNull(result);
		assertEquals(blockAnswer, result);

		verify(clientMock).method(HttpMethod.GET);
		verify(specMock).uri(any(URI.class));
		verify(specMock).retrieve();
		verify(responseSpecMock).bodyToMono(AugmentedMeasurementsDTO.class);
		verify(monoSpec).block(Duration.ofMillis(5000));
	}
}