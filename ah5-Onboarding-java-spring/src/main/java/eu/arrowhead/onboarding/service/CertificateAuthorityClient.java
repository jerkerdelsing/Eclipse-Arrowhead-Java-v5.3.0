/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.http.ArrowheadHttpService;
/**
 * The local-cloud CertificateAuthority signs every certificate of the onboarding procedure (profiles on, de, sy) and keeps
 * the register of issued and revoked certificates. Onboarding holds no CA key.
 */
@Service
public class CertificateAuthorityClient {
    public static final String PROVIDER = "CertificateAuthority";
    public record Signed(long id, List<String> chain, String profile, String serialNumber, String expiresAt) {
        public String leaf() { return chain.getFirst(); }
    }
    private final ArrowheadHttpService http;
    public CertificateAuthorityClient(ArrowheadHttpService http) { this.http = http; }
    @SuppressWarnings("unchecked")
    public Signed sign(String csr, String profile, List<String> addresses) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("encodedCSR", csr); payload.put("profile", profile);
        if (addresses != null && !addresses.isEmpty()) { payload.put("addresses", addresses); }
        Map<String, Object> result = http.consumeService("certificateSigning", "sign", PROVIDER, Map.class, payload);
        if (result == null || !(result.get("certificateChain") instanceof List<?> chain) || chain.isEmpty() || result.get("id") == null) {
            throw new InternalServerError("CertificateAuthority returned no certificate");
        }
        return new Signed(((Number)result.get("id")).longValue(), (List<String>)chain, String.valueOf(result.get("profile")),
            String.valueOf(result.get("serialNumber")), String.valueOf(result.get("validBefore")));
    }
    /** GOOD, REVOKED, EXPIRED or UNKNOWN, as registered by the CertificateAuthority. */
    public String status(String certificate) {
        Map<?, ?> result = http.consumeService("certificateValidation", "check-certificate", PROVIDER, Map.class, Map.of("certificate", certificate));
        return result == null ? "UNKNOWN" : String.valueOf(result.get("status"));
    }
    public void revoke(long certificateId) {
        http.consumeService("certificateAuthorityManagement", "revoke-certificate", PROVIDER, Map.class, List.of(String.valueOf(certificateId)));
    }
}
