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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponents;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.http.HttpService;
import eu.arrowhead.common.http.HttpUtilities;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorConstants;
import eu.arrowhead.deviceqosevaluator.DeviceQoSEvaluatorSystemInfo;
import eu.arrowhead.deviceqosevaluator.dto.AugmentedMeasurementsDTO;
import jakarta.annotation.PostConstruct;

// Reads the load figures of a device from its device agent (device_agent/python). Since 2026-11-09 over mutual TLS:
// the system's own certificate and truststore (HttpService), hostname verification of the agent certificate against the
// device address, and no identity token (the agent authorizes the client certificate). Plain HTTP only with
// device.agent.ssl.enabled=false, which the secure profile refuses.
@Service
public class AugmentedMeasurementDriver {

	//=================================================================================================
	// members

	@Autowired
	private DeviceQoSEvaluatorSystemInfo sysInfo;

	@Autowired
	private HttpService httpService;

	@Value(DeviceQoSEvaluatorConstants.$DEVICE_AGENT_SSL_ENABLED_WD)
	private boolean agentSslEnabled;

	@Value(DeviceQoSEvaluatorConstants.$DEVICE_AGENT_PORT_WD)
	private int agentPort;

	private static final String path = "/device-qos";
	private static final String paramKey = "batch";
	private static final int deviceClientWindowSize = 30; // sec

	private String batchSize;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public AugmentedMeasurementsDTO fetch(final String address) {
		final UriComponents uri = HttpUtilities.createURI(agentSslEnabled ? Constants.HTTPS : Constants.HTTP, address, agentPort, path, paramKey, batchSize);
		return httpService.sendRequest(uri, HttpMethod.GET, AugmentedMeasurementsDTO.class, (Void) null, null, null);
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@PostConstruct
	private void init() {
		if (agentPort < 1 || agentPort > 65535) {
			throw new IllegalArgumentException(DeviceQoSEvaluatorConstants.DEVICE_AGENT_PORT + " must be 1..65535, got " + agentPort);
		}

		if (agentSslEnabled && !sysInfo.isSslEnabled()) {
			throw new IllegalStateException(DeviceQoSEvaluatorConstants.DEVICE_AGENT_SSL_ENABLED + "=true needs server.ssl.enabled=true (the client certificate for the device agents)");
		}

		final long frequency = sysInfo.getAugmentedMeasurementJobInterval();
		this.batchSize = frequency > deviceClientWindowSize ? String.valueOf(deviceClientWindowSize) : String.valueOf(frequency);
	}
}
