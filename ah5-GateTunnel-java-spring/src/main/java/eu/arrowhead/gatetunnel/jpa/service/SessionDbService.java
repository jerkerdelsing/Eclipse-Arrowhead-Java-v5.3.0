/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatetunnel.jpa.service;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.gatetunnel.jpa.entity.Session;
import eu.arrowhead.gatetunnel.jpa.repository.SessionRepository;
import eu.arrowhead.gatetunnel.service.GateTunnelApiException;

@Service
public class SessionDbService {

	//=================================================================================================
	// members

	@Autowired
	private SessionRepository repository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(readOnly = true)
	public List<Session> list(final int limit) {
		return repository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, limit));
	}

	//-------------------------------------------------------------------------------------------------
	// Reserves the lowest free port of the control pool; every stored session (also a closed one) holds its port.
	// Serialized in the JVM; the unique tunnel_port column refuses a concurrent duplicate from elsewhere.
	@Transactional
	public synchronized Session create(final Session template, final int minPort, final int maxPort) {
		final Set<Integer> used = new HashSet<>();
		repository.findAll().forEach(s -> used.add(s.getTunnelPort()));
		int port = 0;
		for (int candidate = minPort; candidate <= maxPort; candidate++) {
			if (!used.contains(candidate)) {
				port = candidate;
				break;
			}
		}
		if (port == 0) {
			throw new GateTunnelApiException(503, "No control ports available");
		}

		final Session session = new Session(template.getSessionId(), template.getQueueId(), template.getPeerName(), template.getServiceDefinition(), port,
				Session.STATE_PROVIDER_READY, template.getConsumerJson(), template.getProviderJson(), template.getConsumerCloudJson(), template.getProviderCloudJson(),
				template.getRelayJson());

		return repository.saveAndFlush(session);
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional
	public Session activate(final String queueId) {
		final Optional<Session> found = repository.findByQueueIdAndStateNot(queueId, Session.STATE_CLOSED);
		if (found.isEmpty()) {
			throw new GateTunnelApiException(404, "Session not found");
		}

		final Session session = found.get();
		session.setState(Session.STATE_ACTIVE);

		return repository.saveAndFlush(session);
	}

	//-------------------------------------------------------------------------------------------------
	// Closes the open session with that id (if given), otherwise the one on that port; true if one was open.
	@Transactional
	public boolean close(final String sessionId, final int port) {
		final Optional<Session> found = sessionId != null && !sessionId.isBlank()
				? repository.findBySessionIdAndStateNot(sessionId, Session.STATE_CLOSED)
				: repository.findByTunnelPortAndStateNot(port, Session.STATE_CLOSED);
		if (found.isEmpty()) {
			return false;
		}

		found.get().setState(Session.STATE_CLOSED);
		repository.saveAndFlush(found.get());

		return true;
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(readOnly = true)
	public long countOpen() {
		return repository.findAll().stream().filter(s -> !Session.STATE_CLOSED.equals(s.getState())).count();
	}

	//-------------------------------------------------------------------------------------------------
	// Removes sessions closed before the threshold, which frees their ports.
	@Transactional
	public int purgeClosed(final ZonedDateTime threshold) {
		final List<Session> old = repository.findByStateAndUpdatedAtBefore(Session.STATE_CLOSED, threshold);
		repository.deleteAll(old);

		return old.size();
	}
}
