package com.jobcom.web;
import com.jobcom.dao.ResumeDAO;import com.jobcom.dao.JobPortalDAO;import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.*;import java.sql.SQLException;import java.util.*;
public class ResumeBuilderServlet extends HttpServlet {
private final ResumeDAO dao = new ResumeDAO();
private final JobPortalDAO jobs = new JobPortalDAO();
private static final int MAX_ENTRIES = 10;
private static final List<String> FIELDS = List.of("full_name", "email", "phone", "headline", "summary", "skills", "experience", "certifications", "linkedin", "portfolio", "education_notes", "project_notes");
private static final List<String> EDUCATION_FIELDS = List.of("course", "institution", "year", "percentage");
private static final List<String> PROJECT_FIELDS = List.of("name", "tools", "description");
protected void service(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{HttpSession s=q.getSession(false);if(s==null||s.getAttribute("seekerId")==null){p.sendRedirect(q.getContextPath()+"/seeker/login");return;}super.service(q,p);}
protected void doGet(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{
  try{
    dao.ensure();
    long user=((Number)q.getSession().getAttribute("seekerId")).longValue();
    try{q.setAttribute("hasChats",!jobs.seekerChats(user).isEmpty());}catch(Exception ignored){q.setAttribute("hasChats",false);}
    List<Map<String,Object>>ts=dao.templates(true);q.setAttribute("templates",ts);
    Map<String,String>profile=dao.profile(user),data=new LinkedHashMap<>();
    data.put("title","My Resume");data.put("full_name",profile.getOrDefault("full_name",profile.getOrDefault("name","")));
    data.put("email",profile.getOrDefault("email",""));data.put("phone",profile.getOrDefault("phone",""));
    data.put("headline",profile.getOrDefault("profile_headline",""));data.put("summary",profile.getOrDefault("profile_summary",""));
    data.put("skills",profile.getOrDefault("skills",""));
    data.put("education_notes","");data.put("project_notes","");
    data.put("experience",profile.getOrDefault("experience_years",""));data.put("linkedin",profile.getOrDefault("linkedin_url",""));
    data.put("portfolio",profile.getOrDefault("portfolio_url",""));
    long tid=ts.isEmpty()?0:((Number)ts.get(0).get("template_id")).longValue();
    try{String requested=q.getParameter("template");if(requested!=null)tid=Long.parseLong(requested);}catch(NumberFormatException ignored){}
    final long requestedTid=tid;
    Map<String,Object>selected=ts.stream().filter(t->((Number)t.get("template_id")).longValue()==requestedTid).findFirst().orElse(null);
    if(selected==null&&!ts.isEmpty()){selected=ts.get(0);tid=((Number)selected.get("template_id")).longValue();}
    if(selected!=null){
      Map<String,String> saved = dao.draft(user,tid);
      data.putAll(saved);
      List<Map<String,String>> educationRows = readRows(saved, "education", EDUCATION_FIELDS);
      if (educationRows.isEmpty()) {
        String oldEducation = saved.getOrDefault("education", "");
        educationRows.add(entry(EDUCATION_FIELDS, oldEducation.isBlank() ? profile.getOrDefault("qualification", "") : ""));
        data.put("education_notes", saved.getOrDefault("education_notes", oldEducation));
      } else data.put("education_notes", saved.getOrDefault("education_notes", ""));
      List<Map<String,String>> projectRows = readRows(saved, "project", PROJECT_FIELDS);
      if (projectRows.isEmpty()) {
        projectRows.add(entry(PROJECT_FIELDS, ""));
        data.put("project_notes", saved.getOrDefault("project_notes", saved.getOrDefault("projects", "")));
      } else data.put("project_notes", saved.getOrDefault("project_notes", ""));
      q.setAttribute("educationRows", educationRows);
      q.setAttribute("projectRows", projectRows);
      String layout=String.valueOf(selected.get("layout"));
      Set<String> layouts=Set.of("classic","modern","compact","creative","ocean","sunset","photo-violet","photo-emerald");
      if(!layouts.contains(layout))layout="classic";
      q.setAttribute("selectedLayout",layout);
      q.setAttribute("photoTemplate",Set.of("photo-violet","photo-emerald").contains(layout));
      String photo=profile.getOrDefault("profile_photo_path",profile.getOrDefault("profile_picture",profile.getOrDefault("avatar_path","")));
      q.setAttribute("profilePhotoAvailable",photo.matches("profile-pictures/[a-fA-F0-9-]{36}\\.(jpg|png)"));
      q.setAttribute("profilePhotoUrl",q.getContextPath()+"/seeker/profile-photo");
      q.setAttribute("sampleFile",selected.get("file_path"));
    }
    q.setAttribute("data",data);q.setAttribute("selectedTemplate",tid);
    String csrf=(String)q.getSession().getAttribute("resumeCsrf");if(csrf==null){csrf=UUID.randomUUID().toString();q.getSession().setAttribute("resumeCsrf",csrf);}q.setAttribute("csrf",csrf);
  }catch(Exception e){getServletContext().log("Resume builder load failed",e);q.setAttribute("error","Resume templates could not be loaded. Please try again.");}
  q.getRequestDispatcher("/WEB-INF/views/seeker/resume-builder.jsp").forward(q,p);
}
protected void doPost(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException {
  q.setCharacterEncoding("UTF-8");
  HttpSession s=q.getSession(false);
  if(s==null||!Objects.equals(s.getAttribute("resumeCsrf"),q.getParameter("csrf"))){p.sendError(403);return;}
  try {
    long tid=Long.parseLong(q.getParameter("template_id"));
    if(dao.templates(true).stream().noneMatch(t->((Number)t.get("template_id")).longValue()==tid))throw new IllegalArgumentException("Choose an available ATS template.");
    Map<String,String> d=new LinkedHashMap<>();
    for(String f:FIELDS)d.put(f,Optional.ofNullable(q.getParameter(f)).orElse("").trim());
    List<Map<String,String>> educationRows=submittedRows(q,"education",EDUCATION_FIELDS);
    List<Map<String,String>> projectRows=submittedRows(q,"project",PROJECT_FIELDS);
    storeRows(d,"education",educationRows,EDUCATION_FIELDS);
    storeRows(d,"project",projectRows,PROJECT_FIELDS);
    d.put("education",summarize(educationRows,"education"));
    d.put("projects",summarize(projectRows,"project"));
    String title=Optional.ofNullable(q.getParameter("title")).orElse("My Resume").trim();
    if(title.isBlank())title="My Resume";
    dao.saveDraft(((Number)s.getAttribute("seekerId")).longValue(),tid,title,d);
    p.sendRedirect(q.getContextPath()+"/resume?template="+tid+"&saved=1");
  } catch(Exception e) {
    getServletContext().log("Resume draft save failed",e);
    q.setAttribute("error",e instanceof IllegalArgumentException?e.getMessage():"Your resume could not be saved. Please try again.");
    doGet(q,p);
  }
}

private List<Map<String,String>> readRows(Map<String,String> data,String prefix,List<String> fields) {
  List<Map<String,String>> rows=new ArrayList<>();
  for(int i=0;i<MAX_ENTRIES;i++) {
    boolean found=false;
    for(String field:fields)if(data.containsKey(prefix+"_"+i+"_"+field)){found=true;break;}
    if(!found)break;
    Map<String,String> row=new LinkedHashMap<>();
    for(String field:fields)row.put(field,data.getOrDefault(prefix+"_"+i+"_"+field,""));
    rows.add(row);
  }
  return rows;
}

private Map<String,String> entry(List<String> fields,String firstValue) {
  Map<String,String> row=new LinkedHashMap<>();
  for(String field:fields)row.put(field,"");
  row.put(fields.get(0),firstValue);
  return row;
}

private List<Map<String,String>> submittedRows(HttpServletRequest q,String prefix,List<String> fields) {
  List<Map<String,String>> rows=new ArrayList<>();
  String[][] values=new String[fields.size()][];
  int count=0;
  for(int i=0;i<fields.size();i++){values[i]=q.getParameterValues(prefix+"_"+fields.get(i));if(values[i]!=null)count=Math.max(count,values[i].length);}
  count=Math.min(count,MAX_ENTRIES);
  for(int rowIndex=0;rowIndex<count;rowIndex++) {
    Map<String,String> row=new LinkedHashMap<>();boolean hasValue=false;
    for(int fieldIndex=0;fieldIndex<fields.size();fieldIndex++) {
      String[] list=values[fieldIndex];String value=list!=null&&rowIndex<list.length?Optional.ofNullable(list[rowIndex]).orElse("").trim():"";
      row.put(fields.get(fieldIndex),value);if(!value.isBlank())hasValue=true;
    }
    if(hasValue)rows.add(row);
  }
  return rows;
}

private void storeRows(Map<String,String> data,String prefix,List<Map<String,String>> rows,List<String> fields) {
  for(int i=0;i<rows.size();i++)for(String field:fields)data.put(prefix+"_"+i+"_"+field,rows.get(i).getOrDefault(field,""));
}

private String summarize(List<Map<String,String>> rows,String prefix) {
  List<String> lines=new ArrayList<>();
  for(Map<String,String> row:rows) {
    if("education".equals(prefix)) {
      List<String> parts=new ArrayList<>();
      addPart(parts,"Course",row.get("course"));addPart(parts,"School / college",row.get("institution"));
      addPart(parts,"Year",row.get("year"));addPart(parts,"Percentage",row.get("percentage"));
      if(!parts.isEmpty())lines.add(String.join(" · ",parts));
    } else {
      List<String> parts=new ArrayList<>();addPart(parts,"Project",row.get("name"));addPart(parts,"Tools used",row.get("tools"));
      addPart(parts,"Details",row.get("description"));if(!parts.isEmpty())lines.add(String.join(" · ",parts));
    }
  }
  return String.join("\n",lines);
}

private void addPart(List<String> parts,String label,String value) {if(value!=null&&!value.isBlank())parts.add(label+": "+value);}
}
