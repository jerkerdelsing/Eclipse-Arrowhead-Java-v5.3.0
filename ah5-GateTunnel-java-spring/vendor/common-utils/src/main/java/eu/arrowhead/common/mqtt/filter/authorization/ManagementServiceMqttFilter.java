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
package eu.arrowhead.common.mqtt.filter.authorization;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Defaults;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.enums.AuthorizationTargetType;

public class ManagementServiceMqttFilter implements ArrowheadMqttFilter {

	//=================================================================================================
	// members

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private ArrowheadHttpService httpService;

	@Autowired
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

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
		logger.debug("ManagementServiceMqttFilter.doFilter started...");

		if (request.getBaseTopic().contains(mgmtPath)) {
			final String systemName = request.getRequester(); // already normalized
			boolean allowed = false;

			switch (sysInfo.getManagementPolicy()) {
			case SYSOP_ONLY:
				allowed = request.isSysOp();
				break;

			case WHITELIST:
				allowed = request.isSysOp() || isWhitelisted(systemName);
				break;

			case AUTHORIZATION:
				allowed = request.isSysOp() || isWhitelisted(systemName) || isAuthorized(systemName, request.getBaseTopic(), request.getOperation());
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
		logger.debug("ManagementServiceMqttFilter.isWhitelisted started...");

		return sysInfo.getManagementWhitelist().contains(systemName);
	}

	//-------------------------------------------------------------------------------------------------
	private boolean isAuthorized(final String systemName, final String baseTopic, final String operation) {
		logger.debug("ManagementServiceMqttFilter.isAuthorized started...");

		// finding service definition
		final Optional<String> match = findServiceDefinition(baseTopic, operation);
		if (match.isEmpty()) { // can't identify the service definition
			logger.warn("Can't identify service definition for topic: {}", baseTopic + operation);
			return false;
		}

		final AuthorizationVerifyRequestDTO payload = new AuthorizationVerifyRequestDTO(
				sysInfo.getSystemName(),
				systemName,
				Defaults.DEFAULT_CLOUD,
				AuthorizationTargetType.SERVICE_DEF.name(),
				serviceDefNameNormalizer.normalize(match.get()),
				operation);

		try {
			return httpService.consumeService(
					Constants.SERVICE_DEF_AUTHORIZATION,
					Constants.SERVICE_OP_VERIFY,
					Boolean.class,
					payload);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);

			return false;
		}
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