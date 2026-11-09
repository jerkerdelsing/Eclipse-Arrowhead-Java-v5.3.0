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
package eu.arrowhead.translationmanager.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.TranslationReportRequestDTO;
import eu.arrowhead.translationmanager.jpa.service.BridgeDbService;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationReportRequestDTO;
import eu.arrowhead.translationmanager.service.validation.TranslationReportValidation;

@Service
public class TranslationReportService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private TranslationReportValidation validator;

	@Autowired
	private BridgeDbService dbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void reportOperation(final String requester, final TranslationReportRequestDTO dto, final String origin) {
		logger.debug("reportOperation started...");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		final String normalizedRequester = validator.validateAndNormalizeRequester(requester, origin);
		final NormalizedTranslationReportRequestDTO normalized = validator.validateAndNormalizeReport(normalizedRequester, dto, origin);

		try {
			dbService.handleTranslationReport(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final ForbiddenException ex) {
			throw new ForbiddenException(ex.getMessage(), origin);
		} catch (final InternalServerError ex) {
			throw new InternalServerError(ex.getMessage(), origin);
		}
	}
}