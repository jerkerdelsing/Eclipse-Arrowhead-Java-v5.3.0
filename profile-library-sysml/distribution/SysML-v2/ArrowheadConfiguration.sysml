library package ArrowheadConfiguration {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def Configuration_configuration_SD :> ArrowheadService {
        @SD { MicroserviceName = "configuration"; }
        doc /* Service version 1.0.0. Source: ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-SysD.tex */
    }
    port def Configuration_configuration_HTTP_IDD :> Configuration_configuration_SD {
        @IDD { MicroserviceName = "configuration"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-configuration-HTTP-IDD.tex"; }
        attribute basePath : String = "/configuration";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address configuration, port 8459 and base path /configuration. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-configuration & GET & \texttt{/config/{systemName}} \\ \hline
get-raw-configuration & GET & \texttt{/config/raw/{systemName}} \\ \hline
\end{longtable}

\newpage */
        doc /* get-configuration: GET /configuration/config/\fparam{{systemName}}. Input: String; output: ConfigurationResponse.  */
        in attribute 'get-configurationRequest' : ScalarValues::String[0..1];
        out item 'get-configurationResponse' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
        doc /* get-raw-configuration: GET /configuration/config/raw/\fparam{{systemName}}. Input: String; output: Object.  */
        in attribute 'get-raw-configurationRequest' : ScalarValues::String[0..1];
        out item 'get-raw-configurationResponse' : JSONMessage[0..1];
    }
    port def Configuration_configuration_HTTP_IDD_Implementation :> Configuration_configuration_HTTP_IDD { @IDD_Implementation; }
    action def 'Configuration_configuration_http_get-configuration_0' {
        doc /* Operation get-configuration. Wire method: GET; address: /configuration/config/\fparam{{systemName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/configuration/config/\\fparam{{systemName}}"; requestType = "String"; responseType = "ConfigurationResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
    }
    action def 'Configuration_configuration_http_get-raw-configuration_1' {
        doc /* Operation get-raw-configuration. Wire method: GET; address: /configuration/config/raw/\fparam{{systemName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/configuration/config/raw/\\fparam{{systemName}}"; requestType = "String"; responseType = "Object"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : JSONMessage[0..1];
    }
    port def Configuration_configurationManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "configurationManagement"; }
        doc /* Service version 1.0.0. Source: ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-SysD.tex */
    }
    port def Configuration_configurationManagement_HTTP_IDD :> Configuration_configurationManagement_SD {
        @IDD { MicroserviceName = "configurationManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-configurationManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/configuration/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address configuration, port 8459 and base path /configuration/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
list-configurations & GET & /config \\ \hline
store-configuration & PUT & \texttt{/config/{systemName}} \\ \hline
remove-configuration & DELETE & \texttt{/config/{systemName}} \\ \hline
\end{longtable}

\newpage */
        doc /* list-configurations: GET /configuration/mgmt/config. Input: None; output: \mref[ConfigurationListResponse]{ConfigurationListResponse}.  */
        out item 'list-configurationsResponse' : JSONMessage[0..1];
        doc /* store-configuration: PUT /configuration/mgmt/config/\fparam{{systemName}}. Input: ConfigurationRequest; output: ConfigurationResponse.  */
        in item 'store-configurationRequest' : ArrowheadConfigurationData::'ConfigurationRequest'[0..1];
        out item 'store-configurationResponse' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
        doc /* remove-configuration: DELETE /configuration/mgmt/config/\fparam{{systemName}}. Input: String; output: ConfigurationResponse.  */
        in attribute 'remove-configurationRequest' : ScalarValues::String[0..1];
        out item 'remove-configurationResponse' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
    }
    port def Configuration_configurationManagement_HTTP_IDD_Implementation :> Configuration_configurationManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Configuration_configurationManagement_http_list-configurations_0' {
        doc /* Operation list-configurations. Wire method: GET; address: /configuration/mgmt/config. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/configuration/mgmt/config"; requestType = "unspecified"; responseType = "\\mref[ConfigurationListResponse]{ConfigurationListResponse}"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : JSONMessage[0..1];
    }
    action def 'Configuration_configurationManagement_http_store-configuration_1' {
        doc /* Operation store-configuration. Wire method: PUT; address: /configuration/mgmt/config/\fparam{{systemName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/configuration/mgmt/config/\\fparam{{systemName}}"; requestType = "ConfigurationRequest"; responseType = "ConfigurationResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadConfigurationData::'ConfigurationRequest'[0..1];
        out item 'response' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
    }
    action def 'Configuration_configurationManagement_http_remove-configuration_2' {
        doc /* Operation remove-configuration. Wire method: DELETE; address: /configuration/mgmt/config/\fparam{{systemName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/configuration/mgmt/config/\\fparam{{systemName}}"; requestType = "String"; responseType = "ConfigurationResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
    }
    port def Configuration_configurationProxy_SD :> ArrowheadService {
        @SD { MicroserviceName = "configurationProxy"; }
        doc /* Service version 1.0.0. Source: ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-SysD.tex */
    }
    port def Configuration_configurationProxy_HTTP_IDD :> Configuration_configurationProxy_SD {
        @IDD { MicroserviceName = "configurationProxy"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-configurationProxy-HTTP-IDD.tex"; }
        attribute basePath : String = "/configuration/proxy";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address configuration, port 8459 and base path /configuration/proxy. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-configuration & GET & \texttt{/config/{systemName}} \\ \hline
store-configuration & PUT & /save-config \\ \hline
\end{longtable}

\newpage */
        doc /* get-configuration: GET /configuration/proxy/config/\fparam{{systemName}}. Input: String; output: ConfigurationResponse.  */
        in attribute 'get-configurationRequest' : ScalarValues::String[0..1];
        out item 'get-configurationResponse' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
        doc /* store-configuration: PUT /configuration/proxy/save-config. Input: ConfigurationRequest; output: ConfigurationResponse.  */
        in item 'store-configurationRequest' : ArrowheadConfigurationData::'ConfigurationRequest'[0..1];
        out item 'store-configurationResponse' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
    }
    port def Configuration_configurationProxy_HTTP_IDD_Implementation :> Configuration_configurationProxy_HTTP_IDD { @IDD_Implementation; }
    action def 'Configuration_configurationProxy_http_get-configuration_0' {
        doc /* Operation get-configuration. Wire method: GET; address: /configuration/proxy/config/\fparam{{systemName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/configuration/proxy/config/\\fparam{{systemName}}"; requestType = "String"; responseType = "ConfigurationResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
    }
    action def 'Configuration_configurationProxy_http_store-configuration_1' {
        doc /* Operation store-configuration. Wire method: PUT; address: /configuration/proxy/save-config. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/configuration/proxy/save-config"; requestType = "ConfigurationRequest"; responseType = "ConfigurationResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadConfigurationData::'ConfigurationRequest'[0..1];
        out item 'response' : ArrowheadConfigurationData::'ConfigurationResponse'[0..1];
    }
    port def Configuration_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-SysD.tex */
    }
    port def Configuration_generalManagement_HTTP_IDD :> Configuration_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-generalManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/configuration/general/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address configuration, port 8459 and base path /configuration/general/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-log & POST & /logs \\ \hline
get-config & GET & /get-config \\ \hline
\end{longtable}

\newpage */
        doc /* get-log: POST /configuration/general/mgmt/logs. Input: LogRequest; output: LogEntryListResponse.  */
        in item 'get-logRequest' : ArrowheadConfigurationData::'LogRequest'[0..1];
        out item 'get-logResponse' : ArrowheadConfigurationData::'LogEntryListResponse'[0..1];
        doc /* get-config: GET /configuration/general/mgmt/get-config?keys=\fparam{{keys}}. Input: None; output: KeyValues.  */
        out item 'get-configResponse' : ArrowheadConfigurationData::'KeyValues'[0..1];
    }
    port def Configuration_generalManagement_HTTP_IDD_Implementation :> Configuration_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Configuration_generalManagement_http_get-log_0' {
        doc /* Operation get-log. Wire method: POST; address: /configuration/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/configuration/general/mgmt/logs"; requestType = "LogRequest"; responseType = "LogEntryListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadConfigurationData::'LogRequest'[0..1];
        out item 'response' : ArrowheadConfigurationData::'LogEntryListResponse'[0..1];
    }
    action def 'Configuration_generalManagement_http_get-config_1' {
        doc /* Operation get-config. Wire method: GET; address: /configuration/general/mgmt/get-config?keys=\fparam{{keys}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/configuration/general/mgmt/get-config?keys=\\fparam{{keys}}"; requestType = "unspecified"; responseType = "KeyValues"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadConfigurationData::'KeyValues'[0..1];
    }
    port def Configuration_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-SysD.tex */
    }
    port def Configuration_sysMonitor_AMQP_IDD :> Configuration_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.Configuration.Configuration";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, Configuration registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.Configuration.Configuration",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.Configuration.Configuration.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadConfigurationData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.Configuration.Configuration.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadConfigurationData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.Configuration.Configuration.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadConfigurationData::'Observation'[0..1];
    }
    port def Configuration_sysMonitor_AMQP_IDD_Implementation :> Configuration_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'Configuration_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Configuration.Configuration.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Configuration.Configuration.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadConfigurationData::'Observation'[0..1];
    }
    action def 'Configuration_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Configuration.Configuration.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Configuration.Configuration.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadConfigurationData::'Observation'[0..1];
    }
    action def 'Configuration_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Configuration.Configuration.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Configuration.Configuration.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadConfigurationData::'Observation'[0..1];
    }
    part def Configuration_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "Configuration"; FunctionalProperties = "This document describes the Configuration support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/Configuration and deployed in the Arrowhead-test-cloud-java local cloud.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-Configuration-java-spring/documentation/v5.2.2/Configuration-v5.2.2-SysD.tex"; }
        doc /* This document describes the Configuration support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/Configuration and deployed in the Arrowhead-test-cloud-java local cloud.
  \end{abstract} */
        port 'configuration_SD' : Configuration_configuration_SD;
        port 'configurationManagement_SD' : Configuration_configurationManagement_SD;
        port 'configurationProxy_SD' : Configuration_configurationProxy_SD;
        port 'generalManagement_SD' : Configuration_generalManagement_SD;
        port 'sysMonitor_SD' : Configuration_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, ConsumerAuthorization, ServiceRegistry. These are external requirements, not assumed co-deployments. */
    }
    part def Configuration_SysDD :> Configuration_SysD {
        @SysDD;
        port 'configuration_http' : Configuration_configuration_HTTP_IDD;
        port 'configurationManagement_http' : Configuration_configurationManagement_HTTP_IDD;
        port 'configurationProxy_http' : Configuration_configurationProxy_HTTP_IDD;
        port 'generalManagement_http' : Configuration_generalManagement_HTTP_IDD;
        port 'sysMonitor_amqp' : Configuration_sysMonitor_AMQP_IDD;
    }
    part def Configuration :> Configuration_SysDD {
        @System_Implementation;
    }
}
