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
package eu.arrowhead.authorization.service.model;

import eu.arrowhead.common.Utilities;

public class EncryptionKeyModel {

	//=================================================================================================
	// members

	private String systemName;
	private String keyValue;
	private String keyEncryptedValue;
	private String algorithm;
	private String internalAuxiliary;
	private String externalAuxiliary;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public EncryptionKeyModel() {
	}

	//-------------------------------------------------------------------------------------------------
	public EncryptionKeyModel(final String systemName, final String keyValue, final String keyEncryptedValue, final String algorithm, final String internalAuxiliary, final String externalAuxiliary) {
		this.systemName = systemName;
		this.keyValue = keyValue;
		this.keyEncryptedValue = keyEncryptedValue;
		this.algorithm = algorithm;
		this.internalAuxiliary = internalAuxiliary;
		this.externalAuxiliary = externalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasExternalAuxiliary() {
		return !Utilities.isEmpty(externalAuxiliary);
	}

	//=================================================================================================
	// boilerplate methods

	//-------------------------------------------------------------------------------------------------
	public String getSystemName() {
		return systemName;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystemName(final String systemName) {
		this.systemName = systemName;
	}

	//-------------------------------------------------------------------------------------------------
	public String getKeyValue() {
		return keyValue;
	}

	//-------------------------------------------------------------------------------------------------
	public void setKeyValue(final String keyValue) {
		this.keyValue = keyValue;
	}

	//-------------------------------------------------------------------------------------------------
	public String getKeyEncryptedValue() {
		return keyEncryptedValue;
	}

	//-------------------------------------------------------------------------------------------------
	public void setKeyEncryptedValue(final String keyEncryptedValue) {
		this.keyEncryptedValue = keyEncryptedValue;
	}

	//-------------------------------------------------------------------------------------------------
	public String getAlgorithm() {
		return algorithm;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAlgorithm(final String algorithm) {
		this.algorithm = algorithm;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInternalAuxiliary() {
		return internalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInternalAuxiliary(final String internalAuxiliary) {
		this.internalAuxiliary = internalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public String getExternalAuxiliary() {
		return externalAuxiliary;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExternalAuxiliary(final String externalAuxiliary) {
		this.externalAuxiliary = externalAuxiliary;
	}
}