library package ArrowheadCertificateAuthorityData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'CertificateCheckRequest' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'certificate' : String[0..1] { doc /* Source type: String. Certificate to check, PEM or Base64 DER (alias encodedCertificate). */ }
    }
    item def 'CertificateCheckResult' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'status' : String[0..1] { doc /* Source type: String. GOOD, REVOKED, EXPIRED or UNKNOWN (not issued by this CA). */ }
        attribute 'valid' : Boolean[0..1] { doc /* Source type: Boolean. True only for GOOD. */ }
        attribute 'serialNumber' : String[0..1] { doc /* Source type: String. Serial number, decimal. */ }
        attribute 'known' : Boolean[0..1] { doc /* Source type: Boolean. Whether the serial is in the register (only for certificates of this CA). */ }
    }
    item def 'CertificateRecord' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Record identifier. */ }
        attribute 'serial_number' : String[0..1] { doc /* Source type: String. Serial number. */ }
        attribute 'common_name' : String[0..1] { doc /* Source type: String. Subject common name. */ }
        attribute 'created_by' : String[0..1] { doc /* Source type: String. Requesting system (revoked-by:<caller> for imported revocations). */ }
        attribute 'valid_from' : String[0..1] { doc /* Source type: String. Start of validity. */ }
        attribute 'valid_until' : String[0..1] { doc /* Source type: String. End of validity. */ }
        attribute 'revoked_at' : String[0..1] { doc /* Source type: String. Revocation time, null if not revoked. */ }
        attribute 'certificate_pem' : String[0..1] { doc /* Source type: String. The certificate, PEM. */ }
    }
    item def 'CertificateSigningRequest' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'encodedCSR' : String[0..1] { doc /* Source type: String. PEM PKCS\textbackslash{}\#10 request or Base64 DER (alias csr). */ }
        attribute 'profile' : String[0..1] { doc /* Source type: String. on, de, sy or op; default the CSR dnQualifier, else sy. Must be allowed for the caller. */ }
        attribute 'addresses' : String[0..*] { doc /* Source type: Array<String>. DNS names and IP addresses for the subject alternative name (at most 16); default those of the CSR. */ }
        ref item 'validAfter' : JSONValue[0..1] { doc /* Source type: DateTime. Start of validity, ISO 8601; default now minus one minute. */ }
        ref item 'validBefore' : JSONValue[0..1] { doc /* Source type: DateTime. End of validity; default start plus the profile maximum (24 h for on, 365 days otherwise), never beyond the CA certificate. */ }
    }
    item def 'CertificateSigningResult' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Record identifier in the certificate register. */ }
        attribute 'certificateChain' : String[0..*] { doc /* Source type: Array<String>. Issued certificate and CA certificate, Base64 DER, leaf first. */ }
        attribute 'commonName' : String[0..1] { doc /* Source type: String. Subject common name. */ }
        attribute 'profile' : String[0..1] { doc /* Source type: String. Granted profile: ON_BOARDING, DEVICE, SYSTEM or OPERATOR. */ }
        attribute 'serialNumber' : String[0..1] { doc /* Source type: String. Serial number, decimal. */ }
        ref item 'validAfter' : JSONValue[0..1] { doc /* Source type: DateTime. Start of validity. */ }
        ref item 'validBefore' : JSONValue[0..1] { doc /* Source type: DateTime. End of validity. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'schemaVersion' : String[0..1] { doc /* Source type: String. 1.0. */ }
        attribute 'observationId' : String[0..1] { doc /* Source type: String. <System>-<type>-<epoch ms>-<sequence>. */ }
        attribute 'providerSystem' : String[0..1] { doc /* Source type: String. The publishing system. */ }
        attribute 'serviceInstance' : String[0..1] { doc /* Source type: String. Service instance, default the system name. */ }
        attribute 'observationType' : String[0..1] { doc /* Source type: String. echo, power, status. */ }
        attribute 'sourceTime' : String[0..1] { doc /* Source type: String. ISO 8601 UTC time with milliseconds. */ }
        attribute 'sequence' : Real[0..1] { doc /* Source type: Number. Sequence number per publisher. */ }
        ref item 'value' : JSONValue[0..1] { doc /* Source type: Object. Observed value, see the operations. */ }
        attribute 'unit' : String[0..1] { doc /* Source type: String. 1 for power and status; absent for echo. */ }
        attribute 'quality' : String[0..1] { doc /* Source type: String. good or uncertain. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. \texttt{{"publisherMode": "native"}}. */ }
    }
    item def 'RemovalResult' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'success' : Boolean[0..1] { doc /* Source type: Boolean. Whether a record was changed; 404 when false. */ }
    }
    item def 'RevocationRequest' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'certificate' : String[0..1] { doc /* Source type: String. Certificate to revoke, PEM or Base64 DER; must be issued by this cloud CA. */ }
    }
    item def 'RevocationResult' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Register record of the certificate. */ }
        attribute 'serialNumber' : String[0..1] { doc /* Source type: String. Serial number. */ }
        attribute 'revoked' : Boolean[0..1] { doc /* Source type: Boolean. Always true. */ }
        attribute 'changed' : Boolean[0..1] { doc /* Source type: Boolean. False if it was already revoked. */ }
    }
    item def 'TrustedKeyCheckRequest' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'publicKey' : String[0..1] { doc /* Source type: String. PEM SubjectPublicKeyInfo (alias publicKeyPem). */ }
    }
    item def 'TrustedKeyCheckResult' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'trusted' : Boolean[0..1] { doc /* Source type: Boolean. Whether the key is a trusted key within its validity. */ }
        ref item 'key' : 'TrustedKeyRecord'[0..1] { doc /* Source type: TrustedKeyRecord. The matching record, or null. */ }
    }
    item def 'TrustedKeyRecord' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Record identifier. */ }
        attribute 'public_key_pem' : String[0..1] { doc /* Source type: String. The key, PEM. */ }
        attribute 'description' : String[0..1] { doc /* Source type: String. Description. */ }
        attribute 'valid_after' : String[0..1] { doc /* Source type: String. Start of validity. */ }
        attribute 'valid_before' : String[0..1] { doc /* Source type: String. End of validity. */ }
        attribute 'created_at' : String[0..1] { doc /* Source type: String. Creation time. */ }
    }
    item def 'TrustedKeyRequest' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'publicKey' : String[0..1] { doc /* Source type: String. PEM SubjectPublicKeyInfo (alias publicKeyPem). */ }
        attribute 'description' : String[0..1] { doc /* Source type: String. Description. */ }
        ref item 'validAfter' : JSONValue[0..1] { doc /* Source type: DateTime. Start of validity; default now. */ }
        ref item 'validBefore' : JSONValue[0..1] { doc /* Source type: DateTime. End of validity; default now plus CERT_VALIDITY_DAYS. */ }
    }
    item def 'TrustedKeyResult' :> JSONMessage {
        doc /* Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Record identifier. */ }
        attribute 'trusted' : Boolean[0..1] { doc /* Source type: Boolean. Always true. */ }
    }
}
