/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import eu.arrowhead.plantdescriptionengine.PlantDescriptionEngineConstants;

public class PlantDescriptionEngineOperationResolverTest {

	private static String op(final String method, final String path) {
		return PlantDescriptionEngineOperationResolver.resolve(method, path).map(PlantDescriptionEngineOperationResolver.ServiceOperation::operation).orElse(null);
	}

	@Test
	public void monitorOperations() {
		assertEquals(PlantDescriptionEngineConstants.SERVICE_OP_QUERY_PLANT_DESCRIPTIONS, op("GET", "/plantdescriptionengine/monitor/plant-descriptions"));
		assertEquals(PlantDescriptionEngineConstants.SERVICE_OP_GET_PLANT_DESCRIPTION, op("GET", "/plantdescriptionengine/monitor/plant-descriptions/3"));
		assertEquals(PlantDescriptionEngineConstants.SERVICE_OP_QUERY_ALARMS, op("GET", "/plantdescriptionengine/monitor/alarms/"));
		assertEquals(PlantDescriptionEngineConstants.SERVICE_OP_GET_ALARM, op("GET", "/plantdescriptionengine/monitor/alarms/7"));
		assertEquals(PlantDescriptionEngineConstants.SERVICE_OP_ACKNOWLEDGE_ALARM, op("PATCH", "/plantdescriptionengine/monitor/alarms/7"));
	}

	@Test
	public void otherPathsAreNotResolved() {
		assertTrue(PlantDescriptionEngineOperationResolver.resolve("GET", "/plantdescriptionengine/mgmt/plant-descriptions").isEmpty());
		assertTrue(PlantDescriptionEngineOperationResolver.resolve("DELETE", "/plantdescriptionengine/monitor/alarms/7").isEmpty());
		assertTrue(PlantDescriptionEngineOperationResolver.resolve("GET", "/plantdescriptionengine/monitor/other").isEmpty());
	}
}
