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
package eu.arrowhead.serviceregistry.jpa.service;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceDefinitionRepository;

@Service
public class ServiceDefinitionDbService {

	//=================================================================================================
	// members

	@Autowired
	private ServiceDefinitionRepository repo;

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public List<ServiceDefinition> createBulk(final List<String> names) {
		logger.debug("createBulk started...");
		Assert.isTrue(!Utilities.isEmpty(names), "service definition name list is empty");

		try {
			final List<ServiceDefinition> existing = repo.findAllByNameIn(names);
			if (!Utilities.isEmpty(existing)) {
				final String existingNames = existing.stream()
						.map(e -> e.getName())
						.collect(Collectors.joining(", "));
				throw new InvalidParameterException(
						"Service definition names already exists: " + existingNames);
			}

			final List<ServiceDefinition> entities = names.stream()
					.map(n -> new ServiceDefinition(n))
					.collect(Collectors.toList());

			return repo.saveAllAndFlush(entities);
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<ServiceDefinition> getPage(final PageRequest pagination) {
		logger.debug("getPage started...");
		Assert.notNull(pagination, "pagination is null");

		try {
			return repo.findAll(pagination);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void removeBulk(final List<String> names) {
		logger.debug("removeBulk started...");

		try {
			final List<ServiceDefinition> entries = repo.findAllByNameIn(names);
			repo.deleteAll(entries);
			repo.flush();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}