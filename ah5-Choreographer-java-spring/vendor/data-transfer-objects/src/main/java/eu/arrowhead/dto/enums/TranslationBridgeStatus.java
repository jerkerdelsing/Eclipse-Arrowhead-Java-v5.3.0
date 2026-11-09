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

public enum TranslationBridgeStatus {
	NEW, DISCOVERED, PENDING, INITIALIZED, USED, ABORTED, CLOSED, ERROR;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public boolean isActiveStatus() {
		return this == INITIALIZED || this == USED;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isEndStatus() {
		return this == ABORTED || this == CLOSED || this == ERROR;
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isValidTransition(final TranslationBridgeStatus from, final TranslationBridgeStatus to) {
		if (from == null || to == null) {
			return false;
		}

		return switch (from) {
		case NEW -> to == DISCOVERED || to == ABORTED || to == CLOSED || to == ERROR;
		case DISCOVERED -> to == PENDING || to == ABORTED || to == CLOSED || to == ERROR;
		case PENDING -> to == INITIALIZED || to == ABORTED || to == CLOSED || to == ERROR;
		case INITIALIZED, USED -> to == USED || to == ABORTED || to == CLOSED || to == ERROR;
		case ABORTED, CLOSED, ERROR -> false;
		};
	}
}