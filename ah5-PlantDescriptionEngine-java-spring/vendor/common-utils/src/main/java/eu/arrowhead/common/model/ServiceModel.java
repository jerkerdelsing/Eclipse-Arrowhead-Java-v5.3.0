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
package eu.arrowhead.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;

public record ServiceModel(
		String serviceDefinition,
		String version,
		List<InterfaceModel> interfaces,
		Map<String, Object> metadata) {

	//-------------------------------------------------------------------------------------------------
	public ServiceModel {
		Assert.isTrue(!Utilities.isEmpty(serviceDefinition), "Service definition is null or blank");
		Assert.isTrue(!Utilities.isEmpty(version), "Version null or blank");
		Assert.isTrue(!Utilities.isEmpty(interfaces), "Interfaces list is null or empty");
	}

	//=================================================================================================
	// nested class

	//-------------------------------------------------------------------------------------------------
	public static class Builder {

		//=================================================================================================
		// members

		private String serviceDefinition;
		private String version;
		private List<InterfaceModel> interfaces = new ArrayList<>();
		private Map<String, Object> metadata = new HashMap<>();

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public Builder serviceDefinition(final String serviceDefinition) {
			this.serviceDefinition = serviceDefinition;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder version(final String version) {
			this.version = version;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder serviceInterface(final InterfaceModel serviceInterface) {
			if (interfaces == null) {
				interfaces = new ArrayList<>();
			}

			if (serviceInterface != null) {
				interfaces.add(serviceInterface);
			}

			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder serviceInterfaces(final List<InterfaceModel> interfaces) {
			this.interfaces = interfaces;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder metadata(final String key, final Object value) {
			if (metadata == null) {
				metadata = new HashMap<>();
			}

			if (!Utilities.isEmpty(key)) {
				metadata.put(key, value);
			}

			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder metadata(final Map<String, Object> metadata) {
			this.metadata = metadata;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public ServiceModel build() {
			return new ServiceModel(serviceDefinition, version, interfaces, metadata);
		}
	}
}