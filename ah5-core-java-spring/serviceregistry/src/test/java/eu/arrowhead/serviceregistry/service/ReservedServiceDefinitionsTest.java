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
package eu.arrowhead.serviceregistry.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.exception.ForbiddenException;

// J9: core and support service definitions may only be registered by their owning systems.
public class ReservedServiceDefinitionsTest {

	//-------------------------------------------------------------------------------------------------
	@Test
	public void ownersMayRegisterOthersAreRefused() {
		final ReservedServiceDefinitions reserved = new ReservedServiceDefinitions("");
        assertDoesNotThrow(() -> reserved.check("DynamicServiceOrchestration", "serviceOrchestrationPlantManagement", "origin"));
        assertThrows(ForbiddenException.class, () -> reserved.check("Other", "serviceOrchestrationPlantManagement", "origin"));
		assertDoesNotThrow(() -> reserved.check("CertificateAuthority", "certificateSigning", "origin"));
		assertDoesNotThrow(() -> reserved.check("SimpleStoreServiceOrchestration", "serviceOrchestration", "origin"));
		final ForbiddenException ex = assertThrows(ForbiddenException.class, () -> reserved.check("RandomDataProducer", "certificateSigning", "origin"));
		assertEquals("Service definition certificateSigning is reserved for CertificateAuthority", ex.getMessage());
		assertThrows(ForbiddenException.class, () -> reserved.check("RandomDataProducer", "identity", "origin"));
		// shared definitions and application services are open
		assertDoesNotThrow(() -> reserved.check("RandomDataProducer", "sysMonitor", "origin"));
		assertDoesNotThrow(() -> reserved.check("RandomDataProducer", "generalManagement", "origin"));
		assertDoesNotThrow(() -> reserved.check("RandomDataProducer", "randomNumberProducer", "origin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void overridesAddReplaceAndLiftReservations() {
		final ReservedServiceDefinitions reserved = new ReservedServiceDefinitions("randomNumberProducer:RandomDataProducer, time:TimeManager|ClockBridge, aiGym:");
		assertThrows(ForbiddenException.class, () -> reserved.check("Other", "randomNumberProducer", "origin"));
		assertDoesNotThrow(() -> reserved.check("ClockBridge", "time", "origin"));
		assertDoesNotThrow(() -> reserved.check("Other", "aiGym", "origin"));
		assertThrows(IllegalArgumentException.class, () -> new ReservedServiceDefinitions("noColon"));
	}
}
