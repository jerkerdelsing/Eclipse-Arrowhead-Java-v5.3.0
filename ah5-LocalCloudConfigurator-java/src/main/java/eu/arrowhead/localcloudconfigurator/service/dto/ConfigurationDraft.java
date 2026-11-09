package eu.arrowhead.localcloudconfigurator.service.dto;

import java.util.List;
import java.util.Map;

// request body of POST /api/configurations and PUT /api/configurations/{id}
public record ConfigurationDraft(String name, List<String> systems, List<String> remove, boolean reapply, boolean smoke, Map<String, LayoutPoint> layout, Long expectedRevision) {
}
