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
package eu.arrowhead.common.http; // checkstyle file length checker can not be suppressed with @SuppressWarnings

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponents;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.dto.enums.ExceptionType;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.el.MethodNotFoundException;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.SslProvider.SslContextSpec;

@ExtendWith(MockitoExtension.class)
public class HttpServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private HttpService service;

	@Spy
	private ObjectMapper mapper;

	@Mock
	private SSLProperties sslProperties;

	//=================================================================================================
	// members

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSendRequest6ClassHttpMethodNull() {
		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test");

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.sendRequest(uri, null, Void.TYPE, "payload", null, Map.of()));

		assertEquals("Request method is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSendRequest6ClassUriNull() {
		final Throwable ex = assertThrows(NullPointerException.class,
				() -> service.sendRequest(null, HttpMethod.POST, Void.TYPE, "payload", null, Map.of()));

		assertEquals("HttpService.sendRequest method received null URI", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSendRequest6ClassNotSupportedMethod() {
		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test");

		final Throwable ex = assertThrows(MethodNotFoundException.class,
				() -> service.sendRequest(uri, HttpMethod.TRACE, Void.TYPE, "payload", null, Map.of()));

		assertEquals("Invalid method type was given to the HttpService.sendRequest() method", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSendRequest6ClassSecureRequestInsecureMode() {
		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, "payload", null, Map.of()));

		assertEquals("SSL Context is not set, but secure request sending was invoked. An insecure application may not send requests to secure servers", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6ClassInsecureBadRequest() {
		final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "httpClient", httpClientMock);

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test");
		final ErrorMessageDTO error = new ErrorMessageDTO("test", 400, ExceptionType.INVALID_PARAMETER, "origin");
		final String errorBody = Utilities.toJson(error);

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(Void.TYPE)).thenThrow(new WebClientResponseException(400, "Bad Request", null, errorBody.getBytes(), null));

			final Throwable ex = assertThrows(InvalidParameterException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(Void.TYPE);

			assertEquals("test", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest6ClassSecureNotStandardError() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final RequestHeadersSpec headersSpecMock = Mockito.mock(RequestHeadersSpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.bodyValue("payload")).thenReturn(headersSpecMock);
			when(headersSpecMock.header("Authorization", new String[] { "Bearer SYSTEM//RequesterSystem" })).thenReturn(headersSpecMock);
			when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(Void.TYPE)).thenThrow(new WebClientResponseException(400, "Bad Request", null, "errorPayload".getBytes(), null));

			final Throwable ex = assertThrows(ArrowheadException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, "payload", null, Map.of("Authorization", "Bearer SYSTEM//RequesterSystem")));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).bodyValue("payload");
			verify(headersSpecMock).header("Authorization", new String[] { "Bearer SYSTEM//RequesterSystem" });
			verify(headersSpecMock).retrieve();
			verify(responseSpecMock).bodyToMono(Void.TYPE);

			assertEquals("errorPayload, status code: 400", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked" })
	@Test
	public void testSendRequest6ClassSecureNotStandardError2() throws KeyManagementException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final SslContext sslContext = createSSLContext();
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");

		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class);
				MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(httpClientMock.secure(any(Consumer.class))).thenReturn(httpClientMock);
			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(Void.TYPE)).thenThrow(new WebClientResponseException(400, "Bad Request", null, null, null));

			final Throwable ex = assertThrows(ArrowheadException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, null, sslContext, Map.of()));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock).secure(any(Consumer.class));
			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(Void.TYPE);

			assertEquals("Bad Request, status code: 400", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6ClassSecurePKIXPathError() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final Exception cause = new Exception("PKIX path building failed:");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(Void.TYPE)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(ForbiddenException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(Void.TYPE);

			assertEquals("The system at https://localhost:12345/test is not part of the same certificate chain of trust", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6ClassSecureNoSAN() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final Exception cause = new Exception("No subject alternative names are found");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(Void.TYPE)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(AuthException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(Void.TYPE);

			assertEquals("The certificate of the system at https://localhost:12345/test does not contain the specified IP address or DNS name as a Subject Alternative Name", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6ClassSecureNoMatchingName() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final Exception cause = new Exception("No name matching is found");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(Void.TYPE)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(AuthException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(Void.TYPE);

			assertEquals("The certificate of the system at https://localhost:12345/test does not contain the specified IP address or DNS name as a Subject Alternative Name", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6ClassSecureExternalError1() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final Exception cause = new Exception("something unexpected");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(Void.TYPE)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(ExternalServerError.class,
					() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(Void.TYPE);

			assertEquals("Could not get any response from: https://localhost:12345/test", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6ClassSecureExternalError2() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final Exception cause = new Exception((String) null);

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(Void.TYPE)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(ExternalServerError.class,
					() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(Void.TYPE);

			assertEquals("Could not get any response from: https://localhost:12345/test", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6ClassSecureExternalError3() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(Void.TYPE)).thenThrow(new RuntimeException("runtime test"));

			final Throwable ex = assertThrows(ExternalServerError.class,
					() -> service.sendRequest(uri, HttpMethod.POST, Void.TYPE, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(Void.TYPE);

			assertEquals("Could not get any response from: https://localhost:12345/test", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest6ClassOk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(String.class)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, String.class, null, null, Map.of());

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(String.class);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest4ClassPayloadOk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final RequestHeadersSpec headersSpecMock = Mockito.mock(RequestHeadersSpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.bodyValue("payload")).thenReturn(headersSpecMock);
			when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(String.class)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, String.class, "payload");

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).bodyValue("payload");
			verify(headersSpecMock).retrieve();
			verify(responseSpecMock).bodyToMono(String.class);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest4ClassSslContextOk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(String.class)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, String.class, (SslContext) null);

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(String.class);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest4ClassCustomHeadersOk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(String.class)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, Map.of(), String.class);

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(String.class);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest3ClassOk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(String.class)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, String.class);

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(String.class);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSendRequest6PTRHttpMethodNull() {
		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> service.sendRequest(uri, null, ptr, "payload", null, Map.of()));

		assertEquals("Request method is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSendRequest6PTRUriNull() {
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};

		final Throwable ex = assertThrows(NullPointerException.class,
				() -> service.sendRequest(null, HttpMethod.POST, ptr, "payload", null, Map.of()));

		assertEquals("HttpService.sendRequest method received null URI", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSendRequest6PTRNotSupportedMethod() {
		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};

		final Throwable ex = assertThrows(MethodNotFoundException.class,
				() -> service.sendRequest(uri, HttpMethod.TRACE, ptr, "payload", null, Map.of()));

		assertEquals("Invalid method type was given to the HttpService.sendRequest() method", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSendRequest6PTRSecureRequestInsecureMode() {
		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> service.sendRequest(uri, HttpMethod.POST, ptr, "payload", null, Map.of()));

		assertEquals("SSL Context is not set, but secure request sending was invoked. An insecure application may not send requests to secure servers", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6PTRInsecureBadRequest() {
		final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "httpClient", httpClientMock);

		final UriComponents uri = HttpUtilities.createURI("http", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};
		final ErrorMessageDTO error = new ErrorMessageDTO("test", 400, ExceptionType.INVALID_PARAMETER, "origin");
		final String errorBody = Utilities.toJson(error);

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenThrow(new WebClientResponseException(400, "Bad Request", null, errorBody.getBytes(), null));

			final Throwable ex = assertThrows(InvalidParameterException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, ptr, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);

			assertEquals("test", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest6PTRSecureNotStandardError() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final RequestHeadersSpec headersSpecMock = Mockito.mock(RequestHeadersSpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.bodyValue("payload")).thenReturn(headersSpecMock);
			when(headersSpecMock.header("Authorization", new String[] { "Bearer SYSTEM//RequesterSystem" })).thenReturn(headersSpecMock);
			when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenThrow(new WebClientResponseException(400, "Bad Request", null, "".getBytes(), null));

			final Throwable ex = assertThrows(ArrowheadException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, ptr, "payload", null, Map.of("Authorization", "Bearer SYSTEM//RequesterSystem")));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).bodyValue("payload");
			verify(headersSpecMock).header("Authorization", new String[] { "Bearer SYSTEM//RequesterSystem" });
			verify(headersSpecMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);

			assertEquals("Bad Request, status code: 400", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked" })
	@Test
	public void testSendRequest6PTRSecureNotStandardError2() throws KeyManagementException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final SslContext sslContext = createSSLContext();
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};

		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class);
				MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(httpClientMock.secure(any(Consumer.class))).thenReturn(httpClientMock);
			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenThrow(new WebClientResponseException(400, "Bad Request", null, null, null));

			final Throwable ex = assertThrows(ArrowheadException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, ptr, null, sslContext, Map.of()));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock).secure(any(Consumer.class));
			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);

			assertEquals("Bad Request, status code: 400", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6PTRSecurePKIXPathError() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};
		final Exception cause = new Exception("PKIX path building failed:");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(ForbiddenException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, ptr, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);

			assertEquals("The system at https://localhost:12345/test is not part of the same certificate chain of trust", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6PTRSecureNoSAN() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};
		final Exception cause = new Exception("No subject alternative names are found");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(AuthException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, ptr, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);

			assertEquals("The certificate of the system at https://localhost:12345/test does not contain the specified IP address or DNS name as a Subject Alternative Name", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6PTRSecureNoMatchingName() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};
		final Exception cause = new Exception("No name matching is found");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(AuthException.class,
					() -> service.sendRequest(uri, HttpMethod.POST, ptr, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);

			assertEquals("The certificate of the system at https://localhost:12345/test does not contain the specified IP address or DNS name as a Subject Alternative Name", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6PTRSecureExternalError1() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};
		final Exception cause = new Exception("something unexpected");

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(ExternalServerError.class,
					() -> service.sendRequest(uri, HttpMethod.POST, ptr, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);

			assertEquals("Could not get any response from: https://localhost:12345/test", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6PTRSecureExternalError2() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};
		final Exception cause = new Exception((String) null);

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenThrow(new RuntimeException("runtime test", cause));

			final Throwable ex = assertThrows(ExternalServerError.class,
					() -> service.sendRequest(uri, HttpMethod.POST, ptr, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);

			assertEquals("Could not get any response from: https://localhost:12345/test", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber" })
	@Test
	public void testSendRequest6PTRSecureExternalError3() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<Void> ptr = new ParameterizedTypeReference<Void>() {
		};

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenThrow(new RuntimeException("runtime test"));

			final Throwable ex = assertThrows(ExternalServerError.class,
					() -> service.sendRequest(uri, HttpMethod.POST, ptr, null, null, Map.of()));

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);

			assertEquals("Could not get any response from: https://localhost:12345/test", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest6PTROk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<String> ptr = new ParameterizedTypeReference<String>() {
		};

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, ptr, null, null, Map.of());

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest4PTRPayloadOk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<String> ptr = new ParameterizedTypeReference<String>() {
		};

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final RequestHeadersSpec headersSpecMock = Mockito.mock(RequestHeadersSpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.bodyValue("payload")).thenReturn(headersSpecMock);
			when(headersSpecMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, ptr, "payload");

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).bodyValue("payload");
			verify(headersSpecMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest4PTRSslContextOk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<String> ptr = new ParameterizedTypeReference<String>() {
		};

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, ptr, (SslContext) null);

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest4PTRCustomHeadersOk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<String> ptr = new ParameterizedTypeReference<String>() {
		};

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, Map.of(), ptr);

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked", "rawtypes" })
	@Test
	public void testSendRequest3PTROk() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		final UriComponents uri = HttpUtilities.createURI("https", "localhost", 12345, "/test");
		final ParameterizedTypeReference<String> ptr = new ParameterizedTypeReference<String>() {
		};

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			final RequestBodyUriSpec uriSpecMock = Mockito.mock(RequestBodyUriSpec.class);
			final RequestBodySpec specMock = Mockito.mock(RequestBodySpec.class);
			final ResponseSpec responseSpecMock = Mockito.mock(ResponseSpec.class);
			final Mono monoMock = Mockito.mock(Mono.class);

			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);
			when(webClientMock.method(HttpMethod.POST)).thenReturn(uriSpecMock);
			when(uriSpecMock.uri(any(URI.class))).thenReturn(specMock);
			when(specMock.retrieve()).thenReturn(responseSpecMock);
			when(responseSpecMock.bodyToMono(ptr)).thenReturn(monoMock);
			when(monoMock.block()).thenReturn("result");

			final String result = service.sendRequest(uri, HttpMethod.POST, ptr);

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();
			verify(webClientMock).method(HttpMethod.POST);
			verify(uriSpecMock).uri(any(URI.class));
			verify(specMock).retrieve();
			verify(responseSpecMock).bodyToMono(ptr);
			verify(monoMock).block();

			assertEquals("result", result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:nowhitespaceafter")
	@Test
	public void testCreateInsecureWebClient() {
		final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "httpClient", httpClientMock);

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);

			final WebClient result = service.createInsecureWebClient();

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();

			assertEquals(webClientMock, result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:nowhitespaceafter")
	@Test
	public void testCreateSecureWebClient() {
		final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
		ReflectionTestUtils.setField(service, "sslClient", sslClientMock);

		try (MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);
			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);

			final WebClient result = service.createSecureWebClient();

			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();

			assertEquals(webClientMock, result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testCreateSecureWebClient1() throws KeyManagementException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final SslContext sslContext = createSSLContext();

		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class);
				MockedStatic<WebClient> webClientStaticMock = Mockito.mockStatic(WebClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final WebClient webClientMock = Mockito.mock(WebClient.class);
			final WebClient.Builder webClientBuilderMock = Mockito.mock(WebClient.Builder.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(httpClientMock.secure(any(Consumer.class))).thenReturn(httpClientMock);
			webClientStaticMock.when(() -> WebClient.builder()).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.clientConnector(any(ClientHttpConnector.class))).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("accept", new String[] { "text/plain", "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.defaultHeader("content-type", new String[] { "application/json" })).thenReturn(webClientBuilderMock);
			when(webClientBuilderMock.build()).thenReturn(webClientMock);

			final WebClient result = service.createSecureWebClient(sslContext);

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock).secure(any(Consumer.class));
			webClientStaticMock.verify(() -> WebClient.builder());
			verify(webClientBuilderMock).clientConnector(any(ClientHttpConnector.class));
			verify(webClientBuilderMock).defaultHeader("accept", new String[] { "text/plain", "application/json" });
			verify(webClientBuilderMock).defaultHeader("content-type", new String[] { "application/json" });
			verify(webClientBuilderMock).build();

			assertEquals(webClientMock, result);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitNoSSL() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(false);

			assertNull(ReflectionTestUtils.getField(service, "httpClient"));
			ReflectionTestUtils.invokeMethod(service, "init");

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();

			assertEquals(httpClientMock, ReflectionTestUtils.getField(service, "httpClient"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitSSLKeyStoreTypeEmpty() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("");

			final Throwable ex = assertThrows(IllegalArgumentException.class,
					() -> ReflectionTestUtils.invokeMethod(service, "init"));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties).getKeyStoreType();

			assertEquals("server.ssl.key-store-type is not defined", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitSSLKeyStoreNull() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
			when(sslProperties.getKeyStore()).thenReturn(null);

			final Throwable ex = assertThrows(IllegalArgumentException.class,
					() -> ReflectionTestUtils.invokeMethod(service, "init"));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties).getKeyStoreType();
			verify(sslProperties).getKeyStore();

			assertEquals("server.ssl.key-store is not defined", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitSSLKeyStoreNotExists() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final Resource keyStoreMock = Mockito.mock(Resource.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
			when(sslProperties.getKeyStore()).thenReturn(keyStoreMock);
			when(keyStoreMock.exists()).thenReturn(false);

			final Throwable ex = assertThrows(IllegalArgumentException.class,
					() -> ReflectionTestUtils.invokeMethod(service, "init"));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties).getKeyStoreType();
			verify(sslProperties, times(2)).getKeyStore();
			verify(keyStoreMock).exists();

			assertEquals("server.ssl.key-store file is not found", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitSSLKeyStorePasswordNull() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final Resource keyStoreMock = Mockito.mock(Resource.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
			when(sslProperties.getKeyStore()).thenReturn(keyStoreMock);
			when(keyStoreMock.exists()).thenReturn(true);
			when(sslProperties.getKeyStorePassword()).thenReturn(null);

			final Throwable ex = assertThrows(IllegalArgumentException.class,
					() -> ReflectionTestUtils.invokeMethod(service, "init"));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties).getKeyStoreType();
			verify(sslProperties, times(2)).getKeyStore();
			verify(keyStoreMock).exists();
			verify(sslProperties).getKeyStorePassword();

			assertEquals("server.ssl.key-store-password is not defined", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitSSLKeyPasswordNull() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final Resource keyStoreMock = Mockito.mock(Resource.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
			when(sslProperties.getKeyStore()).thenReturn(keyStoreMock);
			when(keyStoreMock.exists()).thenReturn(true);
			when(sslProperties.getKeyStorePassword()).thenReturn("123456");
			when(sslProperties.getKeyPassword()).thenReturn(null);

			final Throwable ex = assertThrows(IllegalArgumentException.class,
					() -> ReflectionTestUtils.invokeMethod(service, "init"));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties).getKeyStoreType();
			verify(sslProperties, times(2)).getKeyStore();
			verify(keyStoreMock).exists();
			verify(sslProperties).getKeyStorePassword();
			verify(sslProperties).getKeyPassword();

			assertEquals("server.ssl.key-password is not defined", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitSSLTrustStoreNull() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final Resource keyStoreMock = Mockito.mock(Resource.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
			when(sslProperties.getKeyStore()).thenReturn(keyStoreMock);
			when(keyStoreMock.exists()).thenReturn(true);
			when(sslProperties.getKeyStorePassword()).thenReturn("123456");
			when(sslProperties.getKeyPassword()).thenReturn("123456");
			when(sslProperties.getTrustStore()).thenReturn(null);

			final Throwable ex = assertThrows(IllegalArgumentException.class,
					() -> ReflectionTestUtils.invokeMethod(service, "init"));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties).getKeyStoreType();
			verify(sslProperties, times(2)).getKeyStore();
			verify(keyStoreMock).exists();
			verify(sslProperties).getKeyStorePassword();
			verify(sslProperties).getKeyPassword();
			verify(sslProperties).getTrustStore();

			assertEquals("server.ssl.trust-store is not defined", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitSSLTrustStoreNotExists() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final Resource keyStoreMock = Mockito.mock(Resource.class);
			final Resource trustStoreMock = Mockito.mock(Resource.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
			when(sslProperties.getKeyStore()).thenReturn(keyStoreMock);
			when(keyStoreMock.exists()).thenReturn(true);
			when(sslProperties.getKeyStorePassword()).thenReturn("123456");
			when(sslProperties.getKeyPassword()).thenReturn("123456");
			when(sslProperties.getTrustStore()).thenReturn(trustStoreMock);
			when(trustStoreMock.exists()).thenReturn(false);

			final Throwable ex = assertThrows(IllegalArgumentException.class,
					() -> ReflectionTestUtils.invokeMethod(service, "init"));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties).getKeyStoreType();
			verify(sslProperties, times(2)).getKeyStore();
			verify(keyStoreMock).exists();
			verify(sslProperties).getKeyStorePassword();
			verify(sslProperties).getKeyPassword();
			verify(sslProperties, times(2)).getTrustStore();
			verify(trustStoreMock).exists();

			assertEquals("server.ssl.trust-store file is not found", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitSSLTrustStorePasswordNull() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final Resource keyStoreMock = Mockito.mock(Resource.class);
			final Resource trustStoreMock = Mockito.mock(Resource.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
			when(sslProperties.getKeyStore()).thenReturn(keyStoreMock);
			when(keyStoreMock.exists()).thenReturn(true);
			when(sslProperties.getKeyStorePassword()).thenReturn("123456");
			when(sslProperties.getKeyPassword()).thenReturn("123456");
			when(sslProperties.getTrustStore()).thenReturn(trustStoreMock);
			when(trustStoreMock.exists()).thenReturn(true);
			when(sslProperties.getTrustStorePassword()).thenReturn(null);

			final Throwable ex = assertThrows(IllegalArgumentException.class,
					() -> ReflectionTestUtils.invokeMethod(service, "init"));

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties).getKeyStoreType();
			verify(sslProperties, times(2)).getKeyStore();
			verify(keyStoreMock).exists();
			verify(sslProperties).getKeyStorePassword();
			verify(sslProperties).getKeyPassword();
			verify(sslProperties, times(2)).getTrustStore();
			verify(trustStoreMock).exists();
			verify(sslProperties).getTrustStorePassword();

			assertEquals("server.ssl.trust-store-password is not defined", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "unchecked" })
	@Test
	public void testInitSSLKeyStoreException() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final Resource keyStoreMock = Mockito.mock(Resource.class);
			final Resource trustStoreMock = Mockito.mock(Resource.class);

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
			when(sslProperties.getKeyStore()).thenReturn(keyStoreMock);
			when(keyStoreMock.exists()).thenReturn(true);
			when(sslProperties.getKeyStorePassword()).thenReturn("123456");
			when(sslProperties.getKeyPassword()).thenReturn("123456");
			when(sslProperties.getTrustStore()).thenReturn(trustStoreMock);
			when(trustStoreMock.exists()).thenReturn(true);
			when(sslProperties.getTrustStorePassword()).thenReturn("123456");

			try (MockedStatic<KeyStore> staticMock = Mockito.mockStatic(KeyStore.class)) {
				staticMock.when(() -> KeyStore.getInstance("pkcs12")).thenThrow(new KeyStoreException("test key store exception"));

				final Throwable ex = assertThrows(UndeclaredThrowableException.class,
						() -> ReflectionTestUtils.invokeMethod(service, "init"));

				staticMock.verify(() -> KeyStore.getInstance("pkcs12"));

				assertEquals("test key store exception", ex.getCause().getMessage());
			}

			httpClientStaticMock.verify(() -> HttpClient.create());
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties, times(2)).getKeyStoreType();
			verify(sslProperties, times(2)).getKeyStore();
			verify(keyStoreMock).exists();
			verify(sslProperties).getKeyStorePassword();
			verify(sslProperties).getKeyPassword();
			verify(sslProperties, times(2)).getTrustStore();
			verify(trustStoreMock).exists();
			verify(sslProperties).getTrustStorePassword();

		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "checkstyle:nowhitespaceafter", "checkstyle:MagicNumber", "unchecked" })
	@Test
	public void testInitSSLOk() {
		try (MockedStatic<HttpClient> httpClientStaticMock = Mockito.mockStatic(HttpClient.class)) {
			final HttpClient httpClientMock = Mockito.mock(HttpClient.class);
			final HttpClient sslClientMock = Mockito.mock(HttpClient.class);
			final Resource keyStore = new ClassPathResource("certs/ConsumerAuthorization.p12");
			final Resource trustStore = new ClassPathResource("certs/truststore.p12");

			httpClientStaticMock.when(() -> HttpClient.create()).thenReturn(httpClientMock, sslClientMock);
			when(httpClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(httpClientMock);
			when(httpClientMock.doOnConnected(any(Consumer.class))).thenReturn(httpClientMock);
			when(sslProperties.isSslEnabled()).thenReturn(true);
			when(sslProperties.getKeyStoreType()).thenReturn("pkcs12");
			when(sslProperties.getKeyStore()).thenReturn(keyStore);
			when(sslProperties.getKeyStorePassword()).thenReturn("123456");
			when(sslProperties.getKeyPassword()).thenReturn("123456");
			when(sslProperties.getTrustStore()).thenReturn(trustStore);
			when(sslProperties.getTrustStorePassword()).thenReturn("123456");
			when(sslClientMock.option(any(ChannelOption.class), anyInt())).thenReturn(sslClientMock);
			when(sslClientMock.doOnConnected(any(Consumer.class))).thenReturn(sslClientMock);
			when(sslClientMock.secure(any(Consumer.class))).thenReturn(sslClientMock);

			assertNull(ReflectionTestUtils.getField(service, "httpClient"));
			assertNull(ReflectionTestUtils.getField(service, "sslClient"));
			ReflectionTestUtils.invokeMethod(service, "init");

			httpClientStaticMock.verify(() -> HttpClient.create(), times(2));
			verify(httpClientMock).option(any(ChannelOption.class), anyInt());
			verify(httpClientMock).doOnConnected(any(Consumer.class));
			verify(httpClientMock, never()).secure(any(Consumer.class));
			verify(sslProperties).isSslEnabled();
			verify(sslProperties, times(4)).getKeyStoreType();
			verify(sslProperties, times(3)).getKeyStore();
			verify(sslProperties, times(3)).getKeyStorePassword();
			verify(sslProperties).getKeyPassword();
			verify(sslProperties, times(3)).getTrustStore();
			verify(sslProperties, times(2)).getTrustStorePassword();
			verify(sslClientMock).option(any(ChannelOption.class), anyInt());
			verify(sslClientMock).doOnConnected(any(Consumer.class));
			verify(sslClientMock).secure(any(Consumer.class));

			assertEquals(httpClientMock, ReflectionTestUtils.getField(service, "httpClient"));
			assertEquals(sslClientMock, ReflectionTestUtils.getField(service, "sslClient"));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testInitConnectionHandlers() {
		ReflectionTestUtils.setField(service, "socketTimeout", 12345);
		final Connection connMock = Mockito.mock(Connection.class);

		when(connMock.addHandlerLast(any(ReadTimeoutHandler.class))).thenReturn(connMock);
		when(connMock.addHandlerLast(any(WriteTimeoutHandler.class))).thenReturn(connMock);

		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "initConnectionHandlers", connMock));

		final ArgumentCaptor<ReadTimeoutHandler> readCaptor = ArgumentCaptor.forClass(ReadTimeoutHandler.class);
		final ArgumentCaptor<WriteTimeoutHandler> writeCaptor = ArgumentCaptor.forClass(WriteTimeoutHandler.class);

		verify(connMock).addHandlerLast(readCaptor.capture());
		verify(connMock).addHandlerLast(writeCaptor.capture());

		final long writeTimeOut = TimeUnit.NANOSECONDS.toMillis((long) ReflectionTestUtils.getField(writeCaptor.getValue(), "timeoutNanos"));
		assertEquals(12345, readCaptor.getValue().getReaderIdleTimeInMillis());
		assertEquals(12345, writeTimeOut);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testInitSecuritySettings() throws KeyManagementException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final SslContextSpec specMock = Mockito.mock(SslContextSpec.class);
		final SslContext context = createSSLContext();
		final SslProvider.Builder builderMock = Mockito.mock(SslProvider.Builder.class);

		when(specMock.sslContext(context)).thenReturn(builderMock);
		when(builderMock.handlerConfigurator(any(Consumer.class))).thenReturn(builderMock);

		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "initSecuritySettings", specMock, context));

		verify(specMock).sslContext(context);
		verify(builderMock).handlerConfigurator(any(Consumer.class));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitSSLHandlerNoDisableHostnameVerifier() {
		final SslHandler sslHandlerMock = Mockito.mock(SslHandler.class);
		final SSLEngine engineMock = Mockito.mock(SSLEngine.class);
		final SSLParameters paramsMock = Mockito.mock(SSLParameters.class);

		when(sslHandlerMock.engine()).thenReturn(engineMock);
		when(engineMock.getSSLParameters()).thenReturn(paramsMock);
		doNothing().when(paramsMock).setEndpointIdentificationAlgorithm("https");
		doNothing().when(engineMock).setSSLParameters(paramsMock);

		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "initSSLHandler", sslHandlerMock));

		verify(sslHandlerMock).engine();
		verify(engineMock).getSSLParameters();
		verify(paramsMock).setEndpointIdentificationAlgorithm("https");
		verify(paramsMock, never()).setEndpointIdentificationAlgorithm("");
		verify(engineMock).setSSLParameters(paramsMock);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testInitSSLHandlerDisableHostnameVerifier() {
		ReflectionTestUtils.setField(service, "disableHostnameVerifier", true);

		final SslHandler sslHandlerMock = Mockito.mock(SslHandler.class);
		final SSLEngine engineMock = Mockito.mock(SSLEngine.class);
		final SSLParameters paramsMock = Mockito.mock(SSLParameters.class);

		when(sslHandlerMock.engine()).thenReturn(engineMock);
		when(engineMock.getSSLParameters()).thenReturn(paramsMock);
		doNothing().when(paramsMock).setEndpointIdentificationAlgorithm("https");
		doNothing().when(paramsMock).setEndpointIdentificationAlgorithm("");
		doNothing().when(engineMock).setSSLParameters(paramsMock);

		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "initSSLHandler", sslHandlerMock));

		verify(sslHandlerMock).engine();
		verify(engineMock).getSSLParameters();
		verify(paramsMock).setEndpointIdentificationAlgorithm("https");
		verify(paramsMock).setEndpointIdentificationAlgorithm("");
		verify(engineMock).setSSLParameters(paramsMock);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:nowhitespaceafter")
	@Test
	public void testGetStatusCodeAsStringNullInput() {
		final String result = ReflectionTestUtils.invokeMethod(service, "getStatusCodeAsString", new Object[] { null });

		assertEquals("<unknown>", result);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private SslContext createSSLContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {

		final Resource keyStoreResource = new ClassPathResource("certs/ConsumerAuthorization.p12");
		final KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(keyStoreResource.getInputStream(), "123456".toCharArray());
		final String kmfAlgorithm = System.getProperty("ssl.KeyManagerFactory.algorithm", KeyManagerFactory.getDefaultAlgorithm());
		final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm);
		keyManagerFactory.init(keyStore, "123456".toCharArray());

		final Resource trustStoreResource = new ClassPathResource("certs/truststore.p12");
		final KeyStore trustStore = KeyStore.getInstance("pkcs12");
		trustStore.load(trustStoreResource.getInputStream(), "123456".toCharArray());
		final String tmfAlgorithm = System.getProperty("ssl.TrustManagerFactory.algorithm", TrustManagerFactory.getDefaultAlgorithm());
		final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
		trustManagerFactory.init(trustStore);

		return SslContextBuilder.forClient()
				.keyStoreType("pkcs12")
				.keyManager(keyManagerFactory)
				.trustManager(trustManagerFactory)
				.build();
	}
}