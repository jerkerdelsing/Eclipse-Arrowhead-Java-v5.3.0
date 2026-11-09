/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.gatekeeper.jpa.service;

import java.time.ZonedDateTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.gatekeeper.jpa.entity.Cloud;
import eu.arrowhead.gatekeeper.jpa.entity.Relay;
import eu.arrowhead.gatekeeper.jpa.repository.CloudRepository;
import eu.arrowhead.gatekeeper.jpa.repository.RelayRepository;
import eu.arrowhead.gatekeeper.service.dto.CloudInput;
import eu.arrowhead.gatekeeper.service.dto.RelayInput;

@Service
public class GatekeeperDbService {

	//=================================================================================================
	// members

	@Autowired
	private CloudRepository cloudRepository;

	@Autowired
	private RelayRepository relayRepository;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<Cloud> clouds() {
		return cloudRepository.findAllByOrderByOperatorAscNameAsc();
	}

	//-------------------------------------------------------------------------------------------------
	public List<Relay> relays() {
		return relayRepository.findAllByOrderByNameAsc();
	}

	//-------------------------------------------------------------------------------------------------
	// all rows or none; an existing (name, operator) row keeps its creation time
	@Transactional(rollbackFor = Exception.class)
	public void upsertClouds(final List<CloudInput> rows) {
		logger.debug("upsertClouds started...");
		for (final CloudInput row : rows) {
			final Cloud cloud = cloudRepository.findByNameAndOperator(row.name(), row.operator()).orElseGet(() -> new Cloud(row.name(), row.operator()));
			cloud.setGatekeeperAddress(row.gatekeeperAddress());
			cloud.setGatekeeperPort(row.gatekeeperPort());
			cloud.setMetadata(row.metadata());
			cloudRepository.saveAndFlush(cloud);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = Exception.class)
	public void upsertRelays(final List<RelayInput> rows) {
		logger.debug("upsertRelays started...");
		for (final RelayInput row : rows) {
			final Relay relay = relayRepository.findByName(row.name()).orElseGet(() -> new Relay(row.name()));
			relay.setAddress(row.address());
			relay.setPort(row.port());
			relay.setType(row.type());
			relay.setSecure(row.secure());
			relay.setMetadata(row.metadata());
			relayRepository.saveAndFlush(relay);
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteCloud(final String operator, final String name) {
		return cloudRepository.deleteByOperatorAndName(operator, name) > 0;
	}

	//-------------------------------------------------------------------------------------------------
	// migrate-sqlite: keeps the creation time of the old store; an existing row is left as it is
	@Transactional(rollbackFor = Exception.class)
	public int importRows(final List<CloudInput> clouds, final List<RelayInput> relays, final List<ZonedDateTime> relayCreatedAt) {
		int imported = 0;
		for (final CloudInput row : clouds) {
			if (!cloudRepository.existsByNameAndOperator(row.name(), row.operator())) {
				upsertClouds(List.of(row));
				imported++;
			}
		}

		for (int i = 0; i < relays.size(); i++) {
			final RelayInput row = relays.get(i);
			if (relayRepository.findByName(row.name()).isEmpty()) {
				final Relay relay = new Relay(row.name());
				relay.setAddress(row.address());
				relay.setPort(row.port());
				relay.setType(row.type());
				relay.setSecure(row.secure());
				relay.setMetadata(row.metadata());
				relay.setCreatedAt(relayCreatedAt.get(i));
				relayRepository.saveAndFlush(relay);
				imported++;
			}
		}

		return imported;
	}
}
