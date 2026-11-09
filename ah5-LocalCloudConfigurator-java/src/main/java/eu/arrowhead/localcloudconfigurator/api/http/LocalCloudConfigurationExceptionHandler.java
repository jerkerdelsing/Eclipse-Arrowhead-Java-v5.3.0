package eu.arrowhead.localcloudconfigurator.api.http;

import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.arrowhead.localcloudconfigurator.Refused;
import eu.arrowhead.localcloudconfigurator.Runner;

// The error bodies and status codes of the native 5.2.2 API: {"error": "..."}; 400 for invalid input, 502 for backend failures.
@RestControllerAdvice(assignableTypes = LocalCloudConfigurationAPI.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalCloudConfigurationExceptionHandler {

	@ExceptionHandler(Refused.class)
	public ResponseEntity<Map<String, String>> refused(final Refused ex) {
		return error(ex.status(), ex.getMessage());
	}

	@ExceptionHandler({ IllegalArgumentException.class, JsonProcessingException.class })
	public ResponseEntity<Map<String, String>> invalid(final Exception ex) {
		return error(400, String.valueOf(ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> failed(final Exception ex) {
		return error(502, Runner.redact(String.valueOf(ex.getMessage())));
	}

	private static ResponseEntity<Map<String, String>> error(final int status, final String message) {
		return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_JSON).body(Map.of("error", message));
	}
}
