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
package eu.arrowhead.common.http.filter.authorization;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.http.filter.thirdparty.MultiReadRequestWrapper;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnProperty(name = Constants.ENABLE_BLACKLIST_FILTER, matchIfMissing = false)
@Order(Constants.REQUEST_FILTER_ORDER_AUTHORIZATION_BLACKLIST)
public class BlacklistFilter extends ArrowheadFilter {

	//=================================================================================================
	// members

	@Autowired
	protected SystemInfo sysInfo;

	@Autowired
	protected ArrowheadHttpService arrowheadHttpService;

	@Autowired
	private ServiceCollector collector;

	@Value(Constants.$FORCE_BLACKLIST_FILTER_WD)
	private boolean force;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		log.debug("BlacklistFilter is active");

		try {
			final MultiReadRequestWrapper requestWrapper = (request instanceof MultiReadRequestWrapper) ? (MultiReadRequestWrapper) request : new MultiReadRequestWrapper(request);

			// if requester is sysop, no need for check
			final boolean isSysop = HttpUtilities.isSysop(requestWrapper, "BlacklistFilter.doFilterInternal");

			if (!isSysop) {
				// if request is for lookup for authentication, no need for check
				final boolean isAuthLookup = isAuthenticationLookup(requestWrapper);

				if (!isAuthLookup) {
					log.debug("checking Blacklist");

					try {
						final String systemName = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM).toString();

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
						if (force) {
							throw new ForbiddenException("Blacklist system is not available, the system might be blacklisted");
						}
					}
				}
			}

			chain.doFilter(requestWrapper, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private boolean isAuthenticationLookup(final MultiReadRequestWrapper request) {

		// is the filter running inside SR?
		if (!sysInfo.getSystemName().equals(Constants.SYS_NAME_SERVICE_REGISTRY)) {
			return false;
		}

		// is the request lookup?
		final String requestTarget = Utilities.stripEndSlash(SecurityUtilities.getDecodedUri(request.getRequestURL().toString()));

		// finding the path and method of the lookup operation
		HttpOperationModel lookupOp = null;
		String lookupBasePath = null;
		final String templateName = sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;
		final ServiceModel model = collector.getServiceModel(Constants.SERVICE_DEF_SERVICE_DISCOVERY, templateName, Constants.SYS_NAME_SERVICE_REGISTRY);

		if (model == null || Utilities.isEmpty(model.interfaces())) {
			return false;
		}

		for (final InterfaceModel intf : model.interfaces()) {
			if (intf.templateName().equals(templateName) && intf.properties().containsKey(HttpInterfaceModel.PROP_NAME_OPERATIONS)) {
				final Map<String, HttpOperationModel> ops = (Map<String, HttpOperationModel>) intf.properties().get(HttpInterfaceModel.PROP_NAME_OPERATIONS);
				if (ops.containsKey(Constants.SERVICE_OP_LOOKUP)) {
					// if there is a lookup operation, we can get its path and method
					lookupOp = ops.get(Constants.SERVICE_OP_LOOKUP);
					lookupBasePath = (String) intf.properties().get(HttpInterfaceModel.PROP_NAME_BASE_PATH);
					break;
				}
			}
		}

		if (lookupOp == null || !requestTarget.endsWith(lookupBasePath + lookupOp.path()) || !request.getMethod().equalsIgnoreCase(lookupOp.method())) {
			// SR does not provide lookup operation or the request is not lookup
			return false;
		}

		// is the requester looking for the identity service definition?
		ServiceInstanceLookupRequestDTO dto = null; // expected type for service definition lookup
		try {
			// check if the content type can be mapped to the expected dto
			dto = Utilities.fromJson(request.getCachedBody(), ServiceInstanceLookupRequestDTO.class);
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