package com.jobcom.web;
import jakarta.servlet.http.*;import java.io.IOException;import java.util.Objects;
public class LogoutServlet extends HttpServlet{protected void doPost(HttpServletRequest q,HttpServletResponse p)throws IOException{HttpSession s=q.getSession(false);if(s==null||!Objects.equals(s.getAttribute("csrf"),q.getParameter("csrf"))){p.sendError(403);return;}s.invalidate();p.sendRedirect(q.getContextPath()+"/admin/login");}}
