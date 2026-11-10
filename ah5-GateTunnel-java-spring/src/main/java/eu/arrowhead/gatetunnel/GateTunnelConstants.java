/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel;

import java.util.List;
import java.util.Set;

public final class GateTunnelConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "GateTunnel";
	public static final String SYSTEM_VERSION = "5.2.2";

	// System info related
	public static final String VERSION_CONNECTION = "5.2.2";
	public static final String VERSION_KEY = "5.2.2";
	public static final String VERSION_MANAGEMENT = "5.2.2";
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";

	public static final String SERVICE_DEF_CONNECTION = "gateTunnelConnection";
	public static final String SERVICE_DEF_KEY = "gateTunnelKey";
	public static final String SERVICE_DEF_MANAGEMENT = "gateTunnelManagement";

	public static final String SERVICE_OP_CONNECT_PROVIDER = "connect-provider";
	public static final String SERVICE_OP_CONNECT_CONSUMER = "connect-consumer";
	public static final String SERVICE_OP_CLOSE_SESSIONS = "close-sessions";
	public static final String SERVICE_OP_OPEN_PROVIDER_SESSION = "open-provider-session";
	public static final String SERVICE_OP_OPEN_CONSUMER_SESSION = "open-consumer-session";
	public static final String SERVICE_OP_PUBLIC_KEY = "public-key";
	public static final String SERVICE_OP_SESSIONS = "sessions";
	public static final String SERVICE_OP_CLOSE = "close";
	public static final String SERVICE_OP_STATUS = "status";

	// JPA related
	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.gatetunnel.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.gatetunnel.jpa.repository";

	// HTTP API paths. Every operation is served under /gatetunnel, under the 4.x name /gateway and without a base path.
	public static final String HTTP_API_BASE_PATH = "/gatetunnel";
	public static final String HTTP_API_LEGACY_BASE_PATH = "/gateway";
	public static final String HTTP_API_OP_CONNECT_PROVIDER = "/connect_provider";
	public static final String HTTP_API_OP_CONNECT_CONSUMER = "/connect_consumer";
	public static final String HTTP_API_OP_CLOSE_SESSIONS = "/close_sessions";
	public static final String HTTP_API_OP_INTERCLOUD_PROVIDER = "/intercloud/provider";
	public static final String HTTP_API_OP_INTERCLOUD_CONSUMER = "/intercloud/consumer";
	public static final String HTTP_API_OP_PUBLIC_KEY = "/publickey";
	public static final String HTTP_API_OP_SESSIONS = "/mgmt/sessions";
	public static final String HTTP_API_OP_SESSIONS_CLOSE = "/mgmt/sessions/close";
	public static final String HTTP_API_OP_STATUS = "/mgmt/status";
	public static final String HTTP_API_OP_ECHO = "/echo";
	public static final String HTTP_API_OP_MONITOR_ECHO = "/monitor/echo";
	public static final String HTTP_API_OP_HEALTH = "/health";
	// served by every native 5.2.2 support system, kept for path parity
	public static final String HTTP_API_LEGACY_HEALTH = "/certificate-authority/health";

	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt/general";
	public static final String HTTP_API_OP_GENERAL_STATUS = "/status";

	// Request filter order: right after authentication (15), before the blacklist (20) and management (25) filters
	public static final int REQUEST_FILTER_ORDER_CALLER = 16;

	// Callers (security): certificate profiles and the systems allowed per operation group (unchanged from 5.2.2)
	public static final Set<String> CALLER_PROFILES = Set.of("sy", "op");
	public static final List<String> CONNECTION_CALLERS = List.of("Gatekeeper", "DynamicServiceOrchestration", "SimpleStoreServiceOrchestration", "LocalCloudManager", "Sysop");
	public static final List<String> MANAGEMENT_CALLERS = List.of("LocalCloudManager", "Sysop");
	public static final String INTERCLOUD_CALLER = "Gatekeeper";

	// Federation
	public static final String PROFILE_LEGACY_MTLS = "legacy-mtls";
	public static final String PROFILE_AH_GT_1 = "ah-gt-1";
	public static final String RELAY_ROLE = "gatetunnel";

	// Limits (unchanged from 5.2.2)
	public static final int MAX_BODY_BYTES = 1024 * 1024;
	public static final int MAX_RELAY_PAYLOAD_BYTES = 2 * 1024 * 1024;
	public static final int MAX_PROVIDER_SESSIONS = 256;
	public static final int MAX_PENDING_RELAY_REQUESTS = 128;
	public static final int MAX_TUNNEL_REQUESTS = 64;

	// Configuration keys
	public static final String FEDERATION_PROFILE = "gatetunnel.federation.profile";
	public static final String NEIGHBOR_CLOUDS = "gatetunnel.neighbor.clouds";
	public static final String PEER_ORGANIZATIONS = "gatetunnel.federation.peer.organizations";
	public static final String LOCAL_CLOUD = "gatetunnel.local.cloud";
	public static final String ORGANIZATION = "gatetunnel.organization";
	public static final String ADVERTISED_ADDRESS = "gatetunnel.advertised.address";
	public static final String SESSION_MAX_AGE_SECONDS = "gatetunnel.session.max.age.seconds";
	public static final String SESSION_IDLE_SECONDS = "gatetunnel.session.idle.seconds";
	public static final String SESSION_CLOCK_SKEW_SECONDS = "gatetunnel.session.clock.skew.seconds";
	public static final String INTERCLOUD_MIN_PORT = "gatetunnel.intercloud.min.port";
	public static final String INTERCLOUD_MAX_PORT = "gatetunnel.intercloud.max.port";
	public static final String CONTROL_MIN_PORT = "gatetunnel.control.min.port";
	public static final String CONTROL_MAX_PORT = "gatetunnel.control.max.port";
	public static final String RELAY_HOST = "gatetunnel.relay.host";
	public static final String RELAY_PORT = "gatetunnel.relay.port";
	public static final String RELAY_CHAIN_CERT = "gatetunnel.relay.chain.cert";
	public static final String RELAY_KEY = "gatetunnel.relay.key";
	public static final String RELAY_CA_CERT = "gatetunnel.relay.ca.cert";
	// AH-GT-1 peer revocation (SOS-4): comma-separated CRL files of the organization CA and every peer cloud CA
	public static final String RELAY_CRL_FILES = "gatetunnel.relay.crl.files";
	public static final String CRL_FILE = "arrowhead.ssl.crl-file";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private GateTunnelConstants() {
		throw new UnsupportedOperationException();
	}
}
