/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.service;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Set;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.springframework.stereotype.Service;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
/** Checks client CSRs (RSA, SHA-2, proof of possession, the expected name) and creates key pairs and CSRs for clients without one. */
@Service
public class CsrSupport {
    public record Csr(String encoded, String publicKey, String privateKey) { }
    private static final Set<String> SIGNATURES = Set.of("1.2.840.113549.1.1.11", "1.2.840.113549.1.1.12", "1.2.840.113549.1.1.13");
    public Csr generate(String commonName) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(3072);
            KeyPair pair = generator.generateKeyPair();
            var csr = new JcaPKCS10CertificationRequestBuilder(new X500Name("CN=" + commonName), pair.getPublic())
                .build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate()));
            return new Csr(encode(csr.getEncoded()), encode(pair.getPublic().getEncoded()), encode(pair.getPrivate().getEncoded()));
        } catch (Exception ex) { throw new InternalServerError("Cannot generate key pair and CSR"); }
    }
    /** Validates a client CSR for the expected common name and returns its public key (Base64 DER). */
    public String check(String encoded, String expectedCommonName) {
        if (encoded == null || encoded.isBlank() || encoded.length() > 32768) {
            throw new InvalidParameterException("CSR must be Base64 DER PKCS#10, at most 32768 characters");
        }
        try {
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.getDecoder().decode(encoded));
            if (!SIGNATURES.contains(csr.getSignatureAlgorithm().getAlgorithm().getId())) { throw new IllegalArgumentException("CSR must use SHA-256/384/512 with RSA"); }
            PublicKey key = new org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter().getPublicKey(csr.getSubjectPublicKeyInfo());
            if (!(key instanceof RSAPublicKey rsa) || rsa.getModulus().bitLength() < 2048 || rsa.getModulus().bitLength() > 8192) {
                throw new IllegalArgumentException("CSR must use RSA 2048..8192 bits");
            }
            if (!csr.isSignatureValid(new JcaContentVerifierProviderBuilder().build(key))) { throw new IllegalArgumentException("Invalid CSR proof of possession"); }
            var rdns = csr.getSubject().getRDNs(BCStyle.CN);
            if (rdns.length != 1 || !expectedCommonName.equals(rdns[0].getFirst().getValue().toString())) {
                throw new ForbiddenException("CSR common name must be " + expectedCommonName);
            }
            return encode(key.getEncoded());
        } catch (ForbiddenException ex) { throw ex; }
        catch (Exception ex) { throw new InvalidParameterException("Invalid CSR: require a signed RSA PKCS#10 with one matching CN"); }
    }
    private static String encode(byte[] bytes) { return Base64.getEncoder().encodeToString(bytes); }
}
