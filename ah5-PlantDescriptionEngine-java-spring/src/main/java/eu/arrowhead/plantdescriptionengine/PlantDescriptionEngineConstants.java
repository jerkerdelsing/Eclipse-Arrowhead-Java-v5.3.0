/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine;

public final class PlantDescriptionEngineConstants {

    public static final String SERVICE_DEF_DSO_PLANT_MANAGEMENT = "serviceOrchestrationPlantManagement";

	public static final String SYSTEM_NAME = "PlantDescriptionEngine";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.plantdescriptionengine.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.plantdescriptionengine.jpa.repository";

	// services (v4.6 pde-mgmt and plant-description-monitor)
	public static final String SERVICE_DEF_PLANT_DESCRIPTION_MANAGEMENT = "plantDescriptionManagement";
	public static final String SERVICE_DEF_PLANT_DESCRIPTION_MONITOR = "plantDescriptionMonitor";
	public static final String VERSION_PLANT_DESCRIPTION_MANAGEMENT = "1.0.0";
	public static final String VERSION_PLANT_DESCRIPTION_MONITOR = "1.0.0";
	public static final String VERSION_GENERAL_MANAGEMENT = "1.0.0";

	public static final String SERVICE_OP_QUERY = "query";
	public static final String SERVICE_OP_GET = "get";
	public static final String SERVICE_OP_CREATE = "create";
	public static final String SERVICE_OP_REPLACE = "replace";
	public static final String SERVICE_OP_UPDATE = "update";
	public static final String SERVICE_OP_REMOVE = "remove";
	public static final String SERVICE_OP_QUERY_PLANT_DESCRIPTIONS = "query-plant-descriptions";
	public static final String SERVICE_OP_GET_PLANT_DESCRIPTION = "get-plant-description";
	public static final String SERVICE_OP_QUERY_ALARMS = "query-alarms";
	public static final String SERVICE_OP_GET_ALARM = "get-alarm";
	public static final String SERVICE_OP_ACKNOWLEDGE_ALARM = "acknowledge-alarm";

	public static final String HTTP_API_BASE_PATH = "/plantdescriptionengine";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_API_MONITOR_PATH = HTTP_API_BASE_PATH + "/monitor";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_OP_PLANT_DESCRIPTIONS_PATH = "/plant-descriptions";
	public static final String HTTP_API_OP_PLANT_DESCRIPTION_PATH = HTTP_API_OP_PLANT_DESCRIPTIONS_PATH + "/{id}";
	public static final String HTTP_API_OP_ALARMS_PATH = "/alarms";
	public static final String HTTP_API_OP_ALARM_PATH = HTTP_API_OP_ALARMS_PATH + "/{id}";

	// consumed services
	public static final String SERVICE_DEF_SYS_MONITOR_HISTORY = "sysMonitorHistory";
	public static final String SERVICE_OP_HISTORY_LIST_SYSTEMS = "list-systems";
	public static final String SERVICE_OP_HISTORY_LATEST = "latest";

	// configuration
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":" + PlantDescriptionEngineDefaults.SERVICE_AUTHORIZATION_ENABLED_DEFAULT + "}";
	public static final String PLANT_DESCRIPTION_MAX_SIZE = "plant.description.max.size";
	public static final String $PLANT_DESCRIPTION_MAX_SIZE_WD = "${" + PLANT_DESCRIPTION_MAX_SIZE + ":" + PlantDescriptionEngineDefaults.PLANT_DESCRIPTION_MAX_SIZE_DEFAULT + "}";
	public static final String SYNC_INTERVAL = "plant.sync.interval";
	public static final String $SYNC_INTERVAL_WD = "${" + SYNC_INTERVAL + ":" + PlantDescriptionEngineDefaults.SYNC_INTERVAL_DEFAULT + "}";
	public static final String RULE_PRIORITY_DEFAULT = "plant.rule.default.priority";
	public static final String $RULE_PRIORITY_DEFAULT_WD = "${" + RULE_PRIORITY_DEFAULT + ":" + PlantDescriptionEngineDefaults.RULE_PRIORITY_DEFAULT_DEFAULT + "}";
	public static final String MONITORING_ENABLED = "plant.monitoring.enabled";
	public static final String $MONITORING_ENABLED_WD = "${" + MONITORING_ENABLED + ":" + PlantDescriptionEngineDefaults.MONITORING_ENABLED_DEFAULT + "}";
	public static final String MONITORING_STALE_AFTER = "plant.monitoring.stale.after.seconds";
	public static final String $MONITORING_STALE_AFTER_WD = "${" + MONITORING_STALE_AFTER + ":" + PlantDescriptionEngineDefaults.MONITORING_STALE_AFTER_DEFAULT + "}";

	public static final String ALARM_IGNORED_SYSTEMS = "plant.alarm.ignored.systems";
	public static final String $ALARM_IGNORED_SYSTEMS_WD = "${" + ALARM_IGNORED_SYSTEMS + ":" + PlantDescriptionEngineDefaults.ALARM_IGNORED_SYSTEMS_DEFAULT + "}";

	public static final int REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION = 30;
	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30_000;

	private PlantDescriptionEngineConstants() {
		throw new UnsupportedOperationException();
	}
}
