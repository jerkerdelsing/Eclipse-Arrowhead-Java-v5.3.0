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
package eu.arrowhead.translationmanager.jpa.entity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class BridgeDetails {

	//=================================================================================================
	// members

	public static final String SORT_NAME_HEADER_UUID = "header_uuid";
	public static final String SORT_NAME_HEADER_CREATED_BY = "header_createdBy";
	public static final String SORT_NAME_HEADER_STATUS = "header_status";
	public static final String SORT_NAME_HEADER_USAGE_REPORT_COUNT = "header_usageReportCount";
	public static final String SORT_NAME_HEADER_ALIVES_AT = "header_alivesAt";
	public static final String SORT_NAME_HEADER_CREATED_AT = "header_createdAt";

	public static final Map<String, String> SORT_NAME_ALTERNATIVES = Map.of(
			"bridgeId", SORT_NAME_HEADER_UUID,
			"uuid", SORT_NAME_HEADER_UUID,
			"creator", SORT_NAME_HEADER_CREATED_BY,
			"createdBy", SORT_NAME_HEADER_CREATED_BY,
			"status", SORT_NAME_HEADER_STATUS,
			"usage", SORT_NAME_HEADER_USAGE_REPORT_COUNT,
			"usageReportCount", SORT_NAME_HEADER_USAGE_REPORT_COUNT,
			"alivesAt", SORT_NAME_HEADER_ALIVES_AT,
			"createdAt", SORT_NAME_HEADER_CREATED_AT);

	public static final List<String> SORTABLE_FIELDS_BY = List.of(
			SORT_NAME_HEADER_UUID,
			SORT_NAME_HEADER_CREATED_BY,
			SORT_NAME_HEADER_STATUS,
			SORT_NAME_HEADER_USAGE_REPORT_COUNT,
			SORT_NAME_HEADER_ALIVES_AT,
			SORT_NAME_HEADER_CREATED_AT,
			"consumer",
			"provider",
			"serviceDefinition");

	public static final List<String> ACCEPTABLE_SORT_FIELDS = Stream.concat(SORTABLE_FIELDS_BY.stream(), SORT_NAME_ALTERNATIVES.keySet().stream()).toList();
	public static final String DEFAULT_SORT_FIELD = SORT_NAME_HEADER_CREATED_AT;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "headerId", referencedColumnName = "id", nullable = false)
	private BridgeHeader header;

	@Column(nullable = false, length = UnmodifiableArrowheadEntity.VARCHAR_SMALL)
	private String consumer;

	@Column(nullable = false, length = UnmodifiableArrowheadEntity.VARCHAR_SMALL)
	private String provider;

	@Column(nullable = false, length = UnmodifiableArrowheadEntity.VARCHAR_SMALL)
	private String serviceDefinition;

	@Column(nullable = false, length = UnmodifiableArrowheadEntity.VARCHAR_SMALL)
	private String operation;

	@Column(nullable = false, length = UnmodifiableArrowheadEntity.VARCHAR_SMALL)
	private String interfaceTranslator;

	@Column(nullable = false)
	private String interfaceTranslatorData;

	@Column(nullable = true, length = UnmodifiableArrowheadEntity.VARCHAR_SMALL)
	private String inputDmTranslator;

	@Column(nullable = true)
	private String inputDmTranslatorData;

	@Column(nullable = true, length = UnmodifiableArrowheadEntity.VARCHAR_SMALL)
	private String resultDmTranslator;

	@Column(nullable = true)
	private String resultDmTranslatorData;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public BridgeDetails() {
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:parameternumber")
	public BridgeDetails(
			final BridgeHeader header,
			final String consumer,
			final String provider,
			final String serviceDefinition,
			final String operation,
			final String interfaceTranslator,
			final String interfaceTranslatorData,
			final String inputDmTranslator,
			final String inputDmTranslatorData,
			final String resultDmTranslator,
			final String resultDmTranslatorData) {
		this.header = header;
		this.consumer = consumer;
		this.provider = provider;
		this.serviceDefinition = serviceDefinition;
		this.operation = operation;
		this.interfaceTranslator = interfaceTranslator;
		this.interfaceTranslatorData = interfaceTranslatorData;
		this.inputDmTranslator = inputDmTranslator;
		this.inputDmTranslatorData = inputDmTranslatorData;
		this.resultDmTranslator = resultDmTranslator;
		this.resultDmTranslatorData = resultDmTranslatorData;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "BridgeDetails [id=" + id + ", header=" + header + ", consumer=" + consumer + ", provider=" + provider + ", serviceDefinition=" + serviceDefinition + ", operation=" + operation
				+ ", interfaceTranslator=" + interfaceTranslator + ", interfaceTranslatorData=" + interfaceTranslatorData + ", inputDmTranslator=" + inputDmTranslator + ", inputDmTranslatorData="
				+ inputDmTranslatorData + ", resultDmTranslator=" + resultDmTranslator + ", resultDmTranslatorData=" + resultDmTranslatorData + "]";
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return Objects.hash(id);
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

		final BridgeDetails other = (BridgeDetails) obj;
		return id == other.id;
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public long getId() {
		return id;
	}

	//-------------------------------------------------------------------------------------------------
	public void setId(final long id) {
		this.id = id;
	}

	//-------------------------------------------------------------------------------------------------
	public BridgeHeader getHeader() {
		return header;
	}

	//-------------------------------------------------------------------------------------------------
	public void setHeader(final BridgeHeader header) {
		this.header = header;
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
	public String getInterfaceTranslator() {
		return interfaceTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceTranslator(final String interfaceTranslator) {
		this.interfaceTranslator = interfaceTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInterfaceTranslatorData() {
		return interfaceTranslatorData;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInterfaceTranslatorData(final String interfaceTranslatorData) {
		this.interfaceTranslatorData = interfaceTranslatorData;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInputDmTranslator() {
		return inputDmTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInputDmTranslator(final String inputDmTranslator) {
		this.inputDmTranslator = inputDmTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public String getInputDmTranslatorData() {
		return inputDmTranslatorData;
	}

	//-------------------------------------------------------------------------------------------------
	public void setInputDmTranslatorData(final String inputDmTranslatorData) {
		this.inputDmTranslatorData = inputDmTranslatorData;
	}

	//-------------------------------------------------------------------------------------------------
	public String getResultDmTranslator() {
		return resultDmTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public void setResultDmTranslator(final String resultDmTranslator) {
		this.resultDmTranslator = resultDmTranslator;
	}

	//-------------------------------------------------------------------------------------------------
	public String getResultDmTranslatorData() {
		return resultDmTranslatorData;
	}

	//-------------------------------------------------------------------------------------------------
	public void setResultDmTranslatorData(final String resultDmTranslatorData) {
		this.resultDmTranslatorData = resultDmTranslatorData;
	}
}