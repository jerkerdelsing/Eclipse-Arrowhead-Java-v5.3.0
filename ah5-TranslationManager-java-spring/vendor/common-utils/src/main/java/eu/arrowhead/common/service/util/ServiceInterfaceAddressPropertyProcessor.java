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
package eu.arrowhead.common.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.address.AddressValidator;

@Service
public class ServiceInterfaceAddressPropertyProcessor {

	//=================================================================================================
	// members

	@Autowired
	private AddressValidator addressValidator;

	private List<String> addressAliasNames = new ArrayList<>();

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public void setAddressAliasNames(final List<String> addressAliasNames) {
		this.addressAliasNames = addressAliasNames;
	}

	//-------------------------------------------------------------------------------------------------
	public AddressData findAddresses(final Map<String, Object> interfaceProps) {
		logger.debug("findAddresses started...");
		return discover(interfaceProps);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean filterOnAddressTypes(final Map<String, Object> interfaceProps, final List<String> addressTypeFilters) {
		logger.debug("filter started...");

		final AddressData addressData = discover(interfaceProps);

		if (Utilities.isEmpty(addressData.addresses())) {
			return false;
		}

		// Filter on address type
		final List<String> matchingAddresses = new ArrayList<>();
		for (final String address : addressData.addresses()) {
			final String detectedType = addressValidator.detectType(address).name();
			if (addressTypeFilters.stream().anyMatch(tf -> tf.equalsIgnoreCase(detectedType))) {
				matchingAddresses.add(address);
			}
		}

		if (Utilities.isEmpty(matchingAddresses)) {
			return false;
		}

		if (addressData.isList()) {
			interfaceProps.put(addressData.addressKey(), matchingAddresses);
		} else {
			interfaceProps.put(addressData.addressKey(), matchingAddresses.getFirst());
		}

		return true;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private AddressData discover(final Map<String, Object> interfaceProps) {
		logger.debug("discover started...");

		final List<String> addresses = new ArrayList<>();
		String addressKey = null;
		boolean isList = false;

		// Look for address element or list
		for (final String key : interfaceProps.keySet()) {
			if (addressKey != null) {
				break;
			}

			for (final String alias : addressAliasNames) {
				if (key.equalsIgnoreCase(alias)) {
					final List<?> listValues = castToList(interfaceProps.get(key));
					if (listValues != null) {
						listValues.forEach(v -> {
							final String stringElement = castToString(v);
							if (stringElement != null) {
								addresses.add(stringElement);
							}
						});

						if (!Utilities.isEmpty(addresses)) {
							addressKey = key;
							isList = true;
							break;
						}

					} else {
						final String stringValue = castToString(interfaceProps.get(key));
						if (!Utilities.isEmpty(stringValue)) {
							addressKey = key;
							isList = false;
							addresses.add(stringValue);
							break;
						}
					}
				}
			}

		}

		return new AddressData(addresses, addressKey, isList);
	}

	//-------------------------------------------------------------------------------------------------
	private List<?> castToList(final Object obj) {
		logger.debug("castToList started...");

		try {
			return (List<?>) obj;
		} catch (final ClassCastException __) {
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private String castToString(final Object obj) {
		logger.debug("castToString started...");

		try {
			return (String) obj;
		} catch (final ClassCastException __) {
			return null;
		}
	}

	//=================================================================================================
	// nested record

	//-------------------------------------------------------------------------------------------------
	public record AddressData(
			List<String> addresses,
			String addressKey,
			boolean isList) {
	}
}