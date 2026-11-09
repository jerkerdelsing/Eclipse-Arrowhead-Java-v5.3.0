package eu.arrowhead.onboarding;
import java.nio.file.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
class CertificateFixture {
    static final String PASSWORD = "test-only-password";
    final KeyPair rootKey = key(), orgKey = key(), cloudKey = key(), serverKey = key(), clientKey = key();
    final X509Certificate root = cert("CN=arrowhead.eu,DN=ma",rootKey,null,rootKey,2);
    final X509Certificate organization = cert("CN=Company.arrowhead.eu,DN=or",orgKey,root,rootKey,1);
    final X509Certificate cloud = cert("CN=TestCloud.Company.arrowhead.eu,DN=lo",cloudKey,organization,orgKey,0);
    final X509Certificate server = cert("CN=Onboarding.TestCloud.Company.arrowhead.eu,DN=sy",serverKey,cloud,cloudKey,-1);
    final X509Certificate client = cert("CN=Device1.TestCloud.Company.arrowhead.eu,DN=de",clientKey,cloud,cloudKey,-1);
    static KeyPair key() { try { var generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);return generator.generateKeyPair(); } catch(Exception e){throw new RuntimeException(e);} }
    static X509Certificate cert(String name, KeyPair key, X509Certificate issuer, KeyPair issuerKey, int pathLen) {
        try {
            var subject = new X500Name(name);
            var builder = new JcaX509v3CertificateBuilder(issuer == null ? subject : X500Name.getInstance(issuer.getSubjectX500Principal().getEncoded()),
                new java.math.BigInteger(120,new SecureRandom()), Date.from(Instant.now().minusSeconds(60)),
                Date.from(Instant.now().plusSeconds(86400)), subject,key.getPublic());
            builder.addExtension(Extension.basicConstraints,true,pathLen<0?new BasicConstraints(false):new BasicConstraints(pathLen));
            builder.addExtension(Extension.keyUsage,true,new KeyUsage(pathLen<0 ? KeyUsage.digitalSignature|KeyUsage.keyEncipherment : KeyUsage.keyCertSign|KeyUsage.cRLSign));
            builder.addExtension(Extension.subjectAlternativeName,false,new GeneralNames(new GeneralName(GeneralName.dNSName,"localhost")));
            if(pathLen<0)builder.addExtension(Extension.extendedKeyUsage,false,new ExtendedKeyUsage(new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth,KeyPurposeId.id_kp_serverAuth}));
            return new JcaX509CertificateConverter().getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(issuerKey.getPrivate())));
        }catch(Exception e){throw new RuntimeException(e);}
    }
    Path store(Path directory, String name, KeyPair key, X509Certificate... chain) throws Exception {
        var store=KeyStore.getInstance("PKCS12");store.load(null,null);
        store.setKeyEntry(name,key.getPrivate(),PASSWORD.toCharArray(),chain);
        Path path=directory.resolve(name+".p12");try(var out=Files.newOutputStream(path)){store.store(out,PASSWORD.toCharArray());}return path;
    }
    Path trust(Path directory) throws Exception {
        var store=KeyStore.getInstance("PKCS12");store.load(null,null);store.setCertificateEntry("root",root);
        Path path=directory.resolve("trust.p12");try(var out=Files.newOutputStream(path)){store.store(out,PASSWORD.toCharArray());}return path;
    }
    static String csr(String name, KeyPair key, KeyPair signer) throws Exception {
        var builder=new JcaPKCS10CertificationRequestBuilder(new X500Name(name),key.getPublic());
        // An attacker asks for a CA certificate; issuer must discard these requested extensions.
        var extensions=new ExtensionsGenerator();extensions.addExtension(Extension.basicConstraints,true,new BasicConstraints(true));
        builder.addAttribute(org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,extensions.generate());
        return Base64.getEncoder().encodeToString(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(signer.getPrivate())).getEncoded());
    }
}
