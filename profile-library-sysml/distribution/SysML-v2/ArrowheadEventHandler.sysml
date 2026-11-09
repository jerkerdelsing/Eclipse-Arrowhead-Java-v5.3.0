library package ArrowheadEventHandler {
    private import ScalarValues::*;
    private import ArrowheadFoundation::*;
    private import ArrowheadProfile::*;
    port def EventHandler_eventHandlerManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "eventHandlerManagement"; }
        doc /* Service version 1.0.0. Source: ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-SysD.tex */
    }
    port def EventHandler_eventHandlerManagement_HTTP_IDD :> EventHandler_eventHandlerManagement_SD {
        @IDD { MicroserviceName = "eventHandlerManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-eventHandlerManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/eventhandler/management";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address eventhandler, port 8455 and base path /eventhandler/management. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
query-subscriptions & GET & /subscriptions \\ \hline
update-subscription & PUT & \texttt{/subscriptions/{subscriptionId}} \\ \hline
remove-subscription & DELETE & \texttt{/subscriptions/{subscriptionId}} \\ \hline
\end{longtable}

\newpage */
        doc /* query-subscriptions: GET /eventhandler/management/subscriptions?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}. Input: None; output: SubscriptionListResponse.  */
        out item 'query-subscriptionsResponse' : ArrowheadEventHandlerData::'SubscriptionListResponse'[0..1];
        doc /* update-subscription: PUT /eventhandler/management/subscriptions/\fparam{{subscriptionId}}. Input: SubscriptionRequest; output: SubscriptionResponse.  */
        in item 'update-subscriptionRequest' : ArrowheadEventHandlerData::'SubscriptionRequest'[0..1];
        out item 'update-subscriptionResponse' : ArrowheadEventHandlerData::'SubscriptionResponse'[0..1];
        doc /* remove-subscription: DELETE /eventhandler/management/subscriptions/\fparam{{subscriptionId}}. Input: String; output: None.  */
        in attribute 'remove-subscriptionRequest' : ScalarValues::String[0..1];
    }
    port def EventHandler_eventHandlerManagement_HTTP_IDD_Implementation :> EventHandler_eventHandlerManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'EventHandler_eventHandlerManagement_http_query-subscriptions_0' {
        doc /* Operation query-subscriptions. Wire method: GET; address: /eventhandler/management/subscriptions?page=\fparam{{page}}\&size=\fparam{{size}}\&direction=\fparam{{direction}}\&sortField=\fparam{{sortField}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/eventhandler/management/subscriptions?page=\\fparam{{page}}\\&size=\\fparam{{size}}\\&direction=\\fparam{{direction}}\\&sortField=\\fparam{{sortField}}"; requestType = "unspecified"; responseType = "SubscriptionListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadEventHandlerData::'SubscriptionListResponse'[0..1];
    }
    action def 'EventHandler_eventHandlerManagement_http_update-subscription_1' {
        doc /* Operation update-subscription. Wire method: PUT; address: /eventhandler/management/subscriptions/\fparam{{subscriptionId}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "PUT"; address = "/eventhandler/management/subscriptions/\\fparam{{subscriptionId}}"; requestType = "SubscriptionRequest"; responseType = "SubscriptionResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadEventHandlerData::'SubscriptionRequest'[0..1];
        out item 'response' : ArrowheadEventHandlerData::'SubscriptionResponse'[0..1];
    }
    action def 'EventHandler_eventHandlerManagement_http_remove-subscription_2' {
        doc /* Operation remove-subscription. Wire method: DELETE; address: /eventhandler/management/subscriptions/\fparam{{subscriptionId}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/eventhandler/management/subscriptions/\\fparam{{subscriptionId}}"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    port def EventHandler_eventPublish_SD :> ArrowheadService {
        @SD { MicroserviceName = "eventPublish"; }
        doc /* Service version 1.0.0. Source: ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-SysD.tex */
    }
    port def EventHandler_eventPublish_HTTP_IDD :> EventHandler_eventPublish_SD {
        @IDD { MicroserviceName = "eventPublish"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-eventPublish-HTTP-IDD.tex"; }
        attribute basePath : String = "/eventhandler/event-publish";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address eventhandler, port 8455 and base path /eventhandler/event-publish. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
publish & POST & /publish \\ \hline
\end{longtable}

\newpage */
        doc /* publish: POST /eventhandler/event-publish/publish. Input: EventPublishRequest; output: EventPublishResult.  */
        in item 'publishRequest' : ArrowheadEventHandlerData::'EventPublishRequest'[0..1];
        out item 'publishResponse' : ArrowheadEventHandlerData::'EventPublishResult'[0..1];
    }
    port def EventHandler_eventPublish_HTTP_IDD_Implementation :> EventHandler_eventPublish_HTTP_IDD { @IDD_Implementation; }
    action def 'EventHandler_eventPublish_http_publish_0' {
        doc /* Operation publish. Wire method: POST; address: /eventhandler/event-publish/publish. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/eventhandler/event-publish/publish"; requestType = "EventPublishRequest"; responseType = "EventPublishResult"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadEventHandlerData::'EventPublishRequest'[0..1];
        out item 'response' : ArrowheadEventHandlerData::'EventPublishResult'[0..1];
    }
    port def EventHandler_eventSubscription_SD :> ArrowheadService {
        @SD { MicroserviceName = "eventSubscription"; }
        doc /* Service version 1.0.0. Source: ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-SysD.tex */
    }
    port def EventHandler_eventSubscription_HTTP_IDD :> EventHandler_eventSubscription_SD {
        @IDD { MicroserviceName = "eventSubscription"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-eventSubscription-HTTP-IDD.tex"; }
        attribute basePath : String = "/eventhandler/event-subscription";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address eventhandler, port 8455 and base path /eventhandler/event-subscription. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
subscribe & POST & /subscribe \\ \hline
unsubscribe & DELETE & \texttt{/unsubscribe/{subscriptionId}} \\ \hline
\end{longtable}

\newpage */
        doc /* subscribe: POST /eventhandler/event-subscription/subscribe. Input: SubscriptionRequest; output: SubscriptionResponse.  */
        in item 'subscribeRequest' : ArrowheadEventHandlerData::'SubscriptionRequest'[0..1];
        out item 'subscribeResponse' : ArrowheadEventHandlerData::'SubscriptionResponse'[0..1];
        doc /* unsubscribe: DELETE /eventhandler/event-subscription/unsubscribe/\fparam{{subscriptionId}}. Input: String; output: None.  */
        in attribute 'unsubscribeRequest' : ScalarValues::String[0..1];
    }
    port def EventHandler_eventSubscription_HTTP_IDD_Implementation :> EventHandler_eventSubscription_HTTP_IDD { @IDD_Implementation; }
    action def 'EventHandler_eventSubscription_http_subscribe_0' {
        doc /* Operation subscribe. Wire method: POST; address: /eventhandler/event-subscription/subscribe. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/eventhandler/event-subscription/subscribe"; requestType = "SubscriptionRequest"; responseType = "SubscriptionResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadEventHandlerData::'SubscriptionRequest'[0..1];
        out item 'response' : ArrowheadEventHandlerData::'SubscriptionResponse'[0..1];
    }
    action def 'EventHandler_eventSubscription_http_unsubscribe_1' {
        doc /* Operation unsubscribe. Wire method: DELETE; address: /eventhandler/event-subscription/unsubscribe/\fparam{{subscriptionId}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "DELETE"; address = "/eventhandler/event-subscription/unsubscribe/\\fparam{{subscriptionId}}"; requestType = "String"; responseType = "unspecified"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in attribute 'request' : ScalarValues::String[0..1];
    }
    port def EventHandler_generalManagement_SD :> ArrowheadService {
        @SD { MicroserviceName = "generalManagement"; }
        doc /* Service version 1.0.0. Source: ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-SysD.tex */
    }
    port def EventHandler_generalManagement_HTTP_IDD :> EventHandler_generalManagement_SD {
        @IDD { MicroserviceName = "generalManagement"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'http'; }
        @SourceTrace { path = "ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-generalManagement-HTTP-IDD.tex"; }
        attribute basePath : String = "/eventhandler/general/mgmt";
        attribute templateName : String = "generic_https";
        attribute policy : String = "CERT_AUTH";
        doc /* Registration (source documentation): The service is registered in the ServiceRegistry with the interface template generic_https, policy CERT_AUTH, access address eventhandler, port 8455 and base path /eventhandler/general/mgmt. The operations property of the interface lists the operations below with their method and path relative to the base path (path parameters are placeholders such as \texttt{{id}} that the consumer fills in):

\noindent\begin{longtable}{| p{5.2cm} | p{1.6cm} | p{8.4cm} |} \hline
\rowcolor{gray!33} Operation & Method & Path \\ \hline \endhead
get-log & POST & /logs \\ \hline
get-config & GET & /get-config \\ \hline
\end{longtable}

\newpage */
        doc /* get-log: POST /eventhandler/general/mgmt/logs. Input: LogRequest; output: LogEntryListResponse.  */
        in item 'get-logRequest' : ArrowheadEventHandlerData::'LogRequest'[0..1];
        out item 'get-logResponse' : ArrowheadEventHandlerData::'LogEntryListResponse'[0..1];
        doc /* get-config: GET /eventhandler/general/mgmt/get-config?keys=\fparam{{keys}}. Input: None; output: KeyValues.  */
        out item 'get-configResponse' : ArrowheadEventHandlerData::'KeyValues'[0..1];
    }
    port def EventHandler_generalManagement_HTTP_IDD_Implementation :> EventHandler_generalManagement_HTTP_IDD { @IDD_Implementation; }
    action def 'EventHandler_generalManagement_http_get-log_0' {
        doc /* Operation get-log. Wire method: POST; address: /eventhandler/general/mgmt/logs. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "POST"; address = "/eventhandler/general/mgmt/logs"; requestType = "LogRequest"; responseType = "LogEntryListResponse"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        in item 'request' : ArrowheadEventHandlerData::'LogRequest'[0..1];
        out item 'response' : ArrowheadEventHandlerData::'LogEntryListResponse'[0..1];
    }
    action def 'EventHandler_generalManagement_http_get-config_1' {
        doc /* Operation get-config. Wire method: GET; address: /eventhandler/general/mgmt/get-config?keys=\fparam{{keys}}. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "http"; method = "GET"; address = "/eventhandler/general/mgmt/get-config?keys=\\fparam{{keys}}"; requestType = "unspecified"; responseType = "KeyValues"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadEventHandlerData::'KeyValues'[0..1];
    }
    port def EventHandler_sysMonitor_SD :> ArrowheadService {
        @SD { MicroserviceName = "sysMonitor"; }
        doc /* Service version 1.0.0. Source: ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-SysD.tex */
    }
    port def EventHandler_sysMonitor_AMQP_IDD :> EventHandler_sysMonitor_SD {
        @IDD { MicroserviceName = "sysMonitor"; Protocol = AFPVNDatatypes::ApplicationProtocolKind::'amqp'; }
        @SourceTrace { path = "ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-sysMonitor-AMQP-IDD.tex"; }
        attribute templateName : String = "generic_amqps";
        attribute protocol : String = "amqps";
        attribute policy : String = "CERT_AUTH";
        attribute 'accessPort' : Integer = 5671;
        attribute 'exchange' : String = "arrowhead";
        attribute 'baseRoutingKey' : String = "TestCloud.sysmonitor.EventHandler.EventHandler";
        doc /* Registration (source documentation): With sysmonitor.enabled=true, EventHandler registers the service at start-up with interface template generic_amqps (protocol amqps, policy CERT_AUTH; generic_amqp without TLS) and the following properties and metadata:

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
        "baseRoutingKey": "TestCloud.sysmonitor.EventHandler.EventHandler",
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
        doc /* echo: PUBLISH <Cloud>.sysmonitor.EventHandler.EventHandler.echo. Input: None; output: Observation.  */
        out item 'echoResponse' : ArrowheadEventHandlerData::'Observation'[0..1];
        doc /* power: PUBLISH <Cloud>.sysmonitor.EventHandler.EventHandler.power. Input: None; output: Observation.  */
        out item 'powerResponse' : ArrowheadEventHandlerData::'Observation'[0..1];
        doc /* status: PUBLISH <Cloud>.sysmonitor.EventHandler.EventHandler.status. Input: None; output: Observation.  */
        out item 'statusResponse' : ArrowheadEventHandlerData::'Observation'[0..1];
    }
    port def EventHandler_sysMonitor_AMQP_IDD_Implementation :> EventHandler_sysMonitor_AMQP_IDD { @IDD_Implementation; }
    action def 'EventHandler_sysMonitor_amqp_echo_0' {
        doc /* Operation echo. Wire method: PUBLISH; address: <Cloud>.sysmonitor.EventHandler.EventHandler.echo. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.EventHandler.EventHandler.echo"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadEventHandlerData::'Observation'[0..1];
    }
    action def 'EventHandler_sysMonitor_amqp_power_1' {
        doc /* Operation power. Wire method: PUBLISH; address: <Cloud>.sysmonitor.EventHandler.EventHandler.power. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.EventHandler.EventHandler.power"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadEventHandlerData::'Observation'[0..1];
    }
    action def 'EventHandler_sysMonitor_amqp_status_2' {
        doc /* Operation status. Wire method: PUBLISH; address: <Cloud>.sysmonitor.EventHandler.EventHandler.status. Query parameters: []. Path parameters: []. Status codes: [] */
        @WireOperation { protocol = "amqp"; method = "PUBLISH"; address = "<Cloud>.sysmonitor.EventHandler.EventHandler.status"; requestType = "unspecified"; responseType = "Observation"; queryParameters = "[]"; pathParameters = "[]"; statusCodes = "[]"; }
        out item 'response' : ArrowheadEventHandlerData::'Observation'[0..1];
    }
    part def EventHandler_SysD :> ArrowheadSystem {
        @SysD { MicrosystemName = "EventHandler"; FunctionalProperties = "This document describes the EventHandler support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/EventHandler and deployed in the Arrowhead-test-cloud-java local cloud.\n  \\end{abstract}"; }
        @SourceTrace { path = "ah5-EventHandler-java-spring/documentation/v5.2.2/EventHandler-v5.2.2-SysD.tex"; }
        doc /* This document describes the EventHandler support system of the Eclipse Arrowhead 5.2.2 framework, as implemented in ah5-support-java-spring/EventHandler and deployed in the Arrowhead-test-cloud-java local cloud.
  \end{abstract} */
        port 'eventHandlerManagement_SD' : EventHandler_eventHandlerManagement_SD;
        port 'eventPublish_SD' : EventHandler_eventPublish_SD;
        port 'eventSubscription_SD' : EventHandler_eventSubscription_SD;
        port 'generalManagement_SD' : EventHandler_generalManagement_SD;
        port 'sysMonitor_SD' : EventHandler_sysMonitor_SD;
        doc /* Consumed dependencies from SysD: AMQP broker (SysMonitor), Authentication, ConsumerAuthorization, ServiceRegistry, Subscribers. These are external requirements, not assumed co-deployments. */
    }
    part def EventHandler_SysDD :> EventHandler_SysD {
        @SysDD;
        port 'eventHandlerManagement_http' : EventHandler_eventHandlerManagement_HTTP_IDD;
        port 'eventPublish_http' : EventHandler_eventPublish_HTTP_IDD;
        port 'eventSubscription_http' : EventHandler_eventSubscription_HTTP_IDD;
        port 'generalManagement_http' : EventHandler_generalManagement_HTTP_IDD;
        port 'sysMonitor_amqp' : EventHandler_sysMonitor_AMQP_IDD;
    }
    part def EventHandler :> EventHandler_SysDD {
        @System_Implementation;
    }
}
