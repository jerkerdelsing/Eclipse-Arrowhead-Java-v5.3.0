/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.graphdbmanager.api.http;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.graphdbmanager.GraphDBManagerConstants;
import eu.arrowhead.graphdbmanager.api.dto.SenMLRecordDTO;
import eu.arrowhead.graphdbmanager.api.dto.ServiceListResponseDTO;
import eu.arrowhead.graphdbmanager.api.dto.SystemListResponseDTO;
import eu.arrowhead.graphdbmanager.service.HistorianService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(GraphDBManagerConstants.HTTP_API_DATA_HISTORIAN_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class DataHistorianAPI {

	private final HistorianService historianService;

	public DataHistorianAPI(final HistorianService historianService) {
		this.historianService = historianService;
	}

	@Operation(summary = "Lists systems with historical data")
	@GetMapping(path = GraphDBManagerConstants.HTTP_API_OP_SYSTEMS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody SystemListResponseDTO getSystems() {
		return new SystemListResponseDTO(historianService.getSystems());
	}

	@Operation(summary = "Lists historically stored services of a system")
	@GetMapping(path = GraphDBManagerConstants.HTTP_API_OP_SERVICES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ServiceListResponseDTO getServices(@PathVariable final String systemName) {
		return new ServiceListResponseDTO(historianService.getServices(systemName));
	}

	@Operation(summary = "Queries historical SenML data")
	@GetMapping(path = GraphDBManagerConstants.HTTP_API_OP_DATA_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody List<SenMLRecordDTO> getData(
			@PathVariable final String systemName,
			@PathVariable final String serviceName,
			@RequestParam(required = false) final Double from,
			@RequestParam(required = false) final Double to,
			@RequestParam(required = false) final Integer count,
			@RequestParam(required = false) final List<String> signals,
			@RequestParam(required = false) final Integer signalCount) {
		final String origin = origin(HttpMethod.GET, systemName, serviceName);
		return historianService.get(systemName, serviceName, from, to, count, signals, signalCount, origin);
	}

	@Operation(summary = "Persists a SenML pack")
	@PutMapping(path = GraphDBManagerConstants.HTTP_API_OP_DATA_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> putData(
			@PathVariable final String systemName,
			@PathVariable final String serviceName,
			@RequestBody(required = false) final List<SenMLRecordDTO> records) {
		final String origin = origin(HttpMethod.PUT, systemName, serviceName);
		historianService.put(systemName, serviceName, records, origin);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	private String origin(final HttpMethod method, final String systemName, final String serviceName) {
		return method.name() + " " + GraphDBManagerConstants.HTTP_API_DATA_HISTORIAN_PATH
				+ GraphDBManagerConstants.HTTP_API_OP_DATA_PATH
						.replace(GraphDBManagerConstants.HTTP_PATH_PARAM_SYSTEM_NAME, systemName)
						.replace(GraphDBManagerConstants.HTTP_PATH_PARAM_SERVICE_NAME, serviceName);
	}
}
