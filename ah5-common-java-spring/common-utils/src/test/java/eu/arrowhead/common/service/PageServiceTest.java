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
package eu.arrowhead.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.service.validation.PageValidator;
import eu.arrowhead.dto.PageDTO;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class PageServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private PageService service;

	@Mock
	private PageValidator pageValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void init() {
		ReflectionTestUtils.setField(service, "maxPageSize", 11);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePageParameterNull() {
		final PageDTO page = service.normalizePageParameters(null, Direction.ASC, List.of(), "default", "test");

		assertNotNull(page);
		assertEquals(0, page.page());
		assertEquals(11, page.size());
		assertEquals("ASC", page.direction());
		assertEquals("default", page.sortField());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePageParameterEmptySpec() {
		doNothing().when(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		final PageDTO page = service.normalizePageParameters(new PageDTO(null, null, null, null), Direction.ASC, List.of(), "default", "test");

		verify(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		assertNotNull(page);
		assertEquals(0, page.page());
		assertEquals(11, page.size());
		assertEquals("ASC", page.direction());
		assertEquals("default", page.sortField());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePageParameterWrongSpec() {
		doNothing().when(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		final PageDTO page = service.normalizePageParameters(new PageDTO(-1, 0, null, null), Direction.ASC, List.of(), "default", "test");

		verify(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		assertNotNull(page);
		assertEquals(0, page.page());
		assertEquals(11, page.size());
		assertEquals("ASC", page.direction());
		assertEquals("default", page.sortField());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePageParameterFullSpec() {
		doNothing().when(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		final PageDTO page = service.normalizePageParameters(new PageDTO(1, 10, Direction.DESC.name(), "notDefault"), Direction.ASC, List.of(), "default", "test");

		verify(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		assertNotNull(page);
		assertEquals(1, page.page());
		assertEquals(10, page.size());
		assertEquals("DESC", page.direction());
		assertEquals("notDefault", page.sortField());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizePage2Ok() {
		doNothing().when(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		final PageDTO page = service.normalizePageParameters(new PageDTO(1, 10, Direction.DESC.name(), "notDefault"), List.of(), "default", "test");

		verify(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		assertNotNull(page);
		assertEquals(1, page.page());
		assertEquals(10, page.size());
		assertEquals("DESC", page.direction());
		assertEquals("notDefault", page.sortField());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageRequestOk() {
		doNothing().when(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		final PageRequest pr = service.getPageRequest(new PageDTO(1, 10, Direction.DESC.name(), "notDefault"), Direction.ASC, List.of(), "default", "test");

		verify(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		assertNotNull(pr);
		assertEquals(1, pr.getPageNumber());
		assertEquals(10, pr.getPageSize());
		assertEquals("notDefault", pr.getSort().iterator().next().getProperty());
		assertEquals(Direction.DESC, pr.getSort().getOrderFor("notDefault").getDirection());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetPageRequest2Ok() {
		doNothing().when(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		final PageRequest pr = service.getPageRequest(new PageDTO(1, 10, Direction.DESC.name(), "notDefault"), List.of(), "default", "test");

		verify(pageValidator).validatePageParameter(any(PageDTO.class), anyList(), anyString());

		assertNotNull(pr);
		assertEquals(1, pr.getPageNumber());
		assertEquals(10, pr.getPageSize());
		assertEquals("notDefault", pr.getSort().iterator().next().getProperty());
		assertEquals(Direction.DESC, pr.getSort().getOrderFor("notDefault").getDirection());
	}
}