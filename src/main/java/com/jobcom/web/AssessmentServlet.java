package com.jobcom.web;

import com.jobcom.config.Db;
import com.jobcom.dao.AssessmentResourceSchema;
import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

@MultipartConfig(maxFileSize=5242880,maxRequestSize=6291456)
public class AssessmentServlet extends HttpServlet {
 private Path uploadRoot(){String p=System.getenv("JOBCOM_UPLOAD_DIR");if(p==null||p.isBlank()){String real=getServletContext().getRealPath("/uploads");p=real==null?Paths.get(System.getProperty("user.home"),"Job.com","uploads").toString():real;}return Paths.get(p).toAbsolutePath().normalize();}
 private Path assessmentDir()throws IOException{Path root=uploadRoot(),dir=root.resolve("assessments").normalize();if(!dir.startsWith(root))throw new IOException("Invalid assessment upload path");Files.createDirectories(dir);return dir;}
 private Set<String> columns(Connection c)throws SQLException{Set<String>x=new HashSet<>();try(ResultSet r=c.getMetaData().getColumns(c.getCatalog(),null,"assessments",null)){while(r.next())x.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));}return x;}
 private String col(Set<String>s,String...names)throws SQLException{for(String n:names)if(s.contains(n))return n;throw new SQLException("Assessment table is missing a required column: "+String.join("/",names));}
 private String optional(Set<String>s,String...names){for(String n:names)if(s.contains(n))return n;return null;}
 protected void doGet(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{
  try(Connection c=Db.getConnection()){
   Set<String> cols=AssessmentResourceSchema.ensureCategories(c);String idCol=col(cols,"assessment_id","id"),titleCol=col(cols,"title","name"),fileCol=col(cols,"file_path","document_path"),companyCol=col(cols,"company_name"),roleCol=col(cols,"role_name"),activeCol=optional(cols,"active","is_active","status"),createdCol=optional(cols,"created_at","uploaded_at","created_on");
   String download=q.getParameter("download");if(download!=null){String sql="SELECT `"+fileCol+"` FROM assessments WHERE `"+idCol+"`=?"+(activeCol==null?"":" AND `"+activeCol+"` "+(activeCol.equals("status")?"='ACTIVE'":"=1"));try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,Long.parseLong(download));try(ResultSet r=s.executeQuery()){if(!r.next()){p.sendError(404);return;}String relative=r.getString(1);if(relative==null||!relative.matches("assessments/[a-fA-F0-9-]{36}\\.pdf")){p.sendError(404);return;}Path root=uploadRoot(),f=root.resolve(relative).normalize();if(!f.startsWith(root)||!Files.isRegularFile(f)){p.sendError(404);return;}p.setContentType("application/pdf");p.setHeader("Content-Disposition","attachment; filename=assessment-"+download+".pdf");p.setContentLengthLong(Files.size(f));try(InputStream in=Files.newInputStream(f);OutputStream out=p.getOutputStream()){in.transferTo(out);}return;}}}
   String sql="SELECT `"+idCol+"` AS record_id,`"+titleCol+"` AS record_title,`"+companyCol+"` AS record_company,`"+roleCol+"` AS record_role,"+(activeCol==null?"1":"`"+activeCol+"`")+" AS record_active,"+(createdCol==null?"NULL":"`"+createdCol+"`")+" AS record_created FROM assessments ORDER BY `"+idCol+"` DESC";List<Map<String,Object>>rows=new ArrayList<>();try(PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){while(r.next()){Map<String,Object>m=new HashMap<>();m.put("id",r.getLong("record_id"));m.put("title",r.getString("record_title"));m.put("company",r.getString("record_company"));m.put("role",r.getString("record_role"));Object a=r.getObject("record_active");m.put("active",a instanceof Boolean b?b:!"0".equals(String.valueOf(a))&&!(a instanceof Number n&&n.intValue()==0)&&!"DISABLED".equalsIgnoreCase(String.valueOf(a)));m.put("created_at",r.getObject("record_created"));rows.add(m);}}
   q.setAttribute("rows",rows);q.getRequestDispatcher("/WEB-INF/views/admin/assessments.jsp").forward(q,p);
  }catch(SQLException|NumberFormatException e){getServletContext().log("Assessment page failed",e);throw new ServletException(e);}
 }
 protected void doPost(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{
  HttpSession session=q.getSession(false);if(session==null||!Objects.equals(session.getAttribute("csrf"),q.getParameter("csrf"))){p.sendError(403);return;}
  Path saved=null;try(Connection c=Db.getConnection()){Set<String>cols=AssessmentResourceSchema.ensureCategories(c);String idCol=col(cols,"assessment_id","id"),activeCol=optional(cols,"active","is_active","status"),titleCol=col(cols,"title","name"),fileCol=col(cols,"file_path","document_path"),companyCol=col(cols,"company_name"),roleCol=col(cols,"role_name");String disable=q.getParameter("disable");
   if(disable!=null){if(activeCol==null){throw new SQLException("Assessment table has no active/status column");}String val=activeCol.equals("status")?"DISABLED":"0";try(PreparedStatement s=c.prepareStatement("UPDATE assessments SET `"+activeCol+"`=? WHERE `"+idCol+"`=?")){s.setString(1,val);s.setLong(2,Long.parseLong(disable));s.executeUpdate();}}
   else{String title=q.getParameter("title"),company=q.getParameter("company"),role=q.getParameter("role");Part part=q.getPart("file");if(title==null||title.isBlank()||title.length()>180||company==null||company.isBlank()||company.length()>120||role==null||role.isBlank()||role.length()>120||part==null||part.getSize()<5||part.getSize()>5242880)throw new ServletException("Enter a title, company, job role, and PDF under 5 MB");byte[]h=new byte[5];try(InputStream in=part.getInputStream()){if(in.read(h)!=5||h[0]!='%'||h[1]!='P'||h[2]!='D'||h[3]!='F'||h[4]!='-')throw new ServletException("Upload must be a PDF");}
    Path dir=assessmentDir();String name=UUID.randomUUID()+".pdf";saved=dir.resolve(name);try(InputStream in=part.getInputStream()){Files.copy(in,saved,StandardCopyOption.REPLACE_EXISTING);}LinkedHashMap<String,Object>fields=new LinkedHashMap<>();fields.put(titleCol,title.trim());fields.put(fileCol,"assessments/"+name);fields.put(companyCol,company.trim());fields.put(roleCol,role.trim());if(activeCol!=null){fields.put(activeCol,activeCol.equals("status")?"ACTIVE":"1");}String uploaded=optional(cols,"uploaded_by","admin_id");if(uploaded!=null&&session.getAttribute("adminId")!=null)fields.put(uploaded,session.getAttribute("adminId"));String names=String.join(",",fields.keySet()),marks=String.join(",",Collections.nCopies(fields.size(),"?"));try(PreparedStatement s=c.prepareStatement("INSERT INTO assessments ("+names+") VALUES ("+marks+")")){int i=1;for(Object v:fields.values())s.setObject(i++,v);s.executeUpdate();}saved=null;
   }
   p.sendRedirect(q.getContextPath()+"/admin/assessments");
  }catch(SQLException|NumberFormatException|ServletException e){if(saved!=null)Files.deleteIfExists(saved);getServletContext().log("Assessment action failed",e);q.setAttribute("error","The assessment could not be saved. Please check the assessment table and upload settings.");doGet(q,p);}
 }
}
