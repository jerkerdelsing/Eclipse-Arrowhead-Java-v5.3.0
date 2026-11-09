/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.service;

import static eu.arrowhead.sysmonitor.service.Json.list;
import static eu.arrowhead.sysmonitor.service.Json.map;
import static eu.arrowhead.sysmonitor.service.Json.now;
import static eu.arrowhead.sysmonitor.service.Json.obj;
import static eu.arrowhead.sysmonitor.service.Json.str;
import static eu.arrowhead.sysmonitor.service.Json.strings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;
import jakarta.annotation.PostConstruct;

// Monitored providers: orchestrates the sysMonitor services this system may consume (generic_amqps, CERT_AUTH), derives the
// AMQP bindings from the subscriptions (or the default subscription) and applies them to the intake queue.
@Service
public class MonitoringService {

	//=================================================================================================
	// members

	private static final List<String> WILDCARD = List.of("*");
	private static final int BASE_ROUTING_KEY_PARTS = 4;
	private static final int DEFAULT_AMQPS_PORT = 5671;

	private final Logger logger = LogManager.getLogger(getClass());

	@Autowired
	private ArrowheadHttpService httpService;

	@Autowired
	private MonitorDbService store;

	@Autowired
	private AmqpIntake intake;

	@Value(SysMonitorConstants.$DEFAULT_SUBSCRIPTION_WD)
	private String defaultSubscription;

	@Value(SysMonitorConstants.$RETENTION_DAYS_WD)
	private double retentionDays;

	@Value(SysMonitorConstants.$CLOUD_WD)
	private String cloud;

	private volatile Map<String, Map<String, Object>> providers = new TreeMap<>();
	private volatile boolean discovered;
	private volatile String orchestrationError;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	public void validate() {
		if (!Set.of("all", "none").contains(defaultSubscription) || retentionDays <= 0) {
			throw new IllegalArgumentException("Invalid monitoring retention/default subscription");
		}
	}

	//-------------------------------------------------------------------------------------------------
	// one orchestration round; the result replaces the known providers
	public void orchestrate() throws Exception {
		final Map<String, Object> requirement = map("serviceDefinition", SysMonitorConstants.SERVICE_DEF_SYS_MONITOR,
				"versions", List.of(SysMonitorConstants.SERVICE_VERSION),
				"interfaceTemplateNames", List.of(SysMonitorConstants.INTERFACE_AMQPS),
				"securityPolicies", List.of(SysMonitorConstants.POLICY_CERT_AUTH));
		final Map<?, ?> result = httpService.consumeService(Constants.SERVICE_DEF_SERVICE_ORCHESTRATION, Constants.SERVICE_OP_ORCHESTRATION_PULL, Map.class,
				map("serviceRequirement", requirement, "orchestrationFlags", map()));
		providers = providers(obj(result));
		discovered = true;
		applySubscriptions();
		orchestrationError = providers.isEmpty() ? "Orchestration returned no authorized sysMonitor provider" : null;
	}

	//-------------------------------------------------------------------------------------------------
	public void orchestrationFailed() {
		orchestrationError = "Monitoring orchestration failed";
	}

	//-------------------------------------------------------------------------------------------------
	public void applyIfDiscovered() throws Exception {
		if (discovered) {
			applySubscriptions();
		}
	}

	//-------------------------------------------------------------------------------------------------
	public void applySubscriptions() throws Exception {
		final Map<String, Map<String, Object>> subscriptions = store.subscriptions();
		final Set<String> desired = new TreeSet<>();
		for (final Map.Entry<String, Map<String, Object>> entry : providers.entrySet()) {
			for (final String type : selection(entry.getKey(), subscriptions)) {
				desired.add(entry.getValue().get("baseRoutingKey") + "." + type);
			}
		}

		intake.apply(desired);
	}

	//-------------------------------------------------------------------------------------------------
	// observation types bound for a provider: ["*"], the offered subset of the subscribed types, or none
	public List<String> selection(final String provider, final Map<String, Map<String, Object>> subscriptions) {
		final Map<String, Object> subscription = subscriptions.get(provider);
		if (subscription == null) {
			return "all".equals(defaultSubscription) ? WILDCARD : List.of();
		}

		if (!Boolean.TRUE.equals(subscription.get("enabled"))) {
			return List.of();
		}

		final List<String> types = strings(subscription.get("observationTypes"));
		if (types.equals(WILDCARD)) {
			return types;
		}

		final Map<String, Object> info = providers.get(provider);
		final List<String> offered = info == null ? List.of() : strings(info.get("observationTypes"));
		return types.stream().filter(offered::contains).toList();
	}

	//-------------------------------------------------------------------------------------------------
	public List<Object> providersView() {
		final Map<String, Map<String, Object>> subscriptions = store.subscriptions();
		final List<Object> result = new ArrayList<>();
		for (final Map.Entry<String, Map<String, Object>> entry : providers.entrySet()) {
			final String provider = entry.getKey();
			final Map<String, Object> info = new LinkedHashMap<>(entry.getValue());
			final Map<String, Object> subscription = subscriptions.get(provider);
			final List<String> selected = selection(provider, subscriptions);
			info.put("subscribedTypes", selected.equals(WILDCARD) ? info.get("observationTypes") : selected);
			info.put("selection", subscription == null ? "default" : Boolean.TRUE.equals(subscription.get("enabled")) ? "subscription" : "subscription (disabled)");
			info.put("subscription", subscription);
			result.add(info);
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public List<Object> subscriptionsView() {
		final List<Object> result = new ArrayList<>();
		for (final Map.Entry<String, Map<String, Object>> entry : store.subscriptions().entrySet()) {
			final Map<String, Object> item = new LinkedHashMap<>(entry.getValue());
			final boolean found = providers.containsKey(entry.getKey());
			item.put("providerFound", found);
			final List<String> types = strings(item.get("observationTypes"));
			item.put("unknownTypes", types.equals(WILDCARD) || !found
					? List.of()
					: types.stream().filter(t -> !strings(providers.get(entry.getKey()).get("observationTypes")).contains(t)).toList());
			result.add(item);
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Map<String, Object>> getProviders() {
		return providers;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOrchestrationError() {
		return orchestrationError;
	}

	//-------------------------------------------------------------------------------------------------
	public String getDefaultSubscription() {
		return defaultSubscription;
	}

	//-------------------------------------------------------------------------------------------------
	public double getRetentionDays() {
		return retentionDays;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	// providers of an orchestration response whose generic_amqps/CERT_AUTH interface publishes under
	// <cloud>.sysmonitor.<providerName>; anything else is ignored
	Map<String, Map<String, Object>> providers(final Map<String, Object> response) {
		final Map<String, Map<String, Object>> found = new TreeMap<>();
		for (final Object raw : list(response.get("results"))) {
			final Map<String, Object> service = obj(raw);
			Map<String, Object> properties = null;
			for (final Object value : list(service.get("interfaces"))) {
				final Map<String, Object> intf = obj(value);
				if (str(intf.get("templateName")).equals(SysMonitorConstants.INTERFACE_AMQPS) && str(intf.get("policy")).equals(SysMonitorConstants.POLICY_CERT_AUTH)) {
					properties = obj(intf.get("properties"));
					break;
				}
			}

			if (properties == null) {
				continue;
			}

			final String provider = str(service.getOrDefault("providerName", obj(service.get("provider")).get("name")));
			final String base = str(properties.get("baseRoutingKey"));
			final String[] parts = base.split("\\.", -1);
			if (parts.length != BASE_ROUTING_KEY_PARTS || !parts[0].equals(cloud) || !parts[1].equals("sysmonitor") || !parts[2].equals(provider)) {
				logger.debug("Ignoring sysMonitor offer of {} with routing key {}", provider, base);
				continue;
			}

			Object types = obj(service.get("metadata")).get("observationTypes");
			if (types == null) {
				types = properties.get("operations");
			}

			final List<String> addresses = strings(properties.get("accessAddresses"));
			found.put(provider, map("providerSystem", provider,
					"serviceInstanceId", service.get("serviceInstanceId"),
					"baseRoutingKey", base,
					"exchange", properties.getOrDefault("exchange", SysMonitorConstants.EXCHANGE),
					"observationTypes", new ArrayList<>(new TreeSet<>(strings(types))),
					"broker", addresses.isEmpty() ? intake.getAddress() : addresses.getFirst(),
					"port", properties.getOrDefault("accessPort", DEFAULT_AMQPS_PORT),
					"orchestratedAt", now()));
		}

		return found;
	}
}
