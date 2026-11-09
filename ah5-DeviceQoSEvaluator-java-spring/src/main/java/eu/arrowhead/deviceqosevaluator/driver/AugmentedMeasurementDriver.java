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
package eu.arrowhead.deviceqosevaluator.driver;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.dto.AugmentedMeasurementsDTO;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.PostConstruct;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;

@Service
public class AugmentedMeasurementDriver {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	private static final String path = "/device-qos";
	private static final String paramKey = "batch";
	private static final int deviceClientWindowSize = 30; // sec

	private static final int connectionTimeout = 5000; // ms
	private static final int socketTimeout = 5000; // ms

	private String batchSize;
	private WebClient client;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AugmentedMeasurementsDTO fetch(final String address) {
		final UriComponents uri = HttpUtilities.createURI(Constants.HTTP, address, DeviceQoSEvaluatorConstants.AUGMENTED_MEASUREMENT_PORT, path, paramKey, batchSize);
		final RequestBodySpec spec = client
				.method(HttpMethod.GET)
				.uri(uri.toUri());
		return spec
				.retrieve()
				.bodyToMono(AugmentedMeasurementsDTO.class)
				.block(Duration.ofMillis(socketTimeout));
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		final HttpClient httpCLient = HttpClient
				.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
				.doOnConnected(this::initConnectionHandlers);

		this.client = createWebClient(httpCLient);
		final long frequency = sysInfo.getAugmentedMeasurementJobInterval();
		this.batchSize = frequency > deviceClientWindowSize ? String.valueOf(deviceClientWindowSize) : String.valueOf(frequency);
	}

	//-------------------------------------------------------------------------------------------------
	private void initConnectionHandlers(final Connection connection) {
		connection.addHandlerLast(new ReadTimeoutHandler(socketTimeout, TimeUnit.MILLISECONDS));
		connection.addHandlerLast(new WriteTimeoutHandler(socketTimeout, TimeUnit.MILLISECONDS));
	}

	//-------------------------------------------------------------------------------------------------
	private WebClient createWebClient(final HttpClient client) {
		final Builder builder = WebClient
				.builder()
				.clientConnector(new ReactorClientHttpConnector(client))
				.defaultHeader(HttpHeaderNames.ACCEPT.toString(), MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader(HttpHeaderNames.CONNECTION.toString(), "keep-alive");

		return builder.build();
	}
}