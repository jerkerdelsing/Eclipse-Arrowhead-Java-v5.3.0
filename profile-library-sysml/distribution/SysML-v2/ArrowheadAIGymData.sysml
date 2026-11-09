library package ArrowheadAIGymData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-AIGym-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'schemaVersion' : String[0..1] { doc /* Source type: String. 1.0. */ }
        attribute 'observationId' : String[0..1] { doc /* Source type: String. <System>-<type>-<epoch ms>-<sequence>. */ }
        attribute 'providerSystem' : String[0..1] { doc /* Source type: String. The publishing system. */ }
        attribute 'serviceInstance' : String[0..1] { doc /* Source type: String. Service instance, default the system name. */ }
        attribute 'observationType' : String[0..1] { doc /* Source type: String. echo, power, status, trainingSample, inference. */ }
        attribute 'sourceTime' : String[0..1] { doc /* Source type: String. ISO 8601 UTC time with milliseconds. */ }
        attribute 'sequence' : Real[0..1] { doc /* Source type: Number. Sequence number per publisher. */ }
        ref item 'value' : JSONValue[0..1] { doc /* Source type: Object. Observed value, see the operations. */ }
        attribute 'unit' : String[0..1] { doc /* Source type: String. 1 for power and status; absent for echo. */ }
        attribute 'quality' : String[0..1] { doc /* Source type: String. good or uncertain. */ }
        ref item 'metadata' : JSONValue[0..1] { doc /* Source type: Object. \texttt{{"publisherMode": "native"}}. */ }
    }
    item def 'Prediction' :> JSONMessage {
        doc /* Source: ah5-AIGym-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'model' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'value' : JSONValue[0..1] { doc /* Source type: Object.  */ }
        attribute 'neighbours' : Real[0..1] { doc /* Source type: Number.  */ }
        attribute 'trainingSamples' : Real[0..1] { doc /* Source type: Number.  */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: Object. UTC time of creation. */ }
    }
    item def 'PredictionRequest' :> JSONMessage {
        doc /* Source: ah5-AIGym-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'model' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'features' : JSONValue[0..1] { doc /* Source type: Object<Object>.  */ }
        attribute 'neighbours' : Real[0..1] { doc /* Source type: Number.  */ }
    }
    item def 'Sample' :> JSONMessage {
        doc /* Source: ah5-AIGym-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : String[0..1] { doc /* Source type: String. Identifier of the record. */ }
        attribute 'model' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'features' : JSONValue[0..1] { doc /* Source type: Object<Object>.  */ }
        ref item 'target' : JSONValue[0..1] { doc /* Source type: Object. Target of the policy or token: a service definition or event type. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: Object. UTC time of creation. */ }
    }
    item def 'SampleRequest' :> JSONMessage {
        doc /* Source: ah5-AIGym-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'model' : String[0..1] { doc /* Source type: String.  */ }
        ref item 'features' : JSONValue[0..1] { doc /* Source type: Object<Object>.  */ }
        ref item 'target' : JSONValue[0..1] { doc /* Source type: Object. Target of the policy or token: a service definition or event type. */ }
    }
}
