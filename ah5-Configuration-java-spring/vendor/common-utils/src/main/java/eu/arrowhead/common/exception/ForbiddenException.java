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
package eu.arrowhead.common.exception;

import eu.arrowhead.dto.enums.ExceptionType;

@SuppressWarnings("serial")
public class ForbiddenException extends ArrowheadException {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ForbiddenException(final String msg, final String origin, final Throwable cause) {
		super(msg, origin, cause);
		this.setExceptionType(ExceptionType.FORBIDDEN);
	}

	//-------------------------------------------------------------------------------------------------
	public ForbiddenException(final String msg, final String origin) {
		super(msg, origin);
		this.setExceptionType(ExceptionType.FORBIDDEN);
	}

	//-------------------------------------------------------------------------------------------------
	public ForbiddenException(final String msg, final Throwable cause) {
		super(msg, cause);
		this.setExceptionType(ExceptionType.FORBIDDEN);
	}

	//-------------------------------------------------------------------------------------------------
	public ForbiddenException(final String msg) {
		super(msg);
		this.setExceptionType(ExceptionType.FORBIDDEN);
	}
}