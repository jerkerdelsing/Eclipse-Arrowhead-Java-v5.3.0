library package ArrowheadServiceRegistry {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def ServiceRegistry_deviceDiscovery_SD :> ArrowheadService {
        @SD { MicroserviceName = "deviceDiscovery"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ServiceRegistry_deviceDiscovery_HTTP_IDD :> ServiceRegistry_deviceDiscovery_SD {
        @IDD { MicroserviceName = "deviceDiscovery"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceregistry/device-discovery";
        doc /* register: POST /serviceregistry/device-discovery/register. Input: DeviceRequestDTO; output: ResponseEntity<DeviceResponseDTO>. Returns the newly created or matching device entry */
        in item 'registerRequest' : ArrowheadCoreData::'DeviceRequestDTO'[0..1];
        out item 'registerResponse' : ArrowheadCoreData::'DeviceResponseDTO'[0..1];
        doc /* lookup: POST /serviceregistry/device-discovery/lookup. Input: DeviceLookupRequestDTO; output: DeviceListResponseDTO. Returns device entries according to the given filters */
        in item 'lookupRequest' : ArrowheadCoreData::'DeviceLookupRequestDTO'[0..1];
        out item 'lookupResponse' : ArrowheadCoreData::'DeviceListResponseDTO'[0..1];
        doc /* revoke: DELETE /serviceregistry/device-discovery/revoke/{name}. Input: None; output: ResponseEntity<Void>. Deletes device entry by name if exists and no systems are assigned to it */
    }
    port def ServiceRegistry_deviceDiscovery_HTTP_IDD_Implementation :> ServiceRegistry_deviceDiscovery_HTTP_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_deviceDiscovery_http_register_0' {
        doc /* Returns the newly created or matching device entry. Wire method: POST; address: /serviceregistry/device-discovery/register. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/device-discovery/register"; requestType = "DeviceRequestDTO"; responseType = "ResponseEntity<DeviceResponseDTO>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'DeviceRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'DeviceResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_deviceDiscovery_http_lookup_1' {
        doc /* Returns device entries according to the given filters. Wire method: POST; address: /serviceregistry/device-discovery/lookup. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/device-discovery/lookup"; requestType = "DeviceLookupRequestDTO"; responseType = "DeviceListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'DeviceLookupRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'DeviceListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_deviceDiscovery_http_revoke_2' {
        doc /* Deletes device entry by name if exists and no systems are assigned to it. Wire method: DELETE; address: /serviceregistry/device-discovery/revoke/{name}. Query parameters: []. Path parameters: ["name"]. Status codes: ["OK", "NO_CONTENT", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "LOCKED", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceregistry/device-discovery/revoke/{name}"; requestType = "unspecified"; responseType = "ResponseEntity<Void>"; queryParameters = "[]"; pathParameters = "[\"name\"]"; statusCodes = "[\"OK\", \"NO_CONTENT\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"LOCKED\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    port def ServiceRegistry_deviceDiscovery_MQTT_IDD :> ServiceRegistry_deviceDiscovery_SD {
        @IDD { MicroserviceName = "deviceDiscovery"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceregistry/device-discovery/";
        doc /* register: MQTT request/reply arrowhead/serviceregistry/device-discovery/register. Input: DeviceRequestDTO; output: None.  */
        in item 'registerRequest' : ArrowheadCoreData::'DeviceRequestDTO'[0..1];
        out item 'registerResponse' : JSONMessage[0..1];
        doc /* lookup: MQTT request/reply arrowhead/serviceregistry/device-discovery/lookup. Input: DeviceLookupRequestDTO; output: None.  */
        in item 'lookupRequest' : ArrowheadCoreData::'DeviceLookupRequestDTO'[0..1];
        out item 'lookupResponse' : JSONMessage[0..1];
        doc /* revoke: MQTT request/reply arrowhead/serviceregistry/device-discovery/revoke. Input: String; output: None.  */
        in attribute 'revokeRequest' : ScalarValues::String[0..1];
        out item 'revokeResponse' : JSONMessage[0..1];
    }
    port def ServiceRegistry_deviceDiscovery_MQTT_IDD_Implementation :> ServiceRegistry_deviceDiscovery_MQTT_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_deviceDiscovery_mqtt_register_0' {
        doc /* Operation register. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/device-discovery/register. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/device-discovery/register"; requestType = "DeviceRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'DeviceRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_deviceDiscovery_mqtt_lookup_1' {
        doc /* Operation lookup. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/device-discovery/lookup. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/device-discovery/lookup"; requestType = "DeviceLookupRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'DeviceLookupRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_deviceDiscovery_mqtt_revoke_2' {
        doc /* Operation revoke. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/device-discovery/revoke. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/device-discovery/revoke"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def ServiceRegistry_systemDiscovery_SD :> ArrowheadService {
        @SD { MicroserviceName = "systemDiscovery"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ServiceRegistry_systemDiscovery_HTTP_IDD :> ServiceRegistry_systemDiscovery_SD {
        @IDD { MicroserviceName = "systemDiscovery"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceregistry/system-discovery";
        doc /* register: POST /serviceregistry/system-discovery/register. Input: SystemRegisterRequestDTO; output: ResponseEntity<SystemResponseDTO>. Returns the newly created or matching system entry */
        in item 'registerRequest' : ArrowheadCoreData::'SystemRegisterRequestDTO'[0..1];
        out item 'registerResponse' : ArrowheadCoreData::'SystemResponseDTO'[0..1];
        doc /* lookup: POST /serviceregistry/system-discovery/lookup. Input: SystemLookupRequestDTO; output: SystemListResponseDTO. Returns system entries according to the given filters */
        in item 'lookupRequest' : ArrowheadCoreData::'SystemLookupRequestDTO'[0..1];
        out item 'lookupResponse' : ArrowheadCoreData::'SystemListResponseDTO'[0..1];
        doc /* revoke: DELETE /serviceregistry/system-discovery/revoke. Input: None; output: ResponseEntity<Void>. Deletes system entry if exists */
    }
    port def ServiceRegistry_systemDiscovery_HTTP_IDD_Implementation :> ServiceRegistry_systemDiscovery_HTTP_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_systemDiscovery_http_register_0' {
        doc /* Returns the newly created or matching system entry. Wire method: POST; address: /serviceregistry/system-discovery/register. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/system-discovery/register"; requestType = "SystemRegisterRequestDTO"; responseType = "ResponseEntity<SystemResponseDTO>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'SystemRegisterRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'SystemResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_systemDiscovery_http_lookup_1' {
        doc /* Returns system entries according to the given filters. Wire method: POST; address: /serviceregistry/system-discovery/lookup. Query parameters: [{"name": "verbose", "type": "boolean", "required": false, "default": "false"}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/system-discovery/lookup"; requestType = "SystemLookupRequestDTO"; responseType = "SystemListResponseDTO"; queryParameters = "[{\"name\": \"verbose\", \"type\": \"boolean\", \"required\": false, \"default\": \"false\"}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'SystemLookupRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'SystemListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_systemDiscovery_http_revoke_2' {
        doc /* Deletes system entry if exists. Wire method: DELETE; address: /serviceregistry/system-discovery/revoke. Query parameters: []. Path parameters: []. Status codes: ["OK", "NO_CONTENT", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceregistry/system-discovery/revoke"; requestType = "unspecified"; responseType = "ResponseEntity<Void>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"NO_CONTENT\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    port def ServiceRegistry_systemDiscovery_MQTT_IDD :> ServiceRegistry_systemDiscovery_SD {
        @IDD { MicroserviceName = "systemDiscovery"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceregistry/system-discovery/";
        doc /* register: MQTT request/reply arrowhead/serviceregistry/system-discovery/register. Input: SystemRegisterRequestDTO; output: None.  */
        in item 'registerRequest' : ArrowheadCoreData::'SystemRegisterRequestDTO'[0..1];
        out item 'registerResponse' : JSONMessage[0..1];
        doc /* lookup: MQTT request/reply arrowhead/serviceregistry/system-discovery/lookup. Input: SystemLookupRequestDTO; output: None.  */
        in item 'lookupRequest' : ArrowheadCoreData::'SystemLookupRequestDTO'[0..1];
        out item 'lookupResponse' : JSONMessage[0..1];
        doc /* revoke: MQTT request/reply arrowhead/serviceregistry/system-discovery/revoke. Input: None; output: None.  */
        out item 'revokeResponse' : JSONMessage[0..1];
    }
    port def ServiceRegistry_systemDiscovery_MQTT_IDD_Implementation :> ServiceRegistry_systemDiscovery_MQTT_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_systemDiscovery_mqtt_register_0' {
        doc /* Operation register. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/system-discovery/register. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/system-discovery/register"; requestType = "SystemRegisterRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'SystemRegisterRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_systemDiscovery_mqtt_lookup_1' {
        doc /* Operation lookup. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/system-discovery/lookup. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/system-discovery/lookup"; requestType = "SystemLookupRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'SystemLookupRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_systemDiscovery_mqtt_revoke_2' {
        doc /* Operation revoke. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/system-discovery/revoke. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/system-discovery/revoke"; requestType = "unspecified"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : JSONMessage[0..1];
    }
    port def ServiceRegistry_serviceDiscovery_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceDiscovery"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ServiceRegistry_serviceDiscovery_HTTP_IDD :> ServiceRegistry_serviceDiscovery_SD {
        @IDD { MicroserviceName = "serviceDiscovery"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceregistry/service-discovery";
        doc /* register: POST /serviceregistry/service-discovery/register. Input: ServiceInstanceCreateRequestDTO; output: ServiceInstanceResponseDTO. Returns the created service entry */
        in item 'registerRequest' : ArrowheadCoreData::'ServiceInstanceCreateRequestDTO'[0..1];
        out item 'registerResponse' : ArrowheadCoreData::'ServiceInstanceResponseDTO'[0..1];
        doc /* lookup: POST /serviceregistry/service-discovery/lookup. Input: ServiceInstanceLookupRequestDTO; output: ServiceInstanceListResponseDTO. Returns the service entries according to the given filters */
        in item 'lookupRequest' : ArrowheadCoreData::'ServiceInstanceLookupRequestDTO'[0..1];
        out item 'lookupResponse' : ArrowheadCoreData::'ServiceInstanceListResponseDTO'[0..1];
        doc /* revoke: DELETE /serviceregistry/service-discovery/revoke/{instanceId}. Input: None; output: ResponseEntity<Void>. Deletes service instance entry if exists */
    }
    port def ServiceRegistry_serviceDiscovery_HTTP_IDD_Implementation :> ServiceRegistry_serviceDiscovery_HTTP_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_serviceDiscovery_http_register_0' {
        doc /* Returns the created service entry. Wire method: POST; address: /serviceregistry/service-discovery/register. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/service-discovery/register"; requestType = "ServiceInstanceCreateRequestDTO"; responseType = "ServiceInstanceResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceCreateRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'ServiceInstanceResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceDiscovery_http_lookup_1' {
        doc /* Returns the service entries according to the given filters. Wire method: POST; address: /serviceregistry/service-discovery/lookup. Query parameters: [{"name": "verbose", "type": "boolean", "required": false, "default": "false"}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/service-discovery/lookup"; requestType = "ServiceInstanceLookupRequestDTO"; responseType = "ServiceInstanceListResponseDTO"; queryParameters = "[{\"name\": \"verbose\", \"type\": \"boolean\", \"required\": false, \"default\": \"false\"}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceLookupRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'ServiceInstanceListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceDiscovery_http_revoke_2' {
        doc /* Deletes service instance entry if exists. Wire method: DELETE; address: /serviceregistry/service-discovery/revoke/{instanceId}. Query parameters: []. Path parameters: ["instanceId"]. Status codes: ["OK", "NO_CONTENT", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceregistry/service-discovery/revoke/{instanceId}"; requestType = "unspecified"; responseType = "ResponseEntity<Void>"; queryParameters = "[]"; pathParameters = "[\"instanceId\"]"; statusCodes = "[\"OK\", \"NO_CONTENT\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    port def ServiceRegistry_serviceDiscovery_MQTT_IDD :> ServiceRegistry_serviceDiscovery_SD {
        @IDD { MicroserviceName = "serviceDiscovery"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceregistry/service-discovery/";
        doc /* register: MQTT request/reply arrowhead/serviceregistry/service-discovery/register. Input: ServiceInstanceCreateRequestDTO; output: None.  */
        in item 'registerRequest' : ArrowheadCoreData::'ServiceInstanceCreateRequestDTO'[0..1];
        out item 'registerResponse' : JSONMessage[0..1];
        doc /* lookup: MQTT request/reply arrowhead/serviceregistry/service-discovery/lookup. Input: ServiceInstanceLookupRequestDTO; output: None.  */
        in item 'lookupRequest' : ArrowheadCoreData::'ServiceInstanceLookupRequestDTO'[0..1];
        out item 'lookupResponse' : JSONMessage[0..1];
        doc /* revoke: MQTT request/reply arrowhead/serviceregistry/service-discovery/revoke. Input: String; output: None.  */
        in attribute 'revokeRequest' : ScalarValues::String[0..1];
        out item 'revokeResponse' : JSONMessage[0..1];
    }
    port def ServiceRegistry_serviceDiscovery_MQTT_IDD_Implementation :> ServiceRegistry_serviceDiscovery_MQTT_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_serviceDiscovery_mqtt_register_0' {
        doc /* Operation register. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/service-discovery/register. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/service-discovery/register"; requestType = "ServiceInstanceCreateRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceCreateRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceDiscovery_mqtt_lookup_1' {
        doc /* Operation lookup. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/service-discovery/lookup. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/service-discovery/lookup"; requestType = "ServiceInstanceLookupRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceLookupRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceDiscovery_mqtt_revoke_2' {
        doc /* Operation revoke. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/service-discovery/revoke. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/service-discovery/revoke"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def ServiceRegistry_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ServiceRegistry_generalManagement_HTTP_IDD :> ServiceRegistry_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceregistry/general/mgmt";
        doc /* get-log: POST /serviceregistry/general/mgmt/logs. Input: LogRequestDTO; output: LogEntryListResponseDTO. Returns the log entries */
        in item 'get-logRequest' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'get-logResponse' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
        doc /* get-config: GET /serviceregistry/general/mgmt/get-config. Input: None; output: KeyValuesDTO. Returns a list of configurations */
        out item 'get-configResponse' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def ServiceRegistry_generalManagement_HTTP_IDD_Implementation :> ServiceRegistry_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_generalManagement_http_get-log_0' {
        doc /* Returns the log entries. Wire method: POST; address: /serviceregistry/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/general/mgmt/logs"; requestType = "LogRequestDTO"; responseType = "LogEntryListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'LogEntryListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_generalManagement_http_get-config_1' {
        doc /* Returns a list of configurations. Wire method: GET; address: /serviceregistry/general/mgmt/get-config. Query parameters: [{"name": "keys", "type": "List<String>", "required": false, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/serviceregistry/general/mgmt/get-config"; requestType = "unspecified"; responseType = "KeyValuesDTO"; queryParameters = "[{\"name\": \"keys\", \"type\": \"List<String>\", \"required\": false, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        out item 'response' : ArrowheadCoreData::'KeyValuesDTO'[0..1];
    }
    port def ServiceRegistry_generalManagement_MQTT_IDD :> ServiceRegistry_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceregistry/general/management/";
        doc /* get-log: MQTT request/reply arrowhead/serviceregistry/general/management/get-log. Input: LogRequestDTO; output: None.  */
        in item 'get-logRequest' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'get-logResponse' : JSONMessage[0..1];
        doc /* get-config: MQTT request/reply arrowhead/serviceregistry/general/management/get-config. Input: List<String>; output: None.  */
        in attribute 'get-configRequest' : ScalarValues::String[0..*];
        out item 'get-configResponse' : JSONMessage[0..1];
    }
    port def ServiceRegistry_generalManagement_MQTT_IDD_Implementation :> ServiceRegistry_generalManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_generalManagement_mqtt_get-log_0' {
        doc /* Operation get-log. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/general/management/get-log. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/general/management/get-log"; requestType = "LogRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'LogRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_generalManagement_mqtt_get-config_1' {
        doc /* Operation get-config. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/general/management/get-config. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/general/management/get-config"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    port def ServiceRegistry_serviceRegistryManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "serviceRegistryManagement"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ServiceRegistry_serviceRegistryManagement_HTTP_IDD :> ServiceRegistry_serviceRegistryManagement_SD {
        @IDD { MicroserviceName = "serviceRegistryManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute basePath : String = "/serviceregistry/mgmt";
        doc /* device-query: POST /serviceregistry/mgmt/devices/query. Input: DeviceQueryRequestDTO; output: DeviceListResponseDTO. Returns the device entries according to the query request */
        in item 'device-queryRequest' : ArrowheadCoreData::'DeviceQueryRequestDTO'[0..1];
        out item 'device-queryResponse' : ArrowheadCoreData::'DeviceListResponseDTO'[0..1];
        doc /* device-create: POST /serviceregistry/mgmt/devices. Input: DeviceListRequestDTO; output: DeviceListResponseDTO. Returns the created device entries */
        in item 'device-createRequest' : ArrowheadCoreData::'DeviceListRequestDTO'[0..1];
        out item 'device-createResponse' : ArrowheadCoreData::'DeviceListResponseDTO'[0..1];
        doc /* device-update: PUT /serviceregistry/mgmt/devices. Input: DeviceListRequestDTO; output: DeviceListResponseDTO. Returns the updated device entries */
        in item 'device-updateRequest' : ArrowheadCoreData::'DeviceListRequestDTO'[0..1];
        out item 'device-updateResponse' : ArrowheadCoreData::'DeviceListResponseDTO'[0..1];
        doc /* device-remove: DELETE /serviceregistry/mgmt/devices. Input: None; output: void. Deletes the defined device entries */
        doc /* system-query: POST /serviceregistry/mgmt/systems/query. Input: SystemQueryRequestDTO; output: SystemListResponseDTO. Returns the system entries according to the query request */
        in item 'system-queryRequest' : ArrowheadCoreData::'SystemQueryRequestDTO'[0..1];
        out item 'system-queryResponse' : ArrowheadCoreData::'SystemListResponseDTO'[0..1];
        doc /* system-create: POST /serviceregistry/mgmt/systems. Input: SystemListRequestDTO; output: SystemListResponseDTO. Returns the created system entries */
        in item 'system-createRequest' : ArrowheadCoreData::'SystemListRequestDTO'[0..1];
        out item 'system-createResponse' : ArrowheadCoreData::'SystemListResponseDTO'[0..1];
        doc /* system-update: PUT /serviceregistry/mgmt/systems. Input: SystemListRequestDTO; output: SystemListResponseDTO. Returns the updated system entries */
        in item 'system-updateRequest' : ArrowheadCoreData::'SystemListRequestDTO'[0..1];
        out item 'system-updateResponse' : ArrowheadCoreData::'SystemListResponseDTO'[0..1];
        doc /* system-remove: DELETE /serviceregistry/mgmt/systems. Input: None; output: void. Deletes the defined system entries */
        doc /* service-definition-query: POST /serviceregistry/mgmt/service-definitions/query. Input: PageDTO; output: ServiceDefinitionListResponseDTO. Returns the service definition entries according to the given page request */
        in item 'service-definition-queryRequest' : ArrowheadCoreData::'PageDTO'[0..1];
        out item 'service-definition-queryResponse' : ArrowheadCoreData::'ServiceDefinitionListResponseDTO'[0..1];
        doc /* service-definition-create: POST /serviceregistry/mgmt/service-definitions. Input: ServiceDefinitionListRequestDTO; output: ServiceDefinitionListResponseDTO. Returns the created service definition entries */
        in item 'service-definition-createRequest' : ArrowheadCoreData::'ServiceDefinitionListRequestDTO'[0..1];
        out item 'service-definition-createResponse' : ArrowheadCoreData::'ServiceDefinitionListResponseDTO'[0..1];
        doc /* service-definition-remove: DELETE /serviceregistry/mgmt/service-definitions. Input: None; output: void. Deletes the defined service definition entries */
        doc /* service-query: POST /serviceregistry/mgmt/service-instances/query. Input: ServiceInstanceQueryRequestDTO; output: ServiceInstanceListResponseDTO. Returns the service instance entries according to the query request */
        in item 'service-queryRequest' : ArrowheadCoreData::'ServiceInstanceQueryRequestDTO'[0..1];
        out item 'service-queryResponse' : ArrowheadCoreData::'ServiceInstanceListResponseDTO'[0..1];
        doc /* service-create: POST /serviceregistry/mgmt/service-instances. Input: ServiceInstanceCreateListRequestDTO; output: ServiceInstanceListResponseDTO. Returns the created service instance entries */
        in item 'service-createRequest' : ArrowheadCoreData::'ServiceInstanceCreateListRequestDTO'[0..1];
        out item 'service-createResponse' : ArrowheadCoreData::'ServiceInstanceListResponseDTO'[0..1];
        doc /* service-update: PUT /serviceregistry/mgmt/service-instances. Input: ServiceInstanceUpdateListRequestDTO; output: ServiceInstanceListResponseDTO. Returns the updated service instance entries */
        in item 'service-updateRequest' : ArrowheadCoreData::'ServiceInstanceUpdateListRequestDTO'[0..1];
        out item 'service-updateResponse' : ArrowheadCoreData::'ServiceInstanceListResponseDTO'[0..1];
        doc /* service-remove: DELETE /serviceregistry/mgmt/service-instances. Input: None; output: void. Deletes the defined service instance entries */
        doc /* interface-template-query: POST /serviceregistry/mgmt/interface-templates/query. Input: ServiceInterfaceTemplateQueryRequestDTO; output: ServiceInterfaceTemplateListResponseDTO. Returns the interface template entries according to the query request */
        in item 'interface-template-queryRequest' : ArrowheadCoreData::'ServiceInterfaceTemplateQueryRequestDTO'[0..1];
        out item 'interface-template-queryResponse' : ArrowheadCoreData::'ServiceInterfaceTemplateListResponseDTO'[0..1];
        doc /* interface-template-create: POST /serviceregistry/mgmt/interface-templates. Input: ServiceInterfaceTemplateListRequestDTO; output: ServiceInterfaceTemplateListResponseDTO. Returns the created interface template entries */
        in item 'interface-template-createRequest' : ArrowheadCoreData::'ServiceInterfaceTemplateListRequestDTO'[0..1];
        out item 'interface-template-createResponse' : ArrowheadCoreData::'ServiceInterfaceTemplateListResponseDTO'[0..1];
        doc /* interface-template-remove: DELETE /serviceregistry/mgmt/interface-templates. Input: None; output: void. Deletes the defined interface template entries */
    }
    port def ServiceRegistry_serviceRegistryManagement_HTTP_IDD_Implementation :> ServiceRegistry_serviceRegistryManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_serviceRegistryManagement_http_device-query_0' {
        doc /* Returns the device entries according to the query request. Wire method: POST; address: /serviceregistry/mgmt/devices/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/devices/query"; requestType = "DeviceQueryRequestDTO"; responseType = "DeviceListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'DeviceQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'DeviceListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_device-create_1' {
        doc /* Returns the created device entries. Wire method: POST; address: /serviceregistry/mgmt/devices. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/devices"; requestType = "DeviceListRequestDTO"; responseType = "DeviceListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'DeviceListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'DeviceListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_device-update_2' {
        doc /* Returns the updated device entries. Wire method: PUT; address: /serviceregistry/mgmt/devices. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/serviceregistry/mgmt/devices"; requestType = "DeviceListRequestDTO"; responseType = "DeviceListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'DeviceListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'DeviceListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_device-remove_3' {
        doc /* Deletes the defined device entries. Wire method: DELETE; address: /serviceregistry/mgmt/devices. Query parameters: [{"name": "names", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "LOCKED", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceregistry/mgmt/devices"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"names\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"LOCKED\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_system-query_4' {
        doc /* Returns the system entries according to the query request. Wire method: POST; address: /serviceregistry/mgmt/systems/query. Query parameters: [{"name": "verbose", "type": "boolean", "required": false, "default": "false"}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/systems/query"; requestType = "SystemQueryRequestDTO"; responseType = "SystemListResponseDTO"; queryParameters = "[{\"name\": \"verbose\", \"type\": \"boolean\", \"required\": false, \"default\": \"false\"}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'SystemQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'SystemListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_system-create_5' {
        doc /* Returns the created system entries. Wire method: POST; address: /serviceregistry/mgmt/systems. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/systems"; requestType = "SystemListRequestDTO"; responseType = "SystemListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'SystemListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'SystemListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_system-update_6' {
        doc /* Returns the updated system entries. Wire method: PUT; address: /serviceregistry/mgmt/systems. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/serviceregistry/mgmt/systems"; requestType = "SystemListRequestDTO"; responseType = "SystemListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'SystemListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'SystemListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_system-remove_7' {
        doc /* Deletes the defined system entries. Wire method: DELETE; address: /serviceregistry/mgmt/systems. Query parameters: [{"name": "names", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceregistry/mgmt/systems"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"names\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_service-definition-query_8' {
        doc /* Returns the service definition entries according to the given page request. Wire method: POST; address: /serviceregistry/mgmt/service-definitions/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/service-definitions/query"; requestType = "PageDTO"; responseType = "ServiceDefinitionListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'PageDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'ServiceDefinitionListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_service-definition-create_9' {
        doc /* Returns the created service definition entries. Wire method: POST; address: /serviceregistry/mgmt/service-definitions. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/service-definitions"; requestType = "ServiceDefinitionListRequestDTO"; responseType = "ServiceDefinitionListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'ServiceDefinitionListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'ServiceDefinitionListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_service-definition-remove_10' {
        doc /* Deletes the defined service definition entries. Wire method: DELETE; address: /serviceregistry/mgmt/service-definitions. Query parameters: [{"name": "names", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceregistry/mgmt/service-definitions"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"names\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_service-query_11' {
        doc /* Returns the service instance entries according to the query request. Wire method: POST; address: /serviceregistry/mgmt/service-instances/query. Query parameters: [{"name": "verbose", "type": "boolean", "required": false, "default": "false"}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/service-instances/query"; requestType = "ServiceInstanceQueryRequestDTO"; responseType = "ServiceInstanceListResponseDTO"; queryParameters = "[{\"name\": \"verbose\", \"type\": \"boolean\", \"required\": false, \"default\": \"false\"}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'ServiceInstanceListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_service-create_12' {
        doc /* Returns the created service instance entries. Wire method: POST; address: /serviceregistry/mgmt/service-instances. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/service-instances"; requestType = "ServiceInstanceCreateListRequestDTO"; responseType = "ServiceInstanceListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceCreateListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'ServiceInstanceListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_service-update_13' {
        doc /* Returns the updated service instance entries. Wire method: PUT; address: /serviceregistry/mgmt/service-instances. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/serviceregistry/mgmt/service-instances"; requestType = "ServiceInstanceUpdateListRequestDTO"; responseType = "ServiceInstanceListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceUpdateListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'ServiceInstanceListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_service-remove_14' {
        doc /* Deletes the defined service instance entries. Wire method: DELETE; address: /serviceregistry/mgmt/service-instances. Query parameters: [{"name": "serviceInstances", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceregistry/mgmt/service-instances"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"serviceInstances\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_interface-template-query_15' {
        doc /* Returns the interface template entries according to the query request. Wire method: POST; address: /serviceregistry/mgmt/interface-templates/query. Query parameters: []. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/interface-templates/query"; requestType = "ServiceInterfaceTemplateQueryRequestDTO"; responseType = "ServiceInterfaceTemplateListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInterfaceTemplateQueryRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'ServiceInterfaceTemplateListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_interface-template-create_16' {
        doc /* Returns the created interface template entries. Wire method: POST; address: /serviceregistry/mgmt/interface-templates. Query parameters: []. Path parameters: []. Status codes: ["CREATED", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/serviceregistry/mgmt/interface-templates"; requestType = "ServiceInterfaceTemplateListRequestDTO"; responseType = "ServiceInterfaceTemplateListResponseDTO"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[\"CREATED\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInterfaceTemplateListRequestDTO'[0..1];
        out item 'response' : ArrowheadCoreData::'ServiceInterfaceTemplateListResponseDTO'[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_http_interface-template-remove_17' {
        doc /* Deletes the defined interface template entries. Wire method: DELETE; address: /serviceregistry/mgmt/interface-templates. Query parameters: [{"name": "names", "type": "List<String>", "required": true, "default": null}]. Path parameters: []. Status codes: ["OK", "BAD_REQUEST", "UNAUTHORIZED", "FORBIDDEN", "INTERNAL_SERVER_ERROR"] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/serviceregistry/mgmt/interface-templates"; requestType = "unspecified"; responseType = "void"; queryParameters = "[{\"name\": \"names\", \"type\": \"List<String>\", \"required\": true, \"default\": null}]"; pathParameters = "[]"; statusCodes = "[\"OK\", \"BAD_REQUEST\", \"UNAUTHORIZED\", \"FORBIDDEN\", \"INTERNAL_SERVER_ERROR\"]"; }
    }
    port def ServiceRegistry_serviceRegistryManagement_MQTT_IDD :> ServiceRegistry_serviceRegistryManagement_SD {
        @IDD { MicroserviceName = "serviceRegistryManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        attribute baseTopic : String = "arrowhead/serviceregistry/management/";
        doc /* device-query: MQTT request/reply arrowhead/serviceregistry/management/device-query. Input: DeviceQueryRequestDTO; output: None.  */
        in item 'device-queryRequest' : ArrowheadCoreData::'DeviceQueryRequestDTO'[0..1];
        out item 'device-queryResponse' : JSONMessage[0..1];
        doc /* device-create: MQTT request/reply arrowhead/serviceregistry/management/device-create. Input: DeviceListRequestDTO; output: None.  */
        in item 'device-createRequest' : ArrowheadCoreData::'DeviceListRequestDTO'[0..1];
        out item 'device-createResponse' : JSONMessage[0..1];
        doc /* device-update: MQTT request/reply arrowhead/serviceregistry/management/device-update. Input: DeviceListRequestDTO; output: None.  */
        in item 'device-updateRequest' : ArrowheadCoreData::'DeviceListRequestDTO'[0..1];
        out item 'device-updateResponse' : JSONMessage[0..1];
        doc /* device-remove: MQTT request/reply arrowhead/serviceregistry/management/device-remove. Input: List<String>; output: None.  */
        in attribute 'device-removeRequest' : ScalarValues::String[0..*];
        out item 'device-removeResponse' : JSONMessage[0..1];
        doc /* system-query: MQTT request/reply arrowhead/serviceregistry/management/system-query. Input: SystemQueryRequestDTO; output: None.  */
        in item 'system-queryRequest' : ArrowheadCoreData::'SystemQueryRequestDTO'[0..1];
        out item 'system-queryResponse' : JSONMessage[0..1];
        doc /* system-create: MQTT request/reply arrowhead/serviceregistry/management/system-create. Input: SystemListRequestDTO; output: None.  */
        in item 'system-createRequest' : ArrowheadCoreData::'SystemListRequestDTO'[0..1];
        out item 'system-createResponse' : JSONMessage[0..1];
        doc /* system-update: MQTT request/reply arrowhead/serviceregistry/management/system-update. Input: SystemListRequestDTO; output: None.  */
        in item 'system-updateRequest' : ArrowheadCoreData::'SystemListRequestDTO'[0..1];
        out item 'system-updateResponse' : JSONMessage[0..1];
        doc /* system-remove: MQTT request/reply arrowhead/serviceregistry/management/system-remove. Input: List<String>; output: None.  */
        in attribute 'system-removeRequest' : ScalarValues::String[0..*];
        out item 'system-removeResponse' : JSONMessage[0..1];
        doc /* service-definition-query: MQTT request/reply arrowhead/serviceregistry/management/service-definition-query. Input: PageDTO; output: None.  */
        in item 'service-definition-queryRequest' : ArrowheadCoreData::'PageDTO'[0..1];
        out item 'service-definition-queryResponse' : JSONMessage[0..1];
        doc /* service-definition-create: MQTT request/reply arrowhead/serviceregistry/management/service-definition-create. Input: ServiceDefinitionListRequestDTO; output: None.  */
        in item 'service-definition-createRequest' : ArrowheadCoreData::'ServiceDefinitionListRequestDTO'[0..1];
        out item 'service-definition-createResponse' : JSONMessage[0..1];
        doc /* service-definition-remove: MQTT request/reply arrowhead/serviceregistry/management/service-definition-remove. Input: List<String>; output: None.  */
        in attribute 'service-definition-removeRequest' : ScalarValues::String[0..*];
        out item 'service-definition-removeResponse' : JSONMessage[0..1];
        doc /* service-query: MQTT request/reply arrowhead/serviceregistry/management/service-query. Input: ServiceInstanceQueryRequestDTO; output: None.  */
        in item 'service-queryRequest' : ArrowheadCoreData::'ServiceInstanceQueryRequestDTO'[0..1];
        out item 'service-queryResponse' : JSONMessage[0..1];
        doc /* service-create: MQTT request/reply arrowhead/serviceregistry/management/service-create. Input: ServiceInstanceCreateListRequestDTO; output: None.  */
        in item 'service-createRequest' : ArrowheadCoreData::'ServiceInstanceCreateListRequestDTO'[0..1];
        out item 'service-createResponse' : JSONMessage[0..1];
        doc /* service-update: MQTT request/reply arrowhead/serviceregistry/management/service-update. Input: ServiceInstanceUpdateListRequestDTO; output: None.  */
        in item 'service-updateRequest' : ArrowheadCoreData::'ServiceInstanceUpdateListRequestDTO'[0..1];
        out item 'service-updateResponse' : JSONMessage[0..1];
        doc /* service-remove: MQTT request/reply arrowhead/serviceregistry/management/service-remove. Input: List<String>; output: None.  */
        in attribute 'service-removeRequest' : ScalarValues::String[0..*];
        out item 'service-removeResponse' : JSONMessage[0..1];
        doc /* interface-template-query: MQTT request/reply arrowhead/serviceregistry/management/interface-template-query. Input: ServiceInterfaceTemplateQueryRequestDTO; output: None.  */
        in item 'interface-template-queryRequest' : ArrowheadCoreData::'ServiceInterfaceTemplateQueryRequestDTO'[0..1];
        out item 'interface-template-queryResponse' : JSONMessage[0..1];
        doc /* interface-template-create: MQTT request/reply arrowhead/serviceregistry/management/interface-template-create. Input: ServiceInterfaceTemplateListRequestDTO; output: None.  */
        in item 'interface-template-createRequest' : ArrowheadCoreData::'ServiceInterfaceTemplateListRequestDTO'[0..1];
        out item 'interface-template-createResponse' : JSONMessage[0..1];
        doc /* interface-template-remove: MQTT request/reply arrowhead/serviceregistry/management/interface-template-remove. Input: List<String>; output: None.  */
        in attribute 'interface-template-removeRequest' : ScalarValues::String[0..*];
        out item 'interface-template-removeResponse' : JSONMessage[0..1];
    }
    port def ServiceRegistry_serviceRegistryManagement_MQTT_IDD_Implementation :> ServiceRegistry_serviceRegistryManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_device-query_0' {
        doc /* Operation device-query. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/device-query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/device-query"; requestType = "DeviceQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'DeviceQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_device-create_1' {
        doc /* Operation device-create. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/device-create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/device-create"; requestType = "DeviceListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'DeviceListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_device-update_2' {
        doc /* Operation device-update. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/device-update. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/device-update"; requestType = "DeviceListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'DeviceListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_device-remove_3' {
        doc /* Operation device-remove. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/device-remove. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/device-remove"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_system-query_4' {
        doc /* Operation system-query. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/system-query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/system-query"; requestType = "SystemQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'SystemQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_system-create_5' {
        doc /* Operation system-create. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/system-create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/system-create"; requestType = "SystemListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'SystemListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_system-update_6' {
        doc /* Operation system-update. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/system-update. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/system-update"; requestType = "SystemListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'SystemListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_system-remove_7' {
        doc /* Operation system-remove. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/system-remove. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/system-remove"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_service-definition-query_8' {
        doc /* Operation service-definition-query. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/service-definition-query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/service-definition-query"; requestType = "PageDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'PageDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_service-definition-create_9' {
        doc /* Operation service-definition-create. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/service-definition-create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/service-definition-create"; requestType = "ServiceDefinitionListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'ServiceDefinitionListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_service-definition-remove_10' {
        doc /* Operation service-definition-remove. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/service-definition-remove. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/service-definition-remove"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_service-query_11' {
        doc /* Operation service-query. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/service-query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/service-query"; requestType = "ServiceInstanceQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_service-create_12' {
        doc /* Operation service-create. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/service-create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/service-create"; requestType = "ServiceInstanceCreateListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceCreateListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_service-update_13' {
        doc /* Operation service-update. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/service-update. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/service-update"; requestType = "ServiceInstanceUpdateListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInstanceUpdateListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_service-remove_14' {
        doc /* Operation service-remove. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/service-remove. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/service-remove"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_interface-template-query_15' {
        doc /* Operation interface-template-query. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/interface-template-query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/interface-template-query"; requestType = "ServiceInterfaceTemplateQueryRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInterfaceTemplateQueryRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_interface-template-create_16' {
        doc /* Operation interface-template-create. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/interface-template-create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/interface-template-create"; requestType = "ServiceInterfaceTemplateListRequestDTO"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCoreData::'ServiceInterfaceTemplateListRequestDTO'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'ServiceRegistry_serviceRegistryManagement_mqtt_interface-template-remove_17' {
        doc /* Operation interface-template-remove. Wire method: MQTT request/reply; address: arrowhead/serviceregistry/management/interface-template-remove. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "REQUEST_REPLY"; address = "arrowhead/serviceregistry/management/interface-template-remove"; requestType = "List<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    port def ServiceRegistry_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-core-java-spring/documentation-tools/model.json */
    }
    port def ServiceRegistry_sysMonitor_AMQP_IDD :> ServiceRegistry_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "../Arrowhead/ah5-core-java-spring/serviceregistry/documentation/v5.2.2/ServiceRegistry-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.ServiceRegistry.ServiceRegistry";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, ServiceRegistry registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.ServiceRegistry.ServiceRegistry",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadCoreData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadCoreData::'Observation'[0..1];
    }
    port def ServiceRegistry_sysMonitor_AMQP_IDD_Implementation :> ServiceRegistry_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'ServiceRegistry_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'ServiceRegistry_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    action def 'ServiceRegistry_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.ServiceRegistry.ServiceRegistry.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCoreData::'Observation'[0..1];
    }
    part def ServiceRegistry_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "ServiceRegistry"; FunctionalProperties = "This document describes the ServiceRegistry core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/serviceregistry, including the local extensions of this workspace.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-core-java-spring/documentation-tools/model.json"; }
        doc /* This document describes the ServiceRegistry core system of the Eclipse Arrowhead 5.2.2 framework, as implemented in Java Spring in ah5-core-java-spring/serviceregistry, including the local extensions of this workspace.
  \end{abstract} */
        port 'deviceDiscovery_SD' : ServiceRegistry_deviceDiscovery_SD;
        port 'systemDiscovery_SD' : ServiceRegistry_systemDiscovery_SD;
        port 'serviceDiscovery_SD' : ServiceRegistry_serviceDiscovery_SD;
        port 'generalManagement_SD' : ServiceRegistry_generalManagement_SD;
        port 'serviceRegistryManagement_SD' : ServiceRegistry_serviceRegistryManagement_SD;
        port 'sysMonitor_SD' : ServiceRegistry_sysMonitor_SD;
    }
    part def ServiceRegistry_SysDD :> ServiceRegistry_SysD {
        @SysDD;
        port 'deviceDiscovery_http' : ServiceRegistry_deviceDiscovery_HTTP_IDD;
        port 'deviceDiscovery_mqtt' : ServiceRegistry_deviceDiscovery_MQTT_IDD;
        port 'systemDiscovery_http' : ServiceRegistry_systemDiscovery_HTTP_IDD;
        port 'systemDiscovery_mqtt' : ServiceRegistry_systemDiscovery_MQTT_IDD;
        port 'serviceDiscovery_http' : ServiceRegistry_serviceDiscovery_HTTP_IDD;
        port 'serviceDiscovery_mqtt' : ServiceRegistry_serviceDiscovery_MQTT_IDD;
        port 'generalManagement_http' : ServiceRegistry_generalManagement_HTTP_IDD;
        port 'generalManagement_mqtt' : ServiceRegistry_generalManagement_MQTT_IDD;
        port 'serviceRegistryManagement_http' : ServiceRegistry_serviceRegistryManagement_HTTP_IDD;
        port 'serviceRegistryManagement_mqtt' : ServiceRegistry_serviceRegistryManagement_MQTT_IDD;
        port 'sysMonitor_amqp' : ServiceRegistry_sysMonitor_AMQP_IDD;
    }
    part def ServiceRegistry :> ServiceRegistry_SysDD {
        @System_Implementation;
    }
}
