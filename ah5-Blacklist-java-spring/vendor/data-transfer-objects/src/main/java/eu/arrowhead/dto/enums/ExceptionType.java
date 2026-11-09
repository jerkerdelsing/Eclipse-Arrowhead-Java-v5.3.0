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
package eu.arrowhead.dto.enums;

public enum ExceptionType {
	ARROWHEAD(500),
	INVALID_PARAMETER(400),
	AUTH(401),
	FORBIDDEN(403),
	DATA_NOT_FOUND(404),
	TIMEOUT(408),
	LOCKED(423),
	INTERNAL_SERVER_ERROR(500),
	EXTERNAL_SERVER_ERROR(503);

	//=================================================================================================
	// members

	private final int errorCode;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public int getErrorCode() {
		return errorCode;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private ExceptionType(final int errorCode) {
		this.errorCode = errorCode;
	}
}