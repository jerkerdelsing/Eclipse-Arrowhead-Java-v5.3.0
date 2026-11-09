package eu.arrowhead.serviceregistry.service.matching;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import eu.arrowhead.dto.AddressDTO;

public class AddressMatchingTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAddressListMatchingSameLists() {

		AddressMatching matcher = new AddressMatching();

		assertAll(
			// same order
			() -> assertTrue(matcher.isAddressListMatching(List.of(new AddressDTO("IPV4", "192.168.2.2")), List.of(new AddressDTO("IPV4", "192.168.2.2")))),

			// different order
			() -> assertTrue(matcher.isAddressListMatching(
					List.of(new AddressDTO("IPV4", "192.168.2.2"), new AddressDTO("MAC", "00:1a:2b:3c:4d:5e")),
					List.of(new AddressDTO("MAC", "00:1a:2b:3c:4d:5e"), new AddressDTO("IPV4", "192.168.2.2")))),

			// empty lists
			() -> assertTrue(matcher.isAddressListMatching(List.of(), List.of())),
			// lists with only null elements
			() -> {
				final List<AddressDTO> listOfNull1 = new ArrayList<>(1);
				listOfNull1.add(null);

				final List<AddressDTO> listOfNull2 = new ArrayList<>(1);
				listOfNull2.add(null);

				assertTrue(matcher.isAddressListMatching(listOfNull1, listOfNull2));
			},
			// lists with null and other elements
			() -> {

				final List<AddressDTO> listOfNull1 = new ArrayList<>(2);
				listOfNull1.add(null);
				listOfNull1.add(new AddressDTO("IPV4", "192.168.2.2"));

				final List<AddressDTO> listOfNull2 = new ArrayList<>(2);
				listOfNull2.add(new AddressDTO("IPV4", "192.168.2.2"));
				listOfNull2.add(null);

				assertTrue(matcher.isAddressListMatching(listOfNull1, listOfNull2));
			}
		);
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testIsAddressListMatchingDifferent() {
		AddressMatching matcher = new AddressMatching();

		assertAll(
			// different elements
			() -> assertFalse(matcher.isAddressListMatching(List.of(new AddressDTO("IPV4", "192.168.2.2")), List.of(new AddressDTO("IPV4", "192.168.2.3")))),
			// different and matching elements
			() -> assertFalse(matcher.isAddressListMatching(
					List.of(new AddressDTO("IPV4", "192.168.2.2"), new AddressDTO("MAC", "00:1a:2b:3c:4d:5e")),
					List.of(new AddressDTO("IPV4", "192.168.2.2"), new AddressDTO("MAC", "00:1a:2b:3c:4d:5b")))),
			// only matching elements but not all of them
			() -> assertFalse(matcher.isAddressListMatching(
					List.of(new AddressDTO("IPV4", "192.168.2.2"), new AddressDTO("MAC", "00:1a:2b:3c:4d:5e")),
					List.of(new AddressDTO("IPV4", "192.168.2.2")))),
			// empty list and list with null element
			() -> {
				final List<AddressDTO> listOfNull = new ArrayList<>(1);
				listOfNull.add(null);

				assertFalse(matcher.isAddressListMatching(listOfNull, List.of()));
			}
		);
	}
}
