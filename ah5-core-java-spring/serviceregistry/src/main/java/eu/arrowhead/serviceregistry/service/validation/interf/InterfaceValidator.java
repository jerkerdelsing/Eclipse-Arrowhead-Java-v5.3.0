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
package eu.arrowhead.serviceregistry.service.validation.interf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.intf.properties.IPropertyValidator;
import eu.arrowhead.common.intf.properties.PropertyValidatorType;
import eu.arrowhead.common.intf.properties.PropertyValidators;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor;
import eu.arrowhead.common.service.util.ServiceInterfaceAddressPropertyProcessor.AddressData;
import eu.arrowhead.common.service.validation.address.AddressNormalizer;
import eu.arrowhead.common.service.validation.address.AddressValidator;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameValidator;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.service.ServiceInterfaceTemplateDbService;

@Component
public class InterfaceValidator {

	//=================================================================================================
	// members

	@Autowired
	private ServiceInterfaceTemplateDbService interfaceTemplateDbService;

	@Autowired
	private InterfaceTemplateNameValidator interfaceTemplateNameValidator;

	@Autowired
	private PropertyValidators interfacePropertyValidator;

	@Autowired
	private ServiceInterfaceAddressPropertyProcessor interfaceAddressPropertyProcessor;

	@Autowired
	private AddressNormalizer addressNormalizer;

	@Autowired
	private AddressValidator addressValidator;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceInterfaceRequestDTO> validateNormalizedInterfaceInstancesWithPropsNormalization(final List<ServiceInstanceInterfaceRequestDTO> interfaces)
			throws InvalidParameterException {
		logger.debug("validateNormalizedInterfaceInstances started...");
		Assert.isTrue(!Utilities.isEmpty(interfaces), "Interface instance list is empty");

		final List<ServiceInstanceInterfaceRequestDTO> normalized = new ArrayList<>(interfaces.size());
		interfaces.forEach(interfaceInstance -> {
			interfaceTemplateNameValidator.validateInterfaceTemplateName(interfaceInstance.templateName());

			final Optional<ServiceInterfaceTemplate> templateOpt = interfaceTemplateDbService.getByName(interfaceInstance.templateName());
			if (templateOpt.isEmpty()) { // no existing template
				if (Utilities.isEmpty(interfaceInstance.protocol())) {
					throw new InvalidParameterException("Interface protocol is missing");
				}

				if (interfaceInstance.protocol().length() > Constants.INTERFACE_TEMPLATE_PROTOCOL_MAX_LENGTH) {
					throw new InvalidParameterException("Interface protocol is too long");
				}

				interfaceInstance.properties().keySet().forEach(propName -> {
					if (propName.length() > Constants.INTERFACE_PROPERTY_NAME_MAX_LENGTH) {
						throw new InvalidParameterException("Interface property name is too long");
					}
				});

				discoverAndNormalizeAndValidateAddressProperty(interfaceInstance.properties());

				normalized.add(interfaceInstance);
			} else {
				if (!Utilities.isEmpty(interfaceInstance.protocol()) && !interfaceInstance.protocol().equals(templateOpt.get().getProtocol())) {
					throw new InvalidParameterException(interfaceInstance.protocol() + " protocol is invalid for " + interfaceInstance.templateName());
				}

				final Map<String, Object> normalizedProperties = new HashMap<>(interfaceInstance.properties());
				boolean addressesDiscovered = false;

				interfaceTemplateDbService.getPropertiesByTemplateName(interfaceInstance.templateName())
						.forEach(templateProp -> {
							final Object instanceProp = interfaceInstance.properties().get(templateProp.getPropertyName());

							if (instanceProp == null && templateProp.isMandatory()) {
								throw new InvalidParameterException(templateProp.getPropertyName() + " interface property is missing for " + templateProp.getServiceInterfaceTemplate().getName());
							}

							if (instanceProp != null && !Utilities.isEmpty(templateProp.getValidator())) {
								final String[] validatorWithArgs = templateProp.getValidator().split(ServiceRegistryConstants.INTERFACE_PROPERTY_VALIDATOR_DELIMITER_REGEXP);
								final PropertyValidatorType propertyValidatorType = PropertyValidatorType.valueOf(validatorWithArgs[0]);

								// discover addresses, if it is not done already and the current validator won't do that
								if (!addressesDiscovered && propertyValidatorType != PropertyValidatorType.NOT_EMPTY_ADDRESS_LIST) {
									discoverAndNormalizeAndValidateAddressProperty(interfaceInstance.properties());
								}

								final IPropertyValidator validator = interfacePropertyValidator.getValidator(propertyValidatorType);
								if (validator != null) {
									final Object normalizedProp = validator.validateAndNormalize(
											instanceProp,
											validatorWithArgs.length <= 1 ? new String[0] : Arrays.copyOfRange(validatorWithArgs, 1, validatorWithArgs.length));
									normalizedProperties.put(templateProp.getPropertyName(), normalizedProp);
								} else {
									logger.info("The validator belonging to the interface template property is not implemented: " + propertyValidatorType.name());
								}
							}
						});
				normalized.add(new ServiceInstanceInterfaceRequestDTO(interfaceInstance.templateName(), interfaceInstance.protocol(), interfaceInstance.policy(), normalizedProperties));
			}
		});

		return normalized;
	}

	//-------------------------------------------------------------------------------------------------
	public void validateNormalizedInterfaceTemplates(final List<ServiceInterfaceTemplateRequestDTO> templates) throws InvalidParameterException {
		logger.debug("validateNormalizedInterfaceTemplates started...");
		Assert.isTrue(!Utilities.isEmpty(templates), "Interface template list is empty");

		templates.forEach(template -> {
			interfaceTemplateNameValidator.validateInterfaceTemplateName(template.name());

			if (template.protocol().length() > Constants.INTERFACE_TEMPLATE_PROTOCOL_MAX_LENGTH) {
				throw new InvalidParameterException("Interface protocol is too long");
			}

			template.propertyRequirements().forEach(prop -> {
				if (prop.name().length() > Constants.INTERFACE_PROPERTY_NAME_MAX_LENGTH) {
					throw new InvalidParameterException("Interface property name is too long");
				}

				if (!Utilities.isEmpty(prop.validator()) && !Utilities.isEnumValue(prop.validator(), PropertyValidatorType.class)) {
					throw new InvalidParameterException("Unknown property validator: " + prop.validator());
				}
			});
		});
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private void discoverAndNormalizeAndValidateAddressProperty(final Map<String, Object> props) {
		logger.debug("discoverAndNormalizeAndValidateAddressProperty started..");

		final AddressData addressData = interfaceAddressPropertyProcessor.findAddresses(props);

		if (Utilities.isEmpty(addressData.addresses())) {
			return;
		}

		final List<String> normalizedAndValidAddresses = new ArrayList<>();
		addressData.addresses().forEach(address -> {
			final String normalized = addressNormalizer.normalize(address);
			final AddressType type = addressValidator.detectType(normalized);
			addressValidator.validateNormalizedAddress(type, normalized);
			normalizedAndValidAddresses.add(normalized);
		});

		if (addressData.isList()) {
			props.put(addressData.addressKey(), normalizedAndValidAddresses);
		} else {
			props.put(addressData.addressKey(), normalizedAndValidAddresses.getFirst());
		}
	}
}