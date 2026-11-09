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
public class ArrowheadException extends RuntimeException {

	//=================================================================================================
	// members

	private ExceptionType exceptionType = ExceptionType.ARROWHEAD;
	private final String origin;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ArrowheadException(final String msg, final String origin, final Throwable cause) {
		super(msg, cause);
		this.origin = origin;
	}

	//-------------------------------------------------------------------------------------------------
	public ArrowheadException(final String msg, final String origin) {
		super(msg);
		this.origin = origin;
	}

	//-------------------------------------------------------------------------------------------------
	public ArrowheadException(final String msg, final Throwable cause) {
		super(msg, cause);
		this.origin = null;
	}

	//-------------------------------------------------------------------------------------------------
	public ArrowheadException(final String msg) {
		super(msg);
		this.origin = null;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public ExceptionType getExceptionType() {
		return exceptionType;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOrigin() {
		return origin;
	}

	//-------------------------------------------------------------------------------------------------
	protected void setExceptionType(final ExceptionType exceptionType) {
		this.exceptionType = exceptionType;
	}
}