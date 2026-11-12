package eu.arrowhead.aigym;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ArrowheadGateway {
    private final ObjectMapper json;
    private final ReentrantLock tokenLock = new ReentrantLock();
    private HttpClient http;
    private volatile String identityToken;
    private volatile long tokenRefreshAt;

    @Value("${arrowhead.system-name}") String systemName;
    @Value("${arrowhead.cloud}") String cloud;
    @Value("${arrowhead.password}") String password;
    @Value("${arrowhead.registry-url}") String registryUrl;
    // Only the ServiceRegistry is configured. Authentication is looked up there (an identity token is needed before anything can
    // be orchestrated); every other service, ConsumerAuthorization included, is consumed through an orchestration request.
    @Value("${arrowhead.authentication-url:}") String authenticationUrl;
    @Value("${arrowhead.orchestration-ttl-seconds:300}") long orchestrationTtlSeconds;
    @Value("${arrowhead.key-store}") String keyStorePath;
    @Value("${arrowhead.trust-store}") String trustStorePath;
    @Value("${arrowhead.store-password}") String storePassword;

    record Endpoint(String url, String method, long expiresAt) {}
    private final Map<String,Endpoint> orchestrated = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile Endpoint orchestrator;

    ArrowheadGateway(ObjectMapper json) { this.json = json; }

    @PostConstruct void initialize() throws Exception {
        http = HttpClient.newBuilder().sslContext(sslContext()).connectTimeout(Duration.ofSeconds(8)).build();
    }

    SSLContext sslContext() throws Exception {
        char[] passwordChars = storePassword.toCharArray();
        KeyStore keys = loadStore(keyStorePath, passwordChars);
        KeyStore trust = loadStore(trustStorePath, passwordChars);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keys, passwordChars);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trust);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return context;
    }

    public String token() throws Exception {
        if (identityToken != null && System.currentTimeMillis() < tokenRefreshAt) return identityToken;
        tokenLock.lock();
        try {
            if (identityToken != null && System.currentTimeMillis() < tokenRefreshAt) return identityToken;
            Map<String,Object> response = request("POST", identity("identity-login", "/authentication/identity/login"),
                    Map.of("systemName", systemName, "credentials", Map.of("password", password)), null);
            identityToken = Objects.toString(response.get("token"));
            tokenRefreshAt = System.currentTimeMillis() + 240_000;
            return identityToken;
        } finally { tokenLock.unlock(); }
    }

    public Map<String,Object> verifyIdentity(String token) throws Exception {
        String verify = withPathParameter(identity("identity-verify", "/authentication/identity/verify"), token);
        return withIdentity(header -> request("GET", verify, null, header));
    }

    // Discovered interfaces use /verify/{token}; configured and older interfaces use /verify.
    static String withPathParameter(String url, String value) {
        String encoded = java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
        return url.matches(".*\\{[^}/]+\\}.*")
                ? url.replaceFirst("\\{[^}/]+\\}", java.util.regex.Matcher.quoteReplacement(encoded))
                : url + "/" + encoded;
    }

    public boolean verifyAuthorization(String consumer, String scope) throws Exception {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("provider", systemName); body.put("consumer", consumer); body.put("cloud", "LOCAL");
        body.put("targetType", "SERVICE_DEF"); body.put("target", "aiGym"); body.put("scope", scope);
        return Boolean.TRUE.equals(consume("authorization", "verify", "ConsumerAuthorization", body));
    }

    public void register(String advertisedHost, int port, String amqpHost, int amqpPort, String exchange, String baseRoutingKey) throws Exception {
        request("POST", registryUrl + "/serviceregistry/system-discovery/register",
                Map.of("metadata", Map.of("role", "ai-experimentation", "security", "X.509"), "version", "5.3.0",
                        "addresses", List.of(advertisedHost), "deviceName", ""), systemHeader());
        request("POST", registryUrl + "/serviceregistry/service-discovery/register", Map.of(
                "serviceDefinitionName", "generalManagement", "version", "1.0.0", "metadata", Map.of("systemVersion", "5.3.0"),
                "interfaces", List.of(Map.of("templateName", "generic_https", "protocol", "https", "policy", "CERT_AUTH",
                        "properties", Map.of("accessAddresses", List.of(advertisedHost), "accessPort", port,
                                "basePath", "/aigym/mgmt/general", "operations", Map.of("status", Map.of("method", "GET", "path", "/status")))))), systemHeader());
        Map<String,Object> operations = Map.of(
                "status", Map.of("method", "GET", "path", "/status"),
                "add-sample", Map.of("method", "POST", "path", "/samples"),
                "predict", Map.of("method", "POST", "path", "/predict"),
                "observations", Map.of("method", "GET", "path", "/observations"));
        request("POST", registryUrl + "/serviceregistry/service-discovery/register", Map.of(
                "serviceDefinitionName", "aiGym", "version", "1.0.0", "metadata", Map.of("contract", "AIGym-HTTPS-IDD-5.2.2"),
                "interfaces", List.of(Map.of("templateName", "generic_https", "protocol", "https", "policy", "CERT_AUTH",
                        "properties", Map.of("accessAddresses", List.of(advertisedHost), "accessPort", port,
                                "basePath", "/aigym", "operations", operations)))), systemHeader());
        request("POST", registryUrl + "/serviceregistry/service-discovery/register", Map.of(
                "serviceDefinitionName", "sysMonitor", "version", "1.0.0", "metadata", Map.of(
                        "contract", "SysMonitor-AMQP-IDD-5.2.2", "publisherMode", "native",
                        "schemaVersion", "1.0", "observationTypes", List.of("echo", "power", "status", "trainingSample", "inference")),
                "interfaces", List.of(Map.of("templateName", "generic_amqps", "protocol", "amqps", "policy", "CERT_AUTH",
                        "properties", Map.of("accessAddresses", List.of(amqpHost), "accessPort", amqpPort,
                                "exchange", exchange, "baseRoutingKey", baseRoutingKey,
                                "operations", List.of("echo", "power", "status", "trainingSample", "inference"))))), systemHeader());
    }

    public Map<String,Object> orchestrateSysMonitorData() throws Exception {
        Map<String,Object> requirement = Map.of("serviceDefinition", "sysMonitorData", "operations", List.of("subscribe"),
                "versions", List.of("1.0.0"), "interfaceTemplateNames", List.of("generic_mqtts"),
                "securityPolicies", List.of("CERT_AUTH"));
        Endpoint pull = orchestrator();
        return withIdentity(header -> request(pull.method(), pull.url(),
                Map.of("serviceRequirement", requirement, "orchestrationFlags", Map.of()), header));
    }

    // Authentication's identity service; the ServiceRegistry answers this lookup without a token (unrestrictedDiscovery)
    private volatile Map<String,Object> identityInterface;
    private String identity(String operation, String configuredPath) throws Exception {
        if (!authenticationUrl.isBlank()) return authenticationUrl + configuredPath;
        Map<String,Object> props = identityInterface;
        if (props == null) {
            props = firstHttps(request("POST", registryUrl + "/serviceregistry/service-discovery/lookup", Map.of(
                    "serviceDefinitionNames", List.of("identity"), "providerNames", List.of("Authentication"),
                    "interfaceTemplateNames", List.of("generic_https")), null).get("entries"));
            if (props == null) throw new IllegalStateException("Authentication is not registered in the ServiceRegistry");
            identityInterface = props;
        }
        return endpoint(props, operation, 0).url();
    }

    // The orchestrator is the other service looked up in the ServiceRegistry
    private Endpoint orchestrator() throws Exception {
        Endpoint known = orchestrator;
        if (known != null) return known;
        Map<String,Object> props = firstHttps(withIdentity(header -> request("POST", registryUrl + "/serviceregistry/service-discovery/lookup", Map.of(
                "serviceDefinitionNames", List.of("serviceOrchestration"), "providerNames", List.of("DynamicServiceOrchestration"),
                "interfaceTemplateNames", List.of("generic_https")), header)).get("entries"));
        if (props == null) throw new IllegalStateException("No orchestrator is registered in the ServiceRegistry");
        return orchestrator = endpoint(props, "pull", 0);
    }

    // An orchestration request on behalf of AIGym: only providers AIGym is authorized to consume the operation of are returned.
    private Endpoint orchestrate(String definition, String operation, String provider) throws Exception {
        String key = definition + "|" + operation + "|" + provider;
        Endpoint known = orchestrated.get(key);
        if (known != null && known.expiresAt() > System.currentTimeMillis()) return known;
        // no securityPolicies filter: the core systems register their interfaces with policy NONE (they check mTLS and tokens themselves)
        Map<String,Object> requirement = new LinkedHashMap<>(Map.of("serviceDefinition", definition, "operations", List.of(operation),
                "interfaceTemplateNames", List.of("generic_https")));
        Map<String,Object> flags = new LinkedHashMap<>(Map.of("MATCHMAKING", true, "ALLOW_INTERCLOUD", false, "ALLOW_TRANSLATION", false));
        if (provider != null) { requirement.put("preferredProviders", List.of(provider)); flags.put("ONLY_PREFERRED", true); }
        Endpoint pull = orchestrator();
        Map<String,Object> response;
        try {
            response = withIdentity(header -> request(pull.method(), pull.url(), Map.of("serviceRequirement", requirement, "orchestrationFlags", flags), header));
        } catch (java.io.IOException ex) { orchestrator = null; throw ex; }
        for (Object result : response.get("results") instanceof List<?> list ? list : List.of()) {
            Map<String,Object> props = firstHttps(List.of(result));
            if (props != null && props.get("operations") instanceof Map<?,?> ops && ops.containsKey(operation)) {
                Endpoint found = endpoint(props, operation, System.currentTimeMillis() + orchestrationTtlSeconds * 1000);
                orchestrated.put(key, found);
                return found;
            }
        }
        throw new IllegalStateException("Orchestration found no authorized provider of " + definition + " (" + operation + ")");
    }

    private Object consume(String definition, String operation, String provider, Object body) throws Exception {
        Endpoint target = orchestrate(definition, operation, provider);
        try {
            return withIdentity(header -> requestObject(target.method(), target.url(), body, header));
        } catch (java.io.IOException ex) { orchestrated.values().remove(target); throw ex; }
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> firstHttps(Object services) {
        for (Object service : services instanceof List<?> list ? list : List.of()) {
            if (!(service instanceof Map<?,?> map) || !(map.get("interfaces") instanceof List<?> interfaces)) continue;
            for (Object raw : interfaces)
                if (raw instanceof Map<?,?> intf && "generic_https".equals(intf.get("templateName")) && intf.get("properties") instanceof Map<?,?> props)
                    return (Map<String,Object>) props;
        }
        return null;
    }

    private static Endpoint endpoint(Map<String,Object> props, String operation, long expiresAt) {
        List<?> addresses = props.get("accessAddresses") instanceof List<?> list ? list : List.of();
        if (addresses.isEmpty() || !(props.get("operations") instanceof Map<?,?> ops) || !(ops.get(operation) instanceof Map<?,?> op))
            throw new IllegalStateException("Service does not offer " + operation);
        String method = op.get("method") == null ? "POST" : op.get("method").toString();
        return new Endpoint("https://" + addresses.get(0) + ":" + props.get("accessPort") + Objects.toString(props.get("basePath"), "") + op.get("path"), method, expiresAt);
    }

    @FunctionalInterface
    private interface IdentityCall<T> { T call(Map<String,String> identityHeader) throws Exception; }

    // Authentication keeps one valid token per system: another login as AIGym invalidates the cached token.
    // When the own token is rejected, log in again and repeat the call once.
    private <T> T withIdentity(IdentityCall<T> call) throws Exception {
        String used = token();
        try {
            return call.call(Map.of("Authorization", "Bearer IDENTITY-TOKEN//" + used));
        } catch (IllegalStateException ex) {
            if (ex.getMessage() == null || !ex.getMessage().contains(" returned 401")) throw ex;
            invalidateToken(used);
            return call.call(identityHeader());
        }
    }

    private void invalidateToken(String rejected) {
        tokenLock.lock();
        try { if (rejected.equals(identityToken)) { identityToken = null; tokenRefreshAt = 0; } }
        finally { tokenLock.unlock(); }
    }

    private Map<String,String> identityHeader() throws Exception { return Map.of("Authorization", "Bearer IDENTITY-TOKEN//" + token()); }
    // ServiceRegistry requires AIGym's identity token, bound to its client certificate (mutual TLS).
    private Map<String,String> systemHeader() throws Exception { return Map.of("Authorization", "Bearer IDENTITY-TOKEN//" + token()); }

    @SuppressWarnings("unchecked")
    private Map<String,Object> request(String method, String url, Object body, Map<String,String> headers) throws Exception {
        Object result = requestObject(method, url, body, headers);
        return result instanceof Map<?,?> map ? (Map<String,Object>) map : Map.of("value", result);
    }
    private Object requestObject(String method, String url, Object body, Map<String,String> headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(12)).header("Accept", "application/json");
        if (headers != null) headers.forEach(builder::header);
        if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)));
        HttpResponse<byte[]> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalStateException(method + " " + url + " returned " + response.statusCode() + ": " + new String(response.body()));
        if (response.body().length == 0) return Map.of();
        return json.readValue(response.body(), new TypeReference<Object>() {});
    }

    private static KeyStore loadStore(String configured, char[] password) throws Exception {
        String raw = configured.startsWith("file:") ? URI.create(configured).getPath() : configured;
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(Path.of(raw))) { store.load(input, password); }
        return store;
    }
}
