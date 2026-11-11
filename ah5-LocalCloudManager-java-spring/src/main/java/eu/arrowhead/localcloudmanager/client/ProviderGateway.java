package eu.arrowhead.localcloudmanager.client;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Requests to Arrowhead providers selected by orchestration on behalf of LocalCloudManager (its own certificate and identity token).
 * path is relative to the service's base path (null: the operation's own path) and may carry encoded path values and a query.
 */
public interface ProviderGateway {

	record Response(int status, JsonNode body) {
	}

	/** The response body; an HTTP error is an ApiException with the provider's status and error message. */
	JsonNode call(String serviceDefinition, String operation, String provider, String path, String method, JsonNode body);

	/** Status and body as answered, also for HTTP errors (pass-through). */
	Response exchange(String serviceDefinition, String operation, String provider, String path, String method, JsonNode body);

	/** The registered path of an operation of the orchestrated provider (for path templates). */
	String operationPath(String serviceDefinition, String operation, String provider);

	/** A binary response (a file download): status, content type, Content-Disposition, X-Content-SHA256 and the body. */
	record Download(int status, String contentType, String disposition, String sha256, byte[] body) {
	}

	/** GET of a binary resource of the orchestrated provider, at most maxBytes; errors pass through as status (body: JSON error). */
	default Download download(final String serviceDefinition, final String operation, final String provider, final String path, final int maxBytes) {
		throw new UnsupportedOperationException("download");
	}

	/** ServiceRegistry management (/serviceregistry/mgmt/...) with the identity token; body null means GET. */
	JsonNode serviceRegistry(String path, JsonNode body);
}
