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
package eu.arrowhead.serviceorchestration.service.thread;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponents;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.mqtt.MqttService;
import eu.arrowhead.dto.MqttNotifyTemplate;
import eu.arrowhead.dto.OrchestrationRequestDTO;
import eu.arrowhead.dto.OrchestrationResponseDTO;
import eu.arrowhead.dto.OrchestrationServiceRequirementDTO;
import eu.arrowhead.dto.enums.OrchestrationFlag;
import eu.arrowhead.serviceorchestration.DynamicServiceOrchestrationSystemInfo;
import eu.arrowhead.serviceorchestration.jpa.entity.OrchestrationJob;
import eu.arrowhead.serviceorchestration.jpa.entity.Subscription;
import eu.arrowhead.serviceorchestration.jpa.service.OrchestrationJobDbService;
import eu.arrowhead.serviceorchestration.jpa.service.SubscriptionDbService;
import eu.arrowhead.dto.enums.NotifyProtocol;
import eu.arrowhead.serviceorchestration.service.enums.OrchestrationJobStatus;
import eu.arrowhead.serviceorchestration.service.model.OrchestrationForm;
import eu.arrowhead.serviceorchestration.service.utils.InterCloudServiceOrchestration;
import eu.arrowhead.serviceorchestration.service.utils.LocalServiceOrchestration;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class PushOrchestrationWorkerTest {

	//=================================================================================================
	// members

	@Mock
	private OrchestrationJobDbService orchJobDbService;

	@Mock
	private SubscriptionDbService subscriptionDbService;

	@Spy
	private ObjectMapper mapper;

	@Mock
	private LocalServiceOrchestration localOrch;

	@Mock
	private InterCloudServiceOrchestration interCloudOrch;

	@Mock
	private HttpService httpService;

	@Mock
	private MqttService mqttService;

	@Mock
	private DynamicServiceOrchestrationSystemInfo sysInfo;

	private final UUID jobId = UUID.randomUUID();
	private final UUID subscriptionId = UUID.randomUUID();

	@InjectMocks
	private final PushOrchestrationWorker worker = new PushOrchestrationWorker(jobId);

	@Captor
	private ArgumentCaptor<String> stringCaptor;

	@Captor
	private ArgumentCaptor<OrchestrationForm> orchestrationFormCaptor;

	@Captor
	private ArgumentCaptor<OrchestrationResponseDTO> orchestrationResponseCator;

	@Captor
	private ArgumentCaptor<UriComponents> uriCaptor;

	@Captor
	private ArgumentCaptor<MqttMessage> mqttMessageCaptor;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunLocalCloudWithoutIssueHttpNotify() {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.HTTP.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("address", "127.0.0.1", "port", "8941", "method", "post", "path", "/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(localOrch.doLocalServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(localOrch).doLocalServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.class), eq(orchestrationResponseDTO));
		verify(mqttService, never()).client(anyString());
		verify(orchJobDbService, never()).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
		assertEquals("127.0.0.1", uriCaptor.getValue().getHost());
		assertEquals(8941, uriCaptor.getValue().getPort());
		assertEquals("/notify", uriCaptor.getValue().getPath());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunInterCloudWithoutIssueHttpsNotify() {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null),
				Map.of(OrchestrationFlag.ONLY_INTERCLOUD.name(), true), null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.HTTPS.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("address", "127.0.0.1", "port", "8941", "method", "post", "path", "/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(interCloudOrch.doInterCloudServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(interCloudOrch).doInterCloudServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.class), eq(orchestrationResponseDTO));
		verify(mqttService, never()).client(anyString());
		verify(orchJobDbService, never()).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
		assertEquals("127.0.0.1", uriCaptor.getValue().getHost());
		assertEquals(8941, uriCaptor.getValue().getPort());
		assertEquals("/notify", uriCaptor.getValue().getPath());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunLocalCloudWithoutIssueMqttNotify() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.MQTT.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("topic", "test/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(localOrch.doLocalServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		when(sysInfo.getSystemName()).thenReturn(Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION);
		final MqttClient mqttClient = Mockito.mock(MqttClient.class);
		when(mqttService.client(eq(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID))).thenReturn(mqttClient);

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(localOrch).doLocalServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(sysInfo).isMqttApiEnabled();
		verify(sysInfo).getSystemName();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService).client(eq(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID));
		verify(mqttClient).publish(stringCaptor.capture(), mqttMessageCaptor.capture());
		verify(orchJobDbService, never()).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
		assertEquals("test/notify", stringCaptor.getValue());
		final MqttNotifyTemplate capturedTemplate = mapper.readValue(mqttMessageCaptor.getValue().getPayload(), MqttNotifyTemplate.class);
		assertEquals(orchestrationResponseDTO.warnings().get(0), mapper.readValue(mapper.writeValueAsString(capturedTemplate.payload()), OrchestrationResponseDTO.class).warnings().get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunIntercloudWithoutIssueMqttsNotify() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null),
				Map.of(OrchestrationFlag.ONLY_INTERCLOUD.name(), true), null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.MQTTS.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("topic", "test/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(interCloudOrch.doInterCloudServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		when(sysInfo.getSystemName()).thenReturn(Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION);
		final MqttClient mqttClient = Mockito.mock(MqttClient.class);
		when(mqttService.client(eq(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID))).thenReturn(mqttClient);

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(interCloudOrch).doInterCloudServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(sysInfo).isMqttApiEnabled();
		verify(sysInfo).getSystemName();
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService).client(eq(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID));
		verify(mqttClient).publish(stringCaptor.capture(), mqttMessageCaptor.capture());
		verify(orchJobDbService, never()).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), anyString());

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
		assertEquals("test/notify", stringCaptor.getValue());
		final MqttNotifyTemplate capturedTemplate = mapper.readValue(mqttMessageCaptor.getValue().getPayload(), MqttNotifyTemplate.class);
		assertEquals(orchestrationResponseDTO.warnings().get(0), mapper.readValue(mapper.writeValueAsString(capturedTemplate.payload()), OrchestrationResponseDTO.class).warnings().get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunJobNotExists() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService, never()).setStatus(any(), any(), anyString());
		verify(orchJobDbService, never()).setStatus(any(), any(), any());
		verify(subscriptionDbService, never()).get(any(UUID.class));
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService, never()).client(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunNoSubscription() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), eq("Orchestration job " + jobId + " has no subscription id"));
		verify(subscriptionDbService, never()).get(any(UUID.class));
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService, never()).client(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunSubscriptionNotFound() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.empty());

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), eq("Orchestration job " + jobId + " has no subscription with " + job.getSubscriptionId() + " subscription id"));
		verify(localOrch, never()).doLocalServiceOrchestration(any(), any());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService, never()).client(anyString());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testRunHTTPError() {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.HTTP.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("address", "127.0.0.1", "port", "8941", "method", "post", "path", "/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(localOrch.doLocalServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);
		doThrow(new ArrowheadException("test message")).when(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.class), eq(orchestrationResponseDTO));

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(localOrch).doLocalServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService).sendRequest(uriCaptor.capture(), eq(HttpMethod.POST), eq(Void.class), eq(orchestrationResponseDTO));
		verify(mqttService, never()).client(anyString());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), eq("Error occurred while sending push orchestration via HTTP to subscription: " + subscriptionId.toString() + ". Reason: test message"));

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
		assertEquals("127.0.0.1", uriCaptor.getValue().getHost());
		assertEquals(8941, uriCaptor.getValue().getPort());
		assertEquals("/notify", uriCaptor.getValue().getPath());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunMQTTClientError() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.MQTT.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("topic", "test/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(localOrch.doLocalServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		final MqttClient mqttClient = Mockito.mock(MqttClient.class);
		doThrow(new ArrowheadException("test message")).when(mqttService).client(eq(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID));

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(localOrch).doLocalServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(sysInfo).isMqttApiEnabled();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService).client(eq(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID));
		verify(sysInfo, never()).getSystemName();
		verify(mqttClient, never()).publish(anyString(), any());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), eq("test message"));

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunMQTTPublishError() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.MQTT.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("topic", "test/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(localOrch.doLocalServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);
		when(sysInfo.isMqttApiEnabled()).thenReturn(true);
		when(sysInfo.getSystemName()).thenReturn(Constants.SYS_NAME_DYNAMIC_SERVICE_ORCHESTRATION);
		final MqttClient mqttClient = Mockito.mock(MqttClient.class);
		when(mqttService.client(eq(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID))).thenReturn(mqttClient);
		doThrow(new MqttException(0x00)).when(mqttClient).publish(eq("test/notify"), any());

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(localOrch).doLocalServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(sysInfo).isMqttApiEnabled();
		verify(sysInfo).getSystemName();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService).client(eq(Constants.MQTT_SERVICE_PROVIDING_BROKER_CONNECT_ID));
		verify(mqttClient).publish(stringCaptor.capture(), mqttMessageCaptor.capture());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), eq("Error occurred while sending push orchestration via MQTT to subscription: " + subscriptionId.toString() + ". Reason: MqttException"));

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
		assertEquals("test/notify", stringCaptor.getValue());
		final MqttNotifyTemplate capturedTemplate = mapper.readValue(mqttMessageCaptor.getValue().getPayload(), MqttNotifyTemplate.class);
		assertEquals(orchestrationResponseDTO.warnings().get(0), mapper.readValue(mapper.writeValueAsString(capturedTemplate.payload()), OrchestrationResponseDTO.class).warnings().get(0));
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunMQTTNotEnabled() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol(NotifyProtocol.MQTT.name());
		subscription.setNotifyProperties(Utilities.toJson(Map.of("topic", "test/notify")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(localOrch.doLocalServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);
		when(sysInfo.isMqttApiEnabled()).thenReturn(false);

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(localOrch).doLocalServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(sysInfo).isMqttApiEnabled();
		verify(sysInfo, never()).getSystemName();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService, never()).client(anyString());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), eq("Orchestration push notification via MQTT is required for subscripiton: " + subscriptionId.toString() + ", but MQTT is not enabled"));

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());

	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunUnsupportedProtocol() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol("COAP");
		subscription.setNotifyProperties(Utilities.toJson(Map.of("foo", "bar")));

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(localOrch.doLocalServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(localOrch).doLocalServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(sysInfo, never()).isMqttApiEnabled();
		verify(sysInfo, never()).getSystemName();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService, never()).client(anyString());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), eq("Unsupported protocol: COAP"));

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());

	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Test
	public void testRunNotifyPropsException() throws MqttPersistenceException, MqttException, StreamReadException, DatabindException, IOException {
		final OrchestrationJob job = new OrchestrationJob();
		job.setId(jobId);
		job.setSubscriptionId(subscriptionId.toString());

		final OrchestrationRequestDTO orchestrationRequestDTO = new OrchestrationRequestDTO(new OrchestrationServiceRequirementDTO("testService", null, null, null, null, null, null, null, null, null), null, null, null);

		final Subscription subscription = new Subscription();
		subscription.setId(subscriptionId);
		subscription.setOrchestrationRequest(Utilities.toJson(orchestrationRequestDTO));
		subscription.setNotifyProtocol("HTTP");
		subscription.setNotifyProperties("invalid props");

		final OrchestrationResponseDTO orchestrationResponseDTO = new OrchestrationResponseDTO(List.of(), List.of("test warning"));

		when(orchJobDbService.getById(eq(jobId))).thenReturn(Optional.of(job));
		when(orchJobDbService.setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull())).thenAnswer(invocation -> {
			job.setStatus(OrchestrationJobStatus.IN_PROGRESS);
			return job;
		});
		when(subscriptionDbService.get(eq(subscriptionId))).thenReturn(Optional.of(subscription));
		when(localOrch.doLocalServiceOrchestration(eq(jobId), any(OrchestrationForm.class))).thenReturn(orchestrationResponseDTO);

		assertDoesNotThrow(() -> worker.run());

		verify(orchJobDbService).getById(eq(jobId));
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.IN_PROGRESS), isNull());
		verify(subscriptionDbService).get(eq(subscriptionId));
		verify(localOrch).doLocalServiceOrchestration(eq(jobId), orchestrationFormCaptor.capture());
		verify(sysInfo, never()).isMqttApiEnabled();
		verify(sysInfo, never()).getSystemName();
		verify(interCloudOrch, never()).doInterCloudServiceOrchestration(any(), any());
		verify(httpService, never()).sendRequest(any(), any(), any(Class.class), any());
		verify(mqttService, never()).client(anyString());
		verify(orchJobDbService).setStatus(eq(jobId), eq(OrchestrationJobStatus.ERROR), stringCaptor.capture());

		assertEquals(orchestrationRequestDTO.serviceRequirement().serviceDefinition(), orchestrationFormCaptor.getValue().getServiceDefinition());
		assertTrue(stringCaptor.getValue().startsWith("Unreadable notify properties:"));
	}
}
