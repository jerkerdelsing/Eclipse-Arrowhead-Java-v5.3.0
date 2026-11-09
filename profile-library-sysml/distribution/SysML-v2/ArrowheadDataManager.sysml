library package ArrowheadDataManager {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def DataManager_dataHistorian_SD :> ArrowheadService {
        @SD { MicroserviceName = "dataHistorian"; }
        doc /* Service version 1.0.0. Source: ah5-DataManager-java-spring/documentation/v5.2.2/DataManager-v5.2.2-SysD.tex */
    }
    port def DataManager_dataHistorian_HTTP_IDD :> DataManager_dataHistorian_SD {
        @IDD { MicroserviceName = "dataHistorian"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-DataManager-java-spring/documentation/v5.2.2/DataManager-v5.2.2-dataHistorian-HTTP-IDD.tex"; }
        attribute basePath : String = "/datamanager/data-historian";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address datamanager, port 8461 and base path /datamanager/data-historian. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
list-systems & GET & /systems \\ \hline
list-services & GET & \texttt{/systems/{systemName}/services} \\ \hline
get-data & GET & \texttt{/systems/{systemName}/services/{serviceName}/data} \\ \hline
put-data & PUT & \texttt{/systems/{systemName}/services/{serviceName}/data} \\ \hline
\end{longtable}

\newpage */
        doc /* list-systems: GET /datamanager/data-historian/systems. Input: None; output: SystemListResponse.  */
        out item 'list-systemsResponse' : ArrowheadDataManagerData::'SystemListResponse'[0..1];
        doc /* list-services: GET /datamanager/data-historian/systems/\fparam{{systemName}}/services. Input: String; output: ServiceListResponse.  */
        in attribute 'list-servicesRequest' : ScalarValues::String[0..1];
        out item 'list-servicesResponse' : ArrowheadDataManagerData::'ServiceListResponse'[0..1];
        doc /* get-data: GET /datamanager/data-historian/systems/\fparam{{systemName}}/services/\fparam{{serviceName}}/data?from=\fparam{{from}}\&to=\fparam{{to}}\&count=\fparam{{count}}\&signals=\fparam{{signals}}\&signalCount=\fparam{{signalCount}}. Input: String; output: Array<SenMLRecord>.  */
        in attribute 'get-dataRequest' : ScalarValues::String[0..1];
        out item 'get-dataResponse' : ArrowheadDataManagerData::'SenMLRecord'[0..*];
        doc /* put-data: PUT /datamanager/data-historian/systems/\fparam{{systemName}}/services/\fparam{{serviceName}}/data. Input: Array<SenMLRecord>; output: None.  */
        in item 'put-dataRequest' : ArrowheadDataManagerData::'SenMLRecord'[0..*];
    }
    port def DataManager_dataHistorian_HTTP_IDD_Implementation :> DataManager_dataHistorian_HTTP_IDD { @IDD_Implementation; }
    action def 'DataManager_dataHistorian_http_list-systems_0' {
        doc /* Operation list-systems. Wire method: GET; address: /datamanager/data-historian/systems. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/datamanager/data-historian/systems"; requestType = "unspecified"; responseType = "SystemListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadDataManagerData::'SystemListResponse'[0..1];
    }
    action def 'DataManager_dataHistorian_http_list-services_1' {
        doc /* Operation list-services. Wire method: GET; address: /datamanager/data-historian/systems/\fparam{{systemName}}/services. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/datamanager/data-historian/systems/\\fparam{{systemName}}/services"; requestType = "String"; responseType = "ServiceListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadDataManagerData::'ServiceListResponse'[0..1];
    }
    action def 'DataManager_dataHistorian_http_get-data_2' {
        doc /* Operation get-data. Wire method: GET; address: /datamanager/data-historian/systems/\fparam{{systemName}}/services/\fparam{{serviceName}}/data?from=\fparam{{from}}\&to=\fparam{{to}}\&count=\fparam{{count}}\&signals=\fparam{{signals}}\&signalCount=\fparam{{signalCount}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/datamanager/data-historian/systems/\\fparam{{systemName}}/services/\\fparam{{serviceName}}/data?from=\\fparam{{from}}\\&to=\\fparam{{to}}\\&count=\\fparam{{count}}\\&signals=\\fparam{{signals}}\\&signalCount=\\fparam{{signalCount}}"; requestType = "String"; responseType = "Array<SenMLRecord>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadDataManagerData::'SenMLRecord'[0..*];
    }
    action def 'DataManager_dataHistorian_http_put-data_3' {
        doc /* Operation put-data. Wire method: PUT; address: /datamanager/data-historian/systems/\fparam{{systemName}}/services/\fparam{{serviceName}}/data. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/datamanager/data-historian/systems/\\fparam{{systemName}}/services/\\fparam{{serviceName}}/data"; requestType = "Array<SenMLRecord>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadDataManagerData::'SenMLRecord'[0..*];
    }
    port def DataManager_dataProxy_SD :> ArrowheadService {
        @SD { MicroserviceName = "dataProxy"; }
        doc /* Service version 1.0.0. Source: ah5-DataManager-java-spring/documentation/v5.2.2/DataManager-v5.2.2-SysD.tex */
    }
    port def DataManager_dataProxy_HTTP_IDD :> DataManager_dataProxy_SD {
        @IDD { MicroserviceName = "dataProxy"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-DataManager-java-spring/documentation/v5.2.2/DataManager-v5.2.2-dataProxy-HTTP-IDD.tex"; }
        attribute basePath : String = "/datamanager/data-proxy";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address datamanager, port 8461 and base path /datamanager/data-proxy. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
list-systems & GET & /systems \\ \hline
list-services & GET & \texttt{/systems/{systemName}/services} \\ \hline
get-data & GET & \texttt{/systems/{systemName}/services/{serviceName}/data} \\ \hline
put-data & PUT & \texttt{/systems/{systemName}/services/{serviceName}/data} \\ \hline
\end{longtable}

\newpage */
        doc /* list-systems: GET /datamanager/data-proxy/systems. Input: None; output: SystemListResponse.  */
        out item 'list-systemsResponse' : ArrowheadDataManagerData::'SystemListResponse'[0..1];
        doc /* list-services: GET /datamanager/data-proxy/systems/\fparam{{systemName}}/services. Input: String; output: ServiceListResponse.  */
        in attribute 'list-servicesRequest' : ScalarValues::String[0..1];
        out item 'list-servicesResponse' : ArrowheadDataManagerData::'ServiceListResponse'[0..1];
        doc /* get-data: GET /datamanager/data-proxy/systems/\fparam{{systemName}}/services/\fparam{{serviceName}}/data. Input: String; output: Array<SenMLRecord>.  */
        in attribute 'get-dataRequest' : ScalarValues::String[0..1];
        out item 'get-dataResponse' : ArrowheadDataManagerData::'SenMLRecord'[0..*];
        doc /* put-data: PUT /datamanager/data-proxy/systems/\fparam{{systemName}}/services/\fparam{{serviceName}}/data. Input: Array<SenMLRecord>; output: None.  */
        in item 'put-dataRequest' : ArrowheadDataManagerData::'SenMLRecord'[0..*];
    }
    port def DataManager_dataProxy_HTTP_IDD_Implementation :> DataManager_dataProxy_HTTP_IDD { @IDD_Implementation; }
    action def 'DataManager_dataProxy_http_list-systems_0' {
        doc /* Operation list-systems. Wire method: GET; address: /datamanager/data-proxy/systems. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/datamanager/data-proxy/systems"; requestType = "unspecified"; responseType = "SystemListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadDataManagerData::'SystemListResponse'[0..1];
    }
    action def 'DataManager_dataProxy_http_list-services_1' {
        doc /* Operation list-services. Wire method: GET; address: /datamanager/data-proxy/systems/\fparam{{systemName}}/services. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/datamanager/data-proxy/systems/\\fparam{{systemName}}/services"; requestType = "String"; responseType = "ServiceListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadDataManagerData::'ServiceListResponse'[0..1];
    }
    action def 'DataManager_dataProxy_http_get-data_2' {
        doc /* Operation get-data. Wire method: GET; address: /datamanager/data-proxy/systems/\fparam{{systemName}}/services/\fparam{{serviceName}}/data. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/datamanager/data-proxy/systems/\\fparam{{systemName}}/services/\\fparam{{serviceName}}/data"; requestType = "String"; responseType = "Array<SenMLRecord>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadDataManagerData::'SenMLRecord'[0..*];
    }
    action def 'DataManager_dataProxy_http_put-data_3' {
        doc /* Operation put-data. Wire method: PUT; address: /datamanager/data-proxy/systems/\fparam{{systemName}}/services/\fparam{{serviceName}}/data. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/datamanager/data-proxy/systems/\\fparam{{systemName}}/services/\\fparam{{serviceName}}/data"; requestType = "Array<SenMLRecord>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadDataManagerData::'SenMLRecord'[0..*];
    }
    port def DataManager_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-DataManager-java-spring/documentation/v5.2.2/DataManager-v5.2.2-SysD.tex */
    }
    port def DataManager_sysMonitor_AMQP_IDD :> DataManager_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-DataManager-java-spring/documentation/v5.2.2/DataManager-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.DataManager.DataManager";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, DataManager registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.DataManager.DataManager",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.DataManager.DataManager.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadDataManagerData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.DataManager.DataManager.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadDataManagerData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.DataManager.DataManager.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadDataManagerData::'Observation'[0..1];
    }
    port def DataManager_sysMonitor_AMQP_IDD_Implementation :> DataManager_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'DataManager_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.DataManager.DataManager.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.DataManager.DataManager.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadDataManagerData::'Observation'[0..1];
    }
    action def 'DataManager_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.DataManager.DataManager.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.DataManager.DataManager.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadDataManagerData::'Observation'[0..1];
    }
    action def 'DataManager_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.DataManager.DataManager.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.DataManager.DataManager.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadDataManagerData::'Observation'[0..1];
    }
    part def DataManager_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "DataManager"; FunctionalProperties = "This document describes the DataManager support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/DataManager and deployed in the Arrowhead-test-cloud-java local cloud.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-DataManager-java-spring/documentation/v5.2.2/DataManager-v5.2.2-SysD.tex"; }
        doc /* This document describes the DataManager support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/DataManager and deployed in the Arrowhead-test-cloud-java local cloud.
  \end{abstract} */
        port 'dataHistorian_SD' : DataManager_dataHistorian_SD;
        port 'dataProxy_SD' : DataManager_dataProxy_SD;
        port 'sysMonitor_SD' : DataManager_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, ConsumerAuthorization, ServiceRegistry. These are external requirements, not assumed co-deployments. */
    }
    part def DataManager_SysDD :> DataManager_SysD {
        @SysDD;
        port 'dataHistorian_http' : DataManager_dataHistorian_HTTP_IDD;
        port 'dataProxy_http' : DataManager_dataProxy_HTTP_IDD;
        port 'sysMonitor_amqp' : DataManager_sysMonitor_AMQP_IDD;
    }
    part def DataManager :> DataManager_SysDD {
        @System_Implementation;
    }
}
