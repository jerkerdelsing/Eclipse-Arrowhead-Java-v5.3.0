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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record OrchestrationServiceRequirementDTO(
		String serviceDefinition,
		List<String> operations,
		List<String> versions,
		String alivesAt,
		List<MetadataRequirementDTO> metadataRequirements,
		List<String> interfaceTemplateNames,
		List<String> interfaceAddressTypes,
		List<MetadataRequirementDTO> interfacePropertyRequirements,
		List<String> securityPolicies,
		List<String> preferredProviders) {

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static class Builder {

		//=================================================================================================
		// members

		private String serviceDefinition;
		private List<String> operations;
		private List<String> versions;
		private String alivesAt;
		private List<MetadataRequirementDTO> metadataRequirements;
		private List<String> interfaceTemplateNames;
		private List<String> interfaceAddressTypes;
		private List<MetadataRequirementDTO> interfacePropertyRequirements;
		private List<String> securityPolicies;
		private List<String> preferredProviders;

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public Builder serviceDefinition(final String serviceDefinition) {
			this.serviceDefinition = serviceDefinition;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder operations(final List<String> operations) {
			this.operations = operations;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder operation(final String operation) {
			if (this.operations == null) {
				this.operations = new ArrayList<>();
			}
			this.operations.add(operation);
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
		public Builder metadataRequirements(final List<MetadataRequirementDTO> metadataRequirements) {
			this.metadataRequirements = metadataRequirements;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder metadataRequirement(final MetadataRequirementDTO metadataRequirement) {
			if (this.metadataRequirements == null) {
				this.metadataRequirements = new ArrayList<>();
			}

			this.metadataRequirements.add(metadataRequirement);
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
		public Builder interfaceAddressTypes(final List<String> interfaceAddressTypes) {
			this.interfaceAddressTypes = interfaceAddressTypes;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder interfaceAddressType(final String interfaceAddressType) {
			if (this.interfaceAddressTypes == null) {
				this.interfaceAddressTypes = new ArrayList<>();
			}
			this.interfaceAddressTypes.add(interfaceAddressType);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder interfacePropertyRequirements(final List<MetadataRequirementDTO> interfacePropertyRequirements) {
			this.interfacePropertyRequirements = interfacePropertyRequirements;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder interfacePropertyRequirement(final MetadataRequirementDTO interfacePropertyRequirement) {
			if (this.interfacePropertyRequirements == null) {
				this.interfacePropertyRequirements = new ArrayList<>();
			}

			this.interfacePropertyRequirements.add(interfacePropertyRequirement);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder securityPolicies(final List<String> securityPolicies) {
			this.securityPolicies = securityPolicies;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder securityPolicy(final String securityPolicy) {
			if (this.securityPolicies == null) {
				this.securityPolicies = new ArrayList<>();
			}

			this.securityPolicies.add(securityPolicy);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder preferredProviders(final List<String> preferredProviders) {
			this.preferredProviders = preferredProviders;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder preferredProvider(final String preferredProvider) {
			if (this.preferredProviders == null) {
				this.preferredProviders = new ArrayList<>();
			}

			this.preferredProviders.add(preferredProvider);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public OrchestrationServiceRequirementDTO build() {
			return new OrchestrationServiceRequirementDTO(
					serviceDefinition,
					operations,
					versions,
					alivesAt,
					metadataRequirements,
					interfaceTemplateNames,
					interfaceAddressTypes,
					interfacePropertyRequirements,
					securityPolicies,
					preferredProviders);
		}
	}
}
