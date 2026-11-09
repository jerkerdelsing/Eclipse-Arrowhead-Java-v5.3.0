package eu.arrowhead.port;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class PeerNamesTest {
 @Test void explicitMappingIsUnambiguous(){assertEquals("Lab",PeerNames.parse("RustDev=Lab,GoCloud=Company").get("RustDev"));assertTrue(PeerNames.parse("").isEmpty());assertEquals("RustDev|Lab",PeerNames.cloudIdentity("GateTunnel.RustDev.Lab.arrowhead.eu"));}
 @Test void rejectsMalformedOrAmbiguousBindings(){for(String value:new String[]{"RustDev=Lab,RustDev=Company","RustDev=","=Lab","RustDev=Lab/other","RustDev=Lab,","../Rust=Lab"})assertThrows(IllegalArgumentException.class,()->PeerNames.parse(value));}
}
