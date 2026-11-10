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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.dto.AugmentedMeasurementsDTO;

@ExtendWith(MockitoExtension.class)
public class AugmentedMeasurementDriverTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AugmentedMeasurementDriver driver;

	@Mock
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Mock
	private HttpService httpService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		ReflectionTestUtils.setField(driver, "agentSslEnabled", true);
		ReflectionTestUtils.setField(driver, "agentPort", 59473);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInit1() {
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(100L);

		ReflectionTestUtils.invokeMethod(driver, "init");

		assertEquals("30", ReflectionTestUtils.getField(driver, "batchSize"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInit2() {
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getAugmentedMeasurementJobInterval()).thenReturn(10L);

		ReflectionTestUtils.invokeMethod(driver, "init");

		assertEquals("10", ReflectionTestUtils.getField(driver, "batchSize"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitRefusesAgentTlsWithoutSystemTls() {
		when(sysInfo.isSslEnabled()).thenReturn(false);

		final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ReflectionTestUtils.invokeMethod(driver, "init"));

		assertEquals(true, ex.getMessage().contains("device.agent.ssl.enabled=true"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitRefusesInvalidPort() {
		ReflectionTestUtils.setField(driver, "agentPort", 70000);

		assertThrows(IllegalArgumentException.class, () -> ReflectionTestUtils.invokeMethod(driver, "init"));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testFetchUsesMutualTlsWithoutIdentityToken() {
		final AugmentedMeasurementsDTO answer = new AugmentedMeasurementsDTO();
		answer.put("test", List.of(1.));
		ReflectionTestUtils.setField(driver, "batchSize", "10");
		final ArgumentCaptor<UriComponents> uri = ArgumentCaptor.forClass(UriComponents.class);
		when(httpService.sendRequest(uri.capture(), eq(HttpMethod.GET), eq(AugmentedMeasurementsDTO.class), isNull(), isNull(), isNull())).thenReturn(answer);

		final AugmentedMeasurementsDTO result = driver.fetch("10.0.0.1");

		assertEquals(answer, result);
		assertEquals("https://10.0.0.1:59473/device-qos?batch=10", uri.getValue().toUriString());
		// no custom headers: the device agent never receives the system's identity token
		verify(httpService).sendRequest(uri.getValue(), HttpMethod.GET, AugmentedMeasurementsDTO.class, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testFetchPlainOnlyWhenExplicitlyDisabled() {
		ReflectionTestUtils.setField(driver, "agentSslEnabled", false);
		ReflectionTestUtils.setField(driver, "agentPort", 1234);
		ReflectionTestUtils.setField(driver, "batchSize", "5");
		final ArgumentCaptor<UriComponents> uri = ArgumentCaptor.forClass(UriComponents.class);
		when(httpService.sendRequest(uri.capture(), eq(HttpMethod.GET), eq(AugmentedMeasurementsDTO.class), isNull(), isNull(), isNull())).thenReturn(new AugmentedMeasurementsDTO());

		driver.fetch("device1");

		assertEquals("http://device1:1234/device-qos?batch=5", uri.getValue().toUriString());
	}
}
