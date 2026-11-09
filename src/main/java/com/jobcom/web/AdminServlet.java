package com.jobcom.web;
import com.jobcom.dao.AdminDAO;
import com.jobcom.dao.NotificationDAO;
import com.jobcom.dao.SupportDAO;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
public class AdminServlet extends HttpServlet {
 private final AdminDAO dao=new AdminDAO();private final SupportDAO supportDAO=new SupportDAO();private static final Set<String>SECTIONS=Set.of("dashboard","user-requests","users","company-requests","companies","jobs","catalog","practice","reports","notifications","support");
 private String token(HttpSession s){String t=(String)s.getAttribute("csrf");if(t==null){t=UUID.randomUUID().toString();s.setAttribute("csrf",t);}return t;}
 protected void doGet(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{
  String section=q.getParameter("section");if(section==null||!SECTIONS.contains(section))section="dashboard";q.setAttribute("section",section);q.setAttribute("csrf",token(q.getSession()));
  String recordType=null,accountFilter=null,title=section,subtitle="Manage portal activity, accounts, and verification requests.";
  switch(section){case "user-requests"->{recordType="users";accountFilter="pending";title="New Seeker Requests";subtitle="Review job seeker account and government ID requests.";}case "users"->{recordType="users";accountFilter="approved";title="Job Seeker List";subtitle="Approved job seeker accounts.";}case "company-requests"->{recordType="companies";accountFilter="pending";title="New Recruiter Requests";subtitle="Review recruiter and company license requests.";}case "companies"->{recordType="companies";accountFilter="approved";title="Recruiter List";subtitle="Approved recruiter and company accounts.";}case "jobs"->{recordType="jobs";title="Jobs";subtitle="Review job posts submitted by approved companies.";}case "support"->{title="Help & Support";subtitle="Manage private help conversations and complaints from job seekers and recruiters.";}case "catalog","practice","reports","notifications"->{recordType=section;title=section.substring(0,1).toUpperCase(Locale.ROOT)+section.substring(1);}}
  q.setAttribute("recordType",recordType);q.setAttribute("accountFilter",accountFilter);q.setAttribute("sectionTitle",title);q.setAttribute("sectionSubtitle",subtitle);
  try{Map<String,Object> stats=dao.stats();if("dashboard".equals(section))stats.keySet().removeIf(k->Set.of("Job listings","Pending jobs","Applications").contains(k));q.setAttribute("stats",stats);q.setAttribute("pendingSeekers",stats.get("Pending seekers"));q.setAttribute("pendingRecruiters",stats.get("Pending recruiters"));try{q.setAttribute("openSupportCount",supportDAO.openCount());}catch(Exception supportCountError){q.setAttribute("openSupportCount",0);}if("support".equals(section)){q.setAttribute("supportCases",supportDAO.forAdmin());String rawId=q.getParameter("conversationId");if(rawId!=null&&!rawId.isBlank()){long supportId=Long.parseLong(rawId);Map<String,Object> conversation=supportDAO.conversation(supportId,null);if(conversation!=null){q.setAttribute("selectedSupport",conversation);q.setAttribute("supportMessages",supportDAO.messages(supportId));}}q.setAttribute("rows",List.of());}else q.setAttribute("rows",dao.list(recordType==null?section:recordType,q.getParameter("q"),accountFilter));
  }catch(Exception e){getServletContext().log("Admin data load failed",e);q.setAttribute("error","Database query failed. Please check the database schema and connection settings.");}
  q.getRequestDispatcher("/WEB-INF/views/admin/admin.jsp").forward(q,p);
 }
 protected void doPost(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{
  HttpSession s=q.getSession(false);if(s==null||!Objects.equals(s.getAttribute("csrf"),q.getParameter("csrf"))){p.sendError(403,"Invalid form token");return;}
  String section=q.getParameter("section");try{String action=q.getParameter("action");long id=((Number)s.getAttribute("adminId")).longValue();
   if("supportReply".equals(action)){long conversationId=Long.parseLong(q.getParameter("conversationId"));String body=Objects.toString(q.getParameter("body"),"").trim();if(body.isBlank()||body.length()>4000)throw new IllegalArgumentException("Write a reply containing 1 to 4,000 characters.");if(!supportDAO.replyAsAdmin(conversationId,id,body))throw new IllegalArgumentException("That support request could not be found.");}
   else if("supportStatus".equals(action)){long conversationId=Long.parseLong(q.getParameter("conversationId"));if(!supportDAO.setStatus(conversationId,q.getParameter("status")))throw new IllegalArgumentException("That support request could not be found.");}
   else if(Set.of("approve","reject","block","unblock","verify","delete").contains(action)){
    String type=q.getParameter("type");long recordId=Long.parseLong(q.getParameter("id"));dao.changeStatus(type,recordId,action,id);
    if("jobs".equals(type)&&"verify".equals(action)){try{new NotificationDAO().notifyMatchingSeekersForJob(recordId);}catch(Exception alertError){getServletContext().log("Approved job alert delivery failed for job "+recordId,alertError);}}
   }
   else if("addCatalog".equals(action))dao.addCatalog(q.getParameter("name").trim());
   else if("addPractice".equals(action))dao.addPractice(q.getParameter("title").trim(),q.getParameter("url").trim());
   else if("announce".equals(action))dao.announce(q.getParameter("audience"),q.getParameter("subject").trim(),q.getParameter("body").trim(),id);
   String destination=q.getContextPath()+"/admin?section="+(section!=null&&SECTIONS.contains(section)?section:"dashboard");if("support".equals(section)&&q.getParameter("conversationId")!=null)destination+="&conversationId="+Long.parseLong(q.getParameter("conversationId"));p.sendRedirect(destination);
  }catch(Exception e){getServletContext().log("Admin action failed",e);q.setAttribute("error","The action could not be completed. Please check the information and try again.");doGet(q,p);}
 }
}
