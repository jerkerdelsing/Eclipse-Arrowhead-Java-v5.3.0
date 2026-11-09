library package ArrowheadTimeManager {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def TimeManager_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-TimeManager-java-spring/documentation/v5.2.2/TimeManager-v5.2.2-SysD.tex */
    }
    port def TimeManager_generalManagement_HTTP_IDD :> TimeManager_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-TimeManager-java-spring/documentation/v5.2.2/TimeManager-v5.2.2-generalManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/timemanager/general/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address timemanager, port 8463 and base path /timemanager/general/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-log & POST & /logs \\ \hline
get-config & GET & /get-config \\ \hline
\end{longtable}

\newpage */
        doc /* get-log: POST /timemanager/general/mgmt/logs. Input: LogRequest; output: LogEntryListResponse.  */
        in item 'get-logRequest' : ArrowheadTimeManagerData::'LogRequest'[0..1];
        out item 'get-logResponse' : ArrowheadTimeManagerData::'LogEntryListResponse'[0..1];
        doc /* get-config: GET /timemanager/general/mgmt/get-config?keys=\fparam{{keys}}. Input: None; output: KeyValues.  */
        out item 'get-configResponse' : ArrowheadTimeManagerData::'KeyValues'[0..1];
    }
    port def TimeManager_generalManagement_HTTP_IDD_Implementation :> TimeManager_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'TimeManager_generalManagement_http_get-log_0' {
        doc /* Operation get-log. Wire method: POST; address: /timemanager/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/timemanager/general/mgmt/logs"; requestType = "LogRequest"; responseType = "LogEntryListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadTimeManagerData::'LogRequest'[0..1];
        out item 'response' : ArrowheadTimeManagerData::'LogEntryListResponse'[0..1];
    }
    action def 'TimeManager_generalManagement_http_get-config_1' {
        doc /* Operation get-config. Wire method: GET; address: /timemanager/general/mgmt/get-config?keys=\fparam{{keys}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/timemanager/general/mgmt/get-config?keys=\\fparam{{keys}}"; requestType = "unspecified"; responseType = "KeyValues"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadTimeManagerData::'KeyValues'[0..1];
    }
    port def TimeManager_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-TimeManager-java-spring/documentation/v5.2.2/TimeManager-v5.2.2-SysD.tex */
    }
    port def TimeManager_sysMonitor_AMQP_IDD :> TimeManager_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-TimeManager-java-spring/documentation/v5.2.2/TimeManager-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.TimeManager.TimeManager";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, TimeManager registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.TimeManager.TimeManager",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.TimeManager.TimeManager.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadTimeManagerData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.TimeManager.TimeManager.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadTimeManagerData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.TimeManager.TimeManager.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadTimeManagerData::'Observation'[0..1];
    }
    port def TimeManager_sysMonitor_AMQP_IDD_Implementation :> TimeManager_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'TimeManager_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.TimeManager.TimeManager.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.TimeManager.TimeManager.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadTimeManagerData::'Observation'[0..1];
    }
    action def 'TimeManager_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.TimeManager.TimeManager.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.TimeManager.TimeManager.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadTimeManagerData::'Observation'[0..1];
    }
    action def 'TimeManager_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.TimeManager.TimeManager.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.TimeManager.TimeManager.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadTimeManagerData::'Observation'[0..1];
    }
    port def TimeManager_time_SD :> ArrowheadService {
        @SD { MicroserviceName = "time"; }
        doc /* Service version 1.0.0. Source: ah5-TimeManager-java-spring/documentation/v5.2.2/TimeManager-v5.2.2-SysD.tex */
    }
    port def TimeManager_time_HTTP_IDD :> TimeManager_time_SD {
        @IDD { MicroserviceName = "time"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-TimeManager-java-spring/documentation/v5.2.2/TimeManager-v5.2.2-time-HTTP-IDD.tex"; }
        attribute basePath : String = "/timemanager";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address timemanager, port 8463 and base path /timemanager. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-time & GET & /time \\ \hline
\end{longtable}

\newpage */
        doc /* get-time: GET /timemanager/time. Input: None; output: TimeResponse.  */
        out item 'get-timeResponse' : ArrowheadTimeManagerData::'TimeResponse'[0..1];
    }
    port def TimeManager_time_HTTP_IDD_Implementation :> TimeManager_time_HTTP_IDD { @IDD_Implementation; }
    action def 'TimeManager_time_http_get-time_0' {
        doc /* Operation get-time. Wire method: GET; address: /timemanager/time. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/timemanager/time"; requestType = "unspecified"; responseType = "TimeResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadTimeManagerData::'TimeResponse'[0..1];
    }
    part def TimeManager_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "TimeManager"; FunctionalProperties = "This document describes the TimeManager support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/TimeManager and deployed in the Arrowhead-test-cloud-java local cloud.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-TimeManager-java-spring/documentation/v5.2.2/TimeManager-v5.2.2-SysD.tex"; }
        doc /* This document describes the TimeManager support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/TimeManager and deployed in the Arrowhead-test-cloud-java local cloud.
  \end{abstract} */
        port 'generalManagement_SD' : TimeManager_generalManagement_SD;
        port 'sysMonitor_SD' : TimeManager_sysMonitor_SD;
        port 'time_SD' : TimeManager_time_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, ConsumerAuthorization, NTP servers, ServiceRegistry. These are external requirements, not assumed co-deployments. */
    }
    part def TimeManager_SysDD :> TimeManager_SysD {
        @SysDD;
        port 'generalManagement_http' : TimeManager_generalManagement_HTTP_IDD;
        port 'sysMonitor_amqp' : TimeManager_sysMonitor_AMQP_IDD;
        port 'time_http' : TimeManager_time_HTTP_IDD;
    }
    part def TimeManager :> TimeManager_SysDD {
        @System_Implementation;
    }
}
