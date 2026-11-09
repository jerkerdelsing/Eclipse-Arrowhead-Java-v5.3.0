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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.exception.InvalidParameterException;

public class SSLPropertiesTest {

	//=================================================================================================
	// members

	private SSLProperties props = new SSLProperties();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	public void setUp() {
		ReflectionTestUtils.setField(props, "sslEnabled", true);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateNoSsl() {
		ReflectionTestUtils.setField(props, "sslEnabled", false);

		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(props, "validate"));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateKeyStoreTypeNull() {
		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("keyStoreType is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateKeyStoreTypeEmpty() {
		ReflectionTestUtils.setField(props, "keyStoreType", "");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("keyStoreType is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateKeyStoreNull() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("keyStore is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateKeyStorePasswordNull() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("keyStorePassword is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateKeyStorePasswordEmpty() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));
		ReflectionTestUtils.setField(props, "keyStorePassword", "");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("keyStorePassword is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateKeyPasswordNull() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));
		ReflectionTestUtils.setField(props, "keyStorePassword", "123456");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("keyPassword is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateKeyPasswordEmpty() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));
		ReflectionTestUtils.setField(props, "keyStorePassword", "123456");
		ReflectionTestUtils.setField(props, "keyPassword", "");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("keyPassword is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateKeyAliasNull() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));
		ReflectionTestUtils.setField(props, "keyStorePassword", "123456");
		ReflectionTestUtils.setField(props, "keyPassword", "123456");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("keyAlias is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateKeyAliasEmpty() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));
		ReflectionTestUtils.setField(props, "keyStorePassword", "123456");
		ReflectionTestUtils.setField(props, "keyPassword", "123456");
		ReflectionTestUtils.setField(props, "keyAlias", "");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("keyAlias is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateTrustStoreNull() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));
		ReflectionTestUtils.setField(props, "keyStorePassword", "123456");
		ReflectionTestUtils.setField(props, "keyPassword", "123456");
		ReflectionTestUtils.setField(props, "keyAlias", "ConsumerAuthorization.TestCloud.Company.arrowhead.eu");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("trustStore is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateTrustStorePasswordNull() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));
		ReflectionTestUtils.setField(props, "keyStorePassword", "123456");
		ReflectionTestUtils.setField(props, "keyPassword", "123456");
		ReflectionTestUtils.setField(props, "keyAlias", "ConsumerAuthorization.TestCloud.Company.arrowhead.eu");
		ReflectionTestUtils.setField(props, "trustStore", new ClassPathResource("certs/truststore.p12"));

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("trustStorePassword is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateTrustStorePasswordEmpty() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));
		ReflectionTestUtils.setField(props, "keyStorePassword", "123456");
		ReflectionTestUtils.setField(props, "keyPassword", "123456");
		ReflectionTestUtils.setField(props, "keyAlias", "ConsumerAuthorization.TestCloud.Company.arrowhead.eu");
		ReflectionTestUtils.setField(props, "trustStore", new ClassPathResource("certs/truststore.p12"));
		ReflectionTestUtils.setField(props, "trustStorePassword", "");

		final Throwable ex = assertThrows(InvalidParameterException.class,
				() -> ReflectionTestUtils.invokeMethod(props, "validate"));

		assertEquals("trustStorePassword is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testValidateOk() {
		ReflectionTestUtils.setField(props, "keyStoreType", "pkcs12");
		ReflectionTestUtils.setField(props, "keyStore", new ClassPathResource("certs/ConsumerAuthorization.p12"));
		ReflectionTestUtils.setField(props, "keyStorePassword", "123456");
		ReflectionTestUtils.setField(props, "keyPassword", "123456");
		ReflectionTestUtils.setField(props, "keyAlias", "ConsumerAuthorization.TestCloud.Company.arrowhead.eu");
		ReflectionTestUtils.setField(props, "trustStore", new ClassPathResource("certs/truststore.p12"));
		ReflectionTestUtils.setField(props, "trustStorePassword", "123456");

		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(props, "validate"));
	}
}