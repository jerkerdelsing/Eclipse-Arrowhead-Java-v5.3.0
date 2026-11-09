package eu.arrowhead.localcloudconfigurator.service.dto;

import java.util.List;
import java.util.Map;

// a saved design, in the JSON shape of the native 5.2.2 API
public record ConfigurationRecord(String id, String name, List<String> systems, List<String> remove, boolean reapply, boolean smoke, Map<String, LayoutPoint> layout, long revision, String updatedAt) {
}
