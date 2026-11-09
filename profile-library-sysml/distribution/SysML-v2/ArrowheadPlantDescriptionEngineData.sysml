library package ArrowheadPlantDescriptionEngineData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'Connection' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'consumer' : 'SystemPort'[0..1] { doc /* Source type: SystemPort. The consumer port of the connection. */ }
        ref item 'producer' : 'SystemPort'[0..1] { doc /* Source type: SystemPort. The producer port of the connection. */ }
        attribute 'priority' : Real[0..1] { doc /* Source type: Number. Priority of the resulting orchestration rules (1 highest). */ }
    }
    item def 'ErrorMessage' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String. Error text, if the event is an error. */ }
        attribute 'errorCode' : Real[0..1] { doc /* Source type: Number.  */ }
        ref item 'exceptionType' : JSONValue[0..1] { doc /* Source type: ExceptionType.  */ }
        attribute 'origin' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'KeyValues' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'map' : JSONValue[0..1] { doc /* Source type: Object<String>. The configuration keys and values; secrets are not returned. */ }
    }
    item def 'LogEntry' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'logId' : String[0..1] { doc /* Source type: String. Identifier of the log entry. */ }
        ref item 'entryDate' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the log entry. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Log level, e.g. INFO, WARN, ERROR; as filter, the minimum level. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Text of the log entry or status message of the job. */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String. Stack trace or exception text of the log entry, if any. */ }
    }
    item def 'LogEntryListResponse' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'LogEntry'[0..*] { doc /* Source type: Array<LogEntry>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'LogRequest' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        ref item 'from' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries from this UTC time. */ }
        ref item 'to' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries until this UTC time. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Minimum log level to return. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
    }
    item def 'MonitorPlantDescriptionListResponse' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : JSONValue[0..*] { doc /* Source type: Array<\mref[MonitorPlantDescriptionResponse]{MonitorPlantDescriptionResponse}>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'MonitorPlantDescriptionResponse' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the plant description. */ }
        attribute 'plantDescription' : String[0..1] { doc /* Source type: String. Name of the plant description. */ }
        attribute 'active' : Boolean[0..1] { doc /* Source type: Boolean. Whether the entry is in force. */ }
        attribute 'include' : Real[0..*] { doc /* Source type: Array<Number>. Identifiers of included plant descriptions. */ }
        ref item 'systems' : 'MonitorSystem'[0..*] { doc /* Source type: Array<MonitorSystem>. Systems to create or update. */ }
        ref item 'connections' : 'Connection'[0..*] { doc /* Source type: Array<Connection>. Connections between consumer and producer ports. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'MonitorSystem' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemId' : String[0..1] { doc /* Source type: String. Identifier of the system within the plant description. */ }
        attribute 'systemName' : String[0..1] { doc /* Source type: String. Name of the system, PascalCase (e.g. TemperatureSensor). */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object<String>. Metadata the registered system must have. */ }
        ref item 'ports' : 'Port'[0..*] { doc /* Source type: Array<Port>. Services the system consumes or produces. */ }
        attribute 'registeredSystemName' : String[0..1] { doc /* Source type: String. Name of the registered system matched to this plant system. */ }
        ref item 'monitoring' : JSONValue[0..1] { doc /* Source type: Object. Latest monitoring information from SysMonitor. */ }
        ref item 'lastObservationTime' : JSONValue[0..1] { doc /* Source type: DateTime. Time of the latest stored observation of the system. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'page' : Real[0..1] { doc /* Source type: Number. Zero-based page number. */ }
        attribute 'size' : Real[0..1] { doc /* Source type: Number. Number of records per page (at most max.page.size, default 1000). */ }
        attribute 'direction' : String[0..1] { doc /* Source type: String. Sort direction, ASC or DESC. */ }
        attribute 'sortField' : String[0..1] { doc /* Source type: String. Field to sort by. */ }
    }
    item def 'PdeAlarmListResponse' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'PdeAlarmResponse'[0..*] { doc /* Source type: Array<PdeAlarmResponse>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'PdeAlarmResponse' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the alarm. */ }
        attribute 'systemId' : String[0..1] { doc /* Source type: String. Identifier of the system within the plant description. */ }
        attribute 'systemName' : String[0..1] { doc /* Source type: String. Name of the system the alarm concerns. */ }
        attribute 'acknowledged' : Boolean[0..1] { doc /* Source type: Boolean. Whether an operator acknowledged the alarm. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Severity of the alarm. */ }
        attribute 'cause' : String[0..1] { doc /* Source type: String. Alarm cause: SYSTEM_NOT_REGISTERED, MULTIPLE_MATCHES, SYSTEM_NOT_IN_DESCRIPTION, NOT_MONITORED or NO_RECENT_OBSERVATION. */ }
        attribute 'description' : String[0..1] { doc /* Source type: String. Human-readable alarm text. */ }
        ref item 'raisedAt' : JSONValue[0..1] { doc /* Source type: DateTime. Time the alarm was raised. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
        ref item 'clearedAt' : JSONValue[0..1] { doc /* Source type: DateTime. Time the alarm was cleared, if cleared. */ }
        ref item 'acknowledgedAt' : JSONValue[0..1] { doc /* Source type: DateTime. Time of the acknowledgement. */ }
    }
    item def 'PdeAlarmUpdate' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'acknowledged' : Boolean[0..1] { doc /* Source type: Boolean. Set to true to acknowledge the alarm. */ }
    }
    item def 'PdeSystem' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemId' : String[0..1] { doc /* Source type: String. Identifier of the system within the plant description. */ }
        attribute 'systemName' : String[0..1] { doc /* Source type: String. Name of the registered system (optional; metadata may identify it instead). */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object<String>. Metadata the registered system must have. */ }
        ref item 'ports' : 'Port'[0..*] { doc /* Source type: Array<Port>. Services the system consumes or produces. */ }
    }
    item def 'PlantDescriptionListResponse' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'PlantDescriptionResponse'[0..*] { doc /* Source type: Array<PlantDescriptionResponse>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'PlantDescriptionRequest' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'plantDescription' : String[0..1] { doc /* Source type: String. Name of the plant description. */ }
        attribute 'active' : Boolean[0..1] { doc /* Source type: Boolean. Make this the active plant description. */ }
        attribute 'include' : Real[0..*] { doc /* Source type: Array<Number>. Identifiers of included plant descriptions. */ }
        ref item 'systems' : 'PdeSystem'[0..*] { doc /* Source type: Array<PdeSystem>. Systems to create or update. */ }
        ref item 'connections' : 'Connection'[0..*] { doc /* Source type: Array<Connection>. Connections between consumer and producer ports. */ }
    }
    item def 'PlantDescriptionResponse' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the plant description. */ }
        attribute 'plantDescription' : String[0..1] { doc /* Source type: String. Name of the plant description. */ }
        attribute 'active' : Boolean[0..1] { doc /* Source type: Boolean. Whether this is the active plant description. */ }
        attribute 'include' : Real[0..*] { doc /* Source type: Array<Number>. Identifiers of included plant descriptions. */ }
        ref item 'systems' : 'PdeSystem'[0..*] { doc /* Source type: Array<PdeSystem>. Systems to create or update. */ }
        ref item 'connections' : 'Connection'[0..*] { doc /* Source type: Array<Connection>. Connections between consumer and producer ports. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'PlantDescriptionUpdate' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'plantDescription' : String[0..1] { doc /* Source type: String. Name of the plant description. */ }
        attribute 'active' : Boolean[0..1] { doc /* Source type: Boolean. Make this the active plant description. */ }
        attribute 'include' : Real[0..*] { doc /* Source type: Array<Number>. Identifiers of included plant descriptions. */ }
        ref item 'systems' : 'PdeSystem'[0..*] { doc /* Source type: Array<PdeSystem>. Systems to create or update. */ }
        ref item 'connections' : 'Connection'[0..*] { doc /* Source type: Array<Connection>. Connections between consumer and producer ports. */ }
    }
    item def 'Port' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'portName' : String[0..1] { doc /* Source type: String. Name of the port within its system. */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition name. */ }
        attribute 'serviceInterface' : String[0..1] { doc /* Source type: String. Required interface template of the port, if any. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object<String>. Metadata the service instance must have. */ }
        attribute 'consumer' : Boolean[0..1] { doc /* Source type: Boolean. True for a consumed service, false for a produced one. */ }
    }
    item def 'SystemPort' :> JSONMessage {
        doc /* Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemId' : String[0..1] { doc /* Source type: String. Identifier of the system within the plant description. */ }
        attribute 'portName' : String[0..1] { doc /* Source type: String. Name of the port within its system. */ }
    }
}
