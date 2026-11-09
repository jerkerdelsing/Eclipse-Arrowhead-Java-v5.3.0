/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.service;

import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.configuration.ConfigurationConstants;
import eu.arrowhead.configuration.api.dto.ConfigurationListResponseDTO;
import eu.arrowhead.configuration.api.dto.ConfigurationRequestDTO;
import eu.arrowhead.configuration.api.dto.ConfigurationResponseDTO;
import eu.arrowhead.configuration.jpa.entity.ConfigurationData;
import eu.arrowhead.configuration.jpa.repository.ConfigurationDataRepository;

@Service
public class ConfigurationService {

	private static final int MAX_FILE_NAME_LENGTH = 255;

	private final Logger logger = LogManager.getLogger(this.getClass());

	private final ConfigurationDataRepository repository;
	private final SystemNameNormalizer systemNameNormalizer;
	private final SystemNameValidator systemNameValidator;
	private final int maxSize;

	@Autowired
	public ConfigurationService(
			final ConfigurationDataRepository repository,
			final SystemNameNormalizer systemNameNormalizer,
			final SystemNameValidator systemNameValidator,
			@Value(ConfigurationConstants.$CONFIGURATION_MAX_SIZE_WD) final int maxSize) {
		this.repository = repository;
		this.systemNameNormalizer = systemNameNormalizer;
		this.systemNameValidator = systemNameValidator;
		this.maxSize = maxSize;
	}

	// Arrowhead 5 system names are case-sensitive PascalCase names; v4.6 lower-cased them.
	public String normalizeSystemName(final String systemName, final String origin) {
		if (Utilities.isEmpty(systemName)) {
			throw new InvalidParameterException("System name is missing", origin);
		}
		final String normalized = systemNameNormalizer.normalize(systemName);
		try {
			systemNameValidator.validateSystemName(normalized);
		} catch (final InvalidParameterException ex) {
			throw new InvalidParameterException(ex.getMessage(), origin);
		}
		return normalized;
	}

	@Transactional(readOnly = true)
	public ConfigurationResponseDTO getConfiguration(final String systemName, final String origin) {
		return toDTO(find(systemName, origin));
	}

	@Transactional(readOnly = true)
	public ConfigurationData getConfigurationData(final String systemName, final String origin) {
		return find(systemName, origin);
	}

	@Transactional(readOnly = true)
	public ConfigurationListResponseDTO listConfigurations() {
		final List<ConfigurationResponseDTO> list = repository.findAll().stream()
				.sorted(Comparator.comparing(ConfigurationData::getSystemName))
				.map(this::toDTO)
				.toList();
		return new ConfigurationListResponseDTO(list.size(), list);
	}

	// pathSystemName is null for the v4.6 save-config operation, where the system is named only in the body.
	@Transactional
	public ConfigurationResponseDTO storeConfiguration(final String pathSystemName, final ConfigurationRequestDTO request, final String origin) {
		if (request == null) {
			throw new InvalidParameterException("Request body is missing", origin);
		}
		final String systemName = normalizeSystemName(request.systemName(), origin);
		if (pathSystemName != null && !normalizeSystemName(pathSystemName, origin).equals(systemName)) {
			throw new InvalidParameterException("System name in the path and in the request differ", origin);
		}
		final String fileName = validateFileName(request.fileName(), origin);
		final String contentType = validateContentType(request.contentType(), origin);
		final byte[] content = decode(request.data(), origin);

		final ConfigurationData entity = repository.findBySystemName(systemName)
				.map(existing -> {
					existing.setFileName(fileName);
					existing.setContentType(contentType);
					existing.setData(content);
					return existing;
				})
				.orElseGet(() -> new ConfigurationData(systemName, fileName, contentType, content));
		final ConfigurationData saved = repository.saveAndFlush(entity);
		logger.info("Configuration {} stored for {} ({} bytes, {})", fileName, systemName, content.length, contentType);
		return toDTO(saved);
	}

	@Transactional
	public ConfigurationResponseDTO removeConfiguration(final String systemName, final String origin) {
		final ConfigurationData entity = find(systemName, origin);
		final ConfigurationResponseDTO removed = toDTO(entity);
		repository.delete(entity);
		repository.flush();
		logger.info("Configuration removed for {}", entity.getSystemName());
		return removed;
	}

	public long count() {
		return repository.count();
	}

	private ConfigurationData find(final String systemName, final String origin) {
		final String normalized = normalizeSystemName(systemName, origin);
		final Optional<ConfigurationData> entity = repository.findBySystemName(normalized);
		return entity.orElseThrow(() -> new DataNotFoundException("No configuration stored for " + normalized, origin));
	}

	private String validateFileName(final String fileName, final String origin) {
		if (Utilities.isEmpty(fileName)) {
			throw new InvalidParameterException("fileName is missing", origin);
		}
		final String trimmed = fileName.trim();
		if (trimmed.length() > MAX_FILE_NAME_LENGTH || trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("\"")
				|| trimmed.chars().anyMatch(Character::isISOControl) || ".".equals(trimmed) || "..".equals(trimmed)) {
			throw new InvalidParameterException("fileName must be a plain file name of at most " + MAX_FILE_NAME_LENGTH + " characters", origin);
		}
		return trimmed;
	}

	private String validateContentType(final String contentType, final String origin) {
		if (Utilities.isEmpty(contentType)) {
			throw new InvalidParameterException("contentType is missing", origin);
		}
		try {
			final MediaType mediaType = MediaType.parseMediaType(contentType.trim());
			if (mediaType.isWildcardType() || mediaType.isWildcardSubtype()) {
				throw new InvalidParameterException("contentType must be a concrete media type", origin);
			}
			return mediaType.toString();
		} catch (final InvalidMediaTypeException ex) {
			throw new InvalidParameterException("contentType is not a valid media type", origin);
		}
	}

	private byte[] decode(final String data, final String origin) {
		if (Utilities.isEmpty(data)) {
			throw new InvalidParameterException("data is missing", origin);
		}
		final byte[] content;
		try {
			content = Base64.getDecoder().decode(data.trim());
		} catch (final IllegalArgumentException ex) {
			throw new InvalidParameterException("data is not valid Base64", origin);
		}
		if (content.length == 0) {
			throw new InvalidParameterException("data is empty", origin);
		}
		if (content.length > maxSize) {
			throw new InvalidParameterException("Configuration exceeds " + maxSize + " bytes", origin);
		}
		return content;
	}

	private ConfigurationResponseDTO toDTO(final ConfigurationData entity) {
		return new ConfigurationResponseDTO(
				entity.getId(),
				entity.getSystemName(),
				entity.getFileName(),
				entity.getContentType(),
				Base64.getEncoder().encodeToString(entity.getData()),
				entity.getData().length,
				Utilities.convertZonedDateTimeToUTCString(entity.getCreatedAt()),
				Utilities.convertZonedDateTimeToUTCString(entity.getUpdatedAt()));
	}
}
