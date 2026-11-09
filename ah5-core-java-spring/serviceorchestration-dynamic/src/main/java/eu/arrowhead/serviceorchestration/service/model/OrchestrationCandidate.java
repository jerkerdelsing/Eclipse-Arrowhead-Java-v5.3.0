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
package eu.arrowhead.serviceorchestration.service.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.arrowhead.dto.AuthorizationTokenGenerationResponseDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceResponseDTO;
import eu.arrowhead.dto.ServiceInstanceResponseDTO;

public class OrchestrationCandidate {

	//=================================================================================================
	// members

	private final ServiceInstanceResponseDTO serviceInstance;
	private final boolean isLocal;

	// private Cloud cloud; not supported yet
	private boolean isLocked;
	private boolean canBeExclusive;
	private int exclusivityDuration = 0; // 0 or negative means can't be exclusive
	private ZonedDateTime exclusiveUntil;
	private boolean nonNative; // Interface translation is necessary if allowed
	private boolean preferred;
	private boolean properQoS;
	private Map<String, Map<String, AuthorizationTokenGenerationResponseDTO>> authorizationTokens = new HashMap<>();
	private final List<ServiceInstanceInterfaceResponseDTO> matchingInterfaces = new ArrayList<>();
	private final List<ServiceInstanceInterfaceResponseDTO> translatableInterfaces = new ArrayList<>();

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public OrchestrationCandidate(final ServiceInstanceResponseDTO serviceInstance, final boolean isLocal, final boolean isPreferred) {
		this.serviceInstance = serviceInstance;
		this.isLocal = isLocal;
		this.preferred = isPreferred;
	}

	//-------------------------------------------------------------------------------------------------
	public ServiceInstanceResponseDTO getServiceInstance() {
		return serviceInstance;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isLocal() {
		return isLocal;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isLocked() {
		return isLocked;
	}

	//-------------------------------------------------------------------------------------------------
	public void setLocked(final boolean locked) {
		isLocked = locked;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean canBeExclusive() {
		return canBeExclusive;
	}

	//-------------------------------------------------------------------------------------------------
	public void setCanBeExclusive(final boolean canBeExclusive) {
		this.canBeExclusive = canBeExclusive;
	}

	//-------------------------------------------------------------------------------------------------
	public int getExclusivityDuration() {
		return exclusivityDuration;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExclusivityDuration(final int exclusivityDuration) {
		this.exclusivityDuration = exclusivityDuration;
	}

	//-------------------------------------------------------------------------------------------------
	public ZonedDateTime getExclusiveUntil() {
		return exclusiveUntil;
	}

	//-------------------------------------------------------------------------------------------------
	public void setExclusiveUntil(final ZonedDateTime exclusiveUntil) {
		this.exclusiveUntil = exclusiveUntil;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isNonNative() {
		return nonNative;
	}

	//-------------------------------------------------------------------------------------------------
	public void setNonNative(final boolean nonNative) {
		this.nonNative = nonNative;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean isPreferred() {
		return preferred;
	}

	//-------------------------------------------------------------------------------------------------
	public void setPreferred(final boolean preferred) {
		this.preferred = preferred;
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasProperQoS() {
		return properQoS;
	}

	//-------------------------------------------------------------------------------------------------
	public void setProperQoS(final boolean properQoS) {
		this.properQoS = properQoS;
	}

	//-------------------------------------------------------------------------------------------------
	public Map<String, Map<String, AuthorizationTokenGenerationResponseDTO>> getAuthorizationTokens() {
		return authorizationTokens;
	}

	//-------------------------------------------------------------------------------------------------
	public void addAuthorizationToken(final String tokenType, final String scope, final AuthorizationTokenGenerationResponseDTO tokenDTO) {
		authorizationTokens.putIfAbsent(tokenType, new HashMap<>());
		authorizationTokens.get(tokenType).put(scope, tokenDTO);
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceInterfaceResponseDTO> getMatchingInterfaces() {
		return matchingInterfaces;
	}

	//-------------------------------------------------------------------------------------------------
	public void addMatchingInterface(final ServiceInstanceInterfaceResponseDTO matchingInterface) {
		this.matchingInterfaces.add(matchingInterface);
	}

	//-------------------------------------------------------------------------------------------------
	public void addMatchingInterfaces(final List<ServiceInstanceInterfaceResponseDTO> matchingInterfaces) {
		this.matchingInterfaces.addAll(matchingInterfaces);
	}

	//-------------------------------------------------------------------------------------------------
	public List<ServiceInstanceInterfaceResponseDTO> getTranslatableInterfaces() {
		return translatableInterfaces;
	}

	//-------------------------------------------------------------------------------------------------
	public void addTranslatableInterface(final ServiceInstanceInterfaceResponseDTO translatableInterface) {
		this.translatableInterfaces.add(translatableInterface);
	}

	//-------------------------------------------------------------------------------------------------
	public void addTranslatableInterfaces(final List<ServiceInstanceInterfaceResponseDTO> translatableInterfaces) {
		this.translatableInterfaces.addAll(translatableInterfaces);
	}
}