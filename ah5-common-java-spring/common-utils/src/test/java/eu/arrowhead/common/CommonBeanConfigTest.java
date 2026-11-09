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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import eu.arrowhead.common.collector.HttpCollectorDriver;
import eu.arrowhead.common.collector.ICollectorDriver;
import eu.arrowhead.common.http.filter.NoOpArrowheadFilter;
import eu.arrowhead.common.http.filter.authentication.AuthenticationPolicy;
import eu.arrowhead.common.http.filter.authentication.CertificateFilter;
import eu.arrowhead.common.http.filter.authentication.OutsourcedFilter;
import eu.arrowhead.common.http.filter.authentication.SelfDeclaredFilter;
import eu.arrowhead.common.http.filter.authorization.ManagementServiceFilter;
import eu.arrowhead.common.mqtt.filter.authentication.CertificateMqttFilter;
import eu.arrowhead.common.mqtt.filter.authentication.OutsourcedMqttFilter;
import eu.arrowhead.common.mqtt.filter.authentication.SelfDeclaredMqttFilter;
import eu.arrowhead.common.mqtt.filter.authorization.ManagementServiceMqttFilter;

@ExtendWith(MockitoExtension.class)
public class CommonBeanConfigTest {

	//=================================================================================================
	// members

	@InjectMocks
	private CommonBeanConfig conf;

	@Mock
	private ApplicationContext appContext;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetArrowheadContext() {
		final Map<String, Object> result = conf.getArrowheadContext();

		assertNotNull(result);
		assertTrue(result instanceof ConcurrentHashMap);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAuthenticationPolicyFilterValid() {
		assertAll("authenticationPolicyFilter - valid cases",
				() -> assertTrue(conf.authenticationPolicyFilter(AuthenticationPolicy.CERTIFICATE) instanceof CertificateFilter),
				() -> assertTrue(conf.authenticationPolicyFilter(AuthenticationPolicy.OUTSOURCED) instanceof OutsourcedFilter),
				() -> assertTrue(conf.authenticationPolicyFilter(AuthenticationPolicy.DECLARED) instanceof SelfDeclaredFilter));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAuthenticationPolicyFilterInvalid() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> conf.authenticationPolicyFilter(AuthenticationPolicy.INTERNAL));

		assertEquals("Invalid policy: INTERNAL", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testManagementServiceFilterConsumerAuthorization() {
		final SystemInfo sysInfoMock = Mockito.mock(SystemInfo.class);

		when(appContext.getBean("systemInfo")).thenReturn(sysInfoMock);
		when(sysInfoMock.getSystemName()).thenReturn("ConsumerAuthorization");

		assertTrue(conf.managementServiceFilter() instanceof NoOpArrowheadFilter);

		verify(appContext).getBean("systemInfo");
		verify(sysInfoMock).getSystemName();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testManagementServiceFilterNotConsumerAuthorization() {
		final SystemInfo sysInfoMock = Mockito.mock(SystemInfo.class);

		when(appContext.getBean("systemInfo")).thenReturn(sysInfoMock);
		when(sysInfoMock.getSystemName()).thenReturn("ServiceRegistry");

		assertTrue(conf.managementServiceFilter() instanceof ManagementServiceFilter);

		verify(appContext).getBean("systemInfo");
		verify(sysInfoMock).getSystemName();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAuthenticationPolicyMqttFilterValid() {
		assertAll("authenticationPolicyMqttFilter - valid cases",
				() -> assertNull(conf.authenticationPolicyMqttFilter(false, AuthenticationPolicy.CERTIFICATE)),
				() -> assertTrue(conf.authenticationPolicyMqttFilter(true, AuthenticationPolicy.CERTIFICATE) instanceof CertificateMqttFilter),
				() -> assertTrue(conf.authenticationPolicyMqttFilter(true, AuthenticationPolicy.OUTSOURCED) instanceof OutsourcedMqttFilter),
				() -> assertTrue(conf.authenticationPolicyMqttFilter(true, AuthenticationPolicy.DECLARED) instanceof SelfDeclaredMqttFilter));
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testAuthenticationPolicyMqttFilterInvalid() {
		final Throwable ex = assertThrows(IllegalArgumentException.class,
				() -> conf.authenticationPolicyMqttFilter(true, AuthenticationPolicy.INTERNAL));

		assertEquals("Invalid policy: INTERNAL", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testManagementServiceMqttFilterConsumerAuthorization() {
		final SystemInfo sysInfoMock = Mockito.mock(SystemInfo.class);

		when(appContext.getBean("systemInfo")).thenReturn(sysInfoMock);
		when(sysInfoMock.getSystemName()).thenReturn("ConsumerAuthorization");

		assertNull(conf.managementServiceMqttFilter());

		verify(appContext).getBean("systemInfo");
		verify(sysInfoMock).getSystemName();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testManagementServiceMqttFilterNotConsumerAuthorization() {
		final SystemInfo sysInfoMock = Mockito.mock(SystemInfo.class);

		when(appContext.getBean("systemInfo")).thenReturn(sysInfoMock);
		when(sysInfoMock.getSystemName()).thenReturn("ServiceRegistry");

		assertTrue(conf.managementServiceMqttFilter() instanceof ManagementServiceMqttFilter);

		verify(appContext).getBean("systemInfo");
		verify(sysInfoMock).getSystemName();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testGetDefaultCollectorDriver() {
		final ICollectorDriver result = conf.getDefaultCollectorDriver();

		assertNotNull(result);
		assertTrue(result instanceof HttpCollectorDriver);
	}
}