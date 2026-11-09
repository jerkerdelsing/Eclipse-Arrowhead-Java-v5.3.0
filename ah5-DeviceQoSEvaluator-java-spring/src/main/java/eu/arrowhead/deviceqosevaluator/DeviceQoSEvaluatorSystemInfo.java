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
package eu.arrowhead.deviceqosevaluator;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.model.HttpInterfaceModel;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.model.SystemModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;

@Component(Constants.BEAN_NAME_SYSTEM_INFO)
public class DeviceQoSEvaluatorSystemInfo extends SystemInfo {

	//=================================================================================================
	// members

	@Value(DeviceQoSEvaluatorConstants.$MEASUREMENT_ORGANIZER_JOB_INTERVAL_WD)
	private long measurementOrganizerJobInterval;

	@Value(DeviceQoSEvaluatorConstants.$RTT_MEASUREMENT_JOB_INTERVAL_WD)
	private long rttMeasurementJobInterval;

	@Value(DeviceQoSEvaluatorConstants.$RTT_MEASUREMENT_TIMEOUT_WD)
	private int rttMeasurementTimeout;

	@Value(DeviceQoSEvaluatorConstants.$AUGMENTED_MEASUREMENT_JOB_INTERVAL_WD)
	private long augmentedMeasurementJobInterval;

	@Value(DeviceQoSEvaluatorConstants.$EVALUATION_TIME_WINDOW_WD)
	private long evaluationTimeWindow;

	@Value(DeviceQoSEvaluatorConstants.$CLEANING_JOB_INTERVAL_WD)
	private long cleaningJobInterval;

	@Value(DeviceQoSEvaluatorConstants.$RAW_MEASUREMENT_DATA_MAX_AGE_WD)
	private int rawMeasurementDataMaxAge;

	@Value(DeviceQoSEvaluatorConstants.$INACTIVE_DEVICE_MAX_AGE_WD)
	private int inactiveDeviceMaxAge;

	@Value(Constants.$MAX_PAGE_SIZE_WD)
	private int maxPageSize;

	private SystemModel systemModel;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String getSystemName() {
		return Constants.SYS_NAME_DEVICE_QOS_EVALUATOR;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public SystemModel getSystemModel() {
		if (systemModel == null) {
			SystemModel.Builder builder = new SystemModel.Builder()
					.address(getAddress())
					.version(Constants.AH_FRAMEWORK_VERSION);

			if (AuthenticationPolicy.CERTIFICATE == this.getAuthenticationPolicy()) {
				builder = builder.metadata(Constants.METADATA_KEY_X509_PUBLIC_KEY, getPublicKey());
			}

			systemModel = builder.build();
		}

		return systemModel;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public List<ServiceModel> getServices() {

		final ServiceModel generalManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_GENERAL_MANAGEMENT)
				.version(DeviceQoSEvaluatorConstants.VERSION_GENERAL_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForGeneralManagement())
				.serviceInterface(getMqttServiceInterfaceForGeneralManagement())
				.build();

		final ServiceModel qualityEvaluation = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_QUALITY_EVALUATION)
				.version(DeviceQoSEvaluatorConstants.VERSION_QUALITY_EVALUATION)
				.metadata(Constants.METADATA_KEY_EVALUATION_TYPE, DeviceQoSEvaluatorConstants.METADATA_VALUE_QUALITY_EVALUATION_SERVICE_VARIANT)
				.serviceInterface(getHttpServiceInterfaceForQualityEvaluationService())
				.serviceInterface(getMqttServiceInterfaceForQualityEvaluationService())
				.build();

		final ServiceModel deviceQualityDataManagement = new ServiceModel.Builder()
				.serviceDefinition(Constants.SERVICE_DEF_DEVICE_QUALITY_DATA_MANAGEMENT)
				.version(DeviceQoSEvaluatorConstants.VERSION_DEVICE_QUALITY_DATA_MANAGEMENT)
				.serviceInterface(getHttpServiceInterfaceForDeviceQualityDataManagementService())
				.serviceInterface(getMqttServiceInterfaceForDeviceQualityDataManagementService())
				.build();

		// starting with management services speeds up management filters
		return List.of(generalManagement, deviceQualityDataManagement, qualityEvaluation);
		// TODO: add monitor service when it is specified and implemented
	}

	//-------------------------------------------------------------------------------------------------
	public long getMeasurementOrganizerJobInterval() {
		return measurementOrganizerJobInterval;
	}

	//-------------------------------------------------------------------------------------------------
	public long getRttMeasurementJobInterval() {
		return rttMeasurementJobInterval;
	}

	//-------------------------------------------------------------------------------------------------
	public int getRttMeasurementTimeout() {
		return rttMeasurementTimeout;
	}

	//-------------------------------------------------------------------------------------------------
	public long getAugmentedMeasurementJobInterval() {
		return augmentedMeasurementJobInterval;
	}

	//-------------------------------------------------------------------------------------------------
	public long getEvaluationTimeWindow() {
		return evaluationTimeWindow;
	}

	//-------------------------------------------------------------------------------------------------
	public long getCleaningJobInterval() {
		return cleaningJobInterval;
	}

	//-------------------------------------------------------------------------------------------------
	public int getRawMeasurementDataMaxAge() {
		return rawMeasurementDataMaxAge;
	}

	//-------------------------------------------------------------------------------------------------
	public int getInactiveDeviceMaxAge() {
		return inactiveDeviceMaxAge;
	}

	//-------------------------------------------------------------------------------------------------
	public int getMaxPageSize() {
		return maxPageSize;
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@Override
	protected PublicConfigurationKeysAndDefaults getPublicConfigurationKeysAndDefaults() {
		return new PublicConfigurationKeysAndDefaults(
				Set.of(Constants.SERVER_ADDRESS,
						Constants.SERVER_PORT,
						Constants.MQTT_API_ENABLED,
						Constants.DOMAIN_NAME,
						Constants.AUTHENTICATION_POLICY,
						Constants.ENABLE_MANAGEMENT_FILTER,
						Constants.MANAGEMENT_POLICY,
						Constants.ENABLE_BLACKLIST_FILTER,
						Constants.FORCE_BLACKLIST_FILTER,
						Constants.MAX_PAGE_SIZE,
						Constants.NORMALIZATION_MODE,
						DeviceQoSEvaluatorConstants.MEASUREMENT_ORGANIZER_JOB_INTERVAL,
						DeviceQoSEvaluatorConstants.RTT_MEASUREMENT_JOB_INTERVAL,
						DeviceQoSEvaluatorConstants.AUGMENTED_MEASUREMENT_JOB_INTERVAL,
						DeviceQoSEvaluatorConstants.EVALUATION_TIME_WINDOW),
				DeviceQoSEvaluatorDefaults.class);
	}

	// HTTP Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForGeneralManagement() {
		return getHttpServiceInterfaceForAGeneralManagementService(DeviceQoSEvaluatorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForQualityEvaluationService() {
		return getHttpServiceInterfaceForAQualityEvaluationService(DeviceQoSEvaluatorConstants.HTTP_API_QUALITY_EVALUATION_PATH);
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForDeviceQualityDataManagementService() {
		return getHttpServiceInterfaceForADeviceQualityDataManagementService(DeviceQoSEvaluatorConstants.HTTP_API_DEVICE_QUALITY_DATA_MANAGEMENT_PATH);
	}

	// HTTP Interface Operations

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAGeneralManagementService(final String basePath) {
		final String templateName = isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel log = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(Constants.HTTP_API_OP_LOGS_PATH)
				.build();

		final HttpOperationModel config = new HttpOperationModel.Builder()
				.method(HttpMethod.GET.name())
				.path(Constants.HTTP_API_OP_GET_CONFIG_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(DeviceQoSEvaluatorConstants.HTTP_API_GENERAL_MANAGEMENT_PATH)
				.operation(Constants.SERVICE_OP_GET_LOG, log)
				.operation(Constants.SERVICE_OP_GET_CONFIG, config)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForAQualityEvaluationService(final String basePath) {
		final String templateName = isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel filter = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DeviceQoSEvaluatorConstants.HTTP_API_OP_FILTER_PATH)
				.build();
		final HttpOperationModel sort = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DeviceQoSEvaluatorConstants.HTTP_API_OP_SORT_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_FILTER, filter)
				.operation(Constants.SERVICE_OP_SORT, sort)
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getHttpServiceInterfaceForADeviceQualityDataManagementService(final String basePath) {
		final String templateName = isSslEnabled() ? Constants.GENERIC_HTTPS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_HTTP_INTERFACE_TEMPLATE_NAME;

		final HttpOperationModel query = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DeviceQoSEvaluatorConstants.HTTP_API_OP_QUERY_PATH)
				.build();
		final HttpOperationModel reload = new HttpOperationModel.Builder()
				.method(HttpMethod.POST.name())
				.path(DeviceQoSEvaluatorConstants.HTTP_API_OP_RELOAD_PATH)
				.build();

		return new HttpInterfaceModel.Builder(templateName, getDomainAddress(), getServerPort())
				.basePath(basePath)
				.operation(Constants.SERVICE_OP_QUERY, query)
				.operation(Constants.SERVICE_OP_RELOAD, reload)
				.build();
	}

	// MQTT Interfaces

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForGeneralManagement() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(DeviceQoSEvaluatorConstants.MQTT_API_GENERAL_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_GET_LOG, Constants.SERVICE_OP_GET_CONFIG))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForQualityEvaluationService() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(DeviceQoSEvaluatorConstants.MQTT_API_QUALITY_EVALUATION_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_FILTER, Constants.SERVICE_OP_SORT))
				.build();
	}

	//-------------------------------------------------------------------------------------------------
	private InterfaceModel getMqttServiceInterfaceForDeviceQualityDataManagementService() {
		if (!isMqttApiEnabled()) {
			return null;
		}

		final String templateName = isSslEnabled() ? Constants.GENERIC_MQTTS_INTERFACE_TEMPLATE_NAME : Constants.GENERIC_MQTT_INTERFACE_TEMPLATE_NAME;
		return new MqttInterfaceModel.Builder(templateName, getMqttBrokerAddress(), getMqttBrokerPort())
				.baseTopic(DeviceQoSEvaluatorConstants.MQTT_API_DEVICE_QUALITY_DATA_MANAGEMENT_BASE_TOPIC)
				.operations(Set.of(Constants.SERVICE_OP_QUERY, Constants.SERVICE_OP_RELOAD))
				.build();
	}
}
