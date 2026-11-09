/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.datamanager.api.http;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.datamanager.DataManagerConstants;
import eu.arrowhead.datamanager.api.dto.SenMLRecordDTO;
import eu.arrowhead.datamanager.api.dto.ServiceListResponseDTO;
import eu.arrowhead.datamanager.api.dto.SystemListResponseDTO;
import eu.arrowhead.datamanager.service.ProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(DataManagerConstants.HTTP_API_DATA_PROXY_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class DataProxyAPI {

	private final ProxyService proxyService;

	public DataProxyAPI(final ProxyService proxyService) {
		this.proxyService = proxyService;
	}

	@Operation(summary = "Lists systems with data in the proxy cache")
	@GetMapping(path = DataManagerConstants.HTTP_API_OP_SYSTEMS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody SystemListResponseDTO getSystems() {
		return new SystemListResponseDTO(proxyService.getSystems());
	}

	@Operation(summary = "Lists cached services of a system")
	@GetMapping(path = DataManagerConstants.HTTP_API_OP_SERVICES_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody ServiceListResponseDTO getServices(@PathVariable final String systemName) {
		return new ServiceListResponseDTO(proxyService.getServices(systemName));
	}

	@Operation(summary = "Returns the latest cached SenML pack")
	@GetMapping(path = DataManagerConstants.HTTP_API_OP_DATA_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody List<SenMLRecordDTO> getData(
			@PathVariable final String systemName,
			@PathVariable final String serviceName) {
		final String origin = origin(HttpMethod.GET, systemName, serviceName);
		return proxyService.get(systemName, serviceName, origin);
	}

	@Operation(summary = "Stores the latest SenML pack in the proxy cache")
	@PutMapping(path = DataManagerConstants.HTTP_API_OP_DATA_PATH, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> putData(
			@PathVariable final String systemName,
			@PathVariable final String serviceName,
			@RequestBody(required = false) final List<SenMLRecordDTO> records) {
		final String origin = origin(HttpMethod.PUT, systemName, serviceName);
		proxyService.put(systemName, serviceName, records, origin);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	private String origin(final HttpMethod method, final String systemName, final String serviceName) {
		return method.name() + " " + DataManagerConstants.HTTP_API_DATA_PROXY_PATH
				+ DataManagerConstants.HTTP_API_OP_DATA_PATH
						.replace(DataManagerConstants.HTTP_PATH_PARAM_SYSTEM_NAME, systemName)
						.replace(DataManagerConstants.HTTP_PATH_PARAM_SERVICE_NAME, serviceName);
	}
}
