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

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.filter.ArrowheadMqttFilter;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;

@Service
@ConditionalOnProperty(name = { Constants.MQTT_API_ENABLED, Constants.ENABLE_BLACKLIST_FILTER }, havingValue = "true", matchIfMissing = false)
public class BlacklistMqttFilter implements ArrowheadMqttFilter {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	protected SystemInfo sysInfo;

	@Autowired
	protected ArrowheadHttpService arrowheadHttpService;

	@Autowired
	private ServiceCollector collector;

	@Value(Constants.$FORCE_BLACKLIST_FILTER_WD)
	private boolean force;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public int order() {
		return Constants.REQUEST_FILTER_ORDER_AUTHORIZATION_BLACKLIST;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public void doFilter(final String authKey, final MqttRequestModel request) {
		logger.debug("BlacklistMqttFilter.doFilter started...");

		// if requester is sysop, no need for check
		final boolean isSysop = request.isSysOp();
		if (isSysop) {
			return;
		}

		// if request is for lookup for authentication, no need for check
		final boolean isAuthLookup = isAuthenticationLookup(request);
		if (!isAuthLookup) {
			logger.debug("checking Blacklist");
			try {
				final String systemName = request.getRequester();

				// if requester is blacklist or is on the exclude list, no need for check
				if (!systemName.equals(Constants.SYS_NAME_BLACKLIST)
						&& !sysInfo.getBlacklistCheckExcludeList().contains(systemName)) {
					final boolean isBlacklisted = arrowheadHttpService.consumeService(
							Constants.SERVICE_DEF_BLACKLIST_DISCOVERY,
							Constants.SERVICE_OP_CHECK,
							Constants.SYS_NAME_BLACKLIST,
							Boolean.TYPE,
							List.of(systemName));

					if (isBlacklisted) {
						throw new ForbiddenException(systemName + " system is blacklisted");
					}
				}
			} catch (final ForbiddenException | AuthException ex) {
				throw ex;
			} catch (final ArrowheadException ex) {
				logger.error("Blacklist server is not available");
				logger.debug("Strict blacklist filter: " + force);
				logger.debug(ex);
				if (force) {
					throw new ForbiddenException("Blacklist system is not available, the system might be blacklisted");
				}
			}
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private boolean isAuthenticationLookup(final MqttRequestModel request) {

		// is the filter running inside SR?
		if (!sysInfo.getSystemName().equals(Constants.SYS_NAME_SERVICE_REGISTRY)) {
			return false;
		}

		// is the topic service discovery and is the operation lookup?

		// check if the operation is lookup
		if (!request.getOperation().equals(Constants.SERVICE_OP_LOOKUP)) {
			return false;
		}
		// finding the base topic of the lookup operation
		final String templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		String lookupBaseTopic = null;
		final ServiceModel model = collector.getServiceModel(Constants.SERVICE_DEF_SERVICE_DISCOVERY, templateName, Constants.SYS_NAME_SERVICE_REGISTRY);

		if (model == null || Utilities.isEmpty(model.interfaces())) {
			return false;
		}

		for (final InterfaceModel intf : model.interfaces()) {
			if (intf.templateName().equals(templateName) && intf.properties().containsKey(MqttInterfaceModel.PROP_NAME_OPERATIONS)) {
				final Set<String> ops = (Set<String>) intf.properties().get(MqttInterfaceModel.PROP_NAME_OPERATIONS);
				if (ops.contains(Constants.SERVICE_OP_LOOKUP)) {
					// if there is a lookup operation, we found the corresponding base topic
					lookupBaseTopic = (String) intf.properties().get(MqttInterfaceModel.PROP_NAME_BASE_TOPIC);
					break;
				}
			}
		}

		if (lookupBaseTopic == null || !request.getBaseTopic().equals(lookupBaseTopic)) {
			// SR does not provide any topic with lookup operation or the request topic does not match
			return false;
		}

		// is the requester looking for the identity service definition?
		ServiceInstanceLookupRequestDTO dto = null; // expected type for service definition lookup
		try {
			// check if the content type can be mapped to the expected dto
			dto = Utilities.fromJson(Utilities.toJson(request.getPayload()), ServiceInstanceLookupRequestDTO.class);
		} catch (final Exception ex) {
			return false;
		}

		if (dto == null || dto.serviceDefinitionNames() == null || dto.serviceDefinitionNames().size() != 1 || !dto.serviceDefinitionNames().getFirst().equals(Constants.SERVICE_DEF_IDENTITY)) {
			// dto is null or the requester is not (only) looking for the identity
			return false;
		}

		return true;
	}
}