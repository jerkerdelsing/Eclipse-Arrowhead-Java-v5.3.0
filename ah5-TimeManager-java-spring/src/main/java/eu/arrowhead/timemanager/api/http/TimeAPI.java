/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.timemanager.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.timemanager.TimeManagerConstants;
import eu.arrowhead.timemanager.api.dto.TimeResponseDTO;
import eu.arrowhead.timemanager.service.TimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(TimeManagerConstants.HTTP_API_BASE_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class TimeAPI {

	@Autowired
	private TimeService timeService;

	@Operation(summary = "Returns the current time, the configured time zone and whether the time is trusted (time.get-time)")
	@GetMapping(path = TimeManagerConstants.HTTP_API_OP_TIME_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody TimeResponseDTO getTime() {
		return timeService.getTime();
	}
}
