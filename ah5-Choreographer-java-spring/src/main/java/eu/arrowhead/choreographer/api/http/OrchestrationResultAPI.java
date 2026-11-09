/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.api.http;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.choreographer.ChoreographerConstants;
import eu.arrowhead.choreographer.engine.OrchestrationResultBroker;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import io.swagger.v3.oas.annotations.Hidden;

// Internal callback: DynamicServiceOrchestration pushes the orchestration result of a push subscription here.
// It is not an Arrowhead service; access is checked by OrchestrationCallbackFilter.
@Hidden
@RestController
@RequestMapping(ChoreographerConstants.HTTP_API_ORCHESTRATION_RESULT_PATH)
public class OrchestrationResultAPI {

	@Autowired
	private OrchestrationResultBroker broker;

	private final Logger logger = LogManager.getLogger(this.getClass());

	@PostMapping(path = "/{correlationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> receive(@PathVariable final String correlationId, @RequestBody final OrchestrationResponseDTO dto) {
		logger.debug("orchestration result received for {}", correlationId);
		return new ResponseEntity<>(broker.complete(correlationId, dto) ? HttpStatus.OK : HttpStatus.NOT_FOUND);
	}
}
