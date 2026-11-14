/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper;

import java.util.Set;

import org.springframework.core.Ordered;

public final class GatekeeperConstants {

	//=================================================================================================
	// members

	public static final String SYSTEM_NAME = "Gatekeeper";
	public static final String SYSTEM_VERSION = "5.4.0";
	public static final String IMPLEMENTATION = "java-spring";

	// service definitions and versions (unchanged from the native 5.2.2 registration)
	public static final String SERVICE_DEF_GLOBAL_SERVICE_DISCOVERY = "globalServiceDiscovery";
	public static final String SERVICE_DEF_INTER_CLOUD_NEGOTIATION = "interCloudNegotiation";
	public static final String SERVICE_DEF_GATEKEEPER_MANAGEMENT = "gatekeeperManagement";
	public static final String SERVICE_DEF_GATE_TUNNEL_CONNECTION = "gateTunnelConnection";
	public static final String VERSION_SERVICES = "5.2.2";
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";

	public static final String SERVICE_OP_QUERY = "query";
	public static final String SERVICE_OP_MULTI_QUERY = "multi-query";
	public static final String SERVICE_OP_NEGOTIATE = "negotiate";
	public static final String SERVICE_OP_CLOUDS = "clouds";
	public static final String SERVICE_OP_RELAYS = "relays";
	public static final String SERVICE_OP_STATUS = "status";
	public static final String SERVICE_OP_OPEN_PROVIDER_SESSION = "open-provider-session";
	public static final String SERVICE_OP_OPEN_CONSUMER_SESSION = "open-consumer-session";

	// JPA
	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.gatekeeper.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.gatekeeper.jpa.repository";

	// HTTP API paths
	public static final String HTTP_API_BASE_PATH = "/gatekeeper";
	public static final String HTTP_API_OP_INIT_GSD = "/init_gsd";
	public static final String HTTP_API_OP_INIT_MULTI_GSD = "/init_multi_gsd";
	public static final String HTTP_API_OP_INIT_ICN = "/init_icn";
	public static final String HTTP_API_OP_ECHO = "/echo";
	public static final String HTTP_API_OP_MONITOR_ECHO = "/monitor/echo";
	public static final String HTTP_API_OP_MGMT_CLOUDS = "/mgmt/clouds";
	public static final String HTTP_API_OP_MGMT_RELAYS = "/mgmt/relays";
	public static final String HTTP_API_OP_MGMT_STATUS = "/mgmt/status";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt/general";
	public static final String HTTP_API_OP_GENERAL_STATUS = "/status";
	public static final String HTTP_API_HEALTH = "/health";
	// served by every native 5.2.2 support system, kept for the token-free liveness contract
	public static final String HTTP_API_LEGACY_HEALTH = "/certificate-authority/health";
	public static final String MGMT_PATH_SEGMENT = "/mgmt/";

	// callers (the native allowlists)
	public static final String SYS_NAME_SYSOP = "Sysop";
	public static final String SYS_NAME_LOCAL_CLOUD_MANAGER = "LocalCloudManager";
	public static final Set<String> SERVICE_CALLERS = Set.of("DynamicServiceOrchestration", "SimpleStoreServiceOrchestration", SYS_NAME_LOCAL_CLOUD_MANAGER, SYSTEM_NAME, SYS_NAME_SYSOP);
	public static final Set<String> MANAGEMENT_CALLERS = Set.of(SYS_NAME_LOCAL_CLOUD_MANAGER, SYS_NAME_SYSOP);
	public static final Set<String> MAY_ACT_FOR_OTHERS = Set.of("DynamicServiceOrchestration", "SimpleStoreServiceOrchestration", SYS_NAME_LOCAL_CLOUD_MANAGER, SYS_NAME_SYSOP);
	public static final Set<String> CALLER_PROFILES = Set.of("sy", "op");

	// request filter order (authentication is 15, blacklist 20, management 25)
	public static final int REQUEST_FILTER_ORDER_LEGACY_ERROR_BODY = Ordered.HIGHEST_PRECEDENCE + 10;
	public static final int REQUEST_FILTER_ORDER_LIVENESS = 10;
	public static final int REQUEST_FILTER_ORDER_CALLER_PROFILE = 14;
	public static final int REQUEST_FILTER_ORDER_ACCESS = 30;

	// limits (unchanged)
	public static final int MAX_REQUEST_BYTES = 1024 * 1024;
	public static final int MAX_RELAY_PAYLOAD_BYTES = 2 * 1024 * 1024;

	// federation
	public static final String FEDERATION_PROFILE_LEGACY = "legacy-mtls";
	public static final String FEDERATION_PROFILE_AH_GT_1 = "ah-gt-1";
	public static final Set<String> FEDERATION_PROFILES = Set.of(FEDERATION_PROFILE_LEGACY, FEDERATION_PROFILE_AH_GT_1);
	public static final String RELAY_ROLE = "gatekeeper";

	// configuration keys
	public static final String LOCAL_CLOUD = "gatekeeper.local.cloud";
	public static final String ORGANIZATION = "gatekeeper.organization";
	public static final String FEDERATION_PROFILE = "gatekeeper.federation.profile";
	public static final String FEDERATION_PEER_ORGANIZATIONS = "gatekeeper.federation.peer-organizations";
	// SOS-7: neighbours of another organisation this cloud's consumers may use, "Cloud|Organisation", comma-separated
	public static final String FEDERATION_FOREIGN_TARGETS = "gatekeeper.federation.foreign-targets";
	public static final String RELAY_ENABLED = "gatekeeper.relay.enabled";
	public static final String RELAY_HOST = "gatekeeper.relay.host";
	public static final String RELAY_PORT = "gatekeeper.relay.port";
	public static final String RELAY_CHAIN_CERT = "gatekeeper.relay.chain-cert";
	public static final String RELAY_KEY = "gatekeeper.relay.key";
	public static final String RELAY_CA_CERT = "gatekeeper.relay.ca-cert";
	public static final String RELAY_CRL_FILE = "gatekeeper.relay.crl-file";
	public static final String MIGRATE_SQLITE_FILE = "gatekeeper.migrate.sqlite-file";
	public static final String MIGRATE_SQLITE_ARGUMENT = "migrate-sqlite";

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private GatekeeperConstants() {
		throw new UnsupportedOperationException();
	}
}
