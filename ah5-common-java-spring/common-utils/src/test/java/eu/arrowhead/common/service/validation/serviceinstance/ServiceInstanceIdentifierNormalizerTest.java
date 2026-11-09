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
package eu.arrowhead.common.service.validation.serviceinstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;

@ExtendWith(MockitoExtension.class)
public class ServiceInstanceIdentifierNormalizerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceInstanceIdentifierNormalizer normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private VersionNormalizer versionNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeNullInput() {
		assertNull(normalizer.normalize(null));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePartial1() {
		when(systemNameNormalizer.normalize("system-name")).thenReturn("SystemName");

		final String result = normalizer.normalize("system-name");

		verify(systemNameNormalizer).normalize(anyString());

		assertEquals("SystemName", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePartial2() {
		when(systemNameNormalizer.normalize("system-name")).thenReturn("SystemName");
		when(serviceDefNameNormalizer.normalize("service_def")).thenReturn("serviceDef");

		final String result = normalizer.normalize("system-name|service_def");

		verify(systemNameNormalizer).normalize(anyString());
		verify(serviceDefNameNormalizer).normalize(anyString());

		assertEquals("SystemName|serviceDef", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOk() {
		when(systemNameNormalizer.normalize("system-name")).thenReturn("SystemName");
		when(serviceDefNameNormalizer.normalize("service_def")).thenReturn("serviceDef");
		when(versionNormalizer.normalize("1.2")).thenReturn("1.2.0");

		final String result = normalizer.normalize("system-name|service_def|1.2");

		verify(systemNameNormalizer).normalize(anyString());
		verify(serviceDefNameNormalizer).normalize(anyString());
		verify(versionNormalizer).normalize(anyString());

		assertEquals("SystemName|serviceDef|1.2.0", result);
	}
}