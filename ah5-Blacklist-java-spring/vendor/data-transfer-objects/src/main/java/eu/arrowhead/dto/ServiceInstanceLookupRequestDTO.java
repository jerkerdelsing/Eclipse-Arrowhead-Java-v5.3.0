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
package eu.arrowhead.dto;

import java.util.ArrayList;
import java.util.List;

public record ServiceInstanceLookupRequestDTO(
		List<String> instanceIds,
		List<String> providerNames,
		List<String> serviceDefinitionNames,
		List<String> versions,
		String alivesAt,
		List<MetadataRequirementDTO> metadataRequirementsList,
		List<String> addressTypes,
		List<String> interfaceTemplateNames,
		List<MetadataRequirementDTO> interfacePropertyRequirementsList,
		List<String> policies) {

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static class Builder {

		//=================================================================================================
		// members

		private List<String> instanceIds;
		private List<String> providerNames;
		private List<String> serviceDefinitionNames;
		private List<String> versions;
		private String alivesAt;
		private List<MetadataRequirementDTO> metadataRequirementsList;
		private List<String> addressTypes;
		private List<String> interfaceTemplateNames;
		private List<MetadataRequirementDTO> interfacePropertyRequirementsList;
		private List<String> policies;

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public Builder instanceIds(final List<String> instanceIds) {
			this.instanceIds = instanceIds;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder instanceId(final String instanceId) {
			if (this.instanceIds == null) {
				this.instanceIds = new ArrayList<>();
			}

			this.instanceIds.add(instanceId);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder providerNames(final List<String> providerNames) {
			this.providerNames = providerNames;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder providerName(final String providerName) {
			if (this.providerNames == null) {
				this.providerNames = new ArrayList<>();
			}

			this.providerNames.add(providerName);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder serviceDefinitionNames(final List<String> serviceDefinitionNames) {
			this.serviceDefinitionNames = serviceDefinitionNames;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder serviceDefinitionName(final String serviceDefinitionName) {
			if (this.serviceDefinitionNames == null) {
				this.serviceDefinitionNames = new ArrayList<>();
			}

			this.serviceDefinitionNames.add(serviceDefinitionName);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder versions(final List<String> versions) {
			this.versions = versions;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder version(final String version) {
			if (this.versions == null) {
				this.versions = new ArrayList<>();
			}

			this.versions.add(version);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder alivesAt(final String alivesAt) {
			this.alivesAt = alivesAt;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder metadataRequirementsList(final List<MetadataRequirementDTO> metadataRequirementsList) {
			this.metadataRequirementsList = metadataRequirementsList;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder metadataRequirements(final MetadataRequirementDTO metadataRequirements) {
			if (this.metadataRequirementsList == null) {
				this.metadataRequirementsList = new ArrayList<>();
			}

			this.metadataRequirementsList.add(metadataRequirements);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder addressTypes(final List<String> addressTypes) {
			this.addressTypes = addressTypes;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder addressType(final String addressType) {
			if (this.addressTypes == null) {
				this.addressTypes = new ArrayList<>();
			}
			this.addressTypes.add(addressType);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder interfaceTemplateNames(final List<String> interfaceTemplateNames) {
			this.interfaceTemplateNames = interfaceTemplateNames;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder interfaceTemplateName(final String interfaceTemplateName) {
			if (this.interfaceTemplateNames == null) {
				this.interfaceTemplateNames = new ArrayList<>();
			}

			this.interfaceTemplateNames.add(interfaceTemplateName);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder interfacePropertyRequirementsList(final List<MetadataRequirementDTO> interfacePropertyRequirementsList) {
			this.interfacePropertyRequirementsList = interfacePropertyRequirementsList;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder interfacePropertyRequirements(final MetadataRequirementDTO interfacePropertyRequirements) {
			if (this.interfacePropertyRequirementsList == null) {
				this.interfacePropertyRequirementsList = new ArrayList<>();
			}

			this.interfacePropertyRequirementsList.add(interfacePropertyRequirements);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder policies(final List<String> policies) {
			this.policies = policies;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder policy(final String policy) {
			if (this.policies == null) {
				this.policies = new ArrayList<>();
			}

			this.policies.add(policy);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public ServiceInstanceLookupRequestDTO build() {
			return new ServiceInstanceLookupRequestDTO(
					instanceIds,
					providerNames,
					serviceDefinitionNames,
					versions,
					alivesAt,
					metadataRequirementsList,
					addressTypes,
					interfaceTemplateNames,
					interfacePropertyRequirementsList,
					policies);
		}
	}
}