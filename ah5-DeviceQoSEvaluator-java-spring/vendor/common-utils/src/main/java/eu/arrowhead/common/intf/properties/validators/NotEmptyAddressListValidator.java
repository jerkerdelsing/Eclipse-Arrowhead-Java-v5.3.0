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
package eu.arrowhead.common.intf.properties.validators;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.intf.properties.IPropertyValidator;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.dto.enums.AddressType;

@Service
public class NotEmptyAddressListValidator implements IPropertyValidator {

	//=================================================================================================
	// members

	private static final List<AddressType> acceptableAddressTypes = List.of(AddressType.IPV4, AddressType.IPV6, AddressType.HOSTNAME);

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private AddressNormalizer addressNormalizer;

	@Autowired
	private AddressValidator addressValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// propertyValue should be a list of strings with at least one element
	// all element should be a valid network address (IPv4, IPv6 or hostname)
	@Override
	public Object validateAndNormalize(final Object propertyValue, final String... args) throws InvalidParameterException {
		logger.debug("NotEmptyAddressListValidator.validateAndNormalize started...");

		if (propertyValue instanceof final List<?> list) {
			if (list.isEmpty()) {
				throw new InvalidParameterException("Property value should be a non-empty list of strings");
			}

			final List<String> normalized = new ArrayList<>(list.size());

			for (final Object element : list) {
				if (element instanceof final String address) {
					if (Utilities.isEmpty(address)) {
						throw new InvalidParameterException("Property value should be a list of non-blank strings");
					}
					normalized.add(validateAndNormalizeAddress(address.trim()));
				} else {
					throw new InvalidParameterException("Property value should be a list of strings");
				}
			}

			return normalized;
		}

		throw new InvalidParameterException("Property value should be a list of strings");
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private String validateAndNormalizeAddress(final String addressStr) throws InvalidParameterException {
		logger.debug("NotEmptyAddressListValidator.validateAndNormalizeAddress started...");

		final String normalized = addressNormalizer.normalize(addressStr);
		final AddressType addressType = addressValidator.detectType(normalized);

		if (!acceptableAddressTypes.contains(addressType)) {
			throw new InvalidParameterException("Unacceptable address type in property value: " + addressType.name());
		}

		addressValidator.validateNormalizedAddress(addressType, normalized);

		return normalized;
	}
}