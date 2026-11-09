library package ArrowheadChoreographerData {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    item def 'ActionRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String. Name of the action within the plan. */ }
        attribute 'nextActionName' : String[0..1] { doc /* Source type: String. Action executed after this one. */ }
        attribute 'firstStepNames' : String[0..*] { doc /* Source type: Array<String>. Steps that start the action. */ }
        ref item 'steps' : 'StepRequest'[0..*] { doc /* Source type: Array<StepRequest>. The steps of the action. */ }
    }
    item def 'ActionResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the action. */ }
        attribute 'name' : String[0..1] { doc /* Source type: String. Name of the action within the plan. */ }
        attribute 'firstAction' : Boolean[0..1] { doc /* Source type: Boolean. Whether this is the plan's first action. */ }
        attribute 'nextActionName' : String[0..1] { doc /* Source type: String. Action executed after this one. */ }
        attribute 'firstStepNames' : String[0..*] { doc /* Source type: Array<String>. Steps that start the action. */ }
        ref item 'steps' : 'StepResponse'[0..*] { doc /* Source type: Array<StepResponse>. The steps of the action. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'CheckPlanResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'planId' : Real[0..1] { doc /* Source type: Number. The checked plan. */ }
        attribute 'errorMessages' : String[0..*] { doc /* Source type: Array<String>. Problems found in the plan. */ }
        attribute 'needInterCloud' : Boolean[0..1] { doc /* Source type: Boolean. Whether the plan needs providers in other clouds. */ }
    }
    item def 'ErrorMessage' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'errorMessage' : String[0..1] { doc /* Source type: String. Error text, if the event is an error. */ }
        attribute 'errorCode' : Real[0..1] { doc /* Source type: Number.  */ }
        ref item 'exceptionType' : JSONValue[0..1] { doc /* Source type: ExceptionType.  */ }
        attribute 'origin' : String[0..1] { doc /* Source type: String.  */ }
    }
    item def 'ExecutedStepResult' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'sessionId' : Real[0..1] { doc /* Source type: Number. Identifier of the session. */ }
        attribute 'sessionStepId' : Real[0..1] { doc /* Source type: Number. Identifier of the executed session step. */ }
        ref item 'status' : JSONValue[0..1] { doc /* Source type: ExecutedStepStatus. Result: SUCCESS, ERROR, FATAL_ERROR or ABORTED. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Result message. */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String. Error details, if any. */ }
    }
    item def 'ExecutorCreateRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'systemName' : String[0..1] { doc /* Source type: String. Executor system. */ }
        attribute 'address' : String[0..1] { doc /* Source type: String. One network address (host name, IPv4, IPv6 or MAC address). */ }
        attribute 'port' : Real[0..1] { doc /* Source type: Number. Port of the executor interface. */ }
        attribute 'basePath' : String[0..1] { doc /* Source type: String. Base path of the executor interface. */ }
        attribute 'serviceDefinitionName' : String[0..1] { doc /* Source type: String. Service definition the executor can execute steps for. */ }
        attribute 'minVersion' : Real[0..1] { doc /* Source type: Number. Lowest accepted major version. */ }
        attribute 'maxVersion' : Real[0..1] { doc /* Source type: Number. Highest accepted major version. */ }
    }
    item def 'ExecutorListResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'ExecutorResponse'[0..*] { doc /* Source type: Array<ExecutorResponse>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'ExecutorRegistrationRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'address' : String[0..1] { doc /* Source type: String. Address of the executor; must be one of its ServiceRegistry addresses. */ }
        attribute 'port' : Real[0..1] { doc /* Source type: Number. Port of the executor interface. */ }
        attribute 'basePath' : String[0..1] { doc /* Source type: String. Base path of the executor interface. */ }
        attribute 'serviceDefinitionName' : String[0..1] { doc /* Source type: String. Service definition the executor can execute steps for. */ }
        attribute 'minVersion' : Real[0..1] { doc /* Source type: Number. Lowest accepted major version. */ }
        attribute 'maxVersion' : Real[0..1] { doc /* Source type: Number. Highest accepted major version. */ }
    }
    item def 'ExecutorResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the executor. */ }
        attribute 'name' : String[0..1] { doc /* Source type: String. Executor system. */ }
        attribute 'address' : String[0..1] { doc /* Source type: String. Address of the executor interface. */ }
        attribute 'port' : Real[0..1] { doc /* Source type: Number. Port of the executor interface. */ }
        attribute 'basePath' : String[0..1] { doc /* Source type: String. Base path of the executor interface. */ }
        ref item 'serviceDefinitions' : JSONValue[0..*] { doc /* Source type: Array<\mref[ExecutorServiceDefinitionResponse]{ExecutorServiceDefinitionResponse}>. Service definitions the executor serves. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'ExecutorServiceDefinitionResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceDefinitionName' : String[0..1] { doc /* Source type: String. Service definition name, camelCase (e.g. temperatureReading). */ }
        attribute 'minVersion' : Real[0..1] { doc /* Source type: Number. Lowest accepted major version. */ }
        attribute 'maxVersion' : Real[0..1] { doc /* Source type: Number. Highest accepted major version. */ }
    }
    item def 'KeyValues' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'map' : JSONValue[0..1] { doc /* Source type: Object<String>. The configuration keys and values; secrets are not returned. */ }
    }
    item def 'LogEntry' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'logId' : String[0..1] { doc /* Source type: String. Identifier of the log entry. */ }
        ref item 'entryDate' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the log entry. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Log level, e.g. INFO, WARN, ERROR; as filter, the minimum level. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Text of the log entry or status message of the job. */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String. Stack trace or exception text of the log entry, if any. */ }
    }
    item def 'LogEntryListResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'LogEntry'[0..*] { doc /* Source type: Array<LogEntry>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'LogRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'pagination' : 'Page'[0..1] { doc /* Source type: Page. Paging and sorting of the result. */ }
        ref item 'from' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries from this UTC time. */ }
        ref item 'to' : JSONValue[0..1] { doc /* Source type: DateTime. Return entries until this UTC time. */ }
        attribute 'severity' : String[0..1] { doc /* Source type: String. Minimum log level to return. */ }
        attribute 'logger' : String[0..1] { doc /* Source type: String. Logger (Java class) that wrote the entry; as filter, its name. */ }
    }
    item def 'MetadataRequirement' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
    }
    item def 'Observation' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
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
    item def 'OrchestrationServiceRequirement' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition name. */ }
        attribute 'operations' : String[0..*] { doc /* Source type: Array<String>. Service operations the consumer wants to use. */ }
        ref item 'versions' : JSONValue[0..*] { doc /* Source type: Array<Version>. Accepted service versions. */ }
        ref item 'alivesAt' : JSONValue[0..1] { doc /* Source type: DateTime. Only instances that are still registered (not expired) at this UTC time are matched. */ }
        ref item 'metadataRequirements' : 'MetadataRequirement'[0..*] { doc /* Source type: Array<MetadataRequirement>. Requirements on the service instance metadata; each map is one alternative. */ }
        attribute 'interfaceTemplateNames' : String[0..*] { doc /* Source type: Array<String>. Accepted interface templates, e.g. generic_https. */ }
        attribute 'interfaceAddressTypes' : String[0..*] { doc /* Source type: Array<String>. Address types the provider interface must offer. */ }
        ref item 'interfacePropertyRequirements' : 'MetadataRequirement'[0..*] { doc /* Source type: Array<MetadataRequirement>. Requirements on the interface properties; each map is one alternative. */ }
        attribute 'securityPolicies' : String[0..*] { doc /* Source type: Array<String>. Accepted interface security policies. */ }
        attribute 'preferredProviders' : String[0..*] { doc /* Source type: Array<String>. Provider systems to prefer. */ }
    }
    item def 'Page' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'page' : Real[0..1] { doc /* Source type: Number. Zero-based page number. */ }
        attribute 'size' : Real[0..1] { doc /* Source type: Number. Number of records per page (at most max.page.size, default 1000). */ }
        attribute 'direction' : String[0..1] { doc /* Source type: String. Sort direction, ASC or DESC. */ }
        attribute 'sortField' : String[0..1] { doc /* Source type: String. Field to sort by. */ }
    }
    item def 'PlanListResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'PlanResponse'[0..*] { doc /* Source type: Array<PlanResponse>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'PlanRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String. Unique plan name. */ }
        attribute 'firstActionName' : String[0..1] { doc /* Source type: String. Action the plan starts with. */ }
        ref item 'actions' : 'ActionRequest'[0..*] { doc /* Source type: Array<ActionRequest>. The actions of the plan. */ }
    }
    item def 'PlanResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the plan. */ }
        attribute 'name' : String[0..1] { doc /* Source type: String. Unique plan name. */ }
        attribute 'firstActionName' : String[0..1] { doc /* Source type: String. Action the plan starts with. */ }
        ref item 'actions' : 'ActionResponse'[0..*] { doc /* Source type: Array<ActionResponse>. The actions of the plan. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'RunPlanListRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'sessions' : 'RunPlanRequest'[0..*] { doc /* Source type: Array<RunPlanRequest>. Sessions to start or return. */ }
    }
    item def 'RunPlanListResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'RunPlanResponse'[0..*] { doc /* Source type: Array<RunPlanResponse>. Matching records of the current page. */ }
    }
    item def 'RunPlanRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'planId' : Real[0..1] { doc /* Source type: Number. Identifier of the plan. */ }
        attribute 'quantity' : Real[0..1] { doc /* Source type: Number. Number of times the plan (or step) is executed. */ }
        attribute 'allowInterCloud' : Boolean[0..1] { doc /* Source type: Boolean. Allow inter-cloud orchestration for the steps. */ }
        attribute 'chooseOptimalExecutor' : Boolean[0..1] { doc /* Source type: Boolean. Rank the local executors and choose the best one. */ }
        attribute 'notifyProtocol' : String[0..1] { doc /* Source type: String. Protocol of session notifications, e.g. HTTPS. */ }
        attribute 'notifyAddress' : String[0..1] { doc /* Source type: String. Address of session notifications. */ }
        attribute 'notifyPort' : Real[0..1] { doc /* Source type: Number. Port of session notifications. */ }
        attribute 'notifyPath' : String[0..1] { doc /* Source type: String. Path of session notifications. */ }
    }
    item def 'RunPlanResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'planId' : Real[0..1] { doc /* Source type: Number. Identifier of the plan. */ }
        attribute 'sessionId' : Real[0..1] { doc /* Source type: Number. Identifier of the session. */ }
        attribute 'quantity' : Real[0..1] { doc /* Source type: Number. Requested executions. */ }
        ref item 'status' : JSONValue[0..1] { doc /* Source type: SessionStatus. Status of the session. */ }
        attribute 'errorMessages' : String[0..*] { doc /* Source type: Array<String>. Problems found in the plan. */ }
        attribute 'needInterCloud' : Boolean[0..1] { doc /* Source type: Boolean. Whether the plan needs providers in other clouds. */ }
    }
    item def 'SessionListResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'SessionResponse'[0..*] { doc /* Source type: Array<SessionResponse>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'SessionResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the session. */ }
        attribute 'planId' : Real[0..1] { doc /* Source type: Number. Identifier of the plan. */ }
        attribute 'planName' : String[0..1] { doc /* Source type: String. Name of the plan. */ }
        ref item 'status' : JSONValue[0..1] { doc /* Source type: SessionStatus. Session status. */ }
        attribute 'quantityDone' : Real[0..1] { doc /* Source type: Number. Executions finished so far. */ }
        attribute 'quantityGoal' : Real[0..1] { doc /* Source type: Number. Executions requested. */ }
        attribute 'executionNumber' : Real[0..1] { doc /* Source type: Number. Number of the current execution. */ }
        attribute 'owner' : String[0..1] { doc /* Source type: String. System that started the session. */ }
        attribute 'notifyUri' : String[0..1] { doc /* Source type: String. Where session notifications are sent. */ }
        ref item 'startedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time when the job started. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'SessionStepListResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'SessionStepResponse'[0..*] { doc /* Source type: Array<SessionStepResponse>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'SessionStepResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the session step. */ }
        attribute 'sessionId' : Real[0..1] { doc /* Source type: Number. Identifier of the session. */ }
        attribute 'stepId' : Real[0..1] { doc /* Source type: Number. Identifier of the plan step. */ }
        attribute 'planName' : String[0..1] { doc /* Source type: String. Name of the plan. */ }
        attribute 'actionName' : String[0..1] { doc /* Source type: String. Name of the action. */ }
        attribute 'stepName' : String[0..1] { doc /* Source type: String. Name of the step. */ }
        attribute 'executorName' : String[0..1] { doc /* Source type: String. Executor system running the step. */ }
        ref item 'status' : JSONValue[0..1] { doc /* Source type: SessionStepStatus. Status of the session step. */ }
        attribute 'executionNumber' : Real[0..1] { doc /* Source type: Number. Number of the current execution. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Text of the log entry or status message of the job. */ }
        ref item 'startedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time when the job started. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'StartSessionListRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'sessions' : 'StartSessionRequest'[0..*] { doc /* Source type: Array<StartSessionRequest>. Sessions to start or return. */ }
    }
    item def 'StartSessionRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'planName' : String[0..1] { doc /* Source type: String. Name of the plan. */ }
        attribute 'quantity' : Real[0..1] { doc /* Source type: Number. Number of times the plan (or step) is executed. */ }
        attribute 'allowInterCloud' : Boolean[0..1] { doc /* Source type: Boolean. Allow inter-cloud orchestration for the steps. */ }
        attribute 'chooseOptimalExecutor' : Boolean[0..1] { doc /* Source type: Boolean. Rank the local executors and choose the best one. */ }
        attribute 'notifyProtocol' : String[0..1] { doc /* Source type: String. Protocol of session notifications, e.g. HTTPS. */ }
        attribute 'notifyAddress' : String[0..1] { doc /* Source type: String. Address of session notifications. */ }
        attribute 'notifyPort' : Real[0..1] { doc /* Source type: Number. Port of session notifications. */ }
        attribute 'notifyPath' : String[0..1] { doc /* Source type: String. Path of session notifications. */ }
    }
    item def 'StepRequest' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'name' : String[0..1] { doc /* Source type: String. Name of the step within the action. */ }
        attribute 'quantity' : Real[0..1] { doc /* Source type: Number. Number of times the plan (or step) is executed. */ }
        ref item 'serviceRequirement' : JSONValue[0..1] { doc /* Source type: \mref[OrchestrationServiceRequirement]{OrchestrationServiceRequirement}. Orchestration requirement the executor's provider must meet. */ }
        attribute 'localCloudOnly' : Boolean[0..1] { doc /* Source type: Boolean. Use only providers of the local cloud. */ }
        ref item 'staticParameters' : JSONValue[0..1] { doc /* Source type: Object<String>. Parameters passed to the executor unchanged. */ }
        attribute 'nextStepNames' : String[0..*] { doc /* Source type: Array<String>. Steps that start when this one has finished. */ }
    }
    item def 'StepResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the step. */ }
        attribute 'name' : String[0..1] { doc /* Source type: String. Name of the step within the action. */ }
        attribute 'serviceDefinition' : String[0..1] { doc /* Source type: String. Service definition of the step's requirement. */ }
        attribute 'minVersion' : Real[0..1] { doc /* Source type: Number. Lowest major version derived from versions. */ }
        attribute 'maxVersion' : Real[0..1] { doc /* Source type: Number. Highest major version derived from versions. */ }
        attribute 'quantity' : Real[0..1] { doc /* Source type: Number. Number of times the plan (or step) is executed. */ }
        ref item 'serviceRequirement' : JSONValue[0..1] { doc /* Source type: \mref[OrchestrationServiceRequirement]{OrchestrationServiceRequirement}. Orchestration requirement the executor's provider must meet. */ }
        attribute 'localCloudOnly' : Boolean[0..1] { doc /* Source type: Boolean. Use only providers of the local cloud. */ }
        ref item 'staticParameters' : JSONValue[0..1] { doc /* Source type: Object<String>. Parameters passed to the executor unchanged. */ }
        attribute 'nextStepNames' : String[0..*] { doc /* Source type: Array<String>. Steps that start when this one has finished. */ }
        ref item 'createdAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of creation. */ }
        ref item 'updatedAt' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the last change. */ }
    }
    item def 'WorklogListResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        ref item 'entries' : 'WorklogResponse'[0..*] { doc /* Source type: Array<WorklogResponse>. Matching records of the current page. */ }
        attribute 'count' : Real[0..1] { doc /* Source type: Number. Total number of matching records (all pages). */ }
    }
    item def 'WorklogResponse' :> JSONMessage {
        doc /* Source: ah5-Choreographer-java-spring/documentation/v5.2.2. Field cardinalities are unspecified by this extraction; [0..1] is deliberately permissive. */
        attribute 'id' : Real[0..1] { doc /* Source type: Number. Identifier of the worklog entry. */ }
        ref item 'entryDate' : JSONValue[0..1] { doc /* Source type: DateTime. UTC time of the log entry. */ }
        attribute 'planName' : String[0..1] { doc /* Source type: String. Name of the plan. */ }
        attribute 'actionName' : String[0..1] { doc /* Source type: String. Name of the action. */ }
        attribute 'stepName' : String[0..1] { doc /* Source type: String. Name of the step. */ }
        attribute 'sessionId' : Real[0..1] { doc /* Source type: Number. Identifier of the session. */ }
        attribute 'executionNumber' : Real[0..1] { doc /* Source type: Number. Number of the current execution. */ }
        attribute 'message' : String[0..1] { doc /* Source type: String. Worklog text. */ }
        attribute 'exception' : String[0..1] { doc /* Source type: String. Stack trace or exception text of the log entry, if any. */ }
    }
}
