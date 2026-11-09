/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine;

import eu.arrowhead.common.Defaults;

public final class PlantDescriptionEngineDefaults extends Defaults {

	public static final String SERVER_PORT_DEFAULT = "8432";
	public static final String SERVICE_AUTHORIZATION_ENABLED_DEFAULT = "false";
	public static final String PLANT_DESCRIPTION_MAX_SIZE_DEFAULT = "200000";
	public static final String SYNC_INTERVAL_DEFAULT = "15000";
	public static final String RULE_PRIORITY_DEFAULT_DEFAULT = "1";
	public static final String MONITORING_ENABLED_DEFAULT = "true";
	public static final String MONITORING_STALE_AFTER_DEFAULT = "120";

	// framework systems that never appear in a plant description and so raise no SYSTEM_NOT_IN_DESCRIPTION alarm
	public static final String ALARM_IGNORED_SYSTEMS_DEFAULT = "ServiceRegistry,DynamicServiceOrchestration,SimpleStoreServiceOrchestration,ConsumerAuthorization,Authentication,"
			+ "Blacklist,DeviceQoSEvaluator,TranslationManager,SysMonitor,LocalCloudManager,PlantDescriptionEngine";

	private PlantDescriptionEngineDefaults() {
		throw new UnsupportedOperationException();
	}
}
