package eu.arrowhead.serviceregistry.service.model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.enums.AddressType;
import eu.arrowhead.dto.enums.ServiceInterfacePolicy;

@SuppressWarnings("checkstyle:magicnumber")
public class ServiceLookupFilterModelTest {

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConstructorDtoNull() {
		final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ServiceLookupFilterModel(null));
		assertEquals("ServiceInstanceLookupRequestDTO is null", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConstructorNothingIsEmpty() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
			List.of("TemperatureProvider|temperatureInfo|1.0.0"),
			List.of("TemperatureProvider"),
			List.of("temperatureInfo"),
			List.of("1.0.0"),
			"2025-11-04T01:53:02Z",
			List.of(metadataReq),
			List.of("IPV4"),
			List.of("generic_http"),
			List.of(intfReq),
			List.of("NONE"));

		final ServiceLookupFilterModel model = new ServiceLookupFilterModel(dto);

		assertAll(
			() -> assertEquals(Set.of("TemperatureProvider|temperatureInfo|1.0.0"), model.getInstanceIds()),
			() -> assertEquals(Set.of("TemperatureProvider"), model.getProviderNames()),
			() -> assertEquals(Set.of("temperatureInfo"), model.getServiceDefinitionNames()),
			() -> assertEquals(Set.of("1.0.0"), model.getVersions()),
			() -> assertEquals(ZonedDateTime.of(2025, 11, 4, 1, 53, 2, 0, ZoneId.of("UTC")), model.getAlivesAt()),
			() -> assertEquals(List.of(metadataReq), model.getMetadataRequirementsList()),
			() -> assertEquals(List.of(AddressType.IPV4), model.getAddressTypes()),
			() -> assertEquals(Set.of("generic_http"), model.getInterfaceTemplateNames()),
			() -> assertEquals(List.of(intfReq), model.getInterfacePropertyRequirementsList()),
			() -> assertEquals(Set.of(ServiceInterfacePolicy.NONE), model.getPolicies())
		);

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testConstructorEverythingIsEmpty() {
		try (MockedStatic<Utilities> utilitiesMock = Mockito.mockStatic(Utilities.class)) {

			utilitiesMock.when(() -> Utilities.isEmpty((List<?>) null)).thenReturn(true);

			final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, null, null);
			final ServiceLookupFilterModel model = new ServiceLookupFilterModel(dto);
			assertAll(
					() -> assertEquals(Set.of(), model.getInstanceIds()),
					() -> assertEquals(Set.of(), model.getProviderNames()),
					() -> assertEquals(Set.of(), model.getServiceDefinitionNames()),
					() -> assertEquals(Set.of(), model.getVersions()),
					() -> assertEquals(null, model.getAlivesAt()),
					() -> assertEquals(List.of(), model.getMetadataRequirementsList()),
					() -> assertEquals(List.of(), model.getAddressTypes()),
					() -> assertEquals(Set.of(), model.getInterfaceTemplateNames()),
					() -> assertEquals(List.of(), model.getInterfacePropertyRequirementsList()),
					() -> assertEquals(Set.of(), model.getPolicies())
				);
			utilitiesMock.verify(() -> Utilities.isEmpty((List<?>) any()), times(9));
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testHasFilters() {

		final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
		metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

		final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
		intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

		assertAll(
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(List.of("TemperatureProvider|temperatureInfo|1.0.0"), null, null, null, null, null, null, null, null, null)).hasFilters()),
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, List.of("TemperatureProvider"), null, null, null, null, null, null, null, null)).hasFilters()),
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, List.of("temperatureInfo"), null, null, null, null, null, null, null)).hasFilters()),
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, List.of("1.0.0"), null, null, null, null, null, null)).hasFilters()),
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, "2025-11-04T01:53:02Z", null, null, null, null, null)).hasFilters()),
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, List.of(metadataReq), null, null, null, null)).hasFilters()),
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, List.of("IPV4"), null, null, null)).hasFilters()),
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, List.of("generic_http"), null, null)).hasFilters()),
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, List.of(intfReq), null)).hasFilters()),
			() -> assertTrue(new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, null, List.of("NONE"))).hasFilters()),
			() -> assertTrue(!new ServiceLookupFilterModel(new ServiceInstanceLookupRequestDTO(null, null, null, null, null, null, null, null, null, null)).hasFilters())
		);
	}

}
