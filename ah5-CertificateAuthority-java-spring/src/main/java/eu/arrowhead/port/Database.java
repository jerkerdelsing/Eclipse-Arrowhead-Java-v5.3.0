package eu.arrowhead.port;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static eu.arrowhead.port.Json.*;

final class Database {
 final Path path;
 Database(String file)throws Exception{path=Path.of(file).toAbsolutePath();Files.createDirectories(path.getParent());Class.forName("org.sqlite.JDBC");}
 Connection connect()throws SQLException{var c=DriverManager.getConnection("jdbc:sqlite:"+path);try(var s=c.createStatement()){s.execute("PRAGMA busy_timeout=10000");s.execute("PRAGMA foreign_keys=ON");}return c;}
 interface Work<T>{T run(Connection c)throws Exception;}
 synchronized <T>T transaction(Work<T>w)throws Exception{try(var c=connect()){c.setAutoCommit(false);try{T result=w.run(c);c.commit();return result;}catch(Exception e){c.rollback();throw e;}}}
 void schema(String...sql)throws Exception{try(var c=connect();var s=c.createStatement()){for(String q:sql)s.execute(q);}}
 static int update(Connection c,String sql,Object...params)throws SQLException{try(var s=c.prepareStatement(sql)){bind(s,params);return s.executeUpdate();}}
 int update(String sql,Object...params)throws Exception{return transaction(c->update(c,sql,params));}
 static void bind(PreparedStatement s,Object[]params)throws SQLException{for(int i=0;i<params.length;i++)s.setObject(i+1,params[i]);}
 static List<Map<String,Object>> query(Connection c,String sql,Object...params)throws SQLException{try(var s=c.prepareStatement(sql)){bind(s,params);try(var rs=s.executeQuery()){var list=new ArrayList<Map<String,Object>>();int n=rs.getMetaData().getColumnCount();while(rs.next()){var row=new LinkedHashMap<String,Object>();for(int i=1;i<=n;i++)row.put(rs.getMetaData().getColumnLabel(i),rs.getObject(i));list.add(row);}return list;}}}
 List<Map<String,Object>> query(String sql,Object...params)throws Exception{try(var c=connect()){return query(c,sql,params);}}
 static long inserted(Connection c)throws Exception{return ((Number)query(c,"SELECT last_insert_rowid() AS id").getFirst().get("id")).longValue();}
}
