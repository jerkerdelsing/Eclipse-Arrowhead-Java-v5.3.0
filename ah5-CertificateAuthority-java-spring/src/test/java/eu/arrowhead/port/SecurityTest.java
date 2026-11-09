package eu.arrowhead.port;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.net.http.*;
import java.security.*;
import java.security.cert.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.net.ssl.*;
import com.sun.net.httpserver.*;
import org.bouncycastle.asn1.x500.*;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.*;
import org.bouncycastle.pkcs.jcajce.*;
import static eu.arrowhead.port.Json.*;
import static org.junit.jupiter.api.Assertions.*;

class SecurityTest {
 @TempDir Path dir;final Set<String>properties=new HashSet<>();final List<AutoCloseable>close=new ArrayList<>();
 void env(String key,String value){properties.add(key);System.setProperty("arrowhead.env."+key,value);}
 @AfterEach void cleanup()throws Exception{Collections.reverse(close);for(var c:close)try{c.close();}catch(Exception ignored){}for(String key:properties)System.clearProperty("arrowhead.env."+key);}
 record Pair(Path cert,Path key,X509Certificate certificate,KeyPair keys){}
 final class PKI{
  final Pair root;final Path folder;int n;
  PKI(Path folder)throws Exception{this.folder=folder;Files.createDirectories(folder);root=issueRoot();}
  KeyPair key()throws Exception{var g=KeyPairGenerator.getInstance("EC");g.initialize(256);return g.generateKeyPair();}
  Pair save(X509Certificate cert,KeyPair keys)throws Exception{Path cp=folder.resolve("cert-"+n+".pem"),kp=folder.resolve("key-"+n+++".pem");Files.writeString(cp,CertificateAuthority.pem(cert));Files.writeString(kp,CertificateAuthority.pem(keys.getPrivate()));return new Pair(cp,kp,cert,keys);}
  Pair issueRoot()throws Exception{var keys=key();var name=new X500Name("C=SE,O=Company,CN=TestCloud.Company.arrowhead.eu");var b=new JcaX509v3CertificateBuilder(name,BigInteger.ONE,Date.from(Instant.now().minusSeconds(3600)),Date.from(Instant.now().plus(Duration.ofDays(1095))),name,keys.getPublic());b.addExtension(org.bouncycastle.asn1.x509.Extension.basicConstraints,true,new BasicConstraints(true));b.addExtension(org.bouncycastle.asn1.x509.Extension.keyUsage,true,new KeyUsage(KeyUsage.keyCertSign|KeyUsage.cRLSign));var cert=new JcaX509CertificateConverter().getCertificate(b.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keys.getPrivate())));return save(cert,keys);}
  Pair issue(String name,String profile)throws Exception{var keys=key();var subject=new X500NameBuilder().addRDN(BCStyle.C,"SE").addRDN(BCStyle.O,"Company").addRDN(BCStyle.OU,"Arrowhead Systems").addRDN(BCStyle.CN,name+".TestCloud.Company.arrowhead.eu").addRDN(BCStyle.DN_QUALIFIER,profile).build();var b=new JcaX509v3CertificateBuilder(X500Name.getInstance(root.certificate.getSubjectX500Principal().getEncoded()),new BigInteger(100,new SecureRandom()),Date.from(Instant.now().minusSeconds(60)),Date.from(Instant.now().plusSeconds(3600)),subject,keys.getPublic());b.addExtension(org.bouncycastle.asn1.x509.Extension.basicConstraints,true,new BasicConstraints(false));b.addExtension(org.bouncycastle.asn1.x509.Extension.extendedKeyUsage,false,new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth,KeyPurposeId.id_kp_serverAuth}));b.addExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName,false,new GeneralNames(new GeneralName[]{new GeneralName(GeneralName.dNSName,"localhost"),new GeneralName(GeneralName.iPAddress,"127.0.0.1")}));var cert=new JcaX509CertificateConverter().getCertificate(b.build(new JcaContentSignerBuilder("SHA256withECDSA").build(root.keys.getPrivate())));return save(cert,keys);}
 }
 void config(PKI p,Pair identity,String auth)throws Exception{env("LOCAL_CLOUD","TestCloud");env("ORGANIZATION","Company");env("TLS_CA_CERT",p.root.cert.toString());env("TLS_CA_KEY",p.root.key.toString());env("TLS_CERT",identity.cert.toString());env("TLS_KEY",identity.key.toString());env("TLS_SERVER_CERT",identity.cert.toString());env("TLS_SERVER_KEY",identity.key.toString());env("AUTHENTICATION_PASSWORD","isolated-fixture-password");env("AUTHENTICATION_URL",auth);env("DB_PATH",dir.resolve("fixture.db").toString());env("PORT","0");env("HEALTH_PORT","0");}
 HttpsServer server(PKI p,Pair identity,HttpHandler handler)throws Exception{var tls=Tls.context(identity.cert.toString(),identity.key.toString(),p.root.cert.toString(),null);var server=HttpsServer.create(new InetSocketAddress("127.0.0.1",0),16);server.setHttpsConfigurator(new HttpsConfigurator(tls){public void configure(HttpsParameters parameters){var ssl=tls.getDefaultSSLParameters();ssl.setNeedClientAuth(true);parameters.setSSLParameters(ssl);}});server.createContext("/",handler);server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());server.start();close.add(()->server.stop(0));return server;}
 String endpoint(HttpsServer server){return "https://127.0.0.1:"+server.getAddress().getPort();}
 HttpClient client(PKI p,Pair identity)throws Exception{return Service.httpClient(Tls.context(identity.cert.toString(),identity.key.toString(),p.root.cert.toString(),null));}
 HttpResponse<String>get(HttpClient client,String url,String authorization)throws Exception{var b=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8));if(authorization!=null)b.header("Authorization",authorization);return client.send(b.build(),HttpResponse.BodyHandlers.ofString());}
 static final class Probe extends Service{Probe()throws Exception{super("probe","FixtureService","fixture",0,0,"/data/state.db");}void route(HttpExchange x)throws Exception{send(x,200,m("caller",caller(x)));}void register(){registered(List.of());}}
 @Test void secretsFromFilesWhenNotInEnvironment()throws Exception{Path file=dir.resolve("secret");Files.writeString(file,"from-file\n");env("FIXTURE_SECRET_FILE",file.toString());assertEquals("from-file",Json.env("FIXTURE_SECRET",""));env("FIXTURE_SECRET","direct");assertEquals("direct",Json.env("FIXTURE_SECRET",""));env("MISSING_SECRET_FILE",dir.resolve("absent").toString());assertThrows(IllegalStateException.class,()->Json.env("MISSING_SECRET",""));assertEquals("fallback",Json.env("UNSET_SECRET","fallback"));}
 @Test void mutualTlsAndCertificateTokenBinding()throws Exception{
  var p=new PKI(dir.resolve("pki"));var authIdentity=p.issue("Authentication","sy");var verifiedName=new AtomicReference<>("LocalCloudManager");var verified=new AtomicBoolean(true);var authServer=server(p,authIdentity,x->{try{if(x.getRequestURI().getPath().endsWith("/login"))Service.send(x,200,m("token","own-token"));else Service.send(x,200,m("verified",verified.get(),"systemName",verifiedName.get()));}catch(Exception ignored){}finally{x.close();}});
  config(p,p.issue("FixtureService","sy"),endpoint(authServer));var probe=new Probe();close.add(probe);probe.start();String url=endpoint(probe.server)+"/protected";var lcm=p.issue("LocalCloudManager","sy");var valid=client(p,lcm);close.add(valid);
  assertEquals(200,get(valid,url,"Bearer IDENTITY-TOKEN//caller-token").statusCode());assertEquals(401,get(valid,url,null).statusCode());assertEquals(401,get(valid,url,"Bearer SYSTEM//Sysop").statusCode());verifiedName.set("Sysop");assertEquals(401,get(valid,url,"Bearer IDENTITY-TOKEN//caller-token").statusCode());verifiedName.set("LocalCloudManager");verified.set(false);assertEquals(401,get(valid,url,"Bearer IDENTITY-TOKEN//caller-token").statusCode());verified.set(true);
  var on=client(p,p.issue("LocalCloudManager","on"));close.add(on);assertEquals(403,get(on,url,"Bearer IDENTITY-TOKEN//caller-token").statusCode());
  var trust=KeyStore.getInstance(KeyStore.getDefaultType());trust.load(null,null);trust.setCertificateEntry("root",p.root.certificate);var tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(trust);var noCertContext=SSLContext.getInstance("TLS");noCertContext.init(null,tm.getTrustManagers(),null);var noCert=Service.httpClient(noCertContext);close.add(noCert);assertThrows(Exception.class,()->get(noCert,url,"Bearer IDENTITY-TOKEN//caller-token"));
 }
 @Test void tokenFreeHealthOverMutualTlsDisclosesOnlyLiveness()throws Exception{
  var p=new PKI(dir.resolve("pki"));config(p,p.issue("FixtureService","sy"),"https://127.0.0.1:1");var probe=new Probe();close.add(probe);probe.start();
  var other=client(p,p.issue("AnySystem","sy"));close.add(other);
  for(String path:List.of("/health","/certificate-authority/health")){var response=get(other,endpoint(probe.server)+path,null);assertEquals(200,response.statusCode());var body=Json.obj(Json.parse(response.body()));assertEquals(Set.of("status","system"),body.keySet());assertEquals("ok",body.get("status"));}
  assertTrue(probe.health().containsKey("registration"));
 }
 @Test void pinnedAuthenticationRejectsOtherTrustedSystemBeforeCredentials()throws Exception{var p=new PKI(dir.resolve("pki"));var calls=new AtomicInteger();var evil=server(p,p.issue("OtherSystem","sy"),x->{calls.incrementAndGet();try{Service.send(x,200,m("token","forged"));}catch(Exception ignored){}finally{x.close();}});config(p,p.issue("FixtureService","sy"),endpoint(evil));var service=new Probe();close.add(service);assertThrows(Exception.class,()->service.token(false));assertEquals(0,calls.get());}
 @Test void concurrentRejectedTokensRenewOnlyOnce() throws Exception {
  var p = new PKI(dir.resolve("pki"));
  var logins = new AtomicInteger();
  var authServer = server(p, p.issue("Authentication", "sy"), x -> {
   try { Service.send(x, 200, m("token", "fixture-token-" + logins.incrementAndGet())); }
   catch (Exception ignored) {} finally { x.close(); }
  });
  config(p, p.issue("FixtureService", "sy"), endpoint(authServer));
  var service = new Probe(); close.add(service);
  assertEquals("fixture-token-1", service.token(false));
  var staleRequests = new java.util.concurrent.CountDownLatch(4);
  var protectedServer = server(p, p.issue("OtherSystem", "sy"), x -> {
   try {
    String header = x.getRequestHeaders().getFirst("Authorization");
    if ("Bearer IDENTITY-TOKEN//fixture-token-1".equals(header)) {
     staleRequests.countDown(); staleRequests.await(5, java.util.concurrent.TimeUnit.SECONDS);
     Service.send(x, 401, m("error", "expired"));
    } else Service.send(x, 200, m("ok", true));
   } catch (Exception ignored) {} finally { x.close(); }
  });
  try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
   var tasks = new ArrayList<java.util.concurrent.Future<Object>>();
   for (int i = 0; i < 4; i++) tasks.add(workers.submit(() -> service.call(endpoint(protectedServer), "GET", null)));
   for (var task : tasks) assertEquals(true, obj(task.get(15, java.util.concurrent.TimeUnit.SECONDS)).get("ok"));
  }
  assertEquals(2, logins.get(), "Concurrent 401 responses must not invalidate another request's newly refreshed token");
 }
 // Only the ServiceRegistry is configured: Authentication and the orchestrator are looked up there, ConsumerAuthorization is orchestrated
 @Test void servicesAreOrchestratedNotConfigured()throws Exception{
  var p=new PKI(dir.resolve("pki"));var lookups=new ArrayList<String>();var pulls=new ArrayList<Map<String,Object>>();var verified=new AtomicInteger();
  var authServer=server(p,p.issue("Authentication","sy"),x->{try{Service.send(x,200,x.getRequestURI().getPath().endsWith("/login")?m("token","own-token"):m("verified",true,"systemName","LocalCloudManager"));}catch(Exception ignored){}finally{x.close();}});
  var provider=server(p,p.issue("ConsumerAuthorization","sy"),x->{try{assertEquals("/consumerauthorization/authorization/verify",x.getRequestURI().getPath());assertEquals("Bearer IDENTITY-TOKEN//own-token",x.getRequestHeaders().getFirst("Authorization"));verified.incrementAndGet();Service.send(x,200,true);}catch(Exception ignored){}finally{x.close();}});
  var dso=server(p,p.issue("DynamicServiceOrchestration","sy"),x->{try{pulls.add(obj(Service.body(x)));Service.send(x,200,m("results",List.of(m("providerName","ConsumerAuthorization","interfaces",List.of(m("templateName","generic_https","properties",m("accessAddresses",List.of("127.0.0.1"),"accessPort",provider.getAddress().getPort(),"basePath","/consumerauthorization/authorization","operations",m("verify",m("path","/verify","method","POST")))))))));}catch(Exception ignored){}finally{x.close();}});
  var registry=server(p,p.issue("ServiceRegistry","sy"),x->{try{var request=obj(Service.body(x));String definition=strings(request.get("serviceDefinitionNames")).getFirst();lookups.add(definition+(x.getRequestHeaders().containsKey("Authorization")?"+token":""));var target=definition.equals("identity")?authServer:dso;var ops=definition.equals("identity")?m("identity-login",m("path","/login","method","POST"),"identity-verify",m("path","/verify","method","GET")):m("pull",m("path","/pull","method","POST"));
   Service.send(x,200,m("entries",List.of(m("interfaces",List.of(m("templateName","generic_https","properties",m("accessAddresses",List.of("127.0.0.1"),"accessPort",target.getAddress().getPort(),"basePath",definition.equals("identity")?"/authentication/identity":"/serviceorchestration/orchestration","operations",ops)))))));}catch(Exception ignored){}finally{x.close();}});
  config(p,p.issue("FixtureService","sy"),"unused");System.clearProperty("arrowhead.env.AUTHENTICATION_URL");env("SERVICE_REGISTRY_URL",endpoint(registry));
  var service=new Probe();close.add(service);
  for(int i=0;i<2;i++)assertEquals(true,service.consume("authorization","verify","ConsumerAuthorization","",m("consumer","LocalCloudManager")));
  assertEquals(List.of("identity","serviceOrchestration+token"),lookups,"Authentication is looked up without a token, the orchestrator with one");
  assertEquals(1,pulls.size(),"the orchestration result is reused");assertEquals(2,verified.get());
  var requirement=obj(pulls.getFirst().get("serviceRequirement"));assertEquals("authorization",requirement.get("serviceDefinition"));assertEquals(List.of("verify"),requirement.get("operations"));assertEquals(List.of("ConsumerAuthorization"),requirement.get("preferredProviders"));assertEquals(true,obj(pulls.getFirst().get("orchestrationFlags")).get("ONLY_PREFERRED"));
  assertEquals(503,assertThrows(Service.ApiError.class,()->{pulls.clear();service.orchestrated.clear();service.orchestrate("unknownService","op",null);}).status,"no authorized provider of the operation");
 }
 String csr(PKI p,String name,String profile,GeneralNames sans)throws Exception{var keys=p.key();var subject=new X500NameBuilder().addRDN(BCStyle.CN,name+".TestCloud.Company.arrowhead.eu").addRDN(BCStyle.DN_QUALIFIER,profile).build();var builder=new JcaPKCS10CertificationRequestBuilder(subject,keys.getPublic());if(sans!=null){var extensions=new ExtensionsGenerator();extensions.addExtension(org.bouncycastle.asn1.x509.Extension.subjectAlternativeName,false,sans);builder.addAttribute(org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,extensions.generate());}var request=builder.build(new JcaContentSignerBuilder("SHA256withECDSA").build(keys.getPrivate()));return CertificateAuthority.pem(request);}
 @Test void certificateIssuanceProfilesRevocationAndPersistentCrl()throws Exception{
  var p=new PKI(dir.resolve("pki"));config(p,p.issue("CertificateAuthority","sy"),"https://localhost:8444");var ca=new CertificateAuthority();close.add(ca);
  String request=csr(p,"ProvisionedDevice","sy",new GeneralNames(new GeneralName(GeneralName.dNSName,"device.local")));var issued=ca.sign(m("encodedCSR",request),"Onboarding");var chain=strings(issued.get("certificateChain"));assertEquals(2,chain.size());var cert=CertificateAuthority.certificate(chain.getFirst());cert.verify(p.root.certificate.getPublicKey());assertFalse(cert.getBasicConstraints()>=0);assertEquals("sy",Json.name(cert,"DNQ"));assertEquals("SYSTEM",issued.get("profile"));assertTrue((Boolean)ca.check(m("certificate",chain.getFirst())).get("valid"));assertEquals(403,assertThrows(Service.ApiError.class,()->ca.sign(m("encodedCSR",request),"LocalCloudManager")).status);
  var revoked=ca.store.revokeContent(cert,"Sysop");assertTrue((Boolean)revoked.get("changed"));assertFalse((Boolean)ca.store.revokeContent(cert,"Sysop").get("changed"));assertEquals("REVOKED",ca.check(m("certificate",chain.getFirst())).get("status"));var crl=(X509CRL)CertificateFactory.getInstance("X.509").generateCRL(new ByteArrayInputStream(ca.crl()));crl.verify(p.root.certificate.getPublicKey());assertTrue(crl.isRevoked(cert));var reopened=new CertificateAuthority.CAStore(new Database(ca.db.path.toString()));assertNotNull(reopened.bySerial(cert.getSerialNumber().toString()).get("revoked_at"));
 }
 @Test void revokedClientCertificateIsRefusedInTheHandshake()throws Exception{
  // J4: with CRL_FILE set, a TLS endpoint refuses a peer of its CA whose certificate is on the CRL, at once after the CRL changes
  var p=new PKI(dir.resolve("pki"));config(p,p.issue("CertificateAuthority","sy"),"https://localhost:8444");var ca=new CertificateAuthority();close.add(ca);
  Path crlFile=dir.resolve("ca.crl");Files.write(crlFile,ca.crl());env("CRL_FILE",crlFile.toString());
  var server=server(p,p.issue("FixtureService","sy"),x->{x.sendResponseHeaders(200,-1);x.close();});close.add(()->server.stop(0));
  var good=p.issue("GoodClient","sy");var bad=p.issue("RevokedClient","sy");
  assertEquals(200,get(client(p,bad),endpoint(server)+"/",null).statusCode());
  ca.store.revokeContent(bad.certificate,"Sysop");Files.write(crlFile,ca.crl());
  Files.setLastModifiedTime(crlFile,java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()+2000));
  assertThrows(java.io.IOException.class,()->get(client(p,bad),endpoint(server)+"/",null));
  assertEquals(200,get(client(p,good),endpoint(server)+"/",null).statusCode());
 }
 @Test void crlAuthorityIdentifierRemainsCompatibleWithCrossSignedCa()throws Exception{var p=new PKI(dir.resolve("pki"));config(p,p.issue("CertificateAuthority","sy"),"https://localhost:8444");var ca=new CertificateAuthority();close.add(ca);var crl=(org.bouncycastle.cert.X509CRLHolder)CertificateAuthority.readPem(new String(ca.crl(),java.nio.charset.StandardCharsets.US_ASCII));var authority=AuthorityKeyIdentifier.getInstance(crl.getExtension(org.bouncycastle.asn1.x509.Extension.authorityKeyIdentifier).getParsedValue());assertNotNull(authority.getKeyIdentifier());assertNull(authority.getAuthorityCertIssuer());assertNull(authority.getAuthorityCertSerialNumber());}
 @Test void csrCannotEscalateReservedNameOrOperatorProfile()throws Exception{var p=new PKI(dir.resolve("pki"));config(p,p.issue("CertificateAuthority","sy"),"https://localhost:8444");var ca=new CertificateAuthority();close.add(ca);String reserved=csr(p,"Sysop","sy",null),operator=csr(p,"FreshDevice","op",null);assertEquals(403,assertThrows(Service.ApiError.class,()->ca.sign(m("csr",reserved),"Onboarding")).status);assertEquals(403,assertThrows(Service.ApiError.class,()->ca.sign(m("csr",operator),"Onboarding")).status);var issued=ca.sign(m("csr",operator),"Sysop");assertEquals("OPERATOR",issued.get("profile"));}
 @Test void csrExtensionsAreNotCopiedAsCaPrivileges()throws Exception{var p=new PKI(dir.resolve("pki"));config(p,p.issue("CertificateAuthority","sy"),"https://localhost:8444");var ca=new CertificateAuthority();close.add(ca);String request=csr(p,"FreshDevice","sy",null);assertThrows(IllegalArgumentException.class,()->ca.sign(m("csr",request,"addresses",List.of("bad/name")),"Onboarding"));assertThrows(IllegalArgumentException.class,()->ca.sign(m("csr",request,"validAfter",now(),"validBefore",Instant.now().plus(Duration.ofDays(500)).toString()),"Onboarding"));}
 @Test void independentSystemStartsWithOwnIdentityAndDatabase()throws Exception {
  var p=new PKI(dir.resolve("standalone-pki"));config(p,p.issue("CertificateAuthority","sy"),"https://127.0.0.1:1");
  env("SERVICE_REGISTRY_URL","https://127.0.0.1:1");env("FEDERATION_PROFILE","legacy-mtls");
  var system=new CertificateAuthority();close.add(system);system.start();
  assertEquals("CertificateAuthority",system.name);assertEquals("certificate-authority",system.role);
  var consumer=client(p,p.issue("AnySystem","sy"));close.add(consumer);
  var response=get(consumer,endpoint(system.server)+"/health",null);
  assertEquals(200,response.statusCode());assertTrue(java.nio.file.Files.exists(system.db.path));
 }
}
