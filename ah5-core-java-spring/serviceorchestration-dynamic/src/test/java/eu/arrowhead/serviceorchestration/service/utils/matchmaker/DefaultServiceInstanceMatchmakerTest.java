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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationCandidate;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;

public class DefaultServiceInstanceMatchmakerTest {

	//=================================================================================================
	// members

	private final ServiceInstanceMatchmaker matchmaker = new DefaultServiceInstanceMatchmaker();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingEmptyCandidateList() {
		final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> matchmaker.doMatchmaking(new OrchestrationForm(null, null), List.of()));

		assertEquals("Candidate list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingOnlyOneCandidate() {
		final OrchestrationCandidate candidate = getNothingSpecialCandidate();

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(new OrchestrationForm(null, null), List.of(candidate)));

		assertEquals(candidate, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingNothingSpecial() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, null));
		final OrchestrationCandidate nothingSpec1 = getNothingSpecialCandidate();
		final OrchestrationCandidate nothingSpec2 = getNothingSpecialCandidate();

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(nothingSpec1, nothingSpec2)));

		assertTrue(result.equals(nothingSpec1) || result.equals(nothingSpec2));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingPreferredOnly() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, null));
		final OrchestrationCandidate nothingSpec = getNothingSpecialCandidate();
		final OrchestrationCandidate preferredOnly = getPreferredOnlyCandidate();

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(nothingSpec, preferredOnly)));

		assertEquals(preferredOnly, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingNotPreferredButCanBePartlyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate nothingSpec = getNothingSpecialCandidate();
		final OrchestrationCandidate preferredOnly = getPreferredOnlyCandidate();
		final OrchestrationCandidate notPreferredButCanBePartlyExclusive = getNotPreferredButCanBeExclusiveCandidate(90);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(nothingSpec, preferredOnly, notPreferredButCanBePartlyExclusive)));

		assertEquals(notPreferredButCanBePartlyExclusive, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingPreferredAndCanBePartlyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate nothingSpec = getNothingSpecialCandidate();
		final OrchestrationCandidate preferredOnly = getPreferredOnlyCandidate();
		final OrchestrationCandidate notPreferredButCanBePartlyExclusive = getNotPreferredButCanBeExclusiveCandidate(90);
		final OrchestrationCandidate preferredAndCanBePartlyExclusive = getPreferredAndCanBeExclusiveCandidate(90);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(nothingSpec, preferredOnly, notPreferredButCanBePartlyExclusive, preferredAndCanBePartlyExclusive)));

		assertEquals(preferredAndCanBePartlyExclusive, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingNotPreferredButCanBeFullyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate nothingSpec = getNothingSpecialCandidate();
		final OrchestrationCandidate preferredOnly = getPreferredOnlyCandidate();
		final OrchestrationCandidate notPreferredButCanBePartlyExclusive = getNotPreferredButCanBeExclusiveCandidate(90);
		final OrchestrationCandidate preferredAndCanBePartlyExclusive = getPreferredAndCanBeExclusiveCandidate(90);
		final OrchestrationCandidate notPreferredButCanBeFullyExclusive = getNotPreferredButCanBeExclusiveCandidate(110);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(nothingSpec, preferredOnly, notPreferredButCanBePartlyExclusive,
				preferredAndCanBePartlyExclusive, notPreferredButCanBeFullyExclusive)));

		assertEquals(notPreferredButCanBeFullyExclusive, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingPreferredAndCanBeFullyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate nothingSpec = getNothingSpecialCandidate();
		final OrchestrationCandidate preferredOnly = getPreferredOnlyCandidate();
		final OrchestrationCandidate notPreferredButCanBePartlyExclusive = getNotPreferredButCanBeExclusiveCandidate(90);
		final OrchestrationCandidate preferredAndCanBePartlyExclusive = getPreferredAndCanBeExclusiveCandidate(90);
		final OrchestrationCandidate notPreferredButCanBeFullyExclusive = getNotPreferredButCanBeExclusiveCandidate(110);
		final OrchestrationCandidate preferredAndCanBeFullyExclusive = getPreferredAndCanBeExclusiveCandidate(105);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(nothingSpec, preferredOnly, notPreferredButCanBePartlyExclusive,
				preferredAndCanBePartlyExclusive, notPreferredButCanBeFullyExclusive, preferredAndCanBeFullyExclusive)));

		assertEquals(preferredAndCanBeFullyExclusive, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingFromNotPreferredAndGetBestPartlyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate canBePartlyExclusive90 = getNotPreferredButCanBeExclusiveCandidate(90);
		final OrchestrationCandidate canBePartlyExclusive95 = getNotPreferredButCanBeExclusiveCandidate(95);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(canBePartlyExclusive90, canBePartlyExclusive95)));

		assertEquals(canBePartlyExclusive95, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingFromPreferredAndGetBestPartlyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate canBePartlyExclusive90 = getPreferredAndCanBeExclusiveCandidate(90);
		final OrchestrationCandidate canBePartlyExclusive95 = getPreferredAndCanBeExclusiveCandidate(95);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(canBePartlyExclusive90, canBePartlyExclusive95)));

		assertEquals(canBePartlyExclusive95, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingFromPreferredButLessPartlyExclusiveAndNotPreferredButMorePartlyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate canBePartlyExclusive90Preferred = getPreferredAndCanBeExclusiveCandidate(90);
		final OrchestrationCandidate canBePartlyExclusive95NotPreferred = getNotPreferredButCanBeExclusiveCandidate(95);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(canBePartlyExclusive90Preferred, canBePartlyExclusive95NotPreferred)));

		assertEquals(canBePartlyExclusive90Preferred, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingFromPreferredButPartlyExclusiveAndNotPreferredButFullyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate canBePartlyExclusivePreferred = getPreferredAndCanBeExclusiveCandidate(90);
		final OrchestrationCandidate canBeFullyExclusiveNotPreferred = getNotPreferredButCanBeExclusiveCandidate(110);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(canBePartlyExclusivePreferred, canBeFullyExclusiveNotPreferred)));

		assertEquals(canBeFullyExclusiveNotPreferred, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingTwoSameHigherPartlyExclusiveAndOneLessPartlyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate canBePartlyExclusive90 = getNotPreferredButCanBeExclusiveCandidate(90);
		final OrchestrationCandidate canBePartlyExclusive95_1 = getNotPreferredButCanBeExclusiveCandidate(95);
		final OrchestrationCandidate canBePartlyExclusive95_2 = getNotPreferredButCanBeExclusiveCandidate(95);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(canBePartlyExclusive90, canBePartlyExclusive95_1, canBePartlyExclusive95_2)));

		assertTrue(result.equals(canBePartlyExclusive95_1) || result.equals(canBePartlyExclusive95_2));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoMatchmakingOnlyTwoFullyExclusive() {
		final OrchestrationForm form = new OrchestrationForm("TestRequester", new OrchestrationRequestDTO(null, Map.of(), null, 100));
		final OrchestrationCandidate canBeFullyExclusive102 = getNotPreferredButCanBeExclusiveCandidate(102);
		final OrchestrationCandidate canBeFullyExclusive100 = getNotPreferredButCanBeExclusiveCandidate(100);

		final OrchestrationCandidate result = assertDoesNotThrow(() -> matchmaker.doMatchmaking(form, List.of(canBeFullyExclusive102, canBeFullyExclusive100)));

		assertTrue(result.equals(canBeFullyExclusive102) || result.equals(canBeFullyExclusive100));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private OrchestrationCandidate getNothingSpecialCandidate() {
		return new OrchestrationCandidate(null, false, false);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationCandidate getPreferredOnlyCandidate() {
		return new OrchestrationCandidate(null, false, true);
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationCandidate getNotPreferredButCanBeExclusiveCandidate(final int duration) {
		final OrchestrationCandidate candidate = new OrchestrationCandidate(null, false, false);
		candidate.setExclusivityDuration(duration);
		candidate.setCanBeExclusive(true);
		return candidate;
	}

	//-------------------------------------------------------------------------------------------------
	private OrchestrationCandidate getPreferredAndCanBeExclusiveCandidate(final int duration) {
		final OrchestrationCandidate candidate = new OrchestrationCandidate(null, false, true);
		candidate.setExclusivityDuration(duration);
		candidate.setCanBeExclusive(true);
		return candidate;
	}
}
