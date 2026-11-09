library package ArrowheadSysMonitor {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def SysMonitor_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-SysD.tex */
    }
    port def SysMonitor_sysMonitor_AMQP_IDD :> SysMonitor_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.SysMonitor.SysMonitor";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, SysMonitor registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.SysMonitor.SysMonitor",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.SysMonitor.SysMonitor.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadSysMonitorData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.SysMonitor.SysMonitor.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadSysMonitorData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.SysMonitor.SysMonitor.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadSysMonitorData::'Observation'[0..1];
    }
    port def SysMonitor_sysMonitor_AMQP_IDD_Implementation :> SysMonitor_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'SysMonitor_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.SysMonitor.SysMonitor.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.SysMonitor.SysMonitor.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadSysMonitorData::'Observation'[0..1];
    }
    action def 'SysMonitor_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.SysMonitor.SysMonitor.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.SysMonitor.SysMonitor.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadSysMonitorData::'Observation'[0..1];
    }
    action def 'SysMonitor_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.SysMonitor.SysMonitor.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.SysMonitor.SysMonitor.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadSysMonitorData::'Observation'[0..1];
    }
    port def SysMonitor_sysMonitorData_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitorData"; }
        doc /* Service version 1.0.0. Source: ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-SysD.tex */
    }
    port def SysMonitor_sysMonitorData_MQTT_IDD :> SysMonitor_sysMonitorData_SD {
        @IDD { MicroserviceName = "sysMonitorData"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-sysMonitorData-MQTT-IDD.tex"; }
        attribute templateName : String = "generic_mqtts";
        attribute protocol : String = "ssl";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 8883;
        attribute 'baseTopic' : String = "arrowhead/TestCloud/sysmonitor/data";
        doc /* Registration (source documentation): \begin{lstlisting}[caption={Registered service instance (TestCloud).}]
{
  "serviceDefinitionName": "sysMonitorData",
  "version": "1.0.0",
  "metadata": {
    "contract": "SysMonitor-Data-MQTT-IDD-5.2.2",
    "schemaVersion": "1.0"
  },
  "interfaces": [
    {
      "templateName": "generic_mqtts",
      "protocol": "ssl",
      "policy": "CERT_AUTH",
      "properties": {
        "accessAddresses": [
          "mqtt-broker"
        ],
        "accessPort": 8883,
        "baseTopic": "arrowhead/TestCloud/sysmonitor/data",
        "operations": [
          "subscribe"
        ]
      }
    }
  ]
}
\end{lstlisting} */
        doc /* subscribe: SUBSCRIBE <baseTopic>/<observationType>/<providerSystem>. Input: None; output: StoredObservation.  */
        out item 'subscribeResponse' : ArrowheadSysMonitorData::'StoredObservation'[0..1];
    }
    port def SysMonitor_sysMonitorData_MQTT_IDD_Implementation :> SysMonitor_sysMonitorData_MQTT_IDD { @IDD_Implementation; }
    action def 'SysMonitor_sysMonitorData_mqtt_subscribe_0' {
        doc /* Operation subscribe. Wire method: SUBSCRIBE; address: <baseTopic>/<observationType>/<providerSystem>. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "SUBSCRIBE"; address = "<baseTopic>/<observationType>/<providerSystem>"; requestType = "unspecified"; responseType = "StoredObservation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadSysMonitorData::'StoredObservation'[0..1];
    }
    port def SysMonitor_sysMonitorHistory_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitorHistory"; }
        doc /* Service version 1.0.0. Source: ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-SysD.tex */
    }
    port def SysMonitor_sysMonitorHistory_HTTP_IDD :> SysMonitor_sysMonitorHistory_SD {
        @IDD { MicroserviceName = "sysMonitorHistory"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-sysMonitorHistory-HTTP-IDD.tex"; }
        attribute basePath : String = "/sysmonitor/history";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address sysmonitor, port 8475 and base path /sysmonitor/history. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
list-systems & GET & /systems \\ \hline
query & GET & \texttt{/systems/{systemName}/observations} \\ \hline
latest & GET & \texttt{/systems/{systemName}/latest} \\ \hline
\end{longtable}

\newpage */
        doc /* list-systems: GET /sysmonitor/history/systems. Input: None; output: SystemList.  */
        out item 'list-systemsResponse' : ArrowheadSysMonitorData::'SystemList'[0..1];
        doc /* query: GET /sysmonitor/history/systems/\fparam{{systemName}}/observations?type=\fparam{{type}}\&from=\fparam{{from}}\&to=\fparam{{to}}\&count=\fparam{{count}}. Input: String; output: ObservationList.  */
        in attribute 'queryRequest' : ScalarValues::String[0..1];
        out item 'queryResponse' : ArrowheadSysMonitorData::'ObservationList'[0..1];
        doc /* latest: GET /sysmonitor/history/systems/\fparam{{systemName}}/latest. Input: String; output: Latest.  */
        in attribute 'latestRequest' : ScalarValues::String[0..1];
        out item 'latestResponse' : ArrowheadSysMonitorData::'Latest'[0..1];
    }
    port def SysMonitor_sysMonitorHistory_HTTP_IDD_Implementation :> SysMonitor_sysMonitorHistory_HTTP_IDD { @IDD_Implementation; }
    action def 'SysMonitor_sysMonitorHistory_http_list-systems_0' {
        doc /* Operation list-systems. Wire method: GET; address: /sysmonitor/history/systems. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/sysmonitor/history/systems"; requestType = "unspecified"; responseType = "SystemList"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadSysMonitorData::'SystemList'[0..1];
    }
    action def 'SysMonitor_sysMonitorHistory_http_query_1' {
        doc /* Operation query. Wire method: GET; address: /sysmonitor/history/systems/\fparam{{systemName}}/observations?type=\fparam{{type}}\&from=\fparam{{from}}\&to=\fparam{{to}}\&count=\fparam{{count}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/sysmonitor/history/systems/\\fparam{{systemName}}/observations?type=\\fparam{{type}}\\&from=\\fparam{{from}}\\&to=\\fparam{{to}}\\&count=\\fparam{{count}}"; requestType = "String"; responseType = "ObservationList"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadSysMonitorData::'ObservationList'[0..1];
    }
    action def 'SysMonitor_sysMonitorHistory_http_latest_2' {
        doc /* Operation latest. Wire method: GET; address: /sysmonitor/history/systems/\fparam{{systemName}}/latest. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/sysmonitor/history/systems/\\fparam{{systemName}}/latest"; requestType = "String"; responseType = "Latest"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadSysMonitorData::'Latest'[0..1];
    }
    port def SysMonitor_sysMonitorManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitorManagement"; }
        doc /* Service version 1.0.0. Source: ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-SysD.tex */
    }
    port def SysMonitor_sysMonitorManagement_HTTP_IDD :> SysMonitor_sysMonitorManagement_SD {
        @IDD { MicroserviceName = "sysMonitorManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-sysMonitorManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/sysmonitor/management";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address sysmonitor, port 8475 and base path /sysmonitor/management. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
status & GET & /status \\ \hline
providers & GET & /providers \\ \hline
subscriptions & GET & /subscriptions \\ \hline
subscribe & PUT & \texttt{/subscriptions/{providerSystem}} \\ \hline
unsubscribe & DELETE & \texttt{/subscriptions/{providerSystem}} \\ \hline
reconcile & POST & /reconcile \\ \hline
\end{longtable}

\newpage */
        doc /* status: GET /sysmonitor/management/status. Input: None; output: Status.  */
        out item 'statusResponse' : ArrowheadSysMonitorData::'Status'[0..1];
        doc /* providers: GET /sysmonitor/management/providers. Input: None; output: ProviderList.  */
        out item 'providersResponse' : ArrowheadSysMonitorData::'ProviderList'[0..1];
        doc /* subscriptions: GET /sysmonitor/management/subscriptions. Input: None; output: SubscriptionList.  */
        out item 'subscriptionsResponse' : ArrowheadSysMonitorData::'SubscriptionList'[0..1];
        doc /* subscribe: PUT /sysmonitor/management/subscriptions/\fparam{{providerSystem}}. Input: SubscriptionRequest; output: Subscription.  */
        in item 'subscribeRequest' : ArrowheadSysMonitorData::'SubscriptionRequest'[0..1];
        out item 'subscribeResponse' : ArrowheadSysMonitorData::'Subscription'[0..1];
        doc /* unsubscribe: DELETE /sysmonitor/management/subscriptions/\fparam{{providerSystem}}. Input: String; output: None.  */
        in attribute 'unsubscribeRequest' : ScalarValues::String[0..1];
        doc /* reconcile: POST /sysmonitor/management/reconcile. Input: None; output: ReconcileResult.  */
        out item 'reconcileResponse' : ArrowheadSysMonitorData::'ReconcileResult'[0..1];
    }
    port def SysMonitor_sysMonitorManagement_HTTP_IDD_Implementation :> SysMonitor_sysMonitorManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'SysMonitor_sysMonitorManagement_http_status_0' {
        doc /* Operation status. Wire method: GET; address: /sysmonitor/management/status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/sysmonitor/management/status"; requestType = "unspecified"; responseType = "Status"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadSysMonitorData::'Status'[0..1];
    }
    action def 'SysMonitor_sysMonitorManagement_http_providers_1' {
        doc /* Operation providers. Wire method: GET; address: /sysmonitor/management/providers. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/sysmonitor/management/providers"; requestType = "unspecified"; responseType = "ProviderList"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadSysMonitorData::'ProviderList'[0..1];
    }
    action def 'SysMonitor_sysMonitorManagement_http_subscriptions_2' {
        doc /* Operation subscriptions. Wire method: GET; address: /sysmonitor/management/subscriptions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/sysmonitor/management/subscriptions"; requestType = "unspecified"; responseType = "SubscriptionList"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadSysMonitorData::'SubscriptionList'[0..1];
    }
    action def 'SysMonitor_sysMonitorManagement_http_subscribe_3' {
        doc /* Operation subscribe. Wire method: PUT; address: /sysmonitor/management/subscriptions/\fparam{{providerSystem}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/sysmonitor/management/subscriptions/\\fparam{{providerSystem}}"; requestType = "SubscriptionRequest"; responseType = "Subscription"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadSysMonitorData::'SubscriptionRequest'[0..1];
        out item 'response' : ArrowheadSysMonitorData::'Subscription'[0..1];
    }
    action def 'SysMonitor_sysMonitorManagement_http_unsubscribe_4' {
        doc /* Operation unsubscribe. Wire method: DELETE; address: /sysmonitor/management/subscriptions/\fparam{{providerSystem}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/sysmonitor/management/subscriptions/\\fparam{{providerSystem}}"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    action def 'SysMonitor_sysMonitorManagement_http_reconcile_5' {
        doc /* Operation reconcile. Wire method: POST; address: /sysmonitor/management/reconcile. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/sysmonitor/management/reconcile"; requestType = "unspecified"; responseType = "ReconcileResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadSysMonitorData::'ReconcileResult'[0..1];
    }
    part def SysMonitor_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "SysMonitor"; FunctionalProperties = "This document describes the SysMonitor support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-SysMonitor-java-spring with an independent local-cloud deployment definition in its own folder.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-SysMonitor-java-spring/documentation/v5.2.2/SysMonitor-v5.2.2-SysD.tex"; }
        doc /* This document describes the SysMonitor support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-SysMonitor-java-spring with an independent local-cloud deployment definition in its own folder.
  \end{abstract} */
        port 'sysMonitor_SD' : SysMonitor_sysMonitor_SD;
        port 'sysMonitorData_SD' : SysMonitor_sysMonitorData_SD;
        port 'sysMonitorHistory_SD' : SysMonitor_sysMonitorHistory_SD;
        port 'sysMonitorManagement_SD' : SysMonitor_sysMonitorManagement_SD;
        doc /* Consumed dependencies from SysD: AMQP broker, Authentication, ConsumerAuthorization, DynamicServiceOrchestration, MQTT broker, ServiceRegistry. These are external requirements, not assumed co-deployments. */
    }
    part def SysMonitor_SysDD :> SysMonitor_SysD {
        @SysDD;
        port 'sysMonitor_amqp' : SysMonitor_sysMonitor_AMQP_IDD;
        port 'sysMonitorData_mqtt' : SysMonitor_sysMonitorData_MQTT_IDD;
        port 'sysMonitorHistory_http' : SysMonitor_sysMonitorHistory_HTTP_IDD;
        port 'sysMonitorManagement_http' : SysMonitor_sysMonitorManagement_HTTP_IDD;
    }
    part def SysMonitor :> SysMonitor_SysDD {
        @System_Implementation;
    }
}
