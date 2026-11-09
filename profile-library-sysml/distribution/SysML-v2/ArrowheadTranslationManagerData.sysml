library package ArrowheadTranslationManagerData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'Address' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'type' : String[0..1] { doc /* Source type: String. Address type: HOSTNAME, IPV4, IPV6 or MAC. */ }
        attribute 'address' : String[0..1] { doc /* Source type: String. One network address (host name, IPv4, IPv6 or MAC address). */ }
    }
    item def 'DeviceResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String. Device name. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. Free-form metadata (JSON object). */ }
        ref item 'addresses' : 'Address'[0..*] { doc /* Source type: Array<Address>. Network addresses with their types. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'ErrorMessage' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String. Error text, if the event is an error. */ }
        attribute 'errorCode' : Real[0..1] { doc /* Source type: Number.  */ }
        ref item 'exceptionType' : JSONValue[0..1] { doc /* Source type: ExceptionType.  */ }
        attribute 'origin' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'KeyValues' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'map' : JSONValue[0..1] { doc /* Source type: Object<String>. The configuration keys and values; secrets are not returned. */ }
    }
    item def 'LogEntry' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'logId' : String[0..1] { doc /* Source type: String. Identifier of the log entry. */ }
        ref item 'entryDate' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the log entry. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Log level, e.g. INFO, WARN, ERROR; as filter, the minimum level. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Text of the log entry or status message of the job. */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String. Stack trace or exception text of the log entry, if any. */ }
    }
    item def 'LogEntryListResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'LogEntry'[0..*] { doc /* Source type: Array<LogEntry>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'LogRequest' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        ref item 'from' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries from this UTC time. */ }
        ref item 'to' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries until this UTC time. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Minimum log level to return. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
    }
    item def 'MqttRequestTemplate' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'traceId' : String[0..1] { doc /* Source type: String. Identifier chosen by the requester, returned in the response. */ }
        attribute 'authentication' : String[0..1] { doc /* Source type: String. Identity of the requester (see Section \ref{sec:operations}). */ }
        attribute 'responseTopic' : String[0..1] { doc /* Source type: String. Topic on which the response is published. */ }
        attribute 'qosRequirement' : Real[0..1] { doc /* Source type: Number. MQTT QoS of the response (0, 1 or 2). */ }
        ref item 'params' : JSONValue[0..1] { doc /* Source type: Object<String>. Optional parameters (the query parameters of the HTTP binding). */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object. The operation input. */ }
    }
    item def 'MqttResponseTemplate' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'status' : Real[0..1] { doc /* Source type: Number. Status code. */ }
        attribute 'traceId' : String[0..1] { doc /* Source type: String. The trace identifier of the request. */ }
        attribute 'receiver' : String[0..1] { doc /* Source type: String. Name of the requester. */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object. The operation output, or an ErrorMessage. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
    item def 'Page' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'page' : Real[0..1] { doc /* Source type: Number. Zero-based page number. */ }
        attribute 'size' : Real[0..1] { doc /* Source type: Number. Number of records per page (at most max.page.size, default 1000). */ }
        attribute 'direction' : String[0..1] { doc /* Source type: String. Sort direction, ASC or DESC. */ }
        attribute 'sortField' : String[0..1] { doc /* Source type: String. Field to sort by. */ }
    }
    item def 'ServiceDefinitionResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String. Service definition name. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'ServiceInstanceInterfaceResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'templateName' : String[0..1] { doc /* Source type: String. Name of the interface template, snake_case (e.g. generic_https). */ }
        attribute 'protocol' : String[0..1] { doc /* Source type: String. Protocol of the interface, e.g. http, https, mqtt, mqtts. */ }
        attribute 'policy' : String[0..1] { doc /* Source type: String. Security policy of the interface: NONE, CERT_AUTH or one of the token policies. */ }
        ref item 'properties' : JSONValue[0..1] { doc /* Source type: Object. Interface properties defined by the template, e.g. accessAddresses, accessPort, basePath, operations. */ }
    }
    item def 'ServiceInstanceResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'instanceId' : String[0..1] { doc /* Source type: String. Service instance identifier \texttt{<System>\textbar{}<serviceDefinition>\textbar{}<version>}, or policy instance identifier. */ }
        ref item 'provider' : 'SystemResponse'[0..1] { doc /* Source type: SystemResponse. The provider system. */ }
        ref item 'serviceDefinition' : JSONValue[0..1] { doc /* Source type: \mref[ServiceDefinitionResponse]{ServiceDefinitionResponse}. The service definition. */ }
        ref item 'version' : JSONValue[0..1] { doc /* Source type: Version. Version of the service instance. */ }
        ref item 'expiresAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC expiry time; no value means no expiry. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. Free-form metadata (JSON object). */ }
        ref item 'interfaces' : JSONValue[0..*] { doc /* Source type: Array<\mref[ServiceInstanceInterfaceResponse]{ServiceInstanceInterfaceResponse}>. Service interfaces offered by the instance. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'SystemResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String. System name. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. Free-form metadata (JSON object). */ }
        ref item 'version' : JSONValue[0..1] { doc /* Source type: Version. Version of the system. */ }
        ref item 'addresses' : 'Address'[0..*] { doc /* Source type: Array<Address>. Network addresses with their types. */ }
        ref item 'device' : 'DeviceResponse'[0..1] { doc /* Source type: DeviceResponse. The device hosting the system, if registered. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'TranslationAbortMgmtResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'results' : JSONValue[0..1] { doc /* Source type: Object<Boolean>. Per bridge identifier, whether it was aborted. */ }
    }
    item def 'TranslationBridgeCandidate' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceInstanceId' : String[0..1] { doc /* Source type: String. Service instance identifier \texttt{<System>\textbar{}<serviceDefinition>\textbar{}<version>}. */ }
        attribute 'interfaceTemplateName' : String[0..1] { doc /* Source type: String. Interface template of the candidate the bridge would translate to. */ }
    }
    item def 'TranslationDataModelTranslationDataDescriptor' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'fromModelId' : String[0..1] { doc /* Source type: String. Source data model. */ }
        attribute 'toModelId' : String[0..1] { doc /* Source type: String. Target data model. */ }
        ref item 'interfaceProperties' : JSONValue[0..1] { doc /* Source type: Object. Interface properties of the translator. */ }
        ref item 'configurationSettings' : JSONValue[0..1] { doc /* Source type: Object. Configuration passed to the translator. */ }
    }
    item def 'TranslationDiscoveryMgmtRequest' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'candidates' : 'ServiceInstanceResponse'[0..*] { doc /* Source type: Array<ServiceInstanceResponse>. Candidate service instances (from service discovery). */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String. Consumer on whose behalf the bridge is discovered. */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String. QoS operation: FILTER or SORT. */ }
        attribute 'interfaceTemplateNames' : String[0..*] { doc /* Source type: Array<String>. Accepted interface templates, e.g. generic_https. */ }
        attribute 'inputDataModelId' : String[0..1] { doc /* Source type: String. Data model of the operation input expected by the consumer. */ }
        attribute 'outputDataModelId' : String[0..1] { doc /* Source type: String. Data model of the operation output expected by the consumer. */ }
        ref item 'flags' : JSONValue[0..1] { doc /* Source type: Object<Boolean>. Discovery flags, e.g. CANDIDATES_AUTH_CHECK, CONSUMER_BLACKLIST_CHECK. */ }
    }
    item def 'TranslationDiscoveryRequest' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'candidates' : 'ServiceInstanceResponse'[0..*] { doc /* Source type: Array<ServiceInstanceResponse>. Candidate service instances (from service discovery). */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String. Operation the consumer wants to call. */ }
        attribute 'interfaceTemplateNames' : String[0..*] { doc /* Source type: Array<String>. Interface templates the consumer can use. */ }
        attribute 'inputDataModelId' : String[0..1] { doc /* Source type: String. Data model of the operation input expected by the consumer. */ }
        attribute 'outputDataModelId' : String[0..1] { doc /* Source type: String. Data model of the operation output expected by the consumer. */ }
    }
    item def 'TranslationDiscoveryResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String. Identifier (UUID) of the translation bridge. */ }
        ref item 'candidates' : JSONValue[0..*] { doc /* Source type: Array<\mref[TranslationBridgeCandidate]{TranslationBridgeCandidate}>. Store entries to create. */ }
    }
    item def 'TranslationInterfaceTranslationDataDescriptor' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'fromInterfaceTemplate' : String[0..1] { doc /* Source type: String. Interface template of the consumer side. */ }
        attribute 'toInterfaceTemplate' : String[0..1] { doc /* Source type: String. Interface template of the provider side. */ }
        attribute 'token' : String[0..1] { doc /* Source type: String. The token. */ }
        ref item 'interfaceProperties' : JSONValue[0..1] { doc /* Source type: Object. Interface properties of the translator. */ }
        ref item 'configurationSettings' : JSONValue[0..1] { doc /* Source type: Object. Configuration passed to the translator. */ }
    }
    item def 'TranslationNegotiationMgmtRequest' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String. Identifier (UUID) of the translation bridge. */ }
        attribute 'targetInstanceId' : String[0..1] { doc /* Source type: String. Service instance chosen from the discovery candidates. */ }
    }
    item def 'TranslationNegotiationRequest' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String. Identifier (UUID) of the translation bridge. */ }
        ref item 'target' : 'ServiceInstanceResponse'[0..1] { doc /* Source type: ServiceInstanceResponse. The chosen candidate service instance. */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String. QoS operation: FILTER or SORT. */ }
        attribute 'interfaceTemplateName' : String[0..1] { doc /* Source type: String. Interface template the consumer will use. */ }
        attribute 'inputDataModelId' : String[0..1] { doc /* Source type: String. Data model of the operation input expected by the consumer. */ }
        attribute 'outputDataModelId' : String[0..1] { doc /* Source type: String. Data model of the operation output expected by the consumer. */ }
    }
    item def 'TranslationNegotiationResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String. Identifier (UUID) of the translation bridge. */ }
        ref item 'bridgeInterface' : JSONValue[0..1] { doc /* Source type: \mref[ServiceInstanceInterfaceResponse]{ServiceInstanceInterfaceResponse}. Interface of the bridge the consumer calls instead of the provider. */ }
        ref item 'tokenExpiresAt' : JSONValue[0..1] { doc /* Source type: DateTime. Expiry of the bridge token. */ }
        attribute 'tokenUsageLimit' : Real[0..1] { doc /* Source type: Number. Number of uses allowed with the bridge token. */ }
    }
    item def 'TranslationQueryListResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'TranslationQueryResponse'[0..*] { doc /* Source type: Array<TranslationQueryResponse>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'TranslationQueryRequest' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        attribute 'bridgeIds' : String[0..*] { doc /* Source type: Array<String>. Bridge identifiers to match. */ }
        attribute 'creators' : String[0..*] { doc /* Source type: Array<String>. Systems that created the bridges. */ }
        attribute 'statuses' : String[0..*] { doc /* Source type: Array<String>. Bridge statuses to match. */ }
        attribute 'consumers' : String[0..*] { doc /* Source type: Array<String>. Consumer systems to match. */ }
        attribute 'providers' : String[0..*] { doc /* Source type: Array<String>. Provider system names to match. */ }
        attribute 'serviceDefinitions' : String[0..*] { doc /* Source type: Array<String>. Service definition names to match. */ }
        attribute 'interfaceTranslators' : String[0..*] { doc /* Source type: Array<String>. Interface translator systems to match. */ }
        attribute 'dataModelTranslators' : String[0..*] { doc /* Source type: Array<String>. Data model translator systems to match. */ }
        ref item 'creationFrom' : JSONValue[0..1] { doc /* Source type: DateTime. Lower bound of the creation time. */ }
        ref item 'creationTo' : JSONValue[0..1] { doc /* Source type: DateTime. Upper bound of the creation time. */ }
        ref item 'alivesFrom' : JSONValue[0..1] { doc /* Source type: DateTime. Lower bound of the bridge expiry. */ }
        ref item 'alivesTo' : JSONValue[0..1] { doc /* Source type: DateTime. Upper bound of the bridge expiry. */ }
        attribute 'minUsage' : Real[0..1] { doc /* Source type: Number. Minimum number of usage reports. */ }
        attribute 'maxUsage' : Real[0..1] { doc /* Source type: Number. Maximum number of usage reports. */ }
    }
    item def 'TranslationQueryResponse' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String. Identifier (UUID) of the translation bridge. */ }
        attribute 'status' : String[0..1] { doc /* Source type: String. Bridge status: NEW, DISCOVERED, PENDING, INITIALIZED, USED, ABORTED, CLOSED or ERROR. */ }
        attribute 'usageReportCount' : Real[0..1] { doc /* Source type: Number. Number of usage reports received for the bridge. */ }
        ref item 'alivesAt' : JSONValue[0..1] { doc /* Source type: DateTime. Expiry of the bridge. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Text of the log entry or status message of the job. */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String. Name of the consumer system. */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String. Name of the provider system. */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition name. */ }
        attribute 'operation' : String[0..1] { doc /* Source type: String. QoS operation: FILTER or SORT. */ }
        attribute 'interfaceTranslator' : String[0..1] { doc /* Source type: String. System translating between the interfaces. */ }
        ref item 'interfaceTranslatorData' : JSONValue[0..1] { doc /* Source type: \mref[TranslationInterfaceTranslationDataDescriptor]{TranslationInterfaceTranslationDataDescriptor}. Settings of the interface translation. */ }
        attribute 'inputDataModelTranslator' : String[0..1] { doc /* Source type: String. System translating the input data model. */ }
        ref item 'inputDataModelTranslatorData' : JSONValue[0..1] { doc /* Source type: \mref[TranslationDataModelTranslationDataDescriptor]{TranslationDataModelTranslationDataDescriptor}. Settings of the input data-model translation. */ }
        attribute 'outputDataModelTranslator' : String[0..1] { doc /* Source type: String. System translating the output data model. */ }
        ref item 'outputDataModelTranslatorData' : JSONValue[0..1] { doc /* Source type: \mref[TranslationDataModelTranslationDataDescriptor]{TranslationDataModelTranslationDataDescriptor}. Settings of the output data-model translation. */ }
        attribute 'createdBy' : String[0..1] { doc /* Source type: String. Name of the system that created the record. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'TranslationReportRequest' :> JSONMessage {
        doc /* Source: ah5-TranslationManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bridgeId' : String[0..1] { doc /* Source type: String. Identifier (UUID) of the translation bridge. */ }
        attribute 'timestamp' : String[0..1] { doc /* Source type: String. Time of the event, ISO 8601 UTC. */ }
        attribute 'state' : String[0..1] { doc /* Source type: String. Event: USED, INTERNAL_CLOSED, INTERNAL_ERROR or EXTERNAL_ERROR. */ }
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String. Error text, if the event is an error. */ }
    }
}
