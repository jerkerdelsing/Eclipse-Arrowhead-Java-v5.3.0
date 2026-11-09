/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.sysmonitor.api.http;

import static eu.arrowhead.sysmonitor.service.Json.json;
import static eu.arrowhead.sysmonitor.service.Json.map;
import static eu.arrowhead.sysmonitor.service.Json.obj;
import static eu.arrowhead.sysmonitor.service.Json.parse;
import static eu.arrowhead.sysmonitor.service.Json.str;
import static eu.arrowhead.sysmonitor.service.Json.strings;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.ErrorMessageDTO;
import eu.arrowhead.sysmonitor.SysMonitorConstants;
import eu.arrowhead.sysmonitor.jpa.service.MonitorDbService;
import eu.arrowhead.sysmonitor.service.MonitoringService;
import eu.arrowhead.sysmonitor.service.StatusService;
import eu.arrowhead.sysmonitor.service.SubscriptionConflictException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

// sysMonitorManagement (SysMonitor-Management-HTTP-IDD-5.2.2). LocalCloudManager and Sysop only (SysMonitorAccessFilter).
@RestController
@RequestMapping(SysMonitorConstants.HTTP_API_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class ManagementAPI {

	//=================================================================================================
	// members

	@Autowired
	private MonitorDbService store;

	@Autowired
	private MonitoringService monitoring;

	@Autowired
	private StatusService statusService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "SysMonitor status")
	@GetMapping(path = SysMonitorConstants.HTTP_API_OP_STATUS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> status(final HttpServletRequest request) {
		Responses.caller(request);
		return Responses.json(HttpStatus.OK, statusService.status());
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Orchestrated sysMonitor providers and their selected observation types")
	@GetMapping(path = SysMonitorConstants.HTTP_API_OP_PROVIDERS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> providers(final HttpServletRequest request) {
		Responses.caller(request);
		return Responses.json(HttpStatus.OK, map("defaultSubscription", monitoring.getDefaultSubscription(), "list", monitoring.providersView()));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Subscriptions")
	@GetMapping(path = SysMonitorConstants.HTTP_API_OP_SUBSCRIPTIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> subscriptions(final HttpServletRequest request) {
		Responses.caller(request);
		return Responses.json(HttpStatus.OK, map("defaultSubscription", monitoring.getDefaultSubscription(), "list", monitoring.subscriptionsView()));
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Creates or replaces the subscription of a provider (If-Match: expected revision)")
	@PutMapping(path = SysMonitorConstants.HTTP_API_OP_SUBSCRIPTION_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> subscribe(
			final HttpServletRequest request,
			@PathVariable final String providerSystem,
			@RequestHeader(name = HttpHeaders.IF_MATCH, required = false) final String ifMatch,
			@RequestBody(required = false) final String body) {
		final String actor = Responses.caller(request);
		final String origin = Responses.origin(request);
		try {
			final Map<String, Object> payload = body == null || body.isBlank() ? map() : obj(parse(body));
			final Integer expected = ifMatch == null ? null : Integer.valueOf(ifMatch.replace("\"", ""));
			final List<String> types = strings(payload.getOrDefault("observationTypes", List.of("*")));
			final Map<String, Object> subscription = store.put(providerSystem, types, !Boolean.FALSE.equals(payload.get("enabled")), actor, expected);
			store.audit(actor, "subscribe", providerSystem, json(subscription.get("observationTypes")));
			monitoring.applySubscriptions();
			return ResponseEntity.ok()
					.cacheControl(CacheControl.noStore())
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.ETAG, str(subscription.get("revision")))
					.body(subscription);
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		} catch (final ArrowheadException | SubscriptionConflictException ex) {
			throw ex;
		} catch (final Exception ex) {
			throw new InternalServerError("Internal service failure", origin, ex);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Removes the subscription of a provider (the default subscription applies again)")
	@DeleteMapping(path = SysMonitorConstants.HTTP_API_OP_SUBSCRIPTION_PATH)
	public ResponseEntity<Void> unsubscribe(final HttpServletRequest request, @PathVariable final String providerSystem) {
		final String actor = Responses.caller(request);
		final String origin = Responses.origin(request);
		if (!store.delete(providerSystem)) {
			throw new DataNotFoundException("Unknown subscription", origin);
		}

		store.audit(actor, "unsubscribe", providerSystem, "accepted");
		try {
			monitoring.applySubscriptions();
		} catch (final Exception ex) {
			throw new InternalServerError("Internal service failure", origin, ex);
		}

		return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
	}

	//-------------------------------------------------------------------------------------------------
	@Operation(summary = "Orchestrates the monitored providers again and re-applies the bindings")
	@PostMapping(path = SysMonitorConstants.HTTP_API_OP_RECONCILE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> reconcile(final HttpServletRequest request) {
		final String actor = Responses.caller(request);
		final String origin = Responses.origin(request);
		try {
			monitoring.orchestrate();
		} catch (final ArrowheadException ex) {
			monitoring.orchestrationFailed();
			throw ex;
		} catch (final Exception ex) {
			monitoring.orchestrationFailed();
			throw new InternalServerError("Internal service failure", origin, ex);
		}

		store.audit(actor, "POST", "reconcile", "accepted");
		return Responses.json(HttpStatus.ACCEPTED, map("jobId", "reconcile-" + System.currentTimeMillis() / 1000));
	}

	//-------------------------------------------------------------------------------------------------
	@ExceptionHandler(SubscriptionConflictException.class)
	public ResponseEntity<ErrorMessageDTO> conflict(final SubscriptionConflictException ex, final HttpServletRequest request) {
		return Responses.json(HttpStatus.CONFLICT, new ErrorMessageDTO(ex.getMessage(), HttpStatus.CONFLICT.value(), null, Responses.origin(request)));
	}
}
