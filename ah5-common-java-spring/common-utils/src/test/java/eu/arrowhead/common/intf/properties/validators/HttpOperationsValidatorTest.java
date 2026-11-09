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
package eu.arrowhead.common.intf.properties.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.common.http.model.HttpOperationModel;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceOperationNameValidator;

@ExtendWith(MockitoExtension.class)
public class HttpOperationsValidatorTest {

	//=================================================================================================
	// members

	@InjectMocks
	private HttpOperationsValidator validator;

	@Mock
	private ServiceOperationNameNormalizer operationNameNormalizer;

	@Mock
	private ServiceOperationNameValidator operationNameValidator;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeNotMap() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize("notAMap"));

		assertEquals("Property value should be a map of operations", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeEmptyMap() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(Map.of()));

		assertEquals("Property value should be a non-empty map", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testValidateAndNormalizeKeyNotString() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(Map.of(1, 2)));

		assertEquals("Key should be a string", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MagicNumber")
	@Test
	public void testValidateAndNormalizeValueNotHttpOperationModel() {
		when(operationNameNormalizer.normalize("operation-name")).thenReturn("operation-name");
		doNothing().when(operationNameValidator).validateServiceOperationName("operation-name");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(Map.of("operation-name", 2)));

		verify(operationNameNormalizer).normalize("operation-name");
		verify(operationNameValidator).validateServiceOperationName("operation-name");

		assertEquals("Value should be a HttpOperationModel record", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizePathEmpty() {
		final Map<String, String> model = Map.of("path", "", "method", "POST");
		when(operationNameNormalizer.normalize("operation-name")).thenReturn("operation-name");
		doNothing().when(operationNameValidator).validateServiceOperationName("operation-name");

		try (MockedStatic<Utilities> mockedUtilities = Mockito.mockStatic(Utilities.class)) {
			mockedUtilities.when(() -> Utilities.toJson(model)).thenCallRealMethod();
			final HttpOperationModel mockedModel = Mockito.mock(HttpOperationModel.class);
			when(mockedModel.path()).thenReturn("");
			mockedUtilities.when(() -> Utilities.fromJson(anyString(), eq(HttpOperationModel.class))).thenReturn(mockedModel);
			mockedUtilities.when(() -> Utilities.isEmpty("")).thenCallRealMethod();

			final Throwable ex = assertThrows(InvalidParameterException.class,
					() -> validator.validateAndNormalize(Map.of("operation-name", model)));

			verify(operationNameNormalizer).normalize("operation-name");
			verify(operationNameValidator).validateServiceOperationName("operation-name");
			mockedUtilities.verify(() -> Utilities.fromJson(anyString(), eq(HttpOperationModel.class)));
			verify(mockedModel).path();

			assertEquals("Path should be non-empty", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateAndNormalizeInvalidMethod() {
		final Map<String, String> model = Map.of("path", "/path", "method", "WRONG");
		when(operationNameNormalizer.normalize("operation-name")).thenReturn("operation-name");
		doNothing().when(operationNameValidator).validateServiceOperationName("operation-name");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> validator.validateAndNormalize(Map.of("operation-name", model)));

		verify(operationNameNormalizer).normalize("operation-name");
		verify(operationNameValidator).validateServiceOperationName("operation-name");

		assertEquals("Method should be a standard HTTP method", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testValidateAndNormalizeOk() {
		final Map<String, String> model = Map.of("path", " /path ", "method", "post");
		when(operationNameNormalizer.normalize("operation-name")).thenReturn("operation-name");
		doNothing().when(operationNameValidator).validateServiceOperationName("operation-name");

		final Object normalizedObj = validator.validateAndNormalize(Map.of("operation-name", model));

		verify(operationNameNormalizer).normalize("operation-name");
		verify(operationNameValidator).validateServiceOperationName("operation-name");

		assertTrue(normalizedObj instanceof Map);

		final Map<String, HttpOperationModel> normalized = (Map<String, HttpOperationModel>) normalizedObj;

		assertTrue(normalized.containsKey("operation-name"));
		assertEquals("/path", normalized.get("operation-name").path());
		assertEquals("POST", normalized.get("operation-name").method());
	}
}