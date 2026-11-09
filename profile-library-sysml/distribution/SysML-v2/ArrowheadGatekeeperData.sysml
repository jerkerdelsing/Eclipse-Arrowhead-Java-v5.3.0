library package ArrowheadGatekeeperData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'CloudList' :> JSONMessage {
        doc /* Source: ah5-Gatekeeper-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : JSONValue[0..*] { doc /* Source type: Array<Object>. Configured neighbour clouds (name, operator, gatekeeperAddress, gatekeeperPort, metadata). */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Number of entries. */ }
    }
    item def 'GlobalServiceDiscoveryRequest' :> JSONMessage {
        doc /* Source: ah5-Gatekeeper-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'serviceRequirement' : JSONValue[0..1] { doc /* Source type: Object. Orchestration service requirement; serviceDefinition is required, operations are checked (alias requestedService). */ }
        attribute 'targetClouds' : String[0..*] { doc /* Source type: Array<String>. Clouds to ask (<Cloud> or \texttt{<Cloud>\textbar{}<Organization>}); default all configured peer clouds. */ }
        attribute 'consumerSystem' : String[0..1] { doc /* Source type: String. Consumer on whose behalf the orchestrator asks; default the requester. */ }
    }
    item def 'GlobalServiceDiscoveryResult' :> JSONMessage {
        doc /* Source: ah5-Gatekeeper-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'results' : 'InterCloudProvider'[0..*] { doc /* Source type: Array<InterCloudProvider>. Authorized providers of all asked clouds. */ }
        attribute 'unsuccessfulRequests' : Real[0..1] { doc /* Source type: Number. Number of clouds that did not answer correctly. */ }
        attribute 'queriedClouds' : Real[0..1] { doc /* Source type: Number. Number of clouds asked. */ }
    }
    item def 'InterCloudNegotiationRequest' :> JSONMessage {
        doc /* Source: ah5-Gatekeeper-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'targetCloud' : String[0..1] { doc /* Source type: String. Provider cloud, <Cloud> or \texttt{<Cloud>\textbar{}<Organization>} (alias providerCloud). */ }
        attribute 'providerName' : String[0..1] { doc /* Source type: String. Provider system. */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition. */ }
        attribute 'operations' : String[0..*] { doc /* Source type: Array<String>. Operations to negotiate; default all offered. */ }
        attribute 'consumerSystem' : String[0..1] { doc /* Source type: String. Consumer; default the requester. */ }
    }
    item def 'InterCloudNegotiationResult' :> JSONMessage {
        doc /* Source: ah5-Gatekeeper-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'negotiated' : Boolean[0..1] { doc /* Source type: Boolean. Whether a session was opened. */ }
        attribute 'reason' : String[0..1] { doc /* Source type: String. Refusal reason (when not negotiated). */ }
        attribute 'providerCloud' : String[0..1] { doc /* Source type: String. Provider cloud, \texttt{<Cloud>\textbar{}<Organization>}. */ }
        attribute 'providerName' : String[0..1] { doc /* Source type: String. Provider system. */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition. */ }
        ref item 'version' : JSONValue[0..1] { doc /* Source type: Version. Service version. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. Instance metadata. */ }
        attribute 'sessionId' : String[0..1] { doc /* Source type: String. GateTunnel session. */ }
        ref item 'interface' : JSONValue[0..1] { doc /* Source type: Object. generic_https interface of the local GateTunnel session endpoint (accessAddresses, accessPort, basePath, operations). */ }
    }
    item def 'InterCloudProvider' :> JSONMessage {
        doc /* Source: ah5-Gatekeeper-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'cloud' : String[0..1] { doc /* Source type: String. Provider cloud, \texttt{<Cloud>\textbar{}<Organization>}. */ }
        attribute 'providerName' : String[0..1] { doc /* Source type: String. Provider system. */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition. */ }
        ref item 'version' : JSONValue[0..1] { doc /* Source type: Version. Service version. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. Instance metadata. */ }
        attribute 'operations' : String[0..*] { doc /* Source type: Array<String>. Offered operations. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-Gatekeeper-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
    item def 'RelayList' :> JSONMessage {
        doc /* Source: ah5-Gatekeeper-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : JSONValue[0..*] { doc /* Source type: Array<Object>. Configured relays (name, address, port, type, secure, metadata). */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Number of entries. */ }
    }
    item def 'Status' :> JSONMessage {
        doc /* Source: ah5-Gatekeeper-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'status' : String[0..1] { doc /* Source type: String. ok. */ }
        attribute 'system' : String[0..1] { doc /* Source type: String. Gatekeeper. */ }
        ref item 'version' : JSONValue[0..1] { doc /* Source type: Version. 5.2.2. */ }
        attribute 'implementation' : String[0..1] { doc /* Source type: String. java. */ }
        attribute 'x509' : Boolean[0..1] { doc /* Source type: Boolean. Mutual TLS in use. */ }
        ref item 'registration' : JSONValue[0..1] { doc /* Source type: Object. Registration state in the ServiceRegistry. */ }
        ref item 'relay' : JSONValue[0..1] { doc /* Source type: Object. Connection state of the inter-cloud relay. */ }
    }
}
