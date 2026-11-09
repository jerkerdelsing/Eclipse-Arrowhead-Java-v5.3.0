/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.security;

import java.util.Optional;

import eu.arrowhead.datamanager.DataManagerConstants;

public final class DataManagerOperationResolver {

	// segment positions in "/systems/{systemName}/services/{serviceName}/data" after splitting on "/"
	private static final int SYSTEMS_INDEX = 1;
	private static final int SERVICES_INDEX = 3;
	private static final int DATA_INDEX = 5;
	private static final int LIST_SYSTEMS_LENGTH = SYSTEMS_INDEX + 1;
	private static final int LIST_SERVICES_LENGTH = SERVICES_INDEX + 1;
	private static final int DATA_LENGTH = DATA_INDEX + 1;

	public record ServiceOperation(String serviceDefinition, String operation) {
	}

	public static Optional<ServiceOperation> resolve(final String method, final String path) {
		if (method == null || path == null) {
			return Optional.empty();
		}

		final String serviceDefinition;
		final String rest;
		if (path.startsWith(DataManagerConstants.HTTP_API_DATA_PROXY_PATH + "/")) {
			serviceDefinition = DataManagerConstants.SERVICE_DEF_DATA_PROXY;
			rest = path.substring(DataManagerConstants.HTTP_API_DATA_PROXY_PATH.length());
		} else if (path.startsWith(DataManagerConstants.HTTP_API_DATA_HISTORIAN_PATH + "/")) {
			serviceDefinition = DataManagerConstants.SERVICE_DEF_DATA_HISTORIAN;
			rest = path.substring(DataManagerConstants.HTTP_API_DATA_HISTORIAN_PATH.length());
		} else {
			return Optional.empty();
		}

		final String[] segments = rest.replaceAll("/+$", "").split("/");
		// segments[0] is empty because rest starts with "/"
		final boolean get = "GET".equalsIgnoreCase(method);
		final boolean put = "PUT".equalsIgnoreCase(method);
		if (segments.length == LIST_SYSTEMS_LENGTH && "systems".equals(segments[SYSTEMS_INDEX]) && get) {
			return Optional.of(new ServiceOperation(serviceDefinition, DataManagerConstants.SERVICE_OP_LIST_SYSTEMS));
		}
		if (segments.length == LIST_SERVICES_LENGTH && "systems".equals(segments[SYSTEMS_INDEX]) && "services".equals(segments[SERVICES_INDEX]) && get) {
			return Optional.of(new ServiceOperation(serviceDefinition, DataManagerConstants.SERVICE_OP_LIST_SERVICES));
		}
		if (segments.length == DATA_LENGTH && "systems".equals(segments[SYSTEMS_INDEX]) && "services".equals(segments[SERVICES_INDEX])
				&& "data".equals(segments[DATA_INDEX]) && (get || put)) {
			return Optional.of(new ServiceOperation(serviceDefinition, get ? DataManagerConstants.SERVICE_OP_GET_DATA : DataManagerConstants.SERVICE_OP_PUT_DATA));
		}

		return Optional.empty();
	}

	private DataManagerOperationResolver() {
		throw new UnsupportedOperationException();
	}
}
