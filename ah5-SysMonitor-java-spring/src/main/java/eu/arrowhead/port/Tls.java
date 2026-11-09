package eu.arrowhead.port;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.security.*;
import java.security.cert.*;
import java.util.*;
import javax.net.ssl.*;
import java.net.Socket;
import org.bouncycastle.openssl.*;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import static eu.arrowhead.port.Json.*;

final class Tls {
 static {Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());}
 static PrivateKey key(Path file)throws Exception{try(var p=new PEMParser(Files.newBufferedReader(file))){Object v=p.readObject();var converter=new JcaPEMKeyConverter().setProvider("BC");if(v instanceof PEMKeyPair kp)return converter.getPrivateKey(kp.getPrivateKeyInfo());if(v instanceof PrivateKeyInfo k)return converter.getPrivateKey(k);throw new IllegalArgumentException("Unsupported private key encoding");}}
 static X509Certificate cert(Path file)throws Exception{try(var in=Files.newInputStream(file)){return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(in);}}
 static SSLContext context(String certFile,String keyFile,String caFile,String pinnedCN)throws Exception{
  var factory=CertificateFactory.getInstance("X.509");var keys=KeyStore.getInstance("PKCS12");keys.load(null,null);java.security.cert.Certificate[]chain;
  try(var in=Files.newInputStream(Path.of(certFile))){chain=factory.generateCertificates(in).toArray(java.security.cert.Certificate[]::new);}
  keys.setKeyEntry("identity",key(Path.of(keyFile)),new char[0],chain);var km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(keys,new char[0]);
  var roots=KeyStore.getInstance(KeyStore.getDefaultType());roots.load(null,null);try(var in=Files.newInputStream(Path.of(caFile))){int i=0;for(var c:factory.generateCertificates(in))roots.setCertificateEntry("ca"+i++,c);}
  var tm=TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());tm.init(roots);TrustManager[]trust=tm.getTrustManagers();
  var base=(X509ExtendedTrustManager)Arrays.stream(trust).filter(t->t instanceof X509ExtendedTrustManager).findFirst().orElseThrow();
  // Revocation (J4): peers issued by the CA that signs CRL_FILE are checked against that CRL on every handshake
  String crlFile=env("CRL_FILE","");if(!crlFile.isEmpty())base=new Revoking(base,Crl.of(Path.of(crlFile),roots));
  trust=new TrustManager[]{pinnedCN!=null?new Pinned(base,pinnedCN):base};
  var ctx=SSLContext.getInstance("TLS");ctx.init(km.getKeyManagers(),trust,new SecureRandom());return ctx;
 }
 static final class Pinned extends X509ExtendedTrustManager{
  final X509ExtendedTrustManager base;final String expected;Pinned(X509ExtendedTrustManager b,String n){base=b;expected=n;}
  void pin(X509Certificate[]c)throws CertificateException{if(c.length==0||!cn(c[0]).equals(expected)||!name(c[0],"DNQ").equals("sy"))throw new CertificateException("Unexpected Authentication identity");}
  public void checkClientTrusted(X509Certificate[]c,String a)throws CertificateException{base.checkClientTrusted(c,a);}
  public void checkClientTrusted(X509Certificate[]c,String a,Socket s)throws CertificateException{base.checkClientTrusted(c,a,s);}
  public void checkClientTrusted(X509Certificate[]c,String a,SSLEngine e)throws CertificateException{base.checkClientTrusted(c,a,e);}
  public void checkServerTrusted(X509Certificate[]c,String a)throws CertificateException{base.checkServerTrusted(c,a);pin(c);}
  public void checkServerTrusted(X509Certificate[]c,String a,Socket s)throws CertificateException{base.checkServerTrusted(c,a,s);pin(c);}
  public void checkServerTrusted(X509Certificate[]c,String a,SSLEngine e)throws CertificateException{base.checkServerTrusted(c,a,e);pin(c);}
  public X509Certificate[]getAcceptedIssuers(){return base.getAcceptedIssuers();}
 }

 // CRL of the cloud CA, re-read when the file changes. Fail closed for certificates of that CA: no readable CRL, a CRL
 // with a bad signature or past its nextUpdate refuses them (the brokers behave the same). Other issuers are not checked.
 static final class Crl{
  static final Map<Path,Crl>CACHE=new HashMap<>();
  final Path file;final List<X509Certificate>issuers=new ArrayList<>();X509CRL crl;FileTime loaded;String problem="no CRL loaded";
  Crl(Path f,KeyStore roots)throws Exception{file=f;for(var a:Collections.list(roots.aliases()))if(roots.getCertificate(a)instanceof X509Certificate c)issuers.add(c);}
  static synchronized Crl of(Path f,KeyStore roots)throws Exception{var c=CACHE.get(f);if(c==null){c=new Crl(f,roots);CACHE.put(f,c);}return c;}
  synchronized void refresh(){
   try{var t=Files.getLastModifiedTime(file);if(t.equals(loaded))return;
    X509CRL next;try(var in=Files.newInputStream(file)){next=(X509CRL)CertificateFactory.getInstance("X.509").generateCRL(in);}
    X509Certificate issuer=issuers.stream().filter(c->c.getSubjectX500Principal().equals(next.getIssuerX500Principal())).findFirst()
      .orElseThrow(()->new CRLException("CRL issuer is not a trusted CA"));
    next.verify(issuer.getPublicKey());crl=next;loaded=t;problem=null;
   }catch(Exception e){problem="CRL "+file+" unusable: "+e.getMessage();System.err.println("revocation: "+problem);}
  }
  synchronized void check(X509Certificate leaf)throws CertificateException{
   boolean ours=issuers.stream().anyMatch(c->c.getSubjectX500Principal().equals(leaf.getIssuerX500Principal()));
   refresh();if(crl==null){if(ours)throw new CertificateException("Revocation status unknown: "+problem);return;}
   if(!crl.getIssuerX500Principal().equals(leaf.getIssuerX500Principal()))return;
   if(crl.getNextUpdate()!=null&&crl.getNextUpdate().before(new Date()))throw new CertificateException("CRL expired at "+crl.getNextUpdate().toInstant());
   if(crl.isRevoked(leaf))throw new CertificateException("Certificate "+leaf.getSerialNumber().toString(16)+" is revoked");
  }
 }
 static final class Revoking extends X509ExtendedTrustManager{
  final X509ExtendedTrustManager base;final Crl crl;Revoking(X509ExtendedTrustManager b,Crl c){base=b;crl=c;}
  void revocation(X509Certificate[]c)throws CertificateException{if(c.length>0)crl.check(c[0]);}
  public void checkClientTrusted(X509Certificate[]c,String a)throws CertificateException{base.checkClientTrusted(c,a);revocation(c);}
  public void checkClientTrusted(X509Certificate[]c,String a,Socket s)throws CertificateException{base.checkClientTrusted(c,a,s);revocation(c);}
  public void checkClientTrusted(X509Certificate[]c,String a,SSLEngine e)throws CertificateException{base.checkClientTrusted(c,a,e);revocation(c);}
  public void checkServerTrusted(X509Certificate[]c,String a)throws CertificateException{base.checkServerTrusted(c,a);revocation(c);}
  public void checkServerTrusted(X509Certificate[]c,String a,Socket s)throws CertificateException{base.checkServerTrusted(c,a,s);revocation(c);}
  public void checkServerTrusted(X509Certificate[]c,String a,SSLEngine e)throws CertificateException{base.checkServerTrusted(c,a,e);revocation(c);}
  public X509Certificate[]getAcceptedIssuers(){return base.getAcceptedIssuers();}
 }
}
