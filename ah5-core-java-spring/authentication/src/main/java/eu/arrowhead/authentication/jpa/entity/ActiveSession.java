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
package eu.arrowhead.authentication.jpa.entity;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import eu.arrowhead.common.jpa.ArrowheadEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ActiveSession {

	//=================================================================================================
	// members

	public static final String SORT_NAME_SYSTEM_NAME = "system_name";
	public static final List<String> SYSTEM_NAME_ALTERNATIVES = List.of("systemname", "name");
	public static final List<String> SORTABLE_FIELDS_BY = List.of("id", SORT_NAME_SYSTEM_NAME, "loginTime");
	public static final List<String> ACCEPTABLE_SORT_FIELDS = Stream.concat(SORTABLE_FIELDS_BY.stream(), SYSTEM_NAME_ALTERNATIVES.stream()).toList();
	public static final String DEFAULT_SORT_FIELD = SORT_NAME_SYSTEM_NAME;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "systemId", referencedColumnName = "id", nullable = false, unique = true)
	private System system;

	@Column(nullable = false, length = ArrowheadEntity.VARCHAR_SMALL)
	private String token;

	@Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
	private ZonedDateTime loginTime;

	@Column(nullable = false)
	private ZonedDateTime expirationTime;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public ActiveSession() {
	}

	//-------------------------------------------------------------------------------------------------
	public ActiveSession(final System system, final String token, final ZonedDateTime loginTime, final ZonedDateTime expirationTime) {
		this.system = system;
		this.token = token;
		this.loginTime = loginTime;
		this.expirationTime = expirationTime;
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "ActiveSession [id=" + id + ", system=" + system + ", token=" + token + ", loginTime=" + loginTime + ", expirationTime=" + expirationTime + "]";
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
	public System getSystem() {
		return system;
	}

	//-------------------------------------------------------------------------------------------------
	public void setSystem(final System system) {
		this.system = system;
	}

	//-------------------------------------------------------------------------------------------------
	public String getToken() {
		return token;
	}

	//-------------------------------------------------------------------------------------------------
	public void setToken(final String token) {
		this.token = token;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getLoginTime() {
		return loginTime;
	}

	//-------------------------------------------------------------------------------------------------
	public void setLoginTime(final ZonedDateTime loginTime) {
		this.loginTime = loginTime;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExpirationTime() {
		return expirationTime;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExpirationTime(final ZonedDateTime expirationTime) {
		this.expirationTime = expirationTime;
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

		final ActiveSession other = (ActiveSession) obj;
		return id == other.id;
	}
}