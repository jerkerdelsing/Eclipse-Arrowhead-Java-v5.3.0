/*******************************************************************************
 * Copyright (c) 2026 Lulea University of Technology
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package eu.arrowhead.choreographer.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.common.exception.InvalidParameterException;

public class VersionsTest {

	@Test
	public void majorOfSemanticVersion() {
		assertEquals(5, Versions.major("5.2.2"));
		assertEquals(1, Versions.major(" 1 "));
		assertThrows(InvalidParameterException.class, () -> Versions.major("x.1"));
		assertThrows(InvalidParameterException.class, () -> Versions.major(""));
	}

	@Test
	public void windowOfVersionList() {
		assertNull(Versions.window(List.of()));
		assertArrayEquals(new int[] { 1, 3 }, Versions.window(List.of("3.0.0", "1.2.0", "2.0.0")));
	}

	@Test
	public void overlapWithOpenStepWindow() {
		assertTrue(Versions.overlaps(null, null, 1, 1));
		assertTrue(Versions.overlaps(2, 3, 3, 9));
		assertFalse(Versions.overlaps(2, 3, 4, 9));
		assertFalse(Versions.overlaps(null, null, 0, 0));
	}
}
