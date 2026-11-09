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
package eu.arrowhead.common.intf.properties;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.intf.properties.validators.HttpOperationsValidator;
import eu.arrowhead.common.intf.properties.validators.MinMaxValidator;
import eu.arrowhead.common.intf.properties.validators.NotEmptyAddressListValidator;
import eu.arrowhead.common.intf.properties.validators.NotEmptyStringSetValidator;
import eu.arrowhead.common.intf.properties.validators.PortValidator;
import jakarta.annotation.PostConstruct;

@Service
public class PropertyValidators {

	//=================================================================================================
	// members

	private final Map<PropertyValidatorType, IPropertyValidator> validators = new ConcurrentHashMap<>();

	@Autowired
	private ApplicationContext appContext;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public IPropertyValidator getValidator(final PropertyValidatorType type) {
		return validators.get(type);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		validators.put(PropertyValidatorType.MINMAX, appContext.getBean(MinMaxValidator.class));
		validators.put(PropertyValidatorType.PORT, appContext.getBean(PortValidator.class));
		validators.put(PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST, appContext.getBean(NotEmptyAddressListValidator.class));
		validators.put(PropertyValidatorType.NOT_EMPTY_STRING_SET, appContext.getBean(NotEmptyStringSetValidator.class));
		validators.put(PropertyValidatorType.HTTP_OPERATIONS, appContext.getBean(HttpOperationsValidator.class));
	}
}