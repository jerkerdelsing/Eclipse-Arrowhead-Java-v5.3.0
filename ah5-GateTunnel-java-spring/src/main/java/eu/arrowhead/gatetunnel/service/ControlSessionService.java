/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.service;

import static eu.arrowhead.gatetunnel.service.Wire.id;
import static eu.arrowhead.gatetunnel.service.Wire.json;
import static eu.arrowhead.gatetunnel.service.Wire.m;
import static eu.arrowhead.gatetunnel.service.Wire.parse;
import static eu.arrowhead.gatetunnel.service.Wire.str;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.arrowhead.gatetunnel.jpa.entity.Session;
import eu.arrowhead.gatetunnel.jpa.service.SessionDbService;
import eu.arrowhead.gatetunnel.service.dto.ControlSessionDTO;
import eu.arrowhead.gatetunnel.service.normalization.GateTunnelNormalization;
import eu.arrowhead.gatetunnel.service.validation.GateTunnelValidation;

// The 4.x-compatible control sessions (connect_provider, connect_consumer, close_sessions) and the session management.
@Service
public class ControlSessionService {

	//=================================================================================================
	// members

	@Autowired
	private SessionDbService dbService;

	@Autowired
	private InterCloudSessionService interCloud;

	@Autowired
	private FederationSettings settings;

	@Autowired
	private GateTunnelValidation validation;

	@Autowired
	private GateTunnelNormalization normalization;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> connectProvider(final Map<String, Object> payload) {
		validation.validateControlSessionCreate(payload);
		final Session template = new Session(id(), str(payload.getOrDefault("queueId", "gatetunnel." + id())), str(payload.getOrDefault("peerName", "GateTunnel-" + id())),
				str(payload.get("serviceDefinition")), 0, Session.STATE_PROVIDER_READY, json(payload.get("consumer")), json(payload.get("provider")),
				json(payload.get("consumerCloud")), json(payload.get("providerCloud")), json(payload.get("relay")));
		final Session created = dbService.create(template, settings.getControlMinPort(), settings.getControlMaxPort());

		return m("sessionId", created.getSessionId(), "queueId", created.getQueueId(), "peerName", created.getPeerName(), "serverPort", created.getTunnelPort(), "state", created.getState());
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> connectConsumer(final Map<String, Object> payload) {
		final String queueId = str(payload.get("queueId"));
		validation.validateQueueId(queueId);
		final Session session = dbService.activate(queueId);

		return m("sessionId", session.getSessionId(), "serverPort", session.getTunnelPort(), "state", session.getState());
	}

	//-------------------------------------------------------------------------------------------------
	public List<Object> closeSessions(final Object raw) {
		final List<Object> errors = new ArrayList<>();
		for (final int port : normalization.normalizePorts(raw)) {
			if (!dbService.close(null, port)) {
				errors.add(m("port", port, "error", "Active session not found"));
			}
		}

		return errors;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Object> list(final String rawLimit) {
		final List<ControlSessionDTO> rows = dbService.list(normalization.normalizeLimit(rawLimit)).stream().map(ControlSessionService::view).toList();
		return m("entries", rows, "count", rows.size());
	}

	//-------------------------------------------------------------------------------------------------
	// Closes a control session (by id, else by tunnelPort) and an inter-cloud session of that id; true if any was open.
	public boolean close(final Map<String, Object> payload) {
		final String sessionId = str(payload.get("sessionId"));
		boolean changed = dbService.close(sessionId, Wire.integer(payload.get("tunnelPort"), 0));
		if (interCloud.close(sessionId)) {
			changed = true;
		}

		return changed;
	}

	//-------------------------------------------------------------------------------------------------
	static ControlSessionDTO view(final Session s) {
		return new ControlSessionDTO(s.getSessionId(), s.getQueueId(), s.getPeerName(), s.getServiceDefinition(), s.getTunnelPort(), s.getState(), parse(s.getConsumerJson()),
				parse(s.getProviderJson()), parse(s.getConsumerCloudJson()), parse(s.getProviderCloudJson()), parse(s.getRelayJson()),
				s.getCreatedAt() == null ? 0 : s.getCreatedAt().toInstant().toEpochMilli() / 1000.0);
	}
}
