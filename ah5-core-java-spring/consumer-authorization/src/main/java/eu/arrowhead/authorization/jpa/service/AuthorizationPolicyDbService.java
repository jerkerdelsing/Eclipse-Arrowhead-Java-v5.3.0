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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.authorization.jpa.entity.AuthMgmtPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthPolicy;
import eu.arrowhead.authorization.jpa.entity.AuthPolicyHeader;
import eu.arrowhead.authorization.jpa.entity.AuthProviderPolicyHeader;
import eu.arrowhead.authorization.jpa.repository.AuthMgmtPolicyHeaderRepository;
import eu.arrowhead.authorization.jpa.repository.AuthPolicyRepository;
import eu.arrowhead.authorization.jpa.repository.AuthProviderPolicyHeaderRepository;
import eu.arrowhead.authorization.service.dto.DTOConverter;
import eu.arrowhead.authorization.service.dto.NormalizedAuthorizationPolicyRequest;
import eu.arrowhead.authorization.service.dto.NormalizedGrantRequest;
import eu.arrowhead.authorization.service.dto.NormalizedLookupRequest;
import eu.arrowhead.authorization.service.dto.NormalizedQueryRequest;
import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.utils.InstanceIdUtils;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.AuthorizationPolicyDTO;
import eu.arrowhead.dto.enums.AuthorizationLevel;

@Service
public class AuthorizationPolicyDbService {

	//=================================================================================================
	// members

	@Autowired
	private AuthProviderPolicyHeaderRepository providerHeaderRepository;

	@Autowired
	private AuthMgmtPolicyHeaderRepository mgmtHeaderRepository;

	@Autowired
	private AuthPolicyRepository authPolicyRepository;

	@Autowired
	private DTOConverter dtoConverter;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>, Boolean> createProviderLevelPolicy(final NormalizedGrantRequest request) {
		logger.debug("createProviderLevelPolicy started...");
		Assert.notNull(request, "request is null");

		try {
			// check existing record
			final Optional<AuthProviderPolicyHeader> existing = providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget(
					request.cloud(),
					request.provider(),
					request.targetType(),
					request.target());

			if (existing.isPresent()) {
				final AuthProviderPolicyHeader header = existing.get();
				final List<AuthPolicy> policies = authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, header.getId());
				Pair<AuthProviderPolicyHeader, List<AuthPolicy>> existingPolicy = Pair.of(header, policies);

				// check if every details are the same
				final boolean needDescriptionUpdate = checkExistingProviderLevelPolicyDetails(existingPolicy, request);
				if (needDescriptionUpdate) {
					header.setDescription(request.description());
					existingPolicy = Pair.of(providerHeaderRepository.saveAndFlush(header), policies);
				}

				// return existing entries
				return Pair.of(
						existingPolicy,
						false);
			}

			// new policy

			// create header
			AuthProviderPolicyHeader header = new AuthProviderPolicyHeader(
					InstanceIdUtils.calculateInstanceId(request.level(), request.cloud(), request.provider(), request.targetType(), request.target()),
					request.targetType(),
					request.cloud(),
					request.provider(),
					request.target(),
					request.description());

			header = providerHeaderRepository.saveAndFlush(header);
			final List<AuthPolicy> policies = createAuthPolicies(AuthorizationLevel.PROVIDER, header, request.policies());

			return Pair.of(
					Pair.of(header, policies),
					true);
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean deleteProviderLevelPolicyByInstanceId(final String instanceId) {
		logger.debug("deleteProviderLevelPolicyByInstanceId started");
		Assert.isTrue(!Utilities.isEmpty(instanceId), "instanceId is empty");

		try {
			final Optional<AuthProviderPolicyHeader> headerOpt = providerHeaderRepository.findByInstanceId(instanceId);
			if (headerOpt.isPresent()) {
				final AuthProviderPolicyHeader header = headerOpt.get();

				providerHeaderRepository.delete(header);
				providerHeaderRepository.flush();
				authPolicyRepository.deleteByLevelAndHeaderId(AuthorizationLevel.PROVIDER, header.getId());
				authPolicyRepository.flush();

				return true;
			}

			return false;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> getProviderLevelPoliciesByFilters(final Pageable pagination, final NormalizedLookupRequest request) {
		logger.debug("getProviderLevelPoliciesByFilters started...");
		Assert.notNull(pagination, "pagination is null");

		try {
			final boolean hasFilter = request != null && request.hasFilter();

			return hasFilter ? findProviderLevelPoliciesByFilters(pagination, request) : findAllProviderLevelPolicies(pagination);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> getMgmtLevelPoliciesByFilters(final Pageable pagination, final NormalizedQueryRequest request) {
		logger.debug("getMgmtLevelPoliciesByFilters started...");
		Assert.notNull(pagination, "pagination is null");
		Assert.notNull(request, "request is null");
		Assert.isTrue(request.level() == AuthorizationLevel.MGMT, "Can't use this method for provider level filtering");

		try {
			return request.hasAnyOptionalFilter() ? findMgmtLevelPoliciesByFilters(pagination, request) : findAllMgmtLevelPolicies(pagination);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> findRelevantPolicies(final AuthorizationLevel level, final NormalizedVerifyRequest request) {
		logger.debug("findRelevantPolicies started...");
		Assert.notNull(level, "level is null");
		Assert.notNull(request, "request is null");

		try {
			return AuthorizationLevel.MGMT == level ? findRelevantMgmtPolicies(request) : findRelevantProviderPolicies(request);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> createMgmtLevelPoliciesInBulk(final String requester, final List<NormalizedGrantRequest> requestList) {
		logger.debug("createMgmtLevelPolicies started...");
		Assert.isTrue(!Utilities.isEmpty(requester), "requester is missing");
		Assert.isTrue(!Utilities.isEmpty(requestList), "requestList is missing");
		Assert.isTrue(!Utilities.containsNull(requestList), "requestList contains null element");

		try {
			return requestList
					.stream()
					.map(request -> createMgmtLevelPolicy(requester, request))
					.toList();
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deletePoliciesByInstanceIds(final List<String> instanceIds) {
		logger.debug("deletePoliciesByInstanceIds started...");
		Assert.isTrue(!Utilities.isEmpty(instanceIds), "instanceIds is missing");
		Assert.isTrue(!Utilities.containsNullOrEmpty(instanceIds), "instanceIds contains null or empty element");

		try {
			// delete from MGMT level
			final List<AuthMgmtPolicyHeader> referencedMgmtHeaders = mgmtHeaderRepository.findByInstanceIdIn(instanceIds);
			if (!referencedMgmtHeaders.isEmpty()) {
				final List<Long> ids = referencedMgmtHeaders
						.stream()
						.map(h -> h.getId())
						.toList();

				mgmtHeaderRepository.deleteAll(referencedMgmtHeaders);
				mgmtHeaderRepository.flush();

				authPolicyRepository.deleteByLevelAndHeaderIdIn(AuthorizationLevel.MGMT, ids);
			}

			// delete from Provider level
			final List<AuthProviderPolicyHeader> referencedProviderHeaders = providerHeaderRepository.findByInstanceIdIn(instanceIds);
			if (!referencedProviderHeaders.isEmpty()) {
				final List<Long> ids = referencedProviderHeaders
						.stream()
						.map(h -> h.getId())
						.toList();

				providerHeaderRepository.deleteAll(referencedProviderHeaders);
				providerHeaderRepository.flush();

				authPolicyRepository.deleteByLevelAndHeaderIdIn(AuthorizationLevel.PROVIDER, ids);
			}

			authPolicyRepository.flush();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Pair<AuthMgmtPolicyHeader, List<AuthPolicy>> createMgmtLevelPolicy(final String requester, final NormalizedGrantRequest request) {
		logger.debug("createMgmtLevelPolicy started...");

		// check existing record
		final Optional<AuthMgmtPolicyHeader> existing = mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget(
				request.cloud(),
				request.provider(),
				request.targetType(),
				request.target());

		if (existing.isPresent()) {
			throw new InvalidParameterException("A management level policy is already existing for this cloud / provider / target type / target combination: "
					+ request.cloud() + " / "
					+ request.provider() + " / "
					+ request.targetType().name() + " / "
					+ request.target());
		}

		// create header
		AuthMgmtPolicyHeader header = new AuthMgmtPolicyHeader(
				InstanceIdUtils.calculateInstanceId(request.level(), request.cloud(), request.provider(), request.targetType(), request.target()),
				request.targetType(),
				request.cloud(),
				request.provider(),
				request.target(),
				request.description(),
				requester);

		header = mgmtHeaderRepository.saveAndFlush(header);
		final List<AuthPolicy> policies = createAuthPolicies(AuthorizationLevel.MGMT, header, request.policies());

		return Pair.of(header, policies);
	}

	//-------------------------------------------------------------------------------------------------
	private List<AuthPolicy> createAuthPolicies(final AuthorizationLevel level, final AuthPolicyHeader header, final Map<String, NormalizedAuthorizationPolicyRequest> policies) {
		logger.debug("createAuthPolicies started...");

		final List<AuthPolicy> result = new ArrayList<>(policies.size());
		for (final Entry<String, NormalizedAuthorizationPolicyRequest> policyRequest : policies.entrySet()) {
			final String scope = policyRequest.getKey();
			final NormalizedAuthorizationPolicyRequest policy = policyRequest.getValue();
			result.add(new AuthPolicy(
					level,
					header.getId(),
					scope,
					policy.policyType(),
					stringifyPolicy(policy)));
		}

		return authPolicyRepository.saveAllAndFlush(result);
	}

	//-------------------------------------------------------------------------------------------------
	private String stringifyPolicy(final NormalizedAuthorizationPolicyRequest policy) {
		logger.debug("stringifyPolicy started...");
		String result = null;

		switch (policy.policyType()) {
		case ALL:
			// intentionally does nothing
			break;
		case BLACKLIST:
		case WHITELIST:
			result = String.join(AuthPolicy.LIST_POLICY_DELIMITER, policy.policyList());
			break;
		case SYS_METADATA:
			result = Utilities.toJson(policy.policyMetadataRequirement());
			break;
		default:
			throw new InternalServerError("Unknown policy type: " + policy.policyType().name());
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	private Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> findAllProviderLevelPolicies(final Pageable pagination) {
		logger.debug("findAllProviderLevelPolicies started...");

		final Page<AuthProviderPolicyHeader> headers = providerHeaderRepository.findAll(pagination);
		final List<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> entries = headers
				.stream()
				.map(h -> Pair.of(
						h,
						authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, h.getId())))
				.toList();

		return new PageImpl<>(
				entries,
				pagination,
				headers.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	private Page<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> findProviderLevelPoliciesByFilters(final Pageable pagination, final NormalizedLookupRequest request) {
		logger.debug("findProviderLevelPoliciesByFilters started...");

		BaseFilter baseFilter = BaseFilter.NONE;
		List<AuthProviderPolicyHeader> toFilter = new ArrayList<>();

		if (!Utilities.isEmpty(request.instanceIds())) {
			toFilter = providerHeaderRepository.findByInstanceIdIn(request.instanceIds());
			baseFilter = BaseFilter.INSTANCE_ID;
		} else if (!Utilities.isEmpty(request.targetNames())) {
			toFilter = providerHeaderRepository.findByTargetIn(request.targetNames());
			baseFilter = BaseFilter.TARGET;
		} else if (!Utilities.isEmpty(request.cloudIdentifiers())) {
			toFilter = providerHeaderRepository.findByCloudIn(request.cloudIdentifiers());
			baseFilter = BaseFilter.CLOUD;
		} else if (!Utilities.isEmpty(request.providers())) {
			toFilter = providerHeaderRepository.findByProviderIn(request.providers());
			baseFilter = BaseFilter.PROVIDER;
		} else {
			toFilter = providerHeaderRepository.findAll();
		}

		final Set<Long> matchingIds = new HashSet<>();
		final Map<Long, List<AuthPolicy>> authPolicyMap = new HashMap<>();

		for (final AuthProviderPolicyHeader header : toFilter) {
			// No need to match against instance id requirements, because if instance ids are specified then header is selected using those

			// Match against target requirements
			if (baseFilter != BaseFilter.TARGET && !Utilities.isEmpty(request.targetNames()) && !request.targetNames().contains(header.getTarget())) {
				continue;
			}

			// Match against cloud requirements
			if (baseFilter != BaseFilter.CLOUD && !Utilities.isEmpty(request.cloudIdentifiers()) && !request.cloudIdentifiers().contains(header.getCloud())) {
				continue;
			}

			// Match against provider requirements
			if (baseFilter != BaseFilter.PROVIDER && !Utilities.isEmpty(request.providers()) && !request.providers().contains(header.getProvider())) {
				continue;
			}

			// Match against target type requirement
			if (request.targetType() != null && request.targetType() != header.getTargetType()) {
				continue;
			}

			final List<AuthPolicy> policies = authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, header.getId());
			if (!Utilities.isEmpty(policies)) {
				matchingIds.add(header.getId());
				authPolicyMap.put(header.getId(), policies);
			}
		}

		final Page<AuthProviderPolicyHeader> page = providerHeaderRepository.findAllByIdIn(matchingIds, pagination);
		final List<Pair<AuthProviderPolicyHeader, List<AuthPolicy>>> entries = page
				.stream()
				.map(h -> Pair.of(
						h,
						authPolicyMap.get(h.getId())))
				.toList();

		return new PageImpl<>(
				entries,
				pagination,
				page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	private Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> findAllMgmtLevelPolicies(final Pageable pagination) {
		logger.debug("findAllMgmtLevelPolicies started...");

		final Page<AuthMgmtPolicyHeader> headers = mgmtHeaderRepository.findAll(pagination);
		final List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> entries = headers
				.stream()
				.map(h -> Pair.of(
						h,
						authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.MGMT, h.getId())))
				.toList();

		return new PageImpl<>(
				entries,
				pagination,
				headers.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	private Page<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> findMgmtLevelPoliciesByFilters(final Pageable pagination, final NormalizedQueryRequest request) {
		logger.debug("findMgmtLevelPoliciesByFilters started...");

		BaseFilter baseFilter = BaseFilter.NONE;
		List<AuthMgmtPolicyHeader> toFilter = new ArrayList<>();

		if (!Utilities.isEmpty(request.instanceIds())) {
			toFilter = mgmtHeaderRepository.findByInstanceIdIn(request.instanceIds());
			baseFilter = BaseFilter.INSTANCE_ID;
		} else if (!Utilities.isEmpty(request.providers())) {
			toFilter = mgmtHeaderRepository.findByProviderIn(request.providers());
			baseFilter = BaseFilter.PROVIDER;
		} else if (!Utilities.isEmpty(request.targetNames())) {
			toFilter = mgmtHeaderRepository.findByTargetIn(request.targetNames());
			baseFilter = BaseFilter.TARGET;
		} else if (!Utilities.isEmpty(request.cloudIdentifiers())) {
			toFilter = mgmtHeaderRepository.findByCloudIn(request.cloudIdentifiers());
			baseFilter = BaseFilter.CLOUD;
		} else {
			toFilter = mgmtHeaderRepository.findAll();
		}

		final Set<Long> matchingIds = new HashSet<>();
		final Map<Long, List<AuthPolicy>> authPolicyMap = new HashMap<>();

		for (final AuthMgmtPolicyHeader header : toFilter) {
			// No need to match against instance id requirements, because if instance ids are specified then header is selected using those

			// Match against target requirements
			if (baseFilter != BaseFilter.TARGET && !Utilities.isEmpty(request.targetNames()) && !request.targetNames().contains(header.getTarget())) {
				continue;
			}

			// Match against cloud requirements
			if (baseFilter != BaseFilter.CLOUD && !Utilities.isEmpty(request.cloudIdentifiers()) && !request.cloudIdentifiers().contains(header.getCloud())) {
				continue;
			}

			// Match against provider requirements
			if (baseFilter != BaseFilter.PROVIDER && !Utilities.isEmpty(request.providers()) && !request.providers().contains(header.getProvider())) {
				continue;
			}

			// Match against target type requirement
			if (request.targetType() != null && request.targetType() != header.getTargetType()) {
				continue;
			}

			final List<AuthPolicy> policies = authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.MGMT, header.getId());
			if (!Utilities.isEmpty(policies)) {
				matchingIds.add(header.getId());
				authPolicyMap.put(header.getId(), policies);
			}
		}

		final Page<AuthMgmtPolicyHeader> page = mgmtHeaderRepository.findAllByIdIn(matchingIds, pagination);
		final List<Pair<AuthMgmtPolicyHeader, List<AuthPolicy>>> entries = page
				.stream()
				.map(h -> Pair.of(
						h,
						authPolicyMap.get(h.getId())))
				.toList();

		return new PageImpl<>(
				entries,
				pagination,
				page.getTotalElements());
	}

	//-------------------------------------------------------------------------------------------------
	private Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> findRelevantMgmtPolicies(final NormalizedVerifyRequest request) {
		logger.debug("findRelevantMgmtPolicies started...");

		final Optional<AuthMgmtPolicyHeader> headerOpt = mgmtHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget(
				request.cloud(),
				request.provider(),
				request.targetType(),
				request.target());

		// no relevant policies
		if (headerOpt.isEmpty()) {
			return Optional.empty();
		}

		final AuthMgmtPolicyHeader header = headerOpt.get();

		List<AuthPolicy> policies;
		if (request.scope() == null) {
			// if scope is not specified => both default and all scoped policies must check
			policies = authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.MGMT, header.getId());
		} else {
			// if scope is specified => scope-specific policy (if exists) or default policy (if there is no scope-specific policy) must check
			final List<AuthPolicy> candidates = authPolicyRepository.findByLevelAndHeaderIdAndScopeIn(
					AuthorizationLevel.MGMT,
					header.getId(),
					new HashSet<>(List.of(request.scope(), Defaults.DEFAULT_AUTHORIZATION_SCOPE)));

			if (Utilities.isEmpty(candidates)) {
				// zombie header (should not happen)
				logger.debug("Zombie header detected: {}", header.getInstanceId());
				return Optional.empty();
			}

			final List<AuthPolicy> defaultPolicy = new ArrayList<>(1);
			final List<AuthPolicy> scopedPolicies = new ArrayList<>(1);
			candidates
					.stream()
					.forEach(p -> {
						if (p.getScope().equals(Defaults.DEFAULT_AUTHORIZATION_SCOPE)) {
							defaultPolicy.add(p);
						} else {
							scopedPolicies.add(p);
						}
					});
			policies = scopedPolicies.isEmpty() ? defaultPolicy : scopedPolicies;
		}

		return Optional.of(
				Pair.of(
						header,
						policies));
	}

	//-------------------------------------------------------------------------------------------------
	private Optional<Pair<? extends AuthPolicyHeader, List<AuthPolicy>>> findRelevantProviderPolicies(final NormalizedVerifyRequest request) {
		logger.debug("findRelevantProviderPolicies started...");

		final Optional<AuthProviderPolicyHeader> headerOpt = providerHeaderRepository.findByCloudAndProviderAndTargetTypeAndTarget(
				request.cloud(),
				request.provider(),
				request.targetType(),
				request.target());

		// no relevant policies
		if (headerOpt.isEmpty()) {
			return Optional.empty();
		}

		final AuthProviderPolicyHeader header = headerOpt.get();

		List<AuthPolicy> policies;
		if (request.scope() == null) {
			// if scope is not specified => both default and all scoped policies must check
			policies = authPolicyRepository.findByLevelAndHeaderId(AuthorizationLevel.PROVIDER, header.getId());
		} else {
			// if scope is specified => scope-specific policy (if exists) or default policy (if there is no scope-specific policy) must check
			final List<AuthPolicy> candidates = authPolicyRepository.findByLevelAndHeaderIdAndScopeIn(
					AuthorizationLevel.PROVIDER,
					header.getId(),
					new HashSet<>(List.of(request.scope(), Defaults.DEFAULT_AUTHORIZATION_SCOPE)));

			if (Utilities.isEmpty(candidates)) {
				// zombie header (should not happen)
				logger.debug("Zombie header detected: {}", header.getInstanceId());
				return Optional.empty();
			}

			final List<AuthPolicy> defaultPolicy = new ArrayList<>(1);
			final List<AuthPolicy> scopedPolicies = new ArrayList<>(1);
			candidates
					.stream()
					.forEach(p -> {
						if (p.getScope().equals(Defaults.DEFAULT_AUTHORIZATION_SCOPE)) {
							defaultPolicy.add(p);
						} else {
							scopedPolicies.add(p);
						}
					});
			policies = scopedPolicies.isEmpty() ? defaultPolicy : scopedPolicies;
		}

		return Optional.of(
				Pair.of(
						header,
						policies));
	}

	//-------------------------------------------------------------------------------------------------
	// throws exception, if the two policy doesn't have the same attributes
	private boolean checkExistingProviderLevelPolicyDetails(final Pair<AuthProviderPolicyHeader, List<AuthPolicy>> existingPolicy, final NormalizedGrantRequest request) {
		logger.debug("checkExistingProviderLevelPolicyDetails started...");

		boolean needChange = false;

		// omitting cloud, provider, target type and target check, because we use those to find the existing policy

		// description does not matter in the comparison, but caller method should update it if necessary
		needChange = !existingPolicy.getFirst().getDescription().equals(request.description());

		if (existingPolicy.getSecond().size() != request.policies().size()) {
			throw new InvalidParameterException("A provider level policy is already existing with the specified cloud / provider / target type / target combination, but the policy details"
					+ " are different");
		}

		// policy details
		final Map<String, NormalizedAuthorizationPolicyRequest> policies = request.policies();
		for (final AuthPolicy authPolicy : existingPolicy.getSecond()) {
			final NormalizedAuthorizationPolicyRequest matchingCandidate = policies.get(authPolicy.getScope());

			if (matchingCandidate == null
					|| !policyDetailsEquals(authPolicy, matchingCandidate)) {
				throw new InvalidParameterException("A provider level policy is already existing with the specified cloud / provider / target type / target combination, but the policy details"
						+ " are different");
			}
		}

		return needChange;
	}

	//-------------------------------------------------------------------------------------------------
	private boolean policyDetailsEquals(final AuthPolicy existingEntity, final NormalizedAuthorizationPolicyRequest candidate) {
		logger.debug("policyDetailsEquals started...");

		if (existingEntity.getPolicyType() != candidate.policyType()) {
			return false;
		}

		final AuthorizationPolicyDTO existing = dtoConverter.convertAuthPolicyToDTO(existingEntity);
		switch (existing.policyType()) {
		case ALL:
			return true;
		case BLACKLIST:
		case WHITELIST:
			// order of the lists is not important
			return existing.policyList().size() == candidate.policyList().size() && existing.policyList().containsAll(candidate.policyList());
		case SYS_METADATA:
			return existing.policyMetadataRequirement().equals(candidate.policyMetadataRequirement());
		default:
			return false;
		}
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	private enum BaseFilter {
		NONE, INSTANCE_ID, PROVIDER, CLOUD, TARGET
	}
}