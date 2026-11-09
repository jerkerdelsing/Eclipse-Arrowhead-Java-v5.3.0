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
package eu.arrowhead.authentication.service.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import eu.arrowhead.authentication.jpa.entity.ActiveSession;
import eu.arrowhead.authentication.jpa.entity.System;
import eu.arrowhead.common.Constants;
import eu.arrowhead.dto.IdentityListMgmtResponseDTO;
import eu.arrowhead.dto.IdentityMgmtResponseDTO;
import eu.arrowhead.dto.IdentitySessionListMgmtResponseDTO;
import eu.arrowhead.dto.IdentitySessionResponseDTO;
import eu.arrowhead.dto.enums.AuthenticationMethod;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

public class DTOConverterTest {

	//=================================================================================================
	// members

	private DTOConverter converter = new DTOConverter();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertIdentifiableSystemListToDTO1NullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertIdentifiableSystemListToDTO((List<System>) null));

		assertEquals("systems is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertIdentifiableSystemListToDTO1ListContainsNull() {
		final List<System> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertIdentifiableSystemListToDTO(list));

		assertEquals("system is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertIdentifiableSystemListToDTO1Ok() {
		final ZonedDateTime time = ZonedDateTime.of(2025, 11, 13, 12, 12, 12, 0, ZoneId.of(Constants.UTC));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setCreatedAt(time);
		sys.setUpdatedAt(time);
		final String timeStr = "2025-11-13T12:12:12Z";

		final IdentityListMgmtResponseDTO result = converter.convertIdentifiableSystemListToDTO(List.of(sys));

		assertEquals(1, result.count());
		final IdentityMgmtResponseDTO id = result.identities().get(0);
		assertEquals("TestSystem", id.systemName());
		assertEquals("PASSWORD", id.authenticationMethod());
		assertFalse(id.sysop());
		assertEquals("AdminSystem", id.createdBy());
		assertEquals(timeStr, id.createdAt());
		assertEquals(timeStr, id.updatedAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertIdentifiableSystemListToDTO2NullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertIdentifiableSystemListToDTO((Page<System>) null));

		assertEquals("systems is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertIdentifiableSystemListToDTO2Ok() {
		final ZonedDateTime time = ZonedDateTime.of(2025, 11, 13, 12, 12, 12, 0, ZoneId.of(Constants.UTC));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		sys.setCreatedAt(time);
		sys.setUpdatedAt(time);
		final String timeStr = "2025-11-13T12:12:12Z";

		final IdentityListMgmtResponseDTO result = converter.convertIdentifiableSystemListToDTO(new PageImpl<>(List.of(sys)));

		assertEquals(1, result.count());
		final IdentityMgmtResponseDTO id = result.identities().get(0);
		assertEquals("TestSystem", id.systemName());
		assertEquals("PASSWORD", id.authenticationMethod());
		assertFalse(id.sysop());
		assertEquals("AdminSystem", id.createdBy());
		assertEquals(timeStr, id.createdAt());
		assertEquals(timeStr, id.updatedAt());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSessionListToDTONullList() {
		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertSessionListToDTO(null));

		assertEquals("sessions is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSessionListToDTOListContainsNull() {
		final List<ActiveSession> list = new ArrayList<>(1);
		list.add(null);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> converter.convertSessionListToDTO(new PageImpl<>(list)));

		assertEquals("session is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConvertSessionListToDTOOk() {
		final ZonedDateTime time = ZonedDateTime.of(2025, 11, 13, 12, 12, 12, 0, ZoneId.of(Constants.UTC));
		final ZonedDateTime time2 = ZonedDateTime.of(2025, 11, 13, 13, 12, 12, 0, ZoneId.of(Constants.UTC));
		final System sys = new System("TestSystem", AuthenticationMethod.PASSWORD, false, "AdminSystem");
		final ActiveSession session = new ActiveSession(sys, "token", time, time2);
		final String timeStr = "2025-11-13T12:12:12Z";
		final String timeStr2 = "2025-11-13T13:12:12Z";

		final IdentitySessionListMgmtResponseDTO result = converter.convertSessionListToDTO(new PageImpl<>(List.of(session)));

		assertEquals(1, result.count());
		final IdentitySessionResponseDTO sessionDTO = result.sessions().get(0);
		assertEquals("TestSystem", sessionDTO.systemName());
		assertEquals(timeStr, sessionDTO.loginTime());
		assertEquals(timeStr2, sessionDTO.expirationTime());
	}
}