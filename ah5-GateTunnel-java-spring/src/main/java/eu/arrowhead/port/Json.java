package eu.arrowhead.port;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class Json {
 static final ObjectMapper MAPPER=new ObjectMapper();
 static Map<String,Object> m(Object...pairs){var m=new LinkedHashMap<String,Object>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
 @SuppressWarnings("unchecked") static Map<String,Object> obj(Object v){return v instanceof Map<?,?>?(Map<String,Object>)v:new LinkedHashMap<>();}
 @SuppressWarnings("unchecked") static List<Object> list(Object v){return v instanceof List<?>?(List<Object>)v:List.of();}
 static String str(Object v){return v==null?"":v.toString();}
 static int integer(Object v,int fallback){try{return Integer.parseInt(str(v));}catch(Exception e){return fallback;}}
 static double number(Object v){return v instanceof Number n?n.doubleValue():Double.parseDouble(str(v));}
 static List<String> strings(Object v){return list(v).stream().map(Json::str).toList();}
 static Object parse(String text){try{return MAPPER.readValue(text,Object.class);}catch(Exception e){throw new IllegalArgumentException("Invalid JSON");}}
 static Object parse(byte[] data){return parse(new String(data,StandardCharsets.UTF_8));}
 static String json(Object value){try{return MAPPER.writeValueAsString(value);}catch(Exception e){throw new IllegalArgumentException("JSON serialization failed",e);}}
 static byte[] bytes(Object value){return json(value).getBytes(StandardCharsets.UTF_8);}
 // KEY, else the content of the file named by KEY_FILE (Compose secrets under /run/secrets, security finding J7), else fallback
 static String env(String key,String fallback){String v=System.getProperty("arrowhead.env."+key,System.getenv(key));if(v!=null)return v;String file=key.endsWith("_FILE")?null:env(key+"_FILE",null);return file==null||file.isBlank()?fallback:secretFile(key,file);}
 static String secretFile(String key,String file){try{return Files.readString(Path.of(file)).replaceAll("[\\r\\n]+$","");}catch(java.io.IOException e){throw new IllegalStateException(key+"_FILE is not readable: "+file);}}
 static int envInt(String key,int fallback){return integer(env(key,""),fallback);}
 static Set<String> csv(String value){var s=new TreeSet<String>();for(String x:value.split(","))if(!x.isBlank())s.add(x.trim());return s;}
 static String now(){return java.time.Instant.now().toString();}
 static String id(){return UUID.randomUUID().toString();}
 static String cn(java.security.cert.X509Certificate c){return name(c,"CN");}
 static String name(java.security.cert.X509Certificate c,String attribute){try{for(var r:new javax.naming.ldap.LdapName(c.getSubjectX500Principal().getName()).getRdns())if(r.getType().equalsIgnoreCase(attribute)||attribute.equals("DNQ")&&r.getType().equals("2.5.4.46")){Object v=r.getValue();if(v instanceof byte[]b)return org.bouncycastle.asn1.ASN1String.class.cast(org.bouncycastle.asn1.ASN1Primitive.fromByteArray(b)).getString();return str(v);}}catch(Exception ignored){}return "";}
}
