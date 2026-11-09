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
package eu.arrowhead.common.service.validation.cloud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

@ExtendWith(MockitoExtension.class)
public class CloudIdentifierNormalizerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private CloudIdentifierNormalizer normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeEmptyInput() {
		assertAll("empty input",
				() -> assertNull(normalizer.normalize(null)),
				() -> assertNull(normalizer.normalize("")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeDefaultCloud() {
		assertEquals("LOCAL", normalizer.normalize("local"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeOk() {
		when(systemNameNormalizer.normalize("cloud-name")).thenReturn("CloudName");
		when(systemNameNormalizer.normalize("organization_name")).thenReturn("OrganizationName");

		final String result = normalizer.normalize("cloud-name|organization_name");

		verify(systemNameNormalizer, times(2)).normalize(anyString());

		assertEquals("CloudName|OrganizationName", result);
	}
}