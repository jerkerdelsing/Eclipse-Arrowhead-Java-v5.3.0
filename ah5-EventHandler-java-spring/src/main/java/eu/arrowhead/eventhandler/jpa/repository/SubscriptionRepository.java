/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.jpa.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.eventhandler.jpa.entity.Subscription;

@Repository
public interface SubscriptionRepository extends RefreshableRepository<Subscription, Long> {

	List<Subscription> findAllByEventType(final String eventType);

	Optional<Subscription> findByIdAndSubscriberSystemName(final Long id, final String subscriberSystemName);

	long deleteAllByExpiresAtBefore(final ZonedDateTime now);
}
