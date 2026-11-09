/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import eu.arrowhead.certificateauthority.jpa.entity.IssuedCertificate;
import eu.arrowhead.certificateauthority.jpa.entity.TrustedKey;

// JSON views of the stored rows. Field names and ISO-8601 time strings are those of the native 5.2.2 responses.
public final class CertificateViews {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public static Map<String, Object> certificate(final IssuedCertificate row) {
		final Map<String, Object> view = new LinkedHashMap<>();
		view.put("id", row.getId());
		view.put("serial_number", row.getSerialNumber());
		view.put("common_name", row.getCommonName());
		view.put("created_by", row.getCreatedBy());
		view.put("valid_from", time(row.getValidFrom()));
		view.put("valid_until", time(row.getValidUntil()));
		view.put("revoked_at", time(row.getRevokedAt()));
		view.put("certificate_pem", row.getCertificatePem());

		return view;
	}

	//-------------------------------------------------------------------------------------------------
	public static Map<String, Object> key(final TrustedKey row) {
		final Map<String, Object> view = new LinkedHashMap<>();
		view.put("id", row.getId());
		view.put("public_key_pem", row.getPublicKeyPem());
		view.put("description", row.getDescription());
		view.put("valid_after", time(row.getValidAfter()));
		view.put("valid_before", time(row.getValidBefore()));
		view.put("created_at", time(row.getCreatedAt()));

		return view;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private static String time(final Instant value) {
		return value == null ? null : value.toString();
	}

	//-------------------------------------------------------------------------------------------------
	private CertificateViews() {
		throw new UnsupportedOperationException();
	}
}
