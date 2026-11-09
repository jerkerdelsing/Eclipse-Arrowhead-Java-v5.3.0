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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;

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
import eu.arrowhead.common.exception.LockedException;
import eu.arrowhead.dto.ServiceInterfaceTemplatePropertyDTO;
import eu.arrowhead.dto.ServiceInterfaceTemplateRequestDTO;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplate;
import eu.arrowhead.serviceregistry.jpa.entity.ServiceInterfaceTemplateProperty;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplatePropertyRepository;
import eu.arrowhead.serviceregistry.jpa.repository.ServiceInterfaceTemplateRepository;

@ExtendWith(MockitoExtension.class)
public class ServiceInterfaceTemplateDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceInterfaceTemplateDbService service;

	@Mock
	private ServiceInterfaceTemplateRepository templateRepo;

	@Mock
	private ServiceInterfaceTemplatePropertyRepository templatePropsRepo;

	private static final String DB_ERROR_MSG = "Database operation error";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByName(null));

		assertEquals("name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getByName(""));

		assertEquals("name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameOk() {

		when(templateRepo.findByName(any())).thenReturn(Optional.empty());
		final Optional<ServiceInterfaceTemplate> result = service.getByName("generic_http");

		assertEquals(Optional.empty(), result);
		verify(templateRepo).findByName("generic_http");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByNameThrowsInternalServerError() {

		when(templateRepo.findByName(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getByName("generic_http"));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPropertiesByTemplateNameInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getPropertiesByTemplateName(null));

		assertEquals("name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPropertiesByTemplateNameInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.getPropertiesByTemplateName(""));

		assertEquals("name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPropertiesByTemplateNameExistingTemplate() {

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");

		when(templateRepo.findByName(any())).thenReturn(Optional.of(template));
		when(templatePropsRepo.findByServiceInterfaceTemplate(any())).thenReturn(List.of(property));

		final List<ServiceInterfaceTemplateProperty> actual = service.getPropertiesByTemplateName("generic_http");
		assertEquals(List.of(property), actual);
		verify(templatePropsRepo).findByServiceInterfaceTemplate(template);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPropertiesByTemplateNameNotExistingTemplate() {

		when(templateRepo.findByName(any())).thenReturn(Optional.empty());

		final List<ServiceInterfaceTemplateProperty> actual = service.getPropertiesByTemplateName("generic_http");
		assertEquals(List.of(), actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPropertiesByTemplateNameThrowsInternalServerError() {

		when(templateRepo.findByName(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getPropertiesByTemplateName("generic_http"));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersWithoutFilters() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");

		when(templateRepo.findAll()).thenReturn(List.of(template));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(any())).thenReturn(List.of(property));
		when(templateRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of(template), pageRequest, 1));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(Map.entry(template, List.of(property))), pageRequest, 1);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, null, null);
		verify(templateRepo).findAll();
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByTemplateNamesAndMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");

		when(templateRepo.findAllByNameIn(any())).thenReturn(List.of(template));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(any())).thenReturn(List.of(property));
		when(templateRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of(template), pageRequest, 1));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(Map.entry(template, List.of(property))), pageRequest, 1);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, List.of("generic_http"), null);
		verify(templateRepo).findAllByNameIn(List.of("generic_http"));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByTemplateNamesNoMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_https", "https");

		when(templateRepo.findAllByNameIn(any())).thenReturn(List.of(template));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(argThat(collection -> collection != null && collection.isEmpty()))).thenReturn(List.of());
		when(templateRepo.findAllByIdIn(argThat(collection -> collection != null && collection.isEmpty()), any())).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(), pageRequest, 0);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, List.of("generic_http"), null);
		verify(templateRepo).findAllByNameIn(List.of("generic_http"));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersByProtocolsAndMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");

		when(templateRepo.findAllByProtocolIn(any())).thenReturn(List.of(template));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(any())).thenReturn(List.of(property));
		when(templateRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of(template), pageRequest, 1));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(Map.entry(template, List.of(property))), pageRequest, 1);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, null, List.of("http"));
		verify(templateRepo).findAllByProtocolIn(List.of("http"));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersNeedToMatchTheIntfProperties() {

		final PageRequest pageRequest = PageRequest.of(0, 2, Direction.ASC, "id");
		final ServiceInterfaceTemplate template1 = new ServiceInterfaceTemplate("generic_http", "http");
		template1.setId(0);
		final ServiceInterfaceTemplate template2 = new ServiceInterfaceTemplate("generic_https", "https");
		template2.setId(1);
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template2, "accessAddresses", true, "NOT_EMPTY_ADDRESS_LIST");

		when(templateRepo.findAllByProtocolIn(any())).thenReturn(List.of(template1, template2));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(any())).thenReturn(List.of(property));
		when(templateRepo.findAllByIdIn(any(), any())).thenReturn(new PageImpl<>(List.of(template1, template2), pageRequest, 2));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(Map.entry(template1, List.of()), Map.entry(template2, List.of(property))), pageRequest, 2);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, null, List.of("http", "https"));
		verify(templateRepo).findAllByProtocolIn(List.of("http", "https"));
		assertEquals(expected.getTotalElements(), actual.getTotalElements());
		assertEquals(expected.getContent().size(), actual.getContent().size());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersProtocolDoesNotMatch() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");
		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");

		when(templateRepo.findAllByNameIn(List.of("generic_http"))).thenReturn(List.of(template));
		when(templatePropsRepo.findAllByServiceInterfaceTemplateIn(argThat(collection -> collection != null && collection.isEmpty()))).thenReturn(List.of());
		when(templateRepo.findAllByIdIn(argThat(collection -> collection != null && collection.isEmpty()), any())).thenReturn(new PageImpl<>(List.of(), pageRequest, 0));

		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> expected = new PageImpl<>(List.of(), pageRequest, 0);
		final Page<Entry<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>>> actual = service.getPageByFilters(pageRequest, List.of("generic_http"), List.of("https"));
		assertEquals(expected, actual);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageByFiltersThrowsInternalServerError() {

		final PageRequest pageRequest = PageRequest.of(0, 1, Direction.ASC, "id");

		when(templateRepo.findAll()).thenThrow(new LockedException("error"));
		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.getPageByFilters(pageRequest, null, null));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createBulk(null));

		assertEquals("interface template candidate list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.createBulk(List.of()));

		assertEquals("interface template candidate list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkDuplicatedTemplateThrowsInvalidParameterException() {

		when(templateRepo.existsByName(any())).thenReturn(false);

		final ServiceInterfaceTemplateRequestDTO dto1 = new ServiceInterfaceTemplateRequestDTO(
				"generic_http",
				"http",
				List.of());

		final ServiceInterfaceTemplateRequestDTO dto2 = new ServiceInterfaceTemplateRequestDTO(
				"generic_http",
				"https",
				List.of());

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto1, dto2)));
		assertEquals("Duplicated interface template name: generic_http", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkExistingTemplateThrowsInvalidParameterException() {

		final ServiceInterfaceTemplateRequestDTO dto = new ServiceInterfaceTemplateRequestDTO(
				"generic_http",
				"http",
				List.of());

		when(templateRepo.existsByName(any())).thenReturn(true);

		final InvalidParameterException ex = assertThrows(InvalidParameterException.class, () -> service.createBulk(List.of(dto)));
		assertEquals("Interface template already exists: generic_http", ex.getMessage());
		verify(templateRepo).existsByName("generic_http");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkExistingTemplatePropertyWithoutValidator() {

		final ServiceInterfaceTemplateRequestDTO dto = new ServiceInterfaceTemplateRequestDTO(
				"generic_http",
				"http",
				List.of(new ServiceInterfaceTemplatePropertyDTO("accessAddresses", true, null, List.of())));

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, null);

		when(templateRepo.existsByName(any())).thenReturn(false);
		when(templateRepo.saveAllAndFlush(any())).thenReturn(List.of(template));
		when(templatePropsRepo.saveAllAndFlush(any())).thenReturn(List.of(property));

		final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> expected = Map.of(template, List.of(property));
		final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> actual = service.createBulk(List.of(dto));
		assertEquals(expected, actual);
		verify(templateRepo).saveAllAndFlush(
				argThat(collection -> collection != null && ((Collection<?>) collection).contains(template) && ((Collection<?>) collection).size() == 1));
		verify(templatePropsRepo).saveAllAndFlush(
				argThat(collection -> collection != null && ((Collection<?>) collection).contains(property) && ((Collection<?>) collection).size() == 1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkExistingTemplatePropertyWithValidatorWithoutParams() {

		final ServiceInterfaceTemplateRequestDTO dto = new ServiceInterfaceTemplateRequestDTO(
				"generic_http",
				"http",
				List.of(new ServiceInterfaceTemplatePropertyDTO("accessAddresses", true, "MINMAX", List.of())));

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "MINMAX");

		when(templateRepo.existsByName(any())).thenReturn(false);
		when(templateRepo.saveAllAndFlush(any())).thenReturn(List.of(template));
		when(templatePropsRepo.saveAllAndFlush(any())).thenReturn(List.of(property));

		final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> expected = Map.of(template, List.of(property));
		final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> actual = service.createBulk(List.of(dto));
		assertEquals(expected, actual);
		verify(templateRepo).saveAllAndFlush(
				argThat(collection -> collection != null && ((Collection<?>) collection).contains(template) && ((Collection<?>) collection).size() == 1));
		verify(templatePropsRepo).saveAllAndFlush(
				argThat(collection -> collection != null && ((Collection<?>) collection).contains(property) && ((Collection<?>) collection).size() == 1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkExistingTemplatePropertyWithValidatorWithParams() {

		final ServiceInterfaceTemplateRequestDTO dto = new ServiceInterfaceTemplateRequestDTO(
				"generic_http",
				"http",
				List.of(new ServiceInterfaceTemplatePropertyDTO("accessPort", true, "MINMAX", List.of("4040", "4045"))));

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		final ServiceInterfaceTemplateProperty property = new ServiceInterfaceTemplateProperty(template, "accessAddresses", true, "MINMAX|4040|4045");

		when(templateRepo.existsByName(any())).thenReturn(false);
		when(templateRepo.saveAllAndFlush(any())).thenReturn(List.of(template));
		when(templatePropsRepo.saveAllAndFlush(any())).thenReturn(List.of(property));

		final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> expected = Map.of(template, List.of(property));
		final Map<ServiceInterfaceTemplate, List<ServiceInterfaceTemplateProperty>> actual = service.createBulk(List.of(dto));
		assertEquals(expected, actual);
		verify(templateRepo).saveAllAndFlush(
				argThat(collection -> collection != null && ((Collection<?>) collection).contains(template) && ((Collection<?>) collection).size() == 1));
		verify(templatePropsRepo).saveAllAndFlush(
				argThat(collection -> collection != null && ((Collection<?>) collection).contains(property) && ((Collection<?>) collection).size() == 1));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testCreateBulkExistingTemplateThrowsInternalServerError() {

		final ServiceInterfaceTemplateRequestDTO dto = new ServiceInterfaceTemplateRequestDTO(
				"generic_http",
				"http",
				List.of());

		when(templateRepo.existsByName(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.createBulk(List.of(dto)));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListInputNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByTemplateNameList(null));

		assertEquals("name list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListInputEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> service.deleteByTemplateNameList(List.of()));

		assertEquals("name list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListOk() {

		final ServiceInterfaceTemplate template = new ServiceInterfaceTemplate("generic_http", "http");
		when(templateRepo.findAllByNameIn(any())).thenReturn(List.of(template));

		service.deleteByTemplateNameList(List.of("generic_http"));
		final InOrder inOrder = Mockito.inOrder(templateRepo);
		inOrder.verify(templateRepo).deleteAll(List.of(template));
		inOrder.verify(templateRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByNameListThrowsInternalServerError() {

		when(templateRepo.findAllByNameIn(any())).thenThrow(new LockedException("error"));

		final InternalServerError ex = assertThrows(InternalServerError.class, () -> service.deleteByTemplateNameList(List.of("generic_http")));
		assertEquals(DB_ERROR_MSG, ex.getMessage());
	}
}
