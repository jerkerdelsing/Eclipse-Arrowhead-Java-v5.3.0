/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
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
package eu.arrowhead.common.service.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ServiceInstancIdUtilsTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdSystemNameNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.calculateInstanceId(null, null, null));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdSystemNameEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.calculateInstanceId("", null, null));

		assertEquals("systemName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdServiceDefNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.calculateInstanceId("SystemName", null, null));

		assertEquals("serviceDefinitionName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdServiceDefEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.calculateInstanceId("SystemName", "", null));

		assertEquals("serviceDefinitionName is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdVersionNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.calculateInstanceId("SystemName", "serviceDef", null));

		assertEquals("version is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdVersionEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.calculateInstanceId("SystemName", "serviceDef", ""));

		assertEquals("version is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdOk() {
		assertEquals("SystemName|serviceDef|1.0.1", ServiceInstanceIdUtils.calculateInstanceId("SystemName", "serviceDef", "1.0.1"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRetrieveSystemNameFromInstanceIdInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.retrieveSystemNameFromInstanceId(null));

		assertEquals("instanceId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRetrieveSystemNameFromInstanceIdInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.retrieveSystemNameFromInstanceId(""));

		assertEquals("instanceId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRetrieveSystemNameFromInstanceIdInputInvalid() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.retrieveSystemNameFromInstanceId("SystemName"));

		assertEquals("Invalid instanceId", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRetrieveSystemNameFromInstanceIdOk() {
		assertEquals("SystemName", ServiceInstanceIdUtils.retrieveSystemNameFromInstanceId("SystemName|serviceDef|1.0.2"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBreakDownInstanceIdInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.breakDownInstanceId(null));

		assertEquals("instanceId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBreakDownInstanceIdInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.breakDownInstanceId(""));

		assertEquals("instanceId is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBreakDownInstanceIdInputInvalid() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> ServiceInstanceIdUtils.breakDownInstanceId("SystemName"));

		assertEquals("Invalid instanceId", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testBreakDownInstanceIdOk() {
		final ServiceInstanceIdParts expected = new ServiceInstanceIdParts("SystemName", "serviceDef", "1.0.2");
		final ServiceInstanceIdParts result = ServiceInstanceIdUtils.breakDownInstanceId("SystemName|serviceDef|1.0.2");

		assertEquals(expected, result);
	}
}