package eu.arrowhead.port;

import static eu.arrowhead.port.Json.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import org.junit.jupiter.api.*;

// AH-GT-1 (J5): wire compatibility with the reference implementation (src/test/resources/ahgt1/make_vector.py), key
// exchange verification, sealing, and replay protection.
class SessionSecurityTest {
 static final Path DIR=Path.of("src/test/resources/ahgt1");
 static final String PROVIDER_CN="GateTunnel.GoCloud.Company.arrowhead.eu",CONSUMER_CN="GateTunnel.TestCloud.Company.arrowhead.eu";
 Map<String,Object>vector;SessionSecurity consumer,provider;
 @BeforeEach void load()throws Exception{
  vector=obj(parse(Files.readString(DIR.resolve("vector.json"))));
  consumer=new SessionSecurity(DIR.resolve("TestCloud-relay.key"),DIR.resolve("TestCloud-relay-chain.crt"),DIR.resolve("org-ca.crt"));
  provider=new SessionSecurity(DIR.resolve("GoCloud-relay.key"),DIR.resolve("GoCloud-relay-chain.crt"),DIR.resolve("org-ca.crt"));
  System.setProperty("arrowhead.env.SESSION_CLOCK_SKEW_SECONDS","1000000000"); // the vector's timestamps are from its creation
 }
 @AfterEach void clear(){System.clearProperty("arrowhead.env.SESSION_CLOCK_SKEW_SECONDS");}
 static PrivateKey x25519(Object raw)throws Exception{
  byte[]prefix=HexFormat.of().parseHex("302e020100300506032b656e04220420"),key=Base64.getDecoder().decode(str(raw)),der=new byte[prefix.length+32];
  System.arraycopy(prefix,0,der,0,prefix.length);System.arraycopy(key,0,der,prefix.length,32);return KeyFactory.getInstance("X25519").generatePrivate(new PKCS8EncodedKeySpec(der));
 }

 @Test void verifiesTheReferenceKeyExchangeAndRefusesWrongPeersOrTranscripts()throws Exception{
  var session=obj(vector.get("session"));var pe=obj(vector.get("providerExchange"));var ce=obj(vector.get("consumerExchange"));
  consumer.verifyPeer(pe.get("certificateChain"),PROVIDER_CN,pe.get("signature"),SessionSecurity.transcript("provider",session));
  provider.verifyPeer(ce.get("certificateChain"),CONSUMER_CN,ce.get("signature"),SessionSecurity.transcript("consumer",session));
  assertThrows(SessionSecurity.SecurityError.class,()->consumer.verifyPeer(pe.get("certificateChain"),CONSUMER_CN,pe.get("signature"),SessionSecurity.transcript("provider",session)));
  var tampered=new LinkedHashMap<>(session);tampered.put("provider","OtherProvider");
  assertThrows(SessionSecurity.SecurityError.class,()->consumer.verifyPeer(pe.get("certificateChain"),PROVIDER_CN,pe.get("signature"),SessionSecurity.transcript("provider",tampered)));
  // a chain whose relay certificate is outside its cross-certificate's name constraint: GoCloud's leaf under TestCloud's CA
  String goChain=Files.readString(DIR.resolve("GoCloud-relay-chain.crt")),testChain=Files.readString(DIR.resolve("TestCloud-relay-chain.crt"));
  String mixed=goChain.substring(0,goChain.indexOf("-----END CERTIFICATE-----")+26)+testChain.substring(testChain.indexOf("-----END CERTIFICATE-----")+26);
  assertThrows(SessionSecurity.SecurityError.class,()->consumer.verifyPeer(mixed,PROVIDER_CN,pe.get("signature"),SessionSecurity.transcript("provider",session)));
  // an own signature verifies for the peer
  String own=provider.sign(SessionSecurity.transcript("provider",session));
  consumer.verifyPeer(pe.get("certificateChain"),PROVIDER_CN,own,SessionSecurity.transcript("provider",session));
 }

 @Test void derivesTheReferenceKeysAndOpensItsMessages()throws Exception{
  var session=obj(vector.get("session"));String id=str(session.get("sessionId"));
  var fromConsumer=SessionSecurity.derive(x25519(vector.get("consumerEphemeralPrivate")),session.get("providerKey"),id);
  var fromProvider=SessionSecurity.derive(x25519(vector.get("providerEphemeralPrivate")),session.get("consumerKey"),id);
  for(String direction:List.of("c2p","p2c")){
   assertArrayEquals(Base64.getDecoder().decode(str(vector.get(direction+"Key"))),fromConsumer.get(direction));assertArrayEquals(fromConsumer.get(direction),fromProvider.get(direction));
  }
  var request=SessionSecurity.open(fromProvider.get("c2p"),id,"c2p",obj(vector.get("request")));
  assertEquals(7,request.sequence());assertEquals("/datamanager/data-historian/put-data",request.content().get("path"));
  var reply=SessionSecurity.open(fromConsumer.get("p2c"),id,"p2c",obj(vector.get("reply")));
  assertEquals(201,integer(reply.content().get("status"),0));
  // wrong direction or session, or a changed ciphertext, does not open
  assertThrows(SessionSecurity.SecurityError.class,()->SessionSecurity.open(fromProvider.get("c2p"),id,"p2c",obj(vector.get("request"))));
  assertThrows(SessionSecurity.SecurityError.class,()->SessionSecurity.open(fromProvider.get("c2p"),"other-session","c2p",obj(vector.get("request"))));
  var changed=new LinkedHashMap<>(obj(vector.get("request")));String c=str(changed.get("ciphertext"));changed.put("ciphertext",(c.charAt(0)=='A'?"B":"A")+c.substring(1));
  assertThrows(SessionSecurity.SecurityError.class,()->SessionSecurity.open(fromProvider.get("c2p"),id,"c2p",changed));
 }

 @Test void sealedMessagesRoundTripAndStaleOrReplayedOnesAreRefused()throws Exception{
  System.clearProperty("arrowhead.env.SESSION_CLOCK_SKEW_SECONDS");
  var a=SessionSecurity.ephemeral();var b=SessionSecurity.ephemeral();
  var ka=SessionSecurity.derive(a.getPrivate(),SessionSecurity.raw(b.getPublic()),"s1");var kb=SessionSecurity.derive(b.getPrivate(),SessionSecurity.raw(a.getPublic()),"s1");
  var sealed=SessionSecurity.seal(ka.get("c2p"),"s1","c2p",3,m("method","GET","path","/x"));
  assertEquals("/x",SessionSecurity.open(kb.get("c2p"),"s1","c2p",obj(parse(bytes(sealed)))).content().get("path"));
  // a message outside the clock-skew window is refused (a negative window makes every message stale)
  System.setProperty("arrowhead.env.SESSION_CLOCK_SKEW_SECONDS","-1");
  assertThrows(SessionSecurity.SecurityError.class,()->SessionSecurity.open(kb.get("c2p"),"s1","c2p",obj(parse(bytes(sealed)))));
  System.clearProperty("arrowhead.env.SESSION_CLOCK_SKEW_SECONDS");
  var replay=new SessionSecurity.Replay(4);
  assertTrue(replay.accept(1));assertTrue(replay.accept(3));assertFalse(replay.accept(3));assertTrue(replay.accept(2));assertFalse(replay.accept(0));
  assertTrue(replay.accept(9));assertFalse(replay.accept(4));assertTrue(replay.accept(6));
 }
}
