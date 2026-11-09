/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.api.dto;

// epoch, epochMs, tz, dst and trusted keep the v4.6 TimeManagerTimeResponseDTO field names; the remaining fields are additive.
public record TimeResponseDTO(
		long epoch,
		long epochMs,
		String tz,
		boolean dst,
		boolean trusted,
		Long offsetMs,
		String timeSource,
		String lastCheckTime) {
}
