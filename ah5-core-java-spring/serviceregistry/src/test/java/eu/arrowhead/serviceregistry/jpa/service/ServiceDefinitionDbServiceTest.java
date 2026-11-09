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
package eu.arrowhead.serviceregistry.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceDefinition;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceDefinitionRepository;

@ExtendWith(MockitoExtension.class)
public class ServiceDefinitionDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceDefinitionDbService service;

	@Mock
	private ServiceDefinitionRepository repo;

	private static final String DB_ERROR_MSG = "Database operation error";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createBulk(null));

		assertEquals("service definition name list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkEmptyInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createBulk(List.of()));

		assertEquals("service definition name list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkAlreadyExistingEntities() {

		final ServiceDefinition existing1 = new ServiceDefinition("existingDef1");
		final ServiceDefinition existing2 = new ServiceDefinition("existingDef2");

		when(repo.findAllByNameIn(eq(List.of("existingDef1", "existingDef2", "newDef")))).thenReturn(List.of(existing1, existing2));
		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of("existingDef1", "existingDef2", "newDef")));
		assertEquals("Service definition names already exists: existingDef1, existingDef2", ex.getMessage());

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkNewEntities() {

		final List<ServiceDefinition> expected = List.of(new ServiceDefinition("def"));

		when(repo.findAllByNameIn(any())).thenReturn(List.of());
		when(repo.saveAllAndFlush(any())).thenAnswer(Invocation -> Invocation.getArgument(0));
		final List<ServiceDefinition> actual = service.createBulk(List.of("def"));
		assertEquals(expected, actual);
		verify(repo).saveAllAndFlush(expected);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkThrowsInternalServerError() {
		when(repo.findAllByNameIn(any())).thenThrow(new InternalServerError("test error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createBulk(List.of("def")));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getPage(null));

		assertEquals("pagination is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageOk() {
		final ServiceDefinition existing = new ServiceDefinition("existingDef");
		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(repo.findAll(eq(pageRequest))).thenReturn(new PageImpl<>(List.of(existing)));
		final Page<ServiceDefinition> result = service.getPage(pageRequest);
		assertEquals(new PageImpl<>(List.of(existing)), result);
		verify(repo).findAll(eq(pageRequest));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageThrowsException() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		when(repo.findAll(eq(pageRequest))).thenThrow(new InternalServerError("test error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getPage(pageRequest));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveBulkOk() {

		final ServiceDefinition existing = new ServiceDefinition("def");
		when(repo.findAllByNameIn(eq(List.of("def")))).thenReturn(List.of(existing));

		service.removeBulk(List.of("def"));
		final InOrder inOrder = Mockito.inOrder(repo);
		inOrder.verify(repo).deleteAll(eq(List.of(existing)));
		inOrder.verify(repo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRemoveBulkThrowsException() {

		when(repo.findAllByNameIn(eq(List.of("def")))).thenThrow(new InternalServerError("test error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.removeBulk(List.of("def")));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}
}
