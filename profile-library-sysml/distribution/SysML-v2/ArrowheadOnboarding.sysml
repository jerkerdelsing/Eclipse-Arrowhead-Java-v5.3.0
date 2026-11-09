library package ArrowheadOnboarding {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def Onboarding_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-SysD.tex */
    }
    port def Onboarding_generalManagement_HTTP_IDD :> Onboarding_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-generalManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/onboarding/general/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address onboarding, port 8435 and base path /onboarding/general/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-log & POST & /logs \\ \hline
get-config & GET & /get-config \\ \hline
\end{longtable}

\newpage */
        doc /* get-log: POST /onboarding/general/mgmt/logs. Input: LogRequest; output: LogEntryListResponse.  */
        in item 'get-logRequest' : ArrowheadOnboardingData::'LogRequest'[0..1];
        out item 'get-logResponse' : ArrowheadOnboardingData::'LogEntryListResponse'[0..1];
        doc /* get-config: GET /onboarding/general/mgmt/get-config?keys=\fparam{{keys}}. Input: None; output: KeyValues.  */
        out item 'get-configResponse' : ArrowheadOnboardingData::'KeyValues'[0..1];
    }
    port def Onboarding_generalManagement_HTTP_IDD_Implementation :> Onboarding_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Onboarding_generalManagement_http_get-log_0' {
        doc /* Operation get-log. Wire method: POST; address: /onboarding/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/onboarding/general/mgmt/logs"; requestType = "LogRequest"; responseType = "LogEntryListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadOnboardingData::'LogRequest'[0..1];
        out item 'response' : ArrowheadOnboardingData::'LogEntryListResponse'[0..1];
    }
    action def 'Onboarding_generalManagement_http_get-config_1' {
        doc /* Operation get-config. Wire method: GET; address: /onboarding/general/mgmt/get-config?keys=\fparam{{keys}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/onboarding/general/mgmt/get-config?keys=\\fparam{{keys}}"; requestType = "unspecified"; responseType = "KeyValues"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadOnboardingData::'KeyValues'[0..1];
    }
    port def Onboarding_onboarding_SD :> ArrowheadService {
        @SD { MicroserviceName = "onboarding"; }
        doc /* Service version 1.0.0. Source: ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-SysD.tex */
    }
    port def Onboarding_onboarding_HTTP_IDD :> Onboarding_onboarding_SD {
        @IDD { MicroserviceName = "onboarding"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-onboarding-HTTP-IDD.tex"; }
        attribute basePath : String = "/onboarding";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address onboarding, port 8435 and base path /onboarding. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
onboard-with-name & POST & /certificate/name \\ \hline
onboard-with-csr & POST & /certificate/csr \\ \hline
onboard-device & POST & /device \\ \hline
onboard-system & POST & /system \\ \hline
offboard-device & POST & /device/offboard \\ \hline
\end{longtable}

\newpage */
        doc /* onboard-with-name: POST /onboarding/certificate/name. Input: NameRequest; output: Response.  */
        in item 'onboard-with-nameRequest' : ArrowheadOnboardingData::'NameRequest'[0..1];
        out item 'onboard-with-nameResponse' : ArrowheadOnboardingData::'Response'[0..1];
        doc /* onboard-with-csr: POST /onboarding/certificate/csr. Input: CsrRequest; output: Response.  */
        in item 'onboard-with-csrRequest' : ArrowheadOnboardingData::'CsrRequest'[0..1];
        out item 'onboard-with-csrResponse' : ArrowheadOnboardingData::'Response'[0..1];
        doc /* onboard-device: POST /onboarding/device. Input: DeviceRequest; output: DeviceResponse.  */
        in item 'onboard-deviceRequest' : ArrowheadOnboardingData::'DeviceRequest'[0..1];
        out item 'onboard-deviceResponse' : ArrowheadOnboardingData::'DeviceResponse'[0..1];
        doc /* onboard-system: POST /onboarding/system. Input: SystemRequest; output: SystemResponse.  */
        in item 'onboard-systemRequest' : ArrowheadOnboardingData::'SystemRequest'[0..1];
        out item 'onboard-systemResponse' : ArrowheadOnboardingData::'SystemResponse'[0..1];
        doc /* offboard-device: POST /onboarding/device/offboard. Input: None; output: OffboardingReport.  */
        out item 'offboard-deviceResponse' : ArrowheadOnboardingData::'OffboardingReport'[0..1];
    }
    port def Onboarding_onboarding_HTTP_IDD_Implementation :> Onboarding_onboarding_HTTP_IDD { @IDD_Implementation; }
    action def 'Onboarding_onboarding_http_onboard-with-name_0' {
        doc /* Operation onboard-with-name. Wire method: POST; address: /onboarding/certificate/name. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/onboarding/certificate/name"; requestType = "NameRequest"; responseType = "Response"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadOnboardingData::'NameRequest'[0..1];
        out item 'response' : ArrowheadOnboardingData::'Response'[0..1];
    }
    action def 'Onboarding_onboarding_http_onboard-with-csr_1' {
        doc /* Operation onboard-with-csr. Wire method: POST; address: /onboarding/certificate/csr. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/onboarding/certificate/csr"; requestType = "CsrRequest"; responseType = "Response"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadOnboardingData::'CsrRequest'[0..1];
        out item 'response' : ArrowheadOnboardingData::'Response'[0..1];
    }
    action def 'Onboarding_onboarding_http_onboard-device_2' {
        doc /* Operation onboard-device. Wire method: POST; address: /onboarding/device. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/onboarding/device"; requestType = "DeviceRequest"; responseType = "DeviceResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadOnboardingData::'DeviceRequest'[0..1];
        out item 'response' : ArrowheadOnboardingData::'DeviceResponse'[0..1];
    }
    action def 'Onboarding_onboarding_http_onboard-system_3' {
        doc /* Operation onboard-system. Wire method: POST; address: /onboarding/system. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/onboarding/system"; requestType = "SystemRequest"; responseType = "SystemResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadOnboardingData::'SystemRequest'[0..1];
        out item 'response' : ArrowheadOnboardingData::'SystemResponse'[0..1];
    }
    action def 'Onboarding_onboarding_http_offboard-device_4' {
        doc /* Operation offboard-device. Wire method: POST; address: /onboarding/device/offboard. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/onboarding/device/offboard"; requestType = "unspecified"; responseType = "OffboardingReport"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadOnboardingData::'OffboardingReport'[0..1];
    }
    port def Onboarding_onboardingManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "onboardingManagement"; }
        doc /* Service version 1.0.0. Source: ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-SysD.tex */
    }
    port def Onboarding_onboardingManagement_HTTP_IDD :> Onboarding_onboardingManagement_SD {
        @IDD { MicroserviceName = "onboardingManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-onboardingManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/onboarding/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address onboarding, port 8435 and base path /onboarding/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
query-enrollments & GET & /enrollments \\ \hline
get-enrollment & GET & \texttt{/enrollments/{deviceName}} \\ \hline
approve-device & PUT & \texttt{/enrollments/{deviceName}} \\ \hline
offboard-device & POST & \texttt{/enrollments/{deviceName}/offboard} \\ \hline
remove-enrollment & DELETE & \texttt{/enrollments/{deviceName}} \\ \hline
\end{longtable}

\newpage */
        doc /* query-enrollments: GET /onboarding/mgmt/enrollments. Input: None; output: EnrollmentList.  */
        out item 'query-enrollmentsResponse' : ArrowheadOnboardingData::'EnrollmentList'[0..1];
        doc /* get-enrollment: GET /onboarding/mgmt/enrollments/\fparam{{deviceName}}. Input: String; output: EnrollmentView.  */
        in attribute 'get-enrollmentRequest' : ScalarValues::String[0..1];
        out item 'get-enrollmentResponse' : ArrowheadOnboardingData::'EnrollmentView'[0..1];
        doc /* approve-device: PUT /onboarding/mgmt/enrollments/\fparam{{deviceName}}. Input: EnrollmentRequest; output: EnrollmentView.  */
        in item 'approve-deviceRequest' : ArrowheadOnboardingData::'EnrollmentRequest'[0..1];
        out item 'approve-deviceResponse' : ArrowheadOnboardingData::'EnrollmentView'[0..1];
        doc /* offboard-device: POST /onboarding/mgmt/enrollments/\fparam{{deviceName}}/offboard. Input: String; output: OffboardingReport.  */
        in attribute 'offboard-deviceRequest' : ScalarValues::String[0..1];
        out item 'offboard-deviceResponse' : ArrowheadOnboardingData::'OffboardingReport'[0..1];
        doc /* remove-enrollment: DELETE /onboarding/mgmt/enrollments/\fparam{{deviceName}}. Input: String; output: OffboardingReport.  */
        in attribute 'remove-enrollmentRequest' : ScalarValues::String[0..1];
        out item 'remove-enrollmentResponse' : ArrowheadOnboardingData::'OffboardingReport'[0..1];
    }
    port def Onboarding_onboardingManagement_HTTP_IDD_Implementation :> Onboarding_onboardingManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'Onboarding_onboardingManagement_http_query-enrollments_0' {
        doc /* Operation query-enrollments. Wire method: GET; address: /onboarding/mgmt/enrollments. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/onboarding/mgmt/enrollments"; requestType = "unspecified"; responseType = "EnrollmentList"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadOnboardingData::'EnrollmentList'[0..1];
    }
    action def 'Onboarding_onboardingManagement_http_get-enrollment_1' {
        doc /* Operation get-enrollment. Wire method: GET; address: /onboarding/mgmt/enrollments/\fparam{{deviceName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/onboarding/mgmt/enrollments/\\fparam{{deviceName}}"; requestType = "String"; responseType = "EnrollmentView"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadOnboardingData::'EnrollmentView'[0..1];
    }
    action def 'Onboarding_onboardingManagement_http_approve-device_2' {
        doc /* Operation approve-device. Wire method: PUT; address: /onboarding/mgmt/enrollments/\fparam{{deviceName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/onboarding/mgmt/enrollments/\\fparam{{deviceName}}"; requestType = "EnrollmentRequest"; responseType = "EnrollmentView"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadOnboardingData::'EnrollmentRequest'[0..1];
        out item 'response' : ArrowheadOnboardingData::'EnrollmentView'[0..1];
    }
    action def 'Onboarding_onboardingManagement_http_offboard-device_3' {
        doc /* Operation offboard-device. Wire method: POST; address: /onboarding/mgmt/enrollments/\fparam{{deviceName}}/offboard. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/onboarding/mgmt/enrollments/\\fparam{{deviceName}}/offboard"; requestType = "String"; responseType = "OffboardingReport"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadOnboardingData::'OffboardingReport'[0..1];
    }
    action def 'Onboarding_onboardingManagement_http_remove-enrollment_4' {
        doc /* Operation remove-enrollment. Wire method: DELETE; address: /onboarding/mgmt/enrollments/\fparam{{deviceName}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/onboarding/mgmt/enrollments/\\fparam{{deviceName}}"; requestType = "String"; responseType = "OffboardingReport"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadOnboardingData::'OffboardingReport'[0..1];
    }
    port def Onboarding_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-SysD.tex */
    }
    port def Onboarding_sysMonitor_AMQP_IDD :> Onboarding_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.Onboarding.Onboarding";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, Onboarding registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.Onboarding.Onboarding",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.Onboarding.Onboarding.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadOnboardingData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.Onboarding.Onboarding.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadOnboardingData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.Onboarding.Onboarding.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadOnboardingData::'Observation'[0..1];
    }
    port def Onboarding_sysMonitor_AMQP_IDD_Implementation :> Onboarding_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'Onboarding_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Onboarding.Onboarding.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Onboarding.Onboarding.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadOnboardingData::'Observation'[0..1];
    }
    action def 'Onboarding_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Onboarding.Onboarding.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Onboarding.Onboarding.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadOnboardingData::'Observation'[0..1];
    }
    action def 'Onboarding_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.Onboarding.Onboarding.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.Onboarding.Onboarding.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadOnboardingData::'Observation'[0..1];
    }
    part def Onboarding_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "Onboarding"; FunctionalProperties = "This document describes the Onboarding support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/Onboarding and deployed in the Arrowhead-test-cloud-java local cloud.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-Onboarding-java-spring/documentation/v5.2.2/Onboarding-v5.2.2-SysD.tex"; }
        doc /* This document describes the Onboarding support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/Onboarding and deployed in the Arrowhead-test-cloud-java local cloud.
  \end{abstract} */
        port 'generalManagement_SD' : Onboarding_generalManagement_SD;
        port 'onboarding_SD' : Onboarding_onboarding_SD;
        port 'onboardingManagement_SD' : Onboarding_onboardingManagement_SD;
        port 'sysMonitor_SD' : Onboarding_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, Blacklist, CertificateAuthority, ServiceRegistry. These are external requirements, not assumed co-deployments. */
    }
    part def Onboarding_SysDD :> Onboarding_SysD {
        @SysDD;
        port 'generalManagement_http' : Onboarding_generalManagement_HTTP_IDD;
        port 'onboarding_http' : Onboarding_onboarding_HTTP_IDD;
        port 'onboardingManagement_http' : Onboarding_onboardingManagement_HTTP_IDD;
        port 'sysMonitor_amqp' : Onboarding_sysMonitor_AMQP_IDD;
    }
    part def Onboarding :> Onboarding_SysDD {
        @System_Implementation;
    }
}
