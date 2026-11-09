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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponents;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ExternalServerError;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.dto.ErrorMessageDTO;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import jakarta.el.MethodNotFoundException;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.SslProvider.SslContextSpec;

@Component
public class HttpService {

	//=================================================================================================
	// members

	private static final String ERROR_MESSAGE_PART_PKIX_PATH = "PKIX path";
	private static final String ERROR_MESSAGE_PART_SUBJECT_ALTERNATIVE_NAMES = "No subject alternative";
	private static final String ERROR_MESSAGE_PART_X509_NAME = "No name matching";
	private static final List<HttpMethod> NOT_SUPPORTED_METHODS = List.of(HttpMethod.HEAD, HttpMethod.OPTIONS, HttpMethod.TRACE);
	private static final String SSL_KEY_MANAGER_FACTORY_ALGORITHM = "ssl.KeyManagerFactory.algorithm";
	private static final String SSL_TRUST_MANAGER_FACTORY_ALGORITHM = "ssl.TrustManagerFactory.algorithm";

	private final Logger logger = LogManager.getLogger(HttpService.class);

	@Value(Constants.$DISABLE_HOSTNAME_VERIFIER_WD)
	private boolean disableHostnameVerifier;

	@Value(Constants.$HTTP_CLIENT_CONNECTION_TIMEOUT_WD)
	private int connectionTimeout;

	@Value(Constants.$HTTP_CLIENT_SOCKET_TIMEOUT_WD)
	private int socketTimeout;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private SSLProperties sslProperties;

	private HttpClient httpClient;
	private HttpClient sslClient;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public <T, P> T sendRequest(
			final UriComponents uri,
			final HttpMethod method,
			final Class<T> responseType,
			final P payload,
			final SslContext givenContext,
			final Map<String, String> customHeaders) {
		logger.debug("sendRequest started...");
		Assert.notNull(method, "Request method is not defined");
		logger.debug("Sending {} request to: {}", method, uri);

		if (uri == null) {
			logger.error("sendRequest() is called with null URI.");
			throw new NullPointerException("HttpService.sendRequest method received null URI");
		}

		if (NOT_SUPPORTED_METHODS.contains(method)) {
			throw new MethodNotFoundException("Invalid method type was given to the HttpService.sendRequest() method");
		}

		final boolean secure = Constants.HTTPS.equalsIgnoreCase(uri.getScheme());
		if (secure && sslClient == null) {
			logger.debug("sendRequest(): secure request sending was invoked in insecure mode");
			throw new ForbiddenException("SSL Context is not set, but secure request sending was invoked. An insecure application may not send requests to secure servers");
		}

		HttpClient usedClient;
		if (secure) {
			usedClient = givenContext != null ? createHttpClient(givenContext) : sslClient;
		} else {
			usedClient = httpClient;
		}

		try {
			final WebClient client = createWebClient(usedClient);
			final RequestBodySpec spec = client
					.method(method)
					.uri(uri.toUri());

			RequestHeadersSpec<?> headersSpec = (payload != null) ? spec.bodyValue(payload) : spec;

			if (!Utilities.isEmpty(customHeaders)) {
				for (final Entry<String, String> header : customHeaders.entrySet()) {
					headersSpec = headersSpec.header(header.getKey(), header.getValue());
				}
			}

			return headersSpec
					.retrieve()
					.bodyToMono(responseType)
					.block();
		} catch (final WebClientResponseException ex) {
			throw convertWebClientException(ex, uri.toString());
		} catch (final Exception ex) {
			if (ex.getCause() != null) {
				final Throwable throwable = ex.getCause();
				final String message = throwable.getMessage();
				if (message != null && message.contains(ERROR_MESSAGE_PART_PKIX_PATH)) {
					logger.error("The system at {} is not part of the same certificate chain of trust", uri.toUriString());
					logger.debug("Exception:", throwable);
					throw new ForbiddenException("The system at " + uri.toUriString() + " is not part of the same certificate chain of trust");
				} else if (message != null && (message.contains(ERROR_MESSAGE_PART_SUBJECT_ALTERNATIVE_NAMES) || message.contains(ERROR_MESSAGE_PART_X509_NAME))) {
					logger.error("The certificate of the system at {} does not contain the specified IP address or DNS name as a Subject Alternative Name", uri.toString());
					logger.debug("Exception: ", throwable);
					throw new AuthException("The certificate of the system at " + uri.toString() + " does not contain the specified IP address or DNS name as a Subject Alternative Name");
				}

				logger.error("Service unavailable at {}", uri.toUriString());
				logger.debug("Exception:", throwable);
				throw new ExternalServerError("Could not get any response from: " + uri.toUriString());
			}

			logger.error("Service unavailable at {}", uri.toUriString());
			logger.debug("Exception", ex);
			throw new ExternalServerError("Could not get any response from: " + uri.toUriString());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public <T, P> T sendRequest(final UriComponents uri, final HttpMethod method, final Class<T> responseType, final P payload) {
		return sendRequest(uri, method, responseType, payload, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T sendRequest(final UriComponents uri, final HttpMethod method, final Class<T> responseType, final SslContext givenContext) {
		return sendRequest(uri, method, responseType, null, givenContext, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T sendRequest(final UriComponents uri, final HttpMethod method, final Map<String, String> customHeaders, final Class<T> responseType) {
		return sendRequest(uri, method, responseType, null, null, customHeaders);
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T sendRequest(final UriComponents uri, final HttpMethod method, final Class<T> responseType) {
		return sendRequest(uri, method, responseType, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T, P> T sendRequest(
			final UriComponents uri,
			final HttpMethod method,
			final ParameterizedTypeReference<T> responseType,
			final P payload,
			final SslContext givenContext,
			final Map<String, String> customHeaders) {
		logger.debug("sendRequest started...");
		Assert.notNull(method, "Request method is not defined");
		logger.debug("Sending {} request to: {}", method, uri);

		if (uri == null) {
			logger.error("sendRequest() is called with null URI");
			throw new NullPointerException("HttpService.sendRequest method received null URI");
		}

		if (NOT_SUPPORTED_METHODS.contains(method)) {
			throw new MethodNotFoundException("Invalid method type was given to the HttpService.sendRequest() method");
		}

		final boolean secure = Constants.HTTPS.equalsIgnoreCase(uri.getScheme());
		if (secure && sslClient == null) {
			logger.debug("sendRequest(): secure request sending was invoked in insecure mode");
			throw new ForbiddenException("SSL Context is not set, but secure request sending was invoked. An insecure application may not send requests to secure servers");
		}

		HttpClient usedClient;
		if (secure) {
			usedClient = givenContext != null ? createHttpClient(givenContext) : sslClient;
		} else {
			usedClient = httpClient;
		}

		try {
			final WebClient client = createWebClient(usedClient);
			final RequestBodySpec spec = client
					.method(method)
					.uri(uri.toUri());

			RequestHeadersSpec<?> headersSpec = (payload != null) ? spec.bodyValue(payload) : spec;

			if (!Utilities.isEmpty(customHeaders)) {
				for (final Entry<String, String> header : customHeaders.entrySet()) {
					headersSpec = headersSpec.header(header.getKey(), header.getValue());
				}
			}

			return headersSpec
					.retrieve()
					.bodyToMono(responseType)
					.block();
		} catch (final WebClientResponseException ex) {
			throw convertWebClientException(ex, uri.toString());
		} catch (final Exception ex) {
			if (ex.getCause() != null) {
				final Throwable throwable = ex.getCause();
				final String message = throwable.getMessage();
				if (message != null && message.contains(ERROR_MESSAGE_PART_PKIX_PATH)) {
					logger.error("The system at {} is not part of the same certificate chain of trust", uri.toUriString());
					logger.debug("Exception:", throwable);
					throw new ForbiddenException("The system at " + uri.toUriString() + " is not part of the same certificate chain of trust");
				} else if (message != null && (message.contains(ERROR_MESSAGE_PART_SUBJECT_ALTERNATIVE_NAMES) || message.contains(ERROR_MESSAGE_PART_X509_NAME))) {
					logger.error("The certificate of the system at {} does not contain the specified IP address or DNS name as a Subject Alternative Name", uri.toString());
					logger.debug("Exception:", throwable);
					throw new AuthException("The certificate of the system at " + uri.toString() + " does not contain the specified IP address or DNS name as a Subject Alternative Name");
				}

				logger.error("Service unavailable at {}", uri.toUriString());
				logger.debug("Exception:", throwable);
				throw new ExternalServerError("Could not get any response from: " + uri.toUriString());
			}

			logger.error("Service unavailable at {}", uri.toUriString());
			logger.debug("Exception", ex);
			throw new ExternalServerError("Could not get any response from: " + uri.toUriString());
		}
	}

	//-------------------------------------------------------------------------------------------------
	public <T, P> T sendRequest(final UriComponents uri, final HttpMethod method, final ParameterizedTypeReference<T> responseType, final P payload) {
		return sendRequest(uri, method, responseType, payload, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T sendRequest(final UriComponents uri, final HttpMethod method, final ParameterizedTypeReference<T> responseType, final SslContext givenContext) {
		return sendRequest(uri, method, responseType, null, givenContext, null);
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T sendRequest(final UriComponents uri, final HttpMethod method, final Map<String, String> customHeaders, final ParameterizedTypeReference<T> responseType) {
		return sendRequest(uri, method, responseType, null, null, customHeaders);
	}

	//-------------------------------------------------------------------------------------------------
	public <T> T sendRequest(final UriComponents uri, final HttpMethod method, final ParameterizedTypeReference<T> responseType) {
		return sendRequest(uri, method, responseType, null, null, null);
	}

	//-------------------------------------------------------------------------------------------------
	public WebClient createInsecureWebClient() {
		logger.debug("createInsecureWebClient started...");

		return createWebClient(httpClient);
	}

	//-------------------------------------------------------------------------------------------------
	public WebClient createSecureWebClient() {
		logger.debug("createSecureWebClient started...");

		return createWebClient(sslClient);
	}

	//-------------------------------------------------------------------------------------------------
	public WebClient createSecureWebClient(final SslContext sslContext) {
		logger.debug("createSecureWebClient started...");

		final HttpClient client = createHttpClient(sslContext);
		return createWebClient(client);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() throws Exception {
		logger.debug("Initializing HttpService...");

		httpClient = createHttpClient(null);

		if (sslProperties.isSslEnabled()) {
			SslContext sslContext;

			try {
				sslContext = createSSLContext();
			} catch (final KeyManagementException | UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | IllegalArgumentException ex) {
				// it's initialization so we just logging the exception then let the application die
				logger.error("Error while creating SSL context: {}", ex.getMessage());
				logger.debug("Exception", ex);
				throw ex;
			}

			sslClient = createHttpClient(sslContext);
		}

		logger.debug("HttpService is initialized");
	}

	//-------------------------------------------------------------------------------------------------
	private HttpClient createHttpClient(final SslContext sslContext) {
		HttpClient client = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
				.doOnConnected(this::initConnectionHandlers);

		if (sslContext != null) {
			client = client.secure(t -> this.initSecuritySettings(t, sslContext));
		}

		return client;
	}

	//-------------------------------------------------------------------------------------------------
	private void initConnectionHandlers(final Connection connection) {
		connection.addHandlerLast(new ReadTimeoutHandler(socketTimeout, TimeUnit.MILLISECONDS));
		connection.addHandlerLast(new WriteTimeoutHandler(socketTimeout, TimeUnit.MILLISECONDS));
	}

	//-------------------------------------------------------------------------------------------------
	private SslProvider.Builder initSecuritySettings(final SslContextSpec spec, final SslContext context) {
		return spec.sslContext(context)
				.handlerConfigurator(this::initSSLHandler);
	}

	//-------------------------------------------------------------------------------------------------
	private void initSSLHandler(final SslHandler handler) {
		final SSLEngine sslEngine = handler.engine();
		final SSLParameters sslParameters = sslEngine.getSSLParameters();
		sslParameters.setEndpointIdentificationAlgorithm(Constants.HTTPS);

		if (disableHostnameVerifier) {
			sslParameters.setEndpointIdentificationAlgorithm("");
			// see: https://stackoverflow.com/a/67964695
		}

		sslEngine.setSSLParameters(sslParameters);
	}

	//-------------------------------------------------------------------------------------------------
	private WebClient createWebClient(final HttpClient client) {
		final Builder builder = WebClient
				.builder()
				.clientConnector(new ReactorClientHttpConnector(client))
				.defaultHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON_VALUE);

		return builder.build();
	}

	//-------------------------------------------------------------------------------------------------
	private SslContext createSSLContext() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, KeyManagementException, UnrecoverableKeyException {
		logger.debug("createSSLContext started...");

		final String messageNotDefined = " is not defined";
		Assert.isTrue(!Utilities.isEmpty(sslProperties.getKeyStoreType()), Constants.SERVER_SSL_KEY__STORE__TYPE + messageNotDefined);
		Assert.notNull(sslProperties.getKeyStore(), Constants.SERVER_SSL_KEY__STORE + messageNotDefined);
		Assert.isTrue(sslProperties.getKeyStore().exists(), Constants.SERVER_SSL_KEY__STORE + " file is not found");
		Assert.notNull(sslProperties.getKeyStorePassword(), Constants.SERVER_SSL_KEY__STORE__PASSWORD + messageNotDefined);
		Assert.notNull(sslProperties.getKeyPassword(), Constants.SERVER_SSL_KEY__PASSWORD + messageNotDefined);
		Assert.notNull(sslProperties.getTrustStore(), Constants.SERVER_SSL_TRUST__STORE + messageNotDefined);
		Assert.isTrue(sslProperties.getTrustStore().exists(), Constants.SERVER_SSL_TRUST__STORE + " file is not found");
		Assert.notNull(sslProperties.getTrustStorePassword(), Constants.SERVER_SSL_TRUST__STORE__PASSWORD + messageNotDefined);

		final KeyStore keyStore = KeyStore.getInstance(sslProperties.getKeyStoreType());
		keyStore.load(sslProperties.getKeyStore().getInputStream(), sslProperties.getKeyStorePassword().toCharArray());
		final String kmfAlgorithm = System.getProperty(SSL_KEY_MANAGER_FACTORY_ALGORITHM, KeyManagerFactory.getDefaultAlgorithm());
		final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(kmfAlgorithm);
		keyManagerFactory.init(keyStore, sslProperties.getKeyStorePassword().toCharArray());

		final KeyStore trustStore = KeyStore.getInstance(sslProperties.getKeyStoreType());
		trustStore.load(sslProperties.getTrustStore().getInputStream(), sslProperties.getTrustStorePassword().toCharArray());
		final String tmfAlgorithm = System.getProperty(SSL_TRUST_MANAGER_FACTORY_ALGORITHM, TrustManagerFactory.getDefaultAlgorithm());
		final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm);
		trustManagerFactory.init(trustStore);

		return SslContextBuilder.forClient()
				.keyStoreType(sslProperties.getKeyStoreType())
				.keyManager(keyManagerFactory)
				.trustManager(trustManagerFactory)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private ArrowheadException convertWebClientException(final WebClientResponseException ex, final String uri) {
		logger.debug("convertWebClientException started...");

		ErrorMessageDTO dto;
		try {
			dto = mapper.readValue(ex.getResponseBodyAsByteArray(), ErrorMessageDTO.class);

			if (dto.exceptionType() == null) {
				// it is not an ErrorMessageDTO
				throw new IOException("Not an ErrorMessageDTO object");
			}
		} catch (final IOException iex) {
			logger.debug("Unable to deserialize error message: {}", ex.getMessage());
			logger.debug("Exception: ", ex);
			logger.error("Request failed at {}, response status code: {}, status text: {}", uri, getStatusCodeAsString(ex), ex.getStatusText());
			if (Utilities.isEmpty(ex.getResponseBodyAsString())) {
				logger.error("Body: {}", Utilities.toPrettyJson(ex.getResponseBodyAsString()));
			}

			final String message = Utilities.isEmpty(ex.getResponseBodyAsString()) ? ex.getStatusText() : ex.getResponseBodyAsString();
			final String code = getStatusCodeAsString(ex);

			return new ArrowheadException(message + ", status code: " + code);
		}

		logger.debug("Error occured at {}. Returned with {}, status text: {}", uri, getStatusCodeAsString(ex), ex.getStatusText());
		logger.error("Request from {} returned with {}: {}", dto.origin(), dto.exceptionType(), dto.errorMessage());

		return HttpUtilities.createExceptionFromErrorMessageDTO(dto);
	}

	//-------------------------------------------------------------------------------------------------
	private String getStatusCodeAsString(final WebClientResponseException ex) {
		if (ex != null) {
			return String.valueOf(ex.getStatusCode().value());
		}

		return Constants.UNKNOWN;
	}
}