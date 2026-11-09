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
package eu.arrowhead.common.mqtt.filter.authorization;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.collector.ServiceCollector;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.AuthException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.dto.MqttRequestTemplate;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class BlacklistMqttFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private BlacklistMqttFilter filter;

	@Mock
	protected SystemInfo sysInfo;

	@Mock
	protected ArrowheadHttpService arrowheadHttpService;

	@Mock
	private ServiceCollector collector;

	//=================================================================================================
	// members

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOrder() {
		assertEquals(20, filter.order());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterSysop() {
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(true);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo, never()).getSystemName();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthenticationLookup() {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName("identity")
				.build();

		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, payload));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry")).thenReturn(serviceDiscoverySM);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtt", "ServiceRegistry");
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterARequesterIsBlacklist() {
		final MqttRequestModel request = new MqttRequestModel("arrowhead/consumer-authorization/authorization/", "verify", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("Blacklist");
		request.setSysOp(false);

		when(sysInfo.getSystemName()).thenReturn("ConsumerAuthorization");

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterARequesterIsOnExcludeList() {
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "register", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of("ServiceRegistry", "RequesterSystem"));

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).getBlacklistCheckExcludeList();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterBlacklistServiceUnavailableForceFalse() {
		ReflectionTestUtils.setField(filter, "force", false);

		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(null);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenThrow(ArrowheadException.class);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterBlacklistServiceUnavailableForceTrue() {
		ReflectionTestUtils.setField(filter, "force", true);

		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtts", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();
		serviceDiscoverySM.interfaces().clear();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenThrow(ArrowheadException.class);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);

		assertEquals("Blacklist system is not available, the system might be blacklisted", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterBlacklistServiceReturnsForbiddenException() {
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenThrow(new ForbiddenException("test forbidden"));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);

		assertEquals("test forbidden", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterBlacklistServiceReturnsAuthException() {
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new DummyMqttInterfaceModel("generic_mqtts"))
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenThrow(new AuthException("test auth"));

		final Throwable ex = assertThrows(AuthException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);

		assertEquals("test auth", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterBlacklistServiceReturnsTrue() {
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtts", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenReturn(true);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);

		assertEquals("RequesterSystem system is blacklisted", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterBlacklistServiceReturnsFalse() {
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/system-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtts", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterLookupWithWrongPayload() {
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtts", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterLookupWithNullPayload() {
		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, null));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtts", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterLookupWithNotAcceptablePayload() {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.providerName("ConsumerSystem")
				.build();

		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, payload));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtts", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterLookupWithNotAcceptablePayload2() {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionNames(List.of("serviceDef1", "serviceDef2"))
				.build();

		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, payload));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtts", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterLookupWithNotAcceptablePayload3() {
		final ServiceInstanceLookupRequestDTO payload = new ServiceInstanceLookupRequestDTO.Builder()
				.serviceDefinitionName("serviceDef1")
				.build();

		final MqttRequestModel request = new MqttRequestModel("arrowhead/serviceregistry/service-discovery/", "lookup", new MqttRequestTemplate("trace", "authKey", "response", 0, null, payload));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final ServiceModel serviceDiscoverySM = new ServiceModel.Builder()
				.serviceDefinition("serviceDiscovery")
				.version("5.0.0")
				.serviceInterface(new MqttInterfaceModel.Builder("generic_mqtts", "localhost", 4763)
						.baseTopic("arrowhead/serviceregistry/service-discovery/")
						.operations(Set.of("register", "lookup", "revoke"))
						.build())
				.build();

		when(sysInfo.getSystemName()).thenReturn("ServiceRegistry");
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(collector.getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry")).thenReturn(serviceDiscoverySM);
		when(sysInfo.getBlacklistCheckExcludeList()).thenReturn(List.of());
		final List<String> pathParams = List.of("RequesterSystem");
		when(arrowheadHttpService.consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams)).thenReturn(false);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getSystemName();
		verify(sysInfo).isSslEnabled();
		verify(collector).getServiceModel("serviceDiscovery", "generic_mqtts", "ServiceRegistry");
		verify(sysInfo).getBlacklistCheckExcludeList();
		verify(arrowheadHttpService).consumeService("blacklistDiscovery", "check", "Blacklist", Boolean.TYPE, pathParams);
	}

	//=================================================================================================
	// nested classes

	//-------------------------------------------------------------------------------------------------
	public static class DummyMqttInterfaceModel implements InterfaceModel {
		private String name;

		//-------------------------------------------------------------------------------------------------
		public DummyMqttInterfaceModel(final String name) {
			this.name = name;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String templateName() {
			return name;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public String protocol() {
			return null;
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public Map<String, Object> properties() {
			return Map.of();
		}
	}
}