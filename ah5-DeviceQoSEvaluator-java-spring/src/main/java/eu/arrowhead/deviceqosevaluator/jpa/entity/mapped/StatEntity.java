/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.deviceqosevaluator.jpa.entity.mapped;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class StatEntity {

	//=================================================================================================
	// members

	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", "timestamp");
	public static final String DEFAULT_SORT_FIELD = "timestamp";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected long id;

	@Column(nullable = false)
	protected UUID uuid;

	@Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	protected ZonedDateTime timestamp;

	@Column(nullable = false)
	protected double minimum;

	@Column(nullable = false)
	protected double maximum;

	@Column(nullable = false)
	protected double mean;

	@Column(nullable = false)
	protected double median;

	@Column(nullable = false)
	protected double current;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public StatEntity() {
	}

	//-------------------------------------------------------------------------------------------------
	public StatEntity(final UUID uuid, final ZonedDateTime timestamp, final double minimum, final double maximum, final double mean, final double median, final double current) {
		this.uuid = uuid;
		this.timestamp = timestamp;
		this.minimum = minimum;
		this.maximum = maximum;
		this.mean = mean;
		this.median = median;
		this.current = current;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final StatEntity other = (StatEntity) obj;

		return id == other.id;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final long id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public UUID getUuid() {
		return uuid;
	}

	//-------------------------------------------------------------------------------------------------
	public void setUuid(final UUID uuid) {
		this.uuid = uuid;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getTimestamp() {
		return timestamp;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTimestamp(final ZonedDateTime timestamp) {
		this.timestamp = timestamp;
	}

	//-------------------------------------------------------------------------------------------------
	public double getMinimum() {
		return minimum;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMinimum(final double minimum) {
		this.minimum = minimum;
	}

	//-------------------------------------------------------------------------------------------------
	public double getMaximum() {
		return maximum;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMaximum(final double maximum) {
		this.maximum = maximum;
	}

	//-------------------------------------------------------------------------------------------------
	public double getMean() {
		return mean;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMean(final double mean) {
		this.mean = mean;
	}

	//-------------------------------------------------------------------------------------------------
	public double getMedian() {
		return median;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMedian(final double median) {
		this.median = median;
	}

	//-------------------------------------------------------------------------------------------------
	public double getCurrent() {
		return current;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCurrent(final double current) {
		this.current = current;
	}

}
