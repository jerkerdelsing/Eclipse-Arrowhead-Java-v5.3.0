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

package eu.arrowhead.common;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class EnvironmentPropsTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@AfterEach
	public void reset() {
		ReflectionTestUtils.setField(EnvironmentProps.class, "startupFailed", false);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testFailedStartupReleasesWaitingCaller() {
		ReflectionTestUtils.setField(EnvironmentProps.class, "context", null);
		EnvironmentProps.startupFailed();

		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertThrows(IllegalStateException.class, () -> EnvironmentProps.getProperty("any.key")));
		assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertThrows(IllegalStateException.class, () -> EnvironmentProps.getEnvironment()));
	}
}
