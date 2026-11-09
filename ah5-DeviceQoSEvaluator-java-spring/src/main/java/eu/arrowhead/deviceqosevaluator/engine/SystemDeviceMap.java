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
package eu.arrowhead.deviceqosevaluator.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.arrowhead.common.Constants;
import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.meta.MetadataKeyEvaluator;
import eu.arrowhead.deviceqosevaluator.enums.OidGroup;
import eu.arrowhead.dto.AddressDTO;
import eu.arrowhead.dto.SystemResponseDTO;
import eu.arrowhead.dto.enums.AddressType;

public class SystemDeviceMap {

	//=================================================================================================
	// members

	private int nextDeviceIdx = 0;
	private final HashMap<Integer, Triple<Set<Address>, Set<String>, Bool>> devices = new HashMap<>();

	private final Logger logger = LogManager.getLogger(this.getClass());

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public int getDeviceSize() {
		return devices.size();
	}

	//-------------------------------------------------------------------------------------------------
	public Set<Address> getDeviceAddresses(final int idx) {
		return devices.get(idx).getLeft();
	}

	//-------------------------------------------------------------------------------------------------
	public Set<String> getDeviceSystems(final int idx) {
		return devices.get(idx).getMiddle();
	}

	//-------------------------------------------------------------------------------------------------
	public boolean hasAugmented(final int idx) {
		return devices.get(idx).getRight().getValue();
	}

	//-------------------------------------------------------------------------------------------------
	public void load(final List<SystemResponseDTO> systems) {
		logger.debug("load started");

		for (final SystemResponseDTO system : systems) {
			final Set<Address> addresses = collectAddresses(system);
			Integer device = findDevice(addresses);

			if (device != null) {
				devices.get(device).getLeft().retainAll(addresses);
			} else {
				device = nextDeviceIdx;
				devices.put(device, Triple.of(addresses, new HashSet<>(), new Bool()));
				nextDeviceIdx++;
			}

			devices.get(device).getMiddle().add(system.name());
			if (!devices.get(device).getRight().getValue() && supportsAugmented(system)) {
				devices.get(device).getRight().setValue(true);
			}
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Set<Address> collectAddresses(final SystemResponseDTO system) {
		logger.debug("collectAddresses started");

		final Set<Address> addresses = new HashSet<>(system.addresses().size() + (system.device() == null ? 0 : system.device().addresses().size()));
		if (system.device() != null) {
			for (final AddressDTO addr : system.device().addresses()) {
				if (Utilities.isEnumValue(addr.type(), AddressType.class)
						&& !addr.type().equalsIgnoreCase(AddressType.MAC.name())) {
					addresses.add(new Address(addr.address(), AddressType.valueOf(addr.type()), true));
				}
			}
		}

		for (final AddressDTO addr : system.addresses()) {
			if (Utilities.isEnumValue(addr.type(), AddressType.class)) {
				addresses.add(new Address(addr.address(), AddressType.valueOf(addr.type()), false));
			}
		}

		return addresses;
	}

	//-------------------------------------------------------------------------------------------------
	private Integer findDevice(final Set<Address> addresses) {
		logger.debug("findDevice started");

		for (final Entry<Integer, Triple<Set<Address>, Set<String>, Bool>> entry : devices.entrySet()) {
			for (final Address deviceAddr : entry.getValue().getLeft()) {
				final boolean anyMatch = addresses.stream().anyMatch(addr -> addr.address().equals(deviceAddr.address()));
				if (anyMatch) {
					return entry.getKey();
				}
			}
		}

		return null;
	}

	//-------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private boolean supportsAugmented(final SystemResponseDTO system) {
		logger.debug("supportsAugmented started");

		if (Utilities.isEmpty(system.metadata()) || !system.metadata().containsKey(Constants.PROPERTY_KEY_QOS)) {
			return false;
		}

		final Object object = MetadataKeyEvaluator.getMetadataValueForCompositeKey(system.metadata(), Constants.PROPERTY_KEY_QOS + Constants.DOT + Constants.PROPERTY_KEY_DEVICE_AUGMENTED);
		if (object != null) {
			try {
				final List<String> list = (List<String>) object;
				for (final String item : list) {
					for (final OidGroup oidGroup : OidGroup.values()) {
						if (OidGroup.RTT == oidGroup) {
							continue;
						}

						if (item.equals(oidGroup.getValue())) {
							return true;
						}
					}
				}
			} catch (final ClassCastException ex) {
				return false;
			}
		}

		return false;
	}

	//=================================================================================================
	// nested record

	//-------------------------------------------------------------------------------------------------
	protected record Address(String address, AddressType type, boolean deviceRelated) {

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		@Override
		public int hashCode() {
			return Objects.hash(address, type);
		}

		//-------------------------------------------------------------------------------------------------
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj == null) {
				return false;
			}

			if (getClass() != obj.getClass()) {
				return false;
			}

			final Address other = (Address) obj;

			return Objects.equals(address, other.address) && type == other.type;
		}
	}

	//=================================================================================================
	// nested class

	//-------------------------------------------------------------------------------------------------
	static final class Bool {

		//=================================================================================================
		// members

		private boolean value = false;

		//=================================================================================================
		// methods

		//-------------------------------------------------------------------------------------------------
		public boolean getValue() {
			return value;
		}

		//-------------------------------------------------------------------------------------------------
		public void setValue(final boolean value) {
			this.value = value;
		}
	}
}