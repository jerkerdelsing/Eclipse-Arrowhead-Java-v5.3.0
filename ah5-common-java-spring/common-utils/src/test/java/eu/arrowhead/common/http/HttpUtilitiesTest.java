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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.enums.ExceptionType;

@ExtendWith(MockitoExtension.class)
public class HttpUtilitiesTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateHttpStatusFromArrowheadExceptionNullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> HttpUtilities.calculateHttpStatusFromArrowheadException(null));

		assertEquals("Exception is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateHttpStatusFromArrowheadExceptionTypeNull() {
		final ArrowheadException exception = new ArrowheadException("test");
		ReflectionTestUtils.setField(exception, "exceptionType", null);

		final HttpStatus status = HttpUtilities.calculateHttpStatusFromArrowheadException(exception);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateHttpStatusFromArrowheadExceptionOk() {
		final HttpStatus status = HttpUtilities.calculateHttpStatusFromArrowheadException(new InvalidParameterException("test"));

		assertEquals(HttpStatus.BAD_REQUEST, status);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCalculateHttpStatusFromArrowheadExceptionResolveProblem() {
		try (MockedStatic<HttpStatus> httpStatusMock = Mockito.mockStatic(HttpStatus.class)) {
			httpStatusMock.when(() -> HttpStatus.resolve(anyInt())).thenReturn(null);

			final HttpStatus status = HttpUtilities.calculateHttpStatusFromArrowheadException(new AuthException("test"));
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status);

			httpStatusMock.verify(() -> HttpStatus.resolve(401));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateErrorMessageDTO1() {
		final ExternalServerError ex = new ExternalServerError("test", "testOrigin");

		final ErrorMessageDTO dto = HttpUtilities.createErrorMessageDTO(ex);

		assertEquals("test", dto.errorMessage());
		assertEquals(503, dto.errorCode());
		assertEquals(ExceptionType.EXTERNAL_SERVER_ERROR, dto.exceptionType());
		assertEquals("testOrigin", dto.origin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateErrorMessageDTO2ExternalOriginNull() {
		final ExternalServerError ex = new ExternalServerError("test", "testOrigin");

		final ErrorMessageDTO dto = HttpUtilities.createErrorMessageDTO(ex, null);

		assertEquals("test", dto.errorMessage());
		assertEquals(503, dto.errorCode());
		assertEquals(ExceptionType.EXTERNAL_SERVER_ERROR, dto.exceptionType());
		assertEquals("testOrigin", dto.origin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateErrorMessageDTO2ExternalOriginNotNull() {
		final ExternalServerError ex = new ExternalServerError("test", "testOrigin");

		final ErrorMessageDTO dto = HttpUtilities.createErrorMessageDTO(ex, "externalOrigin");

		assertEquals("test", dto.errorMessage());
		assertEquals(503, dto.errorCode());
		assertEquals(ExceptionType.EXTERNAL_SERVER_ERROR, dto.exceptionType());
		assertEquals("externalOrigin", dto.origin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTONullInput() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> HttpUtilities.createExceptionFromErrorMessageDTO(null));

		assertEquals("Error message object is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTONullExceptionType() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, null, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.ARROWHEAD, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTOExceptionTypeArrowhead() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, ExceptionType.ARROWHEAD, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.ARROWHEAD, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTOExceptionTypeAuth() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, ExceptionType.AUTH, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.AUTH, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTOExceptionTypeForbidden() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, ExceptionType.FORBIDDEN, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.FORBIDDEN, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTOExceptionTypeInvalidParameter() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, ExceptionType.INVALID_PARAMETER, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.INVALID_PARAMETER, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTOExceptionTypeDataNotFound() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, ExceptionType.DATA_NOT_FOUND, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.DATA_NOT_FOUND, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTOExceptionTypeTimeout() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, ExceptionType.TIMEOUT, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.TIMEOUT, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTOExceptionTypeLocked() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, ExceptionType.LOCKED, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.LOCKED, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTOExceptionTypeExternalServerError() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, ExceptionType.EXTERNAL_SERVER_ERROR, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.EXTERNAL_SERVER_ERROR, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateExceptionFromErrorMessageDTOExceptionTypeInternalServerError() {
		final ErrorMessageDTO dto = new ErrorMessageDTO("test", 0, ExceptionType.INTERNAL_SERVER_ERROR, "origin");

		final ArrowheadException ex = HttpUtilities.createExceptionFromErrorMessageDTO(dto);

		assertEquals("test", ex.getMessage());
		assertEquals(ExceptionType.INTERNAL_SERVER_ERROR, ex.getExceptionType());
		assertEquals("origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateUri6Defaults() {
		final UriComponents uri = HttpUtilities.createURI(null, null, 0, null, null, (String[]) null);

		assertEquals("http://localhost:80", uri.toString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateUri6Everything() {
		final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(2);
		queryParams.add("param1", "text");
		queryParams.add("param2", "10");
		final UriComponents uri = HttpUtilities.createURI(
				"https",
				"192.168.0.10",
				12345,
				queryParams,
				"/test",
				"a",
				"b");

		assertEquals("https://192.168.0.10:12345/test/a/b?param1=text&param2=10", uri.toString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateUri6EverythingButPathSegments() {
		final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>(2);
		queryParams.add("param1", "text");
		queryParams.add("param2", "10");
		final UriComponents uri = HttpUtilities.createURI(
				"https",
				"192.168.0.10",
				12345,
				queryParams,
				"/test",
				new String[0]);

		assertEquals("https://192.168.0.10:12345/test?param1=text&param2=10", uri.toString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateUri4Everything() {
		final UriComponents uri = HttpUtilities.createURI(
				"https",
				"192.168.0.10",
				12345,
				"/test");

		assertEquals("https://192.168.0.10:12345/test", uri.toString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateUri5NoQueryParams() {
		final UriComponents uri = HttpUtilities.createURI(
				"https",
				"192.168.0.10",
				12345,
				"/test",
				(String[]) null);

		assertEquals("https://192.168.0.10:12345/test", uri.toString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateUri5NoQueryParams2() {
		final UriComponents uri = HttpUtilities.createURI(
				"https",
				"192.168.0.10",
				12345,
				"/test",
				new String[0]);

		assertEquals("https://192.168.0.10:12345/test", uri.toString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateUri5InvalidParamCount() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> HttpUtilities.createURI(
						"https",
						"192.168.0.10",
						12345,
						"/test",
						"param1",
						"text",
						"param2"));

		assertEquals("queryParams variable arguments contains a key without value", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testCreateUri5QueryParams() {
		final UriComponents uri = HttpUtilities.createURI(
				"https",
				"192.168.0.10",
				12345,
				"/test",
				"param1",
				"text",
				"param2",
				"10");

		assertEquals("https://192.168.0.10:12345/test?param1=text&param2=10", uri.toString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireNameNullRequest() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> HttpUtilities.acquireName(null, "origin"));

		assertEquals("Request is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireNameNameIsMissing() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> HttpUtilities.acquireName(new MockHttpServletRequest(), "origin"));

		assertEquals("Name is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAcquireNameOk() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.authenticated.system", "RequesterSystem");

		final String result = HttpUtilities.acquireName(request, "origin");

		assertEquals("RequesterSystem", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsSysopRequestNull() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> HttpUtilities.isSysop(null, "origin"));

		assertEquals("Request is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsSysopNoAttributeFalse() {
		final boolean result = HttpUtilities.isSysop(new MockHttpServletRequest(), "origin");

		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsSysopFalse() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", false);

		final boolean result = HttpUtilities.isSysop(request, "origin");

		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsSysopTrue() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("arrowhead.sysop.request", true);

		final boolean result = HttpUtilities.isSysop(request, "origin");

		assertTrue(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsValidHttpMethodNullInput() {
		final boolean result = HttpUtilities.isValidHttpMethod(null);

		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsValidHttpMethodEmptyInput() {
		final boolean result = HttpUtilities.isValidHttpMethod("");

		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsValidHttpMethodNotValid() {
		final boolean result = HttpUtilities.isValidHttpMethod("WRONG");

		assertFalse(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsValidHttpMethodValid() {
		final boolean result = HttpUtilities.isValidHttpMethod("  get\t");

		assertTrue(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateAuthorizationHeaderDeclared() {
		final SystemInfo sysInfoMock = Mockito.mock(SystemInfo.class);

		when(sysInfoMock.getIdentityToken()).thenReturn(null);
		when(sysInfoMock.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.DECLARED);
		when(sysInfoMock.getSystemName()).thenReturn("ConsumerAuthorization");

		final String result = HttpUtilities.calculateAuthorizationHeader(sysInfoMock);

		verify(sysInfoMock).getIdentityToken();
		verify(sysInfoMock).getAuthenticationPolicy();
		verify(sysInfoMock).getSystemName();

		assertEquals("Bearer SYSTEM//ConsumerAuthorization", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateAuthorizationHeaderCertificate() {
		final SystemInfo sysInfoMock = Mockito.mock(SystemInfo.class);

		when(sysInfoMock.getIdentityToken()).thenReturn(null);
		when(sysInfoMock.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.CERTIFICATE);

		final String result = HttpUtilities.calculateAuthorizationHeader(sysInfoMock);

		verify(sysInfoMock).getIdentityToken();
		verify(sysInfoMock).getAuthenticationPolicy();

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateAuthorizationHeaderOutsourcedLogout() {
		final SystemInfo sysInfoMock = Mockito.mock(SystemInfo.class);

		when(sysInfoMock.getIdentityToken()).thenReturn(null);
		when(sysInfoMock.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.OUTSOURCED);

		final String result = HttpUtilities.calculateAuthorizationHeader(sysInfoMock);

		verify(sysInfoMock).getIdentityToken();
		verify(sysInfoMock).getAuthenticationPolicy();

		assertNull(result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateAuthorizationHeaderOutsourcedLogin() {
		final SystemInfo sysInfoMock = Mockito.mock(SystemInfo.class);

		when(sysInfoMock.getIdentityToken()).thenReturn("longidentitytoken");
		when(sysInfoMock.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.OUTSOURCED);

		final String result = HttpUtilities.calculateAuthorizationHeader(sysInfoMock);

		verify(sysInfoMock).getIdentityToken();
		verify(sysInfoMock).getAuthenticationPolicy();

		assertEquals("Bearer IDENTITY-TOKEN//longidentitytoken", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCalculateAuthorizationHeaderInternal() {
		final SystemInfo sysInfoMock = Mockito.mock(SystemInfo.class);

		when(sysInfoMock.getIdentityToken()).thenReturn("hash");
		when(sysInfoMock.getAuthenticationPolicy()).thenReturn(AuthenticationPolicy.INTERNAL);
		when(sysInfoMock.getSystemName()).thenReturn("Authentication");

		final String result = HttpUtilities.calculateAuthorizationHeader(sysInfoMock);

		verify(sysInfoMock).getIdentityToken();
		verify(sysInfoMock).getAuthenticationPolicy();
		verify(sysInfoMock).getSystemName();

		assertEquals("Bearer AUTHENTICATOR-KEY//Authentication//hash", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateURIExpandsPathTemplate() {
		assertEquals("https://sr:8443/serviceregistry/service-discovery/revoke/abc",
				HttpUtilities.createURI("https", "sr", 8443, (MultiValueMap<String, String>) null, "/serviceregistry/service-discovery/revoke/{instanceId}", "abc").toUriString());
		assertEquals("https://dm:8461/data/systems/a/services/b/data",
				HttpUtilities.createURI("https", "dm", 8461, (MultiValueMap<String, String>) null, "/data/systems/{systemName}/services/{serviceName}/data", "a", "b").toUriString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateURIAppendsSegmentsWithoutOrBeyondPlaceholders() {
		assertEquals("https://sr:8443/revoke/abc", HttpUtilities.createURI("https", "sr", 8443, (MultiValueMap<String, String>) null, "/revoke", "abc").toUriString());
		assertEquals("https://sr:8443/revoke/abc/x", HttpUtilities.createURI("https", "sr", 8443, (MultiValueMap<String, String>) null, "/revoke/{id}", "abc", "x").toUriString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateURIMissingPathParameter() {
		assertThrows(IllegalArgumentException.class, () -> HttpUtilities.createURI("https", "sr", 8443, (MultiValueMap<String, String>) null, "/revoke/{id}"));
	}
}