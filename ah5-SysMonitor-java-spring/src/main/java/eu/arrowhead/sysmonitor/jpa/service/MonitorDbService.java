/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.jpa.service;

import static eu.arrowhead.sysmonitor.service.Json.epoch;
import static eu.arrowhead.sysmonitor.service.Json.json;
import static eu.arrowhead.sysmonitor.service.Json.map;
import static eu.arrowhead.sysmonitor.service.Json.now;
import static eu.arrowhead.sysmonitor.service.Json.parse;
import static eu.arrowhead.sysmonitor.service.Json.str;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.jpa.entity.AmqpBinding;
import eu.arrowhead.sysmonitor.jpa.entity.Audit;
import eu.arrowhead.sysmonitor.jpa.entity.Observation;
import eu.arrowhead.sysmonitor.jpa.entity.Outbox;
import eu.arrowhead.sysmonitor.jpa.entity.Subscription;
import eu.arrowhead.sysmonitor.jpa.repository.AmqpBindingRepository;
import eu.arrowhead.sysmonitor.jpa.repository.AuditRepository;
import eu.arrowhead.sysmonitor.jpa.repository.ObservationRepository;
import eu.arrowhead.sysmonitor.jpa.repository.OutboxRepository;
import eu.arrowhead.sysmonitor.jpa.repository.SubscriptionRepository;
import eu.arrowhead.sysmonitor.service.SubscriptionConflictException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

// The SysMonitor store: subscriptions, AMQP bindings, the MQTT outbox, the observation history and the audit trail.
// Behaviour of the native MonitorStore (SQLite) on the private MySQL schema ah_sysmonitor.
@Service
public class MonitorDbService {

	//=================================================================================================
	// members

	private static final String PROVIDER_PATTERN = "[A-Za-z][A-Za-z0-9_-]*";
	private static final double SECONDS_PER_DAY = 86400;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private AmqpBindingRepository bindingRepository;

	@Autowired
	private OutboxRepository outboxRepository;

	@Autowired
	private ObservationRepository observationRepository;

	@Autowired
	private AuditRepository auditRepository;

	@PersistenceContext
	private EntityManager entityManager;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	// providerSystem -> subscription view, ordered by provider
	@Transactional(readOnly = true)
	public Map<String, Map<String, Object>> subscriptions() {
		final Map<String, Map<String, Object>> result = new TreeMap<>();
		for (final Subscription subscription : subscriptionRepository.findAllByOrderByProviderSystemAsc()) {
			result.put(subscription.getProviderSystem(), view(subscription));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	// creates or replaces a subscription; expected is the If-Match revision (null: unconditional)
	@Transactional
	public Map<String, Object> put(final String provider, final List<String> types, final boolean enabled, final String actor, final Integer expected) {
		if (provider == null || !provider.matches(PROVIDER_PATTERN) || provider.length() > Subscription.NAME_LENGTH) {
			throw new IllegalArgumentException("Valid providerSystem required");
		}

		if (types.isEmpty() || types.stream().anyMatch(t -> t.isBlank() || t.contains(".") || t.contains("#") || t.contains("/") || t.equals("*") && !types.equals(List.of("*")))) {
			throw new IllegalArgumentException("Valid observationTypes or [*] required");
		}

		final String typesJson = json(new TreeSet<>(types));
		if (typesJson.length() > Subscription.TYPES_LENGTH) {
			throw new IllegalArgumentException("Valid observationTypes or [*] required");
		}

		final Optional<Subscription> existing = subscriptionRepository.findForUpdate(provider);
		final int revision = existing.map(Subscription::getRevision).orElse(0);
		if (expected != null && (existing.isEmpty() || expected != revision)) {
			throw new SubscriptionConflictException("Subscription revision conflict");
		}

		final Subscription subscription = existing.orElseGet(() -> new Subscription(provider));
		subscription.setObservationTypes(typesJson);
		subscription.setEnabled(enabled);
		subscription.setRevision(revision + 1);
		subscription.setUpdatedAt(now());
		subscription.setUpdatedBy(actor);

		return view(subscriptionRepository.saveAndFlush(subscription));
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional
	public boolean delete(final String provider) {
		if (!subscriptionRepository.existsById(provider)) {
			return false;
		}

		subscriptionRepository.deleteById(provider);
		return true;
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional
	public void audit(final String actor, final String operation, final String target, final String outcome) {
		auditRepository.save(new Audit(now(), actor, operation, truncate(target), truncate(outcome)));
	}

	//-------------------------------------------------------------------------------------------------
	// stores a normalised observation and queues it for MQTT; false when the observationId is already known (duplicate)
	@Transactional
	public boolean enqueue(final Map<String, Object> observation) {
		final String id = str(observation.get("observationId"));
		if (observationRepository.existsById(id)) {
			return false;
		}

		final String payload = json(observation);
		final String sourceTime = str(observation.get("sourceTime"));
		final String receivedTime = str(observation.get("receivedTime"));
		observationRepository.save(new Observation(id,
				str(observation.get("providerSystem")),
				str(observation.get("serviceInstance")),
				str(observation.get("observationType")),
				sourceTime,
				epoch(sourceTime),
				receivedTime,
				epoch(receivedTime),
				nullable(observation.get("quality")),
				nullable(observation.get("unit")),
				payload));
		if (!outboxRepository.existsById(id)) {
			outboxRepository.save(new Outbox(id, payload, now()));
		}

		return true;
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(readOnly = true)
	public List<Outbox> pending() {
		return outboxRepository.findPending(System.currentTimeMillis() / 1000.0, PageRequest.of(0, SysMonitorConstants.OUTBOX_BATCH));
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional
	public void published(final String id) {
		outboxRepository.deleteById(id);
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional
	public void failed(final String id) {
		outboxRepository.findById(id).ifPresent(row -> {
			row.failed("MQTT publication failed", System.currentTimeMillis() / 1000.0 + SysMonitorConstants.OUTBOX_RETRY_SECONDS);
			outboxRepository.save(row);
		});
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(readOnly = true)
	public Map<String, Object> counts() {
		return map("pending", outboxRepository.countByMqttPublishedFalse(), "stored", observationRepository.count());
	}

	//-------------------------------------------------------------------------------------------------
	// removes observations received more than retentionDays ago
	@Transactional
	public int prune(final double retentionDays) {
		return observationRepository.deleteReceivedBefore(System.currentTimeMillis() / 1000.0 - retentionDays * SECONDS_PER_DAY);
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(readOnly = true)
	public List<Map<String, Object>> systems() {
		final List<Map<String, Object>> result = new ArrayList<>();
		for (final Object[] row : observationRepository.systems()) {
			result.add(map("systemName", row[0], "observationCount", row[1], "firstObservationTime", row[2], "lastObservationTime", row[3]));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	// newest first; types empty: all types; from/to epoch seconds of the source time, inclusive
	@Transactional(readOnly = true)
	public List<Object> history(final String system, final List<String> types, final Double from, final Double to, final int count) {
		final StringBuilder jpql = new StringBuilder("SELECT o.payload FROM Observation o WHERE o.providerSystem = :system");
		if (!types.isEmpty()) {
			jpql.append(" AND o.observationType IN :types");
		}

		if (from != null) {
			jpql.append(" AND o.sourceEpoch >= :from");
		}

		if (to != null) {
			jpql.append(" AND o.sourceEpoch <= :to");
		}

		jpql.append(" ORDER BY o.sourceEpoch DESC");
		final TypedQuery<String> query = entityManager.createQuery(jpql.toString(), String.class).setParameter("system", system).setMaxResults(count);
		if (!types.isEmpty()) {
			query.setParameter("types", types);
		}

		if (from != null) {
			query.setParameter("from", from);
		}

		if (to != null) {
			query.setParameter("to", to);
		}

		return query.getResultList().stream().map(payload -> parse(payload)).toList();
	}

	//-------------------------------------------------------------------------------------------------
	// observationType -> newest observation
	@Transactional(readOnly = true)
	public Map<String, Object> latest(final String system) {
		final Map<String, Object> result = new TreeMap<>();
		for (final Observation observation : observationRepository.latest(system)) {
			result.put(observation.getObservationType(), parse(observation.getPayload()));
		}

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(readOnly = true)
	public Set<String> bindings() {
		final Set<String> result = new TreeSet<>();
		bindingRepository.findAll().forEach(binding -> result.add(binding.getBindingKey()));
		return result;
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional
	public void bindings(final Set<String> bindings) {
		bindingRepository.deleteAllInBatch();
		bindingRepository.saveAll(bindings.stream().map(AmqpBinding::new).toList());
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	static Map<String, Object> view(final Subscription subscription) {
		return map("providerSystem", subscription.getProviderSystem(),
				"observationTypes", parse(subscription.getObservationTypes()),
				"enabled", subscription.isEnabled(),
				"revision", subscription.getRevision(),
				"updatedAt", subscription.getUpdatedAt(),
				"updatedBy", subscription.getUpdatedBy());
	}

	//-------------------------------------------------------------------------------------------------
	private static String nullable(final Object value) {
		if (value == null) {
			return null;
		}

		final String text = value.toString();
		return text.length() > Observation.SHORT_LENGTH ? text.substring(0, Observation.SHORT_LENGTH) : text;
	}

	//-------------------------------------------------------------------------------------------------
	private static String truncate(final String text) {
		final String value = text == null ? "" : text;
		return value.length() > Audit.TEXT_LENGTH ? value.substring(0, Audit.TEXT_LENGTH) : value;
	}
}
