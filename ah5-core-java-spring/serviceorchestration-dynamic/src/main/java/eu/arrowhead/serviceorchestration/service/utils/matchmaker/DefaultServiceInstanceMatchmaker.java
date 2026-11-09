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
package eu.arrowhead.serviceorchestration.service.utils.matchmaker;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

public class DefaultServiceInstanceMatchmaker implements ServiceInstanceMatchmaker {

	//=================================================================================================
	// members

	private static final int fullTimeExclusivityScore = 15;
	private static final int canBeExclusiveScore = 10;
	private static final int isPreferredScore = 5;

	// preferred and full time exclusive		 		30
	// not preferred, but can be full-time exclusive	25
	// preferred and can be exclusive					15
	// not preferred, but can be exclusive				10
	// preferred only									5
	// nothing											0

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public OrchestrationCandidate doMatchmaking(final OrchestrationForm form, final List<OrchestrationCandidate> candidates) {
		logger.debug("doMatchmaking started");
		Assert.isTrue(!Utilities.isEmpty(candidates), "Candidate list is empty");

		if (candidates.size() == 1) {
			return candidates.getFirst();
		}

		final List<Scored> scoredCandidates = new ArrayList<>();
		for (final OrchestrationCandidate candidate : candidates) {
			int score = candidate.isPreferred() ? isPreferredScore : 0;
			if (form.exclusivityIsPreferred()) {
				score += candidate.canBeExclusive() ? canBeExclusiveScore : 0;
				score += candidate.getExclusivityDuration() >= form.getExclusivityDuration() ? fullTimeExclusivityScore : 0;
			}
			scoredCandidates.add(new Scored(candidate, score));
		}

		scoredCandidates.sort(Comparator.comparingInt(Scored::score).reversed());

		final int highestScore = scoredCandidates.getFirst().score();
		List<Scored> bestCandidates = scoredCandidates
				.stream()
				.filter(s -> s.score() == highestScore)
				.collect(Collectors.toList());
		if (bestCandidates.size() == 1) {
			return bestCandidates.getFirst().candidate();
		}

		if (form.exclusivityIsPreferred()
				&& bestCandidates.getFirst().candidate.getExclusivityDuration() < form.getExclusivityDuration()) {
			// all of them can be exclusive, but not full-time
			bestCandidates.sort(Comparator.comparingInt((final Scored s) -> s.candidate().getExclusivityDuration()).reversed());
			final int bestExclusivityDuration = bestCandidates.getFirst().candidate().getExclusivityDuration();
			bestCandidates = bestCandidates
					.stream()
					.filter(s -> s.candidate().getExclusivityDuration() == bestExclusivityDuration)
					.toList();

			if (bestCandidates.size() == 1) {
				return bestCandidates.getFirst().candidate();
			}
		}

		final SecureRandom rng = new SecureRandom();

		return bestCandidates.get(rng.nextInt(bestCandidates.size())).candidate();
	}

	//=================================================================================================
	// nested record

	//-------------------------------------------------------------------------------------------------
	private record Scored(OrchestrationCandidate candidate, int score) {
	}
}