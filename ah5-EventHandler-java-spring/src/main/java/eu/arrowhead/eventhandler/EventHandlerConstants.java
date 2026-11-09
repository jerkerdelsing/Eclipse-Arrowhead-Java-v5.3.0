/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.eventhandler;

public final class EventHandlerConstants {

	public static final String SYSTEM_NAME = "EventHandler";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.eventhandler.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.eventhandler.jpa.repository";

	public static final String SERVICE_DEF_EVENT_PUBLISH = "eventPublish";
	public static final String SERVICE_DEF_EVENT_SUBSCRIPTION = "eventSubscription";
	public static final String SERVICE_DEF_EVENT_HANDLER_MANAGEMENT = "eventHandlerManagement";
	public static final String VERSION_EVENT_PUBLISH = "1.0.0";
	public static final String VERSION_EVENT_SUBSCRIPTION = "1.0.0";
	public static final String VERSION_EVENT_HANDLER_MANAGEMENT = "1.0.0";

	public static final String SERVICE_OP_PUBLISH = "publish";
	public static final String SERVICE_OP_SUBSCRIBE = "subscribe";
	public static final String SERVICE_OP_UNSUBSCRIBE = "unsubscribe";
	public static final String SERVICE_OP_QUERY_SUBSCRIPTIONS = "query-subscriptions";
	public static final String SERVICE_OP_UPDATE_SUBSCRIPTION = "update-subscription";
	public static final String SERVICE_OP_REMOVE_SUBSCRIPTION = "remove-subscription";

	public static final String HTTP_API_BASE_PATH = "/eventhandler";
	public static final String HTTP_API_EVENT_PUBLISH_PATH = HTTP_API_BASE_PATH + "/event-publish";
	public static final String HTTP_API_EVENT_SUBSCRIPTION_PATH = HTTP_API_BASE_PATH + "/event-subscription";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/management";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	public static final String HTTP_API_OP_PUBLISH_PATH = "/publish";
	public static final String HTTP_API_OP_SUBSCRIBE_PATH = "/subscribe";
	public static final String HTTP_API_OP_UNSUBSCRIBE_PATH = "/unsubscribe/{subscriptionId}";
	public static final String HTTP_API_OP_SUBSCRIPTIONS_PATH = "/subscriptions";
	public static final String HTTP_API_OP_SUBSCRIPTION_BY_ID_PATH = HTTP_API_OP_SUBSCRIPTIONS_PATH + "/{subscriptionId}";

	public static final String PUBLISH_TOLERANCE_SECONDS = "event.publish.tolerance.seconds";
	public static final String $PUBLISH_TOLERANCE_SECONDS_WD = "${" + PUBLISH_TOLERANCE_SECONDS + ":" + EventHandlerDefaults.PUBLISH_TOLERANCE_SECONDS_DEFAULT + "}";
	public static final String DELIVERY_ATTEMPTS = "event.delivery.attempts";
	public static final String $DELIVERY_ATTEMPTS_WD = "${" + DELIVERY_ATTEMPTS + ":" + EventHandlerDefaults.DELIVERY_ATTEMPTS_DEFAULT + "}";
	public static final String SERVICE_AUTHORIZATION_ENABLED = "service.authorization.enabled";
	public static final String $SERVICE_AUTHORIZATION_ENABLED_WD = "${" + SERVICE_AUTHORIZATION_ENABLED + ":" + EventHandlerDefaults.SERVICE_AUTHORIZATION_ENABLED_DEFAULT + "}";
	public static final long SERVICE_AUTHORIZATION_CACHE_MILLIS = 30000;
	public static final int REQUEST_FILTER_ORDER_SERVICE_AUTHORIZATION = 30;

	public static final String EXPIRATION_CLEANUP_INTERVAL = "event.expiration.cleanup.interval";
	public static final String $EXPIRATION_CLEANUP_INTERVAL_WD = "${" + EXPIRATION_CLEANUP_INTERVAL + ":" + EventHandlerDefaults.EXPIRATION_CLEANUP_INTERVAL_DEFAULT + "}";

	private EventHandlerConstants() {
		throw new UnsupportedOperationException();
	}
}
