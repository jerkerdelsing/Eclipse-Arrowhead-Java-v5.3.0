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
package eu.arrowhead.serviceregistry.service.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

public class ServiceLookupFilterModel {

	//=================================================================================================
	// members

	private final Set<String> instanceIds = new HashSet<>();
	private final Set<String> providerNames = new HashSet<>();
	private final Set<String> serviceDefinitionNames = new HashSet<>();
	private final Set<String> versions = new HashSet<>();
	private final ZonedDateTime alivesAt;
	private final List<MetadataRequirementDTO> metadataRequirementsList = new ArrayList<>();
	private final List<AddressType> addressTypes = new ArrayList<>();
	private final Set<String> interfaceTemplateNames = new HashSet<>();
	private final List<MetadataRequirementDTO> interfacePropertyRequirementsList = new ArrayList<>();
	private final Set<ServiceInterfacePolicy> policies = new HashSet<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ServiceLookupFilterModel(final ServiceInstanceLookupRequestDTO dto) {
		Assert.notNull(dto, "ServiceInstanceLookupRequestDTO is null");

		if (!Utilities.isEmpty(dto.instanceIds())) {
			instanceIds.addAll(dto.instanceIds());
		}

		if (!Utilities.isEmpty(dto.providerNames())) {
			providerNames.addAll(dto.providerNames());
		}

		if (!Utilities.isEmpty(dto.serviceDefinitionNames())) {
			serviceDefinitionNames.addAll(dto.serviceDefinitionNames());
		}

		if (!Utilities.isEmpty(dto.versions())) {
			versions.addAll(dto.versions());
		}

		alivesAt = Utilities.parseUTCStringToZonedDateTime(dto.alivesAt());

		if (!Utilities.isEmpty(dto.metadataRequirementsList())) {
			metadataRequirementsList.addAll(dto.metadataRequirementsList());
		}

		if (!Utilities.isEmpty(dto.addressTypes())) {
			for (final String type : dto.addressTypes()) {
				Assert.isTrue(Utilities.isEnumValue(type, AddressType.class), "Invalid address type: " + type);
				addressTypes.add(AddressType.valueOf(type));
			}
		}

		if (!Utilities.isEmpty(dto.interfaceTemplateNames())) {
			interfaceTemplateNames.addAll(dto.interfaceTemplateNames());
		}

		if (!Utilities.isEmpty(dto.interfacePropertyRequirementsList())) {
			interfacePropertyRequirementsList.addAll(dto.interfacePropertyRequirementsList());
		}

		if (!Utilities.isEmpty(dto.policies())) {
			policies.addAll(dto.policies().stream().map(p -> ServiceInterfacePolicy.valueOf(p)).collect(Collectors.toSet()));
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasFilters() {
		return !Utilities.isEmpty(instanceIds)
				|| !Utilities.isEmpty(providerNames)
				|| !Utilities.isEmpty(serviceDefinitionNames)
				|| !Utilities.isEmpty(versions)
				|| alivesAt != null
				|| !Utilities.isEmpty(metadataRequirementsList)
				|| !Utilities.isEmpty(addressTypes)
				|| !Utilities.isEmpty(interfaceTemplateNames)
				|| !Utilities.isEmpty(interfacePropertyRequirementsList)
				|| !Utilities.isEmpty(policies);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public Set<String> getInstanceIds() {
		return instanceIds;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getProviderNames() {
		return providerNames;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getServiceDefinitionNames() {
		return serviceDefinitionNames;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getVersions() {
		return versions;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getAlivesAt() {
		return alivesAt;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getMetadataRequirementsList() {
		return metadataRequirementsList;
	}

	//-------------------------------------------------------------------------------------------------
	public List<AddressType> getAddressTypes() {
		return addressTypes;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getInterfaceTemplateNames() {
		return interfaceTemplateNames;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getInterfacePropertyRequirementsList() {
		return interfacePropertyRequirementsList;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<ServiceInterfacePolicy> getPolicies() {
		return policies;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return Objects.hash(
				addressTypes,
				alivesAt,
				instanceIds,
				interfacePropertyRequirementsList,
				interfaceTemplateNames,
				metadataRequirementsList,
				policies,
				providerNames,
				serviceDefinitionNames,
				versions);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final ServiceLookupFilterModel other = (ServiceLookupFilterModel) obj;
		return Objects.equals(addressTypes, other.addressTypes) && Objects.equals(alivesAt, other.alivesAt) && Objects.equals(instanceIds, other.instanceIds)
				&& Objects.equals(interfacePropertyRequirementsList, other.interfacePropertyRequirementsList) && Objects.equals(interfaceTemplateNames, other.interfaceTemplateNames)
				&& Objects.equals(metadataRequirementsList, other.metadataRequirementsList) && Objects.equals(policies, other.policies) && Objects.equals(providerNames, other.providerNames)
				&& Objects.equals(serviceDefinitionNames, other.serviceDefinitionNames) && Objects.equals(versions, other.versions);
	}
}