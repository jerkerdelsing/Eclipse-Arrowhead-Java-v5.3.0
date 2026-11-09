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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;

@ExtendWith(MockitoExtension.class)
public class AuthorizationPolicyRequestNormalizerTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationPolicyRequestNormalizer normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeNullInput() {
		assertNull(normalizer.normalize(null));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePolicyTypeAll() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO(
				"ALL",
				null,
				null);

		final NormalizedAuthorizationPolicyRequest result = normalizer.normalize(dto);

		assertEquals(AuthorizationPolicyType.ALL, result.policyType());
		assertNull(result.policyList());
		assertNull(result.policyMetadataRequirement());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePolicyTypeWhitelist() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO(
				"whitelist",
				List.of("TrustedSystem", "trustedSystem"),
				null);

		when(systemNameNormalizer.normalize("TrustedSystem")).thenReturn("TrustedSystem");
		when(systemNameNormalizer.normalize("trustedSystem")).thenReturn("TrustedSystem");

		final NormalizedAuthorizationPolicyRequest result = normalizer.normalize(dto);

		verify(systemNameNormalizer).normalize("TrustedSystem");
		verify(systemNameNormalizer).normalize("trustedSystem");

		assertEquals(AuthorizationPolicyType.WHITELIST, result.policyType());
		assertEquals(1, result.policyList().size());
		assertEquals("TrustedSystem", result.policyList().get(0));
		assertNull(result.policyMetadataRequirement());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePolicyTypeBlacklist() {
		MetadataRequirementDTO reqDTO = new MetadataRequirementDTO();
		reqDTO.put("key", "value");
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO(
				"BLACKLIST",
				List.of("BadSystem"),
				reqDTO);

		when(systemNameNormalizer.normalize("BadSystem")).thenReturn("BadSystem");

		final NormalizedAuthorizationPolicyRequest result = normalizer.normalize(dto);

		verify(systemNameNormalizer).normalize("BadSystem");

		assertEquals(AuthorizationPolicyType.BLACKLIST, result.policyType());
		assertEquals(1, result.policyList().size());
		assertEquals("BadSystem", result.policyList().get(0));
		assertNull(result.policyMetadataRequirement());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePolicyTypeSysMetadata() {
		MetadataRequirementDTO reqDTO = new MetadataRequirementDTO();
		reqDTO.put("key", "value");
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO(
				"sys_metadata",
				List.of("BadSystem"),
				reqDTO);

		final NormalizedAuthorizationPolicyRequest result = normalizer.normalize(dto);

		verify(systemNameNormalizer, never()).normalize(anyString());

		assertEquals(AuthorizationPolicyType.SYS_METADATA, result.policyType());
		assertNull(result.policyList());
		assertEquals(1, result.policyMetadataRequirement().size());
		assertTrue(result.policyMetadataRequirement().containsKey("key"));
		assertEquals("value", result.policyMetadataRequirement().get("key"));
	}
}