/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import eu.arrowhead.choreographer.api.dto.CheckPlanResponseDTO;
import eu.arrowhead.choreographer.api.dto.PlanListResponseDTO;
import eu.arrowhead.choreographer.api.dto.PlanRequestDTO;
import eu.arrowhead.choreographer.api.dto.PlanResponseDTO;
import eu.arrowhead.choreographer.jpa.entity.Plan;
import eu.arrowhead.choreographer.jpa.service.PlanDbService;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.PageDTO;

@Service
public class PlanService {

	@Autowired
	private PlanValidator validator;

	@Autowired
	private PlanDbService planDb;

	@Autowired
	private PlanExecutionChecker checker;

	@Autowired
	private DTOConverter converter;

	@Autowired
	private PageService pageService;

	public PlanResponseDTO create(final PlanRequestDTO request, final String origin) {
		final PlanRequestDTO validated = validator.validateAndNormalize(request, origin);
		try {
			return converter.plan(planDb.createPlan(validated));
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
	}

	public PlanListResponseDTO query(final PageDTO page, final String origin) {
		final Page<Plan> plans = planDb.getPlans(pageService.getPageRequest(page, Plan.SORTABLE_FIELDS_BY, "id", origin));
		return new PlanListResponseDTO(plans.getContent().stream().map(converter::plan).toList(), plans.getTotalElements());
	}

	public PlanResponseDTO get(final long id) {
		return converter.plan(planDb.getPlan(id));
	}

	public void remove(final long id) {
		planDb.removePlan(id);
	}

	public CheckPlanResponseDTO check(final long id, final boolean allowInterCloud) {
		return checker.check(planDb.getPlan(id), 1, allowInterCloud, true);
	}
}
