library package ArrowheadEventHandlerData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'ErrorMessage' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String. Error text, if the event is an error. */ }
        attribute 'errorCode' : Real[0..1] { doc /* Source type: Number.  */ }
        ref item 'exceptionType' : JSONValue[0..1] { doc /* Source type: ExceptionType.  */ }
        attribute 'origin' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'EventPublishRequest' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'eventType' : String[0..1] { doc /* Source type: String. Event type, camelCase. */ }
        attribute 'payload' : String[0..1] { doc /* Source type: String. Event payload (text). */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object<String>. Event metadata, matched against subscription filters. */ }
        ref item 'timestamp' : JSONValue[0..1] { doc /* Source type: Object. Time of the event (RFC 3339). */ }
    }
    item def 'EventPublishResult' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'matchedSubscriptions' : Real[0..1] { doc /* Source type: Number. Number of subscriptions the event matched. */ }
        attribute 'acceptedDeliveries' : Real[0..1] { doc /* Source type: Number. Number of notifications accepted for delivery. */ }
    }
    item def 'KeyValues' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'map' : JSONValue[0..1] { doc /* Source type: Object<String>. The configuration keys and values; secrets are not returned. */ }
    }
    item def 'LogEntry' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'logId' : String[0..1] { doc /* Source type: String. Identifier of the log entry. */ }
        ref item 'entryDate' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the log entry. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Log level, e.g. INFO, WARN, ERROR; as filter, the minimum level. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Text of the log entry or status message of the job. */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String. Stack trace or exception text of the log entry, if any. */ }
    }
    item def 'LogEntryListResponse' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'LogEntry'[0..*] { doc /* Source type: Array<LogEntry>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'LogRequest' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        ref item 'from' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries from this UTC time. */ }
        ref item 'to' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries until this UTC time. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Minimum log level to return. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'page' : Real[0..1] { doc /* Source type: Number. Zero-based page number. */ }
        attribute 'size' : Real[0..1] { doc /* Source type: Number. Number of records per page (at most max.page.size, default 1000). */ }
        attribute 'direction' : String[0..1] { doc /* Source type: String. Sort direction, ASC or DESC. */ }
        attribute 'sortField' : String[0..1] { doc /* Source type: String. Field to sort by. */ }
    }
    item def 'SubscriptionListResponse' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'SubscriptionResponse'[0..*] { doc /* Source type: Array<SubscriptionResponse>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'SubscriptionRequest' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'eventType' : String[0..1] { doc /* Source type: String. Event type, camelCase. */ }
        attribute 'notifyUri' : String[0..1] { doc /* Source type: String. HTTPS endpoint to which matching events are posted. */ }
        attribute 'matchMetadata' : Boolean[0..1] { doc /* Source type: Boolean. Whether only events whose metadata contain metadataFilter are delivered. */ }
        ref item 'metadataFilter' : JSONValue[0..1] { doc /* Source type: Object<String>. Metadata an event must contain to be delivered. */ }
        ref item 'startsAt' : JSONValue[0..1] { doc /* Source type: Object. Start of the subscription's validity window. */ }
        ref item 'expiresAt' : JSONValue[0..1] { doc /* Source type: Object. End of the validity window. */ }
    }
    item def 'SubscriptionResponse' :> JSONMessage {
        doc /* Source: ah5-EventHandler-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the subscription. */ }
        attribute 'subscriberSystemName' : String[0..1] { doc /* Source type: String. System that owns the subscription. */ }
        attribute 'eventType' : String[0..1] { doc /* Source type: String. Event type, camelCase. */ }
        attribute 'notifyUri' : String[0..1] { doc /* Source type: String. HTTPS endpoint to which matching events are posted. */ }
        attribute 'matchMetadata' : Boolean[0..1] { doc /* Source type: Boolean. Whether only events whose metadata contain metadataFilter are delivered. */ }
        ref item 'metadataFilter' : JSONValue[0..1] { doc /* Source type: Object<String>. Metadata an event must contain to be delivered. */ }
        ref item 'startsAt' : JSONValue[0..1] { doc /* Source type: Object. Start of the subscription's validity window. */ }
        ref item 'expiresAt' : JSONValue[0..1] { doc /* Source type: Object. End of the validity window. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: Object. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: Object. UTC time of the last change. */ }
    }
}
