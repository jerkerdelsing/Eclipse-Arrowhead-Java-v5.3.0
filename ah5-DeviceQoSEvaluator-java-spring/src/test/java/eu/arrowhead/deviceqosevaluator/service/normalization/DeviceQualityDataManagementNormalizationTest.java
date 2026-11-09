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
package eu.arrowhead.deviceqosevaluator.service.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.QoSDeviceStatQueryRequestDTO;

@ExtendWith(MockitoExtension.class)
public class DeviceQualityDataManagementNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private DeviceQualityDataManagementNormalization normalizer;

	@Mock
	private SystemNameNormalizer sysNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQoSDeviceStatQueryRequestDTONullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeQoSDeviceStatQueryRequestDTO(null));

		assertEquals("dto is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQoSDeviceStatQueryRequestDTOMetricGroupNull() {
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				null,
				null,
				null,
				null,
				null,
				null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto));

		assertEquals("metric group is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testNormalizeQoSDeviceStatQueryRequestDTOOk1() {
		final PageDTO page = new PageDTO(0, 10, "ACV", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				page,
				"rtt",
				null,
				null,
				null,
				null);

		final QoSDeviceStatQueryRequestDTO result = normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto);

		assertNotNull(result);
		assertEquals(page, result.pagination());
		assertEquals("RTT", result.metricGroup());
		assertNull(result.from());
		assertNull(result.to());
		assertNotNull(result.aggregation());
		assertEquals(5, result.aggregation().size());
		assertTrue(result.aggregation().contains("MINIMUM"));
		assertTrue(result.aggregation().contains("MAXIMUM"));
		assertTrue(result.aggregation().contains("MEAN"));
		assertTrue(result.aggregation().contains("MEDIAN"));
		assertTrue(result.aggregation().contains("CURRENT"));
		assertNull(result.systemNames());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeQoSDeviceStatQueryRequestDTOOk2() {
		final PageDTO page = new PageDTO(0, 10, "ACV", "id");
		final QoSDeviceStatQueryRequestDTO dto = new QoSDeviceStatQueryRequestDTO(
				page,
				"rtt",
				"2026-04-10T10:00:00Z ",
				"2026-04-10T12:00:00Z ",
				List.of("maximum"),
				List.of("TestSystem "));

		when(sysNameNormalizer.normalize("TestSystem ")).thenReturn("TestSystem");

		final QoSDeviceStatQueryRequestDTO result = normalizer.normalizeQoSDeviceStatQueryRequestDTO(dto);

		assertNotNull(result);
		assertEquals(page, result.pagination());
		assertEquals("RTT", result.metricGroup());
		assertEquals("2026-04-10T10:00:00Z", result.from());
		assertEquals("2026-04-10T12:00:00Z", result.to());
		assertNotNull(result.aggregation());
		assertEquals(1, result.aggregation().size());
		assertEquals("MAXIMUM", result.aggregation().get(0));
		assertEquals(1, result.systemNames().size());
		assertEquals("TestSystem", result.systemNames().get(0));
	}
}