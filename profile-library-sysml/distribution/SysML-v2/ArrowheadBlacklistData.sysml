library package ArrowheadBlacklistData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'BlacklistCreateListRequest' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entities' : 'BlacklistCreateRequest'[0..*] { doc /* Source type: Array<BlacklistCreateRequest>. Entries to create. */ }
    }
    item def 'BlacklistCreateRequest' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String. System to blacklist. */ }
        ref item 'expiresAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC expiry time; no value means no expiry. */ }
        attribute 'reason' : String[0..1] { doc /* Source type: String. Reason of the blacklisting. */ }
    }
    item def 'BlacklistEntry' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String. The blacklisted system. */ }
        attribute 'createdBy' : String[0..1] { doc /* Source type: String. System that created the entry. */ }
        attribute 'revokedBy' : String[0..1] { doc /* Source type: String. System that inactivated the entry, if any. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
        attribute 'reason' : String[0..1] { doc /* Source type: String. Reason of the blacklisting. */ }
        ref item 'expiresAt' : JSONValue[0..1] { doc /* Source type: DateTime. Expiry of the entry; no value means permanent. */ }
        attribute 'active' : Boolean[0..1] { doc /* Source type: Boolean. Whether the entry is in force. */ }
    }
    item def 'BlacklistEntryListResponse' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'BlacklistEntry'[0..*] { doc /* Source type: Array<BlacklistEntry>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'BlacklistQueryRequest' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        attribute 'systemNames' : String[0..*] { doc /* Source type: Array<String>. System names to match. */ }
        attribute 'mode' : String[0..1] { doc /* Source type: String. Which entries to return: ALL, ACTIVES or INACTIVES. */ }
        attribute 'issuers' : String[0..*] { doc /* Source type: Array<String>. Systems that created the entries. */ }
        attribute 'revokers' : String[0..*] { doc /* Source type: Array<String>. Systems that inactivated the entries. */ }
        attribute 'reason' : String[0..1] { doc /* Source type: String. Reason of the blacklisting. */ }
        ref item 'alivesAt' : JSONValue[0..1] { doc /* Source type: DateTime. Only entries in force at this time. */ }
    }
    item def 'ErrorMessage' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String. Error text, if the event is an error. */ }
        attribute 'errorCode' : Real[0..1] { doc /* Source type: Number.  */ }
        ref item 'exceptionType' : JSONValue[0..1] { doc /* Source type: ExceptionType.  */ }
        attribute 'origin' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'KeyValues' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'map' : JSONValue[0..1] { doc /* Source type: Object<String>. The configuration keys and values; secrets are not returned. */ }
    }
    item def 'LogEntry' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'logId' : String[0..1] { doc /* Source type: String. Identifier of the log entry. */ }
        ref item 'entryDate' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the log entry. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Log level, e.g. INFO, WARN, ERROR; as filter, the minimum level. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Text of the log entry or status message of the job. */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String. Stack trace or exception text of the log entry, if any. */ }
    }
    item def 'LogEntryListResponse' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'LogEntry'[0..*] { doc /* Source type: Array<LogEntry>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'LogRequest' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        ref item 'from' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries from this UTC time. */ }
        ref item 'to' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries until this UTC time. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Minimum log level to return. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
    }
    item def 'MqttRequestTemplate' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'traceId' : String[0..1] { doc /* Source type: String. Identifier chosen by the requester, returned in the response. */ }
        attribute 'authentication' : String[0..1] { doc /* Source type: String. Identity of the requester (see Section \ref{sec:operations}). */ }
        attribute 'responseTopic' : String[0..1] { doc /* Source type: String. Topic on which the response is published. */ }
        attribute 'qosRequirement' : Real[0..1] { doc /* Source type: Number. MQTT QoS of the response (0, 1 or 2). */ }
        ref item 'params' : JSONValue[0..1] { doc /* Source type: Object<String>. Optional parameters (the query parameters of the HTTP binding). */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object. The operation input. */ }
    }
    item def 'MqttResponseTemplate' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'status' : Real[0..1] { doc /* Source type: Number. Status code. */ }
        attribute 'traceId' : String[0..1] { doc /* Source type: String. The trace identifier of the request. */ }
        attribute 'receiver' : String[0..1] { doc /* Source type: String. Name of the requester. */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object. The operation output, or an ErrorMessage. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
        doc /* Source: ah5-Blacklist-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'page' : Real[0..1] { doc /* Source type: Number. Zero-based page number. */ }
        attribute 'size' : Real[0..1] { doc /* Source type: Number. Number of records per page (at most max.page.size, default 1000). */ }
        attribute 'direction' : String[0..1] { doc /* Source type: String. Sort direction, ASC or DESC. */ }
        attribute 'sortField' : String[0..1] { doc /* Source type: String. Field to sort by. */ }
    }
}
