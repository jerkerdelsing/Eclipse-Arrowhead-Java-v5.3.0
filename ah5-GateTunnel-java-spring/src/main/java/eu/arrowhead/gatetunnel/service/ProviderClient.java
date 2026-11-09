/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.gatetunnel.GateTunnelConstants;
import eu.arrowhead.gatetunnel.security.IdentityTokens;
import eu.arrowhead.gatetunnel.security.SystemTls;
import jakarta.annotation.PreDestroy;

// Provider side of an inter-cloud session: calls the negotiated local provider with the GateTunnel's own identity token,
// over mutual TLS pinned to the provider's certificate CN. The remote consumer travels in X-Arrowhead-Remote-Consumer.
// A refused (401) token is renewed once and the call repeated.
@Component
public class ProviderClient {

	//=================================================================================================
	// members

	public static final String REMOTE_CONSUMER_HEADER = "X-Arrowhead-Remote-Consumer";

	private static final int CONNECT_TIMEOUT_SECONDS = 8;
	private static final int REQUEST_TIMEOUT_SECONDS = 15;

	@Autowired
	private SystemTls tls;

	@Autowired
	private IdentityTokens tokens;

	@Autowired
	private FederationSettings settings;

	private final Map<String, HttpClient> clients = new ConcurrentHashMap<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public record Response(int status, byte[] body, String contentType) {
	}

	//-------------------------------------------------------------------------------------------------
	public Response call(final String provider, final String endpoint, final String method, final String contentType, final byte[] body, final String remoteConsumer) throws Exception {
		final HttpClient client = client(provider);
		for (int attempt = 0; attempt < 2; attempt++) {
			final String identity = tokens.current();
			final HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
					.timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
					.header("Authorization", "Bearer IDENTITY-TOKEN//" + identity)
					.header("Content-Type", contentType)
					.header(REMOTE_CONSUMER_HEADER, remoteConsumer)
					.method(method, body.length == 0 ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body))
					.build();
			final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			try (InputStream in = response.body()) {
				if (response.statusCode() == 401 && attempt == 0) {
					tokens.invalidate(identity);
					continue;
				}

				final byte[] data = in.readNBytes(GateTunnelConstants.MAX_BODY_BYTES + 1);
				if (data.length > GateTunnelConstants.MAX_BODY_BYTES) {
					throw new GateTunnelApiException(502, "Provider response too large");
				}

				return new Response(response.statusCode(), data, response.headers().firstValue("Content-Type").orElse("application/json"));
			}
		}

		throw new IllegalStateException("Unreachable provider identity retry");
	}

	//-------------------------------------------------------------------------------------------------
	@PreDestroy
	public void close() {
		clients.values().forEach(HttpClient::close);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private HttpClient client(final String provider) throws Exception {
		HttpClient client = clients.get(provider);
		if (client == null) {
			client = HttpClient.newBuilder()
					.sslContext(tls.clientContext(settings.commonName(provider)))
					.connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
					.followRedirects(HttpClient.Redirect.NEVER)
					.build();
			clients.put(provider, client);
		}

		return client;
	}
}
