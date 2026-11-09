/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.service;

import java.util.List;
import java.util.Map;

import eu.arrowhead.dto.ServiceDefinitionResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.ConnectionDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeSystemDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PortDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.SystemPortDTO;

final class PlantFixtures {

	static PortDTO producer(final String name, final String definition, final Map<String, String> metadata) {
		return new PortDTO(name, definition, null, metadata, false);
	}

	static PortDTO consumer(final String name, final String definition) {
		return new PortDTO(name, definition, null, null, true);
	}

	static PdeSystemDTO system(final String id, final String name, final PortDTO... ports) {
		return new PdeSystemDTO(id, name, null, List.of(ports));
	}

	static ConnectionDTO connect(final String consumerId, final String consumerPort, final String producerId, final String producerPort, final Integer priority) {
		return new ConnectionDTO(new SystemPortDTO(consumerId, consumerPort), new SystemPortDTO(producerId, producerPort), priority);
	}

	static SystemResponseDTO registered(final String name, final Map<String, Object> metadata) {
		return new SystemResponseDTO(name, metadata, "1.0.0", List.of(), null, null, null);
	}

	static ServiceInstanceResponseDTO instance(final String id, final String provider, final String definition, final Map<String, Object> metadata) {
		return new ServiceInstanceResponseDTO(id, registered(provider, null), new ServiceDefinitionResponseDTO(definition, null, null), "1.0.0", null, metadata,
				List.of(new ServiceInstanceInterfaceResponseDTO("generic_https", "https", "CERT_AUTH", Map.of())), null, null);
	}

	private PlantFixtures() {
	}
}
