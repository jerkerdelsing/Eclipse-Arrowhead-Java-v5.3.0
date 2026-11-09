/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A SenML record as defined by RFC 8428. The abbreviated JSON field names are
 * retained because they are mandated by SenML rather than Arrowhead metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SenMLRecordDTO(
		String bn,
		Double bt,
		String bu,
		Double bv,
		Double bs,
		Short bver,
		String n,
		String u,
		Double v,
		String vs,
		Boolean vb,
		String vd,
		Double s,
		Double t,
		Double ut) {

	public SenMLRecordDTO withBaseTime(final double baseTime) {
		return new SenMLRecordDTO(bn, baseTime, bu, bv, bs, bver, n, u, v, vs, vb, vd, s, t, ut);
	}
}
