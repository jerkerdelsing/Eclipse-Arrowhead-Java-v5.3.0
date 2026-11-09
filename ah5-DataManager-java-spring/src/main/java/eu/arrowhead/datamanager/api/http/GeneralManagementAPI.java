package eu.arrowhead.datamanager.api.http;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.datamanager.service.DataManagerHealthIndicator;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class GeneralManagementAPI {
    private final DataManagerHealthIndicator indicator;
    public GeneralManagementAPI(DataManagerHealthIndicator indicator) { this.indicator = indicator; }

    @GetMapping("/datamanager/mgmt/general/status")
    public Map<String, Object> status(HttpServletRequest request) {
        // Enforce the existing sysop-only management policy even when the optional framework filter is disabled.
        if (!HttpUtilities.isSysop(request, null)) {
            throw new ForbiddenException("Requester has no management permission", request.getRequestURI());
        }
        var health = indicator.health();
        return Map.of("system", "DataManager", "status", health.healthy() ? "ok" : "degraded", "detail", health.detail());
    }
}
