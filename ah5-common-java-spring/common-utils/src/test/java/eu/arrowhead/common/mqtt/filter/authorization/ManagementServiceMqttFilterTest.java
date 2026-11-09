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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.SystemInfo;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.http.filter.authorization.ManagementPolicy;
import eu.arrowhead.common.model.InterfaceModel;
import eu.arrowhead.common.model.ServiceModel;
import eu.arrowhead.common.mqtt.model.MqttInterfaceModel;
import eu.arrowhead.common.mqtt.model.MqttRequestModel;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.dto.AuthorizationVerifyRequestDTO;
import eu.arrowhead.dto.MqttRequestTemplate;

@SuppressWarnings("checkstyle:MagicNumber")
@ExtendWith(MockitoExtension.class)
public class ManagementServiceMqttFilterTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ManagementServiceMqttFilter filter;

	@Mock
	private SystemInfo sysInfo;

	@Mock
	private ArrowheadHttpService httpService;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testOrder() {
		assertEquals(25, filter.order());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterNotManagementTopic() {
		final MqttRequestModel request = new MqttRequestModel("test/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo, never()).getManagementPolicy();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterSysopOnlySysop() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(true);

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.SYSOP_ONLY);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterSysopOnlyNotSysop() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.SYSOP_ONLY);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterWhitelistSysop() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(true);

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterWhitelistIsWhitelisted() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("RequesterSystem"));

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterWhitelistNotWhitelisted() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.WHITELIST);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("NotTheRequesterSystem"));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationSysop() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(true);

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationIsWhitelisted() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("RequesterSystem"));

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorizedServiceDefNotFoundNoServices() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("NotTheRequesterSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of());

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorizedServiceDefNotFoundInterfaceDoesNotMatch() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final InterfaceModel serviceInterfaceModel = new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
				.baseTopic("something/management/")
				.operation("test-operation")
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("NotTheRequesterSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(true);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorizedServiceDefNotFoundBaseTopicDoesNotMatch() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final InterfaceModel serviceInterfaceModel = new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
				.baseTopic("otherthing/management/")
				.operation("test-operation")
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("NotTheRequesterSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorizedServiceDefNotFoundOperationDoesNotMatch() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final InterfaceModel serviceInterfaceModel = new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
				.baseTopic("something/management/")
				.operation("other-operation")
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("NotTheRequesterSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorizedException() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final InterfaceModel serviceInterfaceModel = new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
				.baseTopic("something/management/")
				.operation("test-operation")
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("NotTheRequesterSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(sysInfo.getSystemName()).thenReturn("ServiceProvider");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(httpService.consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class))).thenThrow(new ArrowheadException("test exception"));

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(sysInfo).getSystemName();
		verify(serviceDefNameNormalizer).normalize("testService");
		verify(httpService).consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationNotAuthorized() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final InterfaceModel serviceInterfaceModel = new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
				.baseTopic("something/management/")
				.operation("test-operation")
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("NotTheRequesterSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(sysInfo.getSystemName()).thenReturn("ServiceProvider");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(httpService.consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class))).thenReturn(false);

		final Throwable ex = assertThrows(ForbiddenException.class,
				() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(sysInfo).getSystemName();
		verify(serviceDefNameNormalizer).normalize("testService");
		verify(httpService).consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));

		assertEquals("Requester has no management permission", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testDoFilterAuthorizationAuthorized() {
		final MqttRequestModel request = new MqttRequestModel("something/management/", "test-operation", new MqttRequestTemplate("trace", "authKey", "response", 0, null, "payload"));
		request.setRequester("RequesterSystem");
		request.setSysOp(false);

		final InterfaceModel serviceInterfaceModel = new MqttInterfaceModel.Builder("generic_mqtt", "localhost", 4763)
				.baseTopic("something/management/")
				.operation("test-operation")
				.build();

		final ServiceModel serviceModel = new ServiceModel.Builder()
				.serviceDefinition("testService")
				.version("1.0.0")
				.serviceInterface(serviceInterfaceModel)
				.build();

		when(sysInfo.getManagementPolicy()).thenReturn(ManagementPolicy.AUTHORIZATION);
		when(sysInfo.getManagementWhitelist()).thenReturn(List.of("NotTheRequesterSystem"));
		when(sysInfo.isSslEnabled()).thenReturn(false);
		when(sysInfo.getServices()).thenReturn(List.of(serviceModel));
		when(sysInfo.getSystemName()).thenReturn("ServiceProvider");
		when(serviceDefNameNormalizer.normalize("testService")).thenReturn("testService");
		when(httpService.consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class))).thenReturn(true);

		assertDoesNotThrow(() -> filter.doFilter("authKey", request));

		verify(sysInfo).getManagementPolicy();
		verify(sysInfo).getManagementWhitelist();
		verify(sysInfo).isSslEnabled();
		verify(sysInfo).getServices();
		verify(sysInfo).getSystemName();
		verify(serviceDefNameNormalizer).normalize("testService");
		verify(httpService).consumeService(eq("authorization"), eq("verify"), eq(Boolean.class), any(AuthorizationVerifyRequestDTO.class));
	}
}