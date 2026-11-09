/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler.jpa.entity;

import java.time.ZonedDateTime;
import java.util.List;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_subscription", indexes = {
		@Index(name = "idx_event_subscription_type", columnList = "eventType"),
		@Index(name = "idx_event_subscription_subscriber", columnList = "subscriberSystemName")
})
public class Subscription extends ArrowheadEntity {

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "subscriberSystemName", "eventType", "startsAt", "expiresAt", "createdAt", "updatedAt");
	public static final String DEFAULT_SORT_FIELD = "createdAt";

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String subscriberSystemName;

	@Column(nullable = false, length = VARCHAR_SMALL)
	private String eventType;

	@Column(nullable = false, length = VARCHAR_LARGE)
	private String notifyUri;

	@Column(nullable = false)
	private boolean matchMetadata;

	@Column(nullable = true, columnDefinition = "TEXT")
	private String metadataFilter;

	@Column(nullable = true)
	private ZonedDateTime startsAt;

	@Column(nullable = true)
	private ZonedDateTime expiresAt;

	public Subscription() {
	}

	@SuppressWarnings("checkstyle:ParameterNumberCheck")
	public Subscription(
			final String subscriberSystemName,
			final String eventType,
			final String notifyUri,
			final boolean matchMetadata,
			final String metadataFilter,
			final ZonedDateTime startsAt,
			final ZonedDateTime expiresAt) {
		this.subscriberSystemName = subscriberSystemName;
		this.eventType = eventType;
		this.notifyUri = notifyUri;
		this.matchMetadata = matchMetadata;
		this.metadataFilter = metadataFilter;
		this.startsAt = startsAt;
		this.expiresAt = expiresAt;
	}

	public String getSubscriberSystemName() {
		return subscriberSystemName;
	}

	public void setSubscriberSystemName(final String subscriberSystemName) {
		this.subscriberSystemName = subscriberSystemName;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(final String eventType) {
		this.eventType = eventType;
	}

	public String getNotifyUri() {
		return notifyUri;
	}

	public void setNotifyUri(final String notifyUri) {
		this.notifyUri = notifyUri;
	}

	public boolean isMatchMetadata() {
		return matchMetadata;
	}

	public void setMatchMetadata(final boolean matchMetadata) {
		this.matchMetadata = matchMetadata;
	}

	public String getMetadataFilter() {
		return metadataFilter;
	}

	public void setMetadataFilter(final String metadataFilter) {
		this.metadataFilter = metadataFilter;
	}

	public ZonedDateTime getStartsAt() {
		return startsAt;
	}

	public void setStartsAt(final ZonedDateTime startsAt) {
		this.startsAt = startsAt;
	}

	public ZonedDateTime getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(final ZonedDateTime expiresAt) {
		this.expiresAt = expiresAt;
	}
}
