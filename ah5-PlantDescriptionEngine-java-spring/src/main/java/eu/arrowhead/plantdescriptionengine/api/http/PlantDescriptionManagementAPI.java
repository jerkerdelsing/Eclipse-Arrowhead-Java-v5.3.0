/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.plantdescriptionengine.api.http;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.plantdescriptionengine.PlantDescriptionEngineConstants;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionListResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionRequestDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionResponseDTO;
import eu.arrowhead.plantdescriptionengine.api.dto.PlantDescriptionUpdateDTO;
import eu.arrowhead.plantdescriptionengine.service.PlantDescriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping(PlantDescriptionEngineConstants.HTTP_API_MANAGEMENT_PATH)
@SecurityRequirement(name = Constants.SECURITY_REQ_AUTHORIZATION)
public class PlantDescriptionManagementAPI {

	private static final String PDS = PlantDescriptionEngineConstants.HTTP_API_MANAGEMENT_PATH + PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTIONS_PATH;

	@Autowired
	private PlantDescriptionService service;

	@Operation(summary = "Lists plant descriptions (plantDescriptionManagement.query)")
	@GetMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTIONS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PlantDescriptionListResponseDTO query(
			@RequestParam(required = false) final Integer page,
			@RequestParam(required = false) final Integer size,
			@RequestParam(required = false) final String direction,
			@RequestParam(name = "sort_field", required = false) final String sortField,
			@RequestParam(required = false) final Boolean active) {
		return service.query(new PageDTO(page, size, direction, sortField), active, "GET " + PDS);
	}

	@Operation(summary = "Returns one plant description (plantDescriptionManagement.get)")
	@GetMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTION_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PlantDescriptionResponseDTO get(@PathVariable final long id) {
		return service.get(id, "GET " + PDS + "/" + id);
	}

	@Operation(summary = "Creates a plant description (plantDescriptionManagement.create)")
	@PostMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTIONS_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public @ResponseBody PlantDescriptionResponseDTO create(@RequestBody final PlantDescriptionRequestDTO dto) {
		return service.create(dto, "POST " + PDS);
	}

	@Operation(summary = "Replaces a plant description (plantDescriptionManagement.replace)")
	@PutMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTION_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PlantDescriptionResponseDTO replace(@PathVariable final long id, @RequestBody final PlantDescriptionRequestDTO dto) {
		return service.replace(id, dto, "PUT " + PDS + "/" + id);
	}

	@Operation(summary = "Changes members of a plant description (plantDescriptionManagement.update)")
	@PatchMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTION_PATH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody PlantDescriptionResponseDTO update(@PathVariable final long id, @RequestBody final PlantDescriptionUpdateDTO dto) {
		return service.update(id, dto, "PATCH " + PDS + "/" + id);
	}

	@Operation(summary = "Removes a plant description that no other description includes (plantDescriptionManagement.remove)")
	@DeleteMapping(path = PlantDescriptionEngineConstants.HTTP_API_OP_PLANT_DESCRIPTION_PATH)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable final long id) {
		service.remove(id, "DELETE " + PDS + "/" + id);
	}
}
