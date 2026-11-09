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

package eu.arrowhead.serviceorchestration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.service.PageService;
import eu.arrowhead.dto.OrchestrationSimpleStoreListRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreListResponseDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreQueryRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreRequestDTO;
import eu.arrowhead.dto.OrchestrationSimpleStoreResponseDTO;
import eu.arrowhead.dto.PageDTO;
import eu.arrowhead.dto.PriorityRequestDTO;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationStore;
import eu.arrowhead.serviceorchestration.jpa.service.SimpleStoreDbService;
import eu.arrowhead.serviceorchestration.service.dto.DTOConverter;
import eu.arrowhead.serviceorchestration.service.model.NormalizedOrchestrationSimpleStoreQueryRequest;
import eu.arrowhead.serviceorchestration.service.validation.OrchestrationStoreManagementServiceValidation;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class OrchestrationStoreManagementServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private OrchestrationStoreManagementService service;

	@Mock
	private OrchestrationStoreManagementServiceValidation validator;

	@Mock
	private SimpleStoreDbService dbService;

	@Mock
	private PageService pageService;

	@Mock
	private DTOConverter dtoConverter;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSimpleStoreEntriesOk() {

		final OrchestrationSimpleStoreRequestDTO candidate = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|service1|1.0.0", 1);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate));

		final OrchestrationSimpleStoreResponseDTO entry = new OrchestrationSimpleStoreResponseDTO(UUID.randomUUID().toString(), "Consumer1", "service1", "Provider1|service1|1.0.0", 1, "Manager1", null, null, null);
		final OrchestrationSimpleStoreListResponseDTO expected = new OrchestrationSimpleStoreListResponseDTO(List.of(entry), 1);

		final OrchestrationStore dbRule = new OrchestrationStore("Consumer1", "service1", "Provider1|service1|1.0.0", 1, "Manager1");

		when(validator.validateAndNormalizeCreateBulk(eq(dto), any())).thenReturn(List.of(candidate));
		when(dbService.createBulk(eq(List.of(candidate)), any())).thenReturn(List.of(dbRule));
		when(dtoConverter.convertStoreEntityListToResponseListDTO(List.of(dbRule))).thenReturn(expected);

		final OrchestrationSimpleStoreListResponseDTO actual = service.createSimpleStoreEntries(dto, "Manager1", "test origin");
		assertEquals(expected, actual);
		verify(dbService).createBulk(List.of(candidate), "Manager1");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSimpleStoreEntriesThrowsInvalidParameterException() {

		final OrchestrationSimpleStoreRequestDTO candidate = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|service1|1.0.0", 1);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate));

		when(validator.validateAndNormalizeCreateBulk(eq(dto), any())).thenReturn(List.of(candidate));
		when(dbService.createBulk(eq(List.of(candidate)), any())).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createSimpleStoreEntries(dto, "Manager1", "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateSimpleStoreEntriesThrowsInternalServerError() {
		final OrchestrationSimpleStoreRequestDTO candidate = new OrchestrationSimpleStoreRequestDTO("Consumer1", "Provider1|service1|1.0.0", 1);
		final OrchestrationSimpleStoreListRequestDTO dto = new OrchestrationSimpleStoreListRequestDTO(List.of(candidate));

		when(validator.validateAndNormalizeCreateBulk(eq(dto), any())).thenReturn(List.of(candidate));
		when(dbService.createBulk(eq(List.of(candidate)), any())).thenThrow(new InternalServerError("Internal error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createSimpleStoreEntries(dto, "Manager1", "test origin"));
		assertEquals("Internal error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySimpleStoreEntriesOk() {

		final OrchestrationSimpleStoreResponseDTO entry = new OrchestrationSimpleStoreResponseDTO(UUID.randomUUID().toString(), "Consumer1", "service1", "Provider1|service1|1.0.0", 1, "Manager1", null, null, null);
		final OrchestrationSimpleStoreListResponseDTO expected = new OrchestrationSimpleStoreListResponseDTO(List.of(entry), 1);

		final PageDTO pagination = new PageDTO(0, 5, "ASC", "id");
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(pagination, null, null, null, null, null, null, null);

		final NormalizedOrchestrationSimpleStoreQueryRequest normalized = new NormalizedOrchestrationSimpleStoreQueryRequest(pagination, null, null, null, null, null, null, null);
		final PageRequest pageRequest = PageRequest.of(0, 5, Sort.Direction.ASC, "id");

		final OrchestrationStore dbRule = new OrchestrationStore("Consumer1", "service1", "Provider1|service1|1.0.0", 1, "Manager1");
		final Page<OrchestrationStore> entityPage = new PageImpl<>(List.of(dbRule));

		when(validator.validateAndNormalizeQuery(eq(dto), any())).thenReturn(normalized);
		when(pageService.getPageRequest(eq(pagination), any(), any(), any(), any())).thenReturn(pageRequest);
		when(dbService.getPageByFilters(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(entityPage);
		when(dtoConverter.convertStoreEntityPageToResponseListTO(entityPage)).thenReturn(expected);

		assertEquals(expected, service.querySimpleStoreEntries(dto, "test origin"));
		verify(dbService).getPageByFilters(eq(pageRequest), any(), any(), any(), any(), any(), any(), any());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuerySimpleStoreEntriesThrowsInternalServerError() {

		final PageDTO pagination = new PageDTO(0, 5, "ASC", "id");
		final OrchestrationSimpleStoreQueryRequestDTO dto = new OrchestrationSimpleStoreQueryRequestDTO(pagination, null, null, null, null, null, null, null);

		final NormalizedOrchestrationSimpleStoreQueryRequest normalized = new NormalizedOrchestrationSimpleStoreQueryRequest(pagination, null, null, null, null, null, null, null);
		final PageRequest pageRequest = PageRequest.of(0, 5, Sort.Direction.ASC, "id");

		when(validator.validateAndNormalizeQuery(eq(dto), any())).thenReturn(normalized);
		when(pageService.getPageRequest(eq(pagination), any(), any(), any(), any())).thenReturn(pageRequest);
		when(dbService.getPageByFilters(any(), any(), any(), any(), any(), any(), any(), any())).thenThrow(new InternalServerError("Internal error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.querySimpleStoreEntries(dto, "test origin"));
		assertEquals(ex.getMessage(), "Internal error");
		assertEquals(ex.getOrigin(), "test origin");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testModifyPrioritiesOk() {

		final OrchestrationSimpleStoreResponseDTO entry = new OrchestrationSimpleStoreResponseDTO(UUID.randomUUID().toString(), "Consumer1", "service1", "Provider1|service1|1.0.0", 3, "Manager1", null, null, null);
		final OrchestrationSimpleStoreListResponseDTO expected = new OrchestrationSimpleStoreListResponseDTO(List.of(entry), 1);

		final PriorityRequestDTO dto = new PriorityRequestDTO();
		final UUID id = UUID.randomUUID();
		dto.put(UUID.randomUUID().toString(), 3);

		final OrchestrationStore dbRule = new OrchestrationStore("Consumer1", "service1", "Provider1|service1|1.0.0", 3, "Manager1");
		dbRule.setId(id);

		when(validator.validateAndNormalizePriorityRequestDTO(eq(dto), any())).thenReturn(Map.of(id, 3));
		when(dbService.setPriorities(eq(Map.of(id, 3)), eq("Manager1"))).thenReturn(List.of(dbRule));
		when(dtoConverter.convertStoreEntityListToResponseListDTO(List.of(dbRule))).thenReturn(expected);

		final OrchestrationSimpleStoreListResponseDTO actual = service.modifyPriorities(dto, "Manager1", "test origin");
		assertEquals(expected, actual);
		verify(dbService).setPriorities(Map.of(id, 3), "Manager1");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testModifyPrioritiesThrowsInvalidParameterException() {

		final PriorityRequestDTO dto = new PriorityRequestDTO();
		final UUID id = UUID.randomUUID();
		dto.put(UUID.randomUUID().toString(), 3);

		final OrchestrationStore dbRule = new OrchestrationStore("Consumer1", "service1", "Provider1|service1|1.0.0", 3, "Manager1");
		dbRule.setId(id);

		when(validator.validateAndNormalizePriorityRequestDTO(eq(dto), any())).thenReturn(Map.of(id, 3));
		when(dbService.setPriorities(eq(Map.of(id, 3)), eq("Manager1"))).thenThrow(new InvalidParameterException("Invalid parameter"));

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.modifyPriorities(dto, "Manager1", "test origin"));
		assertEquals("Invalid parameter", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testModifyPrioritiesThrowsInternalServerError() {

		final PriorityRequestDTO dto = new PriorityRequestDTO();
		final UUID id = UUID.randomUUID();
		dto.put(UUID.randomUUID().toString(), 3);

		final OrchestrationStore dbRule = new OrchestrationStore("Consumer1", "service1", "Provider1|service1|1.0.0", 3, "Manager1");
		dbRule.setId(id);

		when(validator.validateAndNormalizePriorityRequestDTO(eq(dto), any())).thenReturn(Map.of(id, 3));
		when(dbService.setPriorities(eq(Map.of(id, 3)), eq("Manager1"))).thenThrow(new InternalServerError("Internal error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.modifyPriorities(dto, "Manager1", "test origin"));
		assertEquals("Internal error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSimpleStoreEntriesOk() {

		final UUID id = UUID.randomUUID();
		when(validator.validateAndNormalizeRemove(eq(List.of(id.toString())), any())).thenReturn(List.of(id));

		service.removeSimpleStoreEntries(List.of(id.toString()), "test origin");
		verify(dbService).deleteBulk(List.of(id));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveSimpleStoreEntriesThrowsInternalServerError() {
		final UUID id = UUID.randomUUID();
		when(validator.validateAndNormalizeRemove(eq(List.of(id.toString())), any())).thenReturn(List.of(id));
		doThrow(new InternalServerError("Internal error")).when(dbService).deleteBulk(List.of(id));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeSimpleStoreEntries(List.of(id.toString()), "test origin"));
		assertEquals("Internal error", ex.getMessage());
		assertEquals("test origin", ex.getOrigin());
	}
}
