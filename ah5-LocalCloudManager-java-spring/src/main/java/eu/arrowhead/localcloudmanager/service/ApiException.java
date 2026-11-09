package eu.arrowhead.localcloudmanager.service;

import com.fasterxml.jackson.databind.JsonNode;

/** An error answered to the console as {"error": message} with this status (the "status" of the native API's errors). */
public class ApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;
	private final transient JsonNode payload;

	public ApiException(final int status, final String message) {
		this(status, message, null);
	}

	public ApiException(final int status, final String message, final JsonNode payload) {
		super(message);
		this.status = status;
		this.payload = payload;
	}

	public int status() {
		return status;
	}

	public JsonNode payload() {
		return payload;
	}
}
