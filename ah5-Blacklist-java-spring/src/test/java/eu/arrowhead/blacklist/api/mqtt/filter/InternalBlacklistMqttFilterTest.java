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
package eu.arrowhead.blacklist.api.mqtt.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.blacklist.service.DiscoveryService;
import eu.arrowhead.blacklist.service.normalization.Normalization;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.MqttRequestTemplate;

@ExtendWith(MockitoExtension.class)
public class InternalBlacklistMqttFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private InternalBlacklistMqttFilter filter;

	@Mock
	private DiscoveryService discoveryService;

	@Mock
	private Normalization normalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testOrder() {
		final int result = filter.order();
		assertEquals(20, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterRequesterNull() {
		final MqttRequestModel model = new MqttRequestModel("base/", "test", new MqttRequestTemplate(null, null, null, 0, null, null));

		final ArrowheadException ex = assertThrows(
				AuthException.class,
				() -> filter.doFilter(null, model));

		assertEquals("Unknown requester system", ex.getMessage());
		assertEquals("base/test", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterRequesterEmpty() {
		final MqttRequestModel model = new MqttRequestModel("base/", "test", new MqttRequestTemplate(null, null, null, 0, null, null));
		model.setRequester("");

		final ArrowheadException ex = assertThrows(
				AuthException.class,
				() -> filter.doFilter(null, model));

		assertEquals("Unknown requester system", ex.getMessage());
		assertEquals("base/test", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterForbidden1() {
		final MqttRequestModel model = new MqttRequestModel("base/", "test", new MqttRequestTemplate(null, null, null, 0, null, null));
		model.setRequester("TestSystem");
		model.setSysOp(false);

		when(discoveryService.check("TestSystem", "base/test")).thenReturn(true);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> filter.doFilter(null, model));

		assertEquals("TestSystem system is blacklisted", ex.getMessage());
		assertEquals("base/test", ex.getOrigin());

		verify(discoveryService).check("TestSystem", "base/test");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterForbidden2() {
		final MqttRequestModel model = new MqttRequestModel("arrowhead/blacklist/", "test", new MqttRequestTemplate(null, null, null, 0, null, null));
		model.setRequester("TestSystem");
		model.setSysOp(false);

		when(discoveryService.check("TestSystem", "arrowhead/blacklist/test")).thenReturn(true);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> filter.doFilter(null, model));

		assertEquals("TestSystem system is blacklisted", ex.getMessage());
		assertEquals("arrowhead/blacklist/test", ex.getOrigin());

		verify(discoveryService).check("TestSystem", "arrowhead/blacklist/test");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterForbidden3() {
		final MqttRequestModel model = new MqttRequestModel("arrowhead/blacklist/", "check", new MqttRequestTemplate(null, null, null, 0, null, "OtherSystem"));
		model.setRequester("TestSystem");
		model.setSysOp(false);

		when(normalizer.normalizeSystemName("TestSystem")).thenReturn("TestSystem");
		when(normalizer.normalizeSystemName("OtherSystem")).thenReturn("OthertSystem");
		when(discoveryService.check("TestSystem", "arrowhead/blacklist/check")).thenReturn(true);

		final ArrowheadException ex = assertThrows(
				ForbiddenException.class,
				() -> filter.doFilter(null, model));

		assertEquals("TestSystem system is blacklisted", ex.getMessage());
		assertEquals("arrowhead/blacklist/check", ex.getOrigin());

		verify(normalizer).normalizeSystemName("TestSystem");
		verify(normalizer).normalizeSystemName("OtherSystem");
		verify(discoveryService).check("TestSystem", "arrowhead/blacklist/check");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterOk1() {
		final MqttRequestModel model = new MqttRequestModel("arrowhead/blacklist/", "check", new MqttRequestTemplate(null, null, null, 0, null, null));
		model.setRequester("TestSystem");
		model.setSysOp(true);

		assertDoesNotThrow(() -> filter.doFilter(null, model));

		verify(normalizer, never()).normalizeSystemName(anyString());
		verify(discoveryService, never()).check(anyString(), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterOk2() {
		final MqttRequestModel model = new MqttRequestModel("arrowhead/blacklist/", "check", new MqttRequestTemplate(null, null, null, 0, null, "TestSystem"));
		model.setRequester("TestSystem");
		model.setSysOp(false);

		when(normalizer.normalizeSystemName("TestSystem")).thenReturn("TestSystem");

		assertDoesNotThrow(() -> filter.doFilter(null, model));

		verify(normalizer, times(2)).normalizeSystemName("TestSystem");
		verify(discoveryService, never()).check(anyString(), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterOk3() {
		final MqttRequestModel model = new MqttRequestModel("arrowhead/blacklist/", "lookup", new MqttRequestTemplate(null, null, null, 0, null, null));
		model.setRequester("TestSystem");
		model.setSysOp(false);

		assertDoesNotThrow(() -> filter.doFilter(null, model));

		verify(normalizer, never()).normalizeSystemName(anyString());
		verify(discoveryService, never()).check(anyString(), anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterOk4() {
		final MqttRequestModel model = new MqttRequestModel("arrowhead/blacklist/", "test", new MqttRequestTemplate(null, null, null, 0, null, null));
		model.setRequester("TestSystem");
		model.setSysOp(false);

		when(discoveryService.check("TestSystem", "arrowhead/blacklist/test")).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilter(null, model));

		verify(normalizer, never()).normalizeSystemName(anyString());
		verify(discoveryService).check("TestSystem", "arrowhead/blacklist/test");
	}
}