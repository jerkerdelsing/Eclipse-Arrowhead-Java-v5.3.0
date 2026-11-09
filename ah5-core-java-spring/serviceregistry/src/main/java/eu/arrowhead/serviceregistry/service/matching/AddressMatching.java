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
package eu.arrowhead.serviceregistry.service.matching;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.dto.AddressDTO;

@Component
public class AddressMatching {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// checks if the two list contain exactly the same addresses (the order does not matter)
	public boolean isAddressListMatching(final List<AddressDTO> addresses1, final List<AddressDTO> addresses2) {
		Assert.notNull(addresses1, "first parameter is null");
		Assert.notNull(addresses2, "second parameter is null");

		if (addresses1.size() != addresses2.size()) {
			return false;
		}

		List<AddressDTO> temp1 = new ArrayList<>(addresses1);

		int temp1Size = temp1.size();
		while (temp1Size > 0) {
			for (final AddressDTO address2 : addresses2) {
				temp1.remove(address2);

				// if the size did not change, there was no match
				if (temp1.size() == temp1Size) {
					return false;
				}
				temp1Size = temp1.size();
			}
		}

		return true;
	}
}