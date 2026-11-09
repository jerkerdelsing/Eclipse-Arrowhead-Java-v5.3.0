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
package eu.arrowhead.translationmanager.service.matchmaking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.dto.ServiceInstanceResponseDTO;

@ExtendWith(MockitoExtension.class)
public class DefaultDataModelTranslatorMatchmakerTest {

	//=================================================================================================
	// members

	private DefaultInterfaceTranslatorMatchmaker matchmaker = new DefaultInterfaceTranslatorMatchmaker();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingCandidatesListNull() {
		final ServiceInstanceResponseDTO selected = matchmaker.doMatchmaking(null, null);

		assertNull(selected);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingCandidatesListEmpty() {
		final ServiceInstanceResponseDTO selected = matchmaker.doMatchmaking(List.of(), null);

		assertNull(selected);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testDoMatchmakingOk() {
		final ServiceInstanceResponseDTO dto1 = new ServiceInstanceResponseDTO("Translator1|iterfaceBridgeManagement|1.0.0", null, null, null, null, null, null, null, null);
		final ServiceInstanceResponseDTO dto2 = new ServiceInstanceResponseDTO("Translator2|iterfaceBridgeManagement|1.0.0", null, null, null, null, null, null, null, null);
		final ServiceInstanceResponseDTO dto3 = new ServiceInstanceResponseDTO("Translator3|iterfaceBridgeManagement|1.0.0", null, null, null, null, null, null, null, null);

		final SecureRandom mock = Mockito.mock(SecureRandom.class);
		ReflectionTestUtils.setField(matchmaker, "rnd", mock);

		when(mock.nextInt(3)).thenReturn(0);

		final ServiceInstanceResponseDTO selected = matchmaker.doMatchmaking(List.of(dto1, dto2, dto3), null);

		assertNotNull(selected);
		assertEquals("Translator1|iterfaceBridgeManagement|1.0.0", selected.instanceId());

		verify(mock).nextInt(3);
	}
}