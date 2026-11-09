library package ArrowheadLocalCloudManager {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def LocalCloudManager_localCloudManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "localCloudManagement"; }
        doc /* Service version 1.0.0. Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2/LocalCloudManager-v5.2.2-SysD.tex */
    }
    port def LocalCloudManager_localCloudManagement_HTTP_IDD :> LocalCloudManager_localCloudManagement_SD {
        @IDD { MicroserviceName = "localCloudManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-LocalCloudManager-java-spring/documentation/v5.2.2/LocalCloudManager-v5.2.2-localCloudManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "(path";
        attribute templateName : String = "generic_http";
        attribute policy : String = "NONE:";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_http and policy NONE: the console API is reached through the nginx UI on port 8490 (loopback of the host); the API itself listens on 8491. Base path /api. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
session & GET & /session \\ \hline
login & POST & /session \\ \hline
logout & DELETE & /session \\ \hline
overview & GET & /overview \\ \hline
serviceRegistryManagement & GET & /service-registry-management \\ \hline
dataManager & GET & /datamanager \\ \hline
consumers & GET & /consumers \\ \hline
interCloud & GET & /intercloud \\ \hline
sysMonitorHistory & GET & /sysmonitor \\ \hline
sysMonitorSubscriptions & GET & /sysmonitor/subscriptions \\ \hline
subscribeSysMonitor & PUT & /sysmonitor/subscriptions \\ \hline
unsubscribeSysMonitor & DELETE & /sysmonitor/subscriptions \\ \hline
plantDescriptions & GET & /plantdescriptions \\ \hline
createPlantDescription & POST & /plantdescriptions \\ \hline
replacePlantDescription & PUT & /plantdescriptions \\ \hline
updatePlantDescription & PATCH & /plantdescriptions \\ \hline
removePlantDescription & DELETE & /plantdescriptions \\ \hline
orchestrationRules & GET & /orchestration/rules \\ \hline
createOrchestrationRule & POST & /orchestration/rules \\ \hline
removeOrchestrationRule & DELETE & /orchestration/rules \\ \hline
logs & GET & /logs \\ \hline
lifecycle & POST & /containers \\ \hline
\end{longtable}

\newpage */
        doc /* session: GET /api/session. Input: None; output: Session.  */
        out item 'sessionResponse' : ArrowheadLocalCloudManagerData::'Session'[0..1];
        doc /* login: POST /api/session. Input: Login; output: None.  */
        in item 'loginRequest' : ArrowheadLocalCloudManagerData::'Login'[0..1];
        doc /* logout: DELETE /api/session. Input: None; output: None.  */
        doc /* overview: GET /api/overview. Input: None; output: View.  */
        out item 'overviewResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* serviceRegistryManagement: GET /api/service-registry-management. Input: None; output: View.  */
        out item 'serviceRegistryManagementResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* dataManager: GET /api/datamanager?limit=\fparam{{limit}}. Input: None; output: View.  */
        out item 'dataManagerResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* consumers: GET /api/consumers. Input: None; output: View.  */
        out item 'consumersResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* interCloud: GET /api/intercloud. Input: None; output: View.  */
        out item 'interCloudResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* sysMonitorHistory: GET /api/sysmonitor?system=\fparam{{system}}\&limit=\fparam{{limit}}. Input: None; output: View.  */
        out item 'sysMonitorHistoryResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* sysMonitorSubscriptions: GET /api/sysmonitor/subscriptions. Input: None; output: View.  */
        out item 'sysMonitorSubscriptionsResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* subscribeSysMonitor: PUT /api/sysmonitor/subscriptions. Input: \mref[SysMonitorSubscriptionRequest]{SysMonitorSubscriptionRequest}; output: View.  */
        in item 'subscribeSysMonitorRequest' : JSONMessage[0..1];
        out item 'subscribeSysMonitorResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* unsubscribeSysMonitor: DELETE /api/sysmonitor/subscriptions. Input: String; output: None.  */
        in attribute 'unsubscribeSysMonitorRequest' : ScalarValues::String[0..1];
        doc /* plantDescriptions: GET /api/plantdescriptions. Input: None; output: View.  */
        out item 'plantDescriptionsResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* createPlantDescription: POST /api/plantdescriptions. Input: PlantDescription; output: PlantDescription.  */
        in item 'createPlantDescriptionRequest' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
        out item 'createPlantDescriptionResponse' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
        doc /* replacePlantDescription: PUT /api/plantdescriptions. Input: PlantDescription; output: PlantDescription.  */
        in item 'replacePlantDescriptionRequest' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
        out item 'replacePlantDescriptionResponse' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
        doc /* updatePlantDescription: PATCH /api/plantdescriptions. Input: PlantDescription; output: PlantDescription.  */
        in item 'updatePlantDescriptionRequest' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
        out item 'updatePlantDescriptionResponse' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
        doc /* removePlantDescription: DELETE /api/plantdescriptions. Input: String; output: None.  */
        in attribute 'removePlantDescriptionRequest' : ScalarValues::String[0..1];
        doc /* orchestrationRules: GET /api/orchestration/rules. Input: None; output: View.  */
        out item 'orchestrationRulesResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* createOrchestrationRule: POST /api/orchestration/rules. Input: OrchestrationRule; output: View.  */
        in item 'createOrchestrationRuleRequest' : ArrowheadLocalCloudManagerData::'OrchestrationRule'[0..1];
        out item 'createOrchestrationRuleResponse' : ArrowheadLocalCloudManagerData::'View'[0..1];
        doc /* removeOrchestrationRule: DELETE /api/orchestration/rules. Input: String; output: None.  */
        in attribute 'removeOrchestrationRuleRequest' : ScalarValues::String[0..1];
        doc /* logs: GET /api/logs?service=\fparam{{service}}\&tail=\fparam{{tail}}. Input: None; output: LogLines.  */
        out item 'logsResponse' : ArrowheadLocalCloudManagerData::'LogLines'[0..1];
        doc /* lifecycle: POST /api/containers. Input: String; output: LifecycleResult.  */
        in attribute 'lifecycleRequest' : ScalarValues::String[0..1];
        out item 'lifecycleResponse' : ArrowheadLocalCloudManagerData::'LifecycleResult'[0..1];
    }
    port def LocalCloudManager_localCloudManagement_HTTP_IDD_Implementation :> LocalCloudManager_localCloudManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'LocalCloudManager_localCloudManagement_http_session_0' {
        doc /* Operation session. Wire method: GET; address: /api/session. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/session"; requestType = "unspecified"; responseType = "Session"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'Session'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_login_1' {
        doc /* Operation login. Wire method: POST; address: /api/session. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/api/session"; requestType = "Login"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadLocalCloudManagerData::'Login'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_logout_2' {
        doc /* Operation logout. Wire method: DELETE; address: /api/session. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/api/session"; requestType = "unspecified"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
    }
    action def 'LocalCloudManager_localCloudManagement_http_overview_3' {
        doc /* Operation overview. Wire method: GET; address: /api/overview. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/overview"; requestType = "unspecified"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_serviceRegistryManagement_4' {
        doc /* Operation serviceRegistryManagement. Wire method: GET; address: /api/service-registry-management. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/service-registry-management"; requestType = "unspecified"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_dataManager_5' {
        doc /* Operation dataManager. Wire method: GET; address: /api/datamanager?limit=\fparam{{limit}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/datamanager?limit=\\fparam{{limit}}"; requestType = "unspecified"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_consumers_6' {
        doc /* Operation consumers. Wire method: GET; address: /api/consumers. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/consumers"; requestType = "unspecified"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_interCloud_7' {
        doc /* Operation interCloud. Wire method: GET; address: /api/intercloud. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/intercloud"; requestType = "unspecified"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_sysMonitorHistory_8' {
        doc /* Operation sysMonitorHistory. Wire method: GET; address: /api/sysmonitor?system=\fparam{{system}}\&limit=\fparam{{limit}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/sysmonitor?system=\\fparam{{system}}\\&limit=\\fparam{{limit}}"; requestType = "unspecified"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_sysMonitorSubscriptions_9' {
        doc /* Operation sysMonitorSubscriptions. Wire method: GET; address: /api/sysmonitor/subscriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/sysmonitor/subscriptions"; requestType = "unspecified"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_subscribeSysMonitor_10' {
        doc /* Operation subscribeSysMonitor. Wire method: PUT; address: /api/sysmonitor/subscriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/api/sysmonitor/subscriptions"; requestType = "\\mref[SysMonitorSubscriptionRequest]{SysMonitorSubscriptionRequest}"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : JSONMessage[0..1];
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_unsubscribeSysMonitor_11' {
        doc /* Operation unsubscribeSysMonitor. Wire method: DELETE; address: /api/sysmonitor/subscriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/api/sysmonitor/subscriptions"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_plantDescriptions_12' {
        doc /* Operation plantDescriptions. Wire method: GET; address: /api/plantdescriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/plantdescriptions"; requestType = "unspecified"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_createPlantDescription_13' {
        doc /* Operation createPlantDescription. Wire method: POST; address: /api/plantdescriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/api/plantdescriptions"; requestType = "PlantDescription"; responseType = "PlantDescription"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
        out item 'response' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_replacePlantDescription_14' {
        doc /* Operation replacePlantDescription. Wire method: PUT; address: /api/plantdescriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/api/plantdescriptions"; requestType = "PlantDescription"; responseType = "PlantDescription"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
        out item 'response' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_updatePlantDescription_15' {
        doc /* Operation updatePlantDescription. Wire method: PATCH; address: /api/plantdescriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PATCH"; address = "/api/plantdescriptions"; requestType = "PlantDescription"; responseType = "PlantDescription"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
        out item 'response' : ArrowheadLocalCloudManagerData::'PlantDescription'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_removePlantDescription_16' {
        doc /* Operation removePlantDescription. Wire method: DELETE; address: /api/plantdescriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/api/plantdescriptions"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_orchestrationRules_17' {
        doc /* Operation orchestrationRules. Wire method: GET; address: /api/orchestration/rules. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/orchestration/rules"; requestType = "unspecified"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_createOrchestrationRule_18' {
        doc /* Operation createOrchestrationRule. Wire method: POST; address: /api/orchestration/rules. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/api/orchestration/rules"; requestType = "OrchestrationRule"; responseType = "View"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadLocalCloudManagerData::'OrchestrationRule'[0..1];
        out item 'response' : ArrowheadLocalCloudManagerData::'View'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_removeOrchestrationRule_19' {
        doc /* Operation removeOrchestrationRule. Wire method: DELETE; address: /api/orchestration/rules. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/api/orchestration/rules"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_logs_20' {
        doc /* Operation logs. Wire method: GET; address: /api/logs?service=\fparam{{service}}\&tail=\fparam{{tail}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/api/logs?service=\\fparam{{service}}\\&tail=\\fparam{{tail}}"; requestType = "unspecified"; responseType = "LogLines"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'LogLines'[0..1];
    }
    action def 'LocalCloudManager_localCloudManagement_http_lifecycle_21' {
        doc /* Operation lifecycle. Wire method: POST; address: /api/containers. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/api/containers"; requestType = "String"; responseType = "LifecycleResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadLocalCloudManagerData::'LifecycleResult'[0..1];
    }
    port def LocalCloudManager_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-LocalCloudManager-java-spring/documentation/v5.2.2/LocalCloudManager-v5.2.2-SysD.tex */
    }
    port def LocalCloudManager_sysMonitor_AMQP_IDD :> LocalCloudManager_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-LocalCloudManager-java-spring/documentation/v5.2.2/LocalCloudManager-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.LocalCloudManager.LocalCloudManager";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, LocalCloudManager registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.LocalCloudManager.LocalCloudManager",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.LocalCloudManager.LocalCloudManager.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadLocalCloudManagerData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.LocalCloudManager.LocalCloudManager.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadLocalCloudManagerData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.LocalCloudManager.LocalCloudManager.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadLocalCloudManagerData::'Observation'[0..1];
    }
    port def LocalCloudManager_sysMonitor_AMQP_IDD_Implementation :> LocalCloudManager_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'LocalCloudManager_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.LocalCloudManager.LocalCloudManager.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.LocalCloudManager.LocalCloudManager.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'Observation'[0..1];
    }
    action def 'LocalCloudManager_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.LocalCloudManager.LocalCloudManager.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.LocalCloudManager.LocalCloudManager.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'Observation'[0..1];
    }
    action def 'LocalCloudManager_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.LocalCloudManager.LocalCloudManager.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.LocalCloudManager.LocalCloudManager.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadLocalCloudManagerData::'Observation'[0..1];
    }
    part def LocalCloudManager_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "LocalCloudManager"; FunctionalProperties = "This document describes the LocalCloudManager support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/LocalCloudManager and deployed in the Arrowhead-test-cloud-java local cloud.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-LocalCloudManager-java-spring/documentation/v5.2.2/LocalCloudManager-v5.2.2-SysD.tex"; }
        doc /* This document describes the LocalCloudManager support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/LocalCloudManager and deployed in the Arrowhead-test-cloud-java local cloud.
  \end{abstract} */
        port 'localCloudManagement_SD' : LocalCloudManager_localCloudManagement_SD;
        port 'sysMonitor_SD' : LocalCloudManager_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: Authentication, ConsumerAuthorization, DataManager, Docker proxy, DynamicServiceOrchestration, Gatekeeper, GateTunnel, PlantDescriptionEngine, ServiceRegistry, SimpleStoreServiceOrchestration, SysMonitor. These are external requirements, not assumed co-deployments. */
    }
    part def LocalCloudManager_SysDD :> LocalCloudManager_SysD {
        @SysDD;
        port 'localCloudManagement_http' : LocalCloudManager_localCloudManagement_HTTP_IDD;
        port 'sysMonitor_amqp' : LocalCloudManager_sysMonitor_AMQP_IDD;
    }
    part def LocalCloudManager :> LocalCloudManager_SysDD {
        @System_Implementation;
    }
}
