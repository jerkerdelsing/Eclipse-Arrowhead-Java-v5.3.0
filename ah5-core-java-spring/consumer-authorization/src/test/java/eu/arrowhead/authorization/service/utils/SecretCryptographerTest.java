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
package eu.arrowhead.authorization.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;

@SuppressWarnings({ "checkstyle:MagicNumber", "checkstyle:MethodName" })
@ExtendWith(MockitoExtension.class)
public class SecretCryptographerTest {

	//=================================================================================================
	// members

	@SuppressWarnings("checkstyle:nowhitespaceafter")
	private static final byte[] TEST_RANDOM_BYTES = new byte[] { 20, -72, -62, -39, -28, 123, -35, 115, -87, 68, -82, -49, -61, 105, 31, 31 };

	@InjectMocks
	private SecretCryptographer sc;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGenerateInitializationVectorBase64() {
		final String expected = "FLjC2eR73XOpRK7Pw2kfHw==";

		try (MockedConstruction<SecureRandom> constructorMock = Mockito.mockConstruction(SecureRandom.class,
				(mock, context) -> {
					doAnswer(invocation -> {
						final byte[] array = (byte[]) invocation.getArgument(0);
						System.arraycopy(TEST_RANDOM_BYTES, 0, array, 0, TEST_RANDOM_BYTES.length);
						return null;
					}).when(mock).nextBytes(any(byte[].class));
				})) {
			assertEquals(expected, sc.generateInitializationVectorBase64());

			final SecureRandom randomMock = constructorMock.constructed().get(0);
			verify(randomMock).nextBytes(any(byte[].class));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_ECB_PKCS5PNullRawData() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_ECB_PKCS5P(null, "aKey"));

		assertEquals("rawData is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_ECB_PKCS5PEmptyKey() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_ECB_PKCS5P("dataToEncrypt", ""));

		assertEquals("key is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_ECB_PKCS5PShortKey() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_ECB_PKCS5P("dataToEncrypt", "shortKey"));

		assertEquals("Key must be minimum 16 bytes long", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_ECB_PKCS5PTooLongKey() {
		final Throwable ex = assertThrows(InvalidKeyException.class,
				() -> sc.encrypt_AES_ECB_PKCS5P("dataToEncrypt", "aKeyWithTooMuchBytes"));

		assertEquals("Invalid AES key length: 20 bytes", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_ECB_PKCS5POk() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		final String expected = "94CUzV4FM/WIlCbdYyu7zQ==";

		final String result = sc.encrypt_AES_ECB_PKCS5P("dataToEncrypt", "a16BytesLongKey1");

		assertEquals(expected, result);
	}

	//-------------------------------------------------------------------------------------------------
	// this test demonstrates that method encrypt_AES_CBC_PKCS5P_IV(String, String) calls the public method encrypt_AES_CBC_PKCS5P_IV(String, String, byte[]) after generating an initialization vector
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithGeneratedIVRawDataNull() {
		try (MockedConstruction<SecureRandom> constructorMock = Mockito.mockConstruction(SecureRandom.class,
				(mock, context) -> {
					doAnswer(invocation -> {
						final byte[] array = (byte[]) invocation.getArgument(0);
						System.arraycopy(TEST_RANDOM_BYTES, 0, array, 0, TEST_RANDOM_BYTES.length);
						return null;
					}).when(mock).nextBytes(any(byte[].class));
				})) {

			final Throwable ex = assertThrows(IllegalArgumentException.class,
					() -> sc.encrypt_AES_CBC_PKCS5P_IV(null, "aKey"));

			SecureRandom randomMock = constructorMock.constructed().get(0);
			verify(randomMock).nextBytes(any(byte[].class));

			assertEquals("rawData is empty", ex.getMessage());
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithSpecifiedIVRawDataEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_CBC_PKCS5P_IV("", "aKey", TEST_RANDOM_BYTES));

		assertEquals("rawData is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithSpecifiedIVKeyNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_CBC_PKCS5P_IV("dataToEncrypt", null, TEST_RANDOM_BYTES));

		assertEquals("key is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithSpecifiedIVNullIV() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_CBC_PKCS5P_IV("dataToEncrypt", "aKey", (byte[]) null));

		assertEquals("iv is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:nowhitespaceafter")
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithSpecifiedIVShortIV() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_CBC_PKCS5P_IV("dataToEncrypt", "aKey", new byte[] { 0, 12, 13 }));

		assertEquals("Invalid iv length", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:nowhitespaceafter")
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithSpecifiedIVTooLongIV() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_CBC_PKCS5P_IV("dataToEncrypt", "aKey", new byte[] { 20, -72, -62, -39, -28, 123, -35, 115, -87, 68, -82, -49, -61, 105, 31, 31, 22 }));

		assertEquals("Invalid iv length", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithSpecifiedIVOk() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final String expected = "lMfSoi7gilJdbScSH7xVKw==";
		final String expectedIVStr = "FLjC2eR73XOpRK7Pw2kfHw==";

		final Pair<String, String> result = sc.encrypt_AES_CBC_PKCS5P_IV("dataToEncrypt", "a16BytesLongKey1", TEST_RANDOM_BYTES);

		assertEquals(expected, result.getFirst());
		assertEquals(expectedIVStr, result.getSecond());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithSpecifiedEncodedIVRawDataNull() {
		final String encodedIV = "FLjC2eR73XOpRK7Pw2kfHw==";

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_CBC_PKCS5P_IV(null, "aKey", encodedIV));

		assertEquals("rawData is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithSpecifiedEncodedIVKeyEmpty() {
		final String encodedIV = "FLjC2eR73XOpRK7Pw2kfHw==";

		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_AES_CBC_PKCS5P_IV("dataToEncrypt", "", encodedIV));

		assertEquals("key is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_AES_CBC_PKCS5P_IVWithSpecifiedEncodedIVOk() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final String encodedIV = "FLjC2eR73XOpRK7Pw2kfHw==";
		final String expected = "lMfSoi7gilJdbScSH7xVKw==";

		final Pair<String, String> result = sc.encrypt_AES_CBC_PKCS5P_IV("dataToEncrypt", "a16BytesLongKey1", encodedIV);

		assertEquals(expected, result.getFirst());
		assertEquals(encodedIV, result.getSecond());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_HMAC_SHA256RawDataEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_HMAC_SHA256("", "aKey"));

		assertEquals("rawData is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_HMAC_SHA256KeyNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.encrypt_HMAC_SHA256("dataToEncrypt", null));

		assertEquals("key is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testEncrypt_HMAC_SHA256Ok() throws InvalidKeyException, NoSuchAlgorithmException {
		final String expected = "PH+LluiHzMWPs+4XlMTEB+KN9gIdO94z26B3JZmMX44=";

		final String result = sc.encrypt_HMAC_SHA256("dataToEncrypt", "aKey");

		assertEquals(expected, result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecrypt_AES_ECB_PKCS5PEncryptedDataNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.decrypt_AES_ECB_PKCS5P(null, "aKey"));

		assertEquals("encryptedDataBase64 is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecrypt_AES_ECB_PKCS5PKeyEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.decrypt_AES_ECB_PKCS5P("94CUzV4FM/WIlCbdYyu7zQ==", ""));

		assertEquals("key is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecrypt_AES_ECB_PKCS5POk() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
		final String result = sc.decrypt_AES_ECB_PKCS5P("94CUzV4FM/WIlCbdYyu7zQ==", "a16BytesLongKey1");

		assertEquals("dataToEncrypt", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecrypt_AES_CBC_PKCS5P_IVEncryptedDataEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.decrypt_AES_CBC_PKCS5P_IV("", "FLjC2eR73XOpRK7Pw2kfHw==", "a16BytesLongKey1"));

		assertEquals("encryptedDataBase64 is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecrypt_AES_CBC_PKCS5P_IVEncodedIVNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.decrypt_AES_CBC_PKCS5P_IV("lMfSoi7gilJdbScSH7xVKw==", null, "a16BytesLongKey1"));

		assertEquals("ivBase64 is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecrypt_AES_CBC_PKCS5P_IVKeyEmpty() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> sc.decrypt_AES_CBC_PKCS5P_IV("lMfSoi7gilJdbScSH7xVKw==", "FLjC2eR73XOpRK7Pw2kfHw==", ""));

		assertEquals("key is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDecrypt_AES_CBC_PKCS5P_IVOk() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException {
		final String result = sc.decrypt_AES_CBC_PKCS5P_IV("lMfSoi7gilJdbScSH7xVKw==", "FLjC2eR73XOpRK7Pw2kfHw==", "a16BytesLongKey1");

		assertEquals("dataToEncrypt", result);
	}
}