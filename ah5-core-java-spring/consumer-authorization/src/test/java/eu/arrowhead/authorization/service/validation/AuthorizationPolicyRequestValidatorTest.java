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
package eu.arrowhead.authorization.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.dto.AuthorizationPolicyRequestDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AuthorizationPolicyType;

@ExtendWith(MockitoExtension.class)
public class AuthorizationPolicyRequestValidatorTest {

	//=================================================================================================
	// members

	@InjectMocks
	private AuthorizationPolicyRequestValidator validator;

	@Mock
	private SystemNameValidator systemNameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicyNullDefaultPolicy() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAuthorizationPolicy(null, true, "testOrigin"));

		assertEquals("Default policy is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicyNullNotDefaultPolicy() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAuthorizationPolicy(null, false, "testOrigin"));

		assertEquals("Policy is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicyMissingPolicyType() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO(null, null, null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAuthorizationPolicy(dto, false, "testOrigin"));

		assertEquals("Policy type is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicyInvalidPolicyType() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO("invalid", null, null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAuthorizationPolicy(dto, false, "testOrigin"));

		assertEquals("Policy type is invalid: invalid", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicyAllPolicyTypeOk() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.ALL)
				.build();

		assertDoesNotThrow(() -> validator.validateAuthorizationPolicy(dto, false, "testOrigin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicyBlacklistPolicyTypeNullList() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO("BLACKLIST", null, null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAuthorizationPolicy(dto, false, "testOrigin"));

		assertEquals("List is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicyWhitelistPolicyTypeEmptyElement() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.WHITELIST)
				.whitelist(List.of(""))
				.build();

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAuthorizationPolicy(dto, false, "testOrigin"));

		assertEquals("List contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicyBlacklistPolicyTypeOk() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.BLACKLIST)
				.blacklist(List.of("BadSystem"))
				.build();

		assertDoesNotThrow(() -> validator.validateAuthorizationPolicy(dto, false, "testOrigin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicyWhitelistPolicyTypeOk() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.WHITELIST)
				.addWhitelistElement("TrustedSystem")
				.build();

		assertDoesNotThrow(() -> validator.validateAuthorizationPolicy(dto, false, "testOrigin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicySysMetadataPolicyTypeNullElement() {
		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO("SYS_METADATA", null, null);

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAuthorizationPolicy(dto, false, "testOrigin"));

		assertEquals("Metadata requirements is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAuthorizationPolicySysMetadataPolicyTypeOk() {
		final MetadataRequirementDTO requirement = new MetadataRequirementDTO();
		requirement.put("key", "value");

		final AuthorizationPolicyRequestDTO dto = new AuthorizationPolicyRequestDTO.Builder(AuthorizationPolicyType.SYS_METADATA)
				.policyMetadataRequirement(requirement)
				.build();

		assertDoesNotThrow(() -> validator.validateAuthorizationPolicy(dto, false, "testOrigin"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedAuthorizationPolicySimpleOk() {
		assertDoesNotThrow(() -> validator.validateNormalizedAuthorizationPolicy(new NormalizedAuthorizationPolicyRequest(AuthorizationPolicyType.ALL, null, null)));

		assertDoesNotThrow(() -> validator.validateNormalizedAuthorizationPolicy(new NormalizedAuthorizationPolicyRequest(
				AuthorizationPolicyType.SYS_METADATA,
				null,
				new MetadataRequirementDTO())));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedAuthorizationPolicyBlacklistOk() {
		doNothing().when(systemNameValidator).validateSystemName("BadSystem");

		assertDoesNotThrow(() -> validator.validateNormalizedAuthorizationPolicy(new NormalizedAuthorizationPolicyRequest(
				AuthorizationPolicyType.BLACKLIST,
				List.of("BadSystem"),
				null)));

		verify(systemNameValidator).validateSystemName("BadSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNormalizedAuthorizationPolicyWhitelistOk() {
		doNothing().when(systemNameValidator).validateSystemName("TrustedSystem");

		assertDoesNotThrow(() -> validator.validateNormalizedAuthorizationPolicy(new NormalizedAuthorizationPolicyRequest(
				AuthorizationPolicyType.WHITELIST,
				List.of("TrustedSystem"),
				null)));

		verify(systemNameValidator).validateSystemName("TrustedSystem");
	}
}