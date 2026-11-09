package com.jobcom.dao;

import com.jobcom.config.Db;
import java.sql.*;
import java.util.*;

/** Small schema-owned store for admin supplied ATS templates and seeker resume drafts. */
public final class ResumeDAO {
  public void ensure() throws SQLException {
    try(Connection c=Db.getConnection(); Statement s=c.createStatement()) {
      s.executeUpdate("CREATE TABLE IF NOT EXISTS resume_templates (template_id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(140) NOT NULL,description VARCHAR(500) NOT NULL DEFAULT '',layout VARCHAR(40) NOT NULL DEFAULT 'classic',file_path VARCHAR(500) NULL,active TINYINT(1) NOT NULL DEFAULT 1,uploaded_by BIGINT NULL,created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
      s.executeUpdate("CREATE TABLE IF NOT EXISTS resume_drafts (draft_id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,template_id BIGINT NOT NULL,title VARCHAR(180) NOT NULL,resume_data LONGTEXT NOT NULL,updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,UNIQUE KEY uq_resume_user_title(user_id,title),INDEX(user_id)) ENGINE=InnoDB");
      // Keep a few ready-to-use formats available even before an administrator
      // publishes custom templates. The name check makes this safe on every request.
      s.executeUpdate("INSERT INTO resume_templates(name,description,layout) SELECT 'Classic ATS','A clean, chronological format designed for applicant tracking systems.','classic' WHERE NOT EXISTS (SELECT 1 FROM resume_templates WHERE name='Classic ATS')");
      s.executeUpdate("INSERT INTO resume_templates(name,description,layout) SELECT 'Modern Professional','A polished blue-accented layout for technical and business roles.','modern' WHERE NOT EXISTS (SELECT 1 FROM resume_templates WHERE name='Modern Professional')");
      s.executeUpdate("INSERT INTO resume_templates(name,description,layout) SELECT 'Compact One Page','A space-efficient format for students and early-career applicants.','compact' WHERE NOT EXISTS (SELECT 1 FROM resume_templates WHERE name='Compact One Page')");
      s.executeUpdate("INSERT INTO resume_templates(name,description,layout) SELECT 'Creative Coral','A bold coral-accented design for creative portfolios and product roles.','creative' WHERE NOT EXISTS (SELECT 1 FROM resume_templates WHERE name='Creative Coral')");
      s.executeUpdate("INSERT INTO resume_templates(name,description,layout) SELECT 'Ocean Blue','A calm, colorful layout with clear sections for technical and research roles.','ocean' WHERE NOT EXISTS (SELECT 1 FROM resume_templates WHERE name='Ocean Blue')");
      s.executeUpdate("INSERT INTO resume_templates(name,description,layout) SELECT 'Sunset Gradient','A warm, energetic design for modern marketing and design applications.','sunset' WHERE NOT EXISTS (SELECT 1 FROM resume_templates WHERE name='Sunset Gradient')");
      s.executeUpdate("INSERT INTO resume_templates(name,description,layout) SELECT 'Violet Profile','A violet sidebar layout that includes your saved profile photo.','photo-violet' WHERE NOT EXISTS (SELECT 1 FROM resume_templates WHERE name='Violet Profile')");
      s.executeUpdate("INSERT INTO resume_templates(name,description,layout) SELECT 'Emerald Profile','A fresh emerald layout with your saved profile photo and a spacious career summary.','photo-emerald' WHERE NOT EXISTS (SELECT 1 FROM resume_templates WHERE name='Emerald Profile')");
    }
  }
  public Map<String,Object> authenticate(String email) throws SQLException {
    try(Connection c=Db.getConnection()) { Set<String> cols=columns(c,"users"); String id=first(cols,"user_id","id"), name=first(cols,"full_name","name"), hash=first(cols,"password_hash","password"), status=first(cols,"status","account_status");
      if(id==null||hash==null)throw new SQLException("User account columns are missing");
      String sql="SELECT `"+id+"` AS uid,"+(name==null?"''":"`"+name+"`")+" AS uname,`"+hash+"` AS passhash"+(status==null?"":" ,`"+status+"` AS account_status")+" FROM users WHERE email=? LIMIT 1";
      try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,email);try(ResultSet r=p.executeQuery()){if(!r.next())return null;Map<String,Object> m=new HashMap<>();m.put("id",r.getLong("uid"));m.put("name",r.getString("uname"));m.put("hash",r.getString("passhash"));m.put("status",status==null?"ACTIVE":r.getString("account_status"));return m;}}
    }
  }
  public Map<String,String> profile(long userId) throws SQLException {
    Map<String,String> out=new LinkedHashMap<>();try(Connection c=Db.getConnection()){Set<String> cols=columns(c,"users");String id=first(cols,"user_id","id");if(id==null)return out;
      String[] keys={"full_name","name","email","phone","address","city","state","postal_code","profile_headline","profile_summary","skills","qualification","career_field","experience_years","linkedin_url","portfolio_url","profile_photo_path","profile_picture","avatar_path"};List<String> found=new ArrayList<>();for(String k:keys)if(cols.contains(k))found.add(k);if(found.isEmpty())return out;
      try(PreparedStatement p=c.prepareStatement("SELECT "+String.join(",",found)+" FROM users WHERE `"+id+"`=?")){p.setLong(1,userId);try(ResultSet r=p.executeQuery()){if(r.next())for(String k:found)out.put(k,r.getString(k)==null?"":r.getString(k));}}
      // Normalize legacy schemas that store the seeker name in `name` while
      // the profile and resume views use the canonical `full_name` key.
      if(!out.containsKey("full_name")||out.get("full_name").isBlank())out.put("full_name",out.getOrDefault("name",""));
      if(!out.containsKey("name")||out.get("name").isBlank())out.put("name",out.getOrDefault("full_name",""));
    }return out;
  }
  public void updateProfilePhoto(long userId,String path)throws SQLException{
    try(Connection c=Db.getConnection()){
      Set<String> cols=columns(c,"users");String photo=first(cols,"profile_photo_path","profile_picture","avatar_path");String id=first(cols,"id","user_id");
      if(photo==null||id==null)throw new SQLException("The user profile photo column is not available.");
      try(PreparedStatement p=c.prepareStatement("UPDATE users SET `"+photo+"`=? WHERE `"+id+"`=?")){p.setString(1,path);p.setLong(2,userId);if(p.executeUpdate()!=1)throw new SQLException("Job seeker profile was not found.");}
    }
  }
  public List<Map<String,Object>> templates(boolean activeOnly) throws SQLException {ensure();List<Map<String,Object>> out=new ArrayList<>();try(Connection c=Db.getConnection();PreparedStatement p=c.prepareStatement("SELECT template_id,name,description,layout,file_path,active FROM resume_templates"+(activeOnly?" WHERE active=1":"")+" ORDER BY created_at DESC" );ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object>m=new LinkedHashMap<>();for(String k:List.of("template_id","name","description","layout","file_path","active"))m.put(k,r.getObject(k));out.add(m);}}return out;}
  public void addTemplate(String name,String description,String layout,String path,long adminId)throws SQLException {ensure();try(Connection c=Db.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO resume_templates(name,description,layout,file_path,uploaded_by) VALUES(?,?,?,?,?)")){p.setString(1,name);p.setString(2,description);p.setString(3,layout);p.setString(4,path);p.setLong(5,adminId);p.executeUpdate();}}
  public void setTemplateActive(long id,boolean active)throws SQLException {ensure();try(Connection c=Db.getConnection();PreparedStatement p=c.prepareStatement("UPDATE resume_templates SET active=? WHERE template_id=?")){p.setBoolean(1,active);p.setLong(2,id);p.executeUpdate();}}
  public String templateFile(long id)throws SQLException {ensure();try(Connection c=Db.getConnection();PreparedStatement p=c.prepareStatement("SELECT file_path FROM resume_templates WHERE template_id=? AND active=1")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){return r.next()?r.getString(1):null;}}}
  public Map<String,String> draft(long userId,long templateId)throws SQLException {ensure();try(Connection c=Db.getConnection();PreparedStatement p=c.prepareStatement("SELECT title,resume_data FROM resume_drafts WHERE user_id=? AND template_id=? ORDER BY updated_at DESC LIMIT 1")){p.setLong(1,userId);p.setLong(2,templateId);try(ResultSet r=p.executeQuery()){if(!r.next())return Map.of();Map<String,String> m=decode(r.getString("resume_data"));m.put("title",r.getString("title"));return m;}}}
  public void saveDraft(long userId,long templateId,String title,Map<String,String> data)throws SQLException {ensure();try(Connection c=Db.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO resume_drafts(user_id,template_id,title,resume_data) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE template_id=VALUES(template_id),resume_data=VALUES(resume_data),updated_at=CURRENT_TIMESTAMP")){p.setLong(1,userId);p.setLong(2,templateId);p.setString(3,title);p.setString(4,encode(data));p.executeUpdate();}}
  private String encode(Map<String,String> d){StringJoiner j=new StringJoiner("&");d.forEach((k,v)->j.add(java.net.URLEncoder.encode(k,java.nio.charset.StandardCharsets.UTF_8)+"="+java.net.URLEncoder.encode(v==null?"":v,java.nio.charset.StandardCharsets.UTF_8)));return j.toString();}
  private Map<String,String> decode(String v){Map<String,String> m=new LinkedHashMap<>();if(v!=null)for(String pair:v.split("&")){int i=pair.indexOf('=');if(i>0)m.put(java.net.URLDecoder.decode(pair.substring(0,i),java.nio.charset.StandardCharsets.UTF_8),java.net.URLDecoder.decode(pair.substring(i+1),java.nio.charset.StandardCharsets.UTF_8));}return m;}
  private Set<String> columns(Connection c,String table)throws SQLException{Set<String>s=new HashSet<>();try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,table,null)){while(r.next())s.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));}return s;}
  private String first(Set<String>s,String...names){for(String n:names)if(s.contains(n))return n;return null;}
}
