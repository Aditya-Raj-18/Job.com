package com.jobcom.web;
import jakarta.servlet.*;import jakarta.servlet.http.*;import java.io.IOException;
public class AdminAuthFilter implements Filter{public void doFilter(ServletRequest a,ServletResponse b,FilterChain c)throws IOException,ServletException{HttpServletRequest q=(HttpServletRequest)a;HttpServletResponse p=(HttpServletResponse)b;if(q.getRequestURI().equals(q.getContextPath()+"/admin/login")){c.doFilter(a,b);return;}HttpSession s=q.getSession(false);if(s==null||s.getAttribute("adminId")==null){p.sendRedirect(q.getContextPath()+"/admin/login");return;}c.doFilter(a,b);}}
