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
package eu.arrowhead.authorization.service.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.EncryptionKey;
import eu.arrowhead.authorization.service.model.TokenModel;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyListResponseDTO;
import eu.arrowhead.dto.AuthorizationMgmtEncryptionKeyResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyDTO;
import eu.arrowhead.dto.AuthorizationPolicyListResponseDTO;
import eu.arrowhead.dto.AuthorizationPolicyResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenResponseDTO;
import eu.arrowhead.dto.AuthorizationTokenVerifyResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyListResponseDTO;
import eu.arrowhead.dto.AuthorizationVerifyResponseDTO;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;

@Service
public class DTOConverter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO convertProviderLevelPolicyPageToResponse(final Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> page) {
		logger.debug("convertProviderLevelPolicyPageToResponse started...");
		Assert.notNull(page, "page is null");

		final List<AuthorizationPolicyResponseDTO> convertedList = page
				.stream()
				.map(e -> convertPolicyToResponse(AuthorizationLevel.PROVIDER, e))
				.toList();

		return new AuthorizationPolicyListResponseDTO(convertedList, page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO convertMgmtLevelPolicyListToResponse(final List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> policies) {
		logger.debug("convertMgmtLevelPolicyListToResponse started...");
		Assert.isTrue(!Utilities.isEmpty(policies), "policies is missing");
		Assert.isTrue(!Utilities.containsNull(policies), "policies list contains null element");

		final List<AuthorizationPolicyResponseDTO> convertedList = policies
				.stream()
				.map(e -> convertPolicyToResponse(AuthorizationLevel.MGMT, e))
				.toList();

		return new AuthorizationPolicyListResponseDTO(convertedList, policies.size());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyListResponseDTO convertMgmtLevelPolicyPageToResponse(final Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> page) {
		logger.debug("convertMgmtLevelPolicyPageToResponse started...");
		Assert.notNull(page, "page is null");

		final List<AuthorizationPolicyResponseDTO> convertedList = page
				.stream()
				.map(e -> convertPolicyToResponse(AuthorizationLevel.MGMT, e))
				.toList();

		return new AuthorizationPolicyListResponseDTO(convertedList, page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyResponseDTO convertPolicyToResponse(final AuthorizationLevel level, final Pair<? extends AuthPolicyHeader, List<AuthPolicy>> entities) {
		logger.debug("convertPolicyToResponse started...");
		Assert.notNull(level, "level is null");
		Assert.notNull(entities, "entities is null");
		Assert.isTrue(!Utilities.isEmpty(entities.getSecond()), "list is missing");
		Assert.isTrue(!Utilities.containsNull(entities.getSecond()), "list contains null element");

		final AuthPolicyHeader header = entities.getFirst();
		final List<AuthPolicy> policies = entities.getSecond();

		AuthorizationPolicyDTO defaultPolicy = null;
		final Map<String, AuthorizationPolicyDTO> scopedPolicies = policies.size() > 1 ? new HashMap<>(policies.size() - 1) : null;
		for (final AuthPolicy policy : policies) {
			if (Defaults.DEFAULT_AUTHORIZATION_SCOPE.equals(policy.getScope())) {
				// this is the default policy
				defaultPolicy = convertAuthPolicyToDTO(policy);
			} else {
				scopedPolicies.put(policy.getScope(), convertAuthPolicyToDTO(policy));
			}
		}

		final String createdBy = level == AuthorizationLevel.PROVIDER ? header.getProvider() : ((AuthMgmtPolicyHeader) header).getCreatedBy();

		return new AuthorizationPolicyResponseDTO(
				header.getInstanceId(),
				level,
				header.getCloud(),
				header.getProvider(),
				header.getTargetType(),
				header.getTarget(),
				header.getDescription(),
				defaultPolicy,
				scopedPolicies,
				createdBy,
				Utilities.convertZonedDateTimeToUTCString(header.getCreatedAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationPolicyDTO convertAuthPolicyToDTO(final AuthPolicy policy) {
		logger.debug("convertAuthPolicyToDTO started...");
		Assert.notNull(policy, "policy is null");
		Assert.notNull(policy.getPolicyType(), "policy type is null");

		List<String> list = null;
		MetadataRequirementDTO metadataRequirement = null;

		switch (policy.getPolicyType()) {
		case ALL:
			// intentionally does nothing
			break;
		case BLACKLIST:
		case WHITELIST:
			list = Stream
					.of(policy.getPolicy().split(AuthPolicy.LIST_POLICY_DELIMITER))
					.map(String::trim)
					.toList();
			break;
		case SYS_METADATA:
			metadataRequirement = Utilities.fromJson(policy.getPolicy(), MetadataRequirementDTO.class);
			break;
		default:
			throw new InvalidParameterException("Unknown policy type: " + policy.getPolicyType().name());
		}

		return new AuthorizationPolicyDTO(
				policy.getPolicyType(),
				list,
				metadataRequirement);
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationVerifyListResponseDTO convertCheckResultListToResponse(final List<Pair<NormalizedVerifyRequest, Boolean>> result) {
		logger.debug("convertCheckResultListToResponse started...");
		Assert.notNull(result, "result is null");

		final List<AuthorizationVerifyResponseDTO> convertedList = result
				.stream()
				.map(e -> convertCheckResultToResponse(e.getFirst(), e.getSecond()))
				.toList();

		return new AuthorizationVerifyListResponseDTO(convertedList, result.size());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenGenerationResponseDTO convertTokenModelToResponse(final TokenModel model) {
		logger.debug("convertTokenModelToResponse started...");
		Assert.notNull(model, "TokenModel is null");

		return new AuthorizationTokenGenerationResponseDTO(
				model.getTokenType(),
				model.getTargetType(),
				model.isEncrypted() ? model.getEncryptedToken() : model.getRawToken(),
				model.getUsageLimit(),
				model.getExpiresAt() == null ? null : Utilities.convertZonedDateTimeToUTCString(model.getExpiresAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenResponseDTO convertTokenModelToMgmtResponse(final TokenModel model) {
		logger.debug("convertTokenModelToResponse started...");
		Assert.notNull(model, "TokenModel is null");

		return new AuthorizationTokenResponseDTO(
				model.getTokenType(),
				model.getVariant(),
				model.isEncrypted() ? model.getEncryptedToken() : model.getRawToken(),
				model.getHashedToken(),
				model.getRequester(),
				model.getConsumerCloud(),
				model.getConsumer(),
				model.getProvider(),
				model.getTargetType(),
				model.getTarget(),
				model.getScope(),
				Utilities.convertZonedDateTimeToUTCString(model.getCreatedAt()),
				model.getUsageLimit(),
				model.getUsageLeft(),
				model.getExpiresAt() == null ? null : Utilities.convertZonedDateTimeToUTCString(model.getExpiresAt()));
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationTokenVerifyResponseDTO convertTokenVerificationResultToResponse(final Pair<Boolean, Optional<TokenModel>> pair) {
		logger.debug("convertTokenVerificationResultToResponse started...");
		Assert.notNull(pair, "pair is null");

		// input contains whether the token is verified or not as first item, if the token is verified then the second item contains the details
		if (!pair.getFirst()) {
			return new AuthorizationTokenVerifyResponseDTO(pair.getFirst(), null, null, null, null, null);
		}

		final TokenModel model = pair.getSecond().get();

		return new AuthorizationTokenVerifyResponseDTO(
				pair.getFirst(),
				model.getConsumerCloud(),
				model.getConsumer(),
				model.getTargetType(),
				model.getTarget(),
				model.getScope());
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationMgmtEncryptionKeyListResponseDTO convertEncryptionKeyListToResponse(final List<EncryptionKey> keys, final long size) {
		logger.debug("convertEncryptionKeyListToResponse started...");
		Assert.notNull(keys, "EncryptionKey list is null");

		return new AuthorizationMgmtEncryptionKeyListResponseDTO(keys
				.stream()
				.map((item) -> convertEncryptionKeyToResponse(item))
				.toList(), size);
	}

	//-------------------------------------------------------------------------------------------------
	public AuthorizationMgmtEncryptionKeyResponseDTO convertEncryptionKeyToResponse(final EncryptionKey key) {
		logger.debug("convertEncryptionKeyToResponse started...");
		Assert.notNull(key, "EncryptionKey is null");

		return new AuthorizationMgmtEncryptionKeyResponseDTO(
				key.getSystemName(),
				key.getEncryptedKey(),
				key.getAlgorithm(),
				key.getExternalAuxiliary() == null ? null : key.getExternalAuxiliary().getValue(),
				Utilities.convertZonedDateTimeToUTCString(key.getCreatedAt()));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AuthorizationVerifyResponseDTO convertCheckResultToResponse(final NormalizedVerifyRequest input, final boolean output) {
		logger.debug("convertCheckResultToResponse started...");

		return new AuthorizationVerifyResponseDTO(
				input.provider(),
				input.consumer(),
				input.cloud(),
				input.targetType(),
				input.target(),
				input.scope(),
				output);
	}
}