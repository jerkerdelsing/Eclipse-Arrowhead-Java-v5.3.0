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
package eu.arrowhead.authorization.jpa.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.repository.TokenHeaderRepository;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@ExtendWith(MockitoExtension.class)
public class TokenHeaderDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TokenHeaderDbService dbService;

	@Mock
	private TokenHeaderRepository headerRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1ProviderNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find(null, null));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1ProviderEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find("", null));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1TokenHashNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find("TestProvider", null));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1TokenHashEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find("TestProvider", ""));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1InternalServerError() {
		when(headerRepo.findByProviderAndTokenHash("TestProvider", "hash")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.find("TestProvider", "hash"));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepo).findByProviderAndTokenHash("TestProvider", "hash");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind1ok() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findByProviderAndTokenHash("TestProvider", "hash")).thenReturn(Optional.of(header));

		final Optional<TokenHeader> result = dbService.find("TestProvider", "hash");

		assertFalse(result.isEmpty());
		assertEquals(header, result.get());

		verify(headerRepo).findByProviderAndTokenHash("TestProvider", "hash");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind2TokenHashNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find(null));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind2TokenHashEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.find(""));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind2InternalServerError() {
		when(headerRepo.findByTokenHash("hash")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.find("hash"));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepo).findByTokenHash("hash");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFind2ok() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findByTokenHash("hash")).thenReturn(Optional.of(header));

		final Optional<TokenHeader> result = dbService.find("hash");

		assertFalse(result.isEmpty());
		assertEquals(header, result.get());

		verify(headerRepo).findByTokenHash("hash");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findByTokenHashList(null));

		assertEquals("token hash list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findByTokenHashList(List.of()));

		assertEquals("token hash list is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListContainsNullElement() {
		final List<String> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findByTokenHashList(list));

		assertEquals("token hash list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListContainsEmptyElement() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.findByTokenHashList(List.of("")));

		assertEquals("token hash list contains null or empty element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListInternalServerError() {
		when(headerRepo.findAllByTokenHashIn(List.of("hash"))).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.findByTokenHashList(List.of("hash")));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepo).findAllByTokenHashIn(List.of("hash"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFindByTokenHashListOk() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findAllByTokenHashIn(List.of("hash"))).thenReturn(List.of(header));

		final List<TokenHeader> result = dbService.findByTokenHashList(List.of("hash"));

		assertFalse(result.isEmpty());
		assertEquals(header, result.get(0));

		verify(headerRepo).findAllByTokenHashIn(List.of("hash"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdCollectionNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deleteById(null));

		assertEquals("ID collection is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdCollectionEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deleteById(List.of()));

		assertEquals("ID collection is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdCollectionContainsNullElement() {
		final List<Long> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.deleteById(list));

		assertEquals("ID collection contains null element", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdInternalServerError() {
		doThrow(RuntimeException.class).when(headerRepo).deleteAllByIdInBatch(List.of(1L));

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.deleteById(List.of(1L)));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepo).deleteAllByIdInBatch(List.of(1L));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDeleteByIdOk() {
		doNothing().when(headerRepo).deleteAllByIdInBatch(List.of(1L));
		doNothing().when(headerRepo).flush();

		assertDoesNotThrow(() -> dbService.deleteById(List.of(1L)));

		verify(headerRepo).deleteAllByIdInBatch(List.of(1L));
		verify(headerRepo).flush();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryPaginationNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.query(null, null, null, null, null, null, null, null));

		assertEquals("pagination is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryInternalServerError() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		when(headerRepo.findAllByRequester("AdminSystem")).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.query(pageRequest, "AdminSystem", null, null, null, null, null, null));

		assertEquals("Database operation error", ex.getMessage());

		verify(headerRepo).findAllByRequester("AdminSystem");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryRequesterBaseProviderNotMatch() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(headerRepo.findAllByRequester("AdminSystem")).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<TokenHeader> result = dbService.query(pageRequest, "AdminSystem", null, null, null, "OtherProvider", null, null);

		assertTrue(result.isEmpty());

		verify(headerRepo).findAllByRequester("AdminSystem");
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo, never()).findAllByConsumerCloud(anyString());
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryProviderBaseTargetNotMatch() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(headerRepo.findAllByProvider("TestProvider")).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<TokenHeader> result = dbService.query(pageRequest, null, null, null, null, "TestProvider", "otherServiceDef", null);

		assertTrue(result.isEmpty());

		verify(headerRepo, never()).findAllByRequester(anyString());
		verify(headerRepo).findAllByProvider("TestProvider");
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo, never()).findAllByConsumerCloud(anyString());
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTargetBaseCloudNotMatch() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(headerRepo.findAllByTarget("serviceDef")).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<TokenHeader> result = dbService.query(pageRequest, null, null, "TestCloud|Company", null, null, "serviceDef", null);

		assertTrue(result.isEmpty());

		verify(headerRepo, never()).findAllByRequester(anyString());
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo).findAllByTarget("serviceDef");
		verify(headerRepo, never()).findAllByConsumerCloud(anyString());
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryCloudBaseConsumerNotMatch() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(headerRepo.findAllByConsumerCloud("LOCAL")).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<TokenHeader> result = dbService.query(pageRequest, null, null, "LOCAL", "OtherConsumer", null, null, null);

		assertTrue(result.isEmpty());

		verify(headerRepo, never()).findAllByRequester(anyString());
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo).findAllByConsumerCloud("LOCAL");
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryConsumerBaseTokenTypeNotMatch() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(headerRepo.findAllByConsumer("TestConsumer")).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<TokenHeader> result = dbService.query(pageRequest, null, AuthorizationTokenType.USAGE_LIMITED_TOKEN, null, "TestConsumer", null, null, null);

		assertTrue(result.isEmpty());

		verify(headerRepo, never()).findAllByRequester(anyString());
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo, never()).findAllByConsumerCloud(anyString());
		verify(headerRepo).findAllByConsumer("TestConsumer");
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTokenTypeBaseTargetTypeNotMatch() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(headerRepo.findAllByTokenType(AuthorizationTokenType.TIME_LIMITED_TOKEN)).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<TokenHeader> result = dbService.query(pageRequest, null, AuthorizationTokenType.TIME_LIMITED_TOKEN, null, null, null, null, AuthorizationTargetType.EVENT_TYPE);

		assertTrue(result.isEmpty());

		verify(headerRepo, never()).findAllByRequester(anyString());
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo, never()).findAllByConsumerCloud(anyString());
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo).findAllByTokenType(AuthorizationTokenType.TIME_LIMITED_TOKEN);
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryTargetTypeBaseMatch() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findAllByTargetType(AuthorizationTargetType.SERVICE_DEF)).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(1L), pageRequest)).thenReturn(new PageImpl<>(List.of(header)));

		final Page<TokenHeader> result = dbService.query(pageRequest, null, null, null, null, null, null, AuthorizationTargetType.SERVICE_DEF);

		assertFalse(result.isEmpty());
		assertEquals(header, result.getContent().get(0));

		verify(headerRepo, never()).findAllByRequester(anyString());
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo, never()).findAllByConsumerCloud(anyString());
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo).findAllByTargetType(AuthorizationTargetType.SERVICE_DEF);
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(1L), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryNoFilters() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(header)));

		final Page<TokenHeader> result = dbService.query(pageRequest, null, null, null, null, null, null, null);

		assertFalse(result.isEmpty());
		assertEquals(header, result.getContent().get(0));

		verify(headerRepo, never()).findAllByRequester(anyString());
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo, never()).findAllByConsumerCloud(anyString());
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo).findAll(pageRequest);
		verify(headerRepo, never()).findAllByIdIn(anySet(), eq(pageRequest));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryAllFilters() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findAllByRequester("AdminSystem")).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(1L), pageRequest)).thenReturn(new PageImpl<>(List.of(header)));

		final Page<TokenHeader> result = dbService.query(
				pageRequest,
				"AdminSystem",
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				"serviceDef",
				AuthorizationTargetType.SERVICE_DEF);

		assertFalse(result.isEmpty());
		assertEquals(header, result.getContent().get(0));

		verify(headerRepo).findAllByRequester("AdminSystem");
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo, never()).findAllByConsumerCloud(anyString());
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(1L), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQuery4Filters() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		when(headerRepo.findAllByRequester("AdminSystem")).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(1L), pageRequest)).thenReturn(new PageImpl<>(List.of(header)));

		final Page<TokenHeader> result = dbService.query(
				pageRequest,
				"AdminSystem",
				null,
				null,
				"TestConsumer",
				"TestProvider",
				"serviceDef",
				null);

		assertFalse(result.isEmpty());
		assertEquals(header, result.getContent().get(0));

		verify(headerRepo).findAllByRequester("AdminSystem");
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo, never()).findAllByConsumerCloud(anyString());
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(1L), pageRequest);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testQueryCloudBaseTokenTypeNotMatch() {
		final PageRequest pageRequest = PageRequest.of(0, 1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		when(headerRepo.findAllByConsumerCloud("LOCAL")).thenReturn(List.of(header));
		when(headerRepo.findAllByIdIn(Set.of(), pageRequest)).thenReturn(new PageImpl<>(List.of()));

		final Page<TokenHeader> result = dbService.query(pageRequest, null, AuthorizationTokenType.USAGE_LIMITED_TOKEN, "LOCAL", null, null, null, null);

		assertTrue(result.isEmpty());

		verify(headerRepo, never()).findAllByRequester(anyString());
		verify(headerRepo, never()).findAllByProvider(anyString());
		verify(headerRepo, never()).findAllByTarget(anyString());
		verify(headerRepo).findAllByConsumerCloud("LOCAL");
		verify(headerRepo, never()).findAllByConsumer(anyString());
		verify(headerRepo, never()).findAllByTokenType(any(AuthorizationTokenType.class));
		verify(headerRepo, never()).findAllByTargetType(any(AuthorizationTargetType.class));
		verify(headerRepo, never()).findAll(any(Pageable.class));
		verify(headerRepo).findAllByIdIn(Set.of(), pageRequest);
	}
}