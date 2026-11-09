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
package eu.arrowhead.serviceorchestration.jpa.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.QosEvalResult;
import eu.arrowhead.serviceorchestration.jpa.repository.QosEvalResultRepository;

@Service
public class QosEvalResultDbService {

	//=================================================================================================
	// members

	@Autowired
	private QosEvalResultRepository qosEvalResultRepo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void save(final OrchestrationJob orchestrationJob, final String evaluationType, final String operation, final String result) {
		logger.debug("save started");
		Assert.notNull(orchestrationJob, "orchestrationJob is null");
		Assert.isTrue(!Utilities.isEmpty(evaluationType), "evaluationType is empty");
		Assert.isTrue(!Utilities.isEmpty(operation), "operation is empty");
		Assert.isTrue(!Utilities.isEmpty(result), "result is empty");

		try {
			qosEvalResultRepo.saveAndFlush(new QosEvalResult(orchestrationJob, evaluationType, operation, result));

		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}
