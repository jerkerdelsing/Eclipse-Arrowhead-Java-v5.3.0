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
package eu.arrowhead.serviceregistry.api.http.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class ServiceLookupPreprocessorTest {

	//=================================================================================================
	// members

	private ServiceLookupPreprocessor preprocessor = new ServiceLookupPreprocessor();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsRestrictedNullInput() {
		final boolean result = preprocessor.isRestricted(null);

		assertTrue(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsRestrictedNullAttribute() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		final boolean result = preprocessor.isRestricted(request);

		assertTrue(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsRestrictedTrueAttribute() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("restricted.service.lookup", true);
		final boolean result = preprocessor.isRestricted(request);

		assertTrue(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsRestrictedFalseAttribute() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("restricted.service.lookup", false);
		final boolean result = preprocessor.isRestricted(request);

		assertFalse(result);
	}
}