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
package eu.arrowhead.serviceorchestration.jpa.entity;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import eu.arrowhead.common.jpa.UnmodifiableArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class QosEvalResult extends UnmodifiableArrowheadEntity {

	//=================================================================================================
	// members

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "orchestrationJobId", referencedColumnName = "id", nullable = false)
	private OrchestrationJob orchestrationJob;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_MEDIUM)
	private String evaluationType;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_TINY)
	private String operation;

	@Column(nullable = true)
	private String result;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public QosEvalResult() {
	}

	//-------------------------------------------------------------------------------------------------
	public QosEvalResult(final OrchestrationJob orchestrationJob, final String evaluationType, final String operation, final String result) {
		this.orchestrationJob = orchestrationJob;
		this.evaluationType = evaluationType;
		this.operation = operation;
		this.result = result;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "QosEvalResult [id=" + id + ", createdAt=" + createdAt + ", orchestrationJob=" + orchestrationJob + ", evaluationType=" + evaluationType + ", operation=" + operation
				+ ", result=" + result + "]";
	}

	//-------------------------------------------------------------------------------------------------
	public OrchestrationJob getOrchestrationJob() {
		return orchestrationJob;
	}

	//-------------------------------------------------------------------------------------------------
	public void setOrchestrationJob(final OrchestrationJob orchestrationJob) {
		this.orchestrationJob = orchestrationJob;
	}

	//-------------------------------------------------------------------------------------------------
	public String getEvaluationType() {
		return evaluationType;
	}

	//-------------------------------------------------------------------------------------------------
	public void setEvaluationType(final String evaluationType) {
		this.evaluationType = evaluationType;
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
	public String getResult() {
		return result;
	}

	//-------------------------------------------------------------------------------------------------
	public void setResult(final String result) {
		this.result = result;
	}

}
