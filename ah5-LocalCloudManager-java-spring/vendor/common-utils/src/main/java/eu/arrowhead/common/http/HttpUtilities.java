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
package eu.arrowhead.common.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.common.exception.TimeoutException;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.enums.ExceptionType;
import jakarta.servlet.http.HttpServletRequest;

public final class HttpUtilities {

	//=================================================================================================
	// members

	private static final Logger logger = LogManager.getLogger(HttpUtilities.class);

	private static final Pattern PATH_PLACEHOLDER = Pattern.compile("\\{[^}/]+\\}");

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static HttpStatus calculateHttpStatusFromArrowheadException(final ArrowheadException ex) {
		Assert.notNull(ex, "Exception is null");

		final ExceptionType exceptionType = ex.getExceptionType() != null ? ex.getExceptionType() : ExceptionType.INTERNAL_SERVER_ERROR;
		HttpStatus status = HttpStatus.resolve(exceptionType.getErrorCode());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return status;
	}

	//-------------------------------------------------------------------------------------------------
	public static ErrorMessageDTO createErrorMessageDTO(final ArrowheadException ex) {
		return new ErrorMessageDTO(ex.getMessage(), calculateHttpStatusFromArrowheadException(ex).value(), ex.getExceptionType(), ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	public static ErrorMessageDTO createErrorMessageDTO(final ArrowheadException ex, final String origin) {
		return new ErrorMessageDTO(
				ex.getMessage(),
				calculateHttpStatusFromArrowheadException(ex).value(),
				ex.getExceptionType(),
				Utilities.isEmpty(origin) ? ex.getOrigin() : origin);
	}

	//-------------------------------------------------------------------------------------------------
	public static ArrowheadException createExceptionFromErrorMessageDTO(final ErrorMessageDTO dto) {
		Assert.notNull(dto, "Error message object is null");

		if (dto.exceptionType() == null) {
			return new ArrowheadException(dto.errorMessage(), dto.origin());
		}

		switch (dto.exceptionType()) {
		case ARROWHEAD:
			return new ArrowheadException(dto.errorMessage(), dto.origin());
		case AUTH:
			return new AuthException(dto.errorMessage(), dto.origin());
		case FORBIDDEN:
			return new ForbiddenException(dto.errorMessage(), dto.origin());
		case INVALID_PARAMETER:
			return new InvalidParameterException(dto.errorMessage(), dto.origin());
		case DATA_NOT_FOUND:
			return new DataNotFoundException(dto.errorMessage(), dto.origin());
		case TIMEOUT:
			return new TimeoutException(dto.errorMessage(), dto.origin());
		case LOCKED:
			return new LockedException(dto.errorMessage(), dto.origin());
		case EXTERNAL_SERVER_ERROR:
			return new ExternalServerError(dto.errorMessage(), dto.origin());
		case INTERNAL_SERVER_ERROR:
			return new InternalServerError(dto.errorMessage(), dto.origin());
		default:
			logger.error("Unknown exception type: {}", dto.exceptionType());
			return new ArrowheadException(dto.errorMessage(), dto.origin());
		}
	}

	//-------------------------------------------------------------------------------------------------
	/**
	 * @param scheme       default: http
	 * @param host         default: localhost
	 * @param port         default: 80
	 * @param queryParams  default: null
	 * @param path         default: null
	 * @param pathSegments default: null
	 */
	public static UriComponents createURI(
			final String scheme,
			final String host,
			final int port,
			final MultiValueMap<String, String> queryParams,
			final String path,
			final String... pathSegments) {
		final UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		builder.scheme(Utilities.isEmpty(scheme) ? Constants.HTTP : scheme.trim())
				.host(Utilities.isEmpty(host) ? Constants.LOCALHOST : host.trim())
				.port(port <= 0 ? Constants.HTTP_PORT : port);

		// a registered path may be a template ("/revoke/{id}"): its placeholders take the first path segments in order,
		// the remaining segments are appended
		final int placeholders = Utilities.isEmpty(path) ? 0 : countPlaceholders(path);
		final int segments = pathSegments == null ? 0 : pathSegments.length;
		if (placeholders > segments) {
			throw new IllegalArgumentException("Path " + path + " needs " + placeholders + " path parameters, got " + segments);
		}

		if (!Utilities.isEmpty(path)) {
			builder.path(path);
		}

		if (segments > placeholders) {
			builder.pathSegment(Arrays.copyOfRange(pathSegments, placeholders, segments));
		}

		if (queryParams != null) {
			builder.queryParams(queryParams);
		}

		return placeholders == 0 ? builder.build() : builder.buildAndExpand((Object[]) Arrays.copyOf(pathSegments, placeholders));
	}

	//-------------------------------------------------------------------------------------------------
	private static int countPlaceholders(final String path) {
		int count = 0;
		final Matcher matcher = PATH_PLACEHOLDER.matcher(path);
		while (matcher.find()) {
			count++;
		}

		return count;
	}

	//-------------------------------------------------------------------------------------------------
	public static UriComponents createURI(final String scheme, final String host, final int port, final String path) {
		return createURI(scheme, host, port, null, path, (String[]) null);
	}

	//-------------------------------------------------------------------------------------------------
	public static UriComponents createURI(final String scheme, final String host, final int port, final String path, final String... queryParams) {
		if (queryParams == null || queryParams.length == 0) {
			return createURI(scheme, host, port, path);
		}

		if (queryParams.length % 2 != 0) {
			throw new InvalidParameterException("queryParams variable arguments contains a key without value");
		}

		final LinkedMultiValueMap<String, String> query = new LinkedMultiValueMap<>();

		int count = 1;
		String key = "";
		for (final String varArg : queryParams) {
			if (count % 2 != 0) {
				query.putIfAbsent(varArg, new ArrayList<>());
				key = varArg;
			} else {
				query.get(key).add(varArg);
			}
			count++;
		}

		return createURI(scheme, host, port, query, path);
	}

	//-------------------------------------------------------------------------------------------------
	public static String acquireName(final HttpServletRequest request, final String origin) throws InvalidParameterException {
		if (request == null) {
			throw new InvalidParameterException("Request is null", origin);
		}

		final Object nameObject = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_AUTHENTICATED_SYSTEM);
		if (nameObject == null) {
			throw new InvalidParameterException("Name is missing", origin);
		}

		return nameObject.toString();
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isSysop(final HttpServletRequest request, final String origin) throws InvalidParameterException {

		if (request == null) {
			throw new InvalidParameterException("Request is null", origin);
		}

		final Object isSysopObject = request.getAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST);

		return isSysopObject == null ? false : (boolean) isSysopObject;
	}

	//-------------------------------------------------------------------------------------------------
	public static boolean isValidHttpMethod(final String str) {
		if (!Utilities.isEmpty(str)) {
			final HttpMethod method = HttpMethod.valueOf(str.toUpperCase().trim());

			return Arrays
					.stream(HttpMethod.values())
					.anyMatch(method::equals);
		}

		return false;
	}

	//-------------------------------------------------------------------------------------------------
	public static String calculateAuthorizationHeader(final SystemInfo sysInfo) {
		logger.debug("calculateAuthorizationHeader started...");

		final String identityToken = sysInfo.getIdentityToken();
		switch (sysInfo.getAuthenticationPolicy()) {
		case DECLARED:
			return Constants.AUTHENTICATION_SCHEMA + " " + Constants.AUTHENTICATION_PREFIX_SYSTEM + Constants.AUTHENTICATION_KEY_DELIMITER + sysInfo.getSystemName();
		case OUTSOURCED:
			return identityToken == null ? null
					: Constants.AUTHENTICATION_SCHEMA + " " + Constants.AUTHENTICATION_PREFIX_IDENTITY_TOKEN + Constants.AUTHENTICATION_KEY_DELIMITER + identityToken;
		case INTERNAL:
			return Constants.AUTHENTICATION_SCHEMA + " " + Constants.AUTHENTICATION_PREFIX_AUTHENTICATOR_KEY + Constants.AUTHENTICATION_KEY_DELIMITER
					+ sysInfo.getSystemName() + Constants.AUTHENTICATION_KEY_DELIMITER + identityToken;
		default:
			return null;
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private HttpUtilities() {
		throw new UnsupportedOperationException();
	}
}