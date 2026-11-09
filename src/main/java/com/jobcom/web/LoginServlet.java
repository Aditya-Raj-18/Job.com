package com.jobcom.web;
import com.jobcom.dao.AdminDAO;
import com.jobcom.security.PasswordUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
public class LoginServlet extends HttpServlet {
 protected void doGet(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{
  HttpSession s=q.getSession();String token=(String)s.getAttribute("csrf");if(token==null){token=UUID.randomUUID().toString();s.setAttribute("csrf",token);}q.setAttribute("csrf",token);q.getRequestDispatcher("/WEB-INF/views/auth/admin-login.jsp").forward(q,p);
 }
 protected void doPost(HttpServletRequest q,HttpServletResponse p)throws ServletException,IOException{
  HttpSession session=q.getSession(false);if(session==null||!safeEquals((String)session.getAttribute("csrf"),q.getParameter("csrf"))){session=q.getSession(true);String token=UUID.randomUUID().toString();session.setAttribute("csrf",token);q.setAttribute("csrf",token);q.setAttribute("error","Your sign-in session expired. Please try again.");q.getRequestDispatcher("/WEB-INF/views/auth/admin-login.jsp").forward(q,p);return;}
  try{Map<String,Object>a=new AdminDAO().authenticate(Optional.ofNullable(q.getParameter("email")).orElse("").trim());
   if(a!=null&&PasswordUtil.verify(q.getParameter("password"),(String)a.get("hash"))){session.invalidate();HttpSession s=q.getSession(true);s.setAttribute("adminId",a.get("id"));s.setAttribute("adminName",a.get("name"));s.setAttribute("adminEmail",a.get("email"));p.sendRedirect(q.getContextPath()+"/admin");return;}
   q.setAttribute("error","The email or password is incorrect.");
  }catch(Exception e){getServletContext().log("Admin login failed",e);q.setAttribute("error","We couldn't sign you in because the database is unavailable. Please try again later.");}
  doGet(q,p);
 }
 private boolean safeEquals(String a,String b){return a!=null&&b!=null&&java.security.MessageDigest.isEqual(a.getBytes(java.nio.charset.StandardCharsets.UTF_8),b.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
}
