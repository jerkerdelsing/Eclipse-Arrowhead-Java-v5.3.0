library package ArrowheadBlacklist {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def Blacklist_blacklistDiscovery_SD :> ArrowheadService {
        @SD { MicroserviceName = "blacklistDiscovery"; }
        doc /* Service version 1.0.0. Source: ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-SysD.tex */
    }
    port def Blacklist_blacklistDiscovery_HTTP_IDD :> Blacklist_blacklistDiscovery_SD {
        @IDD { MicroserviceName = "blacklistDiscovery"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-blacklistDiscovery-HTTP-IDD.tex"; }
        attribute basePath : String = "/blacklist";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address blacklist, port 8464 and base path /blacklist. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
check & GET & \texttt{/check/{systemName}} \\ \hline
lookup & GET & /lookup \\ \hline
\end{longtable}

\newpage */
        doc /* check: GET /blacklist/check/\fparam{{systemName}}. Input: String; output: Boolean.  */
        in attribute 'checkRequest' : ScalarValues::String[0..1];
        out attribute 'checkResponse' : ScalarValues::Boolean[0..1];
        doc /* lookup: GET /blacklist/lookup. Input: None; output: \mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}.  */
        out item 'lookupResponse' : JSONMessage[0..1];
    }
    port def Blacklist_blacklistDiscovery_HTTP_IDD_Implementation :> Blacklist_blacklistDiscovery_HTTP_IDD { @IDD_Implementation; }
    action def 'Blacklist_blacklistDiscovery_http_check_0' {
        doc /* Operation check. Wire method: GET; address: /blacklist/check/\fparam{{systemName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/blacklist/check/\\fparam{{systemName}}"; requestType = "String"; responseType = "Boolean"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out attribute 'response' : ScalarValues::Boolean[0..1];
    }
    action def 'Blacklist_blacklistDiscovery_http_lookup_1' {
        doc /* Operation lookup. Wire method: GET; address: /blacklist/lookup. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/blacklist/lookup"; requestType = "unspecified"; responseType = "\\mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : JSONMessage[0..1];
    }
    port def Blacklist_blacklistDiscovery_MQTT_IDD :> Blacklist_blacklistDiscovery_SD {
        @IDD { MicroserviceName = "blacklistDiscovery"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-blacklistDiscovery-MQTT-IDD.tex"; }
        doc /* check: PUBLISH arrowhead/blacklist/check. Input: String; output: Boolean.  */
        in attribute 'checkRequest' : ScalarValues::String[0..1];
        out attribute 'checkResponse' : ScalarValues::Boolean[0..1];
        doc /* lookup: PUBLISH arrowhead/blacklist/lookup. Input: None; output: \mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}.  */
        out item 'lookupResponse' : JSONMessage[0..1];
    }
    port def Blacklist_blacklistDiscovery_MQTT_IDD_Implementation :> Blacklist_blacklistDiscovery_MQTT_IDD { @IDD_Implementation; }
    action def 'Blacklist_blacklistDiscovery_mqtt_check_0' {
        doc /* Operation check. Wire method: PUBLISH; address: arrowhead/blacklist/check. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "PUBLISH"; address = "arrowhead/blacklist/check"; requestType = "String"; responseType = "Boolean"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out attribute 'response' : ScalarValues::Boolean[0..1];
    }
    action def 'Blacklist_blacklistDiscovery_mqtt_lookup_1' {
        doc /* Operation lookup. Wire method: PUBLISH; address: arrowhead/blacklist/lookup. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "PUBLISH"; address = "arrowhead/blacklist/lookup"; requestType = "unspecified"; responseType = "\\mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : JSONMessage[0..1];
    }
    port def Blacklist_blacklistManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "blacklistManagement"; }
        doc /* Service version 1.0.0. Source: ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-SysD.tex */
    }
    port def Blacklist_blacklistManagement_HTTP_IDD :> Blacklist_blacklistManagement_SD {
        @IDD { MicroserviceName = "blacklistManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-blacklistManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/blacklist/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address blacklist, port 8464 and base path /blacklist/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
query & POST & /query \\ \hline
create & POST & /create \\ \hline
remove & DELETE & /remove \\ \hline
\end{longtable}

\newpage */
        doc /* query: POST /blacklist/mgmt/query. Input: BlacklistQueryRequest; output: \mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}.  */
        in item 'queryRequest' : ArrowheadBlacklistData::'BlacklistQueryRequest'[0..1];
        out item 'queryResponse' : JSONMessage[0..1];
        doc /* create: POST /blacklist/mgmt/create. Input: \mref[BlacklistCreateListRequest]{BlacklistCreateListRequest}; output: \mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}.  */
        in item 'createRequest' : JSONMessage[0..1];
        out item 'createResponse' : JSONMessage[0..1];
        doc /* remove: DELETE /blacklist/mgmt/remove?names=\fparam{{names}}. Input: Array<String>; output: None.  */
        in attribute 'removeRequest' : ScalarValues::String[0..*];
    }
    port def Blacklist_blacklistManagement_HTTP_IDD_Implementation :> Blacklist_blacklistManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Blacklist_blacklistManagement_http_query_0' {
        doc /* Operation query. Wire method: POST; address: /blacklist/mgmt/query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/blacklist/mgmt/query"; requestType = "BlacklistQueryRequest"; responseType = "\\mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadBlacklistData::'BlacklistQueryRequest'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Blacklist_blacklistManagement_http_create_1' {
        doc /* Operation create. Wire method: POST; address: /blacklist/mgmt/create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/blacklist/mgmt/create"; requestType = "\\mref[BlacklistCreateListRequest]{BlacklistCreateListRequest}"; responseType = "\\mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : JSONMessage[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Blacklist_blacklistManagement_http_remove_2' {
        doc /* Operation remove. Wire method: DELETE; address: /blacklist/mgmt/remove?names=\fparam{{names}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/blacklist/mgmt/remove?names=\\fparam{{names}}"; requestType = "Array<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
    }
    port def Blacklist_blacklistManagement_MQTT_IDD :> Blacklist_blacklistManagement_SD {
        @IDD { MicroserviceName = "blacklistManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-blacklistManagement-MQTT-IDD.tex"; }
        doc /* create: PUBLISH arrowhead/blacklist/management/create. Input: \mref[BlacklistCreateListRequest]{BlacklistCreateListRequest}; output: \mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}.  */
        in item 'createRequest' : JSONMessage[0..1];
        out item 'createResponse' : JSONMessage[0..1];
        doc /* query: PUBLISH arrowhead/blacklist/management/query. Input: BlacklistQueryRequest; output: \mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}.  */
        in item 'queryRequest' : ArrowheadBlacklistData::'BlacklistQueryRequest'[0..1];
        out item 'queryResponse' : JSONMessage[0..1];
        doc /* remove: PUBLISH arrowhead/blacklist/management/remove. Input: Array<String>; output: None.  */
        in attribute 'removeRequest' : ScalarValues::String[0..*];
        out item 'removeResponse' : JSONMessage[0..1];
    }
    port def Blacklist_blacklistManagement_MQTT_IDD_Implementation :> Blacklist_blacklistManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'Blacklist_blacklistManagement_mqtt_create_0' {
        doc /* Operation create. Wire method: PUBLISH; address: arrowhead/blacklist/management/create. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "PUBLISH"; address = "arrowhead/blacklist/management/create"; requestType = "\\mref[BlacklistCreateListRequest]{BlacklistCreateListRequest}"; responseType = "\\mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : JSONMessage[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Blacklist_blacklistManagement_mqtt_query_1' {
        doc /* Operation query. Wire method: PUBLISH; address: arrowhead/blacklist/management/query. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "PUBLISH"; address = "arrowhead/blacklist/management/query"; requestType = "BlacklistQueryRequest"; responseType = "\\mref[BlacklistEntryListResponse]{BlacklistEntryListResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadBlacklistData::'BlacklistQueryRequest'[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Blacklist_blacklistManagement_mqtt_remove_2' {
        doc /* Operation remove. Wire method: PUBLISH; address: arrowhead/blacklist/management/remove. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "PUBLISH"; address = "arrowhead/blacklist/management/remove"; requestType = "Array<String>"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : JSONMessage[0..1];
    }
    port def Blacklist_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-SysD.tex */
    }
    port def Blacklist_generalManagement_HTTP_IDD :> Blacklist_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-generalManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/blacklist/general/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address blacklist, port 8464 and base path /blacklist/general/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-log & POST & /logs \\ \hline
get-config & GET & /get-config \\ \hline
\end{longtable}

\newpage */
        doc /* get-log: POST /blacklist/general/mgmt/logs. Input: LogRequest; output: LogEntryListResponse.  */
        in item 'get-logRequest' : ArrowheadBlacklistData::'LogRequest'[0..1];
        out item 'get-logResponse' : ArrowheadBlacklistData::'LogEntryListResponse'[0..1];
        doc /* get-config: GET /blacklist/general/mgmt/get-config?keys=\fparam{{keys}}. Input: None; output: KeyValues.  */
        out item 'get-configResponse' : ArrowheadBlacklistData::'KeyValues'[0..1];
    }
    port def Blacklist_generalManagement_HTTP_IDD_Implementation :> Blacklist_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Blacklist_generalManagement_http_get-log_0' {
        doc /* Operation get-log. Wire method: POST; address: /blacklist/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/blacklist/general/mgmt/logs"; requestType = "LogRequest"; responseType = "LogEntryListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadBlacklistData::'LogRequest'[0..1];
        out item 'response' : ArrowheadBlacklistData::'LogEntryListResponse'[0..1];
    }
    action def 'Blacklist_generalManagement_http_get-config_1' {
        doc /* Operation get-config. Wire method: GET; address: /blacklist/general/mgmt/get-config?keys=\fparam{{keys}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/blacklist/general/mgmt/get-config?keys=\\fparam{{keys}}"; requestType = "unspecified"; responseType = "KeyValues"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadBlacklistData::'KeyValues'[0..1];
    }
    port def Blacklist_generalManagement_MQTT_IDD :> Blacklist_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'mqtt'; }
        @SourceTrace { path = "ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-generalManagement-MQTT-IDD.tex"; }
        doc /* get-log: PUBLISH arrowhead/blacklist/general/management/get-log. Input: LogRequest; output: LogEntryListResponse.  */
        in item 'get-logRequest' : ArrowheadBlacklistData::'LogRequest'[0..1];
        out item 'get-logResponse' : ArrowheadBlacklistData::'LogEntryListResponse'[0..1];
        doc /* get-config: PUBLISH arrowhead/blacklist/general/management/get-config. Input: Array<String>; output: KeyValues.  */
        in attribute 'get-configRequest' : ScalarValues::String[0..*];
        out item 'get-configResponse' : ArrowheadBlacklistData::'KeyValues'[0..1];
    }
    port def Blacklist_generalManagement_MQTT_IDD_Implementation :> Blacklist_generalManagement_MQTT_IDD { @IDD_Implementation; }
    action def 'Blacklist_generalManagement_mqtt_get-log_0' {
        doc /* Operation get-log. Wire method: PUBLISH; address: arrowhead/blacklist/general/management/get-log. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "PUBLISH"; address = "arrowhead/blacklist/general/management/get-log"; requestType = "LogRequest"; responseType = "LogEntryListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadBlacklistData::'LogRequest'[0..1];
        out item 'response' : ArrowheadBlacklistData::'LogEntryListResponse'[0..1];
    }
    action def 'Blacklist_generalManagement_mqtt_get-config_1' {
        doc /* Operation get-config. Wire method: PUBLISH; address: arrowhead/blacklist/general/management/get-config. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "mqtt"; method = "PUBLISH"; address = "arrowhead/blacklist/general/management/get-config"; requestType = "Array<String>"; responseType = "KeyValues"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..*];
        out item 'response' : ArrowheadBlacklistData::'KeyValues'[0..1];
    }
    port def Blacklist_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-SysD.tex */
    }
    port def Blacklist_sysMonitor_AMQP_IDD :> Blacklist_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.Blacklist.Blacklist";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, Blacklist registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.Blacklist.Blacklist",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.Blacklist.Blacklist.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadBlacklistData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.Blacklist.Blacklist.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadBlacklistData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.Blacklist.Blacklist.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadBlacklistData::'Observation'[0..1];
    }
    port def Blacklist_sysMonitor_AMQP_IDD_Implementation :> Blacklist_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'Blacklist_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Blacklist.Blacklist.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Blacklist.Blacklist.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadBlacklistData::'Observation'[0..1];
    }
    action def 'Blacklist_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Blacklist.Blacklist.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Blacklist.Blacklist.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadBlacklistData::'Observation'[0..1];
    }
    action def 'Blacklist_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Blacklist.Blacklist.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Blacklist.Blacklist.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadBlacklistData::'Observation'[0..1];
    }
    part def Blacklist_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "Blacklist"; FunctionalProperties = "This document describes the Blacklist support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/Blacklist and deployed in the Arrowhead-test-cloud-java local cloud.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-Blacklist-java-spring/documentation/v5.2.2/Blacklist-v5.2.2-SysD.tex"; }
        doc /* This document describes the Blacklist support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/Blacklist and deployed in the Arrowhead-test-cloud-java local cloud.
  \end{abstract} */
        port 'blacklistDiscovery_SD' : Blacklist_blacklistDiscovery_SD;
        port 'blacklistManagement_SD' : Blacklist_blacklistManagement_SD;
        port 'generalManagement_SD' : Blacklist_generalManagement_SD;
        port 'sysMonitor_SD' : Blacklist_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, ConsumerAuthorization, ServiceRegistry. These are external requirements, not assumed co-deployments. */
    }
    part def Blacklist_SysDD :> Blacklist_SysD {
        @SysDD;
        port 'blacklistDiscovery_http' : Blacklist_blacklistDiscovery_HTTP_IDD;
        port 'blacklistDiscovery_mqtt' : Blacklist_blacklistDiscovery_MQTT_IDD;
        port 'blacklistManagement_http' : Blacklist_blacklistManagement_HTTP_IDD;
        port 'blacklistManagement_mqtt' : Blacklist_blacklistManagement_MQTT_IDD;
        port 'generalManagement_http' : Blacklist_generalManagement_HTTP_IDD;
        port 'generalManagement_mqtt' : Blacklist_generalManagement_MQTT_IDD;
        port 'sysMonitor_amqp' : Blacklist_sysMonitor_AMQP_IDD;
    }
    part def Blacklist :> Blacklist_SysDD {
        @System_Implementation;
    }
}
