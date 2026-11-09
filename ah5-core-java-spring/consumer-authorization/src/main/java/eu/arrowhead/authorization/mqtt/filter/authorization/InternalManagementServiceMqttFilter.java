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
package eu.arrowhead.authorization.mqtt.filter.authorization;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import eu.arrowhead.authorization.service.dto.NormalizedVerifyRequest;
import eu.arrowhead.authorization.service.engine.AuthorizationPolicyEngine;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

public class InternalManagementServiceMqttFilter implements ArrowheadMqttFilter {

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

	private static final String mgmtPath = "/management";

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public int order() {
		return Constants.REQUEST_FILTER_ORDER_AUTHORIZATION_MGMT_SERVICE;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void doFilter(final String authKey, final MqttRequestModel request) {
		logger.debug("InternalManagementServiceMqttFilter.doFilter started...");

		if (request.getBaseTopic().contains(mgmtPath)) {
			final String normalizedSystemName = systemNameNormalizer.normalize(request.getRequester());
			boolean allowed = false;

			switch (sysInfo.getManagementPolicy()) {
			case SYSOP_ONLY:
				allowed = request.isSysOp();
				break;

			case WHITELIST:
				allowed = request.isSysOp() || isWhitelisted(normalizedSystemName);
				break;

			case AUTHORIZATION:
				allowed = request.isSysOp() || isWhitelisted(normalizedSystemName) || isAuthorized(normalizedSystemName, request.getBaseTopic(), request.getOperation());
				break;

			default:
				throw new InternalServerError("Unimplemented management policy: " + sysInfo.getManagementPolicy());
			}

			if (!allowed) {
				throw new ForbiddenException("Requester has no management permission");
			}
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	public boolean isWhitelisted(final String systemName) {
		logger.debug("InternalManagementServiceMqttFilter.isWhitelisted started...");

		return sysInfo.getManagementWhitelist().contains(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isAuthorized(final String systemName, final String baseTopic, final String operation) {
		logger.debug("InternalManagementServiceMqttFilter.isAuthorized started...");

		// finding service definition
		final Optional<String> match = findServiceDefinition(baseTopic, operation);
		if (match.isEmpty()) { // can't identify the service definition
			logger.warn("Can't identify service definition for topic: {}", baseTopic + operation);
			return false;
		}

		final NormalizedVerifyRequest verifyRequest = new NormalizedVerifyRequest(
				systemNameNormalizer.normalize(sysInfo.getSystemName()),
				systemName,
				Defaults.DEFAULT_CLOUD,
				AuthorizationTargetType.SERVICE_DEF,
				serviceDefNameNormalizer.normalize(match.get()),
				serviceOpNameNormalizer.normalize(operation));

		return policyEngine.isAccessGranted(verifyRequest);
	}

	//-------------------------------------------------------------------------------------------------
	private Optional<String> findServiceDefinition(final String baseTopic, final String operation) {
		logger.debug("InternalManagementServiceFilter.findServiceDefinition started...");

		final String templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		String serviceDefinition = null;

		for (final ServiceModel sModel : sysInfo.getServices()) {
			final Optional<InterfaceModel> iModelOpt = sModel.interfaces()
					.stream()
					.filter(im -> im.templateName().equals(templateName))
					.findFirst();
			if (iModelOpt.isPresent()) {
				final MqttInterfaceModel iModel = (MqttInterfaceModel) iModelOpt.get();
				if (iModel.baseTopic().equals(baseTopic) && iModel.operations().contains(operation)) {
					serviceDefinition = sModel.serviceDefinition();
					break;
				}
			}
		}

		return Utilities.isEmpty(serviceDefinition)
				? Optional.empty()
				: Optional.of(serviceDefinition);
	}
}