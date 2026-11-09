package eu.arrowhead.aigym;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ArrowheadSecurityFilter extends OncePerRequestFilter {
    private static final String PREFIX = "Bearer IDENTITY-TOKEN//";
    private final ArrowheadGateway arrowhead;
    private final ObjectMapper json;
    ArrowheadSecurityFilter(ArrowheadGateway arrowhead, ObjectMapper json) { this.arrowhead = arrowhead; this.json = json; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().equals("/health") || request.getRequestURI().equals("/error");
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        try {
            String authorization = request.getHeader("Authorization");
            if (authorization == null || !authorization.startsWith(PREFIX)) throw new SecurityException("Arrowhead identity token is required");
            Map<String,Object> identity = arrowhead.verifyIdentity(authorization.substring(PREFIX.length()));
            if (!Boolean.TRUE.equals(identity.get("verified"))) throw new SecurityException("Identity token is invalid or expired");
            String consumer = String.valueOf(identity.get("systemName"));
            X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
            if (certificates == null || certificates.length == 0) throw new SecurityException("Client certificate is required");
            String expected = "CN=" + consumer + "." + arrowhead.cloud + ".Company.arrowhead.eu";
            if (!certificates[0].getSubjectX500Principal().getName().contains(expected)) throw new SecurityException("Certificate and identity-token subjects differ");
            String scope = scope(request);
            boolean management = request.getRequestURI().startsWith("/aigym/mgmt/");
            if (management ? !consumer.equals("Sysop") : !arrowhead.verifyAuthorization(consumer, scope)) {
                response.setStatus(403); write(response, Map.of("error", "ConsumerAuthorization denied", "consumer", consumer, "scope", scope)); return;
            }
            request.setAttribute("arrowhead.consumer", consumer);
            chain.doFilter(request, response);
        } catch (SecurityException error) {
            response.setStatus(401); write(response, Map.of("error", error.getMessage()));
        } catch (Exception error) {
            response.setStatus(503); write(response, Map.of("error", "Arrowhead security verification unavailable", "detail", error.getMessage()));
        }
    }
    private static String scope(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.endsWith("/status")) return "status";
        if (path.endsWith("/samples")) return "add-sample";
        if (path.endsWith("/predict")) return "predict";
        if (path.endsWith("/observations")) return "observations";
        return request.getMethod().toLowerCase();
    }
    private void write(HttpServletResponse response, Object value) throws IOException {
        response.setContentType("application/json"); json.writeValue(response.getOutputStream(), value);
    }
}
