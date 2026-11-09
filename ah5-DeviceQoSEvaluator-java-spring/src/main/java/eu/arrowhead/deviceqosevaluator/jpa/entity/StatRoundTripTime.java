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
package eu.arrowhead.deviceqosevaluator.jpa.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import eu.arrowhead.deviceqosevaluator.jpa.entity.mapped.StatEntity;
import jakarta.persistence.Entity;

@Entity
public class StatRoundTripTime extends StatEntity {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public StatRoundTripTime() {
	}

	//-------------------------------------------------------------------------------------------------
	public StatRoundTripTime(final UUID uuid, final ZonedDateTime timestamp, final double minimum, final double maximum, final double mean, final double median, final double current) {
		super(uuid, timestamp, minimum, maximum, mean, median, current);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "StatRoundTripTime [id=" + id + ", uuid=" + uuid + ", timestamp=" + timestamp + ", minimum=" + minimum + ", maximum=" + maximum + ", mean=" + mean + ", median=" + median
				+ ", current=" + current + "]";
	}
}