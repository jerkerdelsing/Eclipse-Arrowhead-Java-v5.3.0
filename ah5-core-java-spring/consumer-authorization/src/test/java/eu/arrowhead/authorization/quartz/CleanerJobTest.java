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
package eu.arrowhead.authorization.quartz;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import eu.arrowhead.authorization.AuthorizationSystemInfo;
import eu.arrowhead.authorization.jpa.entity.SelfContainedToken;
import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.service.SelfContainedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TimeLimitedTokenDbService;
import eu.arrowhead.authorization.jpa.service.TokenHeaderDbService;
import eu.arrowhead.authorization.jpa.service.UsageLimitedTokenDbService;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@ExtendWith(MockitoExtension.class)
public class CleanerJobTest {

	//=================================================================================================
	// members

	@InjectMocks
	private CleanerJob job;

	@Mock
	private AuthorizationSystemInfo sysInfo;

	@Mock
	private TokenHeaderDbService tokenHeaderDbService;

	@Mock
	private UsageLimitedTokenDbService usageLimitedTokenDbService;

	@Mock
	private TimeLimitedTokenDbService timeLimitedTokenDbService;

	@Mock
	private SelfContainedTokenDbService selfContainedTokenDbService;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteExceptionHandled() {
		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenThrow(new InternalServerError("test"));

		assertDoesNotThrow(() -> job.execute(null));

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteUsageLimitedTokenNoRelatedRecord() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(usageLimitedTokenDbService.getByHeader(header)).thenReturn(Optional.empty());
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteUsageLimitedTokenNoUsageLeft() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		final UsageLimitedToken token = new UsageLimitedToken(header, 5);
		token.setUsageLeft(0);

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(usageLimitedTokenDbService.getByHeader(header)).thenReturn(Optional.of(token));
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteUsageLimitedTokenTooOld() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);
		header.setCreatedAt(ZonedDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));

		final UsageLimitedToken token = new UsageLimitedToken(header, 5);
		token.setUsageLeft(4);

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(usageLimitedTokenDbService.getByHeader(header)).thenReturn(Optional.of(token));
		when(sysInfo.getTokenMaxAge()).thenReturn(1);
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(sysInfo).getTokenMaxAge();
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteUsageLimitedTokenNoNeedToClean() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);
		header.setCreatedAt(Utilities.utcNow());

		final UsageLimitedToken token = new UsageLimitedToken(header, 5);
		token.setUsageLeft(4);

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(usageLimitedTokenDbService.getByHeader(header)).thenReturn(Optional.of(token));
		when(sysInfo.getTokenMaxAge()).thenReturn(100);

		assertDoesNotThrow(() -> job.execute(null));

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(sysInfo).getTokenMaxAge();
		verify(tokenHeaderDbService, never()).deleteById(anySet());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteTimeLimitedTokenNoRelatedRecord() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(timeLimitedTokenDbService.getByHeader(header)).thenReturn(Optional.empty());
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService, never()).getByHeader(header);
		verify(timeLimitedTokenDbService).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteTimeLimitedTokenExpired() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		final TimeLimitedToken token = new TimeLimitedToken(header, ZonedDateTime.of(2025, 1, 2, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(timeLimitedTokenDbService.getByHeader(header)).thenReturn(Optional.of(token));
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService, never()).getByHeader(header);
		verify(timeLimitedTokenDbService).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteTimeLimitedTokenTooOld() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);
		header.setCreatedAt(ZonedDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));

		final TimeLimitedToken token = new TimeLimitedToken(header, ZonedDateTime.of(2125, 1, 2, 10, 0, 0, 0, ZoneId.of(Constants.UTC))); // test will stop working right after 100 years

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(timeLimitedTokenDbService.getByHeader(header)).thenReturn(Optional.of(token));
		when(sysInfo.getTokenMaxAge()).thenReturn(1);
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService, never()).getByHeader(header);
		verify(timeLimitedTokenDbService).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(sysInfo).getTokenMaxAge();
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteTimeLimitedTokenNoNeedToClean() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);
		header.setCreatedAt(Utilities.utcNow());

		final TimeLimitedToken token = new TimeLimitedToken(header, ZonedDateTime.of(2125, 1, 2, 10, 0, 0, 0, ZoneId.of(Constants.UTC))); // test will stop working after 100 years

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(timeLimitedTokenDbService.getByHeader(header)).thenReturn(Optional.of(token));
		when(sysInfo.getTokenMaxAge()).thenReturn(100);

		assertDoesNotThrow(() -> job.execute(null));

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService, never()).getByHeader(header);
		verify(timeLimitedTokenDbService).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(sysInfo).getTokenMaxAge();
		verify(tokenHeaderDbService, never()).deleteById(anySet());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteSelfContainedTokenNoRelatedRecord() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(selfContainedTokenDbService.getByHeader(header)).thenReturn(Optional.empty());
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService, never()).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService).getByHeader(header);
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteSelfContainedTokenExpired() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		final SelfContainedToken token = new SelfContainedToken(header, "variant", ZonedDateTime.of(2025, 1, 2, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(selfContainedTokenDbService.getByHeader(header)).thenReturn(Optional.of(token));
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService, never()).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService).getByHeader(header);
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteSelfContainedTokenTooOld() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);
		header.setCreatedAt(ZonedDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneId.of(Constants.UTC)));

		final SelfContainedToken token = new SelfContainedToken(header, "variant", ZonedDateTime.of(2125, 1, 2, 10, 0, 0, 0, ZoneId.of(Constants.UTC))); // test will stop working right after 100 years

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(selfContainedTokenDbService.getByHeader(header)).thenReturn(Optional.of(token));
		when(sysInfo.getTokenMaxAge()).thenReturn(1);
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService, never()).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService).getByHeader(header);
		verify(sysInfo).getTokenMaxAge();
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteSelfContainedTokenNoNeedToClean() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);
		header.setCreatedAt(Utilities.utcNow());

		final SelfContainedToken token = new SelfContainedToken(header, "variant", ZonedDateTime.of(2125, 1, 2, 10, 0, 0, 0, ZoneId.of(Constants.UTC))); // test will stop working after 100 years

		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);
		when(selfContainedTokenDbService.getByHeader(header)).thenReturn(Optional.of(token));
		when(sysInfo.getTokenMaxAge()).thenReturn(100);

		assertDoesNotThrow(() -> job.execute(null));

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService, never()).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService).getByHeader(header);
		verify(sysInfo).getTokenMaxAge();
		verify(tokenHeaderDbService, never()).deleteById(anySet());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testExecuteUnhandledTokenType() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TRANSLATION_BRIDGE_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		final Page<TokenHeader> page = new PageImpl<>(List.of(header));

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page);

		assertDoesNotThrow(() -> job.execute(null));

		verify(tokenHeaderDbService).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService, never()).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(tokenHeaderDbService, never()).deleteById(anySet());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testExecuteMorePages() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"hashedToken",
				"Requester",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(1);

		final Page<TokenHeader> page = new PageImpl<>(List.of(header), PageRequest.of(0, 100, Sort.by(Direction.ASC, TokenHeader.DEFAULT_SORT_FIELD)), 101);
		final Page<TokenHeader> page2 = new PageImpl<>(List.of());

		when(tokenHeaderDbService.query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(page).thenReturn(page2);
		when(usageLimitedTokenDbService.getByHeader(header)).thenReturn(Optional.empty());
		doNothing().when(tokenHeaderDbService).deleteById(anySet());

		assertDoesNotThrow(() -> job.execute(null));

		@SuppressWarnings("unchecked")
		final ArgumentCaptor<Set<Long>> idListCaptor = ArgumentCaptor.forClass(Set.class);

		verify(tokenHeaderDbService, times(2)).query(any(Pageable.class), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
		verify(usageLimitedTokenDbService).getByHeader(header);
		verify(timeLimitedTokenDbService, never()).getByHeader(header);
		verify(selfContainedTokenDbService, never()).getByHeader(header);
		verify(tokenHeaderDbService).deleteById(idListCaptor.capture());

		assertEquals(1, idListCaptor.getValue().size());
		assertEquals(1, idListCaptor.getValue().iterator().next());
	}
}