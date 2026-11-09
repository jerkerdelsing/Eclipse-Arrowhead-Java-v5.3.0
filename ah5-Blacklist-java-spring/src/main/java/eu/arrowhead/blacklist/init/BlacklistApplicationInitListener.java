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
package eu.arrowhead.blacklist.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import eu.arrowhead.blacklist.service.WhitelistService;
import eu.arrowhead.common.init.ApplicationInitListener;

@Component
public class BlacklistApplicationInitListener extends ApplicationInitListener {

	//=================================================================================================
	// members

	@Autowired
	private WhitelistService whitelistService;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) throws InterruptedException {
		logger.debug("customInit started...");

		// removing systems, that are on the whitelist from the database
		whitelistService.cleanDatabase();
	}
}