/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import eu.arrowhead.choreographer.api.dto.ActionRequestDTO;
import eu.arrowhead.choreographer.api.dto.PlanRequestDTO;
import eu.arrowhead.choreographer.api.dto.StepRequestDTO;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameValidator;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PlanValidatorTest {

	@InjectMocks
	private PlanValidator validator;

	@Mock
	private ServiceDefinitionNameNormalizer normalizer;

	@Mock
	private ServiceDefinitionNameValidator nameValidator;

	@BeforeEach
	public void setUp() {
		when(normalizer.normalize(anyString())).thenAnswer(i -> ((String) i.getArgument(0)).trim());
	}

	private static StepRequestDTO step(final String name, final String... next) {
		final OrchestrationServiceRequirementDTO req = new OrchestrationServiceRequirementDTO("doSomething", null, List.of("1.0.0"), null, null, null, null, null, null, null);
		return new StepRequestDTO(name, null, req, null, null, List.of(next));
	}

	private static PlanRequestDTO plan(final ActionRequestDTO... actions) {
		return new PlanRequestDTO("plan1", actions[0].name(), List.of(actions));
	}

	@Test
	public void validPlanIsNormalized() {
		// a -> b -> c and the redundant a -> c is removed
		final ActionRequestDTO action = new ActionRequestDTO("act1", null, List.of("a"), List.of(step("a", "b", "c"), step("b", "c"), step("c")));
		final PlanRequestDTO result = validator.validateAndNormalize(plan(action), "test");
		final StepRequestDTO a = result.actions().get(0).steps().get(0);
		assertEquals(List.of("b"), a.nextStepNames());
		assertEquals(1, a.quantity());
		assertEquals(Map.of(), a.staticParameters());
	}

	@Test
	public void circularStepsAreRejected() {
		final ActionRequestDTO action = new ActionRequestDTO("act1", null, List.of("a"), List.of(step("a", "b"), step("b", "c"), step("c", "b")));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalize(plan(action), "test"));
		assertTrue(ex.getMessage().startsWith("Circular reference"));
	}

	@Test
	public void unreachableStepIsRejected() {
		final ActionRequestDTO action = new ActionRequestDTO("act1", null, List.of("a"), List.of(step("a"), step("b")));
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalize(plan(action), "test"));
	}

	@Test
	public void firstStepWithPredecessorIsRejected() {
		final ActionRequestDTO action = new ActionRequestDTO("act1", null, List.of("a", "b"), List.of(step("a", "b"), step("b")));
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalize(plan(action), "test"));
	}

	@Test
	public void actionCycleAndUnreachableActionAreRejected() {
		final ActionRequestDTO a1 = new ActionRequestDTO("act1", "act2", List.of("a"), List.of(step("a")));
		final ActionRequestDTO a2 = new ActionRequestDTO("act2", "act1", List.of("a"), List.of(step("a")));
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalize(plan(a1, a2), "test"));
		final ActionRequestDTO lone = new ActionRequestDTO("act3", null, List.of("a"), List.of(step("a")));
		final ActionRequestDTO first = new ActionRequestDTO("act1", null, List.of("a"), List.of(step("a")));
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalize(plan(first, lone), "test"));
	}

	@Test
	public void invalidNamesAndQuantityAreRejected() {
		final ActionRequestDTO badName = new ActionRequestDTO("act 1", null, List.of("a"), List.of(step("a")));
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalize(plan(badName), "test"));
		final OrchestrationServiceRequirementDTO req = new OrchestrationServiceRequirementDTO("doSomething", null, null, null, null, null, null, null, null, null);
		final ActionRequestDTO zero = new ActionRequestDTO("act1", null, List.of("a"), List.of(new StepRequestDTO("a", 0, req, null, null, List.of())));
		assertThrows(InvalidParameterException.class, () -> validator.validateAndNormalize(plan(zero), "test"));
	}
}
