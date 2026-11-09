package eu.arrowhead.onboarding;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import eu.arrowhead.common.exception.*;
import eu.arrowhead.onboarding.jpa.entity.DeviceEnrollment;
import eu.arrowhead.onboarding.jpa.repository.DeviceEnrollmentRepository;
import eu.arrowhead.onboarding.security.BootstrapAuthentication;
class BootstrapAuthenticationTest {
    DeviceEnrollmentRepository repository=mock(DeviceEnrollmentRepository.class);
    BootstrapAuthentication authentication=new BootstrapAuthentication(repository);
    MockHttpServletRequest request=new MockHttpServletRequest();
    CertificateFixture fixture=new CertificateFixture();
    @BeforeEach void setup(){request.setSecure(true);}
    @Test void requiresTls(){request.setSecure(false);assertThrows(ForbiddenException.class,()->authentication.authenticate(request));}
    @Test void missingCertificateFailsClosed(){assertThrows(AuthException.class,()->authentication.authenticate(request));}
    @Test void basicCredentialsAreNotAccepted(){request.addHeader("Authorization","Basic "+Base64.getEncoder().encodeToString("Device1:secret".getBytes(StandardCharsets.UTF_8)));assertThrows(AuthException.class,()->authentication.authenticate(request));}
    @Test void certificateMustBeApprovedByAnOperator() throws Exception {
        request.setAttribute("jakarta.servlet.request.X509Certificate",new X509Certificate[]{fixture.client});
        when(repository.findByBootstrapFingerprint(any())).thenReturn(Optional.empty());
        assertThrows(AuthException.class,()->authentication.authenticate(request));
        var enrollment=new DeviceEnrollment("Device1",BootstrapAuthentication.fingerprint(fixture.client),List.of("Sensor1"),"Sysop");
        when(repository.findByBootstrapFingerprint(BootstrapAuthentication.fingerprint(fixture.client))).thenReturn(Optional.of(enrollment));
        assertEquals("Device1",authentication.authenticate(request).getDeviceName());
    }
    @Test void offboardedDeviceNeedsANewApproval() throws Exception {
        request.setAttribute("jakarta.servlet.request.X509Certificate",new X509Certificate[]{fixture.client});
        var enrollment=new DeviceEnrollment("Device1",BootstrapAuthentication.fingerprint(fixture.client),List.of("Sensor1"),"Sysop");
        enrollment.offboarded("test");
        when(repository.findByBootstrapFingerprint(any())).thenReturn(Optional.of(enrollment));
        assertThrows(ForbiddenException.class,()->authentication.authenticate(request));
    }
}
