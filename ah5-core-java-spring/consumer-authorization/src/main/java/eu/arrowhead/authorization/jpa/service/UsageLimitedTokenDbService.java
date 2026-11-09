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
package eu.arrowhead.authorization.jpa.service;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.repository.TokenHeaderRepository;
import eu.arrowhead.authorization.jpa.repository.UsageLimitedTokenRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@Service
public class UsageLimitedTokenDbService {

	//=================================================================================================
	// members

	@Autowired
	private UsageLimitedTokenRepository tokenRepo;

	@Autowired
	private TokenHeaderRepository tokenHeaderRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<UsageLimitedToken, Boolean> save(
			final AuthorizationTokenType tokenType,
			final String tokenHash,
			final String requester,
			final String consumerCloud,
			final String consumer,
			final String provider,
			final AuthorizationTargetType targetType,
			final String target,
			final String scope,
			final int usageLimit) {
		logger.debug("save started...");
		Assert.notNull(tokenType, "tokenType is null");
		Assert.isTrue(!Utilities.isEmpty(tokenHash), "tokenHash is empty");
		Assert.isTrue(!Utilities.isEmpty(requester), "requester is empty");
		Assert.isTrue(!Utilities.isEmpty(consumerCloud), "consumerCloud is empty");
		Assert.isTrue(!Utilities.isEmpty(consumer), "consumer is empty");
		Assert.isTrue(!Utilities.isEmpty(provider), "provider is empty");
		Assert.notNull(targetType, "targetType is null");
		Assert.isTrue(!Utilities.isEmpty(target), "target is empty");

		try {
			boolean override = false;
			final String actScope = Utilities.isEmpty(scope) ? null : scope;
			final Optional<TokenHeader> tokenHeaderOpt = tokenHeaderRepo.findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope(
					consumerCloud,
					consumer,
					provider,
					targetType,
					target,
					actScope);
			if (tokenHeaderOpt.isPresent()) {
				final Optional<UsageLimitedToken> tokenOpt = tokenRepo.findByHeader(tokenHeaderOpt.get());
				if (tokenOpt.isPresent()) {
					tokenRepo.delete(tokenOpt.get());
					override = true;
				}
				tokenHeaderRepo.delete(tokenHeaderOpt.get());
			}

			final TokenHeader tokenHeaderRecord = tokenHeaderRepo.saveAndFlush(new TokenHeader(tokenType, tokenHash, requester, consumerCloud, consumer, provider, targetType, target, actScope));
			final UsageLimitedToken tokenRecord = tokenRepo.saveAndFlush(new UsageLimitedToken(tokenHeaderRecord, usageLimit));

			return Pair.of(tokenRecord, !override);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<UsageLimitedToken> getByHeader(final TokenHeader header) {
		logger.debug("getByHeader started...");
		Assert.notNull(header, "header is null");

		try {
			return tokenRepo.findByHeader(header);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Optional<Pair<Integer, Integer>> decrease(final TokenHeader header) {
		logger.debug("decrease started...");
		Assert.notNull(header, "header is null");

		try {
			final Optional<UsageLimitedToken> optional = tokenRepo.findByHeader(header);
			if (optional.isEmpty()) {
				return Optional.empty();
			}

			UsageLimitedToken usageLimitedToken = optional.get();
			final int from = usageLimitedToken.getUsageLeft();
			if (from > 0) {
				usageLimitedToken.setUsageLeft(usageLimitedToken.getUsageLeft() - 1);
				usageLimitedToken = tokenRepo.saveAndFlush(usageLimitedToken);
			}

			return Optional.of(Pair.of(from, usageLimitedToken.getUsageLeft()));
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}