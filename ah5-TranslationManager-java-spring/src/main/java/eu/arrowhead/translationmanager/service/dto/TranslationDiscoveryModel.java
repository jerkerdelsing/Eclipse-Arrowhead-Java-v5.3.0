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
package eu.arrowhead.translationmanager.service.dto;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TranslationDiscoveryModel {

	//=================================================================================================
	// members

	private String targetInstanceId;
	private String provider;
	private String serviceDefinition;
	private String operation;
	private String targetInputDataModelId;
	private String targetOutputDataModelId;
	private String targetPolicy;
	private Map<String, Object> targetProperties;

	private String consumer;
	private String inputDataModelIdRequirement;
	private String outputDataModelIdRequirement;

	private String fromInterfaceTemplate;
	private String toInterfaceTemplate;
	private String interfaceTranslator;
	private String interfaceTranslatorPolicy;
	private Map<String, Object> interfaceTranslatorProperties;
	private String interfaceTranslatorToken;

	private String inputDataModelTranslator;
	private boolean inputDataModelTranslatorFactory;
	private Map<String, Object> inputDataModelTranslatorProperties;

	private String outputDataModelTranslator;
	private boolean outputDataModelTranslatorFactory;
	private Map<String, Object> outputDataModelTranslatorProperties;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public TranslationDiscoveryModel() {
	}

	//-------------------------------------------------------------------------------------------------
	public TranslationDiscoveryModel(
			final String targetInstanceId,
			final String provider,
			final String serviceDefinition,
			final String operation,
			final String consumer,
			final String inputDataModelIdRequirement,
			final String outputDataModelIdRequirement) {
		this.targetInstanceId = targetInstanceId;
		this.provider = provider;
		this.serviceDefinition = serviceDefinition;
		this.operation = operation;
		this.consumer = consumer;
		this.inputDataModelIdRequirement = inputDataModelIdRequirement;
		this.outputDataModelIdRequirement = outputDataModelIdRequirement;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getTargetInstanceId() {
		return targetInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetInstanceId(final String targetInstanceId) {
		this.targetInstanceId = targetInstanceId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getConsumer() {
		return consumer;
	}

	//-------------------------------------------------------------------------------------------------
	public void setConsumer(final String consumer) {
		this.consumer = consumer;
	}

	//-------------------------------------------------------------------------------------------------
	public String getProvider() {
		return provider;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProvider(final String provider) {
		this.provider = provider;
	}

	//-------------------------------------------------------------------------------------------------
	public String getServiceDefinition() {
		return serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public void setServiceDefinition(final String serviceDefinition) {
		this.serviceDefinition = serviceDefinition;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOperation() {
		return operation;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOperation(final String operation) {
		this.operation = operation;
	}

	//-------------------------------------------------------------------------------------------------
	public String getFromInterfaceTemplate() {
		return fromInterfaceTemplate;
	}

	//-------------------------------------------------------------------------------------------------
	public void setFromInterfaceTemplate(final String fromInterfaceTemplate) {
		this.fromInterfaceTemplate = fromInterfaceTemplate;

	}

	//-------------------------------------------------------------------------------------------------
	public String getToInterfaceTemplate() {
		return toInterfaceTemplate;
	}

	//-------------------------------------------------------------------------------------------------
	public void setToInterfaceTemplate(final String toInterfaceTemplate) {
		this.toInterfaceTemplate = toInterfaceTemplate;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInterfaceTranslator() {
		return interfaceTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceTranslator(final String interfaceTranslator) {
		this.interfaceTranslator = interfaceTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> getInterfaceTranslatorProperties() {
		return interfaceTranslatorProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceTranslatorProperties(final Map<String, Object> interfaceTranslatorProperties) {
		this.interfaceTranslatorProperties = interfaceTranslatorProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTargetInputDataModelId() {
		return targetInputDataModelId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetInputDataModelId(final String targetInputDataModelId) {
		this.targetInputDataModelId = targetInputDataModelId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTargetOutputDataModelId() {
		return targetOutputDataModelId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetOutputDataModelId(final String targetOutputDataModelId) {
		this.targetOutputDataModelId = targetOutputDataModelId;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInputDataModelIdRequirement() {
		return inputDataModelIdRequirement;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInputDataModelIdRequirement(final String inputDataModelIdRequirement) {
		this.inputDataModelIdRequirement = inputDataModelIdRequirement;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOutputDataModelIdRequirement() {
		return outputDataModelIdRequirement;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOutputDataModelIdRequirement(final String outputDataModelIdRequirement) {
		this.outputDataModelIdRequirement = outputDataModelIdRequirement;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInputDataModelTranslator() {
		return inputDataModelTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInputDataModelTranslator(final String inputDataModelTranslator) {
		this.inputDataModelTranslator = inputDataModelTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> getInputDataModelTranslatorProperties() {
		return inputDataModelTranslatorProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInputDataModelTranslatorProperties(final Map<String, Object> inputDataModelTranslatorProperties) {
		this.inputDataModelTranslatorProperties = inputDataModelTranslatorProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public String getOutputDataModelTranslator() {
		return outputDataModelTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOutputDataModelTranslator(final String outputDataModelTranslator) {
		this.outputDataModelTranslator = outputDataModelTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> getOutputDataModelTranslatorProperties() {
		return outputDataModelTranslatorProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOutputDataModelTranslatorProperties(final Map<String, Object> outputDataModelTranslatorProperties) {
		this.outputDataModelTranslatorProperties = outputDataModelTranslatorProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isInputDataModelTranslatorFactory() {
		return inputDataModelTranslatorFactory;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInputDataModelTranslatorFactory(final boolean inputDataModelTranslatorFactory) {
		this.inputDataModelTranslatorFactory = inputDataModelTranslatorFactory;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isOutputDataModelTranslatorFactory() {
		return outputDataModelTranslatorFactory;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOutputDataModelTranslatorFactory(final boolean outputDataModelTranslatorFactory) {
		this.outputDataModelTranslatorFactory = outputDataModelTranslatorFactory;
	}

	//-------------------------------------------------------------------------------------------------
	public String getTargetPolicy() {
		return targetPolicy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetPolicy(final String targetPolicy) {
		this.targetPolicy = targetPolicy;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> getTargetProperties() {
		return targetProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public void setTargetProperties(final Map<String, Object> targetProperties) {
		this.targetProperties = targetProperties;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInterfaceTranslatorPolicy() {
		return interfaceTranslatorPolicy;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceTranslatorPolicy(final String interfaceTranslatorPolicy) {
		this.interfaceTranslatorPolicy = interfaceTranslatorPolicy;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInterfaceTranslatorToken() {
		return interfaceTranslatorToken;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceTranslatorToken(final String interfaceTranslatorToken) {
		this.interfaceTranslatorToken = interfaceTranslatorToken;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return Objects.hash(
				consumer,
				fromInterfaceTemplate,
				inputDataModelIdRequirement,
				inputDataModelTranslator,
				inputDataModelTranslatorFactory,
				inputDataModelTranslatorProperties,
				interfaceTranslator,
				interfaceTranslatorPolicy,
				interfaceTranslatorProperties,
				interfaceTranslatorToken,
				operation,
				outputDataModelIdRequirement,
				outputDataModelTranslator,
				outputDataModelTranslatorFactory,
				outputDataModelTranslatorProperties,
				provider,
				serviceDefinition,
				targetInputDataModelId,
				targetInstanceId,
				targetOutputDataModelId,
				targetPolicy,
				targetProperties,
				toInterfaceTemplate);
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		final TranslationDiscoveryModel other = (TranslationDiscoveryModel) obj;

		return Objects.equals(consumer, other.consumer)
				&& Objects.equals(fromInterfaceTemplate, other.fromInterfaceTemplate)
				&& Objects.equals(inputDataModelIdRequirement, other.inputDataModelIdRequirement)
				&& Objects.equals(inputDataModelTranslator, other.inputDataModelTranslator)
				&& inputDataModelTranslatorFactory == other.inputDataModelTranslatorFactory
				&& Objects.equals(inputDataModelTranslatorProperties, other.inputDataModelTranslatorProperties)
				&& Objects.equals(interfaceTranslator, other.interfaceTranslator)
				&& Objects.equals(interfaceTranslatorPolicy, other.interfaceTranslatorPolicy)
				&& Objects.equals(interfaceTranslatorProperties, other.interfaceTranslatorProperties)
				&& Objects.equals(interfaceTranslatorToken, other.interfaceTranslatorToken)
				&& Objects.equals(operation, other.operation)
				&& Objects.equals(outputDataModelIdRequirement, other.outputDataModelIdRequirement)
				&& Objects.equals(outputDataModelTranslator, other.outputDataModelTranslator)
				&& outputDataModelTranslatorFactory == other.outputDataModelTranslatorFactory
				&& Objects.equals(outputDataModelTranslatorProperties, other.outputDataModelTranslatorProperties)
				&& Objects.equals(provider, other.provider)
				&& Objects.equals(serviceDefinition, other.serviceDefinition)
				&& Objects.equals(targetInputDataModelId, other.targetInputDataModelId)
				&& Objects.equals(targetInstanceId, other.targetInstanceId)
				&& Objects.equals(targetOutputDataModelId, other.targetOutputDataModelId)
				&& Objects.equals(targetPolicy, other.targetPolicy)
				&& Objects.equals(targetProperties, other.targetProperties)
				&& Objects.equals(toInterfaceTemplate, other.toInterfaceTemplate);
	}
}