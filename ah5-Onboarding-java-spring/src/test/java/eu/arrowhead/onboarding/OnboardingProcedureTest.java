package eu.arrowhead.onboarding;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.*;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.*;
import org.mockito.InOrder;
import eu.arrowhead.common.exception.*;
import eu.arrowhead.onboarding.api.dto.OnboardingDtos.*;
import eu.arrowhead.onboarding.jpa.entity.*;
import eu.arrowhead.onboarding.jpa.repository.*;
import eu.arrowhead.onboarding.security.CloudCertificateAuthentication.Authenticated;
import eu.arrowhead.onboarding.security.CloudIdentity;
import eu.arrowhead.onboarding.service.*;
class OnboardingProcedureTest {
    static final String FINGERPRINT="ab".repeat(32);
    CertificateAuthorityClient authority=mock(CertificateAuthorityClient.class);
    CloudClient cloud=mock(CloudClient.class);
    DiscoveryService discovery=mock(DiscoveryService.class);
    DeviceEnrollmentRepository devices=mock(DeviceEnrollmentRepository.class);
    SystemEnrollmentRepository systems=mock(SystemEnrollmentRepository.class);
    EnrollmentService enrollments=new EnrollmentService(devices,systems,authority,cloud);
    OnboardingService service=new OnboardingService(new CloudIdentity(new X500Principal("CN=TestCloud.Company.arrowhead.eu"),"TestCloud.Company.arrowhead.eu"),
        new CsrSupport(),authority,cloud,discovery,devices,systems,enrollments);
    DeviceEnrollment enrollment=new DeviceEnrollment("Device1",FINGERPRINT,List.of("Sensor1","Actuator1"),"Sysop");
    long nextId=10;
    static final Authenticated DEVICE=new Authenticated("Device1","serial-1");
    @BeforeEach void setup(){
        when(devices.findById("Device1")).thenReturn(Optional.of(enrollment));
        when(devices.save(any())).thenAnswer(i->i.getArgument(0));
        when(systems.findById(any())).thenReturn(Optional.empty());
        when(systems.findByDeviceName(any())).thenReturn(List.of());
        when(authority.sign(any(),any(),any())).thenAnswer(i->{long id=nextId++;return new CertificateAuthorityClient.Signed(id,List.of("LEAF","CA"),i.getArgument(1),"serial-"+id,"2027-10-14T00:00:00Z");});
    }
    @Test void onboardingCertificateIsSignedByTheCertificateAuthority(){
        var response=service.withName(new NameRequest("Device1.TestCloud.Company.arrowhead.eu"),enrollment,"172.19.0.9");
        verify(authority).sign(any(),eq("on"),eq(List.of("172.19.0.9")));
        assertEquals(List.of("LEAF","CA"),response.certificateChain());
        assertNotNull(response.onboardingCertificate().privateKey());
        assertThrows(ForbiddenException.class,()->service.withName(new NameRequest("Other.TestCloud.Company.arrowhead.eu"),enrollment,"10.0.0.1"));
    }
    @Test void deviceStepIssuesDeviceCertificateAndRegistersTheDevice(){
        enrollment.onboardingIssued(3,"serial-3");
        var response=service.device(new Authenticated("Device1","serial-3"),new DeviceRequest(null,List.of("172.19.0.9","02:42:ac:13:00:09"),Map.of("vendor","Test")),"10.0.0.1");
        verify(authority).sign(any(),eq("de"),eq(List.of("172.19.0.9")));   // the MAC address goes only to the registry
        verify(cloud).createDevice(eq("Device1"),eq(List.of("172.19.0.9","02:42:ac:13:00:09")),argThat(m->"Test".equals(m.get("vendor"))&&m.containsKey("onboarding")));
        assertEquals(DeviceEnrollment.State.DEVICE_REGISTERED,enrollment.getState());
        assertEquals(10L,enrollment.getDeviceCertificateId());
        assertEquals(List.of("Sensor1","Actuator1"),response.approvedSystems());
    }
    @Test void systemStepNeedsARegisteredDeviceAndAnApprovedSystem(){
        assertThrows(ForbiddenException.class,()->service.system(DEVICE,new SystemRequest("Sensor1",null)));
        enrollment.deviceRegistered(1,"serial-1","2027-01-01T00:00:00Z");
        assertThrows(ForbiddenException.class,()->service.system(DEVICE,new SystemRequest("Mallory",null)));
        verify(authority,never()).sign(any(),eq("sy"),any());
    }
    @Test void systemStepIssuesCertificateAndIdentity(){
        enrollment.deviceRegistered(1,"serial-1","2027-01-01T00:00:00Z");
        var response=service.system(DEVICE,new SystemRequest("Sensor1",null));
        verify(authority).sign(any(),eq("sy"),isNull());
        verify(cloud).createIdentity(eq("Sensor1"),eq(response.credentials().get("password")));
        verify(systems).save(argThat(s->s.getSystemName().equals("Sensor1")&&s.getCertificateId()==10L));
        assertEquals("PASSWORD",response.authenticationMethod());
        assertTrue(response.credentials().get("password").length()>=32);
    }
    @Test void failedIdentityCreationWithdrawsTheCertificate(){
        enrollment.deviceRegistered(1,"serial-1","2027-01-01T00:00:00Z");
        doThrow(new ExternalServerError("Authentication down")).when(cloud).createIdentity(any(),any());
        assertThrows(ExternalServerError.class,()->service.system(DEVICE,new SystemRequest("Sensor1",null)));
        verify(authority).revoke(10L);
        verify(systems,never()).save(any());
    }
    @Test void offboardingWithdrawsEverythingAndContinuesAfterAFailure(){
        enrollment.onboardingIssued(4,"serial-4");
        enrollment.deviceRegistered(5,"serial-5","2027-01-01T00:00:00Z");
        var sensor=new SystemEnrollment("Sensor1","Device1",11,"2027-01-01T00:00:00Z");
        when(systems.findByDeviceName("Device1")).thenReturn(List.of(sensor));
        doThrow(new ExternalServerError("Blacklist down")).when(cloud).blacklist(any(),any(),any());
        var report=service.offboard(new Authenticated("Device1","serial-5"));
        InOrder order=inOrder(cloud,authority);
        order.verify(cloud).removeSystem("Sensor1");
        order.verify(cloud).removeIdentity("Sensor1");
        order.verify(authority).revoke(11L);
        order.verify(cloud).removeDevice("Device1");
        order.verify(authority).revoke(5L);
        order.verify(authority).revoke(4L);
        assertEquals(1,report.failed().size());
        assertEquals(6,report.completed().size());
        assertEquals(DeviceEnrollment.State.OFFBOARDED,enrollment.getState());
    }
    @Test void onlyTheLatestOnboardingAndDeviceCertificatesAreAccepted(){
        enrollment.onboardingIssued(3,"serial-3");
        assertThrows(ForbiddenException.class,()->service.device(new Authenticated("Device1","serial-2"),null,"10.0.0.1"));
        enrollment.deviceRegistered(1,"serial-1","2027-01-01T00:00:00Z");
        assertThrows(ForbiddenException.class,()->service.system(new Authenticated("Device1","serial-0"),new SystemRequest("Sensor1",null)));
        assertThrows(ForbiddenException.class,()->service.offboard(new Authenticated("Device1","serial-0")));
        verify(authority,never()).sign(any(),eq("de"),any());
        verify(authority,never()).sign(any(),eq("sy"),any());
    }
    @Test void newOnboardingCertificateReplacesThePreviousOne(){
        service.withName(new NameRequest("Device1.TestCloud.Company.arrowhead.eu"),enrollment,"10.0.0.1");
        service.withName(new NameRequest("Device1.TestCloud.Company.arrowhead.eu"),enrollment,"10.0.0.1");
        verify(authority).revoke(10L);
        assertEquals("serial-11",enrollment.getOnboardingCertificateSerial());
    }
    @Test void approvalValidatesNamesAndFingerprint(){
        when(devices.findById("Device2")).thenReturn(Optional.empty());
        when(devices.findByBootstrapFingerprint(any())).thenReturn(Optional.empty());
        assertThrows(InvalidParameterException.class,()->enrollments.approve("Device2",new EnrollmentRequest("xyz",List.of("Sensor2")),"Sysop"));
        assertThrows(InvalidParameterException.class,()->enrollments.approve("Device2",new EnrollmentRequest("cd".repeat(32),List.of("sensor2")),"Sysop"));
        assertThrows(InvalidParameterException.class,()->enrollments.approve("Device2",new EnrollmentRequest("cd".repeat(32),List.of()),"Sysop"));
        var view=enrollments.approve("Device2",new EnrollmentRequest("CD:".repeat(31)+"CD",List.of("Sensor2")),"Sysop");
        assertEquals("cd".repeat(32),view.bootstrapFingerprint());
        assertEquals("APPROVED",view.state());
    }
    @Test void approvalRefusesReservedAndTakenNames(){
        when(devices.findById("Device2")).thenReturn(Optional.empty());
        when(devices.findByBootstrapFingerprint(any())).thenReturn(Optional.empty());
        when(devices.findAll()).thenReturn(List.of(enrollment));
        String fp="cd".repeat(32);
        // reserved core and support system names, as device or as system (M1)
        assertThrows(InvalidParameterException.class,()->enrollments.approve("SysMonitor",new EnrollmentRequest(fp,List.of("Sensor2")),"Sysop"));
        assertThrows(InvalidParameterException.class,()->enrollments.approve("Device2",new EnrollmentRequest(fp,List.of("Gatekeeper")),"Sysop"));
        // names another device already uses for itself or its systems
        assertThrows(InvalidParameterException.class,()->enrollments.approve("Device2",new EnrollmentRequest(fp,List.of("Sensor1")),"Sysop"));
        assertThrows(InvalidParameterException.class,()->enrollments.approve("Device2",new EnrollmentRequest(fp,List.of("Device1")),"Sysop"));
        assertThrows(InvalidParameterException.class,()->enrollments.approve("Device2",new EnrollmentRequest(fp,List.of("Device2")),"Sysop"));
        assertEquals("APPROVED",enrollments.approve("Device2",new EnrollmentRequest(fp,List.of("Sensor2")),"Sysop").state());
        // re-approving the same device with its own names stays possible
        assertEquals(List.of("Sensor1"),enrollments.approve("Device1",new EnrollmentRequest(FINGERPRINT,List.of("Sensor1")),"Sysop").approvedSystems());
    }
}
