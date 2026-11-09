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
package eu.arrowhead.authorization.http.filter.authorization;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Order(Constants.REQUEST_FILTER_ORDER_AUTHORIZATION_MGMT_SERVICE)
public class InternalManagementServiceFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Autowired
	private ServiceOperationNameNormalizer serviceOpNameNormalizer;

	@Autowired
	private AuthorizationPolicyEngine policyEngine;

	private static final String mgmtPath = "/mgmt/";

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		logger.debug("InternalManagementServiceFilter.doFilterInternal started...");

		final String requestTarget = SecurityUtilities.getDecodedUri(request.getRequestURL().toString());
		if (requestTarget.contains(mgmtPath)) {
			final String systemName = (String) request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
			final String normalizedSystemName = systemNameNormalizer.normalize(systemName);
			boolean allowed = false;

			switch (sysInfo.getManagementPolicy()) {
			case SYSOP_ONLY:
				allowed = isSystemOperator(request);
				break;

			case WHITELIST:
				allowed = isSystemOperator(request) || isWhitelisted(normalizedSystemName);
				break;

			case AUTHORIZATION:
				allowed = isSystemOperator(request) || isWhitelisted(normalizedSystemName) || isAuthorized(normalizedSystemName, request.getRequestURI(), request.getMethod());
				break;

			default:
				throw new InternalServerError("Unimplemented management policy: " + sysInfo.getManagementPolicy(), requestTarget);
			}

			if (!allowed) {
				throw new ForbiddenException("Requester has no management permission", requestTarget);
			}
		}

		super.doFilterInternal(request, response, chain);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isSystemOperator(final HttpServletRequest request) {
		logger.debug("InternalManagementServiceFilter.isSystemOperator started...");

		return HttpUtilities.isSysop(request, request.getRequestURI());
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isWhitelisted(final String systemName) {
		logger.debug("InternalManagementServiceFilter.isWhitelisted started...");

		return sysInfo.getManagementWhitelist().contains(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isAuthorized(final String systemName, final String path, final String method) {
		logger.debug("InternalManagementServiceFilter.isAuthorized started...");

		// finding service definition and operation
		final Optional<Pair<String, String>> match = findServiceDefinitionAndOperation(path, method);
		if (match.isEmpty()) { // can't identify the service definition and operation
			logger.warn("Can't identify service definition and operation for path: {}", path);
			return false;
		}

		final NormalizedVerifyRequest verifyRequest = new NormalizedVerifyRequest(
				systemNameNormalizer.normalize(sysInfo.getSystemName()),
				systemName,
				Defaults.DEFAULT_CLOUD,
				AuthorizationTargetType.SERVICE_DEF,
				serviceDefNameNormalizer.normalize(match.get().getFirst()),
				serviceOpNameNormalizer.normalize(match.get().getSecond()));

		return policyEngine.isAccessGranted(verifyRequest);
	}

	//-------------------------------------------------------------------------------------------------
	private Optional<Pair<String, String>> findServiceDefinitionAndOperation(final String path, final String method) {
		logger.debug("InternalManagementServiceFilter.findServiceDefinitionAndOperation started...");

		final String templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		final String decodedPath = SecurityUtilities.getDecodedUri(path);
		String serviceDefinition = null;
		String operation = null;

		OUTER: for (final ServiceModel sModel : sysInfo.getServices()) {
			final Optional<InterfaceModel> iModelOpt = sModel.interfaces()
					.stream()
					.filter(im -> im.templateName().equals(templateName))
					.findFirst();
			if (iModelOpt.isPresent()) {
				final HttpInterfaceModel iModel = (HttpInterfaceModel) iModelOpt.get();
				for (final Entry<String, HttpOperationModel> opEntry : iModel.operations().entrySet()) {
					final String candidateMethod = opEntry.getValue().method();
					final String candidatePath = iModel.basePath() + opEntry.getValue().path();
					if (method.equalsIgnoreCase(candidateMethod) && decodedPath.equals(candidatePath)) {
						serviceDefinition = sModel.serviceDefinition();
						operation = opEntry.getKey();
						break OUTER;
					}
				}
			}
		}

		return Utilities.isEmpty(serviceDefinition)
				? Optional.empty()
				: Optional.of(Pair.of(serviceDefinition, operation));
	}
}