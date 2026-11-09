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
package eu.arrowhead.authorization.quartz;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.SelfContainedToken;
import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.service.SelfContainedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TimeLimitedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TokenHeaderDbService;
import eu.arrowhead.authorization.jpa.service.UsageLimitedTokenDbService;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@Component
@DisallowConcurrentExecution
public class CleanerJob implements Job {

	//=================================================================================================
	// members

	@Autowired
	private AuthorizationSystemInfo sysInfo;

	@Autowired
	private TokenHeaderDbService tokenHeaderDbService;

	@Autowired
	private UsageLimitedTokenDbService usageLimitedTokenDbService;

	@Autowired
	private TimeLimitedTokenDbService timeLimitedTokenDbService;

	@Autowired
	private SelfContainedTokenDbService selfContainedTokenDbService;

	private static final int PAGE_SIZE = 100;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public void execute(final JobExecutionContext context) throws JobExecutionException {
		logger.debug("execute started...");

		try {
			final ZonedDateTime now = Utilities.utcNow();
			final Set<Long> toDelete = new HashSet<>();
			int currentPage = -1;
			boolean hasMore = false;
			do {
				currentPage++;
				final Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE, Sort.by(Direction.ASC, TokenHeader.DEFAULT_SORT_FIELD));
				final Page<TokenHeader> pageOfHeaders = tokenHeaderDbService.query(pageable, null, null, null, null, null, null, null);

				pageOfHeaders.forEach((header) -> {
					if (header.getTokenType() == AuthorizationTokenType.USAGE_LIMITED_TOKEN) {
						final Optional<UsageLimitedToken> optional = usageLimitedTokenDbService.getByHeader(header);
						if (optional.isEmpty()
								|| optional.get().getUsageLeft() <= 0
								|| header.getCreatedAt().plusMinutes(sysInfo.getTokenMaxAge()).isBefore(now)) {
							toDelete.add(header.getId());
						}
					} else if (header.getTokenType() == AuthorizationTokenType.TIME_LIMITED_TOKEN) {
						final Optional<TimeLimitedToken> optional = timeLimitedTokenDbService.getByHeader(header);
						if (optional.isEmpty()
								|| optional.get().getExpiresAt().isBefore(now)
								|| header.getCreatedAt().plusMinutes(sysInfo.getTokenMaxAge()).isBefore(now)) {
							toDelete.add(header.getId());
						}
					} else if (header.getTokenType() == AuthorizationTokenType.SELF_CONTAINED_TOKEN) {
						final Optional<SelfContainedToken> optional = selfContainedTokenDbService.getByHeader(header);
						if (optional.isEmpty()
								|| optional.get().getExpiresAt().isBefore(now)
								|| header.getCreatedAt().plusMinutes(sysInfo.getTokenMaxAge()).isBefore(now)) {
							toDelete.add(header.getId());
						}
					} else {
						logger.error("Unhandled token type in cleaner job: " + header.getTokenType().name());
					}
				});

				hasMore = currentPage < pageOfHeaders.getTotalPages() - 1;
			} while (hasMore);

			if (!toDelete.isEmpty()) {
				tokenHeaderDbService.deleteById(toDelete);
			}
		} catch (final Exception ex) {
			logger.debug(ex);
			logger.error("Cleaner job error: " + ex.getMessage());
		}
	}
}