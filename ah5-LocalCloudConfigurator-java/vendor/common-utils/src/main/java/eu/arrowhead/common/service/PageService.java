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
package eu.arrowhead.common.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.dto.PageDTO;

@Service
public class PageService {

	//=================================================================================================
	// members

	private static final Direction DEFAULT_DEFAULT_DIRECTION = Direction.ASC;

	@Value(Constants.$MAX_PAGE_SIZE_WD)
	private int maxPageSize;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private PageValidator pageValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public PageDTO normalizePageParameters(final PageDTO page, final Direction defaultDirection, final List<String> availableSortFields, final String defaultSortField, final String origin) {
		logger.debug("normalizePageParameters started...");

		if (page == null) {
			return new PageDTO(0, maxPageSize, defaultDirection.name(), defaultSortField);
		}

		pageValidator.validatePageParameter(page, availableSortFields, origin);

		int normalizedPage = page.page() == null ? -1 : page.page();
		if (normalizedPage < 0) {
			normalizedPage = 0;
		}

		int normalizedSize = page.size() == null ? -1 : page.size();
		if (normalizedSize < 1) {
			normalizedSize = maxPageSize;
		}

		final Direction normalizedDirection = Utilities.isEmpty(page.direction()) ? defaultDirection : Direction.valueOf(page.direction().trim().toUpperCase());
		final String normalizedSortField = Utilities.isEmpty(page.sortField()) ? defaultSortField : page.sortField().trim();

		return new PageDTO(normalizedPage, normalizedSize, normalizedDirection.name(), normalizedSortField);
	}

	//-------------------------------------------------------------------------------------------------
	public PageDTO normalizePageParameters(final PageDTO page, final List<String> availableSortFields, final String defaultSortField, final String origin) {
		return normalizePageParameters(page, DEFAULT_DEFAULT_DIRECTION, availableSortFields, defaultSortField, origin);
	}

	//-------------------------------------------------------------------------------------------------
	public PageRequest getPageRequest(final PageDTO page, final Direction defaultDirection, final List<String> availableSortFields, final String defaultSortField, final String origin) {
		logger.debug("getPageRequest started...");

		final PageDTO normalized = normalizePageParameters(page, defaultDirection, availableSortFields, defaultSortField, origin);
		return PageRequest.of(normalized.page().intValue(), normalized.size().intValue(), Direction.valueOf(normalized.direction()), normalized.sortField());
	}

	//-------------------------------------------------------------------------------------------------
	public PageRequest getPageRequest(final PageDTO page, final List<String> availableSortFields, final String defaultSortField, final String origin) {
		logger.debug("getPageRequest started...");

		final PageDTO normalized = normalizePageParameters(page, availableSortFields, defaultSortField, origin);
		return PageRequest.of(normalized.page().intValue(), normalized.size().intValue(), Direction.valueOf(normalized.direction()), normalized.sortField());
	}
}