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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record OrchestrationRequestDTO(
		OrchestrationServiceRequirementDTO serviceRequirement,
		Map<String, Boolean> orchestrationFlags,
		List<QoSRequirementDTO> qualityRequirements,
		Integer exclusivityDuration) {

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static class Builder {

		//=================================================================================================
		// members

		private OrchestrationServiceRequirementDTO serviceRequirement;
		private Map<String, Boolean> orchestrationFlags;
		private List<QoSRequirementDTO> qualityRequirements;
		private Integer exclusivityDuration;

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public Builder serviceRequirement(final OrchestrationServiceRequirementDTO serviceRequirement) {
			this.serviceRequirement = serviceRequirement;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder orchestrationFlags(final Map<String, Boolean> orchestrationFlags) {
			this.orchestrationFlags = orchestrationFlags;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder orchestrationFlag(final String orchestrationFlag, final boolean value) {
			if (this.orchestrationFlags == null) {
				this.orchestrationFlags = new HashMap<>();
			}
			this.orchestrationFlags.put(orchestrationFlag, value);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder qualityRequirements(final List<QoSRequirementDTO> qualityRequirements) {
			this.qualityRequirements = qualityRequirements;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder qualityRequirements(final QoSRequirementDTO qualityRequirements) {
			if (this.qualityRequirements == null) {
				this.qualityRequirements = new ArrayList<>();
			}
			this.qualityRequirements.add(qualityRequirements);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder exclusivityDuration(final Integer exclusivityDuration) {
			this.exclusivityDuration = exclusivityDuration;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public OrchestrationRequestDTO build() {
			return new OrchestrationRequestDTO(serviceRequirement, orchestrationFlags, qualityRequirements, exclusivityDuration);
		}
	}
}
