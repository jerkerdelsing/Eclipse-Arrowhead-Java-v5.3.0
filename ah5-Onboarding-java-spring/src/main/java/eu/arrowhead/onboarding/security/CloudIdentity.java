/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.onboarding.security;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import eu.arrowhead.common.SSLProperties;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.security.SecurityUtilities;
/**
 * The local cloud of this Onboarding, taken from its own certificate: the cloud CA that issued it, and the cloud part of
 * its common name (<System>.<Cloud>.<Organization>.arrowhead.eu).
 */
@Component
public class CloudIdentity {
    private final X500Principal cloudCa;
    private final String cloudCommonName;
    @Autowired
    public CloudIdentity(SSLProperties ssl) throws Exception {
        KeyStore keys = KeyStore.getInstance(ssl.getKeyStoreType() == null ? "PKCS12" : ssl.getKeyStoreType());
        try (InputStream input = ssl.getKeyStore().getInputStream()) { keys.load(input, ssl.getKeyStorePassword().toCharArray()); }
        X509Certificate own = SecurityUtilities.getCertificateFromKeyStore(keys, ssl.getKeyAlias());
        cloudCa = own.getIssuerX500Principal();
        cloudCommonName = SecurityUtilities.getCloudCN(SecurityUtilities.getIdentificationDataFromCertificate(own).commonName());
    }
    /** For tests and for deployments that configure the cloud explicitly. */
    public CloudIdentity(X500Principal cloudCa, String cloudCommonName) { this.cloudCa = cloudCa; this.cloudCommonName = cloudCommonName; }
    public X500Principal cloudCa() { return cloudCa; }
    public String cloudCommonName() { return cloudCommonName; }
    public String commonName(String name) { return requireName(name) + "." + cloudCommonName; }
    public static String requireName(String name) {
        if (name == null || !name.matches("[A-Z][A-Za-z0-9]{0,62}")) {
            throw new InvalidParameterException("Device and system names must be PascalCase, at most 63 characters");
        }
        return name;
    }
}
