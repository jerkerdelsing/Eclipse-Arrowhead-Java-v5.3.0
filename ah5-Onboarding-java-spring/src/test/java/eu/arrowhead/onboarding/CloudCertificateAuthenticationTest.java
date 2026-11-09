package eu.arrowhead.onboarding;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.security.CertificateProfileType;
import eu.arrowhead.onboarding.security.CloudCertificateAuthentication;
import eu.arrowhead.onboarding.security.CloudIdentity;
import eu.arrowhead.onboarding.service.CertificateAuthorityClient;
class CloudCertificateAuthenticationTest {
    CertificateFixture fixture=new CertificateFixture();
    CertificateAuthorityClient authority=mock(CertificateAuthorityClient.class);
    CloudCertificateAuthentication authentication=new CloudCertificateAuthentication(
        new CloudIdentity(fixture.cloud.getSubjectX500Principal(),"TestCloud.Company.arrowhead.eu"),authority);
    MockHttpServletRequest request(X509Certificate certificate){
        var request=new MockHttpServletRequest();request.setSecure(true);
        request.setAttribute("jakarta.servlet.request.X509Certificate",new X509Certificate[]{certificate});return request;
    }
    @Test void acceptsAGoodDeviceCertificateOfTheCloud(){
        when(authority.status(any())).thenReturn("GOOD");
        var caller=authentication.authenticate(request(fixture.client),CertificateProfileType.DEVICE);
        assertEquals("Device1",caller.name());
        assertEquals(fixture.client.getSerialNumber().toString(),caller.serialNumber());
    }
    @Test void requiresTheProfileOfTheStep(){
        when(authority.status(any())).thenReturn("GOOD");
        assertThrows(ForbiddenException.class,()->authentication.authenticate(request(fixture.client),CertificateProfileType.ON_BOARDING));
        assertThrows(ForbiddenException.class,()->authentication.authenticate(request(fixture.server),CertificateProfileType.DEVICE));
    }
    @Test void rejectsCertificatesOfAnotherIssuer(){
        var foreign=CertificateFixture.cert("CN=Device1.TestCloud.Company.arrowhead.eu,DN=de",fixture.clientKey,fixture.organization,fixture.orgKey,-1);
        assertThrows(ForbiddenException.class,()->authentication.authenticate(request(foreign),CertificateProfileType.DEVICE));
        verifyNoInteractions(authority);
    }
    @Test void rejectsRevokedCertificates(){
        when(authority.status(any())).thenReturn("REVOKED");
        assertThrows(ForbiddenException.class,()->authentication.authenticate(request(fixture.client),CertificateProfileType.DEVICE));
    }
}
