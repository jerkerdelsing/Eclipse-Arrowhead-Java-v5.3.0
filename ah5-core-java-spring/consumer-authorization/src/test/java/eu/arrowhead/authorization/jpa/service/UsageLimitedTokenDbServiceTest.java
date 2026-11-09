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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.entity.UsageLimitedToken;
import eu.arrowhead.authorization.jpa.repository.TokenHeaderRepository;
import eu.arrowhead.authorization.jpa.repository.UsageLimitedTokenRepository;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class UsageLimitedTokenDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private UsageLimitedTokenDbService dbService;

	@Mock
	private UsageLimitedTokenRepository tokenRepo;

	@Mock
	private TokenHeaderRepository tokenHeaderRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTokenTypeNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(null, null, null, null, null, null, null, null, null, 10));

		assertEquals("tokenType is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTokenHashNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, null, null, null, null, null, null, null, null, 10));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTokenHashEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "", null, null, null, null, null, null, null, 10));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveRequesterNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "tokenHash", null, null, null, null, null, null, null, 10));

		assertEquals("requester is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveRequesterEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "tokenHash", "", null, null, null, null, null, null, 10));

		assertEquals("requester is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveConsumerCloudNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "tokenHash", "AdminSystem", null, null, null, null, null, null, 10));

		assertEquals("consumerCloud is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveConsumerCloudEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "tokenHash", "AdminSystem", "", null, null, null, null, null, 10));

		assertEquals("consumerCloud is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveConsumerNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", null, null, null, null, null, 10));

		assertEquals("consumer is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveConsumerEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", "", null, null, null, null, 10));

		assertEquals("consumer is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveProviderNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", "TestConsumer", null, null, null, null, 10));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveProviderEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", "TestConsumer", "", null, null, null, 10));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTargetTypeNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.USAGE_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", "TestConsumer", "TestProvider", null, null, null, 10));

		assertEquals("targetType is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTargetNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(
						AuthorizationTokenType.USAGE_LIMITED_TOKEN,
						"tokenHash",
						"AdminSystem",
						"LOCAL",
						"TestConsumer",
						"TestProvider",
						AuthorizationTargetType.SERVICE_DEF,
						null,
						null,
						10));

		assertEquals("target is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTargetEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(
						AuthorizationTokenType.USAGE_LIMITED_TOKEN,
						"tokenHash",
						"AdminSystem",
						"LOCAL",
						"TestConsumer",
						"TestProvider",
						AuthorizationTargetType.SERVICE_DEF,
						"",
						null,
						10));

		assertEquals("target is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveInternalServerError() {
		when(tokenHeaderRepo.findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope("LOCAL", "TestConsumer", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef", null))
				.thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.save(
						AuthorizationTokenType.USAGE_LIMITED_TOKEN,
						"tokenHash",
						"AdminSystem",
						"LOCAL",
						"TestConsumer",
						"TestProvider",
						AuthorizationTargetType.SERVICE_DEF,
						"serviceDef",
						null,
						10));

		assertEquals("Database operation error", ex.getMessage());

		verify(tokenHeaderRepo).findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope("LOCAL", "TestConsumer", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef", null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveNoOverride() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final TokenHeader savedHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		savedHeader.setId(1);

		final UsageLimitedToken token = new UsageLimitedToken(savedHeader, 10);
		final UsageLimitedToken savedToken = new UsageLimitedToken(savedHeader, 10);
		savedToken.setId(1);

		when(tokenHeaderRepo.findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope(
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation")).thenReturn(Optional.empty());
		when(tokenHeaderRepo.saveAndFlush(header)).thenReturn(savedHeader);
		when(tokenRepo.saveAndFlush(token)).thenReturn(savedToken);

		final Pair<UsageLimitedToken, Boolean> result = dbService.save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				10);

		assertEquals(savedToken, result.getFirst());
		assertTrue(result.getSecond());

		verify(tokenHeaderRepo).findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope(
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		verify(tokenHeaderRepo).saveAndFlush(header);
		verify(tokenRepo).saveAndFlush(token);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveNoOverrideWithZombieHeader() {
		final TokenHeader zombie = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		zombie.setId(1);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final TokenHeader savedHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		savedHeader.setId(2);

		final UsageLimitedToken token = new UsageLimitedToken(savedHeader, 10);
		final UsageLimitedToken savedToken = new UsageLimitedToken(savedHeader, 10);
		savedToken.setId(1);

		when(tokenHeaderRepo.findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope(
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation")).thenReturn(Optional.of(zombie));
		when(tokenRepo.findByHeader(zombie)).thenReturn(Optional.empty());
		doNothing().when(tokenHeaderRepo).delete(zombie);
		when(tokenHeaderRepo.saveAndFlush(header)).thenReturn(savedHeader);
		when(tokenRepo.saveAndFlush(token)).thenReturn(savedToken);

		final Pair<UsageLimitedToken, Boolean> result = dbService.save(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				10);

		assertEquals(savedToken, result.getFirst());
		assertTrue(result.getSecond());

		verify(tokenHeaderRepo).findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope(
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		verify(tokenRepo).findByHeader(zombie);
		verify(tokenHeaderRepo).delete(zombie);
		verify(tokenHeaderRepo).saveAndFlush(header);
		verify(tokenRepo).saveAndFlush(token);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveOverride() {
		final TokenHeader oldHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		oldHeader.setId(1);

		final UsageLimitedToken oldToken = new UsageLimitedToken(oldHeader, 10);

		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final TokenHeader savedHeader = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		savedHeader.setId(2);

		final UsageLimitedToken token = new UsageLimitedToken(savedHeader, 10);
		final UsageLimitedToken savedToken = new UsageLimitedToken(savedHeader, 10);
		savedToken.setId(1);

		when(tokenHeaderRepo.findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope(
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation")).thenReturn(Optional.of(oldHeader));
		when(tokenRepo.findByHeader(oldHeader)).thenReturn(Optional.of(oldToken));
		doNothing().when(tokenRepo).delete(oldToken);
		doNothing().when(tokenHeaderRepo).delete(oldHeader);
		when(tokenHeaderRepo.saveAndFlush(header)).thenReturn(savedHeader);
		when(tokenRepo.saveAndFlush(token)).thenReturn(savedToken);

		final Pair<UsageLimitedToken, Boolean> result = dbService.save(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				10);

		assertEquals(savedToken, result.getFirst());
		assertFalse(result.getSecond());

		verify(tokenHeaderRepo).findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope(
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		verify(tokenRepo).findByHeader(oldHeader);
		verify(tokenRepo).delete(oldToken);
		verify(tokenHeaderRepo).delete(oldHeader);
		verify(tokenHeaderRepo).saveAndFlush(header);
		verify(tokenRepo).saveAndFlush(token);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByHeaderNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.getByHeader(null));

		assertEquals("header is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByHeaderInternalServerError() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.SELF_CONTAINED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(2);

		when(tokenRepo.findByHeader(header)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.getByHeader(header));

		assertEquals("Database operation error", ex.getMessage());

		verify(tokenRepo).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByHeaderNotFound() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(2);

		when(tokenRepo.findByHeader(header)).thenReturn(Optional.empty());

		final Optional<UsageLimitedToken> result = dbService.getByHeader(header);

		assertTrue(result.isEmpty());

		verify(tokenRepo).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByHeaderOk() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(2);

		final UsageLimitedToken token = new UsageLimitedToken(header, 10);
		token.setId(1);

		when(tokenRepo.findByHeader(header)).thenReturn(Optional.of(token));

		final Optional<UsageLimitedToken> result = dbService.getByHeader(header);

		assertFalse(result.isEmpty());
		assertEquals(token, result.get());

		verify(tokenRepo).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecreaseNullInput() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.decrease(null));

		assertEquals("header is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecreaseInternalServerError() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(2);

		when(tokenRepo.findByHeader(header)).thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.decrease(header));

		assertEquals("Database operation error", ex.getMessage());

		verify(tokenRepo).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecreaseNoUsageLimitedTokenForHeader() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(2);

		when(tokenRepo.findByHeader(header)).thenReturn(Optional.empty());

		Optional<Pair<Integer, Integer>> result = dbService.decrease(header);

		assertTrue(result.isEmpty());

		verify(tokenRepo).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecreaseExpiredToken() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(2);

		final UsageLimitedToken token = new UsageLimitedToken(header, 5);
		token.setId(1);
		token.setUsageLeft(0);

		when(tokenRepo.findByHeader(header)).thenReturn(Optional.of(token));

		Optional<Pair<Integer, Integer>> result = dbService.decrease(header);

		assertTrue(result.isPresent());
		assertEquals(0, result.get().getFirst());
		assertEquals(0, result.get().getSecond());

		verify(tokenRepo).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecreaseOk() {
		final TokenHeader header = new TokenHeader(
				AuthorizationTokenType.USAGE_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		header.setId(2);

		final UsageLimitedToken token = new UsageLimitedToken(header, 5);
		token.setId(1);
		token.setUsageLeft(5);

		when(tokenRepo.findByHeader(header)).thenReturn(Optional.of(token));
		when(tokenRepo.saveAndFlush(token)).thenReturn(token);

		Optional<Pair<Integer, Integer>> result = dbService.decrease(header);

		assertTrue(result.isPresent());
		assertEquals(5, result.get().getFirst());
		assertEquals(4, result.get().getSecond());

		verify(tokenRepo).findByHeader(header);
		verify(tokenRepo).saveAndFlush(token);
	}
}