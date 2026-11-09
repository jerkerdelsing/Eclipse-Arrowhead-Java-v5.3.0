package eu.arrowhead.serviceregistry.service.normalization;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.service.validation.name.InterfaceTemplateNameNormalizer;
import eu.arrowhead.common.service.validation.name.ServiceDefinitionNameNormalizer;
import eu.arrowhead.common.service.validation.name.SystemNameNormalizer;
import eu.arrowhead.common.service.validation.serviceinstance.ServiceInstanceIdentifierNormalizer;
import eu.arrowhead.common.service.validation.version.VersionNormalizer;
import eu.arrowhead.dto.MetadataRequirementDTO;
import eu.arrowhead.dto.ServiceInstanceInterfaceRequestDTO;
import eu.arrowhead.dto.ServiceInstanceLookupRequestDTO;
import eu.arrowhead.dto.ServiceInstanceRequestDTO;
import eu.arrowhead.serviceregistry.service.validation.interf.InterfaceNormalizer;

@SuppressWarnings("checkstyle:magicnumber")
@ExtendWith(MockitoExtension.class)
public class ServiceDiscoveryNormalizationTest {

	//=================================================================================================
	// members

	@InjectMocks
	private ServiceDiscoveryNormalization normalizer;

	@Mock
	private SystemNameNormalizer systemNameNormalizer;

	@Mock
	private ServiceDefinitionNameNormalizer serviceDefNameNormalizer;

	@Mock
	private InterfaceTemplateNameNormalizer interfaceTemplateNameNormalizer;

	@Mock
	private VersionNormalizer versionNormalizer;

	@Mock
	private InterfaceNormalizer interfaceNormalizer;

	@Mock
	private ServiceInstanceIdentifierNormalizer serviceInstanceIdentifierNormalizer;

	private static MockedStatic<Utilities> utilitiesMock;

	private static final String EMPTY = "\n ";

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeServiceInstanceRequestDTO() {

		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(serviceDefNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(interfaceNormalizer.normalizeInterfaceDTO(any())).thenAnswer(invocation -> invocation.getArgument(0));

		final ServiceInstanceInterfaceRequestDTO intf = new ServiceInstanceInterfaceRequestDTO("generic_http", "http", "NONE", Map.of("accessPort", 8080));

		assertAll(

				// nothing is empty
				() -> {
					final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("AlertProvider", "alertService", "16.4.3", "2025-11-04T01:53:02Z\n\n", Map.of("indoor", true), List.of(intf));
					final ServiceInstanceRequestDTO expected = new ServiceInstanceRequestDTO("AlertProvider", "alertService", "16.4.3", "2025-11-04T01:53:02Z", Map.of("indoor", true), List.of(intf));

					final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeServiceInstanceRequestDTO(dto));
					assertEquals(expected, normalized);
					verify(systemNameNormalizer, times(1)).normalize("AlertProvider");
					verify(serviceDefNameNormalizer, times(1)).normalize("alertService");
					verify(versionNormalizer, times(1)).normalize("16.4.3");
					verify(interfaceNormalizer, times(1)).normalizeInterfaceDTO(intf);

				},

				// expiration is empty
				() -> {
					resetUtilitiesMock();
					final ServiceInstanceRequestDTO dto = new ServiceInstanceRequestDTO("AlertProvider", "alertService", "16.4.3", EMPTY, Map.of("indoor", true), List.of(intf));
					final ServiceInstanceRequestDTO expected = new ServiceInstanceRequestDTO("AlertProvider", "alertService", "16.4.3", "", Map.of("indoor", true), List.of(intf));

					final ServiceInstanceRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeServiceInstanceRequestDTO(dto));
					assertEquals(expected, normalized);
					utilitiesMock.verify(() -> Utilities.isEmpty(EMPTY));
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeServiceInstanceLookupRequestDTO() {

		when(serviceInstanceIdentifierNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(serviceDefNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(versionNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
		when(interfaceTemplateNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		assertAll(

				// nothing is null
				() -> {
					final MetadataRequirementDTO metadataReq = new MetadataRequirementDTO();
					metadataReq.put("priority", Map.of("op", "LESS_THAN", "value", 10));

					final MetadataRequirementDTO intfReq = new MetadataRequirementDTO();
					intfReq.put("port", Map.of("op", "NOT_EQUALS", "value", 1444));

					final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(
							List.of("AlertProvider|alertService|16.4.3"),
							List.of("AlertProvider"),
							List.of("alertService"),
							List.of("16.4.3"),
							"\t  2025-11-04T01:53:02Z \n",
							List.of(metadataReq),
							List.of("IPv4  "),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("none \t"));

					final ServiceInstanceLookupRequestDTO expected = new ServiceInstanceLookupRequestDTO(
							List.of("AlertProvider|alertService|16.4.3"),
							List.of("AlertProvider"),
							List.of("alertService"),
							List.of("16.4.3"),
							"2025-11-04T01:53:02Z",
							List.of(metadataReq),
							List.of("IPV4"),
							List.of("generic_http"),
							List.of(intfReq),
							List.of("NONE"));

					final ServiceInstanceLookupRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeServiceInstanceLookupRequestDTO(dto));
					assertEquals(expected, normalized);
					verify(serviceInstanceIdentifierNormalizer, times(1)).normalize("AlertProvider|alertService|16.4.3");
					verify(systemNameNormalizer, times(1)).normalize("AlertProvider");
					verify(serviceDefNameNormalizer, times(1)).normalize("alertService");
					verify(versionNormalizer, times(1)).normalize("16.4.3");
					verify(interfaceTemplateNameNormalizer, times(1)).normalize("generic_http");
				},

				// everything is null
				() -> {
					resetUtilitiesMock();

					final ServiceInstanceLookupRequestDTO dto = new ServiceInstanceLookupRequestDTO(List.of(), List.of(), List.of(), List.of(), EMPTY, List.of(), List.of(), List.of(), List.of(), List.of());
					final ServiceInstanceLookupRequestDTO expected = new ServiceInstanceLookupRequestDTO(
							new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "", new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

					final ServiceInstanceLookupRequestDTO normalized = assertDoesNotThrow(() -> normalizer.normalizeServiceInstanceLookupRequestDTO(dto));
					assertEquals(expected, normalized);
					utilitiesMock.verify(() -> Utilities.isEmpty(eq(EMPTY)), times(1));
					utilitiesMock.verify(() -> Utilities.isEmpty(eq(List.of())), times(9));
				});
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testNormalizeSystemNameNullName() {
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeSystemName(null));

		assertEquals("System name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testNormalizeSystemNameEmptyName() {
		utilitiesMock.when(() -> Utilities.isEmpty("")).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeSystemName(""));

		assertEquals("System name is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeSystemName() {
		when(systemNameNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final String normalized = assertDoesNotThrow(() -> normalizer.normalizeSystemName("AlertProvider"));
		assertEquals("AlertProvider", normalized);
		verify(systemNameNormalizer, times(1)).normalize("AlertProvider");

	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testNormalizeServiceInstanceIdNullId() {
		utilitiesMock.when(() -> Utilities.isEmpty((String) null)).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeServiceInstanceId(null));

		assertEquals("Service instance id is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	void testNormalizeServiceInstanceIdEmptyId() {
		utilitiesMock.when(() -> Utilities.isEmpty("")).thenReturn(true);

		final Throwable ex = assertThrows(
				IllegalArgumentException.class,
				() -> normalizer.normalizeServiceInstanceId(""));

		assertEquals("Service instance id is empty", ex.getMessage());
	}

	//-------------------------------------------------------------------------------------------------
	@Test
	public void testNormalizeServiceInstanceId() {

		when(serviceInstanceIdentifierNormalizer.normalize(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

		final String normalized = assertDoesNotThrow(() -> normalizer.normalizeServiceInstanceId("AlertProvider|alertService|16.4.3"));
		assertEquals("AlertProvider|alertService|16.4.3", normalized);
		verify(serviceInstanceIdentifierNormalizer, times(1)).normalize("AlertProvider|alertService|16.4.3");

	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	@BeforeAll
	private static void initializeUtilitiesMock() {
		createUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	@BeforeEach
	private void resetUtilitiesMockBeforeEach() {
		resetUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	private void resetUtilitiesMock() {
		if (utilitiesMock != null) {
			utilitiesMock.close();
		}
		createUtilitiesMock();
	}

	//-------------------------------------------------------------------------------------------------
	private static void createUtilitiesMock() {
		utilitiesMock = mockStatic(Utilities.class);

		// mock common cases
		utilitiesMock.when(() -> Utilities.isEmpty(EMPTY)).thenReturn(true);
		utilitiesMock.when(() -> Utilities.isEmpty(List.of())).thenReturn(true);
	}

	//-------------------------------------------------------------------------------------------------
	@AfterAll
	private static void closeUtilitiesMock() {
		utilitiesMock.close();
	}
}
