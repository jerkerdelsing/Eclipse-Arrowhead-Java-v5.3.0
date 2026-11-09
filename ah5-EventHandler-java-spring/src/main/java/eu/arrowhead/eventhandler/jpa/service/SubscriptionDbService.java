/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.jpa.service;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.eventhandler.jpa.entity.Subscription;
import eu.arrowhead.eventhandler.jpa.repository.SubscriptionRepository;

@Service
public class SubscriptionDbService {

	@Autowired
	private SubscriptionRepository repository;

	@Transactional
	public Subscription save(final Subscription subscription) {
		return repository.saveAndFlush(subscription);
	}

	@Transactional(readOnly = true)
	public List<Subscription> findMatchingEventType(final String eventType) {
		return repository.findAllByEventType(eventType);
	}

	@Transactional(readOnly = true)
	public Subscription findById(final long id, final String origin) {
		return repository.findById(id).orElseThrow(() -> new DataNotFoundException("Subscription not found: " + id, origin));
	}

	@Transactional(readOnly = true)
	public Subscription findOwnedById(final long id, final String subscriberSystemName, final String origin) {
		return repository.findByIdAndSubscriberSystemName(id, subscriberSystemName)
				.orElseThrow(() -> new DataNotFoundException("Subscription not found: " + id, origin));
	}

	@Transactional(readOnly = true)
	public Page<Subscription> findAll(final Pageable pageable) {
		return repository.findAll(pageable);
	}

	@Transactional
	public void delete(final Subscription subscription) {
		repository.delete(subscription);
	}

	@Transactional
	public long deleteExpired(final ZonedDateTime now) {
		return repository.deleteAllByExpiresAtBefore(now);
	}
}
