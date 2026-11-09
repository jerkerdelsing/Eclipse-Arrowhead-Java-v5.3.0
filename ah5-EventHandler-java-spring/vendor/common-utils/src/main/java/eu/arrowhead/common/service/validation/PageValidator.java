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
package eu.arrowhead.common.service.validation;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.PageDTO;

@Service
public class PageValidator {

	//=================================================================================================
	// members

	@Value(Constants.$MAX_PAGE_SIZE_WD)
	private int maxPageSize;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void validatePageParameter(final PageDTO page, final List<String> availableSortFields, final String origin) {
		logger.debug("validatePageParameter started...");

		if (page != null) {
			if (page.size() != null) {
				if (page.page() == null) {
					throw new InvalidParameterException("If size parameter is defined then page parameter cannot be undefined", origin);
				}

				if (page.size() > maxPageSize) {
					throw new InvalidParameterException("The page size cannot be larger than " + maxPageSize, origin);
				}
			}

			if (page.page() != null && page.size() == null) {
				throw new InvalidParameterException("If page parameter is defined then size parameter cannot be undefined", origin);
			}

			if (!Utilities.isEmpty(page.direction())) {
				try {
					Direction.valueOf(page.direction().trim().toUpperCase());
				} catch (final IllegalArgumentException ex) {
					throw new InvalidParameterException("Direction is invalid. Only ASC or DESC are allowed", origin);
				}
			}

			if (!Utilities.isEmpty(page.sortField()) && !availableSortFields.contains(page.sortField().trim())) {
				throw new InvalidParameterException("Sort field is invalid. Only the following are allowed: " + availableSortFields, origin);
			}
		}
	}
}