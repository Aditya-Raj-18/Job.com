package com.jobcom.dao;
import com.jobcom.config.Db;
import java.sql.*;
import java.util.*;
public class AdminDAO {
 public Map<String,Object> authenticate(String email)throws SQLException{
  try(Connection c=Db.getConnection()){
   Set<String> columns=columns(c,"admins");
   String idColumn=columns.contains("id")?"id":"admin_id";
   String nameColumn=columns.contains("display_name")?"display_name":"name";
   String sql="SELECT "+idColumn+" AS admin_key,email,password_hash,"+nameColumn+" AS admin_name FROM admins WHERE email=?"+(columns.contains("status")?" AND status='ACTIVE'":"");
   try(PreparedStatement p=c.prepareStatement(sql)){
    p.setString(1,email);try(ResultSet r=p.executeQuery()){if(!r.next())return null;Map<String,Object>m=new HashMap<>();m.put("id",r.getLong("admin_key"));m.put("email",r.getString("email"));m.put("hash",r.getString("password_hash"));m.put("name",r.getString("admin_name"));return m;}
   }
  }
 }
 public void bootstrap(String email,String hash)throws SQLException{
  try(Connection c=Db.getConnection()){
   Set<String> columns=columns(c,"admins");
   List<String> names=new ArrayList<>(List.of("email","password_hash",columns.contains("display_name")?"display_name":"name"));
   List<String> values=new ArrayList<>(List.of("?","?","?"));
   if(columns.contains("role")){names.add("role");values.add("?");}
   if(columns.contains("status")){names.add("status");values.add("?");}
   // Seed the administrator password once. Later recovery/password changes must survive Tomcat restarts.
   String sql="INSERT INTO admins("+String.join(",",names)+") VALUES("+String.join(",",values)+") ON DUPLICATE KEY UPDATE email=VALUES(email)";
   try(PreparedStatement p=c.prepareStatement(sql)){
    int i=1;p.setString(i++,email);p.setString(i++,hash);p.setString(i++,"Administrator");
    if(columns.contains("role"))p.setString(i++,"SUPER_ADMIN");
    if(columns.contains("status"))p.setString(i,"ACTIVE");
    p.executeUpdate();
   }
  }
 }
 private Set<String> columns(Connection c,String table)throws SQLException{
  Set<String> names=new HashSet<>();
  try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,table,null)){while(r.next())names.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));}
  return names;
 }
 private String column(Set<String> columns,String... candidates){for(String candidate:candidates)if(columns.contains(candidate.toLowerCase(Locale.ROOT)))return "`"+candidate+"`";return null;}
 private String required(Set<String> columns,String table,String... candidates)throws SQLException{String value=column(columns,candidates);if(value==null)throw new SQLException("Table "+table+" is missing expected columns: "+String.join(", ",candidates));return value;}
 private String optional(Set<String> columns,String alias,String... candidates){String value=column(columns,candidates);return value==null?"'' AS `"+alias+"`":value+" AS `"+alias+"`";}
 private String optionalTime(Set<String> columns){String value=column(columns,"created_at","created_on","registered_at","registration_date","date_created");return value==null?"NULL AS `created_at`":value+" AS `created_at`";}
 public Map<String,Object> stats()throws SQLException{
  Map<String,Object>m=new LinkedHashMap<>();try(Connection c=Db.getConnection();Statement s=c.createStatement()){
   m.put("Job seekers",count(s,"users",null));m.put("Pending seekers",countByStatus(s,c,"users","PENDING"));m.put("Recruiters",count(s,"companies",null));m.put("Pending recruiters",countByStatus(s,c,"companies","PENDING"));m.put("Job listings",count(s,"jobs",null));m.put("Pending jobs",countByStatus(s,c,"jobs","PENDING"));m.put("Applications",count(s,"applications",null));return m;
  }
 }
 private long countByStatus(Statement s,Connection c,String table,String wanted)throws SQLException{Set<String> cols=columns(c,table);String status=column(cols,"status","account_status","approval_status");return status==null?0:count(s,table,status+"='"+wanted+"'");}
 private long count(Statement s,String table,String where)throws SQLException{String q="SELECT COUNT(*) FROM "+table+(where==null?"":" WHERE "+where);try(ResultSet r=s.executeQuery(q)){r.next();return r.getLong(1);}}
 public List<Map<String,Object>> list(String type,String q)throws SQLException{return list(type,q,null);}
 public List<Map<String,Object>> list(String type,String q,String accountFilter)throws SQLException{
  if("notifications".equals(type))return new NotificationDAO().adminList();
  try(Connection c=Db.getConnection()){
   if("dashboard".equals(type))return List.of();
   String table=switch(type){case "users"->"users";case "companies"->"companies";case "jobs"->"jobs";case "catalog"->"fields";case "practice"->"practice_sets";case "notifications"->"notifications";case "reports"->"audit_logs";default->"practice_sets";};
   Set<String> cols=columns(c,table);String[] idCandidates=switch(type){case "users"->new String[]{"id","user_id"};case "companies"->new String[]{"id","company_id"};case "jobs"->new String[]{"id","job_id"};case "catalog"->new String[]{"id","field_id","category_id"};case "practice"->new String[]{"id","practice_id","set_id","practice_set_id"};case "notifications"->new String[]{"id","message_id","notification_id"};case "reports"->new String[]{"id","audit_id","log_id"};default->new String[]{"id","practice_id","set_id","practice_set_id"};};String id=required(cols,table,idCandidates);
   String name=switch(type){case "users"->required(cols,table,"full_name","name","display_name","username");case "companies"->required(cols,table,"name","company_name","display_name");case "jobs"->required(cols,table,"title","job_title","position","name");case "catalog"->required(cols,table,"name","field_name","category_name");case "practice"->required(cols,table,"title","name");case "notifications"->required(cols,table,"subject","title","message");case "reports"->required(cols,table,"action","event","activity");default->required(cols,table,"title","name");};
   String email=type.equals("jobs")?jobCompanyExpression(c,cols):optional(cols,"email","email","url","audience","entity_type","company_name");
   String status=optional(cols,"status","status","account_status","approval_status","active");String created=optionalTime(cols);
    String sql="SELECT *"+(type.equals("jobs")?","+jobCompanyExpression(c,cols):"")+" FROM `"+table+"`";
   List<String> searchColumns=new ArrayList<>();for(String candidate:switch(type){case "users"->new String[]{"full_name","name","display_name","username","email"};case "companies"->new String[]{"name","company_name","email"};case "jobs"->new String[]{"title","job_title","position"};default->new String[]{};}){String col=column(cols,candidate);if(col!=null&&!searchColumns.contains(col))searchColumns.add(col);}
   String term=q==null?"":q.trim();List<Object> params=new ArrayList<>();List<String> conditions=new ArrayList<>();
   if(accountFilter!=null&&(type.equals("users")||type.equals("companies"))){String statusColumn=column(cols,"status","account_status","approval_status");if(statusColumn!=null){if(accountFilter.equals("pending"))conditions.add(statusColumn+"='PENDING'");else if(accountFilter.equals("approved"))conditions.add(statusColumn+" IN ('ACTIVE','APPROVED','BLOCKED')");}}
   if(!searchColumns.isEmpty()&&!term.isEmpty()){conditions.add("("+String.join(" OR ",searchColumns.stream().map(col->col+" LIKE ?").toList())+")");for(int i=0;i<searchColumns.size();i++)params.add("%"+term+"%");}
   if(!conditions.isEmpty())sql+=" WHERE "+String.join(" AND ",conditions);
   sql+=" ORDER BY "+id+" DESC LIMIT "+("notifications".equals(type)?100:200);
    try(PreparedStatement p=c.prepareStatement(sql)){for(int i=0;i<params.size();i++)p.setString(i+1,(String)params.get(i));try(ResultSet r=p.executeQuery()){List<Map<String,Object>>out=new ArrayList<>();ResultSetMetaData md=r.getMetaData();while(r.next()){Map<String,Object>x=new LinkedHashMap<>();Map<String,Object>details=new LinkedHashMap<>();Map<String,Object>documents=new LinkedHashMap<>();for(int i=1;i<=md.getColumnCount();i++){String key=md.getColumnLabel(i).toLowerCase(Locale.ROOT);Object value=r.getObject(i);if(!hiddenFromAdminDetails(key)&&!isBinary(md.getColumnType(i))&&!isCoreColumn(key,type)){if(isDocumentPath(key)&&value!=null&&!value.toString().isBlank()){if(isSafeDocumentReference(value.toString()))documents.put(labelFor(key),value.toString());else details.put(labelFor(key),"Submitted document recorded");}else details.put(labelFor(key),displayValue(key,value));}if(isIdColumn(key,type)&&x.get("id")==null)x.put("id",value);if(isNameColumn(key,type))x.put("name",value);if(isEmailColumn(key,type))x.put("email",value);if(isStatusColumn(key)&&x.get("status")==null)x.put("status",value);if(isCreatedColumn(key))x.put("created_at",value);}if(type.equals("users")&&x.get("id")!=null){String resume=latestResume(c,cols,x.get("id"));if(resume!=null&&!resume.isBlank()){if(isSafeDocumentReference(resume))documents.put("Resume",resume);else details.put("Resume","Submitted document recorded");}}x.put("details",details);x.put("documents",documents);out.add(x);}return out;}}
  }
 }
 private boolean isIdColumn(String key,String type){return key.equals("id")||key.equals(switch(type){case "users"->"user_id";case "companies"->"company_id";case "jobs"->"job_id";case "notifications"->"message_id";case "reports"->"audit_id";case "catalog"->"field_id";case "practice"->"practice_id";default->"id";});}
 private boolean hiddenFromAdminDetails(String key){return key.contains("password")||key.contains("secret")||key.contains("token")||key.equals("salt")||key.startsWith("reset_")||key.contains("session");}
 private boolean isDocumentPath(String key){return key.endsWith("_document_path")||key.equals("resume_path")||key.endsWith("_file_path")||key.endsWith("_proof_path");}
 private boolean isSafeDocumentReference(String path){return path.matches("(ids|licenses|resumes)/[a-fA-F0-9-]{36}\\.(pdf|png|jpe?g)");}
 private String latestResume(Connection c,Set<String> userColumns,Object id)throws SQLException{Set<String> resumeCols=columns(c,"resumes");String userId=column(resumeCols,"user_id","candidate_id");String path=column(resumeCols,"file_path","resume_path","document_path");String created=column(resumeCols,"created_at","created_on");if(userId==null||path==null||column(userColumns,"id","user_id")==null)return null;String sql="SELECT "+path+" FROM resumes WHERE "+userId+"=?"+(created==null?"":" ORDER BY "+created+" DESC")+" LIMIT 1";try(PreparedStatement p=c.prepareStatement(sql)){p.setObject(1,id);try(ResultSet r=p.executeQuery()){return r.next()?r.getString(1):null;}}}
 private boolean isBinary(int type){return type==Types.BINARY||type==Types.VARBINARY||type==Types.LONGVARBINARY||type==Types.BLOB;}
 private boolean isCoreColumn(String key,String type){return isIdColumn(key,type)||isNameColumn(key,type)||isEmailColumn(key,type)||isStatusColumn(key)||isCreatedColumn(key);}
 private boolean isNameColumn(String key,String type){return switch(type){case "users"->Set.of("full_name","name","display_name","username").contains(key);case "companies"->Set.of("name","company_name","display_name").contains(key);case "jobs"->Set.of("title","job_title","position").contains(key);case "catalog"->Set.of("name","field_name","category_name").contains(key);case "practice"->Set.of("title","name").contains(key);case "notifications"->Set.of("subject","title","message").contains(key);case "reports"->Set.of("action","event","activity").contains(key);default->false;};}
 private boolean isEmailColumn(String key,String type){return type.equals("jobs")?Set.of("company_name","employer_name","email").contains(key):Set.of("email","url","audience","entity_type").contains(key);}
 private boolean isStatusColumn(String key){return Set.of("status","account_status","approval_status","active").contains(key);}
 private boolean isCreatedColumn(String key){return Set.of("created_at","created_on","registered_at","registration_date","date_created","applied_at").contains(key);}
 private Object displayValue(String key,Object value){if(value==null)return "—";if(key.endsWith("_last4"))return "•••• "+value;if(key.contains("gov_id")||key.contains("government_id")||key.contains("aadhaar")||key.contains("aadhar")||key.contains("identity_number"))return maskIdentity(value.toString());return value;}
 private String maskIdentity(String value){String compact=value.replaceAll("\\s+","");return compact.length()<=4?"••••":"•••• •••• "+compact.substring(compact.length()-4);}
 private String labelFor(String key){String cleaned=key.replace('_',' ').trim();StringBuilder out=new StringBuilder();for(String part:cleaned.split("\\s+")){if(!out.isEmpty())out.append(' ');out.append(part.substring(0,1).toUpperCase(Locale.ROOT)).append(part.substring(1));}return out.toString();}
 private String jobCompanyExpression(Connection c,Set<String> jobCols)throws SQLException{
  String direct=column(jobCols,"company_name","employer_name");if(direct!=null)return direct+" AS `email`";
  String fk=column(jobCols,"company_id","employer_id");if(fk==null)return "'' AS `email`";
  Set<String> companyCols=columns(c,"companies");String companyId=column(companyCols,"id","company_id");String companyName=column(companyCols,"name","company_name","display_name");
  if(companyId==null||companyName==null)return "'' AS `email`";
  return "(SELECT c."+companyName+" FROM `companies` c WHERE c."+companyId+"=jobs."+fk+" LIMIT 1) AS `email`";
 }
 public void changeStatus(String type,long id,String action,long adminId)throws SQLException{
  String table,status;
  switch(type){
   case "users"->{table="users";status=map(action,Map.of("approve","ACTIVE","reject","REJECTED","block","BLOCKED","unblock","ACTIVE"));}
   case "companies"->{table="companies";status=map(action,Map.of("approve","APPROVED","reject","REJECTED","block","BLOCKED","unblock","APPROVED"));}
   case "jobs"->{table="jobs";status=map(action,Map.of("verify","VERIFIED","reject","REJECTED","delete","DELETED"));}
   default->throw new IllegalArgumentException("Unknown type");
  }
   try(Connection c=Db.getConnection()){Set<String> cols=columns(c,table);String idColumn=required(cols,table,"id",type.equals("users")?"user_id":type.equals("companies")?"company_id":"job_id");String statusColumn=required(cols,table,"status","account_status","approval_status");String verificationColumn=(type.equals("users")||type.equals("companies"))?column(cols,"id_verification_status","verification_status"):null;String verification=action.equals("approve")?"APPROVED":action.equals("reject")?"REJECTED":null;String sql="UPDATE `"+table+"` SET "+statusColumn+"=?"+(verificationColumn!=null&&verification!=null?","+verificationColumn+"=?":"")+" WHERE "+idColumn+"=?";try(PreparedStatement p=c.prepareStatement(sql)){int i=1;p.setString(i++,status);if(verificationColumn!=null&&verification!=null)p.setString(i++,verification);p.setLong(i,id);p.executeUpdate();}}
  audit(adminId,action,type,id);
 }
 private String map(String a,Map<String,String>m){String v=m.get(a);if(v==null)throw new IllegalArgumentException("Action not allowed");return v;}
 public void addCatalog(String name)throws SQLException{try(Connection c=Db.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO fields(name) VALUES(?)")){p.setString(1,name);p.executeUpdate();}}
 public void addPractice(String title,String url)throws SQLException{try(Connection c=Db.getConnection()){Set<String> cols=columns(c,"practice_sets");String titleCol=required(cols,"practice_sets","title");String urlCol=required(cols,"practice_sets","content_url","url");try(PreparedStatement p=c.prepareStatement("INSERT INTO practice_sets("+titleCol+","+urlCol+") VALUES(?,?)")){p.setString(1,title);p.setString(2,url);p.executeUpdate();}}}
 public void announce(String audience,String subject,String body,long admin)throws SQLException{new NotificationDAO().announce(audience,subject,body,admin);}
 private void audit(long admin,String action,String type,long id)throws SQLException{try(Connection c=Db.getConnection()){Set<String> cols=columns(c,"audit_logs");String adminColumn=column(cols,"admin_id","adminId");String entityColumn=column(cols,"entity_id","record_id","target_id");String typeColumn=column(cols,"entity_type","record_type","target_type");String actionColumn=required(cols,"audit_logs","action","event");List<String> names=new ArrayList<>();List<String> values=new ArrayList<>();if(adminColumn!=null){names.add(adminColumn);values.add("?");}names.add(actionColumn);values.add("?");if(typeColumn!=null){names.add(typeColumn);values.add("?");}if(entityColumn!=null){names.add(entityColumn);values.add("?");}try(PreparedStatement p=c.prepareStatement("INSERT INTO `audit_logs`("+String.join(",",names)+") VALUES("+String.join(",",values)+")")){int i=1;if(adminColumn!=null)p.setLong(i++,admin);p.setString(i++,action);if(typeColumn!=null)p.setString(i++,type);if(entityColumn!=null)p.setLong(i,id);p.executeUpdate();}}}
}

