library package ArrowheadDeviceQoSEvaluatorData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'ErrorMessage' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String. Error text, if the event is an error. */ }
        attribute 'errorCode' : Real[0..1] { doc /* Source type: Number.  */ }
        ref item 'exceptionType' : JSONValue[0..1] { doc /* Source type: ExceptionType.  */ }
        attribute 'origin' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'KeyValues' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'map' : JSONValue[0..1] { doc /* Source type: Object<String>. The configuration keys and values; secrets are not returned. */ }
    }
    item def 'LogEntry' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'logId' : String[0..1] { doc /* Source type: String. Identifier of the log entry. */ }
        ref item 'entryDate' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the log entry. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Log level, e.g. INFO, WARN, ERROR; as filter, the minimum level. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Text of the log entry or status message of the job. */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String. Stack trace or exception text of the log entry, if any. */ }
    }
    item def 'LogEntryListResponse' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'LogEntry'[0..*] { doc /* Source type: Array<LogEntry>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'LogRequest' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        ref item 'from' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries from this UTC time. */ }
        ref item 'to' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries until this UTC time. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Minimum log level to return. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
    }
    item def 'MqttRequestTemplate' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'traceId' : String[0..1] { doc /* Source type: String. Identifier chosen by the requester, returned in the response. */ }
        attribute 'authentication' : String[0..1] { doc /* Source type: String. Identity of the requester (see Section \ref{sec:operations}). */ }
        attribute 'responseTopic' : String[0..1] { doc /* Source type: String. Topic on which the response is published. */ }
        attribute 'qosRequirement' : Real[0..1] { doc /* Source type: Number. MQTT QoS of the response (0, 1 or 2). */ }
        ref item 'params' : JSONValue[0..1] { doc /* Source type: Object<String>. Optional parameters (the query parameters of the HTTP binding). */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object. The operation input. */ }
    }
    item def 'MqttResponseTemplate' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'status' : Real[0..1] { doc /* Source type: Number. Status code. */ }
        attribute 'traceId' : String[0..1] { doc /* Source type: String. The trace identifier of the request. */ }
        attribute 'receiver' : String[0..1] { doc /* Source type: String. Name of the requester. */ }
        ref item 'payload' : JSONValue[0..1] { doc /* Source type: Object. The operation output, or an ErrorMessage. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'page' : Real[0..1] { doc /* Source type: Number. Zero-based page number. */ }
        attribute 'size' : Real[0..1] { doc /* Source type: Number. Number of records per page (at most max.page.size, default 1000). */ }
        attribute 'direction' : String[0..1] { doc /* Source type: String. Sort direction, ASC or DESC. */ }
        attribute 'sortField' : String[0..1] { doc /* Source type: String. Field to sort by. */ }
    }
    item def 'QoSDeviceStatQueryRequest' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        attribute 'metricGroup' : String[0..1] { doc /* Source type: String. Metric group: RTT, CPU_TOTAL_LOAD, MEMORY_USED, NETWORK_EGRESS_LOAD or NETWORK_INGRESS_LOAD. */ }
        ref item 'from' : JSONValue[0..1] { doc /* Source type: DateTime. Lower bound of the entry time. */ }
        ref item 'to' : JSONValue[0..1] { doc /* Source type: DateTime. Upper bound of the entry time. */ }
        attribute 'aggregation' : String[0..*] { doc /* Source type: Array<String>. Statistics to return (minimum, maximum, mean, median, current). */ }
        attribute 'systemNames' : String[0..*] { doc /* Source type: Array<String>. Systems whose devices are returned. */ }
    }
    item def 'QoSDeviceStatQueryResponse' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'records' : 'QoSDeviceStatRecord'[0..*] { doc /* Source type: Array<QoSDeviceStatRecord>. Statistic records of the page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'QoSDeviceStatRecord' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'metricGroup' : String[0..1] { doc /* Source type: String. Metric group: RTT, CPU_TOTAL_LOAD, MEMORY_USED, NETWORK_EGRESS_LOAD or NETWORK_INGRESS_LOAD. */ }
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the record. */ }
        attribute 'timestamp' : String[0..1] { doc /* Source type: String. End of the aggregation period. */ }
        attribute 'uuid' : String[0..1] { doc /* Source type: String. Identifier of the measured device. */ }
        ref item 'minimum' : JSONValue[0..1] { doc /* Source type: Object. Minimum value in the period. */ }
        ref item 'maximum' : JSONValue[0..1] { doc /* Source type: Object. Maximum value in the period. */ }
        ref item 'mean' : JSONValue[0..1] { doc /* Source type: Object. Mean value in the period. */ }
        ref item 'median' : JSONValue[0..1] { doc /* Source type: Object. Median value in the period. */ }
        ref item 'current' : JSONValue[0..1] { doc /* Source type: Object. Last measured value. */ }
        attribute 'systems' : String[0..*] { doc /* Source type: Array<String>. Systems hosted by the device. */ }
    }
    item def 'QoSEvaluationFilterResponse' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'passedProviders' : String[0..*] { doc /* Source type: Array<String>. Providers that meet the threshold. */ }
        attribute 'droppedProviders' : String[0..*] { doc /* Source type: Array<String>. Providers that do not meet it. */ }
        ref item 'warnings' : JSONValue[0..1] { doc /* Source type: Object<Array<String>>. Per provider, warnings such as missing measurements. */ }
    }
    item def 'QoSEvaluationRequest' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'providers' : String[0..*] { doc /* Source type: Array<String>. Candidate provider systems. */ }
        ref item 'configuration' : JSONValue[0..1] { doc /* Source type: Object. Evaluation settings: metricNames (e.g. RTT_MEAN), metricWeights, timeWindow (seconds) and threshold. */ }
    }
    item def 'QoSEvaluationSortResponse' :> JSONMessage {
        doc /* Source: ah5-DeviceQoSEvaluator-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'sortedProviders' : String[0..*] { doc /* Source type: Array<String>. Providers ordered from best to worst. */ }
        ref item 'warnings' : JSONValue[0..1] { doc /* Source type: Object<Array<String>>. Per provider, warnings such as missing measurements. */ }
    }
}
