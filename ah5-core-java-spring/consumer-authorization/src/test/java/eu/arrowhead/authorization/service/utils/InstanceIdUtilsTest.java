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
package eu.arrowhead.authorization.service.utils;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import eu.arrowhead.dto.enums.AuthorizationLevel;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

public class InstanceIdUtilsTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdLevelNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> InstanceIdUtils.calculateInstanceId(null, "LOCAL", "ProviderName", AuthorizationTargetType.SERVICE_DEF, "testService"));

		assertEquals("level is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdCloudEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> InstanceIdUtils.calculateInstanceId(AuthorizationLevel.MGMT, "", "ProviderName", AuthorizationTargetType.SERVICE_DEF, "testService"));

		assertEquals("cloud is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdProviderNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> InstanceIdUtils.calculateInstanceId(AuthorizationLevel.MGMT, "LOCAL", null, AuthorizationTargetType.SERVICE_DEF, "testService"));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdTargetTypeNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> InstanceIdUtils.calculateInstanceId(AuthorizationLevel.MGMT, "LOCAL", "ProviderName", null, "testService"));

		assertEquals("targetType is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdTargetEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> InstanceIdUtils.calculateInstanceId(AuthorizationLevel.MGMT, "LOCAL", "ProviderName", AuthorizationTargetType.SERVICE_DEF, ""));

		assertEquals("target is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateInstanceIdOk() {
		assertAll("calculateInstanceId",
				() -> assertEquals("MGMT|LOCAL|ProviderName|SERVICE_DEF|testService",
						InstanceIdUtils.calculateInstanceId(AuthorizationLevel.MGMT, "LOCAL", "ProviderName", AuthorizationTargetType.SERVICE_DEF, "testService")),
				() -> assertEquals("PR|TestCloud|Company|ProviderName|EVENT_TYPE|testEvent",
						InstanceIdUtils.calculateInstanceId(AuthorizationLevel.PROVIDER, "TestCloud|Company", "ProviderName", AuthorizationTargetType.EVENT_TYPE, "testEvent")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRetrieveProviderNameInstanceIdEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> InstanceIdUtils.retrieveProviderName(""));

		assertEquals("Instance id is null or empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRetrieveProviderNameInstanceIdTooShort() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> InstanceIdUtils.retrieveProviderName("MGMT|LOCAL|ProviderName|SERVICE_DEF"));

		assertEquals("Invalid instance id", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRetrieveProviderNameInstanceIdTooShortForForeignCloud() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> InstanceIdUtils.retrieveProviderName("MGMT|TestCloud|Company|ProviderName|SERVICE_DEF"));

		assertEquals("Invalid instance id", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRetrieveProviderNameOk() {
		assertAll("retrieveProviderName",
				() -> assertEquals("ProviderName", InstanceIdUtils.retrieveProviderName("MGMT|LOCAL|ProviderName|SERVICE_DEF|testService")),
				() -> assertEquals("ProviderName", InstanceIdUtils.retrieveProviderName("PR|TestCloud|Company|ProviderName|EVENT_TYPE|testEvent")));
	}
}