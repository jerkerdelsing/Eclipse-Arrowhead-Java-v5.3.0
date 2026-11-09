package eu.arrowhead.onboarding;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.MultiValueMap;
import eu.arrowhead.common.http.ArrowheadHttpService;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.dto.*;
import eu.arrowhead.onboarding.service.DiscoveryService;
class DiscoveryServiceTest {
    ArrowheadHttpService http=mock(ArrowheadHttpService.class);
    DiscoveryService discovery=new DiscoveryService(http);
    @Test void usesExactV5LookupContractAndReturnsDescriptors() {
        var entries=List.of(entry("deviceDiscovery"),entry("systemDiscovery"),entry("serviceDiscovery"));
        when(http.consumeService(eq("serviceDiscovery"),eq("lookup"),eq("ServiceRegistry"),eq(ServiceInstanceListResponseDTO.class),any(ServiceInstanceLookupRequestDTO.class),any(MultiValueMap.class)))
            .thenReturn(new ServiceInstanceListResponseDTO(entries,3));
        assertEquals(entries,discovery.discover());
        var payload=ArgumentCaptor.forClass(ServiceInstanceLookupRequestDTO.class);
        var params=ArgumentCaptor.forClass(MultiValueMap.class);
        verify(http).consumeService(eq("serviceDiscovery"),eq("lookup"),eq("ServiceRegistry"),eq(ServiceInstanceListResponseDTO.class),payload.capture(),params.capture());
        assertEquals(List.of("ServiceRegistry"),payload.getValue().providerNames());
        assertEquals(List.of("generic_https"),payload.getValue().interfaceTemplateNames());
        assertEquals(List.of("deviceDiscovery","systemDiscovery","serviceDiscovery"),payload.getValue().serviceDefinitionNames());
        assertEquals("false",params.getValue().getFirst("verbose"));
    }
    @Test void missingDefinitionFailsRatherThanReturningPartialSuccess() {
        when(http.consumeService(eq("serviceDiscovery"),eq("lookup"),eq("ServiceRegistry"),eq(ServiceInstanceListResponseDTO.class),any(ServiceInstanceLookupRequestDTO.class),any(MultiValueMap.class)))
            .thenReturn(new ServiceInstanceListResponseDTO(List.of(entry("deviceDiscovery")),1));
        assertThrows(InternalServerError.class,discovery::discover);
    }
    @Test void nullResultFailsClosed(){assertThrows(InternalServerError.class,discovery::discover);}
    static ServiceInstanceResponseDTO entry(String name){return new ServiceInstanceResponseDTO(name,null,new ServiceDefinitionResponseDTO(name,null,null),"1.0.0",null,null,List.of(),null,null);}
}
