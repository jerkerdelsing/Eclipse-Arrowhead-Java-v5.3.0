package eu.arrowhead.localcloudconfigurator;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/** The shared JSON mapper: unknown fields are rejected, so a typo in a catalog or a request is an error. */
public final class Json {

	public static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
			.setSerializationInclusion(JsonInclude.Include.NON_NULL);

	/** Lenient mapper for ColonyOS replies, which carry many fields the configurator does not use. */
	public static final ObjectMapper LENIENT = new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	private Json() {
	}

	public static String write(final Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (final JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}
}
