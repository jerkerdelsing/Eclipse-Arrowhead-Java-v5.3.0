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
package eu.arrowhead.common.http.filter.authentication;

import java.io.IOException;

import eu.arrowhead.common.exception.ArrowheadException;
import jakarta.servlet.http.HttpServletResponse;

// we test this derived class instead of the original one
public class CertificateFilterTestHelper extends CertificateFilter {

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// this method just re-throws the input exception which is easier to test than intercept the error response somehow
	@Override
	protected void handleException(final ArrowheadException ex, final HttpServletResponse response) throws IOException {
		throw ex;
	}
}