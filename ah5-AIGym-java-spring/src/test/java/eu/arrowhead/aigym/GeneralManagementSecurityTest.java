package eu.arrowhead.aigym;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.FilterChain;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneralManagementSecurityTest {
 @Test void identityVerificationUsesDiscoveredPathTemplateOrLegacySuffix() {
  for(String path:new String[]{"https://authentication:8444/authentication/identity/verify/{token}","https://authentication:8444/authentication/identity/verify"}) {
   assertEquals("https://authentication:8444/authentication/identity/verify/a%2Fb",ArrowheadGateway.withPathParameter(path,"a/b"));
  }
 }

 @Test void managementCannotBeGrantedByFunctionalServicePolicy() throws Exception {
  var gateway=mock(ArrowheadGateway.class);gateway.cloud="TestCloud";
  var filter=new ArrowheadSecurityFilter(gateway,new ObjectMapper());
  for(String caller:new String[]{"Sysop","AnySystem"}){
   when(gateway.verifyIdentity("test-token")).thenReturn(Map.of("verified",true,"systemName",caller));
   when(gateway.verifyAuthorization(caller,"status")).thenReturn(true);
   var cert=mock(X509Certificate.class);when(cert.getSubjectX500Principal()).thenReturn(new X500Principal("CN="+caller+".TestCloud.Company.arrowhead.eu"));
   var req=new MockHttpServletRequest("GET","/aigym/mgmt/general/status");
   req.addHeader("Authorization","Bearer IDENTITY-TOKEN//test-token");req.setAttribute("jakarta.servlet.request.X509Certificate",new X509Certificate[]{cert});
   var response=new MockHttpServletResponse();var chain=mock(FilterChain.class);filter.doFilter(req,response,chain);
   assertEquals(caller.equals("Sysop")?200:403,response.getStatus());
   if(caller.equals("Sysop"))verify(chain).doFilter(req,response);else verifyNoInteractions(chain);
  }
 }
}
