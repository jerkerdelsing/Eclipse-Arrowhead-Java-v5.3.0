library package ArrowheadDataManagerData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'ErrorMessage' :> JSONMessage {
        doc /* Source: ah5-DataManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String. Error text, if the event is an error. */ }
        attribute 'errorCode' : Real[0..1] { doc /* Source type: Number.  */ }
        ref item 'exceptionType' : JSONValue[0..1] { doc /* Source type: ExceptionType.  */ }
        attribute 'origin' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-DataManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
    item def 'SenMLRecord' :> JSONMessage {
        doc /* Source: ah5-DataManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'bn' : String[0..1] { doc /* Source type: String. SenML base name. */ }
        ref item 'bt' : JSONValue[0..1] { doc /* Source type: Object. SenML base time (seconds). */ }
        attribute 'bu' : String[0..1] { doc /* Source type: String. SenML base unit. */ }
        ref item 'bv' : JSONValue[0..1] { doc /* Source type: Object. SenML base value. */ }
        ref item 'bs' : JSONValue[0..1] { doc /* Source type: Object. SenML base sum. */ }
        ref item 'bver' : JSONValue[0..1] { doc /* Source type: Object. SenML base version. */ }
        attribute 'n' : String[0..1] { doc /* Source type: String. SenML name. */ }
        attribute 'u' : String[0..1] { doc /* Source type: String. SenML unit. */ }
        ref item 'v' : JSONValue[0..1] { doc /* Source type: Object. SenML numeric value. */ }
        attribute 'vs' : String[0..1] { doc /* Source type: String. SenML string value. */ }
        attribute 'vb' : Boolean[0..1] { doc /* Source type: Boolean. SenML Boolean value. */ }
        attribute 'vd' : String[0..1] { doc /* Source type: String. SenML data value (Base64). */ }
        ref item 's' : JSONValue[0..1] { doc /* Source type: Object. SenML sum. */ }
        ref item 't' : JSONValue[0..1] { doc /* Source type: Object. SenML time (seconds, relative to the base time). */ }
        ref item 'ut' : JSONValue[0..1] { doc /* Source type: Object. SenML update time (seconds). */ }
    }
    item def 'ServiceListResponse' :> JSONMessage {
        doc /* Source: ah5-DataManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'services' : String[0..*] { doc /* Source type: Array<String>. Service names with stored data. */ }
    }
    item def 'SystemListResponse' :> JSONMessage {
        doc /* Source: ah5-DataManager-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systems' : String[0..*] { doc /* Source type: Array<String>. System names with stored data. */ }
    }
}
