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
package eu.arrowhead.common.security;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ResourceUtils;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.security.SecurityUtilities.CommonNameAndType;
import eu.arrowhead.common.service.normalization.NormalizationMode;
import eu.arrowhead.common.service.validation.cloud.CloudIdentifierNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;

public class SecurityUtilitiesTest {

	//=================================================================================================
	// members

	private SystemNameNormalizer systemNameNormalizer;
	private CloudIdentifierNormalizer cloudIdentifierNormalizer;
	private ApplicationContext appContext;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getCertificateFromKeyStoreNullKeyStore() {
		final Throwable ex = assertThrows(IllegalArgumentException.class, () -> {
			SecurityUtilities.getCertificateFromKeyStore(null, null);
		});
		assertEquals("Key store is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getCertificateFromKeyStoreNullAlias() {
		final Throwable ex = assertThrows(IllegalArgumentException.class, () -> {
			SecurityUtilities.getCertificateFromKeyStore(KeyStore.getInstance(KeyStore.getDefaultType()), null);
		});
		assertEquals("Alias is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getCertificateFromKeyStoreEmptyAlias() {
		final Throwable ex = assertThrows(IllegalArgumentException.class, () -> {
			SecurityUtilities.getCertificateFromKeyStore(KeyStore.getInstance(KeyStore.getDefaultType()), " ");
		});
		assertEquals("Alias is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getCertificateFromKeyStoreInvalidKeyStore() {
		final Throwable ex = assertThrows(ServiceConfigurationError.class, () -> {
			SecurityUtilities.getCertificateFromKeyStore(KeyStore.getInstance(KeyStore.getDefaultType()), "test");
		});
		assertEquals("Accessing certificate from key store failed...", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getCertificateFromKeyStoreInvalidAlias() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = initializeTestKeyStore("test.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "wrong");
		assertNull(cert);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getCertificateFromKeyStoreOk() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = initializeTestKeyStore("test.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "test.rubin.aitia.arrowhead.eu");
		assertNotNull(cert);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getPrivateKeyFromKeyStoreNullKeyStore() {
		final Throwable ex = assertThrows(IllegalArgumentException.class, () -> {
			SecurityUtilities.getPrivateKeyFromKeyStore(null, null, null);
		});
		assertEquals("Key store is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getPrivateKeyFromKeyStoreNullAlias() {
		final Throwable ex = assertThrows(IllegalArgumentException.class, () -> {
			SecurityUtilities.getPrivateKeyFromKeyStore(KeyStore.getInstance(KeyStore.getDefaultType()), null, null);
		});
		assertEquals("Alias is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getPrivateKeyFromKeyStoreEmptyAlias() {
		final Throwable ex = assertThrows(IllegalArgumentException.class, () -> {
			SecurityUtilities.getPrivateKeyFromKeyStore(KeyStore.getInstance(KeyStore.getDefaultType()), " ", null);
		});
		assertEquals("Alias is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getPrivateKeyFromKeyStoreNullKeyPass() {
		final Throwable ex = assertThrows(IllegalArgumentException.class, () -> {
			SecurityUtilities.getPrivateKeyFromKeyStore(KeyStore.getInstance(KeyStore.getDefaultType()), "test", null);
		});
		assertEquals("Password is not defined", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getPrivateKeyFromKeyStoreInvalidKeyStore() {
		final Throwable ex = assertThrows(ServiceConfigurationError.class, () -> {
			SecurityUtilities.getPrivateKeyFromKeyStore(KeyStore.getInstance(KeyStore.getDefaultType()), "test", "pass");
		});
		assertEquals("Getting the private key from key store failed...", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getPrivateKeyFromKeyStoreInvalidAlias() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = initializeTestKeyStore("test.p12");
		final PrivateKey key = SecurityUtilities.getPrivateKeyFromKeyStore(keyStore, "wrong", "123456");
		assertNull(key);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getPrivateKeyFromKeyStoreWrongPass() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = initializeTestKeyStore("test.p12");
		final Throwable ex = assertThrows(ServiceConfigurationError.class, () -> {
			SecurityUtilities.getPrivateKeyFromKeyStore(keyStore, "test.rubin.aitia.arrowhead.eu", "pass");
		});
		assertEquals("Getting the private key from key store failed...", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getPrivateKeyFromKeyStoreOk() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = initializeTestKeyStore("test.p12");
		final PrivateKey key = SecurityUtilities.getPrivateKeyFromKeyStore(keyStore, "test.rubin.aitia.arrowhead.eu", "123456");
		assertNotNull(key);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromSubjectDNNullDN() {
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromSubjectDN(null);
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromSubjectDNEmptyDN() {
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromSubjectDN(" ");
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromSubjectDNInvalidFormat() {
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromSubjectDN("blabla");
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromSubjectDNNoQualifier() {
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromSubjectDN("cn=Test.Rubin.Aitia.arrowhead.eu");
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromSubjectDNInvalidQualifier() {
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromSubjectDN("cn=Test.Rubin.Aitia.arrowhead.eu,2.5.4.46=#" + HexFormat.of().formatHex("wrong".getBytes()));
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromSubjectDNMissiongCN() {
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromSubjectDN("2.5.4.46=#" + HexFormat.of().formatHex("sy".getBytes()));
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromSubjectDNOk() {
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromSubjectDN("cn=Test.Rubin.Aitia.arrowhead.eu,2.5.4.46=#" + HexFormat.of().formatHex("sy".getBytes())
				+ ",2.5.4.46=#" + HexFormat.of().formatHex("other".getBytes()));

		assertAll("Identification data - ok",
				() -> assertNotNull(data),
				() -> assertEquals("Test.Rubin.Aitia.arrowhead.eu", data.commonName()),
				() -> assertEquals(CertificateProfileType.SYSTEM, data.profileType()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromRequestNullRequest() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> SecurityUtilities.getIdentificationDataFromRequest(null));
		assertEquals("request must not be null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromRequestNoCertificateChain() {
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromRequest(new MockHttpServletRequest());
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromRequestEmptyCertificateChain() {
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, new X509Certificate[0]);
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromRequest(request);
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromRequestInvalidCertificate() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = initializeTestKeyStore("wrong.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "wrong.rubin.aitia.arrowhead.eu");
		final X509Certificate[] certChain = new X509Certificate[1];
		certChain[0] = cert;
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, certChain);
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromRequest(request);
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromRequestOk() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = initializeTestKeyStore("test.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "test.rubin.aitia.arrowhead.eu");
		final X509Certificate[] certChain = new X509Certificate[1];
		certChain[0] = cert;
		final MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(Constants.HTTP_ATTR_JAKARTA_SERVLET_REQUEST_X509_CERTIFICATE, certChain);
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromRequest(request);

		assertAll("Identification data from request - ok",
				() -> assertNotNull(data),
				() -> assertEquals("test.rubin.aitia.arrowhead.eu", data.commonName()),
				() -> assertEquals(CertificateProfileType.SYSTEM, data.profileType()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromCertificateNull() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> SecurityUtilities.getIdentificationDataFromCertificate(null));
		assertEquals("certificate must not be null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromCertificateInvalidCertificate() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = initializeTestKeyStore("wrong.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "wrong.rubin.aitia.arrowhead.eu");
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromCertificate(cert);
		assertNull(data);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getIdentificationDataFromCertificateOk() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keyStore = initializeTestKeyStore("test.p12");
		final X509Certificate cert = SecurityUtilities.getCertificateFromKeyStore(keyStore, "test.rubin.aitia.arrowhead.eu");
		final CommonNameAndType data = SecurityUtilities.getIdentificationDataFromCertificate(cert);

		assertAll("Identification data from certificate - ok",
				() -> assertNotNull(data),
				() -> assertEquals("test.rubin.aitia.arrowhead.eu", data.commonName()),
				() -> assertEquals(CertificateProfileType.SYSTEM, data.profileType()));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isValidSystemCommonNameNull() {
		final boolean valid = SecurityUtilities.isValidSystemCommonName(null);
		assertFalse(valid);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isValidSystemCommonNameEmpty() {
		final boolean valid = SecurityUtilities.isValidSystemCommonName("  ");
		assertFalse(valid);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isValidSystemCommonNameTooShort() {
		final boolean valid = SecurityUtilities.isValidSystemCommonName("rubin.aitia.arrowhead.eu");
		assertFalse(valid);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isValidSystemCommonNameTooLong() {
		final boolean valid = SecurityUtilities.isValidSystemCommonName("plusone.test.rubin.aitia.arrowhead.eu");
		assertFalse(valid);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isValidSystemCommonNameOk() {
		final boolean valid = SecurityUtilities.isValidSystemCommonName("test.rubin.aitia.arrowhead.eu");
		assertTrue(valid);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getCloudCNInvalidFormat() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> SecurityUtilities.getCloudCN("not_a_cn"));
		assertEquals("Client common name is invalid: not_a_cn", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getCloudCNOk() {
		final String cloudCN = SecurityUtilities.getCloudCN("test.rubin.aitia.arrowhead.eu");
		assertEquals("rubin.aitia.arrowhead.eu", cloudCN);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isClientInTheLocalCloudByCNsNoBean() {
		final boolean answer = SecurityUtilities.isClientInTheLocalCloudByCNs(new DummyApplicationContext(), "Test_Rubin_Aitia_arrowhead_eu", "Rubin.Aitia.arrowhead.eu");
		assertFalse(answer);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isClientInTheLocalCloudByCNsBasicTests() {
		assertAll("isClientInTheLocalCloudByCNs - basic",
				() -> assertFalse(SecurityUtilities.isClientInTheLocalCloudByCNs(null, null, null)),
				() -> assertFalse(SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, null, null)),
				() -> assertFalse(SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, " ", null)),
				() -> assertFalse(SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, "Test.Rubin.Aitia.arrowhead.eu", null)),
				() -> assertFalse(SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, "Test.Rubin.Aitia.arrowhead.eu", "")));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isClientInTheLocalCloudByCNsInvalidClientCN() {
		final boolean answer = SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, "Test_Rubin_Aitia_arrowhead_eu", "Rubin.Aitia.arrowhead.eu");
		assertFalse(answer);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isClientInTheLocalCloudByCNsInvalidCloudCN() {
		final boolean answer = SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, "Test.Rubin.Aitia.arrowhead.eu", "RubinAitia.arrowhead.eu");
		assertFalse(answer);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isClientInTheLocalCloudByCNsNo() {
		final boolean answer = SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, "Test.Rubin.Aitia.arrowhead.eu", "Testcloud.Aitia.arrowhead.eu");
		assertFalse(answer);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void isClientInTheLocalCloudByCNsYes() {
		final boolean answer = SecurityUtilities.isClientInTheLocalCloudByCNs(appContext, "Test.Rubin.Aitia.arrowhead.eu", "Rubin.Aitia.arrowhead.eu");
		assertTrue(answer);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getClientNameFromClientCNNull() {
		final String clientName = SecurityUtilities.getClientNameFromClientCN(null);
		assertNull(clientName);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void getClientNameFromClientCNOk() {
		final String clientName = SecurityUtilities.getClientNameFromClientCN("test.rubin.aitia.arrowhead.eu");
		assertAll("getClientNameFromClientCN - ok",
				() -> assertNotNull(clientName),
				() -> assertEquals("test", clientName));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void hashWithSecretKeyNullData() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> SecurityUtilities.hashWithSecretKey(null, "abcdef"));
		assertEquals("data is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void hashWithSecretKeyEmptyData() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> SecurityUtilities.hashWithSecretKey(" ", "abcdef"));
		assertEquals("data is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void hashWithSecretKeyNullSecret() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> SecurityUtilities.hashWithSecretKey("testData", null));
		assertEquals("secretKey is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void hashWithSecretKeyEmptySecret() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> SecurityUtilities.hashWithSecretKey("testData", ""));
		assertEquals("secretKey is missing", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void hashWithSecretKeyOk() throws InvalidKeyException, NoSuchAlgorithmException {
		final String result = SecurityUtilities.hashWithSecretKey("testData", "abcdef");
		assertEquals("aeaf6a892cc3a9a1385cf1ec708174035f168008aaa8b02db6a7790e136d7119", result);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testGetDecodedUri() {
		assertAll(
				() -> assertNull(SecurityUtilities.getDecodedUri(null), "null input should return null"),
				() -> assertEquals("", SecurityUtilities.getDecodedUri(""), "empty string should be returned as-is"),
				() -> assertEquals("   ", SecurityUtilities.getDecodedUri("   "), "blank string should be returned as-is"),
				() -> assertEquals("hello", SecurityUtilities.getDecodedUri("hello"), "plain string with no encoding"),
				() -> assertEquals("hello world", SecurityUtilities.getDecodedUri("hello%20world"), "decodes %20 to space"),
				() -> assertEquals("hello world", SecurityUtilities.getDecodedUri("  hello%20world  "), "trims before decoding"),
				() -> assertEquals("a=b&c=d", SecurityUtilities.getDecodedUri("a%3Db%26c%3Dd"), "decodes special characters"),
				() -> assertEquals("café", SecurityUtilities.getDecodedUri("caf%C3%A9"), "decodes UTF-8 multi-byte sequences"),
				() -> assertEquals("100%", SecurityUtilities.getDecodedUri("100%25"), "decodes literal percent sign"),
				() -> assertThrows(IllegalArgumentException.class,
				() -> SecurityUtilities.getDecodedUri("100%"), "malformed encoding throws"));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private KeyStore initializeTestKeyStore(final String fileName) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		final KeyStore keystore = KeyStore.getInstance("pkcs12");
		try (InputStream input = new FileInputStream(ResourceUtils.getFile("src/test/resources/certs/" + fileName))) {
			keystore.load(input, "123456".toCharArray());
		}

		return keystore;
	}

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	private void init() {
		if (systemNameNormalizer == null) {
			systemNameNormalizer = new SystemNameNormalizer();
			ReflectionTestUtils.setField(systemNameNormalizer, "normalizationMode", NormalizationMode.EXTENDED);
		}

		if (cloudIdentifierNormalizer == null) {
			cloudIdentifierNormalizer = new CloudIdentifierNormalizer();
			ReflectionTestUtils.setField(cloudIdentifierNormalizer, "systemNameNormalizer", systemNameNormalizer);
		}

		initAppContext();
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("checkstyle:MethodLength")
	private void initAppContext() {
		if (appContext == null) {
			appContext = new NotSoDummyApplicationContext();
		}
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	private class DummyApplicationContext implements ApplicationContext {

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		@Override
		public <T> T getBean(final Class<T> requiredType) throws BeansException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <T> T getBean(final Class<T> requiredType, final Object... args) throws BeansException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Environment getEnvironment() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean containsBeanDefinition(final String beanName) {
			return false;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public int getBeanDefinitionCount() {
			return 0;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String[] getBeanDefinitionNames() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <T> ObjectProvider<T> getBeanProvider(final Class<T> requiredType, final boolean allowEagerInit) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <T> ObjectProvider<T> getBeanProvider(final ResolvableType requiredType, final boolean allowEagerInit) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String[] getBeanNamesForType(final ResolvableType type) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String[] getBeanNamesForType(final ResolvableType type, final boolean includeNonSingletons, final boolean allowEagerInit) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String[] getBeanNamesForType(final Class<?> type) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String[] getBeanNamesForType(final Class<?> type, final boolean includeNonSingletons, final boolean allowEagerInit) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <T> Map<String, T> getBeansOfType(final Class<T> type) throws BeansException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <T> Map<String, T> getBeansOfType(final Class<T> type, final boolean includeNonSingletons, final boolean allowEagerInit) throws BeansException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String[] getBeanNamesForAnnotation(final Class<? extends Annotation> annotationType) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Map<String, Object> getBeansWithAnnotation(final Class<? extends Annotation> annotationType) throws BeansException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <A extends Annotation> A findAnnotationOnBean(final String beanName, final Class<A> annotationType) throws NoSuchBeanDefinitionException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <A extends Annotation> A findAnnotationOnBean(final String beanName, final Class<A> annotationType, final boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <A extends Annotation> Set<A> findAllAnnotationsOnBean(final String beanName, final Class<A> annotationType, final boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Object getBean(final String name) throws BeansException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <T> T getBean(final String name, final Class<T> requiredType) throws BeansException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Object getBean(final String name, final Object... args) throws BeansException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <T> ObjectProvider<T> getBeanProvider(final Class<T> requiredType) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public <T> ObjectProvider<T> getBeanProvider(final ResolvableType requiredType) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean containsBean(final String name) {
			return false;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean isSingleton(final String name) throws NoSuchBeanDefinitionException {
			return false;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean isPrototype(final String name) throws NoSuchBeanDefinitionException {
			return false;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean isTypeMatch(final String name, final ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
			return false;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean isTypeMatch(final String name, final Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
			return false;
		}

		@Override
		public Class<?> getType(final String name) throws NoSuchBeanDefinitionException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Class<?> getType(final String name, final boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String[] getAliases(final String name) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public BeanFactory getParentBeanFactory() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean containsLocalBean(final String name) {
			return false;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getMessage(final String code, final Object[] args, final String defaultMessage, final Locale locale) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getMessage(final String code, final Object[] args, final Locale locale) throws NoSuchMessageException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getMessage(final MessageSourceResolvable resolvable, final Locale locale) throws NoSuchMessageException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public void publishEvent(final Object event) {
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Resource[] getResources(final String locationPattern) throws IOException {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Resource getResource(final String location) {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public ClassLoader getClassLoader() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getId() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getApplicationName() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String getDisplayName() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public long getStartupDate() {
			return 0;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public ApplicationContext getParent() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
			return null;
		}
	}

	//-------------------------------------------------------------------------------------------------
	private final class NotSoDummyApplicationContext extends DummyApplicationContext {

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getBean(final Class<T> requiredType) throws BeansException {
			if (requiredType.equals(CloudIdentifierNormalizer.class)) {
				return (T) cloudIdentifierNormalizer;
			}
			return null;
		}
	}
}