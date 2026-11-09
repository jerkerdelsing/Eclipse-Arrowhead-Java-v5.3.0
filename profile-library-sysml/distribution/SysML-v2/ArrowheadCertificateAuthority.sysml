library package ArrowheadCertificateAuthority {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def CertificateAuthority_certificateAuthorityManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "certificateAuthorityManagement"; }
        doc /* Service version 1.0.0. Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-SysD.tex */
    }
    port def CertificateAuthority_certificateAuthorityManagement_HTTP_IDD :> CertificateAuthority_certificateAuthorityManagement_SD {
        @IDD { MicroserviceName = "certificateAuthorityManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-certificateAuthorityManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/certificate-authority";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address certificate-authority, port 8448 and base path /certificate-authority. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
list-certificates & GET & /mgmt/certificates \\ \hline
revoke-certificate & DELETE & \texttt{/mgmt/certificates/{id}} \\ \hline
revoke-certificate-by-content & POST & /mgmt/revocations \\ \hline
list-keys & GET & /mgmt/keys \\ \hline
add-key & PUT & /mgmt/keys \\ \hline
delete-key & DELETE & \texttt{/mgmt/keys/{id}} \\ \hline
\end{longtable}

\newpage */
        doc /* list-certificates: GET /certificate-authority/mgmt/certificates. Input: None; output: Array<CertificateRecord>.  */
        out item 'list-certificatesResponse' : ArrowheadCertificateAuthorityData::'CertificateRecord'[0..*];
        doc /* revoke-certificate: DELETE /certificate-authority/mgmt/certificates/\fparam{{id}}. Input: String; output: RemovalResult.  */
        in attribute 'revoke-certificateRequest' : ScalarValues::String[0..1];
        out item 'revoke-certificateResponse' : ArrowheadCertificateAuthorityData::'RemovalResult'[0..1];
        doc /* revoke-certificate-by-content: POST /certificate-authority/mgmt/revocations. Input: RevocationRequest; output: RevocationResult.  */
        in item 'revoke-certificate-by-contentRequest' : ArrowheadCertificateAuthorityData::'RevocationRequest'[0..1];
        out item 'revoke-certificate-by-contentResponse' : ArrowheadCertificateAuthorityData::'RevocationResult'[0..1];
        doc /* list-keys: GET /certificate-authority/mgmt/keys. Input: None; output: Array<TrustedKeyRecord>.  */
        out item 'list-keysResponse' : ArrowheadCertificateAuthorityData::'TrustedKeyRecord'[0..*];
        doc /* add-key: PUT /certificate-authority/mgmt/keys. Input: TrustedKeyRequest; output: TrustedKeyResult.  */
        in item 'add-keyRequest' : ArrowheadCertificateAuthorityData::'TrustedKeyRequest'[0..1];
        out item 'add-keyResponse' : ArrowheadCertificateAuthorityData::'TrustedKeyResult'[0..1];
        doc /* delete-key: DELETE /certificate-authority/mgmt/keys/\fparam{{id}}. Input: String; output: RemovalResult.  */
        in attribute 'delete-keyRequest' : ScalarValues::String[0..1];
        out item 'delete-keyResponse' : ArrowheadCertificateAuthorityData::'RemovalResult'[0..1];
    }
    port def CertificateAuthority_certificateAuthorityManagement_HTTP_IDD_Implementation :> CertificateAuthority_certificateAuthorityManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'CertificateAuthority_certificateAuthorityManagement_http_list-certificates_0' {
        doc /* Operation list-certificates. Wire method: GET; address: /certificate-authority/mgmt/certificates. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/certificate-authority/mgmt/certificates"; requestType = "unspecified"; responseType = "Array<CertificateRecord>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCertificateAuthorityData::'CertificateRecord'[0..*];
    }
    action def 'CertificateAuthority_certificateAuthorityManagement_http_revoke-certificate_1' {
        doc /* Operation revoke-certificate. Wire method: DELETE; address: /certificate-authority/mgmt/certificates/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/certificate-authority/mgmt/certificates/\\fparam{{id}}"; requestType = "String"; responseType = "RemovalResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadCertificateAuthorityData::'RemovalResult'[0..1];
    }
    action def 'CertificateAuthority_certificateAuthorityManagement_http_revoke-certificate-by-content_2' {
        doc /* Operation revoke-certificate-by-content. Wire method: POST; address: /certificate-authority/mgmt/revocations. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/certificate-authority/mgmt/revocations"; requestType = "RevocationRequest"; responseType = "RevocationResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCertificateAuthorityData::'RevocationRequest'[0..1];
        out item 'response' : ArrowheadCertificateAuthorityData::'RevocationResult'[0..1];
    }
    action def 'CertificateAuthority_certificateAuthorityManagement_http_list-keys_3' {
        doc /* Operation list-keys. Wire method: GET; address: /certificate-authority/mgmt/keys. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/certificate-authority/mgmt/keys"; requestType = "unspecified"; responseType = "Array<TrustedKeyRecord>"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCertificateAuthorityData::'TrustedKeyRecord'[0..*];
    }
    action def 'CertificateAuthority_certificateAuthorityManagement_http_add-key_4' {
        doc /* Operation add-key. Wire method: PUT; address: /certificate-authority/mgmt/keys. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/certificate-authority/mgmt/keys"; requestType = "TrustedKeyRequest"; responseType = "TrustedKeyResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCertificateAuthorityData::'TrustedKeyRequest'[0..1];
        out item 'response' : ArrowheadCertificateAuthorityData::'TrustedKeyResult'[0..1];
    }
    action def 'CertificateAuthority_certificateAuthorityManagement_http_delete-key_5' {
        doc /* Operation delete-key. Wire method: DELETE; address: /certificate-authority/mgmt/keys/\fparam{{id}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/certificate-authority/mgmt/keys/\\fparam{{id}}"; requestType = "String"; responseType = "RemovalResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
        out item 'response' : ArrowheadCertificateAuthorityData::'RemovalResult'[0..1];
    }
    port def CertificateAuthority_certificateSigning_SD :> ArrowheadService {
        @SD { MicroserviceName = "certificateSigning"; }
        doc /* Service version 1.0.0. Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-SysD.tex */
    }
    port def CertificateAuthority_certificateSigning_HTTP_IDD :> CertificateAuthority_certificateSigning_SD {
        @IDD { MicroserviceName = "certificateSigning"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-certificateSigning-HTTP-IDD.tex"; }
        attribute basePath : String = "/certificate-authority";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address certificate-authority, port 8448 and base path /certificate-authority. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
sign & POST & /sign \\ \hline
\end{longtable}

\newpage */
        doc /* sign: POST /certificate-authority/sign. Input: \mref[CertificateSigningRequest]{CertificateSigningRequest}; output: CertificateSigningResult.  */
        in item 'signRequest' : JSONMessage[0..1];
        out item 'signResponse' : ArrowheadCertificateAuthorityData::'CertificateSigningResult'[0..1];
    }
    port def CertificateAuthority_certificateSigning_HTTP_IDD_Implementation :> CertificateAuthority_certificateSigning_HTTP_IDD { @IDD_Implementation; }
    action def 'CertificateAuthority_certificateSigning_http_sign_0' {
        doc /* Operation sign. Wire method: POST; address: /certificate-authority/sign. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/certificate-authority/sign"; requestType = "\\mref[CertificateSigningRequest]{CertificateSigningRequest}"; responseType = "CertificateSigningResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : JSONMessage[0..1];
        out item 'response' : ArrowheadCertificateAuthorityData::'CertificateSigningResult'[0..1];
    }
    port def CertificateAuthority_certificateValidation_SD :> ArrowheadService {
        @SD { MicroserviceName = "certificateValidation"; }
        doc /* Service version 1.0.0. Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-SysD.tex */
    }
    port def CertificateAuthority_certificateValidation_HTTP_IDD :> CertificateAuthority_certificateValidation_SD {
        @IDD { MicroserviceName = "certificateValidation"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-certificateValidation-HTTP-IDD.tex"; }
        attribute basePath : String = "/certificate-authority";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address certificate-authority, port 8448 and base path /certificate-authority. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
check-certificate & POST & /checkCertificate \\ \hline
check-trusted-key & POST & /checkTrustedKey \\ \hline
get-crl & GET & /crl \\ \hline
\end{longtable}

\newpage */
        doc /* check-certificate: POST /certificate-authority/checkCertificate. Input: CertificateCheckRequest; output: CertificateCheckResult.  */
        in item 'check-certificateRequest' : ArrowheadCertificateAuthorityData::'CertificateCheckRequest'[0..1];
        out item 'check-certificateResponse' : ArrowheadCertificateAuthorityData::'CertificateCheckResult'[0..1];
        doc /* check-trusted-key: POST /certificate-authority/checkTrustedKey. Input: TrustedKeyCheckRequest; output: TrustedKeyCheckResult.  */
        in item 'check-trusted-keyRequest' : ArrowheadCertificateAuthorityData::'TrustedKeyCheckRequest'[0..1];
        out item 'check-trusted-keyResponse' : ArrowheadCertificateAuthorityData::'TrustedKeyCheckResult'[0..1];
        doc /* get-crl: GET /certificate-authority/crl. Input: None; output: None.  */
    }
    port def CertificateAuthority_certificateValidation_HTTP_IDD_Implementation :> CertificateAuthority_certificateValidation_HTTP_IDD { @IDD_Implementation; }
    action def 'CertificateAuthority_certificateValidation_http_check-certificate_0' {
        doc /* Operation check-certificate. Wire method: POST; address: /certificate-authority/checkCertificate. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/certificate-authority/checkCertificate"; requestType = "CertificateCheckRequest"; responseType = "CertificateCheckResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCertificateAuthorityData::'CertificateCheckRequest'[0..1];
        out item 'response' : ArrowheadCertificateAuthorityData::'CertificateCheckResult'[0..1];
    }
    action def 'CertificateAuthority_certificateValidation_http_check-trusted-key_1' {
        doc /* Operation check-trusted-key. Wire method: POST; address: /certificate-authority/checkTrustedKey. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/certificate-authority/checkTrustedKey"; requestType = "TrustedKeyCheckRequest"; responseType = "TrustedKeyCheckResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadCertificateAuthorityData::'TrustedKeyCheckRequest'[0..1];
        out item 'response' : ArrowheadCertificateAuthorityData::'TrustedKeyCheckResult'[0..1];
    }
    action def 'CertificateAuthority_certificateValidation_http_get-crl_2' {
        doc /* Operation get-crl. Wire method: GET; address: /certificate-authority/crl. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/certificate-authority/crl"; requestType = "unspecified"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
    }
    port def CertificateAuthority_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-SysD.tex */
    }
    port def CertificateAuthority_sysMonitor_AMQP_IDD :> CertificateAuthority_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.CertificateAuthority.CertificateAuthority";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, CertificateAuthority registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.CertificateAuthority.CertificateAuthority",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.CertificateAuthority.CertificateAuthority.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadCertificateAuthorityData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.CertificateAuthority.CertificateAuthority.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadCertificateAuthorityData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.CertificateAuthority.CertificateAuthority.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadCertificateAuthorityData::'Observation'[0..1];
    }
    port def CertificateAuthority_sysMonitor_AMQP_IDD_Implementation :> CertificateAuthority_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'CertificateAuthority_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.CertificateAuthority.CertificateAuthority.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.CertificateAuthority.CertificateAuthority.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCertificateAuthorityData::'Observation'[0..1];
    }
    action def 'CertificateAuthority_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.CertificateAuthority.CertificateAuthority.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.CertificateAuthority.CertificateAuthority.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCertificateAuthorityData::'Observation'[0..1];
    }
    action def 'CertificateAuthority_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.CertificateAuthority.CertificateAuthority.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.CertificateAuthority.CertificateAuthority.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadCertificateAuthorityData::'Observation'[0..1];
    }
    part def CertificateAuthority_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "CertificateAuthority"; FunctionalProperties = "This document describes the CertificateAuthority support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-CertificateAuthority-java-spring with an independent local-cloud deployment definition in its own folder.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-CertificateAuthority-java-spring/documentation/v5.2.2/CertificateAuthority-v5.2.2-SysD.tex"; }
        doc /* This document describes the CertificateAuthority support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-CertificateAuthority-java-spring with an independent local-cloud deployment definition in its own folder.
  \end{abstract} */
        port 'certificateAuthorityManagement_SD' : CertificateAuthority_certificateAuthorityManagement_SD;
        port 'certificateSigning_SD' : CertificateAuthority_certificateSigning_SD;
        port 'certificateValidation_SD' : CertificateAuthority_certificateValidation_SD;
        port 'sysMonitor_SD' : CertificateAuthority_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, ServiceRegistry. These are external requirements, not assumed co-deployments. */
    }
    part def CertificateAuthority_SysDD :> CertificateAuthority_SysD {
        @SysDD;
        port 'certificateAuthorityManagement_http' : CertificateAuthority_certificateAuthorityManagement_HTTP_IDD;
        port 'certificateSigning_http' : CertificateAuthority_certificateSigning_HTTP_IDD;
        port 'certificateValidation_http' : CertificateAuthority_certificateValidation_HTTP_IDD;
        port 'sysMonitor_amqp' : CertificateAuthority_sysMonitor_AMQP_IDD;
    }
    part def CertificateAuthority :> CertificateAuthority_SysDD {
        @System_Implementation;
    }
}
