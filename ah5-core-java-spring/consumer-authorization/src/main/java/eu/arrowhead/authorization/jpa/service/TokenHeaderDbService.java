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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.repository.TokenHeaderRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@Service
public class TokenHeaderDbService {

	//=================================================================================================
	// members

	@Autowired
	private TokenHeaderRepository headerRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> find(final String provider, final String tokenHash) {
		logger.debug("find started");
		Assert.isTrue(!Utilities.isEmpty(provider), "provider is empty");
		Assert.isTrue(!Utilities.isEmpty(tokenHash), "tokenHash is empty");

		try {
			return headerRepo.findByProviderAndTokenHash(provider, tokenHash);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Optional<TokenHeader> find(final String tokenHash) {
		logger.debug("find started");
		Assert.isTrue(!Utilities.isEmpty(tokenHash), "tokenHash is empty");

		try {
			return headerRepo.findByTokenHash(tokenHash);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<TokenHeader> findByTokenHashList(final List<String> tokenHashes) {
		logger.debug("findByTokenHashList started");
		Assert.isTrue(!Utilities.isEmpty(tokenHashes), "token hash list is empty");
		Assert.isTrue(!Utilities.containsNullOrEmpty(tokenHashes), "token hash list contains null or empty element");

		try {
			return headerRepo.findAllByTokenHashIn(tokenHashes);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteById(final Collection<Long> ids) {
		logger.debug("deleteById started");
		Assert.isTrue(!Utilities.isEmpty(ids), "ID collection is empty");
		Assert.isTrue(!Utilities.containsNull(ids), "ID collection contains null element");

		try {
			headerRepo.deleteAllByIdInBatch(ids);
			headerRepo.flush();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	public Page<TokenHeader> query(
			final Pageable pagination,
			final String requester,
			final AuthorizationTokenType tokenType,
			final String consumerCloud,
			final String consumer,
			final String provider,
			final String target,
			final AuthorizationTargetType targetType) {
		logger.debug("query started");
		Assert.notNull(pagination, "pagination is null");

		try {
			BaseFilter baseFilter = BaseFilter.NONE;
			List<TokenHeader> baseList = null;

			if (!Utilities.isEmpty(requester)) {
				baseList = headerRepo.findAllByRequester(requester);
				baseFilter = BaseFilter.REQUESTER;
			} else if (!Utilities.isEmpty(provider)) {
				baseList = headerRepo.findAllByProvider(provider);
				baseFilter = BaseFilter.PROVIDER;
			} else if (!Utilities.isEmpty(target)) {
				baseList = headerRepo.findAllByTarget(target);
				baseFilter = BaseFilter.TARGET;
			} else if (!Utilities.isEmpty(consumerCloud)) {
				baseList = headerRepo.findAllByConsumerCloud(consumerCloud);
				baseFilter = BaseFilter.CLOUD;
			} else if (!Utilities.isEmpty(consumer)) {
				baseList = headerRepo.findAllByConsumer(consumer);
				baseFilter = BaseFilter.CONSUMER;
			} else if (tokenType != null) {
				baseList = headerRepo.findAllByTokenType(tokenType);
				baseFilter = BaseFilter.TOKEN_TYPE;
			} else if (targetType != null) {
				baseList = headerRepo.findAllByTargetType(targetType);
				baseFilter = BaseFilter.TARGET_TYPE;
			} else {
				return headerRepo.findAll(pagination);
			}

			final Set<Long> matchingIds = new HashSet<>();
			for (final TokenHeader header : baseList) {
				// No need to match against requester requirement, because if requester is specified then header is selected using those

				// Match against consumer
				if (baseFilter != BaseFilter.CONSUMER && !Utilities.isEmpty(consumer) && !header.getConsumer().equals(consumer)) {
					continue;
				}

				// Match against provider
				if (baseFilter != BaseFilter.PROVIDER && !Utilities.isEmpty(provider) && !header.getProvider().equals(provider)) {
					continue;
				}

				// Match against target
				if (baseFilter != BaseFilter.TARGET && !Utilities.isEmpty(target) && !header.getTarget().equals(target)) {
					continue;
				}

				// Match against cloud
				if (baseFilter != BaseFilter.CLOUD && !Utilities.isEmpty(consumerCloud) && !header.getConsumerCloud().equals(consumerCloud)) {
					continue;
				}

				// Match against token type
				if (baseFilter != BaseFilter.TOKEN_TYPE && tokenType != null && header.getTokenType() != tokenType) {
					continue;
				}

				// Match against target type
				if (baseFilter != BaseFilter.TARGET_TYPE && targetType != null && header.getTargetType() != targetType) {
					continue;
				}

				matchingIds.add(header.getId());
			}

			return headerRepo.findAllByIdIn(matchingIds, pagination);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	private enum BaseFilter {
		NONE, REQUESTER, TOKEN_TYPE, CLOUD, CONSUMER, PROVIDER, TARGET, TARGET_TYPE
	}
}