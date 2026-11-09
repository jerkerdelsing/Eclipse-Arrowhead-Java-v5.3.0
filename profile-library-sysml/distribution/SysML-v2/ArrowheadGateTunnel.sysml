library package ArrowheadGateTunnel {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def GateTunnel_gateTunnelConnection_SD :> ArrowheadService {
        @SD { MicroserviceName = "gateTunnelConnection"; }
        doc /* Service version 1.0.0. Source: ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-SysD.tex */
    }
    port def GateTunnel_gateTunnelConnection_HTTP_IDD :> GateTunnel_gateTunnelConnection_SD {
        @IDD { MicroserviceName = "gateTunnelConnection"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-gateTunnelConnection-HTTP-IDD.tex"; }
        attribute basePath : String = "/gatetunnel";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address gatetunnel, port 8453 and base path /gatetunnel. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
connect-provider & POST & /connect_provider \\ \hline
connect-consumer & POST & /connect_consumer \\ \hline
close-sessions & POST & /close_sessions \\ \hline
open-provider-session & POST & /intercloud/provider \\ \hline
open-consumer-session & POST & /intercloud/consumer \\ \hline
\end{longtable}

\newpage */
        doc /* connect-provider: POST /gatetunnel/connect_provider. Input: ControlSessionRequest; output: ControlSessionResult.  */
        in item 'connect-providerRequest' : ArrowheadGateTunnelData::'ControlSessionRequest'[0..1];
        out item 'connect-providerResponse' : ArrowheadGateTunnelData::'ControlSessionResult'[0..1];
        doc /* connect-consumer: POST /gatetunnel/connect_consumer. Input: ConnectConsumerRequest; output: ControlSessionResult.  */
        in item 'connect-consumerRequest' : ArrowheadGateTunnelData::'ConnectConsumerRequest'[0..1];
        out item 'connect-consumerResponse' : ArrowheadGateTunnelData::'ControlSessionResult'[0..1];
        doc /* close-sessions: POST /gatetunnel/close_sessions. Input: ClosePortsRequest; output: Array<Object>.  */
        in item 'close-sessionsRequest' : ArrowheadGateTunnelData::'ClosePortsRequest'[0..1];
        out item 'close-sessionsResponse' : JSONMessage[0..*];
        doc /* open-provider-session: POST /gatetunnel/intercloud/provider. Input: ProviderSessionRequest; output: SessionEndpoint.  */
        in item 'open-provider-sessionRequest' : ArrowheadGateTunnelData::'ProviderSessionRequest'[0..1];
        out item 'open-provider-sessionResponse' : ArrowheadGateTunnelData::'SessionEndpoint'[0..1];
        doc /* open-consumer-session: POST /gatetunnel/intercloud/consumer. Input: ConsumerSessionRequest; output: SessionEndpoint.  */
        in item 'open-consumer-sessionRequest' : ArrowheadGateTunnelData::'ConsumerSessionRequest'[0..1];
        out item 'open-consumer-sessionResponse' : ArrowheadGateTunnelData::'SessionEndpoint'[0..1];
    }
    port def GateTunnel_gateTunnelConnection_HTTP_IDD_Implementation :> GateTunnel_gateTunnelConnection_HTTP_IDD { @IDD_Implementation; }
    action def 'GateTunnel_gateTunnelConnection_http_connect-provider_0' {
        doc /* Operation connect-provider. Wire method: POST; address: /gatetunnel/connect_provider. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/gatetunnel/connect_provider"; requestType = "ControlSessionRequest"; responseType = "ControlSessionResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadGateTunnelData::'ControlSessionRequest'[0..1];
        out item 'response' : ArrowheadGateTunnelData::'ControlSessionResult'[0..1];
    }
    action def 'GateTunnel_gateTunnelConnection_http_connect-consumer_1' {
        doc /* Operation connect-consumer. Wire method: POST; address: /gatetunnel/connect_consumer. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/gatetunnel/connect_consumer"; requestType = "ConnectConsumerRequest"; responseType = "ControlSessionResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadGateTunnelData::'ConnectConsumerRequest'[0..1];
        out item 'response' : ArrowheadGateTunnelData::'ControlSessionResult'[0..1];
    }
    action def 'GateTunnel_gateTunnelConnection_http_close-sessions_2' {
        doc /* Operation close-sessions. Wire method: POST; address: /gatetunnel/close_sessions. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/gatetunnel/close_sessions"; requestType = "ClosePortsRequest"; responseType = "Array<Object>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadGateTunnelData::'ClosePortsRequest'[0..1];
        out item 'response' : JSONMessage[0..*];
    }
    action def 'GateTunnel_gateTunnelConnection_http_open-provider-session_3' {
        doc /* Operation open-provider-session. Wire method: POST; address: /gatetunnel/intercloud/provider. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/gatetunnel/intercloud/provider"; requestType = "ProviderSessionRequest"; responseType = "SessionEndpoint"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadGateTunnelData::'ProviderSessionRequest'[0..1];
        out item 'response' : ArrowheadGateTunnelData::'SessionEndpoint'[0..1];
    }
    action def 'GateTunnel_gateTunnelConnection_http_open-consumer-session_4' {
        doc /* Operation open-consumer-session. Wire method: POST; address: /gatetunnel/intercloud/consumer. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/gatetunnel/intercloud/consumer"; requestType = "ConsumerSessionRequest"; responseType = "SessionEndpoint"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadGateTunnelData::'ConsumerSessionRequest'[0..1];
        out item 'response' : ArrowheadGateTunnelData::'SessionEndpoint'[0..1];
    }
    port def GateTunnel_gateTunnelKey_SD :> ArrowheadService {
        @SD { MicroserviceName = "gateTunnelKey"; }
        doc /* Service version 1.0.0. Source: ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-SysD.tex */
    }
    port def GateTunnel_gateTunnelKey_HTTP_IDD :> GateTunnel_gateTunnelKey_SD {
        @IDD { MicroserviceName = "gateTunnelKey"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-gateTunnelKey-HTTP-IDD.tex"; }
        attribute basePath : String = "/gatetunnel";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address gatetunnel, port 8453 and base path /gatetunnel. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
public-key & GET & /publickey \\ \hline
\end{longtable}

\newpage */
        doc /* public-key: GET /gatetunnel/publickey. Input: None; output: PublicKey.  */
        out item 'public-keyResponse' : ArrowheadGateTunnelData::'PublicKey'[0..1];
    }
    port def GateTunnel_gateTunnelKey_HTTP_IDD_Implementation :> GateTunnel_gateTunnelKey_HTTP_IDD { @IDD_Implementation; }
    action def 'GateTunnel_gateTunnelKey_http_public-key_0' {
        doc /* Operation public-key. Wire method: GET; address: /gatetunnel/publickey. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/gatetunnel/publickey"; requestType = "unspecified"; responseType = "PublicKey"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadGateTunnelData::'PublicKey'[0..1];
    }
    port def GateTunnel_gateTunnelManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "gateTunnelManagement"; }
        doc /* Service version 1.0.0. Source: ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-SysD.tex */
    }
    port def GateTunnel_gateTunnelManagement_HTTP_IDD :> GateTunnel_gateTunnelManagement_SD {
        @IDD { MicroserviceName = "gateTunnelManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-gateTunnelManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/gatetunnel";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address gatetunnel, port 8453 and base path /gatetunnel. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
sessions & GET & /mgmt/sessions \\ \hline
close & POST & /mgmt/sessions/close \\ \hline
status & GET & /mgmt/status \\ \hline
\end{longtable}

\newpage */
        doc /* sessions: GET /gatetunnel/mgmt/sessions?limit=\fparam{{limit}}. Input: None; output: SessionList.  */
        out item 'sessionsResponse' : ArrowheadGateTunnelData::'SessionList'[0..1];
        doc /* close: POST /gatetunnel/mgmt/sessions/close. Input: SessionCloseRequest; output: CloseResult.  */
        in item 'closeRequest' : ArrowheadGateTunnelData::'SessionCloseRequest'[0..1];
        out item 'closeResponse' : ArrowheadGateTunnelData::'CloseResult'[0..1];
        doc /* status: GET /gatetunnel/mgmt/status. Input: None; output: Status.  */
        out item 'statusResponse' : ArrowheadGateTunnelData::'Status'[0..1];
    }
    port def GateTunnel_gateTunnelManagement_HTTP_IDD_Implementation :> GateTunnel_gateTunnelManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'GateTunnel_gateTunnelManagement_http_sessions_0' {
        doc /* Operation sessions. Wire method: GET; address: /gatetunnel/mgmt/sessions?limit=\fparam{{limit}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/gatetunnel/mgmt/sessions?limit=\\fparam{{limit}}"; requestType = "unspecified"; responseType = "SessionList"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadGateTunnelData::'SessionList'[0..1];
    }
    action def 'GateTunnel_gateTunnelManagement_http_close_1' {
        doc /* Operation close. Wire method: POST; address: /gatetunnel/mgmt/sessions/close. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/gatetunnel/mgmt/sessions/close"; requestType = "SessionCloseRequest"; responseType = "CloseResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadGateTunnelData::'SessionCloseRequest'[0..1];
        out item 'response' : ArrowheadGateTunnelData::'CloseResult'[0..1];
    }
    action def 'GateTunnel_gateTunnelManagement_http_status_2' {
        doc /* Operation status. Wire method: GET; address: /gatetunnel/mgmt/status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/gatetunnel/mgmt/status"; requestType = "unspecified"; responseType = "Status"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadGateTunnelData::'Status'[0..1];
    }
    port def GateTunnel_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-SysD.tex */
    }
    port def GateTunnel_sysMonitor_AMQP_IDD :> GateTunnel_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.GateTunnel.GateTunnel";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, GateTunnel registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.GateTunnel.GateTunnel",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.GateTunnel.GateTunnel.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadGateTunnelData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.GateTunnel.GateTunnel.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadGateTunnelData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.GateTunnel.GateTunnel.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadGateTunnelData::'Observation'[0..1];
    }
    port def GateTunnel_sysMonitor_AMQP_IDD_Implementation :> GateTunnel_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'GateTunnel_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.GateTunnel.GateTunnel.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.GateTunnel.GateTunnel.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadGateTunnelData::'Observation'[0..1];
    }
    action def 'GateTunnel_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.GateTunnel.GateTunnel.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.GateTunnel.GateTunnel.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadGateTunnelData::'Observation'[0..1];
    }
    action def 'GateTunnel_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.GateTunnel.GateTunnel.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.GateTunnel.GateTunnel.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadGateTunnelData::'Observation'[0..1];
    }
    part def GateTunnel_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "GateTunnel"; FunctionalProperties = "This document describes the GateTunnel support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-GateTunnel-java-spring with an independent local-cloud deployment definition in its own folder.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-GateTunnel-java-spring/documentation/v5.2.2/GateTunnel-v5.2.2-SysD.tex"; }
        doc /* This document describes the GateTunnel support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-GateTunnel-java-spring with an independent local-cloud deployment definition in its own folder.
  \end{abstract} */
        port 'gateTunnelConnection_SD' : GateTunnel_gateTunnelConnection_SD;
        port 'gateTunnelKey_SD' : GateTunnel_gateTunnelKey_SD;
        port 'gateTunnelManagement_SD' : GateTunnel_gateTunnelManagement_SD;
        port 'sysMonitor_SD' : GateTunnel_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, Providers, Relay (MQTT), ServiceRegistry. These are external requirements, not assumed co-deployments. */
    }
    part def GateTunnel_SysDD :> GateTunnel_SysD {
        @SysDD;
        port 'gateTunnelConnection_http' : GateTunnel_gateTunnelConnection_HTTP_IDD;
        port 'gateTunnelKey_http' : GateTunnel_gateTunnelKey_HTTP_IDD;
        port 'gateTunnelManagement_http' : GateTunnel_gateTunnelManagement_HTTP_IDD;
        port 'sysMonitor_amqp' : GateTunnel_sysMonitor_AMQP_IDD;
    }
    part def GateTunnel :> GateTunnel_SysDD {
        @System_Implementation;
    }
}
