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
package eu.arrowhead.common.jpa;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.boot.logging.LogLevel;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class LogEntity {

	//=================================================================================================
	// members

	private static final int VARCHAR = 100;

	public static final List<String> SORTABLE_FIELDS_BY = List.of("logId", "entryDate", "logger", "logLevel");
	public static final String DEFAULT_SORT_FIELD = "entryDate";
	public static final String FIELD_NAME_ID = "logId";

	@Id
	@Column(length = VARCHAR)
	protected String logId;

	@Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	protected ZonedDateTime entryDate;

	@Column(nullable = true, length = VARCHAR)
	protected String logger;

	@Column(nullable = true, length = VARCHAR)
	@Enumerated(EnumType.STRING)
	protected LogLevel logLevel;

	@Column(nullable = true)
	protected String message;

	@Column(nullable = true)
	protected String exception;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public LogEntity() {
	}

	//-------------------------------------------------------------------------------------------------
	public LogEntity(final String logId, final ZonedDateTime entryDate, final String logger, final LogLevel logLevel, final String message, final String exception) {
		this.logId = logId;
		this.entryDate = entryDate;
		this.logger = logger;
		this.logLevel = logLevel;
		this.message = message;
		this.exception = exception;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		final String logLevelName = logLevel == null ? "null" : logLevel.name();
		return "Logs [logId = " + logId + ", entryDate = " + entryDate + ", logger = " + logger + ", logLevel = " + logLevelName + ", message = " + message + "]";
	}

	//=================================================================================================
	// boilerplate

	//-------------------------------------------------------------------------------------------------
	public String getLogId() {
		return logId;
	}

	//-------------------------------------------------------------------------------------------------
	public void setLogId(final String logId) {
		this.logId = logId;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getEntryDate() {
		return entryDate;
	}

	//-------------------------------------------------------------------------------------------------
	public void setEntryDate(final ZonedDateTime entryDate) {
		this.entryDate = entryDate;
	}

	//-------------------------------------------------------------------------------------------------
	public String getLogger() {
		return logger;
	}

	//-------------------------------------------------------------------------------------------------
	public void setLogger(final String logger) {
		this.logger = logger;
	}

	//-------------------------------------------------------------------------------------------------
	public LogLevel getLogLevel() {
		return logLevel;
	}

	//-------------------------------------------------------------------------------------------------
	public void setLogLevel(final LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	//-------------------------------------------------------------------------------------------------
	public String getMessage() {
		return message;
	}

	//-------------------------------------------------------------------------------------------------
	public void setMessage(final String message) {
		this.message = message;
	}

	//-------------------------------------------------------------------------------------------------
	public String getException() {
		return exception;
	}

	//-------------------------------------------------------------------------------------------------
	public void setException(final String exception) {
		this.exception = exception;
	}
}