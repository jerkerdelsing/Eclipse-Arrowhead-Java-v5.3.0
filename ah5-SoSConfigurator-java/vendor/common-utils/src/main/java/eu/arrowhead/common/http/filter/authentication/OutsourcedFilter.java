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
package eu.arrowhead.common.http.filter.authentication;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.ArrowheadFilter;
import eu.arrowhead.common.http.filter.thirdparty.MultiReadRequestWrapper;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.security.SecurityUtilities;
import eu.arrowhead.common.security.TokenCertificateBinding;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Order(Constants.REQUEST_FILTER_ORDER_AUTHENTICATION)
public class OutsourcedFilter extends ArrowheadFilter implements IAuthenticationPolicyFilter {

	//=================================================================================================
	// members

	@Value(Constants.$AUTHENTICATOR_SECRET_KEYS)
	private Map<String, String> rawSecretKeys;

	private Map<String, String> secretKeys = null;

	@Autowired
	private SystemInfo sysInfo;

	@Autowired
	private ServiceCollector collector;

	@Autowired
	private ArrowheadHttpService httpService;

	@Autowired
	private SystemNameNormalizer systemNameNormalizer;

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
		log.debug("Checking access in OutsourcedFilter...");

		try {
			initializeRequestAttributes(request);

			final MultiReadRequestWrapper requestWrapper = (request instanceof MultiReadRequestWrapper) ? (MultiReadRequestWrapper) request : new MultiReadRequestWrapper(request);

			// if request is for lookup for authentication's identity service, no need for check
			final boolean isAuthenticationLookup = isAuthenticationLookup(requestWrapper);

			if (!isAuthenticationLookup) {
				final AuthenticationData data = processAuthHeader(requestWrapper);
				TokenCertificateBinding.check(requestWrapper, data.systemName(), systemNameNormalizer);
				request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM, data.systemName());
				request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST, data.sysop());
			}

			chain.doFilter(requestWrapper, response);
		} catch (final ArrowheadException ex) {
			handleException(ex, response);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private boolean isAuthenticationLookup(final MultiReadRequestWrapper request) {
		log.debug("OutsourcedFilter.isAuthenticationLookup started...");

		// is the filter running inside ServiceRegistry?
		if (!sysInfo.getSystemName().equals(Constants.SYS_NAME_SERVICE_REGISTRY)) {
			return false;
		}

		// is the request is a lookup?
		// finding the path and method of the lookup operation
		String basePath = null;
		HttpOperationModel lookupOp = null;
		final ServiceModel model = collector.getServiceModel(
				Constants.SERVICE_DEF_SERVICE_DISCOVERY,
				sysInfo.isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME,
				Constants.SYS_NAME_SERVICE_REGISTRY);

		if (model == null) {
			return false;
		}

		for (final InterfaceModel intf : model.interfaces()) {
			final Map<String, HttpOperationModel> ops = (Map<String, HttpOperationModel>) intf.properties().get(HttpInterfaceModel.PROP_NAME_OPERATIONS);
			if (ops.containsKey(Constants.SERVICE_OP_LOOKUP)) {
				// if there is a lookup operation, we can get its path and method
				basePath = (String) intf.properties().get(HttpInterfaceModel.PROP_NAME_BASE_PATH);
				lookupOp = ops.get(Constants.SERVICE_OP_LOOKUP);
				break;
			}
		}

		final String requestTarget = Utilities.stripEndSlash(SecurityUtilities.getDecodedUri(request.getRequestURI()));
		if (lookupOp == null || basePath == null || !request.getMethod().equalsIgnoreCase(lookupOp.method()) || !requestTarget.equals(basePath + lookupOp.path())) {
			// SR does not provide lookup operation or the request is not lookup
			return false;
		}

		// is the requester looking for the identity service definition?
		ServiceInstanceLookupRequestDTO dto = null; // expected type for service definition lookup
		try {
			// check if the content type can be mapped to the expected DTO
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

	//-------------------------------------------------------------------------------------------------
	private AuthenticationData processAuthHeader(final HttpServletRequest request) {
		log.debug("OutsourcedFilter.processAuthHeader started...");

		final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (Utilities.isEmpty(authHeader)) {
			throw new AuthException("No authorization header has been provided");
		}

		String[] split = authHeader.trim().split(" ");
		if (split.length != 2 || !split[0].equals(Constants.AUTHENTICATION_SCHEMA)) {
			throw new AuthException("Invalid authorization header");
		}

		split = split[1].split(Constants.AUTHENTICATION_KEY_DELIMITER);
		if (split[0].equals(Constants.AUTHENTICATION_PREFIX_AUTHENTICATOR_KEY)) {
			return checkAuthenticaticatorKey(split);
		}

		if (split[0].equals(Constants.AUTHENTICATION_PREFIX_IDENTITY_TOKEN)) {
			return checkIdentityToken(split);
		}

		throw new AuthException("Invalid authorization header");
	}

	//-------------------------------------------------------------------------------------------------
	// handling header using IDENTITY-TOKEN//<token> format
	private AuthenticationData checkIdentityToken(final String[] headerParts) {
		log.debug("OutsourcedFilter.checkIdentityToken started...");

		if (headerParts.length != 2) {
			throw new AuthException("Invalid authorization header");
		}

		final String token = headerParts[1].trim();
		final IdentityVerifyResponseDTO response = httpService.consumeService(Constants.SERVICE_DEF_IDENTITY, Constants.SERVICE_OP_IDENTITY_VERIFY, IdentityVerifyResponseDTO.class, List.of(token));
		if (response.verified()) {
			return new AuthenticationData(
					response.systemName(),
					response.sysop());
		}

		throw new AuthException("Invalid authorization header");
	}

	//-------------------------------------------------------------------------------------------------
	// handling header using AUTHENTICATOR-KEY//<system-name>//<hash>
	@SuppressWarnings("checkstyle:MagicNumber")
	private AuthenticationData checkAuthenticaticatorKey(final String[] headerParts) {
		log.debug("OutsourcedFilter.checkAuthenticaticatorKey started...");

		if (Utilities.isEmpty(secretKeys)) {
			// this system does not support authenticator keys
			throw new AuthException("Invalid authorization header");
		}

		if (headerParts.length != 3) {
			throw new AuthException("Invalid authorization header");
		}

		final String systemName = systemNameNormalizer.normalize(headerParts[1]);
		final String hash = headerParts[2].trim();

		if (!secretKeys.containsKey(systemName)) {
			// requester system is unknown
			throw new AuthException("Invalid authorization header");
		}

		try {
			final String calculatedHash = SecurityUtilities.hashWithSecretKey(systemName, secretKeys.get(systemName));
			if (!hash.equals(calculatedHash)) {
				// secret keys aren't the same
				throw new AuthException("Invalid authorization header");
			}
		} catch (final InvalidKeyException | NoSuchAlgorithmException | IllegalArgumentException ex) {
			log.error(ex.getMessage());
			log.debug(ex);

			// can't authenticate
			throw new AuthException("Invalid authorization header");
		}

		// an authentication system cannot be sysop
		return new AuthenticationData(systemName, false);
	}

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		if (!Utilities.isEmpty(rawSecretKeys)) {
			log.info("Authentication keys are supported.");

			secretKeys = new ConcurrentHashMap<>(rawSecretKeys.size());
			rawSecretKeys.forEach((name, secretKey) -> secretKeys.put(systemNameNormalizer.normalize(name), secretKey.trim()));
		}
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	private record AuthenticationData(
			String systemName,
			boolean sysop) {
	}
}