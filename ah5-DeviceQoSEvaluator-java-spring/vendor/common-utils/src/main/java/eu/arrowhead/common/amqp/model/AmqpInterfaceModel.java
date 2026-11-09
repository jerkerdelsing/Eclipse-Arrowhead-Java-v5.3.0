/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.common.amqp.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.model.InterfaceModel;

// Interface of the generic_amqp / generic_amqps templates: an AMQP 0-9-1 topic exchange and the routing-key prefix
// below which the provider publishes, one routing-key word per operation (<baseRoutingKey>.<operation>).
public record AmqpInterfaceModel(
		String templateName,
		List<String> accessAddresses,
		int accessPort,
		String exchange,
		String baseRoutingKey,
		Set<String> operations) implements InterfaceModel {

	//-------------------------------------------------------------------------------------------------
	public AmqpInterfaceModel {
		Assert.isTrue(!Utilities.isEmpty(templateName), "'templateName' is missing or empty");
		Assert.isTrue(!Utilities.isEmpty(accessAddresses), "'accessAddresses' is missing or empty");
		Assert.isTrue(accessPort >= Constants.MIN_PORT && accessPort <= Constants.MAX_PORT, "'accessPort' is invalid");
		Assert.isTrue(!Utilities.isEmpty(exchange), "'exchange' is missing or empty");
		Assert.isTrue(!Utilities.isEmpty(baseRoutingKey), "'baseRoutingKey' is missing or empty");
		Assert.isTrue(!Utilities.isEmpty(operations), "'operations' is missing or empty");
	}

	//=================================================================================================
	// members

	public static final String PROP_NAME_ACCESS_ADDRESSES = "accessAddresses";
	public static final String PROP_NAME_ACCESS_PORT = "accessPort";
	public static final String PROP_NAME_EXCHANGE = "exchange";
	public static final String PROP_NAME_BASE_ROUTING_KEY = "baseRoutingKey";
	public static final String PROP_NAME_OPERATIONS = "operations";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Override
	public String protocol() {
		return templateName.equals(Constants.GENERIC_AMQP_INTERFACE_TEMPLATE_NAME) ? "amqp" : "amqps";
	}

	//-------------------------------------------------------------------------------------------------
	@Override
	public Map<String, Object> properties() {
		return Map.of(PROP_NAME_ACCESS_ADDRESSES, accessAddresses,
				PROP_NAME_ACCESS_PORT, accessPort,
				PROP_NAME_EXCHANGE, exchange,
				PROP_NAME_BASE_ROUTING_KEY, baseRoutingKey,
				PROP_NAME_OPERATIONS, operations);
	}
}
