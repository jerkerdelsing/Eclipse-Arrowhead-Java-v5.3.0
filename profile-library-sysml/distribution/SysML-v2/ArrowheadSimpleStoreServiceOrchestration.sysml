library package ArrowheadSimpleStoreServiceOrchestration {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def SimpleStoreServiceOrchestration_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def SimpleStoreServiceOrchestration_generalManagement_HTTP_IDD :> SimpleStoreServiceOrchestration_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/general/mgmt";
        doc /* get-log: POST /serviceorchestration/general/mgmt/logs. Input: LogRequestDTO; output: LogEntryListResponseDTO. Returns the log entries */
        in item 'get-logRequest' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'get-logResponse' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
        doc /* get-config: GET /serviceorchestration/general/mgmt/get-config. Input: None; output: KeyValuesDTO. Returns a list of configurations */
        out item 'get-configResponse' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def SimpleStoreServiceOrchestration_generalManagement_HTTP_IDD_Implementation :> SimpleStoreServiceOrchestration_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_generalManagement_http_get-log_0' {
        doc /* Returns the log entries. Wire method: POST; address: /serviceorchestration/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/general/mgmt/logs"; requestType = "LogRequestDTO"; responseType = "LogEntryListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_generalManagement_http_get-config_1' {
        doc /* Returns a list of configurations. Wire method: GET; address: /serviceorchestration/general/mgmt/get-config. Query parameters: [{"name": "keys", "type": "List<String>", "required": false, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/serviceorchestration/general/mgmt/get-config"; requestType = "unspecified"; responseType = "KeyValuesDTO"; queryParameters = "[{\"name\": \"keys\", \"type\": \"List<String>\", \"required\": false, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        out item 'response' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def SimpleStoreServiceOrchestration_generalManagement_MQTT_IDD :> SimpleStoreServiceOrchestration_generalManagement_SD {
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
    port def SimpleStoreServiceOrchestration_generalManagement_MQTT_IDD_Implementation :> SimpleStoreServiceOrchestration_generalManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_generalManagement_mqtt_get-log_0' {
        doc /* Operation get-log. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/general/management/get-log. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/general/management/get-log"; requestType = "LogRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_generalManagement_mqtt_get-config_1' {
        doc /* Operation get-config. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/general/management/get-config. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/general/management/get-config"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestration_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceOrchestration"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestration_HTTP_IDD :> SimpleStoreServiceOrchestration_serviceOrchestration_SD {
        @IDD { MicroserviceName = "serviceOrchestration"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/orchestration";
        doc /* pull: POST /serviceorchestration/orchestration/pull. Input: OrchestrationRequestDTO; output: OrchestrationResponseDTO. Returns the orchestration results from the simple store */
        in item 'pullRequest' : ArrowheadCoreData::'OrchestrationRequestDTO'[0..1];
        out item 'pullResponse' : ArrowheadCoreData::'OrchestrationResponseDTO'[0..1];
        doc /* subscribe: POST /serviceorchestration/orchestration/subscribe. Input: OrchestrationSubscriptionRequestDTO; output: ResponseEntity<String>. Returns a subscription id. Existing subscriptions will be overwritten. */
        in item 'subscribeRequest' : ArrowheadCoreData::'OrchestrationSubscriptionRequestDTO'[0..1];
        out attribute 'subscribeResponse' : ScalarValues::String[0..1];
        doc /* unsubscribe: DELETE /serviceorchestration/orchestration/unsubscribe/{id}. Input: None; output: ResponseEntity<Void>. Removes the subscription by the given id */
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestration_HTTP_IDD_Implementation :> SimpleStoreServiceOrchestration_serviceOrchestration_HTTP_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestration_http_pull_0' {
        doc /* Returns the orchestration results from the simple store. Wire method: POST; address: /serviceorchestration/orchestration/pull. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/pull"; requestType = "OrchestrationRequestDTO"; responseType = "OrchestrationResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationResponseDTO'[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestration_http_subscribe_1' {
        doc /* Returns a subscription id. Existing subscriptions will be overwritten.. Wire method: POST; address: /serviceorchestration/orchestration/subscribe. Query parameters: [{"name": "trigger", "type": "Boolean", "required": false, "default": "false"}]. Path parameters: []. Status codes: ["CREATED", "OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/subscribe"; requestType = "OrchestrationSubscriptionRequestDTO"; responseType = "ResponseEntity<String>"; queryParameters = "[{\"name\": \"trigger\", \"type\": \"Boolean\", \"required\": false, \"default\": \"false\"}]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionRequestDTO'[0..1];
        out attribute 'response' : ScalarValues::String[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestration_http_unsubscribe_2' {
        doc /* Removes the subscription by the given id. Wire method: DELETE; address: /serviceorchestration/orchestration/unsubscribe/{id}. Query parameters: []. Path parameters: ["id"]. Status codes: ["OK", "NO_CONTENT", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceorchestration/orchestration/unsubscribe/{id}"; requestType = "unspecified"; responseType = "ResponseEntity<Void>"; queryParameters = "[]"; pathParameters = "[\"id\"]"; statusCodes = "[\"OK\", \"NO_CONTENT\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestration_MQTT_IDD :> SimpleStoreServiceOrchestration_serviceOrchestration_SD {
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
    port def SimpleStoreServiceOrchestration_serviceOrchestration_MQTT_IDD_Implementation :> SimpleStoreServiceOrchestration_serviceOrchestration_MQTT_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestration_mqtt_pull_0' {
        doc /* Operation pull. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/pull. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/pull"; requestType = "OrchestrationRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestration_mqtt_subscribe_1' {
        doc /* Operation subscribe. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/subscribe. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/subscribe"; requestType = "OrchestrationSubscriptionRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestration_mqtt_unsubscribe_2' {
        doc /* Operation unsubscribe. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/unsubscribe. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/unsubscribe"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceOrchestrationSimpleStoreManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_HTTP_IDD :> SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationSimpleStoreManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/orchestration/mgmt/simple-store";
        doc /* query: POST /serviceorchestration/orchestration/mgmt/simple-store/query. Input: OrchestrationSimpleStoreQueryRequestDTO; output: OrchestrationSimpleStoreListResponseDTO. Returns the store records according to the filters */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationSimpleStoreQueryRequestDTO'[0..1];
        out item 'queryResponse' : ArrowheadCoreData::'OrchestrationSimpleStoreListResponseDTO'[0..1];
        doc /* remove: DELETE /serviceorchestration/orchestration/mgmt/simple-store/remove. Input: None; output: void. Deletes the simple store records by UUID. */
        doc /* create: POST /serviceorchestration/orchestration/mgmt/simple-store/create. Input: OrchestrationSimpleStoreListRequestDTO; output: OrchestrationSimpleStoreListResponseDTO. Returns the store records that were actually created */
        in item 'createRequest' : ArrowheadCoreData::'OrchestrationSimpleStoreListRequestDTO'[0..1];
        out item 'createResponse' : ArrowheadCoreData::'OrchestrationSimpleStoreListResponseDTO'[0..1];
        doc /* modify-priorities: PUT /serviceorchestration/orchestration/mgmt/simple-store/modify-priorities. Input: PriorityRequestDTO; output: OrchestrationSimpleStoreListResponseDTO. Returns the store records that were updated */
        in item 'modify-prioritiesRequest' : JSONMessage[0..1];
        out item 'modify-prioritiesResponse' : ArrowheadCoreData::'OrchestrationSimpleStoreListResponseDTO'[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_HTTP_IDD_Implementation :> SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_http_query_0' {
        doc /* Returns the store records according to the filters. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/simple-store/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/simple-store/query"; requestType = "OrchestrationSimpleStoreQueryRequestDTO"; responseType = "OrchestrationSimpleStoreListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSimpleStoreQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationSimpleStoreListResponseDTO'[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_http_remove_1' {
        doc /* Deletes the simple store records by UUID.. Wire method: DELETE; address: /serviceorchestration/orchestration/mgmt/simple-store/remove. Query parameters: [{"name": "uuids", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceorchestration/orchestration/mgmt/simple-store/remove"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"uuids\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_http_create_2' {
        doc /* Returns the store records that were actually created. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/simple-store/create. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/simple-store/create"; requestType = "OrchestrationSimpleStoreListRequestDTO"; responseType = "OrchestrationSimpleStoreListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSimpleStoreListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationSimpleStoreListResponseDTO'[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_http_modify-priorities_3' {
        doc /* Returns the store records that were updated. Wire method: PUT; address: /serviceorchestration/orchestration/mgmt/simple-store/modify-priorities. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/serviceorchestration/orchestration/mgmt/simple-store/modify-priorities"; requestType = "PriorityRequestDTO"; responseType = "OrchestrationSimpleStoreListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : JSONMessage[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationSimpleStoreListResponseDTO'[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_MQTT_IDD :> SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationSimpleStoreManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceorchestration/orchestration/management/store/";
        doc /* create: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/store/create. Input: OrchestrationSimpleStoreListRequestDTO; output: None.  */
        in item 'createRequest' : ArrowheadCoreData::'OrchestrationSimpleStoreListRequestDTO'[0..1];
        out item 'createResponse' : JSONMessage[0..1];
        doc /* query: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/store/query. Input: OrchestrationSimpleStoreQueryRequestDTO; output: None.  */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationSimpleStoreQueryRequestDTO'[0..1];
        out item 'queryResponse' : JSONMessage[0..1];
        doc /* remove: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/store/remove. Input: List<String>; output: None.  */
        in attribute 'removeRequest' : ScalarValues::String[0..*];
        out item 'removeResponse' : JSONMessage[0..1];
        doc /* modify-priorities: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/store/modify-priorities. Input: PriorityRequestDTO; output: None.  */
        in item 'modify-prioritiesRequest' : JSONMessage[0..1];
        out item 'modify-prioritiesResponse' : JSONMessage[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_MQTT_IDD_Implementation :> SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_mqtt_create_0' {
        doc /* Operation create. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/store/create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/store/create"; requestType = "OrchestrationSimpleStoreListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSimpleStoreListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_mqtt_query_1' {
        doc /* Operation query. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/store/query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/store/query"; requestType = "OrchestrationSimpleStoreQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSimpleStoreQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_mqtt_remove_2' {
        doc /* Operation remove. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/store/remove. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/store/remove"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_mqtt_modify-priorities_3' {
        doc /* Operation modify-priorities. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/store/modify-priorities. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/store/modify-priorities"; requestType = "PriorityRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : JSONMessage[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceOrchestrationPushManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_HTTP_IDD :> SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_SD {
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
    port def SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_HTTP_IDD_Implementation :> SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_http_subscribe_0' {
        doc /* Returns the created subscription records. Existing ones will be overwritten.. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/push/subscribe. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/push/subscribe"; requestType = "OrchestrationSubscriptionListRequestDTO"; responseType = "OrchestrationSubscriptionListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationSubscriptionListResponseDTO'[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_http_unsubscribe_1' {
        doc /* Removes the specified subscriptions. Wire method: DELETE; address: /serviceorchestration/orchestration/mgmt/push/unsubscribe. Query parameters: [{"name": "ids", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceorchestration/orchestration/mgmt/push/unsubscribe"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"ids\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_http_trigger_2' {
        doc /* Returns the created push orchestration job records. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/push/trigger. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/push/trigger"; requestType = "OrchestrationPushTriggerDTO"; responseType = "OrchestrationPushJobListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationPushTriggerDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationPushJobListResponseDTO'[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_http_query_3' {
        doc /* Returns the required subcription records.. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/push/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/push/query"; requestType = "OrchestrationSubscriptionQueryRequestDTO"; responseType = "OrchestrationSubscriptionListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationSubscriptionListResponseDTO'[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_MQTT_IDD :> SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_SD {
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
    port def SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_MQTT_IDD_Implementation :> SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_mqtt_subscribe_0' {
        doc /* Operation subscribe. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/push/subscribe. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/push/subscribe"; requestType = "OrchestrationSubscriptionListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_mqtt_trigger_1' {
        doc /* Operation trigger. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/push/trigger. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/push/trigger"; requestType = "OrchestrationPushTriggerDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationPushTriggerDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_mqtt_unsubscribe_2' {
        doc /* Operation unsubscribe. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/push/unsubscribe. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/push/unsubscribe"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_mqtt_query_3' {
        doc /* Operation query. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/push/query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/push/query"; requestType = "OrchestrationSubscriptionQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationSubscriptionQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceOrchestrationHistoryManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_HTTP_IDD :> SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationHistoryManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceorchestration/orchestration/mgmt/history";
        doc /* query: POST /serviceorchestration/orchestration/mgmt/history/query. Input: OrchestrationHistoryQueryRequestDTO; output: OrchestrationHistoryResponseDTO. Returns the orchestration job history records according to the given filters. */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationHistoryQueryRequestDTO'[0..1];
        out item 'queryResponse' : ArrowheadCoreData::'OrchestrationHistoryResponseDTO'[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_HTTP_IDD_Implementation :> SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_http_query_0' {
        doc /* Returns the orchestration job history records according to the given filters.. Wire method: POST; address: /serviceorchestration/orchestration/mgmt/history/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceorchestration/orchestration/mgmt/history/query"; requestType = "OrchestrationHistoryQueryRequestDTO"; responseType = "OrchestrationHistoryResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationHistoryQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'OrchestrationHistoryResponseDTO'[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_MQTT_IDD :> SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_SD {
        @IDD { MicroserviceName = "serviceOrchestrationHistoryManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceorchestration/orchestration/management/history/";
        doc /* query: MQTT request/reply arrowhead/serviceorchestration/orchestration/management/history/query. Input: OrchestrationHistoryQueryRequestDTO; output: None.  */
        in item 'queryRequest' : ArrowheadCoreData::'OrchestrationHistoryQueryRequestDTO'[0..1];
        out item 'queryResponse' : JSONMessage[0..1];
    }
    port def SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_MQTT_IDD_Implementation :> SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_mqtt_query_0' {
        doc /* Operation query. Wire method: MQTT request/reply; address: arrowhead/serviceorchestration/orchestration/management/history/query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceorchestration/orchestration/management/history/query"; requestType = "OrchestrationHistoryQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'OrchestrationHistoryQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def SimpleStoreServiceOrchestration_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def SimpleStoreServiceOrchestration_sysMonitor_AMQP_IDD :> SimpleStoreServiceOrchestration_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "../Arrowhead/ah5-core-java-spring/serviceorchestration-simple/documentation/v5.2.2/SimpleStoreServiceOrchestration-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, SimpleStoreServiceOrchestration registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadCoreData::'Observation'[0..1];
    }
    port def SimpleStoreServiceOrchestration_sysMonitor_AMQP_IDD_Implementation :> SimpleStoreServiceOrchestration_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'SimpleStoreServiceOrchestration_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'SimpleStoreServiceOrchestration_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.SimpleStoreServiceOrchestration.SimpleStoreServiceOrchestration.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    part def SimpleStoreServiceOrchestration_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "SimpleStoreServiceOrchestration"; FunctionalProperties = "This document describes the SimpleStoreServiceOrchestration core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/serviceorchestration-simple, including the local extensions of this workspace.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        doc /* This document describes the SimpleStoreServiceOrchestration core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/serviceorchestration-simple, including the local extensions of this workspace.
  \end{abstract} */
        port 'generalManagement_SD' : SimpleStoreServiceOrchestration_generalManagement_SD;
        port 'serviceOrchestration_SD' : SimpleStoreServiceOrchestration_serviceOrchestration_SD;
        port 'serviceOrchestrationSimpleStoreManagement_SD' : SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_SD;
        port 'serviceOrchestrationPushManagement_SD' : SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_SD;
        port 'serviceOrchestrationHistoryManagement_SD' : SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_SD;
        port 'sysMonitor_SD' : SimpleStoreServiceOrchestration_sysMonitor_SD;
    }
    part def SimpleStoreServiceOrchestration_SysDD :> SimpleStoreServiceOrchestration_SysD {
        @SysDD;
        port 'generalManagement_http' : SimpleStoreServiceOrchestration_generalManagement_HTTP_IDD;
        port 'generalManagement_mqtt' : SimpleStoreServiceOrchestration_generalManagement_MQTT_IDD;
        port 'serviceOrchestration_http' : SimpleStoreServiceOrchestration_serviceOrchestration_HTTP_IDD;
        port 'serviceOrchestration_mqtt' : SimpleStoreServiceOrchestration_serviceOrchestration_MQTT_IDD;
        port 'serviceOrchestrationSimpleStoreManagement_http' : SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_HTTP_IDD;
        port 'serviceOrchestrationSimpleStoreManagement_mqtt' : SimpleStoreServiceOrchestration_serviceOrchestrationSimpleStoreManagement_MQTT_IDD;
        port 'serviceOrchestrationPushManagement_http' : SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_HTTP_IDD;
        port 'serviceOrchestrationPushManagement_mqtt' : SimpleStoreServiceOrchestration_serviceOrchestrationPushManagement_MQTT_IDD;
        port 'serviceOrchestrationHistoryManagement_http' : SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_HTTP_IDD;
        port 'serviceOrchestrationHistoryManagement_mqtt' : SimpleStoreServiceOrchestration_serviceOrchestrationHistoryManagement_MQTT_IDD;
        port 'sysMonitor_amqp' : SimpleStoreServiceOrchestration_sysMonitor_AMQP_IDD;
    }
    part def SimpleStoreServiceOrchestration :> SimpleStoreServiceOrchestration_SysDD {
        @System_Implementation;
    }
}
