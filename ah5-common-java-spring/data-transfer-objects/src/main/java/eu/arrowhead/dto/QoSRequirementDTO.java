package eu.arrowhead.dto;

import java.util.Map;

public record QoSRequirementDTO(
			String type,
			String operation,
			Map<String, Object> requirements
		) {

}
