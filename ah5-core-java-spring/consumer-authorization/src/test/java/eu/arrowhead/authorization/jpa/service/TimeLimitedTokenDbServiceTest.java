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

import java.time.ZonedDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

import eu.arrowhead.authorization.jpa.entity.TimeLimitedToken;
import eu.arrowhead.authorization.jpa.entity.TokenHeader;
import eu.arrowhead.authorization.jpa.repository.TimeLimitedTokenRepository;
import eu.arrowhead.authorization.jpa.repository.TokenHeaderRepository;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.enums.AuthorizationTargetType;
import eu.arrowhead.dto.enums.AuthorizationTokenType;

@ExtendWith(MockitoExtension.class)
public class TimeLimitedTokenDbServiceTest {

	//=================================================================================================
	// members

	@InjectMocks
	private TimeLimitedTokenDbService dbService;

	@Mock
	private TimeLimitedTokenRepository tokenRepo;

	@Mock
	private TokenHeaderRepository tokenHeaderRepo;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTokenTypeNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(null, null, null, null, null, null, null, null, null, null));

		assertEquals("tokenType is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTokenHashNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, null, null, null, null, null, null, null, null, null));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTokenHashEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "", null, null, null, null, null, null, null, null));

		assertEquals("tokenHash is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveRequesterNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "tokenHash", null, null, null, null, null, null, null, null));

		assertEquals("requester is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveRequesterEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "tokenHash", "", null, null, null, null, null, null, null));

		assertEquals("requester is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveConsumerCloudNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "tokenHash", "AdminSystem", null, null, null, null, null, null, null));

		assertEquals("consumerCloud is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveConsumerCloudEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "tokenHash", "AdminSystem", "", null, null, null, null, null, null));

		assertEquals("consumerCloud is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveConsumerNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", null, null, null, null, null, null));

		assertEquals("consumer is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveConsumerEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", "", null, null, null, null, null));

		assertEquals("consumer is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveProviderNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", "TestConsumer", null, null, null, null, null));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveProviderEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", "TestConsumer", "", null, null, null, null));

		assertEquals("provider is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTargetTypeNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(AuthorizationTokenType.TIME_LIMITED_TOKEN, "tokenHash", "AdminSystem", "LOCAL", "TestConsumer", "TestProvider", null, null, null, null));

		assertEquals("targetType is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTargetNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(
						AuthorizationTokenType.TIME_LIMITED_TOKEN,
						"tokenHash",
						"AdminSystem",
						"LOCAL",
						"TestConsumer",
						"TestProvider",
						AuthorizationTargetType.SERVICE_DEF,
						null,
						null,
						null));

		assertEquals("target is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveTargetEmpty() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(
						AuthorizationTokenType.TIME_LIMITED_TOKEN,
						"tokenHash",
						"AdminSystem",
						"LOCAL",
						"TestConsumer",
						"TestProvider",
						AuthorizationTargetType.SERVICE_DEF,
						"",
						null,
						null));

		assertEquals("target is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveExpiresAtNull() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> dbService.save(
						AuthorizationTokenType.TIME_LIMITED_TOKEN,
						"tokenHash",
						"AdminSystem",
						"LOCAL",
						"TestConsumer",
						"TestProvider",
						AuthorizationTargetType.SERVICE_DEF,
						"serviceDef",
						null,
						null));

		assertEquals("expiresAt is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveInternalServerError() {
		when(tokenHeaderRepo.findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope("LOCAL", "TestConsumer", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef", null))
				.thenThrow(RuntimeException.class);

		final Throwable ex = assertThrows(
				InternalServerError.class,
				() -> dbService.save(
						AuthorizationTokenType.TIME_LIMITED_TOKEN,
						"tokenHash",
						"AdminSystem",
						"LOCAL",
						"TestConsumer",
						"TestProvider",
						AuthorizationTargetType.SERVICE_DEF,
						"serviceDef",
						null,
						Utilities.utcNow()));

		assertEquals("Database operation error", ex.getMessage());

		verify(tokenHeaderRepo).findByConsumerCloudAndConsumerAndProviderAndTargetTypeAndTargetAndScope("LOCAL", "TestConsumer", "TestProvider", AuthorizationTargetType.SERVICE_DEF, "serviceDef", null);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testSaveNoOverride() {
		final ZonedDateTime expiresAt = Utilities.utcNow().plusHours(1);

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

		final TokenHeader savedHeader = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		savedHeader.setId(1);

		final TimeLimitedToken token = new TimeLimitedToken(savedHeader, expiresAt);
		final TimeLimitedToken savedToken = new TimeLimitedToken(savedHeader, expiresAt);
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

		final Pair<TimeLimitedToken, Boolean> result = dbService.save(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				expiresAt);

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
		final ZonedDateTime expiresAt = Utilities.utcNow().plusHours(1);

		final TokenHeader zombie = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
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
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");

		final TokenHeader savedHeader = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		savedHeader.setId(2);

		final TimeLimitedToken token = new TimeLimitedToken(savedHeader, expiresAt);
		final TimeLimitedToken savedToken = new TimeLimitedToken(savedHeader, expiresAt);
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

		final Pair<TimeLimitedToken, Boolean> result = dbService.save(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				expiresAt);

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
		final ZonedDateTime expiresAt = Utilities.utcNow().plusHours(1);

		final TokenHeader oldHeader = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		oldHeader.setId(1);

		final TimeLimitedToken oldToken = new TimeLimitedToken(oldHeader, expiresAt);

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

		final TokenHeader savedHeader = new TokenHeader(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation");
		savedHeader.setId(2);

		final TimeLimitedToken token = new TimeLimitedToken(savedHeader, expiresAt);
		final TimeLimitedToken savedToken = new TimeLimitedToken(savedHeader, expiresAt);
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

		final Pair<TimeLimitedToken, Boolean> result = dbService.save(
				AuthorizationTokenType.TIME_LIMITED_TOKEN,
				"tokenHash",
				"AdminSystem",
				"LOCAL",
				"TestConsumer",
				"TestProvider",
				AuthorizationTargetType.SERVICE_DEF,
				"serviceDef",
				"operation",
				expiresAt);

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

		final Optional<TimeLimitedToken> result = dbService.getByHeader(header);

		assertTrue(result.isEmpty());

		verify(tokenRepo).findByHeader(header);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetByHeaderOk() {
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

		final TimeLimitedToken token = new TimeLimitedToken(header, Utilities.utcNow().plusHours(1));
		token.setId(1);

		when(tokenRepo.findByHeader(header)).thenReturn(Optional.of(token));

		final Optional<TimeLimitedToken> result = dbService.getByHeader(header);

		assertFalse(result.isEmpty());
		assertEquals(token, result.get());

		verify(tokenRepo).findByHeader(header);
	}
}