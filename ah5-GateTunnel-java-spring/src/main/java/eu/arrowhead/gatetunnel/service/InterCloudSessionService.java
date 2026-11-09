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

import static eu.arrowhead.gatetunnel.service.Wire.bytes;
import static eu.arrowhead.gatetunnel.service.Wire.id;
import static eu.arrowhead.gatetunnel.service.Wire.integer;
import static eu.arrowhead.gatetunnel.service.Wire.m;
import static eu.arrowhead.gatetunnel.service.Wire.obj;
import static eu.arrowhead.gatetunnel.service.Wire.str;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import eu.arrowhead.dto.IdentityVerifyResponseDTO;
import eu.arrowhead.gatetunnel.GateTunnelConstants;
import eu.arrowhead.gatetunnel.security.IdentityTokens;
import eu.arrowhead.gatetunnel.security.SystemTls;
import eu.arrowhead.gatetunnel.security.TlsSupport;
import eu.arrowhead.gatetunnel.service.validation.OperationScope;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

// Inter-cloud relay sessions, opened by the local Gatekeeper after a successful inter-cloud negotiation. Ported verbatim in
// behaviour from the 5.2.2 native GateTunnel:
// - provider side (open-provider-session): remembers the negotiated provider endpoint and operations; requests arrive over
//   the relay (incoming) and are executed against the provider (execute);
// - consumer side (open-consumer-session): opens a mutual-TLS listener in the INTERCLOUD port pool (8200-8299) for the
//   negotiated consumer; its requests are forwarded over the relay (forward).
// FEDERATION_PROFILE legacy-mtls relays the request as relay-visible JSON (the earlier wire protocol); ah-gt-1 seals it
// end-to-end (SessionSecurity). There is no fallback between them: a peer on the other profile is refused.
// Sessions live in memory only; they end on close, on expiry (expireSessions, scheduled) or with the process.
@Service
public class InterCloudSessionService {

	//=================================================================================================
	// members

	private static final int LISTENER_BACKLOG = 32;

	private final Logger logger = LogManager.getLogger(getClass());

	private final FederationSettings settings;
	private final RelayClient relay;
	private final ProviderClient providerClient;
	private final SystemTls tls;
	private final IdentityTokens tokens;
	private final SessionSecurity security;
	private final boolean e2e;

	private final Map<String, Session> providers = new ConcurrentHashMap<>();
	private final Map<String, Session> consumers = new ConcurrentHashMap<>();
	private final Semaphore requests = new Semaphore(GateTunnelConstants.MAX_TUNNEL_REQUESTS);

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public InterCloudSessionService(final FederationSettings settings, final RelayClient relay, final ProviderClient providerClient, final SystemTls tls, final IdentityTokens tokens,
			final ObjectProvider<SessionSecurity> security) {
		this.settings = settings;
		this.relay = relay;
		this.providerClient = providerClient;
		this.tls = tls;
		this.tokens = tokens;
		this.e2e = settings.isEndToEnd();
		this.security = e2e ? security.getIfAvailable() : null;
		if (e2e && this.security == null) {
			throw new IllegalStateException("The ah-gt-1 profile requires the AH-GT-1 relay identity");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	public void bindRelay() {
		relay.bind(settings::trustedPeer, this::incoming);
	}

	//-------------------------------------------------------------------------------------------------
	public synchronized Map<String, Object> openProvider(final Map<String, Object> payload) {
		final Session s = session(payload);
		if (!s.consumerCloud.equals(PeerNames.cloudIdentity(s.peer))) {
			throw new IllegalArgumentException("consumerCloud does not match peer");
		}
		if (obj(s.target.get("operations")).isEmpty()) {
			throw new IllegalArgumentException("Negotiated operations required");
		}

		final String address = str(s.target.get("address"));
		final int targetPort = integer(s.target.get("port"), 0);
		if (address.isEmpty() || address.matches(".*[/@?#\\s].*") || targetPort < 1 || targetPort > 65535) {
			throw new IllegalArgumentException("Invalid provider endpoint");
		}
		if (providers.size() >= GateTunnelConstants.MAX_PROVIDER_SESSIONS) {
			throw new GateTunnelApiException(503, "Provider session limit reached");
		}

		providers.entrySet().removeIf(e -> {
			final Session old = e.getValue();
			return old.consumer.equals(s.consumer) && old.consumerCloud.equals(s.consumerCloud) && old.provider.equals(s.provider) && old.definition.equals(s.definition);
		});

		if (e2e) {
			try {
				s.ephemeral = SessionSecurity.ephemeral();
				s.params = params(s, s.peer, settings.ownCommonName(), SessionSecurity.raw(s.ephemeral.getPublic()));
				providers.put(s.id, s);
				return m("sessionId", s.id, "keyExchange", security.exchange("provider", s.params));
			} catch (final Exception ex) {
				throw new GateTunnelApiException(500, "AH-GT-1 key exchange failed");
			}
		}

		providers.put(s.id, s);
		return m("sessionId", s.id);
	}

	//-------------------------------------------------------------------------------------------------
	public synchronized Map<String, Object> openConsumer(final Map<String, Object> payload) throws Exception {
		final Session s = session(payload);
		if (!s.providerCloud.equals(PeerNames.cloudIdentity(s.peer))) {
			throw new IllegalArgumentException("providerCloud does not match peer");
		}
		s.consumerCloud = settings.getCloud() + "|" + settings.getOrganization();

		if (e2e) {
			// the provider GateTunnel's signed ephemeral key, verified against the organization CA; then the own half (J5)
			final Map<String, Object> exchange = obj(payload.get("keyExchange"));
			if (exchange.isEmpty()) {
				throw new GateTunnelApiException(400, "AH-GT-1 key exchange of the provider GateTunnel is required");
			}

			s.params = params(s, settings.ownCommonName(), s.peer, exchange.get("providerKey"));
			try {
				security.verifyPeer(exchange.get("certificateChain"), s.peer, exchange.get("signature"), SessionSecurity.transcript("provider", s.params));
			} catch (final SessionSecurity.SecurityError ex) {
				throw new GateTunnelApiException(502, "Peer key exchange refused: " + ex.getMessage());
			}

			final KeyPair own = SessionSecurity.ephemeral();
			s.params.put("consumerKey", SessionSecurity.raw(own.getPublic()));
			s.ownExchange = security.exchange("consumer", s.params);
			s.keys = SessionSecurity.derive(own.getPrivate(), exchange.get("providerKey"), s.id);
		}

		for (final Session old : new ArrayList<>(consumers.values())) {
			if (old.consumer.equals(s.consumer) && old.providerCloud.equals(s.providerCloud) && old.provider.equals(s.provider) && old.definition.equals(s.definition)) {
				consumers.remove(old.id);
				stop(old);
			}
		}

		final Set<Integer> used = new HashSet<>();
		consumers.values().forEach(old -> used.add(old.port));
		HttpsServer listener = null;
		for (int port = settings.getInterCloudMinPort(); port <= settings.getInterCloudMaxPort(); port++) {
			if (used.contains(port)) {
				continue;
			}
			try {
				listener = HttpsServer.create(new InetSocketAddress("0.0.0.0", port), LISTENER_BACKLOG);
				s.port = port;
				break;
			} catch (final IOException ignored) {
				// port taken by something else: try the next one
			}
		}
		if (listener == null) {
			throw new GateTunnelApiException(503, "No tunnel ports available");
		}

		final SSLContext context = tls.serverContext();
		listener.setHttpsConfigurator(new HttpsConfigurator(context) {
			@Override
			public void configure(final HttpsParameters parameters) {
				final SSLParameters ssl = context.getDefaultSSLParameters();
				ssl.setNeedClientAuth(true);
				ssl.setProtocols(new String[] { "TLSv1.3", "TLSv1.2" });
				parameters.setSSLParameters(ssl);
			}
		});
		listener.createContext("/", exchange -> handleTunnel(s, exchange));
		listener.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
		s.server = listener;
		consumers.put(s.id, s);
		listener.start();

		return m("address", settings.getAdvertisedAddress(), "port", s.port, "sessionId", s.id);
	}

	//-------------------------------------------------------------------------------------------------
	// Removes an inter-cloud session of either side; true if one existed.
	public boolean close(final String sessionId) {
		boolean changed = false;
		final Session consumer = consumers.remove(sessionId);
		if (consumer != null) {
			stop(consumer);
			changed = true;
		}
		if (providers.remove(sessionId) != null) {
			changed = true;
		}

		return changed;
	}

	//-------------------------------------------------------------------------------------------------
	public void expireSessions() {
		providers.entrySet().removeIf(e -> expired(e.getValue()));
		for (final Session s : consumers.values()) {
			if (expired(s) && consumers.remove(s.id, s)) {
				stop(s);
			}
		}
	}

	//-------------------------------------------------------------------------------------------------
	public int providerSessionCount() {
		return providers.size();
	}

	//-------------------------------------------------------------------------------------------------
	public int consumerSessionCount() {
		return consumers.size();
	}

	//-------------------------------------------------------------------------------------------------
	public List<Object> summaries() {
		final List<Object> out = new ArrayList<>();
		providers.values().forEach(s -> out.add(summary(s, "provider")));
		consumers.values().forEach(s -> out.add(summary(s, "consumer")));

		return out;
	}

	//-------------------------------------------------------------------------------------------------
	@PreDestroy
	public void shutdown() {
		consumers.values().forEach(this::stop);
		consumers.clear();
		providers.clear();
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	boolean expired(final Session s) {
		final long now = System.currentTimeMillis();
		return now - s.created > settings.getSessionMaxAgeSeconds() * 1000L || now - s.lastUsed > settings.getSessionIdleSeconds() * 1000L;
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> summary(final Session s, final String side) {
		return m("sessionId", s.id, "side", side, "consumer", s.consumer, "consumerCloud", s.consumerCloud, "provider", s.provider, "providerCloud", s.providerCloud,
				"serviceDefinition", s.definition, "requests", s.requests.get(), "createdAt", s.created / 1000.0);
	}

	//-------------------------------------------------------------------------------------------------
	private Session session(final Map<String, Object> payload) {
		final Session s = new Session();
		s.id = str(payload.getOrDefault("sessionId", id()));
		s.consumer = str(payload.get("consumer"));
		s.provider = str(payload.get("provider"));
		s.definition = str(payload.get("serviceDefinition"));
		s.consumerCloud = str(payload.get("consumerCloud"));
		s.providerCloud = str(payload.get("providerCloud"));
		s.peer = str(payload.get("peer"));
		s.target = new LinkedHashMap<>(obj(payload.get("target")));
		if (s.id.isEmpty() || s.consumer.isEmpty() || s.provider.isEmpty() || s.definition.isEmpty() || !settings.trustedPeer(s.peer)) {
			throw new IllegalArgumentException("Required session fields or trusted peer missing");
		}
		if (!e2e && payload.get("keyExchange") != null) {
			throw new GateTunnelApiException(400, "AH-GT-1 session cannot be downgraded to legacy-mTLS");
		}

		return s;
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> params(final Session s, final String consumerGateTunnel, final String providerGateTunnel, final Object providerKey) {
		return new LinkedHashMap<>(m("sessionId", s.id, "consumer", s.consumer, "consumerCloud", s.consumerCloud, "provider", s.provider, "serviceDefinition", s.definition,
				"consumerGateTunnel", consumerGateTunnel, "providerGateTunnel", providerGateTunnel, "providerKey", providerKey));
	}

	//-------------------------------------------------------------------------------------------------
	private void handleTunnel(final Session s, final HttpExchange exchange) {
		final boolean acquired = requests.tryAcquire();
		try {
			if (!acquired) {
				throw new GateTunnelApiException(503, "Too many requests");
			}
			forward(s, exchange);
		} catch (final GateTunnelApiException ex) {
			sendQuietly(exchange, ex.getStatus(), bytes(m("error", ex.getMessage())), "application/json; charset=utf-8");
		} catch (final Exception ex) {
			logger.debug("Tunnel forwarding failed", ex);
			sendQuietly(exchange, 502, bytes(m("error", "Tunnel forwarding failed")), "application/json; charset=utf-8");
		} finally {
			if (acquired) {
				requests.release();
			}
			exchange.close();
		}
	}

	//-------------------------------------------------------------------------------------------------
	void forward(final Session s, final HttpExchange x) throws Exception {
		if (expired(s) || consumers.get(s.id) != s) {
			throw new GateTunnelApiException(401, "Session expired; orchestrate again");
		}
		if (!caller(x).equals(s.consumer)) {
			throw new GateTunnelApiException(403, "Caller is not the negotiated consumer");
		}

		final byte[] body = x.getRequestBody().readNBytes(GateTunnelConstants.MAX_BODY_BYTES + 1);
		if (body.length > GateTunnelConstants.MAX_BODY_BYTES) {
			throw new GateTunnelApiException(413, "Request too large");
		}

		final Map<String, Object> content = m("method", x.getRequestMethod(), "path", x.getRequestURI().toASCIIString(),
				"contentType", Objects.toString(x.getRequestHeaders().getFirst("Content-Type"), "application/json"),
				"body", Base64.getEncoder().encodeToString(body));
		final String peerCloud = s.peer.split("\\.")[1];
		final Map<String, Object> reply;
		if (e2e) {
			// method, path and body travel sealed with the session key (J5)
			final long sequence = s.sequence.incrementAndGet();
			final Map<String, Object> message = new LinkedHashMap<>(m("type", "http-request", "sessionId", s.id, "keyExchange", s.ownExchange));
			message.putAll(SessionSecurity.seal(s.keys.get("c2p"), s.id, "c2p", sequence, content));
			final Map<String, Object> sealed = relay.request(peerCloud, message);
			if (sealed.get("ciphertext") == null) {
				throw new GateTunnelApiException(401, "Peer GateTunnel refused the request: " + str(sealed.getOrDefault("error", "no reason")));
			}

			final SessionSecurity.Opened opened;
			try {
				opened = SessionSecurity.open(s.keys.get("p2c"), s.id, "p2c", sealed, settings.getClockSkewSeconds());
			} catch (final SessionSecurity.SecurityError ex) {
				throw new GateTunnelApiException(401, ex.getMessage());
			}
			if (opened.sequence() != sequence) {
				throw new GateTunnelApiException(401, "Reply does not belong to this request");
			}
			reply = opened.content();
		} else {
			final Map<String, Object> legacy = new LinkedHashMap<>(m("type", "http-request", "sessionId", s.id));
			legacy.putAll(content);
			reply = relay.request(peerCloud, legacy);
		}

		final int status = integer(reply.get("status"), 502);
		if (status < 100 || status > 599) {
			throw new GateTunnelApiException(502, "Invalid peer status");
		}
		final byte[] data = Base64.getDecoder().decode(str(reply.get("body")));
		if (data.length > GateTunnelConstants.MAX_BODY_BYTES) {
			throw new GateTunnelApiException(502, "Peer response too large");
		}
		s.requests.incrementAndGet();
		s.lastUsed = System.currentTimeMillis();
		send(x, status, data, str(reply.getOrDefault("contentType", "application/json")));
	}

	//-------------------------------------------------------------------------------------------------
	// The tunnel caller: system or operator certificate, identity token verified by Authentication and bound to that certificate.
	private String caller(final HttpExchange exchange) {
		if (!(exchange instanceof final HttpsExchange https)) {
			throw new GateTunnelApiException(401, "Mutual TLS is required");
		}

		final X509Certificate leaf;
		try {
			leaf = (X509Certificate) https.getSSLSession().getPeerCertificates()[0];
		} catch (final Exception ex) {
			throw new GateTunnelApiException(401, "Mutual TLS is required");
		}
		if (!GateTunnelConstants.CALLER_PROFILES.contains(TlsSupport.attribute(leaf, "DNQ"))) {
			throw new GateTunnelApiException(403, "Only system/operator certificates may call services");
		}

		final String header = https.getRequestHeaders().getFirst("Authorization");
		final String prefix = "Bearer IDENTITY-TOKEN//";
		if (header == null || !header.startsWith(prefix)) {
			throw new GateTunnelApiException(401, "Arrowhead identity token is required");
		}
		final String token = header.substring(prefix.length());
		if (token.isEmpty() || token.length() > 4096 || !token.matches("[A-Za-z0-9._~+-]+")) {
			throw new GateTunnelApiException(401, "Invalid identity token syntax");
		}

		final IdentityVerifyResponseDTO verified = tokens.verify(token);
		final String system = str(verified.systemName());
		if (!TlsSupport.cn(leaf).equals(settings.commonName(system))) {
			throw new GateTunnelApiException(401, "Identity token does not match the client certificate");
		}

		return system;
	}

	//-------------------------------------------------------------------------------------------------
	private static Map<String, Object> answer(final int status, final byte[] body, final String contentType) {
		return m("status", status, "body", Base64.getEncoder().encodeToString(body), "contentType", contentType);
	}

	//-------------------------------------------------------------------------------------------------
	private static Map<String, Object> refusal(final String reason) {
		return m("error", reason, "accepted", false);
	}

	//-------------------------------------------------------------------------------------------------
	// A relayed request of a peer GateTunnel (provider side).
	Map<String, Object> incoming(final String peer, final Map<String, Object> message) throws Exception {
		final Session s = providers.get(str(message.get("sessionId")));
		if (!e2e) {
			if (!"http-request".equals(str(message.get("type"))) || s == null || !s.peer.equals(peer) || expired(s)) {
				return answer(403, bytes(m("error", "Unknown, expired or wrong-peer session")), "application/json");
			}
			if (message.get("ciphertext") != null) {
				return answer(403, bytes(m("error", "Encrypted profile cannot be downgraded")), "application/json");
			}

			return execute(s, message);
		}

		// AH-GT-1 (J5): refusals carry no session data, only why the request was not executed
		if (!"http-request".equals(str(message.get("type"))) || s == null || !s.peer.equals(peer)) {
			return refusal("Unknown inter-cloud session or wrong peer");
		}
		if (expired(s)) {
			providers.remove(s.id, s);
			return refusal("Inter-cloud session has expired");
		}
		if (message.get("ciphertext") == null) {
			return refusal("AH-GT-1 is required; a legacy request is not executed");
		}

		final SessionSecurity.Opened opened;
		try {
			final Map<String, Object> exchange = obj(message.get("keyExchange"));
			synchronized (s) {
				if (s.keys == null) {
					// first request: the consumer GateTunnel's signed ephemeral key completes the key exchange
					final Map<String, Object> candidate = new LinkedHashMap<>(s.params);
					candidate.put("consumerKey", exchange.get("consumerKey"));
					security.verifyPeer(exchange.get("certificateChain"), s.peer, exchange.get("signature"), SessionSecurity.transcript("consumer", candidate));
					s.consumerKey = str(exchange.get("consumerKey"));
					s.keys = SessionSecurity.derive(s.ephemeral.getPrivate(), s.consumerKey, s.id);
					s.ephemeral = null;
				} else if (!str(exchange.get("consumerKey")).equals(s.consumerKey)) {
					throw new SessionSecurity.SecurityError("key exchange differs from the established one");
				}
			}

			opened = SessionSecurity.open(s.keys.get("c2p"), s.id, "c2p", message, settings.getClockSkewSeconds());
			if (!s.replay.accept(opened.sequence())) {
				throw new SessionSecurity.SecurityError("replayed or outdated request");
			}
		} catch (final SessionSecurity.SecurityError ex) {
			return refusal(ex.getMessage());
		}

		Map<String, Object> result;
		try {
			result = execute(s, opened.content());
		} catch (final Exception ex) {
			result = answer(502, bytes(m("error", "Provider call failed")), "application/json");
		}

		return SessionSecurity.seal(s.keys.get("p2c"), s.id, "p2c", opened.sequence(), result);
	}

	//-------------------------------------------------------------------------------------------------
	private Map<String, Object> execute(final Session s, final Map<String, Object> message) throws Exception {
		final String method = str(message.get("method"));
		final String path = str(message.get("path"));
		if (!OperationScope.operationAllowed(method, path, str(s.target.get("basePath")), obj(s.target.get("operations")))) {
			return answer(403, bytes(m("error", "Operation outside negotiated session")), "application/json");
		}

		final byte[] body = Base64.getDecoder().decode(str(message.get("body")));
		if (body.length > GateTunnelConstants.MAX_BODY_BYTES) {
			throw new IllegalArgumentException("Request too large");
		}

		final String endpoint = new URI("https", null, str(s.target.get("address")), integer(s.target.get("port"), 0), null, null, null).toASCIIString() + path;
		final ProviderClient.Response response = providerClient.call(s.provider, endpoint, method, str(message.getOrDefault("contentType", "application/json")), body,
				s.consumer + "|" + s.consumerCloud);
		s.requests.incrementAndGet();
		s.lastUsed = System.currentTimeMillis();

		return answer(response.status(), response.body(), response.contentType());
	}

	//-------------------------------------------------------------------------------------------------
	private void stop(final Session s) {
		if (s.server != null) {
			s.server.stop(0);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static void send(final HttpExchange x, final int status, final byte[] value, final String type) throws IOException {
		x.getResponseHeaders().set("Content-Type", type);
		x.getResponseHeaders().set("Cache-Control", "no-store");
		x.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
		x.sendResponseHeaders(status, status == 204 ? -1 : value.length);
		if (status != 204) {
			x.getResponseBody().write(value);
		}
	}

	//-------------------------------------------------------------------------------------------------
	private static void sendQuietly(final HttpExchange x, final int status, final byte[] value, final String type) {
		try {
			send(x, status, value, type);
		} catch (final Exception ignored) {
			// the client is gone
		}
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	static final class Session {
		private String id;
		private String consumer;
		private String consumerCloud;
		private String provider;
		private String providerCloud;
		private String definition;
		private String peer;
		private Map<String, Object> target;
		private final long created = System.currentTimeMillis();
		private volatile long lastUsed = created;
		private final AtomicLong requests = new AtomicLong();
		private int port;
		private HttpsServer server;
		// AH-GT-1 (J5): transcript parameters, own ephemeral key until the exchange completes, per-direction keys, sequence
		private Map<String, Object> params;
		private KeyPair ephemeral;
		private Map<String, byte[]> keys;
		private String consumerKey;
		private Map<String, Object> ownExchange;
		private final AtomicLong sequence = new AtomicLong();
		private final SessionSecurity.Replay replay = new SessionSecurity.Replay();
	}
}
