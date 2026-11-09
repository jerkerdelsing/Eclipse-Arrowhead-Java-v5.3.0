/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.configuration.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.DataNotFoundException;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameValidator;
import eu.arrowhead.configuration.api.dto.ConfigurationRequestDTO;
import eu.arrowhead.configuration.api.dto.ConfigurationResponseDTO;
import eu.arrowhead.configuration.jpa.entity.ConfigurationData;
import eu.arrowhead.configuration.jpa.repository.ConfigurationDataRepository;

@ExtendWith(MockitoExtension.class)
public class ConfigurationServiceTest {

	private static final String ORIGIN = "test";

	@Mock
	private ConfigurationDataRepository repository;

	@Mock
	private SystemNameNormalizer normalizer;

	@Mock
	private SystemNameValidator validator;

	private final Map<String, ConfigurationData> store = new LinkedHashMap<>();
	private long nextId = 1;
	private ConfigurationService service;

	@BeforeEach
	void init() {
		service = new ConfigurationService(repository, normalizer, validator, 16);
		lenient().when(normalizer.normalize(anyString())).thenAnswer(invocation -> ((String) invocation.getArgument(0)).trim());
		lenient().doAnswer(invocation -> {
			final String name = invocation.getArgument(0);
			if (!name.matches("[A-Z][A-Za-z0-9]*")) {
				throw new InvalidParameterException("Invalid system name: " + name);
			}
			return null;
		}).when(validator).validateSystemName(anyString());
		lenient().when(repository.findBySystemName(anyString())).thenAnswer(invocation -> Optional.ofNullable(store.get((String) invocation.getArgument(0))));
		lenient().when(repository.saveAndFlush(any())).thenAnswer(invocation -> {
			final ConfigurationData entity = invocation.getArgument(0);
			if (entity.getId() == 0) {
				ReflectionTestUtils.setField(entity, "id", nextId++);
				entity.onCreate();
			} else {
				entity.onUpdate();
			}
			store.put(entity.getSystemName(), entity);
			return entity;
		});
		lenient().when(repository.findAll()).thenAnswer(invocation -> new ArrayList<>(store.values()));
		lenient().doAnswer(invocation -> store.remove(((ConfigurationData) invocation.getArgument(0)).getSystemName())).when(repository).delete(any());
		lenient().when(repository.count()).thenAnswer(invocation -> (long) store.size());
	}

	private static ConfigurationRequestDTO request(final String system, final String file, final String type, final String content) {
		return new ConfigurationRequestDTO(system, file, type, Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void storesAndReturnsTheFileNameV46DidNotReturn() {
		final ConfigurationResponseDTO stored = service.storeConfiguration("Thermostat", request("Thermostat", "thermostat.json", "application/json", "{\"setpoint\":21}"), ORIGIN);
		assertEquals("thermostat.json", stored.fileName());
		assertEquals("application/json", stored.contentType());
		assertEquals(15, stored.size());
		final ConfigurationResponseDTO read = service.getConfiguration("Thermostat", ORIGIN);
		assertEquals("thermostat.json", read.fileName());
		assertEquals("{\"setpoint\":21}", new String(Base64.getDecoder().decode(read.data()), StandardCharsets.UTF_8));
		assertArrayEquals("{\"setpoint\":21}".getBytes(StandardCharsets.UTF_8), service.getConfigurationData("Thermostat", ORIGIN).getData());
	}

	@Test
	public void replacesTheConfigurationOfASystem() {
		final long id = service.storeConfiguration(null, request("Thermostat", "a.txt", "text/plain", "one"), ORIGIN).id();
		final ConfigurationResponseDTO replaced = service.storeConfiguration(null, request("Thermostat", "b.ini", "text/plain;charset=utf-8", "two"), ORIGIN);
		assertEquals(id, replaced.id());
		assertEquals("b.ini", replaced.fileName());
		assertEquals("text/plain;charset=utf-8", replaced.contentType());
		assertEquals(1, service.listConfigurations().count());
	}

	@Test
	public void keepsSystemNamesCaseSensitive() {
		service.storeConfiguration(null, request("ThermoStat", "a.txt", "text/plain", "x"), ORIGIN);
		assertThrows(DataNotFoundException.class, () -> service.getConfiguration("Thermostat", ORIGIN));
		assertEquals("ThermoStat", service.getConfiguration("ThermoStat", ORIGIN).systemName());
	}

	@Test
	public void rejectsInvalidRequests() {
		assertThrows(InvalidParameterException.class, () -> service.storeConfiguration("Other", request("Thermostat", "a.txt", "text/plain", "x"), ORIGIN));
		assertThrows(InvalidParameterException.class, () -> service.storeConfiguration(null, request("thermostat-1", "a.txt", "text/plain", "x"), ORIGIN));
		assertThrows(InvalidParameterException.class, () -> service.storeConfiguration(null, new ConfigurationRequestDTO("Thermostat", "a.txt", "text/plain", "%%%"), ORIGIN));
		assertThrows(InvalidParameterException.class, () -> service.storeConfiguration(null, request("Thermostat", "a.txt", "text/plain", "this content is longer than 16 bytes"), ORIGIN));
		assertThrows(InvalidParameterException.class, () -> service.storeConfiguration(null, request("Thermostat", "../a.txt", "text/plain", "x"), ORIGIN));
		assertThrows(InvalidParameterException.class, () -> service.storeConfiguration(null, request("Thermostat", "a.txt", "text/*", "x"), ORIGIN));
		assertThrows(InvalidParameterException.class, () -> service.storeConfiguration(null, request("Thermostat", "a.txt", "not a type", "x"), ORIGIN));
		assertThrows(InvalidParameterException.class, () -> service.storeConfiguration(null, new ConfigurationRequestDTO("Thermostat", "a.txt", "text/plain", ""), ORIGIN));
		assertThrows(InvalidParameterException.class, () -> service.storeConfiguration(null, null, ORIGIN));
		assertTrue(store.isEmpty());
	}

	@Test
	public void listsSortedAndRemovesConfigurations() {
		service.storeConfiguration(null, request("Valve", "v.txt", "text/plain", "v"), ORIGIN);
		service.storeConfiguration(null, request("Pump", "p.txt", "text/plain", "p"), ORIGIN);
		final List<ConfigurationResponseDTO> list = service.listConfigurations().data();
		assertEquals(List.of("Pump", "Valve"), list.stream().map(ConfigurationResponseDTO::systemName).toList());
		assertEquals("p.txt", service.removeConfiguration("Pump", ORIGIN).fileName());
		assertFalse(store.containsKey("Pump"));
		assertThrows(DataNotFoundException.class, () -> service.removeConfiguration("Pump", ORIGIN));
	}

	@Test
	public void healthFollowsTheStore() {
		final ConfigurationHealthIndicator indicator = new ConfigurationHealthIndicator(service);
		assertTrue(indicator.health().healthy());
		when(repository.count()).thenThrow(new IllegalStateException("database down"));
		assertFalse(indicator.health().healthy());
	}
}
