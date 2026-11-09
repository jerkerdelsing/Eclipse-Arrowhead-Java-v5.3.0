package eu.arrowhead.onboarding;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import eu.arrowhead.common.exception.*;
import eu.arrowhead.onboarding.service.CsrSupport;
class CsrSupportTest {
    CsrSupport csrs=new CsrSupport();
    @Test void generatedRequestPassesTheCheck(){
        var csr=csrs.generate("Sensor1.TestCloud.Company.arrowhead.eu");
        assertEquals(csr.publicKey(),csrs.check(csr.encoded(),"Sensor1.TestCloud.Company.arrowhead.eu"));
        assertNotNull(csr.privateKey());
    }
    @Test void nameMustMatch(){
        var csr=csrs.generate("Mallory.TestCloud.Company.arrowhead.eu");
        assertThrows(ForbiddenException.class,()->csrs.check(csr.encoded(),"Sensor1.TestCloud.Company.arrowhead.eu"));
    }
    @Test void proofOfPossessionIsRequired() throws Exception {
        var fixture=new CertificateFixture();
        String foreign=CertificateFixture.csr("CN=Sensor1.TestCloud.Company.arrowhead.eu",fixture.clientKey,fixture.serverKey);
        assertThrows(InvalidParameterException.class,()->csrs.check(foreign,"Sensor1.TestCloud.Company.arrowhead.eu"));
        assertThrows(InvalidParameterException.class,()->csrs.check("not-base64!","Sensor1.TestCloud.Company.arrowhead.eu"));
    }
}
