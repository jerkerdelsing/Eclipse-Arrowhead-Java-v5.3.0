/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A routing key bound to the SysMonitor intake queue, so that unbinding works after a restart.
@Entity
@Table(name = "amqp_binding")
public class AmqpBinding {

	//=================================================================================================
	// members

	public static final int KEY_LENGTH = 255;

	@Id
	@Column(name = "binding_key", nullable = false, length = KEY_LENGTH)
	private String bindingKey;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AmqpBinding() {
	}

	//-------------------------------------------------------------------------------------------------
	public AmqpBinding(final String bindingKey) {
		this.bindingKey = bindingKey;
	}

	//-------------------------------------------------------------------------------------------------
	public String getBindingKey() {
		return bindingKey;
	}
}
