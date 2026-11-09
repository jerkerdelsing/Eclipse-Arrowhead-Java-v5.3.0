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
package eu.arrowhead.authorization.service.normalization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.EventTypeNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

@ExtendWith(MockitoExtension.class)
public class AuthorizationPolicyInstanceIdentifierNormalizerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationPolicyInstanceIdentifierNormalizer normalizer;

	@Mock
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private EventTypeNameNormalizer eventTypeNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeEmptyInput() {
		assertNull(normalizer.normalize(" "));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeLocalCloudServiceDefOk() {
		when(cloudIdentifierNormalizer.normalize("LOCAL")).thenReturn("LOCAL");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");

		final String result = normalizer.normalize("pr|LOCAL|ProviderName|service_def|testService");

		verify(cloudIdentifierNormalizer).normalize("LOCAL");
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(serviceDefNameNormalizer).normalize("testService");
		verify(eventTypeNameNormalizer, never()).normalize(anyString());

		assertEquals("PR|LOCAL|ProviderName|SERVICE_DEF|testService", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeForeignCloudEventTypeOk() {
		when(cloudIdentifierNormalizer.normalize("TestCloud|Company")).thenReturn("TestCloud|Company");
		when(systemNameNormalizer.normalize("ProviderName")).thenReturn("ProviderName");
		when(eventTypeNameNormalizer.normalize("testEvent")).thenReturn("testEvent");

		final String result = normalizer.normalize("mgmt|TestCloud|Company|ProviderName|event_type|testEvent");

		verify(cloudIdentifierNormalizer).normalize("TestCloud|Company");
		verify(systemNameNormalizer).normalize("ProviderName");
		verify(serviceDefNameNormalizer, never()).normalize(anyString());
		verify(eventTypeNameNormalizer).normalize("testEvent");

		assertEquals("MGMT|TestCloud|Company|ProviderName|EVENT_TYPE|testEvent", result);
	}
}