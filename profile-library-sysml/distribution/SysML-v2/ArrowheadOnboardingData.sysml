library package ArrowheadOnboardingData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'Address' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'type' : String[0..1] { doc /* Source type: String. Address type: HOSTNAME, IPV4, IPV6 or MAC. */ }
        attribute 'address' : String[0..1] { doc /* Source type: String. One network address (host name, IPv4, IPv6 or MAC address). */ }
    }
    item def 'CertificateResult' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'certificate' : String[0..1] { doc /* Source type: String. The certificate, Base64 DER. */ }
        attribute 'certificateFormat' : String[0..1] { doc /* Source type: String. Encoding of the certificate, e.g. X.509. */ }
        attribute 'certificateType' : String[0..1] { doc /* Source type: String. Certificate profile: ON_BOARDING, DEVICE or SYSTEM. */ }
        attribute 'publicKey' : String[0..1] { doc /* Source type: String. Public key, Base64 X.509 SubjectPublicKeyInfo. */ }
        attribute 'privateKey' : String[0..1] { doc /* Source type: String. Generated private key (only without a CSR, returned once). */ }
        ref item 'expiresAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC expiry time; no value means no expiry. */ }
    }
    item def 'CsrRequest' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'certificateSigningRequest' : String[0..1] { doc /* Source type: String. PKCS\textbackslash{}\#10 certificate signing request, Base64 DER. */ }
    }
    item def 'DeviceRequest' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'certificateSigningRequest' : String[0..1] { doc /* Source type: String. PKCS\textbackslash{}\#10 certificate signing request, Base64 DER. */ }
        attribute 'addresses' : String[0..*] { doc /* Source type: Array<String>. Network addresses of the device for the ServiceRegistry. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. Device metadata for the ServiceRegistry. */ }
    }
    item def 'DeviceResponse' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'deviceName' : String[0..1] { doc /* Source type: String. The onboarded device. */ }
        attribute 'certificateChain' : String[0..*] { doc /* Source type: Array<String>. Issued certificate first, then the issuing CA (Base64 DER). */ }
        ref item 'deviceCertificate' : 'CertificateResult'[0..1] { doc /* Source type: CertificateResult. The issued device certificate (de). */ }
        attribute 'approvedSystems' : String[0..*] { doc /* Source type: Array<String>. Systems the operator approved for the device. */ }
        attribute 'nextStep' : String[0..1] { doc /* Source type: String. The operation the caller performs next in the onboarding procedure. */ }
    }
    item def 'DeviceResponseDTO' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String. Device name. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. Free-form metadata (JSON object). */ }
        ref item 'addresses' : 'Address'[0..*] { doc /* Source type: Array<Address>. Network addresses with their types. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'EnrollmentList' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'EnrollmentView'[0..*] { doc /* Source type: Array<EnrollmentView>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Number of enrollments. */ }
    }
    item def 'EnrollmentRequest' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bootstrapFingerprint' : String[0..1] { doc /* Source type: String. SHA-256 fingerprint of the approved bootstrap certificate. */ }
        attribute 'systems' : String[0..*] { doc /* Source type: Array<String>. Systems the device may onboard. */ }
    }
    item def 'EnrollmentView' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'deviceName' : String[0..1] { doc /* Source type: String. Name of the device hosting the system. */ }
        attribute 'bootstrapFingerprint' : String[0..1] { doc /* Source type: String. SHA-256 fingerprint of the approved bootstrap certificate. */ }
        attribute 'approvedSystems' : String[0..*] { doc /* Source type: Array<String>. Systems the operator approved for the device. */ }
        attribute 'state' : String[0..1] { doc /* Source type: String. Enrollment state: APPROVED, DEVICE_REGISTERED or OFFBOARDED. */ }
        attribute 'deviceCertificateId' : Real[0..1] { doc /* Source type: Number. CertificateAuthority record of the device certificate. */ }
        ref item 'deviceCertificateExpiresAt' : JSONValue[0..1] { doc /* Source type: DateTime. Expiry of the device certificate. */ }
        ref item 'systems' : 'SystemEnrollmentView'[0..*] { doc /* Source type: Array<SystemEnrollmentView>. Systems onboarded for the device. */ }
        attribute 'approvedBy' : String[0..1] { doc /* Source type: String. Operator who approved the device. */ }
        ref item 'approvedAt' : JSONValue[0..1] { doc /* Source type: DateTime. Time of the approval. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
        attribute 'lastMessage' : String[0..1] { doc /* Source type: String. Last status message of the enrollment. */ }
    }
    item def 'ErrorMessage' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String. Error text, if the event is an error. */ }
        attribute 'errorCode' : Real[0..1] { doc /* Source type: Number.  */ }
        ref item 'exceptionType' : JSONValue[0..1] { doc /* Source type: ExceptionType.  */ }
        attribute 'origin' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'KeyValues' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'map' : JSONValue[0..1] { doc /* Source type: Object<String>. The configuration keys and values; secrets are not returned. */ }
    }
    item def 'LogEntry' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'logId' : String[0..1] { doc /* Source type: String. Identifier of the log entry. */ }
        ref item 'entryDate' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the log entry. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Log level, e.g. INFO, WARN, ERROR; as filter, the minimum level. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Text of the log entry or status message of the job. */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String. Stack trace or exception text of the log entry, if any. */ }
    }
    item def 'LogEntryListResponse' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'LogEntry'[0..*] { doc /* Source type: Array<LogEntry>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'LogRequest' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        ref item 'from' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries from this UTC time. */ }
        ref item 'to' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries until this UTC time. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Minimum log level to return. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
    }
    item def 'NameRequest' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'commonName' : String[0..1] { doc /* Source type: String. Common name of the requested certificate, <Name>.<Cloud>.<Organization>.arrowhead.eu. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
    item def 'OffboardingReport' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'deviceName' : String[0..1] { doc /* Source type: String. The off-boarded device. */ }
        attribute 'completed' : String[0..*] { doc /* Source type: Array<String>. Off-boarding steps that succeeded. */ }
        attribute 'failed' : String[0..*] { doc /* Source type: Array<String>. Off-boarding steps that failed. */ }
    }
    item def 'Page' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'page' : Real[0..1] { doc /* Source type: Number. Zero-based page number. */ }
        attribute 'size' : Real[0..1] { doc /* Source type: Number. Number of records per page (at most max.page.size, default 1000). */ }
        attribute 'direction' : String[0..1] { doc /* Source type: String. Sort direction, ASC or DESC. */ }
        attribute 'sortField' : String[0..1] { doc /* Source type: String. Field to sort by. */ }
    }
    item def 'Response' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'certificateChain' : String[0..*] { doc /* Source type: Array<String>. Issued certificate first, then the issuing CA (Base64 DER). */ }
        ref item 'onboardingCertificate' : 'CertificateResult'[0..1] { doc /* Source type: CertificateResult. The issued onboarding certificate (on). */ }
        ref item 'services' : 'ServiceInstanceResponse'[0..*] { doc /* Source type: Array<ServiceInstanceResponse>. Service instances of the core services the caller needs next. */ }
        attribute 'nextStep' : String[0..1] { doc /* Source type: String. The operation the caller performs next in the onboarding procedure. */ }
    }
    item def 'ServiceDefinitionResponse' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String. Service definition name. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'ServiceInstanceInterfaceResponse' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'templateName' : String[0..1] { doc /* Source type: String. Name of the interface template, snake_case (e.g. generic_https). */ }
        attribute 'protocol' : String[0..1] { doc /* Source type: String. Protocol of the interface, e.g. http, https, mqtt, mqtts. */ }
        attribute 'policy' : String[0..1] { doc /* Source type: String. Security policy of the interface: NONE, CERT_AUTH or one of the token policies. */ }
        ref item 'properties' : JSONValue[0..1] { doc /* Source type: Object. Interface properties defined by the template, e.g. accessAddresses, accessPort, basePath, operations. */ }
    }
    item def 'ServiceInstanceResponse' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'instanceId' : String[0..1] { doc /* Source type: String. Service instance identifier \texttt{<System>\textbar{}<serviceDefinition>\textbar{}<version>}, or policy instance identifier. */ }
        ref item 'provider' : 'SystemResponseDTO'[0..1] { doc /* Source type: SystemResponseDTO. The provider system. */ }
        ref item 'serviceDefinition' : JSONValue[0..1] { doc /* Source type: \mref[ServiceDefinitionResponse]{ServiceDefinitionResponse}. The service definition. */ }
        ref item 'version' : JSONValue[0..1] { doc /* Source type: Version. Version of the service instance. */ }
        ref item 'expiresAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC expiry time; no value means no expiry. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. Free-form metadata (JSON object). */ }
        ref item 'interfaces' : JSONValue[0..*] { doc /* Source type: Array<\mref[ServiceInstanceInterfaceResponse]{ServiceInstanceInterfaceResponse}>. Service interfaces offered by the instance. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'SystemEnrollmentView' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String. Name of the system, PascalCase (e.g. TemperatureSensor). */ }
        attribute 'certificateId' : Real[0..1] { doc /* Source type: Number. CertificateAuthority record of the system certificate. */ }
        ref item 'certificateExpiresAt' : JSONValue[0..1] { doc /* Source type: DateTime. Expiry of the system certificate. */ }
        ref item 'issuedAt' : JSONValue[0..1] { doc /* Source type: DateTime. Time the certificate was issued. */ }
    }
    item def 'SystemRequest' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String. System to onboard; must be approved for the device. */ }
        attribute 'certificateSigningRequest' : String[0..1] { doc /* Source type: String. PKCS\textbackslash{}\#10 certificate signing request, Base64 DER. */ }
    }
    item def 'SystemResponse' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String. The onboarded system. */ }
        attribute 'deviceName' : String[0..1] { doc /* Source type: String. Name of the device hosting the system. */ }
        attribute 'certificateChain' : String[0..*] { doc /* Source type: Array<String>. Issued certificate first, then the issuing CA (Base64 DER). */ }
        ref item 'systemCertificate' : 'CertificateResult'[0..1] { doc /* Source type: CertificateResult. The issued system certificate (sy). */ }
        attribute 'authenticationMethod' : String[0..1] { doc /* Source type: String. Authentication method of the identity, PASSWORD. */ }
        ref item 'credentials' : JSONValue[0..1] { doc /* Source type: Object<String>. Identity password of the new Authentication identity (returned once). */ }
        ref item 'services' : 'ServiceInstanceResponse'[0..*] { doc /* Source type: Array<ServiceInstanceResponse>. Service instances of the core services the caller needs next. */ }
        attribute 'nextStep' : String[0..1] { doc /* Source type: String. The operation the caller performs next in the onboarding procedure. */ }
    }
    item def 'SystemResponseDTO' :> JSONMessage {
        doc /* Source: ah5-Onboarding-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String. System name. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. Free-form metadata (JSON object). */ }
        ref item 'version' : JSONValue[0..1] { doc /* Source type: Version. Version of the system. */ }
        ref item 'addresses' : 'Address'[0..*] { doc /* Source type: Array<Address>. Network addresses with their types. */ }
        ref item 'device' : 'DeviceResponseDTO'[0..1] { doc /* Source type: DeviceResponseDTO. The device hosting the system, if registered. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
}
