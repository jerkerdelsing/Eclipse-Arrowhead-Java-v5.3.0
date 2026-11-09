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
package eu.arrowhead.serviceregistry.jpa.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import eu.arrowhead.common.jpa.RefreshableRepository;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.serviceregistry.jpa.entity.Device;
import eu.arrowhead.serviceregistry.jpa.entity.DeviceAddress;

@Repository
public interface DeviceAddressRepository extends RefreshableRepository<DeviceAddress, Long> {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public List<DeviceAddress> findAllByDevice(final Device device);

	//-------------------------------------------------------------------------------------------------
	public List<DeviceAddress> findAllByDeviceAndAddressType(final Device device, final AddressType type);

	//-------------------------------------------------------------------------------------------------
	public List<DeviceAddress> findAllByDeviceAndAddressTypeIn(final Device device, final List<AddressType> types);

	//-------------------------------------------------------------------------------------------------
	public List<DeviceAddress> findAllByDeviceAndAddressIn(final Device device, final List<String> addresses);

	//-------------------------------------------------------------------------------------------------
	public List<DeviceAddress> deleteAllByDeviceIn(final List<Device> devices);

	//-------------------------------------------------------------------------------------------------
	public List<DeviceAddress> findAllByDeviceIn(final List<Device> devices);
}