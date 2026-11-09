package eu.arrowhead.port;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.cert.*;
import java.security.spec.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import static eu.arrowhead.port.Json.*;

// AH-GT-1: end-to-end protection of inter-cloud GateTunnel sessions (security finding J5). Wire-compatible with the
// reference Arrowhead-test-cloud-java/gatetunnel/src/session_security.py and the Go relay (ah5-support-go/internal/relay).
// Key exchange, once per session, carried by the Gatekeepers' ICN messages and the first tunnel request: each GateTunnel
// makes an ephemeral X25519 key and signs a transcript of the session parameters and the ephemeral key(s) with its relay
// key (RSA-PSS, SHA-256, salt 32). The signature comes with the relay chain (relay certificate + cross-certificate of its
// cloud CA), verified against the organization CA, the cross-certificate's name constraint and the expected peer name.
// Both sides derive one AES-256-GCM key per direction (HKDF-SHA256, salt = session id). Every relayed request and reply
// is then sealed (AAD = protocol, session, direction, sequence); requests carry a sequence number and a timestamp, so
// duplicates and stale messages are refused, and a reply must carry the sequence number of its request. The relay sees
// only ciphertext; the ephemeral keys give forward secrecy.
final class SessionSecurity {
 static final String PROTOCOL="AH-GT-1";
 static final byte[]X25519_PREFIX=HexFormat.of().parseHex("302a300506032b656e032100");
 static final SecureRandom RANDOM=new SecureRandom();

 static final class SecurityError extends Exception{SecurityError(String m){super(m);}SecurityError(String m,Throwable c){super(m,c);}}
 record Opened(long sequence,Map<String,Object>content){}

 final PrivateKey key;final String chainPem;final X509Certificate orgCa;
 SessionSecurity(Path keyFile,Path chainFile,Path orgCaFile)throws Exception{key=Tls.key(keyFile);chainPem=Files.readString(chainFile);orgCa=Tls.cert(orgCaFile);}
 static SessionSecurity fromEnvironment(String role)throws Exception{return new SessionSecurity(Path.of(env("RELAY_KEY","/certs/"+role+"-relay.key")),Path.of(env("RELAY_CHAIN_CERT","/certs/"+role+"-relay-chain.crt")),Path.of(env("RELAY_CA_CERT","/certs/org-ca.crt")));}

 static Signature pss()throws Exception{var s=Signature.getInstance("RSASSA-PSS");s.setParameter(new PSSParameterSpec("SHA-256","MGF1",MGF1ParameterSpec.SHA256,32,1));return s;}
 String sign(byte[]transcript)throws Exception{var s=pss();s.initSign(key);s.update(transcript);return Base64.getEncoder().encodeToString(s.sign());}

 // The own half of the key exchange: ephemeral key, signature over the transcript, relay chain.
 Map<String,Object>exchange(String role,Map<String,Object>session)throws Exception{return m(role+"Key",session.get(role+"Key"),"signature",sign(transcript(role,session)),"certificateChain",chainPem);}

 // Verifies relay certificate -> cross-certificate -> organization CA (PKIX: signatures, validity, CA flags and the
 // directory-name constraint), the expected peer name and profile, and the signature over the transcript.
 void verifyPeer(Object chain,String expectedCn,Object signature,byte[]transcript)throws SecurityError{
  try{
   var factory=CertificateFactory.getInstance("X.509");var certs=new ArrayList<X509Certificate>();
   for(var c:factory.generateCertificates(new ByteArrayInputStream(str(chain).getBytes(StandardCharsets.US_ASCII))))certs.add((X509Certificate)c);
   if(certs.size()!=2)throw new SecurityError("peer chain must be relay certificate + cross-certificate");
   var params=new PKIXParameters(Set.of(new TrustAnchor(orgCa,null)));params.setRevocationEnabled(false);
   try{CertPathValidator.getInstance("PKIX").validate(factory.generateCertPath(certs),params);}catch(CertPathValidatorException e){throw new SecurityError("peer chain does not verify: "+e.getMessage(),e);}
   var leaf=certs.getFirst();if(leaf.getBasicConstraints()>=0)throw new SecurityError("peer relay certificate must not be a CA");
   if(!cn(leaf).equals(expectedCn))throw new SecurityError("peer relay certificate is not "+expectedCn);
   if(!name(leaf,"DNQ").equals("sy"))throw new SecurityError("peer relay certificate is not a system (sy) certificate");
   var s=pss();s.initVerify(leaf.getPublicKey());s.update(transcript);
   if(!s.verify(Base64.getDecoder().decode(str(signature))))throw new SecurityError("peer key-exchange signature is invalid");
  }catch(SecurityError e){throw e;}catch(Exception e){throw new SecurityError("peer key exchange is malformed",e);}
 }

 // Canonical bytes both sides sign: protocol, role, session parameters and the ephemeral key(s) so far.
 static byte[]transcript(String role,Map<String,Object>s){
  var fields=new ArrayList<String>(List.of(PROTOCOL,role,str(s.get("sessionId")),str(s.get("consumer")),str(s.get("consumerCloud")),str(s.get("provider")),str(s.get("serviceDefinition")),str(s.get("consumerGateTunnel")),str(s.get("providerGateTunnel")),str(s.get("providerKey"))));
  if(role.equals("consumer"))fields.add(str(s.get("consumerKey")));return String.join("\n",fields).getBytes(StandardCharsets.UTF_8);
 }

 static KeyPair ephemeral()throws Exception{return KeyPairGenerator.getInstance("X25519").generateKeyPair();}
 static String raw(PublicKey key){byte[]e=key.getEncoded();return Base64.getEncoder().encodeToString(Arrays.copyOfRange(e,e.length-32,e.length));}
 static Map<String,byte[]>derive(PrivateKey own,Object peerPublic,String sessionId)throws SecurityError{
  try{
   byte[]rawPeer=Base64.getDecoder().decode(str(peerPublic));if(rawPeer.length!=32)throw new SecurityError("peer key is not an X25519 public key");
   byte[]encoded=new byte[X25519_PREFIX.length+32];System.arraycopy(X25519_PREFIX,0,encoded,0,X25519_PREFIX.length);System.arraycopy(rawPeer,0,encoded,X25519_PREFIX.length,32);
   var agreement=KeyAgreement.getInstance("X25519");agreement.init(own);agreement.doPhase(KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(encoded)),true);byte[]shared=agreement.generateSecret();
   var keys=new HashMap<String,byte[]>();for(String direction:List.of("c2p","p2c"))keys.put(direction,hkdf(shared,sessionId.getBytes(StandardCharsets.UTF_8),(PROTOCOL+" "+direction).getBytes(StandardCharsets.UTF_8)));return keys;
  }catch(SecurityError e){throw e;}catch(Exception e){throw new SecurityError("key agreement failed",e);}
 }
 // HKDF-SHA256 (RFC 5869) with a 32-byte output: one expand block.
 static byte[]hkdf(byte[]ikm,byte[]salt,byte[]info)throws Exception{
  var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(salt,"HmacSHA256"));byte[]prk=mac.doFinal(ikm);
  mac.init(new SecretKeySpec(prk,"HmacSHA256"));mac.update(info);mac.update((byte)1);return mac.doFinal();
 }

 static byte[]aad(String sessionId,String direction,long sequence){return (PROTOCOL+"|"+sessionId+"|"+direction+"|"+sequence).getBytes(StandardCharsets.UTF_8);}
 static Map<String,Object>seal(byte[]key,String sessionId,String direction,long sequence,Map<String,Object>content)throws Exception{
  byte[]nonce=new byte[12];RANDOM.nextBytes(nonce);var plain=new LinkedHashMap<>(content);plain.put("timestamp",System.currentTimeMillis()/1000.0);
  var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));cipher.updateAAD(aad(sessionId,direction,sequence));
  return m("seq",sequence,"nonce",Base64.getEncoder().encodeToString(nonce),"ciphertext",Base64.getEncoder().encodeToString(cipher.doFinal(bytes(plain))));
 }
 static Opened open(byte[]key,String sessionId,String direction,Map<String,Object>envelope)throws SecurityError{
  long sequence;Map<String,Object>content;
  try{
   sequence=((Number)envelope.get("seq")).longValue();
   var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Base64.getDecoder().decode(str(envelope.get("nonce")))));cipher.updateAAD(aad(sessionId,direction,sequence));
   content=obj(parse(cipher.doFinal(Base64.getDecoder().decode(str(envelope.get("ciphertext"))))));
  }catch(Exception e){throw new SecurityError("message does not decrypt or authenticate",e);}
  double timestamp=content.get("timestamp")instanceof Number n?n.doubleValue():0;
  if(Math.abs(System.currentTimeMillis()/1000.0-timestamp)>envInt("SESSION_CLOCK_SKEW_SECONDS",120))throw new SecurityError("message is outside the accepted time window");
  return new Opened(sequence,content);
 }

 // Accepts each sequence number once, within a sliding window above the highest seen.
 static final class Replay{
  final int size;long highest;final TreeSet<Long>seen=new TreeSet<>();Replay(int size){this.size=size;}Replay(){this(4096);}
  synchronized boolean accept(long sequence){if(sequence<=0||seen.contains(sequence)||sequence<=highest-size)return false;seen.add(sequence);highest=Math.max(highest,sequence);seen.headSet(highest-size,true).clear();return true;}
 }
}
