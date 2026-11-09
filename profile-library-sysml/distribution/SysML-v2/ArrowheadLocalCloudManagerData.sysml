library package ArrowheadLocalCloudManagerData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'LifecycleResult' :> JSONMessage {
        doc /* Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'service' : String[0..1] { doc /* Source type: String. Compose service. */ }
        attribute 'action' : String[0..1] { doc /* Source type: String. start, stop or restart. */ }
        attribute 'accepted' : Boolean[0..1] { doc /* Source type: Boolean. Forwarded to the Docker proxy. */ }
        attribute 'at' : String[0..1] { doc /* Source type: String. Time of the action. */ }
    }
    item def 'LogLines' :> JSONMessage {
        doc /* Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'service' : String[0..1] { doc /* Source type: String. Compose service. */ }
        attribute 'lines' : String[0..*] { doc /* Source type: Array<String>. Log lines (tail latest). */ }
        ref item 'collectedAt' : JSONValue[0..1] { doc /* Source type: DateTime. Collection time. */ }
    }
    item def 'Login' :> JSONMessage {
        doc /* Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'password' : String[0..1] { doc /* Source type: String. Console password (JSON only; 204 on success, 401 wrong, 429 locked). */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
    item def 'OrchestrationRule' :> JSONMessage {
        doc /* Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'consumer' : String[0..1] { doc /* Source type: String. Consumer system. */ }
        attribute 'serviceInstanceId' : String[0..1] { doc /* Source type: String. Service instance. */ }
        attribute 'priority' : Real[0..1] { doc /* Source type: Number. Priority (1 highest). */ }
    }
    item def 'PlantDescription' :> JSONMessage {
        doc /* Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item '*' : JSONValue[0..1] { doc /* Source type: Object. Plant description as in PlantDescriptionEngine plantDescriptionManagement. */ }
    }
    item def 'Session' :> JSONMessage {
        doc /* Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'authenticated' : Boolean[0..1] { doc /* Source type: Boolean. Whether the request carries a valid console session. */ }
        attribute 'configured' : Boolean[0..1] { doc /* Source type: Boolean. Whether a console password is configured. */ }
    }
    item def 'SysMonitorSubscriptionRequest' :> JSONMessage {
        doc /* Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'observationTypes' : String[0..*] { doc /* Source type: Array<String>. Types to receive, * for all. */ }
        attribute 'enabled' : Boolean[0..1] { doc /* Source type: Boolean. Default true. */ }
    }
    item def 'View' :> JSONMessage {
        doc /* Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item '*' : JSONValue[0..1] { doc /* Source type: Object. View document assembled from the providing systems; a failing source is reported in the document instead of failing the request. */ }
    }
}
