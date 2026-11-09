/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer;

public final class ChoreographerConstants {

	public static final String SYSTEM_NAME = "Choreographer";

	public static final String DATABASE_ENTITY_PACKAGE = "eu.arrowhead.choreographer.jpa.entity";
	public static final String DATABASE_REPOSITORY_PACKAGE = "eu.arrowhead.choreographer.jpa.repository";

	// ---------------------------------------------------------------------------------------- services
	public static final String SERVICE_DEF_SESSION = "choreographerSession";
	public static final String SERVICE_DEF_EXECUTOR = "choreographerExecutor";
	public static final String SERVICE_DEF_MANAGEMENT = "choreographerManagement";
	public static final String VERSION = "1.0.0";

	// choreographerSession (consumers start and abort their own sessions)
	public static final String SERVICE_OP_START_SESSION = "start-session";
	public static final String SERVICE_OP_ABORT_SESSION = "abort-session";
	// choreographerExecutor (executor systems)
	public static final String SERVICE_OP_REGISTER = "register";
	public static final String SERVICE_OP_UNREGISTER = "unregister";
	public static final String SERVICE_OP_NOTIFY_STEP_DONE = "notify-step-done";
	// choreographerManagement
	public static final String SERVICE_OP_QUERY_PLANS = "query-plans";
	public static final String SERVICE_OP_GET_PLAN = "get-plan";
	public static final String SERVICE_OP_CREATE_PLAN = "create-plan";
	public static final String SERVICE_OP_REMOVE_PLAN = "remove-plan";
	public static final String SERVICE_OP_CHECK_PLAN = "check-plan";
	public static final String SERVICE_OP_START_SESSIONS = "start-sessions";
	public static final String SERVICE_OP_MGMT_ABORT_SESSION = "abort-session";
	public static final String SERVICE_OP_QUERY_SESSIONS = "query-sessions";
	public static final String SERVICE_OP_QUERY_SESSION_STEPS = "query-session-steps";
	public static final String SERVICE_OP_QUERY_WORKLOG = "query-worklog";
	public static final String SERVICE_OP_QUERY_EXECUTORS = "query-executors";
	public static final String SERVICE_OP_GET_EXECUTOR = "get-executor";
	public static final String SERVICE_OP_CREATE_EXECUTOR = "create-executor";
	public static final String SERVICE_OP_REMOVE_EXECUTOR = "remove-executor";

	// ---------------------------------------------------------------------------------------- HTTP API
	public static final String HTTP_API_BASE_PATH = "/choreographer";
	public static final String HTTP_API_SESSION_PATH = HTTP_API_BASE_PATH + "/session";
	public static final String HTTP_API_EXECUTOR_PATH = HTTP_API_BASE_PATH + "/executor";
	public static final String HTTP_API_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/mgmt";
	public static final String HTTP_API_GENERAL_MANAGEMENT_PATH = HTTP_API_BASE_PATH + "/general/mgmt";
	// Internal, unregistered: DynamicServiceOrchestration delivers on-behalf push orchestration results here.
	public static final String HTTP_API_ORCHESTRATION_RESULT_PATH = HTTP_API_BASE_PATH + "/orchestration-result";

	public static final String HTTP_PARAM_ID = "id";
	public static final String HTTP_PATH_ID = "/{" + HTTP_PARAM_ID + "}";
	public static final String HTTP_API_OP_START_PATH = "/start";
	public static final String HTTP_API_OP_ABORT_PATH = "/abort" + HTTP_PATH_ID;
	public static final String HTTP_API_OP_REGISTER_PATH = "/register";
	public static final String HTTP_API_OP_UNREGISTER_PATH = "/unregister";
	public static final String HTTP_API_OP_NOTIFY_STEP_DONE_PATH = "/notify-step-done";
	public static final String HTTP_API_OP_PLANS_PATH = "/plans";
	public static final String HTTP_API_OP_PLAN_BY_ID_PATH = HTTP_API_OP_PLANS_PATH + HTTP_PATH_ID;
	public static final String HTTP_API_OP_CHECK_PLAN_PATH = HTTP_API_OP_PLAN_BY_ID_PATH + "/check";
	public static final String HTTP_API_OP_SESSIONS_PATH = "/sessions";
	public static final String HTTP_API_OP_START_SESSIONS_PATH = HTTP_API_OP_SESSIONS_PATH + "/start";
	public static final String HTTP_API_OP_SESSION_BY_ID_PATH = HTTP_API_OP_SESSIONS_PATH + HTTP_PATH_ID;
	public static final String HTTP_API_OP_SESSION_STEPS_PATH = "/session-steps";
	public static final String HTTP_API_OP_WORKLOG_PATH = "/worklog";
	public static final String HTTP_API_OP_EXECUTORS_PATH = "/executors";
	public static final String HTTP_API_OP_EXECUTOR_BY_ID_PATH = HTTP_API_OP_EXECUTORS_PATH + HTTP_PATH_ID;

	// Executor endpoints (relative to the executor base path), unchanged from v4.6
	public static final String EXECUTOR_OP_SERVICE_INFO_PATH = "/service-info";
	public static final String EXECUTOR_OP_START_PATH = "/start";
	public static final String EXECUTOR_OP_ABORT_PATH = "/abort";

	// ---------------------------------------------------------------------------------------- properties
	public static final String MAX_PLAN_ITERATION = "max.plan.iteration";
	public static final String $MAX_PLAN_ITERATION_WD = "${" + MAX_PLAN_ITERATION + ":" + ChoreographerDefaults.MAX_PLAN_ITERATION_DEFAULT + "}";
	public static final String ORCHESTRATION_TIMEOUT = "orchestration.timeout";
	public static final String $ORCHESTRATION_TIMEOUT_WD = "${" + ORCHESTRATION_TIMEOUT + ":" + ChoreographerDefaults.ORCHESTRATION_TIMEOUT_DEFAULT + "}";
	public static final String SESSION_WORKER_THREADS = "session.worker.threads";
	public static final String $SESSION_WORKER_THREADS_WD = "${" + SESSION_WORKER_THREADS + ":" + ChoreographerDefaults.SESSION_WORKER_THREADS_DEFAULT + "}";

	public static final String DOT = ".";

	private ChoreographerConstants() {
		throw new UnsupportedOperationException();
	}
}
