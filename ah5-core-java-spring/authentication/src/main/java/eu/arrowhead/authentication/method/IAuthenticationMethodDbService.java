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
package eu.arrowhead.authentication.method;

import java.util.List;

import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.authentication.service.dto.IdentityData;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InternalServerError;

public interface IAuthenticationMethodDbService {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// If the method implementation wants to store something in the System entities' extra field it can use the return value list.
	// The return list must use the same ordering than the input.
	// The return list can be null if extra field is not used
	public List<String> createIdentifiableSystemsInBulk(final List<IdentityData> identities) throws InternalServerError, ExternalServerError;

	//-------------------------------------------------------------------------------------------------
	public default void rollbackCreateIdentifiableSystemsInBulk(final List<IdentityData> identities) {
		// intentionally does nothing
	}

	//-------------------------------------------------------------------------------------------------
	// If the method implementation wants to change something in the System entities' extra field it can use the return value list,
	// but in this case the return list must contain the extra fields of all systems.
	// The return list must use the same ordering than the input.
	// The return list can be null if extra field is not used
	public List<String> updateIdentifiableSystemsInBulk(final List<IdentityData> identities) throws InternalServerError, ExternalServerError;

	//-------------------------------------------------------------------------------------------------
	// This method has to roll back the credentials to the old ones. To do this, the implementation may have to store the old credentials
	// temporarily.
	public default void rollbackUpdateIdentifiableSystemsInBulk(final List<IdentityData> identities) {
		// intentionally does nothing
	}

	//-------------------------------------------------------------------------------------------------
	// In this method the implementation can forget the related temporarily stored old credentials
	public default void commitUpdateIdentifiableSystemsInBulk(final List<IdentityData> identities) {
		// intentionally does nothing
	}

	//-------------------------------------------------------------------------------------------------
	public void removeIdentifiableSystemsInBulk(final List<System> systems) throws InternalServerError, ExternalServerError;

	//-------------------------------------------------------------------------------------------------
	// This method has to roll back the system removals. To do this, the implementation may have to store the system-related data
	// temporarily.
	public default void rollbackRemoveIdentifiableSystemsInBulk(final List<System> systems) {
		// intentionally does nothing
	}

	//-------------------------------------------------------------------------------------------------
	// In this method the implementation can forget the related temporarily stored data
	public default void commitRemoveIdentifiableSystemsInBulk(final List<System> systems) {
		// intentionally does nothing
	}
}