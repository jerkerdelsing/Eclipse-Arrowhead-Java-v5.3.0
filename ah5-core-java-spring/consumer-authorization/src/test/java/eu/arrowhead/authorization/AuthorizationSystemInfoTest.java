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
package eu.arrowhead.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

@ExtendWith(MockitoExtension.class)
public class AuthorizationSystemInfoTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationSystemInfo sysInfo;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	//

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasSystemUnboundedTokenGenerationRightNoWhitelist() {
		final boolean result = sysInfo.hasSystemUnboundedTokenGenerationRight("TestConsumer");

		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasSystemUnboundedTokenGenerationRightNoNormalizedWhitelistAndFalse() {
		ReflectionTestUtils.setField(sysInfo, "unboundedTokenGenerationWhitelist", List.of("AdminSystem"));

		when(systemNameNormalizer.normalize("AdminSystem")).thenReturn("AdminSystem");

		final boolean result = sysInfo.hasSystemUnboundedTokenGenerationRight("TestConsumer");

		assertFalse(result);

		verify(systemNameNormalizer).normalize("AdminSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasSystemUnboundedTokenGenerationRightTrue() {
		ReflectionTestUtils.setField(sysInfo, "unboundedTokenGenerationWhitelist", List.of("TestConsumer"));
		ReflectionTestUtils.setField(sysInfo, "normalizedUnboundedTokenGenerationWhitelist", List.of("TestConsumer"));

		final boolean result = sysInfo.hasSystemUnboundedTokenGenerationRight("TestConsumer");

		assertTrue(result);

		verify(systemNameNormalizer, never()).normalize("TestConsumer");
	}
}