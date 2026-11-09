library package ArrowheadAuthentication {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def Authentication_identity_SD :> ArrowheadService {
        @SD { MicroserviceName = "identity"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def Authentication_identity_HTTP_IDD :> Authentication_identity_SD {
        @IDD { MicroserviceName = "identity"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/authentication/identity";
        doc /* identity-login: POST /authentication/identity/login. Input: IdentityRequestDTO; output: IdentityLoginResponseDTO. Returns an identity token for the identified system */
        in item 'identity-loginRequest' : ArrowheadCoreData::'IdentityRequestDTO'[0..1];
        out item 'identity-loginResponse' : ArrowheadCoreData::'IdentityLoginResponseDTO'[0..1];
        doc /* identity-logout: POST /authentication/identity/logout. Input: IdentityRequestDTO; output: void. Logs out from the Local Cloud */
        in item 'identity-logoutRequest' : ArrowheadCoreData::'IdentityRequestDTO'[0..1];
        doc /* identity-change-credentials: POST /authentication/identity/change. Input: IdentityChangeRequestDTO; output: void. Changes the credentials (where it is possible) of the identified system */
        in item 'identity-change-credentialsRequest' : ArrowheadCoreData::'IdentityChangeRequestDTO'[0..1];
        doc /* identity-verify: GET /authentication/identity/verify/{token}. Input: None; output: IdentityVerifyResponseDTO. Returns information about a system (if verified) based on token */
        out item 'identity-verifyResponse' : ArrowheadCoreData::'IdentityVerifyResponseDTO'[0..1];
    }
    port def Authentication_identity_HTTP_IDD_Implementation :> Authentication_identity_HTTP_IDD { @IDD_Implementation; }
    action def 'Authentication_identity_http_identity-login_0' {
        doc /* Returns an identity token for the identified system. Wire method: POST; address: /authentication/identity/login. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/authentication/identity/login"; requestType = "IdentityRequestDTO"; responseType = "IdentityLoginResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"INTERNAL_SERVER_ERROR\", \"SERVICE_UNAVAILABLE\"]"; }
        in item 'request' : ArrowheadCoreData::'IdentityRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'IdentityLoginResponseDTO'[0..1];
    }
    action def 'Authentication_identity_http_identity-logout_1' {
        doc /* Logs out from the Local Cloud. Wire method: POST; address: /authentication/identity/logout. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/authentication/identity/logout"; requestType = "IdentityRequestDTO"; responseType = "void"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"INTERNAL_SERVER_ERROR\", \"SERVICE_UNAVAILABLE\"]"; }
        in item 'request' : ArrowheadCoreData::'IdentityRequestDTO'[0..1];
    }
    action def 'Authentication_identity_http_identity-change-credentials_2' {
        doc /* Changes the credentials (where it is possible) of the identified system. Wire method: POST; address: /authentication/identity/change. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/authentication/identity/change"; requestType = "IdentityChangeRequestDTO"; responseType = "void"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"INTERNAL_SERVER_ERROR\", \"SERVICE_UNAVAILABLE\"]"; }
        in item 'request' : ArrowheadCoreData::'IdentityChangeRequestDTO'[0..1];
    }
    action def 'Authentication_identity_http_identity-verify_3' {
        doc /* Returns information about a system (if verified) based on token. Wire method: GET; address: /authentication/identity/verify/{token}. Query parameters: []. Path parameters: ["token"]. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/authentication/identity/verify/{token}"; requestType = "unspecified"; responseType = "IdentityVerifyResponseDTO"; queryParameters = "[]"; pathParameters = "[\"token\"]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"INTERNAL_SERVER_ERROR\"]"; }
        out item 'response' : ArrowheadCoreData::'IdentityVerifyResponseDTO'[0..1];
    }
    port def Authentication_identity_MQTT_IDD :> Authentication_identity_SD {
        @IDD { MicroserviceName = "identity"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/authentication/identity/";
        doc /* identity-login: MQTT request/reply arrowhead/authentication/identity/identity-login. Input: IdentityRequestDTO; output: None.  */
        in item 'identity-loginRequest' : ArrowheadCoreData::'IdentityRequestDTO'[0..1];
        out item 'identity-loginResponse' : JSONMessage[0..1];
        doc /* identity-logout: MQTT request/reply arrowhead/authentication/identity/identity-logout. Input: IdentityRequestDTO; output: None.  */
        in item 'identity-logoutRequest' : ArrowheadCoreData::'IdentityRequestDTO'[0..1];
        out item 'identity-logoutResponse' : JSONMessage[0..1];
        doc /* identity-change-credentials: MQTT request/reply arrowhead/authentication/identity/identity-change-credentials. Input: IdentityChangeRequestDTO; output: None.  */
        in item 'identity-change-credentialsRequest' : ArrowheadCoreData::'IdentityChangeRequestDTO'[0..1];
        out item 'identity-change-credentialsResponse' : JSONMessage[0..1];
        doc /* identity-verify: MQTT request/reply arrowhead/authentication/identity/identity-verify. Input: String; output: None.  */
        in attribute 'identity-verifyRequest' : ScalarValues::String[0..1];
        out item 'identity-verifyResponse' : JSONMessage[0..1];
    }
    port def Authentication_identity_MQTT_IDD_Implementation :> Authentication_identity_MQTT_IDD { @IDD_Implementation; }
    action def 'Authentication_identity_mqtt_identity-login_0' {
        doc /* Operation identity-login. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/identity-login. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/identity-login"; requestType = "IdentityRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'IdentityRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Authentication_identity_mqtt_identity-logout_1' {
        doc /* Operation identity-logout. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/identity-logout. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/identity-logout"; requestType = "IdentityRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'IdentityRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Authentication_identity_mqtt_identity-change-credentials_2' {
        doc /* Operation identity-change-credentials. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/identity-change-credentials. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/identity-change-credentials"; requestType = "IdentityChangeRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'IdentityChangeRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Authentication_identity_mqtt_identity-verify_3' {
        doc /* Operation identity-verify. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/identity-verify. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/identity-verify"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def Authentication_identityManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "identityManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def Authentication_identityManagement_HTTP_IDD :> Authentication_identityManagement_SD {
        @IDD { MicroserviceName = "identityManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/authentication/mgmt";
        doc /* identity-mgmt-create: POST /authentication/mgmt/identities. Input: IdentityListMgmtCreateRequestDTO; output: IdentityListMgmtResponseDTO. Returns the created system identities (without sensitive information) */
        in item 'identity-mgmt-createRequest' : ArrowheadCoreData::'IdentityListMgmtCreateRequestDTO'[0..1];
        out item 'identity-mgmt-createResponse' : ArrowheadCoreData::'IdentityListMgmtResponseDTO'[0..1];
        doc /* identity-mgmt-update: PUT /authentication/mgmt/identities. Input: IdentityListMgmtUpdateRequestDTO; output: IdentityListMgmtResponseDTO. Returns the updated system identities (without sensitive information) */
        in item 'identity-mgmt-updateRequest' : ArrowheadCoreData::'IdentityListMgmtUpdateRequestDTO'[0..1];
        out item 'identity-mgmt-updateResponse' : ArrowheadCoreData::'IdentityListMgmtResponseDTO'[0..1];
        doc /* identity-mgmt-remove: DELETE /authentication/mgmt/identities. Input: None; output: void. Deletes the defined system identities */
        doc /* identity-mgmt-query: POST /authentication/mgmt/identities/query. Input: IdentityQueryRequestDTO; output: IdentityListMgmtResponseDTO. Returns the identifiable systems according to the query request */
        in item 'identity-mgmt-queryRequest' : ArrowheadCoreData::'IdentityQueryRequestDTO'[0..1];
        out item 'identity-mgmt-queryResponse' : ArrowheadCoreData::'IdentityListMgmtResponseDTO'[0..1];
        doc /* identity-mgmt-session-close: DELETE /authentication/mgmt/sessions. Input: None; output: void. Closes the defined system identities' active sessions */
        doc /* identity-mgmt-session-query: POST /authentication/mgmt/sessions. Input: IdentitySessionQueryRequestDTO; output: IdentitySessionListMgmtResponseDTO. Returns the active sessions according to the query request */
        in item 'identity-mgmt-session-queryRequest' : ArrowheadCoreData::'IdentitySessionQueryRequestDTO'[0..1];
        out item 'identity-mgmt-session-queryResponse' : ArrowheadCoreData::'IdentitySessionListMgmtResponseDTO'[0..1];
    }
    port def Authentication_identityManagement_HTTP_IDD_Implementation :> Authentication_identityManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Authentication_identityManagement_http_identity-mgmt-create_0' {
        doc /* Returns the created system identities (without sensitive information). Wire method: POST; address: /authentication/mgmt/identities. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/authentication/mgmt/identities"; requestType = "IdentityListMgmtCreateRequestDTO"; responseType = "IdentityListMgmtResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\", \"SERVICE_UNAVAILABLE\"]"; }
        in item 'request' : ArrowheadCoreData::'IdentityListMgmtCreateRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'IdentityListMgmtResponseDTO'[0..1];
    }
    action def 'Authentication_identityManagement_http_identity-mgmt-update_1' {
        doc /* Returns the updated system identities (without sensitive information). Wire method: PUT; address: /authentication/mgmt/identities. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/authentication/mgmt/identities"; requestType = "IdentityListMgmtUpdateRequestDTO"; responseType = "IdentityListMgmtResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\", \"SERVICE_UNAVAILABLE\"]"; }
        in item 'request' : ArrowheadCoreData::'IdentityListMgmtUpdateRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'IdentityListMgmtResponseDTO'[0..1];
    }
    action def 'Authentication_identityManagement_http_identity-mgmt-remove_2' {
        doc /* Deletes the defined system identities. Wire method: DELETE; address: /authentication/mgmt/identities. Query parameters: [{"name": "names", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/authentication/mgmt/identities"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"names\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\", \"SERVICE_UNAVAILABLE\"]"; }
    }
    action def 'Authentication_identityManagement_http_identity-mgmt-query_3' {
        doc /* Returns the identifiable systems according to the query request. Wire method: POST; address: /authentication/mgmt/identities/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/authentication/mgmt/identities/query"; requestType = "IdentityQueryRequestDTO"; responseType = "IdentityListMgmtResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'IdentityQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'IdentityListMgmtResponseDTO'[0..1];
    }
    action def 'Authentication_identityManagement_http_identity-mgmt-session-close_4' {
        doc /* Closes the defined system identities' active sessions. Wire method: DELETE; address: /authentication/mgmt/sessions. Query parameters: [{"name": "names", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/authentication/mgmt/sessions"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"names\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'Authentication_identityManagement_http_identity-mgmt-session-query_5' {
        doc /* Returns the active sessions according to the query request. Wire method: POST; address: /authentication/mgmt/sessions. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/authentication/mgmt/sessions"; requestType = "IdentitySessionQueryRequestDTO"; responseType = "IdentitySessionListMgmtResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'IdentitySessionQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'IdentitySessionListMgmtResponseDTO'[0..1];
    }
    port def Authentication_identityManagement_MQTT_IDD :> Authentication_identityManagement_SD {
        @IDD { MicroserviceName = "identityManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/authentication/identity/management/";
        doc /* identity-mgmt-create: MQTT request/reply arrowhead/authentication/identity/management/identity-mgmt-create. Input: IdentityListMgmtCreateRequestDTO; output: None.  */
        in item 'identity-mgmt-createRequest' : ArrowheadCoreData::'IdentityListMgmtCreateRequestDTO'[0..1];
        out item 'identity-mgmt-createResponse' : JSONMessage[0..1];
        doc /* identity-mgmt-update: MQTT request/reply arrowhead/authentication/identity/management/identity-mgmt-update. Input: IdentityListMgmtUpdateRequestDTO; output: None.  */
        in item 'identity-mgmt-updateRequest' : ArrowheadCoreData::'IdentityListMgmtUpdateRequestDTO'[0..1];
        out item 'identity-mgmt-updateResponse' : JSONMessage[0..1];
        doc /* identity-mgmt-remove: MQTT request/reply arrowhead/authentication/identity/management/identity-mgmt-remove. Input: List<String>; output: None.  */
        in attribute 'identity-mgmt-removeRequest' : ScalarValues::String[0..*];
        out item 'identity-mgmt-removeResponse' : JSONMessage[0..1];
        doc /* identity-mgmt-query: MQTT request/reply arrowhead/authentication/identity/management/identity-mgmt-query. Input: IdentityQueryRequestDTO; output: None.  */
        in item 'identity-mgmt-queryRequest' : ArrowheadCoreData::'IdentityQueryRequestDTO'[0..1];
        out item 'identity-mgmt-queryResponse' : JSONMessage[0..1];
        doc /* identity-mgmt-session-close: MQTT request/reply arrowhead/authentication/identity/management/identity-mgmt-session-close. Input: List<String>; output: None.  */
        in attribute 'identity-mgmt-session-closeRequest' : ScalarValues::String[0..*];
        out item 'identity-mgmt-session-closeResponse' : JSONMessage[0..1];
        doc /* identity-mgmt-session-query: MQTT request/reply arrowhead/authentication/identity/management/identity-mgmt-session-query. Input: IdentitySessionQueryRequestDTO; output: None.  */
        in item 'identity-mgmt-session-queryRequest' : ArrowheadCoreData::'IdentitySessionQueryRequestDTO'[0..1];
        out item 'identity-mgmt-session-queryResponse' : JSONMessage[0..1];
    }
    port def Authentication_identityManagement_MQTT_IDD_Implementation :> Authentication_identityManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'Authentication_identityManagement_mqtt_identity-mgmt-create_0' {
        doc /* Operation identity-mgmt-create. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/management/identity-mgmt-create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/management/identity-mgmt-create"; requestType = "IdentityListMgmtCreateRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'IdentityListMgmtCreateRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Authentication_identityManagement_mqtt_identity-mgmt-update_1' {
        doc /* Operation identity-mgmt-update. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/management/identity-mgmt-update. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/management/identity-mgmt-update"; requestType = "IdentityListMgmtUpdateRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'IdentityListMgmtUpdateRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Authentication_identityManagement_mqtt_identity-mgmt-remove_2' {
        doc /* Operation identity-mgmt-remove. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/management/identity-mgmt-remove. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/management/identity-mgmt-remove"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Authentication_identityManagement_mqtt_identity-mgmt-query_3' {
        doc /* Operation identity-mgmt-query. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/management/identity-mgmt-query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/management/identity-mgmt-query"; requestType = "IdentityQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'IdentityQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Authentication_identityManagement_mqtt_identity-mgmt-session-close_4' {
        doc /* Operation identity-mgmt-session-close. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/management/identity-mgmt-session-close. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/management/identity-mgmt-session-close"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Authentication_identityManagement_mqtt_identity-mgmt-session-query_5' {
        doc /* Operation identity-mgmt-session-query. Wire method: MQTT request/reply; address: arrowhead/authentication/identity/management/identity-mgmt-session-query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/identity/management/identity-mgmt-session-query"; requestType = "IdentitySessionQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'IdentitySessionQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def Authentication_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def Authentication_generalManagement_HTTP_IDD :> Authentication_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/authentication/general/mgmt";
        doc /* get-log: POST /authentication/general/mgmt/logs. Input: LogRequestDTO; output: LogEntryListResponseDTO. Returns the log entries */
        in item 'get-logRequest' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'get-logResponse' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
        doc /* get-config: GET /authentication/general/mgmt/get-config. Input: None; output: KeyValuesDTO. Returns a list of configurations */
        out item 'get-configResponse' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def Authentication_generalManagement_HTTP_IDD_Implementation :> Authentication_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Authentication_generalManagement_http_get-log_0' {
        doc /* Returns the log entries. Wire method: POST; address: /authentication/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/authentication/general/mgmt/logs"; requestType = "LogRequestDTO"; responseType = "LogEntryListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
    }
    action def 'Authentication_generalManagement_http_get-config_1' {
        doc /* Returns a list of configurations. Wire method: GET; address: /authentication/general/mgmt/get-config. Query parameters: [{"name": "keys", "type": "List<String>", "required": false, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/authentication/general/mgmt/get-config"; requestType = "unspecified"; responseType = "KeyValuesDTO"; queryParameters = "[{\"name\": \"keys\", \"type\": \"List<String>\", \"required\": false, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        out item 'response' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def Authentication_generalManagement_MQTT_IDD :> Authentication_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/authentication/general/management/";
        doc /* get-log: MQTT request/reply arrowhead/authentication/general/management/get-log. Input: LogRequestDTO; output: None.  */
        in item 'get-logRequest' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'get-logResponse' : JSONMessage[0..1];
        doc /* get-config: MQTT request/reply arrowhead/authentication/general/management/get-config. Input: List<String>; output: None.  */
        in attribute 'get-configRequest' : ScalarValues::String[0..*];
        out item 'get-configResponse' : JSONMessage[0..1];
    }
    port def Authentication_generalManagement_MQTT_IDD_Implementation :> Authentication_generalManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'Authentication_generalManagement_mqtt_get-log_0' {
        doc /* Operation get-log. Wire method: MQTT request/reply; address: arrowhead/authentication/general/management/get-log. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/general/management/get-log"; requestType = "LogRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Authentication_generalManagement_mqtt_get-config_1' {
        doc /* Operation get-config. Wire method: MQTT request/reply; address: arrowhead/authentication/general/management/get-config. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/authentication/general/management/get-config"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    port def Authentication_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def Authentication_sysMonitor_AMQP_IDD :> Authentication_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "../Arrowhead/ah5-core-java-spring/authentication/documentation/v5.2.2/Authentication-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.Authentication.Authentication";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, Authentication registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.Authentication.Authentication",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.Authentication.Authentication.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.Authentication.Authentication.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.Authentication.Authentication.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadCoreData::'Observation'[0..1];
    }
    port def Authentication_sysMonitor_AMQP_IDD_Implementation :> Authentication_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'Authentication_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Authentication.Authentication.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Authentication.Authentication.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'Authentication_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Authentication.Authentication.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Authentication.Authentication.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'Authentication_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Authentication.Authentication.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Authentication.Authentication.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    part def Authentication_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "Authentication"; FunctionalProperties = "This document describes the Authentication core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/authentication, including the local extensions of this workspace.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        doc /* This document describes the Authentication core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/authentication, including the local extensions of this workspace.
  \end{abstract} */
        port 'identity_SD' : Authentication_identity_SD;
        port 'identityManagement_SD' : Authentication_identityManagement_SD;
        port 'generalManagement_SD' : Authentication_generalManagement_SD;
        port 'sysMonitor_SD' : Authentication_sysMonitor_SD;
    }
    part def Authentication_SysDD :> Authentication_SysD {
        @SysDD;
        port 'identity_http' : Authentication_identity_HTTP_IDD;
        port 'identity_mqtt' : Authentication_identity_MQTT_IDD;
        port 'identityManagement_http' : Authentication_identityManagement_HTTP_IDD;
        port 'identityManagement_mqtt' : Authentication_identityManagement_MQTT_IDD;
        port 'generalManagement_http' : Authentication_generalManagement_HTTP_IDD;
        port 'generalManagement_mqtt' : Authentication_generalManagement_MQTT_IDD;
        port 'sysMonitor_amqp' : Authentication_sysMonitor_AMQP_IDD;
    }
    part def Authentication :> Authentication_SysDD {
        @System_Implementation;
    }
}
