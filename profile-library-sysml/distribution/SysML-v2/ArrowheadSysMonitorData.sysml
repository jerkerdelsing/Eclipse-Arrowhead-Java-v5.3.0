library package ArrowheadSysMonitorData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'Latest' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String. The system. */ }
        ref item 'latest' : JSONValue[0..1] { doc /* Source type: Object. Latest observation per type. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
    item def 'ObservationList' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String. The system. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Number of observations. */ }
        ref item 'observations' : 'StoredObservation'[0..*] { doc /* Source type: Array<StoredObservation>. Newest first. */ }
    }
    item def 'ProviderList' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'defaultSubscription' : String[0..1] { doc /* Source type: String. all or none (SYSMONITOR_DEFAULT_SUBSCRIPTION). */ }
        ref item 'list' : JSONValue[0..*] { doc /* Source type: Array<Object>. Orchestrated sysMonitor providers: providerSystem, serviceInstanceId, baseRoutingKey, exchange, observationTypes, broker, port, orchestratedAt. */ }
    }
    item def 'ReconcileResult' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'jobId' : String[0..1] { doc /* Source type: String. Identifier of the started re-orchestration. */ }
    }
    item def 'Status' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'status' : String[0..1] { doc /* Source type: String. running or degraded. */ }
        attribute 'system' : String[0..1] { doc /* Source type: String. SysMonitor. */ }
        attribute 'implementation' : String[0..1] { doc /* Source type: String. java. */ }
        ref item 'version' : JSONValue[0..1] { doc /* Source type: Version. 5.2.2. */ }
        ref item 'amqp' : JSONValue[0..1] { doc /* Source type: Object. Intake connection, bindings and last message time. */ }
        ref item 'storage' : JSONValue[0..1] { doc /* Source type: Object. History counts, retention and pruning. */ }
        attribute 'subscriptions' : Real[0..1] { doc /* Source type: Number. Number of subscriptions. */ }
    }
    item def 'StoredObservation' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'schemaVersion' : String[0..1] { doc /* Source type: String. 1.0. */ }
        attribute 'observationId' : String[0..1] { doc /* Source type: String. Identifier (publisher's, or a SHA-256 of routing key and content). */ }
        attribute 'providerSystem' : String[0..1] { doc /* Source type: String. Publishing system. */ }
        attribute 'serviceInstance' : String[0..1] { doc /* Source type: String. Service instance. */ }
        attribute 'observationType' : String[0..1] { doc /* Source type: String. Observation type. */ }
        ref item 'sourceTime' : JSONValue[0..1] { doc /* Source type: DateTime. Publisher time, UTC. */ }
        ref item 'receivedTime' : JSONValue[0..1] { doc /* Source type: DateTime. Time SysMonitor received it. */ }
        ref item 'publishedTime' : JSONValue[0..1] { doc /* Source type: DateTime. Time it was forwarded on sysMonitorData. */ }
        ref item 'value' : JSONValue[0..1] { doc /* Source type: Object. Observed value. */ }
        attribute 'unit' : String[0..1] { doc /* Source type: String. Unit. */ }
        attribute 'quality' : String[0..1] { doc /* Source type: String. Quality. */ }
        ref item 'pipeline' : JSONValue[0..1] { doc /* Source type: Object. Intake details (routing key, exchange). */ }
    }
    item def 'Subscription' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'providerSystem' : String[0..1] { doc /* Source type: String. Provider system. */ }
        attribute 'observationTypes' : String[0..*] { doc /* Source type: Array<String>. Selected types. */ }
        attribute 'enabled' : Boolean[0..1] { doc /* Source type: Boolean. Whether active. */ }
        attribute 'revision' : Real[0..1] { doc /* Source type: Number. Revision, also returned as ETag. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. Time of the last change. */ }
        attribute 'updatedBy' : String[0..1] { doc /* Source type: String. Last editor. */ }
    }
    item def 'SubscriptionList' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'defaultSubscription' : String[0..1] { doc /* Source type: String. all or none. */ }
        ref item 'list' : 'Subscription'[0..*] { doc /* Source type: Array<Subscription>. Subscriptions with providerFound and unknownTypes. */ }
    }
    item def 'SubscriptionRequest' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'observationTypes' : String[0..*] { doc /* Source type: Array<String>. Types to receive; * for all (default). */ }
        attribute 'enabled' : Boolean[0..1] { doc /* Source type: Boolean. Default true. */ }
    }
    item def 'SystemList' :> JSONMessage {
        doc /* Source: ah5-SysMonitor-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systems' : String[0..*] { doc /* Source type: Array<String>. Systems with stored observations. */ }
        ref item 'retentionDays' : JSONValue[0..1] { doc /* Source type: Object. History retention in days. */ }
    }
}
