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
package eu.arrowhead.blacklist.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.blacklist.BlacklistConstants;
import eu.arrowhead.blacklist.jpa.service.EntryDbService;
import eu.arrowhead.blacklist.service.normalization.Normalization;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import jakarta.annotation.PostConstruct;

@Service
public class WhitelistService {

	//=================================================================================================
	// members

	@Value(BlacklistConstants.$WHITELIST_WD)
	private List<String> whitelist;

	private List<String> normalizedWhitelist = new ArrayList<>();

	@Autowired
	private EntryDbService dbService;

	@Autowired
	private Normalization normalizer;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// Throws exception, if the provided system name list contains element(s) that are on the whitelist
	public void checkWhitelist(final List<String> names, final String origin) {
		logger.debug("checkWhitelist started");
		Assert.isTrue(!Utilities.isEmpty(origin), "origin is empty");

		if (Utilities.isEmpty(names)) {
			return;
		}

		final List<String> normalizedNames = normalizer.normalizeSystemNames(names);
		final List<String> namesOnWhitelist = normalizedNames
				.stream()
				.filter(n -> normalizedWhitelist.contains(n))
				.collect(Collectors.toList());

		if (!namesOnWhitelist.isEmpty()) {
			throw new InvalidParameterException("The following system names cannot be added, because they are on the whitelist: " + String.join(", ", namesOnWhitelist), origin);
		}
	}

	//-------------------------------------------------------------------------------------------------
	// Inactivates entries in the database that refer to whitelisted systems
	public void cleanDatabase() {
		logger.debug("cleanDatabase started");

		if (!Utilities.isEmpty(normalizedWhitelist)) {
			dbService.inactivateNameList(normalizedWhitelist, Constants.SYS_NAME_BLACKLIST);
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		if (!Utilities.isEmpty(whitelist)) {
			normalizedWhitelist = normalizer.normalizeSystemNames(whitelist);
		}
	}
}