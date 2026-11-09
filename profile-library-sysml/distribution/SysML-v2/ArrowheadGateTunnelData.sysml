library package ArrowheadGateTunnelData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'ClosePortsRequest' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'ports' : Real[0..*] { doc /* Source type: Array<Number>. Control ports to close (a bare JSON array is also accepted). */ }
    }
    item def 'CloseResult' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'closed' : Boolean[0..1] { doc /* Source type: Boolean. Whether a session was closed; 404 when false. */ }
    }
    item def 'ConnectConsumerRequest' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'queueId' : String[0..1] { doc /* Source type: String. Relay queue of the provider session. */ }
    }
    item def 'ConsumerSessionRequest' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'sessionId' : String[0..1] { doc /* Source type: String. Session identifier from the provider cloud. */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String. Consumer system. */ }
        attribute 'providerCloud' : String[0..1] { doc /* Source type: String. Provider cloud. */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String. Provider system. */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition. */ }
        attribute 'peer' : String[0..1] { doc /* Source type: String. Peer GateTunnel common name. */ }
    }
    item def 'ControlSessionRequest' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'consumer' : JSONValue[0..1] { doc /* Source type: Object. Consumer system descriptor. */ }
        ref item 'provider' : JSONValue[0..1] { doc /* Source type: Object. Provider system descriptor. */ }
        ref item 'consumerCloud' : JSONValue[0..1] { doc /* Source type: Object. Consumer cloud. */ }
        ref item 'providerCloud' : JSONValue[0..1] { doc /* Source type: Object. Provider cloud. */ }
        ref item 'relay' : JSONValue[0..1] { doc /* Source type: Object. Relay descriptor. */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition. */ }
        attribute 'queueId' : String[0..1] { doc /* Source type: String. Relay queue; generated if absent. */ }
        attribute 'peerName' : String[0..1] { doc /* Source type: String. Peer name; generated if absent. */ }
    }
    item def 'ControlSessionResult' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'sessionId' : String[0..1] { doc /* Source type: String. Session identifier. */ }
        attribute 'queueId' : String[0..1] { doc /* Source type: String. Relay queue. */ }
        attribute 'peerName' : String[0..1] { doc /* Source type: String. Peer name. */ }
        attribute 'serverPort' : Real[0..1] { doc /* Source type: Number. Local control port. */ }
        attribute 'state' : String[0..1] { doc /* Source type: String. Session state. */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
    item def 'ProviderSessionRequest' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'sessionId' : String[0..1] { doc /* Source type: String. Session identifier. */ }
        attribute 'consumer' : String[0..1] { doc /* Source type: String. Consumer system. */ }
        attribute 'consumerCloud' : String[0..1] { doc /* Source type: String. Consumer cloud, must match the peer certificate. */ }
        attribute 'provider' : String[0..1] { doc /* Source type: String. Provider system. */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition. */ }
        attribute 'peer' : String[0..1] { doc /* Source type: String. Peer GateTunnel common name. */ }
        ref item 'target' : JSONValue[0..1] { doc /* Source type: Object. Provider endpoint: address, port, basePath and the negotiated operations. */ }
    }
    item def 'PublicKey' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'algorithm' : String[0..1] { doc /* Source type: String. Key algorithm. */ }
        attribute 'format' : String[0..1] { doc /* Source type: String. X.509. */ }
        attribute 'publicKey' : String[0..1] { doc /* Source type: String. Public key of the GateTunnel certificate, Base64. */ }
    }
    item def 'SessionCloseRequest' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'sessionId' : String[0..1] { doc /* Source type: String. Session to close. */ }
        attribute 'tunnelPort' : Real[0..1] { doc /* Source type: Number. Or the control port to close. */ }
    }
    item def 'SessionEndpoint' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'address' : String[0..1] { doc /* Source type: String. Advertised address of the local session listener. */ }
        attribute 'port' : Real[0..1] { doc /* Source type: Number. Port of the session listener. */ }
        attribute 'sessionId' : String[0..1] { doc /* Source type: String. Session identifier. */ }
    }
    item def 'SessionList' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : JSONValue[0..*] { doc /* Source type: Array<Object>. Control sessions: sessionId, queueId, peerName, serviceDefinition, tunnelPort, state, consumer, provider, consumerCloud, providerCloud, relay, sessionStartedAt. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Number of entries. */ }
    }
    item def 'Status' :> JSONMessage {
        doc /* Source: ah5-GateTunnel-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'status' : String[0..1] { doc /* Source type: String. ok. */ }
        attribute 'system' : String[0..1] { doc /* Source type: String. GateTunnel. */ }
        ref item 'version' : JSONValue[0..1] { doc /* Source type: Version. 5.2.2. */ }
        attribute 'implementation' : String[0..1] { doc /* Source type: String. java. */ }
        ref item 'registration' : JSONValue[0..1] { doc /* Source type: Object. Registration state. */ }
        ref item 'relay' : JSONValue[0..1] { doc /* Source type: Object. Relay connection state. */ }
        ref item 'interCloud' : JSONValue[0..1] { doc /* Source type: Object. providerSessions, consumerSessions and session summaries. */ }
        attribute 'activeSessions' : Real[0..1] { doc /* Source type: Number. Control sessions not closed. */ }
    }
}
