package eu.arrowhead.localcloudmanager.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** The ServiceRegistry registration of LocalCloudManager as reported by /health and the overview. */
@Component
public class RegistrationState {

	private volatile ObjectNode state = Json.obj().put("registered", false).putNull("error").putNull("checkedAt");
	private volatile List<String> instanceIds = List.of();

	public ObjectNode get() {
		return state.deepCopy();
	}

	public List<String> instanceIds() {
		return instanceIds;
	}

	public void registered(final List<String> ids) {
		final ObjectNode s = Json.obj().put("registered", true);
		s.put("instanceId", ids.isEmpty() ? null : ids.get(0));
		s.set("instanceIds", Json.strings(ids));
		s.putNull("error");
		s.put("checkedAt", Json.now());
		instanceIds = List.copyOf(ids);
		state = s;
	}

	public void failed(final String error) {
		state = Json.obj().put("registered", false).put("error", error).put("checkedAt", Json.now());
	}
}
