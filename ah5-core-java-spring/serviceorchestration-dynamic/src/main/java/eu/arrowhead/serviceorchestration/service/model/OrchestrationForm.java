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
package eu.arrowhead.serviceorchestration.service.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.QoSRequirementDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;

public class OrchestrationForm {

	//=================================================================================================
	// members

	private String requesterSystemName;
	private String targetSystemName; // consumer
	private Map<String, Boolean> orchestrationFlags = new HashMap<>();
	private List<QoSRequirementDTO> qualityRequirements;
	private Integer exclusivityDuration;

	// Service related
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
	public OrchestrationForm(final String requesterSystemName, final OrchestrationRequestDTO dto) {
		this(requesterSystemName, requesterSystemName, dto);
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationForm(final String requesterSystemName, final String targetSystemName, final OrchestrationRequestDTO dto) {
		this.requesterSystemName = requesterSystemName;
		this.targetSystemName = targetSystemName;

		if (dto != null) {
			this.orchestrationFlags.putAll(dto.orchestrationFlags() == null ? Map.of() : dto.orchestrationFlags());
			this.qualityRequirements = dto.qualityRequirements();
			this.exclusivityDuration = dto.exclusivityDuration();

			if (dto.serviceRequirement() != null) {
				this.serviceDefinition = dto.serviceRequirement().serviceDefinition();
				this.operations = dto.serviceRequirement().operations();
				this.versions = dto.serviceRequirement().versions();
				this.alivesAt = dto.serviceRequirement().alivesAt();
				this.metadataRequirements = dto.serviceRequirement().metadataRequirements();
				this.interfaceTemplateNames = dto.serviceRequirement().interfaceTemplateNames();
				this.interfaceAddressTypes = dto.serviceRequirement().interfaceAddressTypes();
				this.interfacePropertyRequirements = dto.serviceRequirement().interfacePropertyRequirements();
				this.securityPolicies = dto.serviceRequirement().securityPolicies();
				this.preferredProviders = dto.serviceRequirement().preferredProviders();
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public boolean getFlag(final OrchestrationFlag flag) {
		for (final String rawFlag : orchestrationFlags.keySet()) {
			if (rawFlag.equalsIgnoreCase(flag.name())) {
				return orchestrationFlags.get(rawFlag);
			}
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean addFlag(final OrchestrationFlag flag, final boolean value) {
		Boolean changed = null;
		for (final String rawFlag : orchestrationFlags.keySet()) {
			if (rawFlag.equalsIgnoreCase(flag.name())) {
				changed = orchestrationFlags.get(rawFlag) != value;
				break;
			}
		}
		orchestrationFlags.put(flag.name(), value);

		changed = changed == null ? true : changed;
		return changed;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean exclusivityIsPreferred() {
		return exclusivityDuration != null;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasPreferredProviders() {
		return !Utilities.isEmpty(preferredProviders);
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasQualityRequirements() {
		return !Utilities.isEmpty(qualityRequirements);
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationRequestDTO extractOrchestrationRequestDTO() {
		final OrchestrationServiceRequirementDTO serviceReq = new OrchestrationServiceRequirementDTO(
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

		return new OrchestrationRequestDTO(serviceReq, orchestrationFlags, qualityRequirements, exclusivityDuration);
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getRequesterSystemName() {
		return requesterSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTargetSystemName() {
		return targetSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Boolean> getOrchestrationFlags() {
		return orchestrationFlags;
	}

	//-------------------------------------------------------------------------------------------------
	public List<QoSRequirementDTO> getQualityRequirements() {
		return qualityRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getOperations() {
		return operations;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getVersions() {
		return versions;
	}

	//-------------------------------------------------------------------------------------------------
	public String getAlivesAt() {
		return alivesAt;
	}

	//-------------------------------------------------------------------------------------------------
	public Integer getExclusivityDuration() {
		return exclusivityDuration;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getMetadataRequirements() {
		return metadataRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getInterfaceTemplateNames() {
		return interfaceTemplateNames;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getInterfaceAddressTypes() {
		return interfaceAddressTypes;
	}

	//-------------------------------------------------------------------------------------------------
	public List<MetadataRequirementDTO> getInterfacePropertyRequirements() {
		return interfacePropertyRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getSecurityPolicies() {
		return securityPolicies;
	}

	//-------------------------------------------------------------------------------------------------
	public List<String> getPreferredProviders() {
		return preferredProviders;
	}

	//-------------------------------------------------------------------------------------------------
	public void setRequesterSystemName(final String requesterSystemName) {
		this.requesterSystemName = requesterSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetSystemName(final String targetSystemName) {
		this.targetSystemName = targetSystemName;
	}

	//-------------------------------------------------------------------------------------------------
	public void setQualityRequirements(final List<QoSRequirementDTO> qualityRequirements) {
		this.qualityRequirements = qualityRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExclusivityDuration(final Integer exclusivityDuration) {
		this.exclusivityDuration = exclusivityDuration;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(final String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOperations(final List<String> operations) {
		this.operations = operations;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOrchestrationFlags(final Map<String, Boolean> orchestrationFlags) {
		this.orchestrationFlags = orchestrationFlags;
	}

	//-------------------------------------------------------------------------------------------------
	public void setVersions(final List<String> versions) {
		this.versions = versions;
	}

	//-------------------------------------------------------------------------------------------------
	public void setAlivesAt(final String alivesAt) {
		this.alivesAt = alivesAt;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMetadataRequirements(final List<MetadataRequirementDTO> metadataRequirements) {
		this.metadataRequirements = metadataRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceTemplateNames(final List<String> interfaceTemplateNames) {
		this.interfaceTemplateNames = interfaceTemplateNames;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceAddressTypes(final List<String> interfaceAddressTypes) {
		this.interfaceAddressTypes = interfaceAddressTypes;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfacePropertyRequirements(final List<MetadataRequirementDTO> interfacePropertyRequirements) {
		this.interfacePropertyRequirements = interfacePropertyRequirements;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSecurityPolicies(final List<String> securityPolicies) {
		this.securityPolicies = securityPolicies;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPreferredProviders(final List<String> preferredProviders) {
		this.preferredProviders = preferredProviders;
	}
}