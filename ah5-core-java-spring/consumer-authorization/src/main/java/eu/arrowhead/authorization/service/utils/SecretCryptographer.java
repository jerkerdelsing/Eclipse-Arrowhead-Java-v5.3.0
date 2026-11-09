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

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;

@Service
public class SecretCryptographer {

	//=================================================================================================
	// members

	public static final String AES_ECB_ALGORITHM = "AES/ECB/PKCS5Padding"; // Without initialization vector
	public static final String AES_CBC_ALGORITHM_IV_BASED = "AES/CBC/PKCS5Padding"; // With initialization vector
	public static final String DEFAULT_ENCRYPTION_ALGORITHM = AES_ECB_ALGORITHM;
	public static final int AES_KEY_MIN_SIZE = 16; // 128 bits
	public static final int IV_KEY_SIZE = 16; // 128 bits

	private static final String AES_KEY_ALGORITHM = "AES";
	private static final String HMAC_ALGORITHM = "HmacSHA256";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public String generateInitializationVectorBase64() {
		return Base64.getEncoder().encodeToString(generateIV());
	}

	// ENCRYPTION

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodName")
	public String encrypt_AES_ECB_PKCS5P(final String rawData, final String key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(rawData), "rawData is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");

		final SecretKeySpec keySpec = getAESKeySpecFromString(key);
		final Cipher cipher = Cipher.getInstance(AES_ECB_ALGORITHM);

		cipher.init(Cipher.ENCRYPT_MODE, keySpec);
		final byte[] encrypted = cipher.doFinal(rawData.getBytes());

		return Base64.getEncoder().encodeToString(encrypted);
	}

	//-------------------------------------------------------------------------------------------------
	// returns a pair with the following items: encrypted and Base64 encoded version of rawData, Base64 encoded version of a generated initialization vector
	@SuppressWarnings("checkstyle:MethodName")
	public Pair<String, String> encrypt_AES_CBC_PKCS5P_IV(final String rawData, final String key)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

		return encrypt_AES_CBC_PKCS5P_IV(rawData, key, generateIV());
	}

	//-------------------------------------------------------------------------------------------------
	// returns a pair with the following items: encrypted and Base64 encoded version of rawData, Base64 encoded version of the initialization vector (iv)
	@SuppressWarnings("checkstyle:MethodName")
	public Pair<String, String> encrypt_AES_CBC_PKCS5P_IV(final String rawData, final String key, final byte[] iv)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(rawData), "rawData is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");
		Assert.notNull(iv, "iv is null");
		Assert.isTrue(iv.length == IV_KEY_SIZE, "Invalid iv length");

		final String encryptedBase64 = encrypt_AES_CBC_PKCS5P_IV(rawData, key, getIvParameterSpec(iv));
		final String ivBase64 = Base64.getEncoder().encodeToString(iv);

		return Pair.of(encryptedBase64, ivBase64);
	}

	//-------------------------------------------------------------------------------------------------
	// returns a pair with the following items: encrypted and Base64 encoded version of rawData, ivBase64
	@SuppressWarnings("checkstyle:MethodName")
	public Pair<String, String> encrypt_AES_CBC_PKCS5P_IV(final String rawData, final String key, final String ivBase64)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(rawData), "rawData is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");

		final String encryptedBase64 = encrypt_AES_CBC_PKCS5P_IV(rawData, key, getIvParameterSpec(ivBase64));

		return Pair.of(encryptedBase64, ivBase64);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodName")
	public String encrypt_HMAC_SHA256(final String rawData, final String key) throws NoSuchAlgorithmException, InvalidKeyException {
		Assert.isTrue(!Utilities.isEmpty(rawData), "rawData is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");

		final Mac sha256HMAC = Mac.getInstance(HMAC_ALGORITHM);
		final SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), HMAC_ALGORITHM);
		sha256HMAC.init(keySpec);

		final byte[] hash = sha256HMAC.doFinal(rawData.getBytes());

		return Base64.getEncoder().encodeToString(hash);
	}

	// DECRYPTION

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodName")
	public String decrypt_AES_ECB_PKCS5P(final String encryptedDataBase64, final String key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(encryptedDataBase64), "encryptedDataBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");

		final SecretKeySpec keySpec = getAESKeySpecFromString(key);
		final Cipher cipher = Cipher.getInstance(AES_ECB_ALGORITHM);

		cipher.init(Cipher.DECRYPT_MODE, keySpec);
		final byte[] encryptedBytes = Base64.getDecoder().decode(encryptedDataBase64);
		final byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

		return new String(decryptedBytes, StandardCharsets.ISO_8859_1);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodName")
	public String decrypt_AES_CBC_PKCS5P_IV(final String encryptedDataBase64, final String ivBase64, final String key)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.isTrue(!Utilities.isEmpty(encryptedDataBase64), "encryptedDataBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(ivBase64), "ivBase64 is empty");
		Assert.isTrue(!Utilities.isEmpty(key), "key is empty");

		final SecretKeySpec keySpec = getAESKeySpecFromString(key);
		final byte[] iv = Base64.getDecoder().decode(ivBase64);
		final IvParameterSpec ivSpec = new IvParameterSpec(iv);
		final Cipher cipher = Cipher.getInstance(AES_CBC_ALGORITHM_IV_BASED);

		cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
		final byte[] encryptedBytes = Base64.getDecoder().decode(encryptedDataBase64);
		final byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

		return new String(decryptedBytes, StandardCharsets.ISO_8859_1);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private byte[] generateIV() {
		final byte[] iv = new byte[IV_KEY_SIZE];
		new SecureRandom().nextBytes(iv);

		return iv;
	}

	//-------------------------------------------------------------------------------------------------
	private IvParameterSpec getIvParameterSpec(final byte[] iv) {
		return new IvParameterSpec(iv);
	}

	//-------------------------------------------------------------------------------------------------
	private IvParameterSpec getIvParameterSpec(final String ivBase64) {
		return new IvParameterSpec(Base64.getDecoder().decode(ivBase64));
	}

	//-------------------------------------------------------------------------------------------------
	private SecretKeySpec getAESKeySpecFromString(final String key) {
		final byte[] keyBytes = key.getBytes();
		Assert.isTrue(keyBytes.length >= AES_KEY_MIN_SIZE, "Key must be minimum " + AES_KEY_MIN_SIZE + " bytes long");

		return new SecretKeySpec(keyBytes, AES_KEY_ALGORITHM);
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodName")
	private String encrypt_AES_CBC_PKCS5P_IV(final String rawData, final String key, final IvParameterSpec ivSpec)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		Assert.notNull(ivSpec, "ivSpec is null");

		final SecretKeySpec keySpec = getAESKeySpecFromString(key);
		final Cipher cipher = Cipher.getInstance(AES_CBC_ALGORITHM_IV_BASED);

		cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
		final byte[] encrypted = cipher.doFinal(rawData.getBytes());

		return Base64.getEncoder().encodeToString(encrypted);
	}
}