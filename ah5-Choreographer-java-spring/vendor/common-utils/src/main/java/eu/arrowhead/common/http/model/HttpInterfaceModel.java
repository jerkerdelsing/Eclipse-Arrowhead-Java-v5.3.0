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
package eu.arrowhead.common.http.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.model.InterfaceModel;

public record HttpInterfaceModel(
		String templateName,
		List<String> accessAddresses,
		int accessPort,
		String basePath,
		Map<String, HttpOperationModel> operations) implements InterfaceModel {

	//-------------------------------------------------------------------------------------------------
	public HttpInterfaceModel {
		Assert.isTrue(!Utilities.isEmpty(templateName), "'templateName' is missing or empty");
		Assert.isTrue(!Utilities.isEmpty(accessAddresses), "'accessAddresses' is missing or empty");
		Assert.isTrue(accessPort >= Constants.MIN_PORT && accessPort <= Constants.MAX_PORT, "'accessPort' is invalid");
		Assert.isTrue(!Utilities.isEmpty(basePath), "'basePath' is missing");
		Assert.isTrue(!Utilities.isEmpty(operations), "'operations' is missing or empty");
	}

	//=================================================================================================
	// members

	public static final String PROP_NAME_ACCESS_ADDRESSES = "accessAddresses";
	public static final String PROP_NAME_ACCESS_PORT = "accessPort";
	public static final String PROP_NAME_BASE_PATH = "basePath";
	public static final String PROP_NAME_OPERATIONS = "operations";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String protocol() {
		return templateName.equals(Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME) ? Constants.HTTP : Constants.HTTPS;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public Map<String, Object> properties() {
		return Map.of(
				PROP_NAME_ACCESS_ADDRESSES, accessAddresses,
				PROP_NAME_ACCESS_PORT, accessPort,
				PROP_NAME_BASE_PATH, basePath,
				PROP_NAME_OPERATIONS, operations);
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static class Builder {

		//=================================================================================================
		// members

		private final String templateName;
		private List<String> accessAddresses = new ArrayList<>();
		private int accessPort;
		private String basePath;
		private Map<String, HttpOperationModel> operations = new HashMap<>();

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public Builder(final String templateName) {
			this.templateName = templateName;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder(final String templateName, final String domainName, final int port) {
			this(templateName);
			this.accessAddresses.add(domainName);
			this.accessPort = port;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder accessAddresses(final List<String> accessAddresses) {
			this.accessAddresses = accessAddresses;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder accessAddress(final String address) {
			if (accessAddresses == null) {
				accessAddresses = new ArrayList<>();
			}
			accessAddresses.add(address);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder accessPort(final int accessPort) {
			this.accessPort = accessPort;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder basePath(final String basePath) {
			this.basePath = basePath;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder operations(final Map<String, HttpOperationModel> operations) {
			this.operations = operations;
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public Builder operation(final String operationName, final HttpOperationModel model) {
			if (operations == null) {
				operations = new HashMap<>();
			}
			operations.put(operationName, model);
			return this;
		}

		//-------------------------------------------------------------------------------------------------
		public HttpInterfaceModel build() {
			return new HttpInterfaceModel(templateName, accessAddresses, accessPort, basePath, operations);
		}
	}
}