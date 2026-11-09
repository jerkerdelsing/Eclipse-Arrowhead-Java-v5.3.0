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
package eu.arrowhead.deviceqosevaluator.service.model;

import java.util.HashSet;
import java.util.Set;

import eu.arrowhead.deviceqosevaluator.enums.OidGroup;

public class SystemEvalModel {

	//=================================================================================================
	// members

	private final String name;
	private double score = 0;
	private Set<OidGroup> noStat = new HashSet<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public SystemEvalModel(final String name) {
		this.name = name;
	}

	//-------------------------------------------------------------------------------------------------
	public String getName() {
		return name;
	}

	//-------------------------------------------------------------------------------------------------
	public double getScore() {
		return score;
	}

	//-------------------------------------------------------------------------------------------------
	public void setScore(final double score) {
		this.score = score;
	}

	//-------------------------------------------------------------------------------------------------
	public Set<OidGroup> getNoStat() {
		return noStat;
	}

	//-------------------------------------------------------------------------------------------------
	public void addNoStat(final OidGroup group) {
		noStat.add(group);

	}

	//-------------------------------------------------------------------------------------------------
	public void setNoStat(final Set<OidGroup> noStat) {
		this.noStat = noStat;
	}
}
