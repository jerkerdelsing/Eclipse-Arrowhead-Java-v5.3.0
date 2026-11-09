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
package eu.arrowhead.common.jpa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.logging.LogLevel;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import eu.arrowhead.common.Utilities;
import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
public class RefreshableRepositoryImplTest {

	//=================================================================================================
	// members

	private RefreshableRepositoryImpl<LogEntity, String> impl;

	@Mock
	private EntityManager entityManager;

	@Mock
	private JpaEntityInformation<LogEntity, ?> entityInformation;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		when(entityManager.getDelegate()).thenReturn(new Object());
		impl = new RefreshableRepositoryImpl<>(entityInformation, entityManager);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRefresh() {
		final LogEntity entity = new LogEntity(
				"id",
				Utilities.parseUTCStringToZonedDateTime("2025-07-21T12:41:12Z"),
				"loggername",
				LogLevel.INFO,
				"something important",
				null);

		doNothing().when(entityManager).refresh(any(LogEntity.class));

		assertDoesNotThrow(() -> impl.refresh(entity));

		verify(entityManager).refresh(any(LogEntity.class));
	}
}