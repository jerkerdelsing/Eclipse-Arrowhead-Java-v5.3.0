/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.plantdescriptionengine.PlantDescriptionEngineConstants;
import eu.arrowhead.plantdescriptionengine.api.dto.MonitorPlantDescriptionListResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.MonitorPlantDescriptionResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeAlarmListResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeAlarmResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PdeAlarmUpdateDTO;
import eu.arrowhead.plantdescriptionengine.service.AlarmService;
import eu.arrowhead.plantdescriptionengine.service.MonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(PlantDescriptionEngineConstants.HTTP_API_MONITOR_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class PlantDescriptionMonitorAPI {

	private static final String BASE = PlantDescriptionEngineConstants.HTTP_API_MONITOR_PATH;

	@Autowired
	private MonitorService monitorService;

	@Autowired
	private AlarmService alarmService;

	@Operation(summary = "Lists plant descriptions with the matched registered systems (plantDescriptionMonitor.query-plant-descriptions)")
	@GetMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody MonitorPlantDescriptionListResponseDTO queryPlantDescriptions(
			@RequestParam(required = false) final Integer page,
			@RequestParam(required = false) final Integer size,
			@RequestParam(required = false) final String direction,
			@RequestParam(name = "sort_field", required = false) final String sortField,
			@RequestParam(required = false) final Boolean active) {
		return monitorService.query(new PageDTO(page, size, direction, sortField), active, "GET " + BASE + PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTIONS_PATH);
	}

	@Operation(summary = "Returns one plant description with the latest sysMonitor observations (plantDescriptionMonitor.get-plant-description)")
	@GetMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTION_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody MonitorPlantDescriptionResponseDTO getPlantDescription(@PathVariable final long id) {
		return monitorService.get(id, "GET " + BASE + PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTIONS_PATH + "/" + id);
	}

	@Operation(summary = "Lists alarms (plantDescriptionMonitor.query-alarms)")
	@GetMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_ALARMS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PdeAlarmListResponseDTO queryAlarms(
			@RequestParam(required = false) final Integer page,
			@RequestParam(required = false) final Integer size,
			@RequestParam(required = false) final String direction,
			@RequestParam(name = "sort_field", required = false) final String sortField,
			@RequestParam(name = "system_name", required = false) final String systemName,
			@RequestParam(required = false) final String severity,
			@RequestParam(required = false) final Boolean acknowledged) {
		return alarmService.query(new PageDTO(page, size, direction, sortField), systemName, severity, acknowledged, "GET " + BASE + PlantDescriptionEngineConstants.HTTP_API_OP_ALARMS_PATH);
	}

	@Operation(summary = "Returns one alarm (plantDescriptionMonitor.get-alarm)")
	@GetMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_ALARM_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PdeAlarmResponseDTO getAlarm(@PathVariable final long id) {
		return alarmService.get(id, "GET " + BASE + PlantDescriptionEngineConstants.HTTP_API_OP_ALARMS_PATH + "/" + id);
	}

	@Operation(summary = "Acknowledges an alarm (plantDescriptionMonitor.acknowledge-alarm)")
	@PatchMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_ALARM_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PdeAlarmResponseDTO acknowledgeAlarm(@PathVariable final long id, @RequestBody final PdeAlarmUpdateDTO dto) {
		return alarmService.acknowledge(id, dto, "PATCH " + BASE + PlantDescriptionEngineConstants.HTTP_API_OP_ALARMS_PATH + "/" + id);
	}
}
