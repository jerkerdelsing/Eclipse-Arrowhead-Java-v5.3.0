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
package eu.arrowhead.common.http.filter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

public class NoOpArrowheadFilter extends ArrowheadFilter {

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected boolean isActive() {
		return false;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	protected boolean shouldNotFilter(final HttpServletRequest request) throws ServletException {
		return true;
	}
}