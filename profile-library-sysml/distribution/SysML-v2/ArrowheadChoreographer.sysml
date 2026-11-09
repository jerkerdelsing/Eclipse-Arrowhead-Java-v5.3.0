library package ArrowheadChoreographer {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def Choreographer_choreographerExecutor_SD :> ArrowheadService {
        @SD { MicroserviceName = "choreographerExecutor"; }
        doc /* Service version 1.0.0. Source: ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-SysD.tex */
    }
    port def Choreographer_choreographerExecutor_HTTP_IDD :> Choreographer_choreographerExecutor_SD {
        @IDD { MicroserviceName = "choreographerExecutor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-choreographerExecutor-HTTP-IDD.tex"; }
        attribute basePath : String = "/choreographer/executor";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address choreographer, port 8457 and base path /choreographer/executor. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
register & POST & /register \\ \hline
unregister & DELETE & /unregister \\ \hline
notify-step-done & POST & /notify-step-done \\ \hline
\end{longtable}

\newpage */
        doc /* register: POST /choreographer/executor/register. Input: \mref[ExecutorRegistrationRequest]{ExecutorRegistrationRequest}; output: ExecutorResponse.  */
        in item 'registerRequest' : JSONMessage[0..1];
        out item 'registerResponse' : ArrowheadChoreographerData::'ExecutorResponse'[0..1];
        doc /* unregister: DELETE /choreographer/executor/unregister. Input: None; output: None.  */
        doc /* notify-step-done: POST /choreographer/executor/notify-step-done. Input: ExecutedStepResult; output: None.  */
        in item 'notify-step-doneRequest' : ArrowheadChoreographerData::'ExecutedStepResult'[0..1];
    }
    port def Choreographer_choreographerExecutor_HTTP_IDD_Implementation :> Choreographer_choreographerExecutor_HTTP_IDD { @IDD_Implementation; }
    action def 'Choreographer_choreographerExecutor_http_register_0' {
        doc /* Operation register. Wire method: POST; address: /choreographer/executor/register. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/choreographer/executor/register"; requestType = "\\mref[ExecutorRegistrationRequest]{ExecutorRegistrationRequest}"; responseType = "ExecutorResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : JSONMessage[0..1];
        out item 'response' : ArrowheadChoreographerData::'ExecutorResponse'[0..1];
    }
    action def 'Choreographer_choreographerExecutor_http_unregister_1' {
        doc /* Operation unregister. Wire method: DELETE; address: /choreographer/executor/unregister. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/choreographer/executor/unregister"; requestType = "unspecified"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
    }
    action def 'Choreographer_choreographerExecutor_http_notify-step-done_2' {
        doc /* Operation notify-step-done. Wire method: POST; address: /choreographer/executor/notify-step-done. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/choreographer/executor/notify-step-done"; requestType = "ExecutedStepResult"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadChoreographerData::'ExecutedStepResult'[0..1];
    }
    port def Choreographer_choreographerManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "choreographerManagement"; }
        doc /* Service version 1.0.0. Source: ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-SysD.tex */
    }
    port def Choreographer_choreographerManagement_HTTP_IDD :> Choreographer_choreographerManagement_SD {
        @IDD { MicroserviceName = "choreographerManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-choreographerManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/choreographer/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address choreographer, port 8457 and base path /choreographer/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
query-plans & GET & /plans \\ \hline
get-plan & GET & \texttt{/plans/{id}} \\ \hline
create-plan & POST & /plans \\ \hline
remove-plan & DELETE & \texttt{/plans/{id}} \\ \hline
check-plan & GET & \texttt{/plans/{id}/check} \\ \hline
start-sessions & POST & /sessions/start \\ \hline
abort-session & DELETE & \texttt{/sessions/{id}} \\ \hline
query-sessions & GET & /sessions \\ \hline
query-session-steps & GET & /session-steps \\ \hline
query-worklog & GET & /worklog \\ \hline
query-executors & GET & /executors \\ \hline
get-executor & GET & \texttt{/executors/{id}} \\ \hline
create-executor & POST & /executors \\ \hline
remove-executor & DELETE & \texttt{/executors/{id}} \\ \hline
\end{longtable}

\newpage */
        doc /* query-plans: GET /choreographer/mgmt/plans?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}. Input: None; output: PlanListResponse.  */
        out item 'query-plansResponse' : ArrowheadChoreographerData::'PlanListResponse'[0..1];
        doc /* get-plan: GET /choreographer/mgmt/plans/\fparam{{id}}. Input: String; output: PlanResponse.  */
        in attribute 'get-planRequest' : ScalarValues::String[0..1];
        out item 'get-planResponse' : ArrowheadChoreographerData::'PlanResponse'[0..1];
        doc /* create-plan: POST /choreographer/mgmt/plans. Input: PlanRequest; output: PlanResponse.  */
        in item 'create-planRequest' : ArrowheadChoreographerData::'PlanRequest'[0..1];
        out item 'create-planResponse' : ArrowheadChoreographerData::'PlanResponse'[0..1];
        doc /* remove-plan: DELETE /choreographer/mgmt/plans/\fparam{{id}}. Input: String; output: None.  */
        in attribute 'remove-planRequest' : ScalarValues::String[0..1];
        doc /* check-plan: GET /choreographer/mgmt/plans/\fparam{{id}}/check?allowInterCloud=\fparam{{allowInterCloud}}. Input: String; output: CheckPlanResponse.  */
        in attribute 'check-planRequest' : ScalarValues::String[0..1];
        out item 'check-planResponse' : ArrowheadChoreographerData::'CheckPlanResponse'[0..1];
        doc /* start-sessions: POST /choreographer/mgmt/sessions/start. Input: RunPlanListRequest; output: RunPlanListResponse.  */
        in item 'start-sessionsRequest' : ArrowheadChoreographerData::'RunPlanListRequest'[0..1];
        out item 'start-sessionsResponse' : ArrowheadChoreographerData::'RunPlanListResponse'[0..1];
        doc /* abort-session: DELETE /choreographer/mgmt/sessions/\fparam{{id}}. Input: String; output: None.  */
        in attribute 'abort-sessionRequest' : ScalarValues::String[0..1];
        doc /* query-sessions: GET /choreographer/mgmt/sessions?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}\&planId=\fparam{{planId}}\&status=\fparam{{status}}. Input: None; output: SessionListResponse.  */
        out item 'query-sessionsResponse' : ArrowheadChoreographerData::'SessionListResponse'[0..1];
        doc /* query-session-steps: GET /choreographer/mgmt/session-steps?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}\&sessionId=\fparam{{sessionId}}\&status=\fparam{{status}}. Input: None; output: SessionStepListResponse.  */
        out item 'query-session-stepsResponse' : ArrowheadChoreographerData::'SessionStepListResponse'[0..1];
        doc /* query-worklog: GET /choreographer/mgmt/worklog?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}\&sessionId=\fparam{{sessionId}}\&planName=\fparam{{planName}}\&actionName=\fparam{{actionName}}\&stepName=\fparam{{stepName}}. Input: None; output: WorklogListResponse.  */
        out item 'query-worklogResponse' : ArrowheadChoreographerData::'WorklogListResponse'[0..1];
        doc /* query-executors: GET /choreographer/mgmt/executors?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}. Input: None; output: ExecutorListResponse.  */
        out item 'query-executorsResponse' : ArrowheadChoreographerData::'ExecutorListResponse'[0..1];
        doc /* get-executor: GET /choreographer/mgmt/executors/\fparam{{id}}. Input: String; output: ExecutorResponse.  */
        in attribute 'get-executorRequest' : ScalarValues::String[0..1];
        out item 'get-executorResponse' : ArrowheadChoreographerData::'ExecutorResponse'[0..1];
        doc /* create-executor: POST /choreographer/mgmt/executors. Input: ExecutorCreateRequest; output: ExecutorResponse.  */
        in item 'create-executorRequest' : ArrowheadChoreographerData::'ExecutorCreateRequest'[0..1];
        out item 'create-executorResponse' : ArrowheadChoreographerData::'ExecutorResponse'[0..1];
        doc /* remove-executor: DELETE /choreographer/mgmt/executors/\fparam{{id}}. Input: String; output: None.  */
        in attribute 'remove-executorRequest' : ScalarValues::String[0..1];
    }
    port def Choreographer_choreographerManagement_HTTP_IDD_Implementation :> Choreographer_choreographerManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Choreographer_choreographerManagement_http_query-plans_0' {
        doc /* Operation query-plans. Wire method: GET; address: /choreographer/mgmt/plans?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/choreographer/mgmt/plans?page=\\fparam{{page}}\\&size=\\fparam{{size}}\\&direction=\\fparam{{direction}}\\&sortField=\\fparam{{sortField}}"; requestType = "unspecified"; responseType = "PlanListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadChoreographerData::'PlanListResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_get-plan_1' {
        doc /* Operation get-plan. Wire method: GET; address: /choreographer/mgmt/plans/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/choreographer/mgmt/plans/\\fparam{{id}}"; requestType = "String"; responseType = "PlanResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadChoreographerData::'PlanResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_create-plan_2' {
        doc /* Operation create-plan. Wire method: POST; address: /choreographer/mgmt/plans. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/choreographer/mgmt/plans"; requestType = "PlanRequest"; responseType = "PlanResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadChoreographerData::'PlanRequest'[0..1];
        out item 'response' : ArrowheadChoreographerData::'PlanResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_remove-plan_3' {
        doc /* Operation remove-plan. Wire method: DELETE; address: /choreographer/mgmt/plans/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/choreographer/mgmt/plans/\\fparam{{id}}"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_check-plan_4' {
        doc /* Operation check-plan. Wire method: GET; address: /choreographer/mgmt/plans/\fparam{{id}}/check?allowInterCloud=\fparam{{allowInterCloud}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/choreographer/mgmt/plans/\\fparam{{id}}/check?allowInterCloud=\\fparam{{allowInterCloud}}"; requestType = "String"; responseType = "CheckPlanResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadChoreographerData::'CheckPlanResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_start-sessions_5' {
        doc /* Operation start-sessions. Wire method: POST; address: /choreographer/mgmt/sessions/start. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/choreographer/mgmt/sessions/start"; requestType = "RunPlanListRequest"; responseType = "RunPlanListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadChoreographerData::'RunPlanListRequest'[0..1];
        out item 'response' : ArrowheadChoreographerData::'RunPlanListResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_abort-session_6' {
        doc /* Operation abort-session. Wire method: DELETE; address: /choreographer/mgmt/sessions/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/choreographer/mgmt/sessions/\\fparam{{id}}"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_query-sessions_7' {
        doc /* Operation query-sessions. Wire method: GET; address: /choreographer/mgmt/sessions?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}\&planId=\fparam{{planId}}\&status=\fparam{{status}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/choreographer/mgmt/sessions?page=\\fparam{{page}}\\&size=\\fparam{{size}}\\&direction=\\fparam{{direction}}\\&sortField=\\fparam{{sortField}}\\&planId=\\fparam{{planId}}\\&status=\\fparam{{status}}"; requestType = "unspecified"; responseType = "SessionListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadChoreographerData::'SessionListResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_query-session-steps_8' {
        doc /* Operation query-session-steps. Wire method: GET; address: /choreographer/mgmt/session-steps?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}\&sessionId=\fparam{{sessionId}}\&status=\fparam{{status}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/choreographer/mgmt/session-steps?page=\\fparam{{page}}\\&size=\\fparam{{size}}\\&direction=\\fparam{{direction}}\\&sortField=\\fparam{{sortField}}\\&sessionId=\\fparam{{sessionId}}\\&status=\\fparam{{status}}"; requestType = "unspecified"; responseType = "SessionStepListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadChoreographerData::'SessionStepListResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_query-worklog_9' {
        doc /* Operation query-worklog. Wire method: GET; address: /choreographer/mgmt/worklog?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}\&sessionId=\fparam{{sessionId}}\&planName=\fparam{{planName}}\&actionName=\fparam{{actionName}}\&stepName=\fparam{{stepName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/choreographer/mgmt/worklog?page=\\fparam{{page}}\\&size=\\fparam{{size}}\\&direction=\\fparam{{direction}}\\&sortField=\\fparam{{sortField}}\\&sessionId=\\fparam{{sessionId}}\\&planName=\\fparam{{planName}}\\&actionName=\\fparam{{actionName}}\\&stepName=\\fparam{{stepName}}"; requestType = "unspecified"; responseType = "WorklogListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadChoreographerData::'WorklogListResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_query-executors_10' {
        doc /* Operation query-executors. Wire method: GET; address: /choreographer/mgmt/executors?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/choreographer/mgmt/executors?page=\\fparam{{page}}\\&size=\\fparam{{size}}\\&direction=\\fparam{{direction}}\\&sortField=\\fparam{{sortField}}"; requestType = "unspecified"; responseType = "ExecutorListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadChoreographerData::'ExecutorListResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_get-executor_11' {
        doc /* Operation get-executor. Wire method: GET; address: /choreographer/mgmt/executors/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/choreographer/mgmt/executors/\\fparam{{id}}"; requestType = "String"; responseType = "ExecutorResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadChoreographerData::'ExecutorResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_create-executor_12' {
        doc /* Operation create-executor. Wire method: POST; address: /choreographer/mgmt/executors. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/choreographer/mgmt/executors"; requestType = "ExecutorCreateRequest"; responseType = "ExecutorResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadChoreographerData::'ExecutorCreateRequest'[0..1];
        out item 'response' : ArrowheadChoreographerData::'ExecutorResponse'[0..1];
    }
    action def 'Choreographer_choreographerManagement_http_remove-executor_13' {
        doc /* Operation remove-executor. Wire method: DELETE; address: /choreographer/mgmt/executors/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/choreographer/mgmt/executors/\\fparam{{id}}"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    port def Choreographer_choreographerSession_SD :> ArrowheadService {
        @SD { MicroserviceName = "choreographerSession"; }
        doc /* Service version 1.0.0. Source: ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-SysD.tex */
    }
    port def Choreographer_choreographerSession_HTTP_IDD :> Choreographer_choreographerSession_SD {
        @IDD { MicroserviceName = "choreographerSession"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-choreographerSession-HTTP-IDD.tex"; }
        attribute basePath : String = "/choreographer/session";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address choreographer, port 8457 and base path /choreographer/session. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
start-session & POST & /start \\ \hline
abort-session & DELETE & \texttt{/abort/{id}} \\ \hline
\end{longtable}

\newpage */
        doc /* start-session: POST /choreographer/session/start. Input: StartSessionListRequest; output: RunPlanListResponse.  */
        in item 'start-sessionRequest' : ArrowheadChoreographerData::'StartSessionListRequest'[0..1];
        out item 'start-sessionResponse' : ArrowheadChoreographerData::'RunPlanListResponse'[0..1];
        doc /* abort-session: DELETE /choreographer/session/abort/\fparam{{id}}. Input: String; output: None.  */
        in attribute 'abort-sessionRequest' : ScalarValues::String[0..1];
    }
    port def Choreographer_choreographerSession_HTTP_IDD_Implementation :> Choreographer_choreographerSession_HTTP_IDD { @IDD_Implementation; }
    action def 'Choreographer_choreographerSession_http_start-session_0' {
        doc /* Operation start-session. Wire method: POST; address: /choreographer/session/start. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/choreographer/session/start"; requestType = "StartSessionListRequest"; responseType = "RunPlanListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadChoreographerData::'StartSessionListRequest'[0..1];
        out item 'response' : ArrowheadChoreographerData::'RunPlanListResponse'[0..1];
    }
    action def 'Choreographer_choreographerSession_http_abort-session_1' {
        doc /* Operation abort-session. Wire method: DELETE; address: /choreographer/session/abort/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/choreographer/session/abort/\\fparam{{id}}"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    port def Choreographer_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-SysD.tex */
    }
    port def Choreographer_generalManagement_HTTP_IDD :> Choreographer_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-generalManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/choreographer/general/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address choreographer, port 8457 and base path /choreographer/general/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-log & POST & /logs \\ \hline
get-config & GET & /get-config \\ \hline
\end{longtable}

\newpage */
        doc /* get-log: POST /choreographer/general/mgmt/logs. Input: LogRequest; output: LogEntryListResponse.  */
        in item 'get-logRequest' : ArrowheadChoreographerData::'LogRequest'[0..1];
        out item 'get-logResponse' : ArrowheadChoreographerData::'LogEntryListResponse'[0..1];
        doc /* get-config: GET /choreographer/general/mgmt/get-config?keys=\fparam{{keys}}. Input: None; output: KeyValues.  */
        out item 'get-configResponse' : ArrowheadChoreographerData::'KeyValues'[0..1];
    }
    port def Choreographer_generalManagement_HTTP_IDD_Implementation :> Choreographer_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Choreographer_generalManagement_http_get-log_0' {
        doc /* Operation get-log. Wire method: POST; address: /choreographer/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/choreographer/general/mgmt/logs"; requestType = "LogRequest"; responseType = "LogEntryListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadChoreographerData::'LogRequest'[0..1];
        out item 'response' : ArrowheadChoreographerData::'LogEntryListResponse'[0..1];
    }
    action def 'Choreographer_generalManagement_http_get-config_1' {
        doc /* Operation get-config. Wire method: GET; address: /choreographer/general/mgmt/get-config?keys=\fparam{{keys}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/choreographer/general/mgmt/get-config?keys=\\fparam{{keys}}"; requestType = "unspecified"; responseType = "KeyValues"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadChoreographerData::'KeyValues'[0..1];
    }
    port def Choreographer_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-SysD.tex */
    }
    port def Choreographer_sysMonitor_AMQP_IDD :> Choreographer_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.Choreographer.Choreographer";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, Choreographer registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

\begin{lstlisting}[caption={Registered service instance (TestCloud).}]
{
  "serviceDefinitionName": "sysMonitor",
  "version": "1.0.0",
  "metadata": {
    "contract": "SysMonitor-AMQP-IDD-5.2.2",
    "schemaVersion": "1.0",
    "publisherMode": "native",
    "observationTypes": [
      "echo",
      "power",
      "status"
    ],
    "delivery": "persistent, publisher-confirmed"
  },
  "interfaces": [
    {
      "templateName": "generic_amqps",
      "protocol": "amqps",
      "policy": "CERT_AUTH",
      "properties": {
        "accessAddresses": [
          "amqp-broker"
        ],
        "accessPort": 5671,
        "exchange": "arrowhead",
        "baseRoutingKey": "TestCloud.sysmonitor.Choreographer.Choreographer",
        "operations": [
          "echo",
          "power",
          "status"
        ]
      }
    }
  ]
}
\end{lstlisting} */
        doc /* echo: PUBLISH <Cloud>.sysmonitor.Choreographer.Choreographer.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadChoreographerData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.Choreographer.Choreographer.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadChoreographerData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.Choreographer.Choreographer.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadChoreographerData::'Observation'[0..1];
    }
    port def Choreographer_sysMonitor_AMQP_IDD_Implementation :> Choreographer_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'Choreographer_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Choreographer.Choreographer.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Choreographer.Choreographer.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadChoreographerData::'Observation'[0..1];
    }
    action def 'Choreographer_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Choreographer.Choreographer.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Choreographer.Choreographer.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadChoreographerData::'Observation'[0..1];
    }
    action def 'Choreographer_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Choreographer.Choreographer.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Choreographer.Choreographer.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadChoreographerData::'Observation'[0..1];
    }
    part def Choreographer_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "Choreographer"; FunctionalProperties = "This document describes the Choreographer support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/Choreography and deployed in the Arrowhead-test-cloud-java local cloud.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-Choreographer-java-spring/documentation/v5.2.2/Choreographer-v5.2.2-SysD.tex"; }
        doc /* This document describes the Choreographer support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/Choreography and deployed in the Arrowhead-test-cloud-java local cloud.
  \end{abstract} */
        port 'choreographerExecutor_SD' : Choreographer_choreographerExecutor_SD;
        port 'choreographerManagement_SD' : Choreographer_choreographerManagement_SD;
        port 'choreographerSession_SD' : Choreographer_choreographerSession_SD;
        port 'generalManagement_SD' : Choreographer_generalManagement_SD;
        port 'sysMonitor_SD' : Choreographer_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, ConsumerAuthorization, DynamicServiceOrchestration, Executors, ServiceRegistry. These are external requirements, not assumed co-deployments. */
    }
    part def Choreographer_SysDD :> Choreographer_SysD {
        @SysDD;
        port 'choreographerExecutor_http' : Choreographer_choreographerExecutor_HTTP_IDD;
        port 'choreographerManagement_http' : Choreographer_choreographerManagement_HTTP_IDD;
        port 'choreographerSession_http' : Choreographer_choreographerSession_HTTP_IDD;
        port 'generalManagement_http' : Choreographer_generalManagement_HTTP_IDD;
        port 'sysMonitor_amqp' : Choreographer_sysMonitor_AMQP_IDD;
    }
    part def Choreographer :> Choreographer_SysDD {
        @System_Implementation;
    }
}
