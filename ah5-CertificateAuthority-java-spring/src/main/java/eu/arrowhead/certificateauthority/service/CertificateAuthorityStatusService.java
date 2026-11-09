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

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.arrowhead.certificateauthority.CertificateAuthorityConstants;
import eu.arrowhead.certificateauthority.jpa.repository.IssuedCertificateRepository;
import eu.arrowhead.certificateauthority.jpa.repository.TrustedKeyRepository;
import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.common.sysmonitor.SysMonitorHealthIndicator;

// The generalManagement status document, the token-authenticated liveness answer and the sysMonitor echo detail.
@Component
public class CertificateAuthorityStatusService implements SysMonitorHealthIndicator {

	//=================================================================================================
	// members

	private final IssuedCertificateRepository certificates;
	private final TrustedKeyRepository keys;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Autowired
	public CertificateAuthorityStatusService(final IssuedCertificateRepository certificates, final TrustedKeyRepository keys) {
		this.certificates = certificates;
		this.keys = keys;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> liveness() {
		final Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", "ok");
		result.put("system", CertificateAuthorityConstants.SYSTEM_NAME);

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> status() {
		final Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", "ok");
		result.put("system", CertificateAuthorityConstants.SYSTEM_NAME);
		result.put("version", CertificateAuthorityConstants.SYSTEM_VERSION);
		result.put("implementation", "java-spring");
		result.put("x509", true);
		result.put("issuedCertificates", certificates.count());
		result.put("revokedCertificates", certificates.countByRevokedAtIsNotNull());
		result.put("trustedKeys", keys.count());

		return result;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SysMonitorHealth health() {
		try {
			return SysMonitorHealth.alive("issuedCertificates=" + certificates.count() + ", revokedCertificates=" + certificates.countByRevokedAtIsNotNull() + ", trustedKeys=" + keys.count());
		} catch (final Exception ex) {
			return SysMonitorHealth.degraded("database unavailable");
		}
	}
}
