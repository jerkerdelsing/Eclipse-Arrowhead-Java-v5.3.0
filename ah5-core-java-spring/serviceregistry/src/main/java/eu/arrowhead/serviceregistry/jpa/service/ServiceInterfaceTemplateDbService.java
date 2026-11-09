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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.serviceregistry.ServiceRegistryConstants;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplatePropertyRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplateRepository;

@Service
public class ServiceInterfaceTemplateDbService {

	//=================================================================================================
	// members

	@Autowired
	private ServiceInterfaceTemplateRepository templateRepo;

	@Autowired
	private ServiceInterfaceTemplatePropertyRepository templatePropsRepo;

	private static final Object LOCK = new Object();

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public Optional<ServiceInterfaceTemplate> getByName(final String name) {
		logger.debug("getByName started");
		Assert.isTrue(!Utilities.isEmpty(name), "name is empty");

		try {
			return templateRepo.findByName(name);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInterfaceTemplateProperty> getPropertiesByTemplateName(final String name) {
		logger.debug("getPropertiesByTemplateName started");
		Assert.isTrue(!Utilities.isEmpty(name), "name is empty");

		try {
			final Optional<ServiceInterfaceTemplate> templateOpt = templateRepo.findByName(name);

			if (templateOpt.isPresent()) {
				return templatePropsRepo.findByServiceInterfaceTemplate(templateOpt.get());
			}

			return List.of();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	public Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> getPageByFilters(final PageRequest pagination, final List<String> templateNames, final List<String> protocols) {
		logger.debug("getPageByFilters started");
		Assert.notNull(pagination, "page is null");

		try {
			synchronized (LOCK) {
				final boolean hasFilters = !Utilities.isEmpty(templateNames) || !Utilities.isEmpty(protocols);

				List<ServiceInterfaceTemplate> templates;
				if (!hasFilters) {
					templates = templateRepo.findAll();
				} else if (!Utilities.isEmpty(templateNames)) {
					templates = templateRepo.findAllByNameIn(templateNames);
				} else {
					templates = templateRepo.findAllByProtocolIn(protocols);
				}

				final Map<Long, ServiceInterfaceTemplate> matchings = new HashMap<>();
				for (final ServiceInterfaceTemplate template : templates) {
					if (!hasFilters) {
						matchings.put(template.getId(), template);
						continue;
					}

					if (!Utilities.isEmpty(templateNames) && !templateNames.contains(template.getName())) {
						continue;
					}

					if (!Utilities.isEmpty(protocols) && !protocols.contains(template.getProtocol())) {
						continue;
					}

					matchings.put(template.getId(), template);
				}

				final List<ServiceInterfaceTemplateProperty> properties = templatePropsRepo.findAllByServiceInterfaceTemplateIn(matchings.values());
				final Page<ServiceInterfaceTemplate> templatePage = templateRepo.findAllByIdIn(matchings.keySet(), pagination);

				final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> results = new LinkedHashMap<>(templatePage.getSize());
				for (final ServiceInterfaceTemplate template : templatePage) {
					results.putIfAbsent(template, new ArrayList<>());
					results.get(template).addAll(properties.stream().filter(prop -> prop.getServiceInterfaceTemplate().getId() == template.getId()).toList());
				}

				return new PageImpl<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>>(
						new ArrayList<>(results.entrySet()),
						pagination,
						templatePage.getTotalElements());
			}
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> createBulk(final List<ServiceInterfaceTemplateRequestDTO> candidates) {
		logger.debug("createBulk started");
		Assert.isTrue(!Utilities.isEmpty(candidates), "interface template candidate list is empty");

		try {
			final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> results = new HashMap<>(candidates.size());

			final Map<String, ServiceInterfaceTemplate> templateEntries = new HashMap<>(candidates.size());
			for (final ServiceInterfaceTemplateRequestDTO candidate : candidates) {
				if (templateEntries.containsKey(candidate.name())) {
					throw new InvalidParameterException("Duplicated interface template name: " + candidate.name());
				}

				if (templateRepo.existsByName(candidate.name())) {
					throw new InvalidParameterException("Interface template already exists: " + candidate.name());
				}

				templateEntries.put(candidate.name(), new ServiceInterfaceTemplate(candidate.name(), candidate.protocol()));
			}

			templateRepo.saveAllAndFlush(templateEntries.values()).forEach(template -> {
				templateEntries.put(template.getName(), template);
				results.put(template, new ArrayList<>());
			});

			final List<ServiceInterfaceTemplateProperty> properties = new ArrayList<>();
			for (final ServiceInterfaceTemplateRequestDTO candidate : candidates) {
				final ServiceInterfaceTemplate template = templateEntries.get(candidate.name());
				for (final ServiceInterfaceTemplatePropertyDTO property : candidate.propertyRequirements()) {
					String validator = Utilities.isEmpty(property.validator()) ? null : property.validator();
					if (!Utilities.isEmpty(property.validatorParams())) {
						validator = validator + ServiceRegistryConstants.INTERFACE_PROPERTY_VALIDATOR_DELIMITER
								+ property.validatorParams().stream().collect(Collectors.joining(ServiceRegistryConstants.INTERFACE_PROPERTY_VALIDATOR_DELIMITER));
					}

					properties.add(new ServiceInterfaceTemplateProperty(template, property.name(), property.mandatory(), validator));
				}
			}

			templatePropsRepo.saveAllAndFlush(properties).forEach(property -> results.get(property.getServiceInterfaceTemplate()).add(property));

			return results;
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void deleteByTemplateNameList(final Collection<String> templateNames) {
		logger.debug("deleteByTemplateNameList started");
		Assert.isTrue(!Utilities.isEmpty(templateNames), "name list is empty");

		try {
			final List<ServiceInterfaceTemplate> entities = templateRepo.findAllByNameIn(templateNames);
			templateRepo.deleteAll(entities);
			templateRepo.flush();
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}
}