package eu.arrowhead.aigym;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.eclipse.paho.client.mqttv3.*;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultSaslConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiGymRuntime {
    private final ArrowheadGateway arrowhead;
    private final ObjectMapper json;
    private final ModelStore models;
    private final Deque<Map<String,Object>> observations = new ConcurrentLinkedDeque<>();
    private volatile MqttClient mqtt;          // sysMonitorData subscription (MQTT broker)
    private Connection amqp;                  // own sysMonitor observations (AMQP broker)
    private Channel amqpChannel;
    private volatile boolean registered;
    private volatile String registrationError;
    private volatile String orchestrationError;
    private volatile String subscribedTopic;

    @Value("${server.port}") int port;
    @Value("${arrowhead.advertised-host}") String advertisedHost;
    @Value("${arrowhead.mqtt-host}") String mqttHost;
    @Value("${arrowhead.mqtt-port}") int mqttPort;
    @Value("${arrowhead.amqp-host}") String amqpHost;
    @Value("${arrowhead.amqp-port}") int amqpPort;
    @Value("${arrowhead.amqp-exchange}") String amqpExchange;

    AiGymRuntime(ArrowheadGateway arrowhead, ObjectMapper json, ModelStore models) {
        this.arrowhead = arrowhead; this.json = json; this.models = models;
    }

    public String monitoringBaseRoutingKey() { return arrowhead.cloud + ".sysmonitor." + arrowhead.systemName + ".aiGym"; }

    @PostConstruct void start() { Thread.ofVirtual().name("aigym-bootstrap").start(this::bootstrap); }

    private void bootstrap() {
        try {
            MqttClient client = new MqttClient("ssl://" + mqttHost + ":" + mqttPort, arrowhead.systemName + "-" + UUID.randomUUID(), null);
            MqttConnectOptions options = new MqttConnectOptions(); options.setSocketFactory(arrowhead.sslContext().getSocketFactory());
            options.setAutomaticReconnect(true); options.setCleanSession(false); options.setConnectionTimeout(10);
            client.setCallback(new MqttCallbackExtended() {
                public void connectComplete(boolean reconnect, String uri) { orchestrate(); }
                public void connectionLost(Throwable cause) { orchestrationError = cause == null ? "MQTT connection lost" : cause.getMessage(); }
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Map<String,Object> value = json.readValue(message.getPayload(), new TypeReference<>() {});
                    value = new LinkedHashMap<>(value); value.put("mqttTopic", topic); value.put("receivedByAIGym", Instant.now().toString());
                    observations.addFirst(value); while (observations.size() > 200) observations.removeLast();
                }
                public void deliveryComplete(IMqttDeliveryToken token) { }
            });
            client.connect(options); mqtt = client;
            register(); orchestrate();
        } catch (Exception error) { registrationError = error.getMessage(); }
    }

    @Scheduled(initialDelay = 15_000, fixedDelay = 30_000)
    void reconcile() {
        if (mqtt == null || !mqtt.isConnected()) { bootstrap(); return; }
        if (!registered) register();
        orchestrate();
    }

    @Scheduled(initialDelayString = "${arrowhead.status-interval-ms}", fixedDelayString = "${arrowhead.status-interval-ms}")
    void publishStatus() {
        publishObservation("echo", Map.of("status", "alive"), null, Map.of());
        publishObservation("power", Map.of("supplyType", "line", "state", "available"), "1", Map.of());
        publishObservation("status", Map.of("models", models.inventory(), "sysMonitorConnected", subscribedTopic != null), "1", Map.of());
    }

    void register() {
        try { arrowhead.register(advertisedHost, port, amqpHost, amqpPort, amqpExchange, monitoringBaseRoutingKey()); registered = true; registrationError = null; }
        catch (Exception error) { registered = false; registrationError = error.getMessage(); }
    }

    @SuppressWarnings("unchecked")
    synchronized void orchestrate() {
        try {
            Map<String,Object> response = arrowhead.orchestrateSysMonitorData();
            List<Map<String,Object>> results = (List<Map<String,Object>>) response.getOrDefault("results", List.of());
            if (results.isEmpty()) throw new IllegalStateException("No authorized sysMonitorData provider");
            List<Map<String,Object>> interfaces = (List<Map<String,Object>>) results.get(0).getOrDefault("interfaces", List.of());
            Map<String,Object> selected = interfaces.stream().filter(item -> "ssl".equals(item.get("protocol")) && "CERT_AUTH".equals(item.get("policy"))).findFirst().orElseThrow();
            Map<String,Object> properties = (Map<String,Object>) selected.get("properties");
            String topic = properties.get("baseTopic") + "/+/+";
            if (!topic.equals(subscribedTopic)) {
                if (subscribedTopic != null) mqtt.unsubscribe(subscribedTopic);
                mqtt.subscribe(topic, 1); subscribedTopic = topic;
            }
            orchestrationError = null;
        } catch (Exception error) { orchestrationError = error.getMessage(); }
    }

    public void publishObservation(String type, Object value, String unit, Map<String,Object> metadata) {
        try {
            Map<String,Object> payload = new LinkedHashMap<>();
            payload.put("schemaVersion", "1.0"); payload.put("observationId", UUID.randomUUID().toString()); payload.put("providerSystem", arrowhead.systemName);
            payload.put("serviceInstance", "aiGym"); payload.put("observationType", type); payload.put("sourceTime", Instant.now().toString());
            payload.put("value", value); payload.put("unit", unit); payload.put("quality", "good"); payload.put("metadata", metadata);
            publishAmqp(monitoringBaseRoutingKey() + "." + type, (String) payload.get("observationId"), json.writeValueAsBytes(payload));
        } catch (Exception error) { orchestrationError = "Monitoring publish failed: " + error.getMessage(); }
    }

    // SysMonitor-AMQP-IDD-5.2.2: persistent, broker-confirmed messages; the broker user is the certificate CN (SASL EXTERNAL).
    private synchronized void publishAmqp(String routingKey, String messageId, byte[] body) throws Exception {
        if (amqpChannel == null || !amqpChannel.isOpen()) {
            closeAmqp();
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(amqpHost); factory.setPort(amqpPort); factory.setAutomaticRecoveryEnabled(false);
            factory.useSslProtocol(arrowhead.sslContext()); factory.enableHostnameVerification();
            factory.setSaslConfig(DefaultSaslConfig.EXTERNAL);
            amqp = factory.newConnection(arrowhead.systemName + "-sysMonitor");
            amqpChannel = amqp.createChannel(); amqpChannel.confirmSelect();
        }
        try {
            amqpChannel.basicPublish(amqpExchange, routingKey, new AMQP.BasicProperties.Builder().contentType("application/json")
                    .deliveryMode(2).messageId(messageId).build(), body);
            amqpChannel.waitForConfirmsOrDie(5000);
        } catch (Exception error) { closeAmqp(); throw error; }
    }

    private synchronized void closeAmqp() {
        try { if (amqp != null) amqp.close(1000); } catch (Exception ignored) { }
        amqp = null; amqpChannel = null;
    }

    public List<Map<String,Object>> observations() { return List.copyOf(observations); }
    public Map<String,Object> status() {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("status", mqtt != null && mqtt.isConnected() && registered ? "running" : "degraded");
        result.put("system", arrowhead.systemName); result.put("arrowheadVersion", "5.2.2"); result.put("models", models.inventory());
        result.put("registration", Map.of("registered", registered, "error", registrationError == null ? "" : registrationError));
        result.put("mqtt", Map.of("connected", mqtt != null && mqtt.isConnected(), "broker", mqttHost + ":" + mqttPort));
        result.put("amqp", Map.of("broker", amqpHost + ":" + amqpPort, "exchange", amqpExchange, "routingKey", monitoringBaseRoutingKey()));
        result.put("sysMonitorData", Map.of("topic", subscribedTopic == null ? "" : subscribedTopic, "received", observations.size(), "error", orchestrationError == null ? "" : orchestrationError));
        result.put("security", "mutual TLS + Arrowhead identity token + ConsumerAuthorization"); return result;
    }
    @PreDestroy void stop() throws Exception { closeAmqp(); if (mqtt != null) { mqtt.disconnectForcibly(); mqtt.close(); } }
}
