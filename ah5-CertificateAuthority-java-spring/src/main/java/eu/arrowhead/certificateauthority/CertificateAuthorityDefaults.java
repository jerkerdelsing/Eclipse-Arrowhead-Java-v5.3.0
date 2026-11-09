/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.certificateauthority;

import eu.arrowhead.common.Defaults;

public final class CertificateAuthorityDefaults extends Defaults {

	//=================================================================================================
	// members

	public static final String SERVER_PORT_DEFAULT = "8448";
	public static final String CA_CERTIFICATE_FILE_DEFAULT = "/certs/ca.crt";
	public static final String CA_KEY_FILE_DEFAULT = "/certs/ca.key";
	// an empty directory disables publication of the CRL file
	public static final String CRL_DIR_DEFAULT = "/crl";
	public static final String CRL_VALIDITY_HOURS_DEFAULT = "168";
	public static final String CRL_REFRESH_SECONDS_DEFAULT = "3600";
	public static final String CERT_VALIDITY_DAYS_DEFAULT = "365";
	public static final String ONBOARDING_CERT_VALIDITY_HOURS_DEFAULT = "24";
	public static final String RESERVED_NAMES_DEFAULT = "ServiceRegistry,Authentication,ConsumerAuthorization,DynamicServiceOrchestration,"
			+ "SimpleStoreServiceOrchestration,Blacklist,TranslationManager,DeviceQoSEvaluator,Onboarding,CertificateAuthority,DataManager,TimeManager,"
			+ "EventHandler,PlantDescriptionEngine,Configuration,Choreographer,SysMonitor,Gatekeeper,GateTunnel,AIGym,LocalCloudManager,"
			+ "LocalCloudConfigurator,SoSConfigurator,Sysop,RandomDataProducer,SmokeClient,Relay";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private CertificateAuthorityDefaults() {
		throw new UnsupportedOperationException();
	}
}
