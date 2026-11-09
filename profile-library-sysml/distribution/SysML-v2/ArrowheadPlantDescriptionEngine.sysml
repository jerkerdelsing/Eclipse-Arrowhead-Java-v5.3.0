library package ArrowheadPlantDescriptionEngine {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def PlantDescriptionEngine_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-SysD.tex */
    }
    port def PlantDescriptionEngine_generalManagement_HTTP_IDD :> PlantDescriptionEngine_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-generalManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/plantdescriptionengine/general/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address plantdescriptionengine, port 8432 and base path /plantdescriptionengine/general/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-log & POST & /logs \\ \hline
get-config & GET & /get-config \\ \hline
\end{longtable}

\newpage */
        doc /* get-log: POST /plantdescriptionengine/general/mgmt/logs. Input: LogRequest; output: LogEntryListResponse.  */
        in item 'get-logRequest' : ArrowheadPlantDescriptionEngineData::'LogRequest'[0..1];
        out item 'get-logResponse' : ArrowheadPlantDescriptionEngineData::'LogEntryListResponse'[0..1];
        doc /* get-config: GET /plantdescriptionengine/general/mgmt/get-config?keys=\fparam{{keys}}. Input: None; output: KeyValues.  */
        out item 'get-configResponse' : ArrowheadPlantDescriptionEngineData::'KeyValues'[0..1];
    }
    port def PlantDescriptionEngine_generalManagement_HTTP_IDD_Implementation :> PlantDescriptionEngine_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'PlantDescriptionEngine_generalManagement_http_get-log_0' {
        doc /* Operation get-log. Wire method: POST; address: /plantdescriptionengine/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/plantdescriptionengine/general/mgmt/logs"; requestType = "LogRequest"; responseType = "LogEntryListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadPlantDescriptionEngineData::'LogRequest'[0..1];
        out item 'response' : ArrowheadPlantDescriptionEngineData::'LogEntryListResponse'[0..1];
    }
    action def 'PlantDescriptionEngine_generalManagement_http_get-config_1' {
        doc /* Operation get-config. Wire method: GET; address: /plantdescriptionengine/general/mgmt/get-config?keys=\fparam{{keys}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/plantdescriptionengine/general/mgmt/get-config?keys=\\fparam{{keys}}"; requestType = "unspecified"; responseType = "KeyValues"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadPlantDescriptionEngineData::'KeyValues'[0..1];
    }
    port def PlantDescriptionEngine_plantDescriptionManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "plantDescriptionManagement"; }
        doc /* Service version 1.0.0. Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-SysD.tex */
    }
    port def PlantDescriptionEngine_plantDescriptionManagement_HTTP_IDD :> PlantDescriptionEngine_plantDescriptionManagement_SD {
        @IDD { MicroserviceName = "plantDescriptionManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-plantDescriptionManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/plantdescriptionengine/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address plantdescriptionengine, port 8432 and base path /plantdescriptionengine/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
query & GET & /plant-descriptions \\ \hline
get & GET & \texttt{/plant-descriptions/{id}} \\ \hline
create & POST & /plant-descriptions \\ \hline
replace & PUT & \texttt{/plant-descriptions/{id}} \\ \hline
update & PATCH & \texttt{/plant-descriptions/{id}} \\ \hline
remove & DELETE & \texttt{/plant-descriptions/{id}} \\ \hline
\end{longtable}

\newpage */
        doc /* query: GET /plantdescriptionengine/mgmt/plant-descriptions?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sort_field={sort_field}\&active=\fparam{{active}}. Input: None; output: \mref[PlantDescriptionListResponse]{PlantDescriptionListResponse}.  */
        out item 'queryResponse' : JSONMessage[0..1];
        doc /* get: GET /plantdescriptionengine/mgmt/plant-descriptions/\fparam{{id}}. Input: String; output: PlantDescriptionResponse.  */
        in attribute 'getRequest' : ScalarValues::String[0..1];
        out item 'getResponse' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionResponse'[0..1];
        doc /* create: POST /plantdescriptionengine/mgmt/plant-descriptions. Input: PlantDescriptionRequest; output: PlantDescriptionResponse.  */
        in item 'createRequest' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionRequest'[0..1];
        out item 'createResponse' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionResponse'[0..1];
        doc /* replace: PUT /plantdescriptionengine/mgmt/plant-descriptions/\fparam{{id}}. Input: PlantDescriptionRequest; output: PlantDescriptionResponse.  */
        in item 'replaceRequest' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionRequest'[0..1];
        out item 'replaceResponse' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionResponse'[0..1];
        doc /* update: PATCH /plantdescriptionengine/mgmt/plant-descriptions/\fparam{{id}}. Input: PlantDescriptionUpdate; output: PlantDescriptionResponse.  */
        in item 'updateRequest' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionUpdate'[0..1];
        out item 'updateResponse' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionResponse'[0..1];
        doc /* remove: DELETE /plantdescriptionengine/mgmt/plant-descriptions/\fparam{{id}}. Input: String; output: None.  */
        in attribute 'removeRequest' : ScalarValues::String[0..1];
    }
    port def PlantDescriptionEngine_plantDescriptionManagement_HTTP_IDD_Implementation :> PlantDescriptionEngine_plantDescriptionManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'PlantDescriptionEngine_plantDescriptionManagement_http_query_0' {
        doc /* Operation query. Wire method: GET; address: /plantdescriptionengine/mgmt/plant-descriptions?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sort_field={sort_field}\&active=\fparam{{active}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/plantdescriptionengine/mgmt/plant-descriptions?page=\\fparam{{page}}\\&size=\\fparam{{size}}\\&direction=\\fparam{{direction}}\\&sort_field={sort_field}\\&active=\\fparam{{active}}"; requestType = "unspecified"; responseType = "\\mref[PlantDescriptionListResponse]{PlantDescriptionListResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : JSONMessage[0..1];
    }
    action def 'PlantDescriptionEngine_plantDescriptionManagement_http_get_1' {
        doc /* Operation get. Wire method: GET; address: /plantdescriptionengine/mgmt/plant-descriptions/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/plantdescriptionengine/mgmt/plant-descriptions/\\fparam{{id}}"; requestType = "String"; responseType = "PlantDescriptionResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionResponse'[0..1];
    }
    action def 'PlantDescriptionEngine_plantDescriptionManagement_http_create_2' {
        doc /* Operation create. Wire method: POST; address: /plantdescriptionengine/mgmt/plant-descriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/plantdescriptionengine/mgmt/plant-descriptions"; requestType = "PlantDescriptionRequest"; responseType = "PlantDescriptionResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionRequest'[0..1];
        out item 'response' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionResponse'[0..1];
    }
    action def 'PlantDescriptionEngine_plantDescriptionManagement_http_replace_3' {
        doc /* Operation replace. Wire method: PUT; address: /plantdescriptionengine/mgmt/plant-descriptions/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/plantdescriptionengine/mgmt/plant-descriptions/\\fparam{{id}}"; requestType = "PlantDescriptionRequest"; responseType = "PlantDescriptionResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionRequest'[0..1];
        out item 'response' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionResponse'[0..1];
    }
    action def 'PlantDescriptionEngine_plantDescriptionManagement_http_update_4' {
        doc /* Operation update. Wire method: PATCH; address: /plantdescriptionengine/mgmt/plant-descriptions/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PATCH"; address = "/plantdescriptionengine/mgmt/plant-descriptions/\\fparam{{id}}"; requestType = "PlantDescriptionUpdate"; responseType = "PlantDescriptionResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionUpdate'[0..1];
        out item 'response' : ArrowheadPlantDescriptionEngineData::'PlantDescriptionResponse'[0..1];
    }
    action def 'PlantDescriptionEngine_plantDescriptionManagement_http_remove_5' {
        doc /* Operation remove. Wire method: DELETE; address: /plantdescriptionengine/mgmt/plant-descriptions/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/plantdescriptionengine/mgmt/plant-descriptions/\\fparam{{id}}"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    port def PlantDescriptionEngine_plantDescriptionMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "plantDescriptionMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-SysD.tex */
    }
    port def PlantDescriptionEngine_plantDescriptionMonitor_HTTP_IDD :> PlantDescriptionEngine_plantDescriptionMonitor_SD {
        @IDD { MicroserviceName = "plantDescriptionMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-plantDescriptionMonitor-HTTP-IDD.tex"; }
        attribute basePath : String = "/plantdescriptionengine/monitor";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address plantdescriptionengine, port 8432 and base path /plantdescriptionengine/monitor. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
query-plant-descriptions & GET & /plant-descriptions \\ \hline
get-plant-description & GET & \texttt{/plant-descriptions/{id}} \\ \hline
query-alarms & GET & /alarms \\ \hline
get-alarm & GET & \texttt{/alarms/{id}} \\ \hline
acknowledge-alarm & PATCH & \texttt{/alarms/{id}} \\ \hline
\end{longtable}

\newpage */
        doc /* query-plant-descriptions: GET /plantdescriptionengine/monitor/plant-descriptions?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sort_field={sort_field}\&active=\fparam{{active}}. Input: None; output: \mref[MonitorPlantDescriptionListResponse]{MonitorPlantDescriptionListResponse}.  */
        out item 'query-plant-descriptionsResponse' : JSONMessage[0..1];
        doc /* get-plant-description: GET /plantdescriptionengine/monitor/plant-descriptions/\fparam{{id}}. Input: String; output: \mref[MonitorPlantDescriptionResponse]{MonitorPlantDescriptionResponse}.  */
        in attribute 'get-plant-descriptionRequest' : ScalarValues::String[0..1];
        out item 'get-plant-descriptionResponse' : JSONMessage[0..1];
        doc /* query-alarms: GET /plantdescriptionengine/monitor/alarms?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sort_field={sort_field}\&system_name={system_name}\&severity=\fparam{{severity}}\&acknowledged=\fparam{{acknowledged}}. Input: None; output: PdeAlarmListResponse.  */
        out item 'query-alarmsResponse' : ArrowheadPlantDescriptionEngineData::'PdeAlarmListResponse'[0..1];
        doc /* get-alarm: GET /plantdescriptionengine/monitor/alarms/\fparam{{id}}. Input: String; output: PdeAlarmResponse.  */
        in attribute 'get-alarmRequest' : ScalarValues::String[0..1];
        out item 'get-alarmResponse' : ArrowheadPlantDescriptionEngineData::'PdeAlarmResponse'[0..1];
        doc /* acknowledge-alarm: PATCH /plantdescriptionengine/monitor/alarms/\fparam{{id}}. Input: PdeAlarmUpdate; output: PdeAlarmResponse.  */
        in item 'acknowledge-alarmRequest' : ArrowheadPlantDescriptionEngineData::'PdeAlarmUpdate'[0..1];
        out item 'acknowledge-alarmResponse' : ArrowheadPlantDescriptionEngineData::'PdeAlarmResponse'[0..1];
    }
    port def PlantDescriptionEngine_plantDescriptionMonitor_HTTP_IDD_Implementation :> PlantDescriptionEngine_plantDescriptionMonitor_HTTP_IDD { @IDD_Implementation; }
    action def 'PlantDescriptionEngine_plantDescriptionMonitor_http_query-plant-descriptions_0' {
        doc /* Operation query-plant-descriptions. Wire method: GET; address: /plantdescriptionengine/monitor/plant-descriptions?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sort_field={sort_field}\&active=\fparam{{active}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/plantdescriptionengine/monitor/plant-descriptions?page=\\fparam{{page}}\\&size=\\fparam{{size}}\\&direction=\\fparam{{direction}}\\&sort_field={sort_field}\\&active=\\fparam{{active}}"; requestType = "unspecified"; responseType = "\\mref[MonitorPlantDescriptionListResponse]{MonitorPlantDescriptionListResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : JSONMessage[0..1];
    }
    action def 'PlantDescriptionEngine_plantDescriptionMonitor_http_get-plant-description_1' {
        doc /* Operation get-plant-description. Wire method: GET; address: /plantdescriptionengine/monitor/plant-descriptions/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/plantdescriptionengine/monitor/plant-descriptions/\\fparam{{id}}"; requestType = "String"; responseType = "\\mref[MonitorPlantDescriptionResponse]{MonitorPlantDescriptionResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'PlantDescriptionEngine_plantDescriptionMonitor_http_query-alarms_2' {
        doc /* Operation query-alarms. Wire method: GET; address: /plantdescriptionengine/monitor/alarms?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sort_field={sort_field}\&system_name={system_name}\&severity=\fparam{{severity}}\&acknowledged=\fparam{{acknowledged}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/plantdescriptionengine/monitor/alarms?page=\\fparam{{page}}\\&size=\\fparam{{size}}\\&direction=\\fparam{{direction}}\\&sort_field={sort_field}\\&system_name={system_name}\\&severity=\\fparam{{severity}}\\&acknowledged=\\fparam{{acknowledged}}"; requestType = "unspecified"; responseType = "PdeAlarmListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadPlantDescriptionEngineData::'PdeAlarmListResponse'[0..1];
    }
    action def 'PlantDescriptionEngine_plantDescriptionMonitor_http_get-alarm_3' {
        doc /* Operation get-alarm. Wire method: GET; address: /plantdescriptionengine/monitor/alarms/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/plantdescriptionengine/monitor/alarms/\\fparam{{id}}"; requestType = "String"; responseType = "PdeAlarmResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadPlantDescriptionEngineData::'PdeAlarmResponse'[0..1];
    }
    action def 'PlantDescriptionEngine_plantDescriptionMonitor_http_acknowledge-alarm_4' {
        doc /* Operation acknowledge-alarm. Wire method: PATCH; address: /plantdescriptionengine/monitor/alarms/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PATCH"; address = "/plantdescriptionengine/monitor/alarms/\\fparam{{id}}"; requestType = "PdeAlarmUpdate"; responseType = "PdeAlarmResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadPlantDescriptionEngineData::'PdeAlarmUpdate'[0..1];
        out item 'response' : ArrowheadPlantDescriptionEngineData::'PdeAlarmResponse'[0..1];
    }
    port def PlantDescriptionEngine_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-SysD.tex */
    }
    port def PlantDescriptionEngine_sysMonitor_AMQP_IDD :> PlantDescriptionEngine_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, PlantDescriptionEngine registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadPlantDescriptionEngineData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadPlantDescriptionEngineData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadPlantDescriptionEngineData::'Observation'[0..1];
    }
    port def PlantDescriptionEngine_sysMonitor_AMQP_IDD_Implementation :> PlantDescriptionEngine_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'PlantDescriptionEngine_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadPlantDescriptionEngineData::'Observation'[0..1];
    }
    action def 'PlantDescriptionEngine_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadPlantDescriptionEngineData::'Observation'[0..1];
    }
    action def 'PlantDescriptionEngine_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.PlantDescriptionEngine.PlantDescriptionEngine.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadPlantDescriptionEngineData::'Observation'[0..1];
    }
    part def PlantDescriptionEngine_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "PlantDescriptionEngine"; FunctionalProperties = "This document describes the PlantDescriptionEngine support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/PlantDescriptionEngine and deployed in the Arrowhead-test-cloud-java local cloud.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-PlantDescriptionEngine-java-spring/documentation/v5.2.2/PlantDescriptionEngine-v5.2.2-SysD.tex"; }
        doc /* This document describes the PlantDescriptionEngine support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/PlantDescriptionEngine and deployed in the Arrowhead-test-cloud-java local cloud.
  \end{abstract} */
        port 'generalManagement_SD' : PlantDescriptionEngine_generalManagement_SD;
        port 'plantDescriptionManagement_SD' : PlantDescriptionEngine_plantDescriptionManagement_SD;
        port 'plantDescriptionMonitor_SD' : PlantDescriptionEngine_plantDescriptionMonitor_SD;
        port 'sysMonitor_SD' : PlantDescriptionEngine_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, ConsumerAuthorization, DynamicServiceOrchestration, ServiceRegistry, SysMonitor. These are external requirements, not assumed co-deployments. */
    }
    part def PlantDescriptionEngine_SysDD :> PlantDescriptionEngine_SysD {
        @SysDD;
        port 'generalManagement_http' : PlantDescriptionEngine_generalManagement_HTTP_IDD;
        port 'plantDescriptionManagement_http' : PlantDescriptionEngine_plantDescriptionManagement_HTTP_IDD;
        port 'plantDescriptionMonitor_http' : PlantDescriptionEngine_plantDescriptionMonitor_HTTP_IDD;
        port 'sysMonitor_amqp' : PlantDescriptionEngine_sysMonitor_AMQP_IDD;
    }
    part def PlantDescriptionEngine :> PlantDescriptionEngine_SysDD {
        @System_Implementation;
    }
}
