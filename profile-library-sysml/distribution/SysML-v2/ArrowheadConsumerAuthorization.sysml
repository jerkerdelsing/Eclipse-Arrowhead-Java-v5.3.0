library package ArrowheadConsumerAuthorization {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def ConsumerAuthorization_authorization_SD :> ArrowheadService {
        @SD { MicroserviceName = "authorization"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ConsumerAuthorization_authorization_HTTP_IDD :> ConsumerAuthorization_authorization_SD {
        @IDD { MicroserviceName = "authorization"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/consumerauthorization/authorization";
        doc /* grant: POST /consumerauthorization/authorization/grant. Input: AuthorizationGrantRequestDTO; output: ResponseEntity<AuthorizationPolicyResponseDTO>. Returns the created authorization policy */
        in item 'grantRequest' : ArrowheadCoreData::'AuthorizationGrantRequestDTO'[0..1];
        out item 'grantResponse' : ArrowheadCoreData::'AuthorizationPolicyResponseDTO'[0..1];
        doc /* revoke: DELETE /consumerauthorization/authorization/revoke/{instanceId}. Input: None; output: ResponseEntity<Void>. Deletes policy instance entry if exists */
        doc /* lookup: POST /consumerauthorization/authorization/lookup. Input: AuthorizationLookupRequestDTO; output: AuthorizationPolicyListResponseDTO. Returns the requester created authorization policies according to the given filters */
        in item 'lookupRequest' : ArrowheadCoreData::'AuthorizationLookupRequestDTO'[0..1];
        out item 'lookupResponse' : ArrowheadCoreData::'AuthorizationPolicyListResponseDTO'[0..1];
        doc /* verify: POST /consumerauthorization/authorization/verify. Input: AuthorizationVerifyRequestDTO; output: boolean. Returns whether a consumer has access to a specified service/service operation/event type. */
        in item 'verifyRequest' : ArrowheadCoreData::'AuthorizationVerifyRequestDTO'[0..1];
        out attribute 'verifyResponse' : ScalarValues::Boolean[0..1];
    }
    port def ConsumerAuthorization_authorization_HTTP_IDD_Implementation :> ConsumerAuthorization_authorization_HTTP_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_authorization_http_grant_0' {
        doc /* Returns the created authorization policy. Wire method: POST; address: /consumerauthorization/authorization/grant. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization/grant"; requestType = "AuthorizationGrantRequestDTO"; responseType = "ResponseEntity<AuthorizationPolicyResponseDTO>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationGrantRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'AuthorizationPolicyResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_authorization_http_revoke_1' {
        doc /* Deletes policy instance entry if exists. Wire method: DELETE; address: /consumerauthorization/authorization/revoke/{instanceId}. Query parameters: []. Path parameters: ["instanceId"]. Status codes: ["OK", "NO_CONTENT", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/consumerauthorization/authorization/revoke/{instanceId}"; requestType = "unspecified"; responseType = "ResponseEntity<Void>"; queryParameters = "[]"; pathParameters = "[\"instanceId\"]"; statusCodes = "[\"OK\", \"NO_CONTENT\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'ConsumerAuthorization_authorization_http_lookup_2' {
        doc /* Returns the requester created authorization policies according to the given filters. Wire method: POST; address: /consumerauthorization/authorization/lookup. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization/lookup"; requestType = "AuthorizationLookupRequestDTO"; responseType = "AuthorizationPolicyListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationLookupRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'AuthorizationPolicyListResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_authorization_http_verify_3' {
        doc /* Returns whether a consumer has access to a specified service/service operation/event type.. Wire method: POST; address: /consumerauthorization/authorization/verify. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization/verify"; requestType = "AuthorizationVerifyRequestDTO"; responseType = "boolean"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationVerifyRequestDTO'[0..1];
        out attribute 'response' : ScalarValues::Boolean[0..1];
    }
    port def ConsumerAuthorization_authorization_MQTT_IDD :> ConsumerAuthorization_authorization_SD {
        @IDD { MicroserviceName = "authorization"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/consumer-authorization/authorization/";
        doc /* grant: MQTT request/reply arrowhead/consumer-authorization/authorization/grant. Input: AuthorizationGrantRequestDTO; output: None.  */
        in item 'grantRequest' : ArrowheadCoreData::'AuthorizationGrantRequestDTO'[0..1];
        out item 'grantResponse' : JSONMessage[0..1];
        doc /* revoke: MQTT request/reply arrowhead/consumer-authorization/authorization/revoke. Input: String; output: None.  */
        in attribute 'revokeRequest' : ScalarValues::String[0..1];
        out item 'revokeResponse' : JSONMessage[0..1];
        doc /* lookup: MQTT request/reply arrowhead/consumer-authorization/authorization/lookup. Input: AuthorizationLookupRequestDTO; output: None.  */
        in item 'lookupRequest' : ArrowheadCoreData::'AuthorizationLookupRequestDTO'[0..1];
        out item 'lookupResponse' : JSONMessage[0..1];
        doc /* verify: MQTT request/reply arrowhead/consumer-authorization/authorization/verify. Input: AuthorizationVerifyRequestDTO; output: None.  */
        in item 'verifyRequest' : ArrowheadCoreData::'AuthorizationVerifyRequestDTO'[0..1];
        out item 'verifyResponse' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_authorization_MQTT_IDD_Implementation :> ConsumerAuthorization_authorization_MQTT_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_authorization_mqtt_grant_0' {
        doc /* Operation grant. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization/grant. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization/grant"; requestType = "AuthorizationGrantRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationGrantRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorization_mqtt_revoke_1' {
        doc /* Operation revoke. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization/revoke. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization/revoke"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorization_mqtt_lookup_2' {
        doc /* Operation lookup. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization/lookup. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization/lookup"; requestType = "AuthorizationLookupRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationLookupRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorization_mqtt_verify_3' {
        doc /* Operation verify. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization/verify. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization/verify"; requestType = "AuthorizationVerifyRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationVerifyRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_authorizationToken_SD :> ArrowheadService {
        @SD { MicroserviceName = "authorizationToken"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ConsumerAuthorization_authorizationToken_HTTP_IDD :> ConsumerAuthorization_authorizationToken_SD {
        @IDD { MicroserviceName = "authorizationToken"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/consumerauthorization/authorization-token";
        doc /* generate: POST /consumerauthorization/authorization-token/generate. Input: AuthorizationTokenGenerationRequestDTO; output: AuthorizationTokenGenerationResponseDTO. Returns a generated authorization token and its parameters. */
        in item 'generateRequest' : ArrowheadCoreData::'AuthorizationTokenGenerationRequestDTO'[0..1];
        out item 'generateResponse' : ArrowheadCoreData::'AuthorizationTokenGenerationResponseDTO'[0..1];
        doc /* verify: GET /consumerauthorization/authorization-token/token/verify/{token}. Input: None; output: AuthorizationTokenVerifyResponseDTO. Verifies the provided token and returns the belonged details. */
        out item 'verifyResponse' : ArrowheadCoreData::'AuthorizationTokenVerifyResponseDTO'[0..1];
        doc /* get-public-key: GET /consumerauthorization/authorization-token/public-key. Input: None; output: String. Returns the public key of the server if any. */
        out attribute 'get-public-keyResponse' : ScalarValues::String[0..1];
        doc /* register-encryption-key: POST /consumerauthorization/authorization-token/encryption-key. Input: AuthorizationEncryptionKeyRegistrationRequestDTO; output: String. Registers the provided encryption key and returns the belonged auxiliary if any. */
        in item 'register-encryption-keyRequest' : ArrowheadCoreData::'AuthorizationEncryptionKeyRegistrationRequestDTO'[0..1];
        out attribute 'register-encryption-keyResponse' : ScalarValues::String[0..1];
        doc /* unregister-encryption-key: DELETE /consumerauthorization/authorization-token/encryption-key. Input: None; output: ResponseEntity<Void>. Unregisters the encryption key belonged to the requester system. */
    }
    port def ConsumerAuthorization_authorizationToken_HTTP_IDD_Implementation :> ConsumerAuthorization_authorizationToken_HTTP_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_authorizationToken_http_generate_0' {
        doc /* Returns a generated authorization token and its parameters.. Wire method: POST; address: /consumerauthorization/authorization-token/generate. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization-token/generate"; requestType = "AuthorizationTokenGenerationRequestDTO"; responseType = "AuthorizationTokenGenerationResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationTokenGenerationRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'AuthorizationTokenGenerationResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_authorizationToken_http_verify_1' {
        doc /* Verifies the provided token and returns the belonged details.. Wire method: GET; address: /consumerauthorization/authorization-token/token/verify/{token}. Query parameters: []. Path parameters: ["token"]. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/consumerauthorization/authorization-token/token/verify/{token}"; requestType = "unspecified"; responseType = "AuthorizationTokenVerifyResponseDTO"; queryParameters = "[]"; pathParameters = "[\"token\"]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        out item 'response' : ArrowheadCoreData::'AuthorizationTokenVerifyResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_authorizationToken_http_get-public-key_2' {
        doc /* Returns the public key of the server if any.. Wire method: GET; address: /consumerauthorization/authorization-token/public-key. Query parameters: []. Path parameters: []. Status codes: ["OK", "UNAUTHORIZED", "FORBIDDEN", "NOT_FOUND", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/consumerauthorization/authorization-token/public-key"; requestType = "unspecified"; responseType = "String"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"NOT_FOUND\", \"INTERNAL_SERVER_ERROR\"]"; }
        out attribute 'response' : ScalarValues::String[0..1];
    }
    action def 'ConsumerAuthorization_authorizationToken_http_register-encryption-key_3' {
        doc /* Registers the provided encryption key and returns the belonged auxiliary if any.. Wire method: POST; address: /consumerauthorization/authorization-token/encryption-key. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization-token/encryption-key"; requestType = "AuthorizationEncryptionKeyRegistrationRequestDTO"; responseType = "String"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationEncryptionKeyRegistrationRequestDTO'[0..1];
        out attribute 'response' : ScalarValues::String[0..1];
    }
    action def 'ConsumerAuthorization_authorizationToken_http_unregister-encryption-key_4' {
        doc /* Unregisters the encryption key belonged to the requester system.. Wire method: DELETE; address: /consumerauthorization/authorization-token/encryption-key. Query parameters: []. Path parameters: []. Status codes: ["OK", "NO_CONTENT", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/consumerauthorization/authorization-token/encryption-key"; requestType = "unspecified"; responseType = "ResponseEntity<Void>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"NO_CONTENT\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    port def ConsumerAuthorization_authorizationToken_MQTT_IDD :> ConsumerAuthorization_authorizationToken_SD {
        @IDD { MicroserviceName = "authorizationToken"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/consumer-authorization/authorization-token/";
        doc /* generate: MQTT request/reply arrowhead/consumer-authorization/authorization-token/generate. Input: AuthorizationTokenGenerationRequestDTO; output: None.  */
        in item 'generateRequest' : ArrowheadCoreData::'AuthorizationTokenGenerationRequestDTO'[0..1];
        out item 'generateResponse' : JSONMessage[0..1];
        doc /* verify: MQTT request/reply arrowhead/consumer-authorization/authorization-token/verify. Input: String; output: None.  */
        in attribute 'verifyRequest' : ScalarValues::String[0..1];
        out item 'verifyResponse' : JSONMessage[0..1];
        doc /* get-public-key: MQTT request/reply arrowhead/consumer-authorization/authorization-token/get-public-key. Input: None; output: None.  */
        out item 'get-public-keyResponse' : JSONMessage[0..1];
        doc /* register-encryption-key: MQTT request/reply arrowhead/consumer-authorization/authorization-token/register-encryption-key. Input: AuthorizationEncryptionKeyRegistrationRequestDTO; output: None.  */
        in item 'register-encryption-keyRequest' : ArrowheadCoreData::'AuthorizationEncryptionKeyRegistrationRequestDTO'[0..1];
        out item 'register-encryption-keyResponse' : JSONMessage[0..1];
        doc /* unregister-encryption-key: MQTT request/reply arrowhead/consumer-authorization/authorization-token/unregister-encryption-key. Input: None; output: None.  */
        out item 'unregister-encryption-keyResponse' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_authorizationToken_MQTT_IDD_Implementation :> ConsumerAuthorization_authorizationToken_MQTT_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_authorizationToken_mqtt_generate_0' {
        doc /* Operation generate. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/generate. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/generate"; requestType = "AuthorizationTokenGenerationRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationTokenGenerationRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationToken_mqtt_verify_1' {
        doc /* Operation verify. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/verify. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/verify"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationToken_mqtt_get-public-key_2' {
        doc /* Operation get-public-key. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/get-public-key. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/get-public-key"; requestType = "unspecified"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationToken_mqtt_register-encryption-key_3' {
        doc /* Operation register-encryption-key. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/register-encryption-key. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/register-encryption-key"; requestType = "AuthorizationEncryptionKeyRegistrationRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationEncryptionKeyRegistrationRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationToken_mqtt_unregister-encryption-key_4' {
        doc /* Operation unregister-encryption-key. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/unregister-encryption-key. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/unregister-encryption-key"; requestType = "unspecified"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_authorizationManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "authorizationManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ConsumerAuthorization_authorizationManagement_HTTP_IDD :> ConsumerAuthorization_authorizationManagement_SD {
        @IDD { MicroserviceName = "authorizationManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/consumerauthorization/authorization/mgmt";
        doc /* grant-policies: POST /consumerauthorization/authorization/mgmt/grant. Input: AuthorizationMgmtGrantListRequestDTO; output: AuthorizationPolicyListResponseDTO. Returns the created authorization policies */
        in item 'grant-policiesRequest' : ArrowheadCoreData::'AuthorizationMgmtGrantListRequestDTO'[0..1];
        out item 'grant-policiesResponse' : ArrowheadCoreData::'AuthorizationPolicyListResponseDTO'[0..1];
        doc /* revoke-policies: DELETE /consumerauthorization/authorization/mgmt/revoke. Input: None; output: void. Deletes policy instance entries if exists */
        doc /* query-policies: POST /consumerauthorization/authorization/mgmt/query. Input: AuthorizationQueryRequestDTO; output: AuthorizationPolicyListResponseDTO. Returns authorization policies according to the given filters */
        in item 'query-policiesRequest' : ArrowheadCoreData::'AuthorizationQueryRequestDTO'[0..1];
        out item 'query-policiesResponse' : ArrowheadCoreData::'AuthorizationPolicyListResponseDTO'[0..1];
        doc /* check-policies: POST /consumerauthorization/authorization/mgmt/check. Input: AuthorizationVerifyListRequestDTO; output: AuthorizationVerifyListResponseDTO. Returns whether consumers have access to specified services/service operations/event types. */
        in item 'check-policiesRequest' : ArrowheadCoreData::'AuthorizationVerifyListRequestDTO'[0..1];
        out item 'check-policiesResponse' : ArrowheadCoreData::'AuthorizationVerifyListResponseDTO'[0..1];
    }
    port def ConsumerAuthorization_authorizationManagement_HTTP_IDD_Implementation :> ConsumerAuthorization_authorizationManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_authorizationManagement_http_grant-policies_0' {
        doc /* Returns the created authorization policies. Wire method: POST; address: /consumerauthorization/authorization/mgmt/grant. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization/mgmt/grant"; requestType = "AuthorizationMgmtGrantListRequestDTO"; responseType = "AuthorizationPolicyListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationMgmtGrantListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'AuthorizationPolicyListResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_authorizationManagement_http_revoke-policies_1' {
        doc /* Deletes policy instance entries if exists. Wire method: DELETE; address: /consumerauthorization/authorization/mgmt/revoke. Query parameters: [{"name": "instanceIds", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/consumerauthorization/authorization/mgmt/revoke"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"instanceIds\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'ConsumerAuthorization_authorizationManagement_http_query-policies_2' {
        doc /* Returns authorization policies according to the given filters. Wire method: POST; address: /consumerauthorization/authorization/mgmt/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization/mgmt/query"; requestType = "AuthorizationQueryRequestDTO"; responseType = "AuthorizationPolicyListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'AuthorizationPolicyListResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_authorizationManagement_http_check-policies_3' {
        doc /* Returns whether consumers have access to specified services/service operations/event types.. Wire method: POST; address: /consumerauthorization/authorization/mgmt/check. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization/mgmt/check"; requestType = "AuthorizationVerifyListRequestDTO"; responseType = "AuthorizationVerifyListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationVerifyListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'AuthorizationVerifyListResponseDTO'[0..1];
    }
    port def ConsumerAuthorization_authorizationManagement_MQTT_IDD :> ConsumerAuthorization_authorizationManagement_SD {
        @IDD { MicroserviceName = "authorizationManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/consumer-authorization/authorization/management/";
        doc /* grant-policies: MQTT request/reply arrowhead/consumer-authorization/authorization/management/grant-policies. Input: AuthorizationMgmtGrantListRequestDTO; output: None.  */
        in item 'grant-policiesRequest' : ArrowheadCoreData::'AuthorizationMgmtGrantListRequestDTO'[0..1];
        out item 'grant-policiesResponse' : JSONMessage[0..1];
        doc /* revoke-policies: MQTT request/reply arrowhead/consumer-authorization/authorization/management/revoke-policies. Input: List<String>; output: None.  */
        in attribute 'revoke-policiesRequest' : ScalarValues::String[0..*];
        out item 'revoke-policiesResponse' : JSONMessage[0..1];
        doc /* query-policies: MQTT request/reply arrowhead/consumer-authorization/authorization/management/query-policies. Input: AuthorizationQueryRequestDTO; output: None.  */
        in item 'query-policiesRequest' : ArrowheadCoreData::'AuthorizationQueryRequestDTO'[0..1];
        out item 'query-policiesResponse' : JSONMessage[0..1];
        doc /* check-policies: MQTT request/reply arrowhead/consumer-authorization/authorization/management/check-policies. Input: AuthorizationVerifyListRequestDTO; output: None.  */
        in item 'check-policiesRequest' : ArrowheadCoreData::'AuthorizationVerifyListRequestDTO'[0..1];
        out item 'check-policiesResponse' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_authorizationManagement_MQTT_IDD_Implementation :> ConsumerAuthorization_authorizationManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_authorizationManagement_mqtt_grant-policies_0' {
        doc /* Operation grant-policies. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization/management/grant-policies. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization/management/grant-policies"; requestType = "AuthorizationMgmtGrantListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationMgmtGrantListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationManagement_mqtt_revoke-policies_1' {
        doc /* Operation revoke-policies. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization/management/revoke-policies. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization/management/revoke-policies"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationManagement_mqtt_query-policies_2' {
        doc /* Operation query-policies. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization/management/query-policies. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization/management/query-policies"; requestType = "AuthorizationQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationManagement_mqtt_check-policies_3' {
        doc /* Operation check-policies. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization/management/check-policies. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization/management/check-policies"; requestType = "AuthorizationVerifyListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationVerifyListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_authorizationTokenManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "authorizationTokenManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ConsumerAuthorization_authorizationTokenManagement_HTTP_IDD :> ConsumerAuthorization_authorizationTokenManagement_SD {
        @IDD { MicroserviceName = "authorizationTokenManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/consumerauthorization/authorization/mgmt/token";
        doc /* generate-tokens: POST /consumerauthorization/authorization/mgmt/token/generate. Input: AuthorizationTokenGenerationMgmtListRequestDTO; output: AuthorizationTokenMgmtListResponseDTO. Returns the generated authorization tokens and their parameters. */
        in item 'generate-tokensRequest' : ArrowheadCoreData::'AuthorizationTokenGenerationMgmtListRequestDTO'[0..1];
        out item 'generate-tokensResponse' : ArrowheadCoreData::'AuthorizationTokenMgmtListResponseDTO'[0..1];
        doc /* query-tokens: POST /consumerauthorization/authorization/mgmt/token/query. Input: AuthorizationTokenQueryRequestDTO; output: AuthorizationTokenMgmtListResponseDTO. Returns the matching token entries according to the given filters. */
        in item 'query-tokensRequest' : ArrowheadCoreData::'AuthorizationTokenQueryRequestDTO'[0..1];
        out item 'query-tokensResponse' : ArrowheadCoreData::'AuthorizationTokenMgmtListResponseDTO'[0..1];
        doc /* revoke-tokens: DELETE /consumerauthorization/authorization/mgmt/token/revoke. Input: None; output: void. Deletes token entries by tokenReference if exists */
        doc /* add-encryption-keys: POST /consumerauthorization/authorization/mgmt/token/encryption-key. Input: AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO; output: AuthorizationMgmtEncryptionKeyListResponseDTO. Returns the created encryption key entries. */
        in item 'add-encryption-keysRequest' : ArrowheadCoreData::'AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO'[0..1];
        out item 'add-encryption-keysResponse' : ArrowheadCoreData::'AuthorizationMgmtEncryptionKeyListResponseDTO'[0..1];
        doc /* remove-encryption-keys: DELETE /consumerauthorization/authorization/mgmt/token/encryption-key. Input: None; output: void. Deletes encryption key entries if exists */
    }
    port def ConsumerAuthorization_authorizationTokenManagement_HTTP_IDD_Implementation :> ConsumerAuthorization_authorizationTokenManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_authorizationTokenManagement_http_generate-tokens_0' {
        doc /* Returns the generated authorization tokens and their parameters.. Wire method: POST; address: /consumerauthorization/authorization/mgmt/token/generate. Query parameters: [{"name": "unbound", "type": "boolean", "required": false, "default": "false"}]. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization/mgmt/token/generate"; requestType = "AuthorizationTokenGenerationMgmtListRequestDTO"; responseType = "AuthorizationTokenMgmtListResponseDTO"; queryParameters = "[{\"name\": \"unbound\", \"type\": \"boolean\", \"required\": false, \"default\": \"false\"}]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationTokenGenerationMgmtListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'AuthorizationTokenMgmtListResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_authorizationTokenManagement_http_query-tokens_1' {
        doc /* Returns the matching token entries according to the given filters.. Wire method: POST; address: /consumerauthorization/authorization/mgmt/token/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization/mgmt/token/query"; requestType = "AuthorizationTokenQueryRequestDTO"; responseType = "AuthorizationTokenMgmtListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationTokenQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'AuthorizationTokenMgmtListResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_authorizationTokenManagement_http_revoke-tokens_2' {
        doc /* Deletes token entries by tokenReference if exists. Wire method: DELETE; address: /consumerauthorization/authorization/mgmt/token/revoke. Query parameters: [{"name": "tokenReferences", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/consumerauthorization/authorization/mgmt/token/revoke"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"tokenReferences\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'ConsumerAuthorization_authorizationTokenManagement_http_add-encryption-keys_3' {
        doc /* Returns the created encryption key entries.. Wire method: POST; address: /consumerauthorization/authorization/mgmt/token/encryption-key. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/authorization/mgmt/token/encryption-key"; requestType = "AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO"; responseType = "AuthorizationMgmtEncryptionKeyListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'AuthorizationMgmtEncryptionKeyListResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_authorizationTokenManagement_http_remove-encryption-keys_4' {
        doc /* Deletes encryption key entries if exists. Wire method: DELETE; address: /consumerauthorization/authorization/mgmt/token/encryption-key. Query parameters: [{"name": "systemNames", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/consumerauthorization/authorization/mgmt/token/encryption-key"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"systemNames\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    port def ConsumerAuthorization_authorizationTokenManagement_MQTT_IDD :> ConsumerAuthorization_authorizationTokenManagement_SD {
        @IDD { MicroserviceName = "authorizationTokenManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/consumer-authorization/authorization-token/management/";
        doc /* generate-tokens: MQTT request/reply arrowhead/consumer-authorization/authorization-token/management/generate-tokens. Input: AuthorizationTokenGenerationMgmtListRequestDTO; output: None.  */
        in item 'generate-tokensRequest' : ArrowheadCoreData::'AuthorizationTokenGenerationMgmtListRequestDTO'[0..1];
        out item 'generate-tokensResponse' : JSONMessage[0..1];
        doc /* query-tokens: MQTT request/reply arrowhead/consumer-authorization/authorization-token/management/query-tokens. Input: AuthorizationTokenQueryRequestDTO; output: None.  */
        in item 'query-tokensRequest' : ArrowheadCoreData::'AuthorizationTokenQueryRequestDTO'[0..1];
        out item 'query-tokensResponse' : JSONMessage[0..1];
        doc /* revoke-tokens: MQTT request/reply arrowhead/consumer-authorization/authorization-token/management/revoke-tokens. Input: List<String>; output: None.  */
        in attribute 'revoke-tokensRequest' : ScalarValues::String[0..*];
        out item 'revoke-tokensResponse' : JSONMessage[0..1];
        doc /* add-encryption-keys: MQTT request/reply arrowhead/consumer-authorization/authorization-token/management/add-encryption-keys. Input: AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO; output: None.  */
        in item 'add-encryption-keysRequest' : ArrowheadCoreData::'AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO'[0..1];
        out item 'add-encryption-keysResponse' : JSONMessage[0..1];
        doc /* remove-encryption-keys: MQTT request/reply arrowhead/consumer-authorization/authorization-token/management/remove-encryption-keys. Input: List<String>; output: None.  */
        in attribute 'remove-encryption-keysRequest' : ScalarValues::String[0..*];
        out item 'remove-encryption-keysResponse' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_authorizationTokenManagement_MQTT_IDD_Implementation :> ConsumerAuthorization_authorizationTokenManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_authorizationTokenManagement_mqtt_generate-tokens_0' {
        doc /* Operation generate-tokens. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/management/generate-tokens. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/management/generate-tokens"; requestType = "AuthorizationTokenGenerationMgmtListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationTokenGenerationMgmtListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationTokenManagement_mqtt_query-tokens_1' {
        doc /* Operation query-tokens. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/management/query-tokens. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/management/query-tokens"; requestType = "AuthorizationTokenQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationTokenQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationTokenManagement_mqtt_revoke-tokens_2' {
        doc /* Operation revoke-tokens. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/management/revoke-tokens. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/management/revoke-tokens"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationTokenManagement_mqtt_add-encryption-keys_3' {
        doc /* Operation add-encryption-keys. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/management/add-encryption-keys. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/management/add-encryption-keys"; requestType = "AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'AuthorizationMgmtEncryptionKeyRegistrationListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_authorizationTokenManagement_mqtt_remove-encryption-keys_4' {
        doc /* Operation remove-encryption-keys. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/authorization-token/management/remove-encryption-keys. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/authorization-token/management/remove-encryption-keys"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ConsumerAuthorization_generalManagement_HTTP_IDD :> ConsumerAuthorization_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/consumerauthorization/general/mgmt";
        doc /* get-log: POST /consumerauthorization/general/mgmt/logs. Input: LogRequestDTO; output: LogEntryListResponseDTO. Returns the log entries */
        in item 'get-logRequest' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'get-logResponse' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
        doc /* get-config: GET /consumerauthorization/general/mgmt/get-config. Input: None; output: KeyValuesDTO. Returns a list of configurations */
        out item 'get-configResponse' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def ConsumerAuthorization_generalManagement_HTTP_IDD_Implementation :> ConsumerAuthorization_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_generalManagement_http_get-log_0' {
        doc /* Returns the log entries. Wire method: POST; address: /consumerauthorization/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/consumerauthorization/general/mgmt/logs"; requestType = "LogRequestDTO"; responseType = "LogEntryListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
    }
    action def 'ConsumerAuthorization_generalManagement_http_get-config_1' {
        doc /* Returns a list of configurations. Wire method: GET; address: /consumerauthorization/general/mgmt/get-config. Query parameters: [{"name": "keys", "type": "List<String>", "required": false, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/consumerauthorization/general/mgmt/get-config"; requestType = "unspecified"; responseType = "KeyValuesDTO"; queryParameters = "[{\"name\": \"keys\", \"type\": \"List<String>\", \"required\": false, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        out item 'response' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def ConsumerAuthorization_generalManagement_MQTT_IDD :> ConsumerAuthorization_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/consumer-authorization/general/management/";
        doc /* get-log: MQTT request/reply arrowhead/consumer-authorization/general/management/get-log. Input: LogRequestDTO; output: None.  */
        in item 'get-logRequest' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'get-logResponse' : JSONMessage[0..1];
        doc /* get-config: MQTT request/reply arrowhead/consumer-authorization/general/management/get-config. Input: List<String>; output: None.  */
        in attribute 'get-configRequest' : ScalarValues::String[0..*];
        out item 'get-configResponse' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_generalManagement_MQTT_IDD_Implementation :> ConsumerAuthorization_generalManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_generalManagement_mqtt_get-log_0' {
        doc /* Operation get-log. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/general/management/get-log. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/general/management/get-log"; requestType = "LogRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ConsumerAuthorization_generalManagement_mqtt_get-config_1' {
        doc /* Operation get-config. Wire method: MQTT request/reply; address: arrowhead/consumer-authorization/general/management/get-config. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/consumer-authorization/general/management/get-config"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    port def ConsumerAuthorization_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ConsumerAuthorization_sysMonitor_AMQP_IDD :> ConsumerAuthorization_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "../Arrowhead/ah5-core-java-spring/consumer-authorization/documentation/v5.2.2/ConsumerAuthorization-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.ConsumerAuthorization.ConsumerAuthorization";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, ConsumerAuthorization registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

\begin{lstlisting}[caption={Registered service instance (TestCloud).}]
{
  "serviceDefinitionName": "sysMonitor",
  "version": "1.0.0",
  "metadata": {
    "contract": "SysMonitor-AMQP-IDD-5.2.2",
    "schemaVersion": "1.0",
    "publisherMode": "native",
    "observationTypes": [
      "echo",
      "power",
      "status"
    ],
    "delivery": "persistent, publisher-confirmed"
  },
  "interfaces": [
    {
      "templateName": "generic_amqps",
      "protocol": "amqps",
      "policy": "CERT_AUTH",
      "properties": {
        "accessAddresses": [
          "amqp-broker"
        ],
        "accessPort": 5671,
        "exchange": "arrowhead",
        "baseRoutingKey": "TestCloud.sysmonitor.ConsumerAuthorization.ConsumerAuthorization",
        "operations": [
          "echo",
          "power",
          "status"
        ]
      }
    }
  ]
}
\end{lstlisting} */
        doc /* echo: PUBLISH <Cloud>.sysmonitor.ConsumerAuthorization.ConsumerAuthorization.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.ConsumerAuthorization.ConsumerAuthorization.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.ConsumerAuthorization.ConsumerAuthorization.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadCoreData::'Observation'[0..1];
    }
    port def ConsumerAuthorization_sysMonitor_AMQP_IDD_Implementation :> ConsumerAuthorization_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'ConsumerAuthorization_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.ConsumerAuthorization.ConsumerAuthorization.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.ConsumerAuthorization.ConsumerAuthorization.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'ConsumerAuthorization_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.ConsumerAuthorization.ConsumerAuthorization.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.ConsumerAuthorization.ConsumerAuthorization.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'ConsumerAuthorization_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.ConsumerAuthorization.ConsumerAuthorization.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.ConsumerAuthorization.ConsumerAuthorization.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    part def ConsumerAuthorization_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "ConsumerAuthorization"; FunctionalProperties = "This document describes the ConsumerAuthorization core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/consumer-authorization, including the local extensions of this workspace.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        doc /* This document describes the ConsumerAuthorization core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/consumer-authorization, including the local extensions of this workspace.
  \end{abstract} */
        port 'authorization_SD' : ConsumerAuthorization_authorization_SD;
        port 'authorizationToken_SD' : ConsumerAuthorization_authorizationToken_SD;
        port 'authorizationManagement_SD' : ConsumerAuthorization_authorizationManagement_SD;
        port 'authorizationTokenManagement_SD' : ConsumerAuthorization_authorizationTokenManagement_SD;
        port 'generalManagement_SD' : ConsumerAuthorization_generalManagement_SD;
        port 'sysMonitor_SD' : ConsumerAuthorization_sysMonitor_SD;
    }
    part def ConsumerAuthorization_SysDD :> ConsumerAuthorization_SysD {
        @SysDD;
        port 'authorization_http' : ConsumerAuthorization_authorization_HTTP_IDD;
        port 'authorization_mqtt' : ConsumerAuthorization_authorization_MQTT_IDD;
        port 'authorizationToken_http' : ConsumerAuthorization_authorizationToken_HTTP_IDD;
        port 'authorizationToken_mqtt' : ConsumerAuthorization_authorizationToken_MQTT_IDD;
        port 'authorizationManagement_http' : ConsumerAuthorization_authorizationManagement_HTTP_IDD;
        port 'authorizationManagement_mqtt' : ConsumerAuthorization_authorizationManagement_MQTT_IDD;
        port 'authorizationTokenManagement_http' : ConsumerAuthorization_authorizationTokenManagement_HTTP_IDD;
        port 'authorizationTokenManagement_mqtt' : ConsumerAuthorization_authorizationTokenManagement_MQTT_IDD;
        port 'generalManagement_http' : ConsumerAuthorization_generalManagement_HTTP_IDD;
        port 'generalManagement_mqtt' : ConsumerAuthorization_generalManagement_MQTT_IDD;
        port 'sysMonitor_amqp' : ConsumerAuthorization_sysMonitor_AMQP_IDD;
    }
    part def ConsumerAuthorization :> ConsumerAuthorization_SysDD {
        @System_Implementation;
    }
}
