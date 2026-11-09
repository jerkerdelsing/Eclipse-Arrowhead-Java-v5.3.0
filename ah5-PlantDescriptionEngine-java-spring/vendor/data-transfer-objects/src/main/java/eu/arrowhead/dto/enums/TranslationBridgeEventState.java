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

public enum TranslationBridgeEventState {
	USED, INTERNAL_CLOSED, INTERNAL_ERROR, EXTERNAL_ERROR;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static TranslationBridgeStatus transformToBridgeStatus(final TranslationBridgeEventState state) {
		return switch (state) {
		case USED -> TranslationBridgeStatus.USED;
		case INTERNAL_CLOSED -> TranslationBridgeStatus.CLOSED;
		case INTERNAL_ERROR, EXTERNAL_ERROR -> TranslationBridgeStatus.ERROR;
		case null, default -> null;
		};
	}
}