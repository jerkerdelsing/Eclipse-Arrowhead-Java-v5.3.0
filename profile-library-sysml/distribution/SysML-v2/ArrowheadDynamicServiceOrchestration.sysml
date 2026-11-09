library package ArrowheadDynamicServiceOrchestration {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def DynamicServiceOrchestration_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def DynamicServiceOrchestration_generalManagement_HTTP_IDD :> DynamicServiceOrchestration_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/general/mgmt";
        doc /* get-log: POST /serviceorchestration/general/mgmt/logs. Input: LogRequestDTO; output: LogEntryListResponseDTO. Returns the log entries */
        in item 'get-logRequest' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'get-logResponse' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
        doc /* get-config: GET /serviceorchestration/general/mgmt/get-config. Input: None; output: KeyValuesDTO. Returns a list of configurations */
        out item 'get-configResponse' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def DynamicServiceOrchestration_generalManagement_HTTP_IDD_Implementation :> DynamicServiceOrchestration_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_generalManagement_http_get-log_0' {
        doc /* Returns the log entries. Wire method: POST; address: /serviceorchestration/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/general/mgmt/logs"; requestType = "LogRequestDTO"; responseType = "LogEntryListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
    }
    action def 'DynamicServiceOrchestration_generalManagement_http_get-config_1' {
        doc /* Returns a list of configurations. Wire method: GET; address: /serviceorchestration/general/mgmt/get-config. Query parameters: [{"name": "keys", "type": "List<String>", "required": false, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/serviceorchestration/general/mgmt/get-config"; requestType = "unspecified"; responseType = "KeyValuesDTO"; queryParameters = "[{\"name\": \"keys\", \"type\": \"List<String>\", \"required\": false, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        out item 'response' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def DynamicServiceOrchestration_generalManagement_MQTT_IDD :> DynamicServiceOrchestration_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceorchestration/general/management/";
        doc /* get-log: MQTT request/reply arrowhead/serviceorchestration/general/management/get-log. Input: LogRequestDTO; output: None.  */
        in item 'get-logRequest' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'get-logResponse' : JSONMessage[0..1];
        doc /* get-config: MQTT request/reply arrowhead/serviceorchestration/general/management/get-config. Input: List<String>; output: None.  */
        in attribute 'get-configRequest' : ScalarValues::String[0..*];
        out item 'get-configResponse' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_generalManagement_MQTT_IDD_Implementation :> DynamicServiceOrchestration_generalManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_generalManagement_mqtt_get-log_0' {
        doc /* Operation get-log. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/general/management/get-log. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/general/management/get-log"; requestType = "LogRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'DynamicServiceOrchestration_generalManagement_mqtt_get-config_1' {
        doc /* Operation get-config. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/general/management/get-config. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/general/management/get-config"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestration_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceOrchestration"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def DynamicServiceOrchestration_serviceOrchestration_HTTP_IDD :> DynamicServiceOrchestration_serviceOrchestration_SD {
        @IDD { MicroserviceName = "serviceOrchestration"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/orchestration";
        doc /* pull: POST /serviceorchestration/orchestration/pull. Input: OrchestrationRequestDTO; output: OrchestrationResponseDTO. Returns the dynamically produced orchestration results */
        in item 'pullRequest' : ArrowheadCoreData::'OrchestrationRequestDTO'[0..1];
        out item 'pullResponse' : ArrowheadCoreData::'OrchestrationResponseDTO'[0..1];
        doc /* subscribe: POST /serviceorchestration/orchestration/subscribe. Input: OrchestrationSubscriptionRequestDTO; output: ResponseEntity<String>. Returns a subscription id. Existing subscriptions will be overwritten. */
        in item 'subscribeRequest' : ArrowheadCoreData::'OrchestrationSubscriptionRequestDTO'[0..1];
        out attribute 'subscribeResponse' : ScalarValues::String[0..1];
        doc /* unsubscribe: DELETE /serviceorchestration/orchestration/unsubscribe/{id}. Input: None; output: ResponseEntity<Void>. Removes the subscription by the given id */
    }
    port def DynamicServiceOrchestration_serviceOrchestration_HTTP_IDD_Implementation :> DynamicServiceOrchestration_serviceOrchestration_HTTP_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_serviceOrchestration_http_pull_0' {
        doc /* Returns the dynamically produced orchestration results. Wire method: POST; address: /serviceorchestration/orchestration/pull. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR", "SERVICE_UNAVAILABLE"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/pull"; requestType = "OrchestrationRequestDTO"; responseType = "OrchestrationResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\", \"SERVICE_UNAVAILABLE\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationResponseDTO'[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestration_http_subscribe_1' {
        doc /* Returns a subscription id. Existing subscriptions will be overwritten.. Wire method: POST; address: /serviceorchestration/orchestration/subscribe. Query parameters: [{"name": "trigger", "type": "Boolean", "required": false, "default": "false"}]. Path parameters: []. Status codes: ["CREATED", "OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/subscribe"; requestType = "OrchestrationSubscriptionRequestDTO"; responseType = "ResponseEntity<String>"; queryParameters = "[{\"name\": \"trigger\", \"type\": \"Boolean\", \"required\": false, \"default\": \"false\"}]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionRequestDTO'[0..1];
        out attribute 'response' : ScalarValues::String[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestration_http_unsubscribe_2' {
        doc /* Removes the subscription by the given id. Wire method: DELETE; address: /serviceorchestration/orchestration/unsubscribe/{id}. Query parameters: []. Path parameters: ["id"]. Status codes: ["OK", "NO_CONTENT", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceorchestration/orchestration/unsubscribe/{id}"; requestType = "unspecified"; responseType = "ResponseEntity<Void>"; queryParameters = "[]"; pathParameters = "[\"id\"]"; statusCodes = "[\"OK\", \"NO_CONTENT\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    port def DynamicServiceOrchestration_serviceOrchestration_MQTT_IDD :> DynamicServiceOrchestration_serviceOrchestration_SD {
        @IDD { MicroserviceName = "serviceOrchestration"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceorchestration/orchestration/";
        doc /* pull: MQTT request/reply arrowhead/serviceorchestration/orchestration/pull. Input: OrchestrationRequestDTO; output: None.  */
        in item 'pullRequest' : ArrowheadCoreData::'OrchestrationRequestDTO'[0..1];
        out item 'pullResponse' : JSONMessage[0..1];
        doc /* subscribe: MQTT request/reply arrowhead/serviceorchestration/orchestration/subscribe. Input: OrchestrationSubscriptionRequestDTO; output: None.  */
        in item 'subscribeRequest' : ArrowheadCoreData::'OrchestrationSubscriptionRequestDTO'[0..1];
        out item 'subscribeResponse' : JSONMessage[0..1];
        doc /* unsubscribe: MQTT request/reply arrowhead/serviceorchestration/orchestration/unsubscribe. Input: String; output: None.  */
        in attribute 'unsubscribeRequest' : ScalarValues::String[0..1];
        out item 'unsubscribeResponse' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestration_MQTT_IDD_Implementation :> DynamicServiceOrchestration_serviceOrchestration_MQTT_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_serviceOrchestration_mqtt_pull_0' {
        doc /* Operation pull. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/pull. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/pull"; requestType = "OrchestrationRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestration_mqtt_subscribe_1' {
        doc /* Operation subscribe. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/subscribe. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/subscribe"; requestType = "OrchestrationSubscriptionRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestration_mqtt_unsubscribe_2' {
        doc /* Operation unsubscribe. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/unsubscribe. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/unsubscribe"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationPushManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceOrchestrationPushManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def DynamicServiceOrchestration_serviceOrchestrationPushManagement_HTTP_IDD :> DynamicServiceOrchestration_serviceOrchestrationPushManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationPushManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/orchestration/mgmt/push";
        doc /* subscribe: POST /serviceorchestration/orchestration/mgmt/push/subscribe. Input: OrchestrationSubscriptionListRequestDTO; output: OrchestrationSubscriptionListResponseDTO. Returns the created subscription records. Existing ones will be overwritten. */
        in item 'subscribeRequest' : ArrowheadCoreData::'OrchestrationSubscriptionListRequestDTO'[0..1];
        out item 'subscribeResponse' : ArrowheadCoreData::'OrchestrationSubscriptionListResponseDTO'[0..1];
        doc /* unsubscribe: DELETE /serviceorchestration/orchestration/mgmt/push/unsubscribe. Input: None; output: void. Removes the specified subscriptions */
        doc /* trigger: POST /serviceorchestration/orchestration/mgmt/push/trigger. Input: OrchestrationPushTriggerDTO; output: OrchestrationPushJobListResponseDTO. Returns the created push orchestration job records */
        in item 'triggerRequest' : ArrowheadCoreData::'OrchestrationPushTriggerDTO'[0..1];
        out item 'triggerResponse' : ArrowheadCoreData::'OrchestrationPushJobListResponseDTO'[0..1];
        doc /* query: POST /serviceorchestration/orchestration/mgmt/push/query. Input: OrchestrationSubscriptionQueryRequestDTO; output: OrchestrationSubscriptionListResponseDTO. Returns the required subcription records. */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationSubscriptionQueryRequestDTO'[0..1];
        out item 'queryResponse' : ArrowheadCoreData::'OrchestrationSubscriptionListResponseDTO'[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationPushManagement_HTTP_IDD_Implementation :> DynamicServiceOrchestration_serviceOrchestrationPushManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPushManagement_http_subscribe_0' {
        doc /* Returns the created subscription records. Existing ones will be overwritten.. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/push/subscribe. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/push/subscribe"; requestType = "OrchestrationSubscriptionListRequestDTO"; responseType = "OrchestrationSubscriptionListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationSubscriptionListResponseDTO'[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPushManagement_http_unsubscribe_1' {
        doc /* Removes the specified subscriptions. Wire method: DELETE; address: /serviceorchestration/orchestration/mgmt/push/unsubscribe. Query parameters: [{"name": "ids", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceorchestration/orchestration/mgmt/push/unsubscribe"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"ids\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPushManagement_http_trigger_2' {
        doc /* Returns the created push orchestration job records. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/push/trigger. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/push/trigger"; requestType = "OrchestrationPushTriggerDTO"; responseType = "OrchestrationPushJobListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationPushTriggerDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationPushJobListResponseDTO'[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPushManagement_http_query_3' {
        doc /* Returns the required subcription records.. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/push/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/push/query"; requestType = "OrchestrationSubscriptionQueryRequestDTO"; responseType = "OrchestrationSubscriptionListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationSubscriptionListResponseDTO'[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationPushManagement_MQTT_IDD :> DynamicServiceOrchestration_serviceOrchestrationPushManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationPushManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceorchestration/orchestration/management/push/";
        doc /* subscribe: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/push/subscribe. Input: OrchestrationSubscriptionListRequestDTO; output: None.  */
        in item 'subscribeRequest' : ArrowheadCoreData::'OrchestrationSubscriptionListRequestDTO'[0..1];
        out item 'subscribeResponse' : JSONMessage[0..1];
        doc /* trigger: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/push/trigger. Input: OrchestrationPushTriggerDTO; output: None.  */
        in item 'triggerRequest' : ArrowheadCoreData::'OrchestrationPushTriggerDTO'[0..1];
        out item 'triggerResponse' : JSONMessage[0..1];
        doc /* unsubscribe: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/push/unsubscribe. Input: List<String>; output: None.  */
        in attribute 'unsubscribeRequest' : ScalarValues::String[0..*];
        out item 'unsubscribeResponse' : JSONMessage[0..1];
        doc /* query: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/push/query. Input: OrchestrationSubscriptionQueryRequestDTO; output: None.  */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationSubscriptionQueryRequestDTO'[0..1];
        out item 'queryResponse' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationPushManagement_MQTT_IDD_Implementation :> DynamicServiceOrchestration_serviceOrchestrationPushManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPushManagement_mqtt_subscribe_0' {
        doc /* Operation subscribe. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/push/subscribe. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/push/subscribe"; requestType = "OrchestrationSubscriptionListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPushManagement_mqtt_trigger_1' {
        doc /* Operation trigger. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/push/trigger. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/push/trigger"; requestType = "OrchestrationPushTriggerDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationPushTriggerDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPushManagement_mqtt_unsubscribe_2' {
        doc /* Operation unsubscribe. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/push/unsubscribe. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/push/unsubscribe"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPushManagement_mqtt_query_3' {
        doc /* Operation query. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/push/query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/push/query"; requestType = "OrchestrationSubscriptionQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationLockManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceOrchestrationLockManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def DynamicServiceOrchestration_serviceOrchestrationLockManagement_HTTP_IDD :> DynamicServiceOrchestration_serviceOrchestrationLockManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationLockManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/orchestration/mgmt/lock";
        doc /* query: POST /serviceorchestration/orchestration/mgmt/lock/query. Input: OrchestrationLockQueryRequestDTO; output: OrchestrationLockListResponseDTO. Returns the lock records according to the given filters */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationLockQueryRequestDTO'[0..1];
        out item 'queryResponse' : ArrowheadCoreData::'OrchestrationLockListResponseDTO'[0..1];
        doc /* remove: DELETE /serviceorchestration/orchestration/mgmt/lock/remove/{owner}. Input: None; output: void. Deletes the existing lock entries */
        doc /* create: POST /serviceorchestration/orchestration/mgmt/lock/create. Input: OrchestrationLockListRequestDTO; output: OrchestrationLockListResponseDTO. Returns the created lock records */
        in item 'createRequest' : ArrowheadCoreData::'OrchestrationLockListRequestDTO'[0..1];
        out item 'createResponse' : ArrowheadCoreData::'OrchestrationLockListResponseDTO'[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationLockManagement_HTTP_IDD_Implementation :> DynamicServiceOrchestration_serviceOrchestrationLockManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_serviceOrchestrationLockManagement_http_query_0' {
        doc /* Returns the lock records according to the given filters. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/lock/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/lock/query"; requestType = "OrchestrationLockQueryRequestDTO"; responseType = "OrchestrationLockListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationLockQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationLockListResponseDTO'[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationLockManagement_http_remove_1' {
        doc /* Deletes the existing lock entries. Wire method: DELETE; address: /serviceorchestration/orchestration/mgmt/lock/remove/{owner}. Query parameters: [{"name": "instanceIds", "type": "List<String>", "required": true, "default": null}]. Path parameters: ["owner"]. Status codes: ["OK", "NO_CONTENT", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceorchestration/orchestration/mgmt/lock/remove/{owner}"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"instanceIds\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[\"owner\"]"; statusCodes = "[\"OK\", \"NO_CONTENT\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationLockManagement_http_create_2' {
        doc /* Returns the created lock records. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/lock/create. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/lock/create"; requestType = "OrchestrationLockListRequestDTO"; responseType = "OrchestrationLockListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationLockListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationLockListResponseDTO'[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationLockManagement_MQTT_IDD :> DynamicServiceOrchestration_serviceOrchestrationLockManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationLockManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceorchestration/orchestration/management/lock/";
        doc /* create: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/lock/create. Input: OrchestrationLockListRequestDTO; output: None.  */
        in item 'createRequest' : ArrowheadCoreData::'OrchestrationLockListRequestDTO'[0..1];
        out item 'createResponse' : JSONMessage[0..1];
        doc /* query: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/lock/query. Input: OrchestrationLockQueryRequestDTO; output: None.  */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationLockQueryRequestDTO'[0..1];
        out item 'queryResponse' : JSONMessage[0..1];
        doc /* remove: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/lock/remove. Input: List<String>; output: None.  */
        in attribute 'removeRequest' : ScalarValues::String[0..*];
        out item 'removeResponse' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationLockManagement_MQTT_IDD_Implementation :> DynamicServiceOrchestration_serviceOrchestrationLockManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_serviceOrchestrationLockManagement_mqtt_create_0' {
        doc /* Operation create. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/lock/create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/lock/create"; requestType = "OrchestrationLockListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationLockListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationLockManagement_mqtt_query_1' {
        doc /* Operation query. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/lock/query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/lock/query"; requestType = "OrchestrationLockQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationLockQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationLockManagement_mqtt_remove_2' {
        doc /* Operation remove. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/lock/remove. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/lock/remove"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceOrchestrationHistoryManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_HTTP_IDD :> DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationHistoryManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/orchestration/mgmt/history";
        doc /* query: POST /serviceorchestration/orchestration/mgmt/history/query. Input: OrchestrationHistoryQueryRequestDTO; output: OrchestrationHistoryResponseDTO. Returns the orchestration job history records according to the given filters. */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationHistoryQueryRequestDTO'[0..1];
        out item 'queryResponse' : ArrowheadCoreData::'OrchestrationHistoryResponseDTO'[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_HTTP_IDD_Implementation :> DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_http_query_0' {
        doc /* Returns the orchestration job history records according to the given filters.. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/history/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/history/query"; requestType = "OrchestrationHistoryQueryRequestDTO"; responseType = "OrchestrationHistoryResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationHistoryQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationHistoryResponseDTO'[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_MQTT_IDD :> DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationHistoryManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceorchestration/orchestration/management/history/";
        doc /* query: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/history/query. Input: OrchestrationHistoryQueryRequestDTO; output: None.  */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationHistoryQueryRequestDTO'[0..1];
        out item 'queryResponse' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_MQTT_IDD_Implementation :> DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_mqtt_query_0' {
        doc /* Operation query. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/history/query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/history/query"; requestType = "OrchestrationHistoryQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationHistoryQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationPlantManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceOrchestrationPlantManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def DynamicServiceOrchestration_serviceOrchestrationPlantManagement_HTTP_IDD :> DynamicServiceOrchestration_serviceOrchestrationPlantManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationPlantManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/mgmt/plant-rules";
        doc /* query: POST /serviceorchestration/mgmt/plant-rules/query. Input: None; output: OrchestrationPlantRulesDTO. Returns the active engineered plant snapshot */
        out item 'queryResponse' : ArrowheadCoreData::'OrchestrationPlantRulesDTO'[0..1];
        doc /* replace: PUT /serviceorchestration/mgmt/plant-rules/replace. Input: OrchestrationPlantRulesDTO; output: OrchestrationPlantRulesDTO. Atomically replaces all active-plant scopes; empty rules clears the snapshot */
        in item 'replaceRequest' : ArrowheadCoreData::'OrchestrationPlantRulesDTO'[0..1];
        out item 'replaceResponse' : ArrowheadCoreData::'OrchestrationPlantRulesDTO'[0..1];
    }
    port def DynamicServiceOrchestration_serviceOrchestrationPlantManagement_HTTP_IDD_Implementation :> DynamicServiceOrchestration_serviceOrchestrationPlantManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPlantManagement_http_query_0' {
        doc /* Returns the active engineered plant snapshot. Wire method: POST; address: /serviceorchestration/mgmt/plant-rules/query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/mgmt/plant-rules/query"; requestType = "unspecified"; responseType = "OrchestrationPlantRulesDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'OrchestrationPlantRulesDTO'[0..1];
    }
    action def 'DynamicServiceOrchestration_serviceOrchestrationPlantManagement_http_replace_1' {
        doc /* Atomically replaces all active-plant scopes; empty rules clears the snapshot. Wire method: PUT; address: /serviceorchestration/mgmt/plant-rules/replace. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/serviceorchestration/mgmt/plant-rules/replace"; requestType = "OrchestrationPlantRulesDTO"; responseType = "OrchestrationPlantRulesDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationPlantRulesDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationPlantRulesDTO'[0..1];
    }
    port def DynamicServiceOrchestration_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def DynamicServiceOrchestration_sysMonitor_AMQP_IDD :> DynamicServiceOrchestration_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "../Arrowhead/ah5-core-java-spring/serviceorchestration-dynamic/documentation/v5.2.2/DynamicServiceOrchestration-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, DynamicServiceOrchestration registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadCoreData::'Observation'[0..1];
    }
    port def DynamicServiceOrchestration_sysMonitor_AMQP_IDD_Implementation :> DynamicServiceOrchestration_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'DynamicServiceOrchestration_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'DynamicServiceOrchestration_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'DynamicServiceOrchestration_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.DynamicServiceOrchestration.DynamicServiceOrchestration.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    part def DynamicServiceOrchestration_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "DynamicServiceOrchestration"; FunctionalProperties = "This document describes the DynamicServiceOrchestration core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/serviceorchestration-dynamic, including the local extensions of this workspace.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        doc /* This document describes the DynamicServiceOrchestration core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/serviceorchestration-dynamic, including the local extensions of this workspace.
  \end{abstract} */
        port 'generalManagement_SD' : DynamicServiceOrchestration_generalManagement_SD;
        port 'serviceOrchestration_SD' : DynamicServiceOrchestration_serviceOrchestration_SD;
        port 'serviceOrchestrationPushManagement_SD' : DynamicServiceOrchestration_serviceOrchestrationPushManagement_SD;
        port 'serviceOrchestrationLockManagement_SD' : DynamicServiceOrchestration_serviceOrchestrationLockManagement_SD;
        port 'serviceOrchestrationHistoryManagement_SD' : DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_SD;
        port 'serviceOrchestrationPlantManagement_SD' : DynamicServiceOrchestration_serviceOrchestrationPlantManagement_SD;
        port 'sysMonitor_SD' : DynamicServiceOrchestration_sysMonitor_SD;
    }
    part def DynamicServiceOrchestration_SysDD :> DynamicServiceOrchestration_SysD {
        @SysDD;
        port 'generalManagement_http' : DynamicServiceOrchestration_generalManagement_HTTP_IDD;
        port 'generalManagement_mqtt' : DynamicServiceOrchestration_generalManagement_MQTT_IDD;
        port 'serviceOrchestration_http' : DynamicServiceOrchestration_serviceOrchestration_HTTP_IDD;
        port 'serviceOrchestration_mqtt' : DynamicServiceOrchestration_serviceOrchestration_MQTT_IDD;
        port 'serviceOrchestrationPushManagement_http' : DynamicServiceOrchestration_serviceOrchestrationPushManagement_HTTP_IDD;
        port 'serviceOrchestrationPushManagement_mqtt' : DynamicServiceOrchestration_serviceOrchestrationPushManagement_MQTT_IDD;
        port 'serviceOrchestrationLockManagement_http' : DynamicServiceOrchestration_serviceOrchestrationLockManagement_HTTP_IDD;
        port 'serviceOrchestrationLockManagement_mqtt' : DynamicServiceOrchestration_serviceOrchestrationLockManagement_MQTT_IDD;
        port 'serviceOrchestrationHistoryManagement_http' : DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_HTTP_IDD;
        port 'serviceOrchestrationHistoryManagement_mqtt' : DynamicServiceOrchestration_serviceOrchestrationHistoryManagement_MQTT_IDD;
        port 'serviceOrchestrationPlantManagement_http' : DynamicServiceOrchestration_serviceOrchestrationPlantManagement_HTTP_IDD;
        port 'sysMonitor_amqp' : DynamicServiceOrchestration_sysMonitor_AMQP_IDD;
    }
    part def DynamicServiceOrchestration :> DynamicServiceOrchestration_SysDD {
        @System_Implementation;
    }
}
