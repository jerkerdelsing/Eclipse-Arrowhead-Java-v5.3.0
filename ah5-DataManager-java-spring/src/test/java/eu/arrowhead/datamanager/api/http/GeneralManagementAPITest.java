package eu.arrowhead.datamanager.api.http;
import eu.arrowhead.common.Constants;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.sysmonitor.SysMonitorHealth;
import eu.arrowhead.datamanager.service.DataManagerHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class GeneralManagementAPITest {
 @Test void onlyAuthenticatedSysopCanReadManagementStatus(){
  var indicator=mock(DataManagerHealthIndicator.class);var api=new GeneralManagementAPI(indicator);
  var request=new MockHttpServletRequest("GET","/datamanager/mgmt/general/status");
  assertThrows(ForbiddenException.class,()->api.status(request));verifyNoInteractions(indicator);
  request.setAttribute(Constants.HTTP_ATTR_ARROWHEAD_SYSOP_REQUEST,true);
  when(indicator.health()).thenReturn(SysMonitorHealth.degraded("storage unavailable"));
  assertEquals("degraded",api.status(request).get("status"));
 }
}
