package com.jobcom.web;

import com.jobcom.dao.ResumeDAO;
import com.jobcom.security.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;

public class SeekerLoginServlet extends HttpServlet {
    private final ResumeDAO dao=new ResumeDAO();

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        if("1".equals(req.getParameter("logout"))){HttpSession old=req.getSession(false);if(old!=null)old.invalidate();resp.sendRedirect(req.getContextPath()+"/seeker/login");return;}
        HttpSession existing=req.getSession(false);
        if(existing!=null&&existing.getAttribute("seekerId")!=null){resp.sendRedirect(req.getContextPath()+"/seeker");return;}
        HttpSession session=req.getSession();String token=(String)session.getAttribute("seekerCsrf");if(token==null){token=UUID.randomUUID().toString();session.setAttribute("seekerCsrf",token);}
        req.setAttribute("csrf",token);req.getRequestDispatcher("/WEB-INF/views/auth/seeker-login.jsp").forward(req,resp);
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        HttpSession session=req.getSession(false);if(session==null||!safeEquals((String)session.getAttribute("seekerCsrf"),req.getParameter("csrf"))){session=req.getSession(true);String token=UUID.randomUUID().toString();session.setAttribute("seekerCsrf",token);req.setAttribute("csrf",token);req.setAttribute("error","Your sign-in session expired. Please try again.");req.getRequestDispatcher("/WEB-INF/views/auth/seeker-login.jsp").forward(req,resp);return;}
        try{
            String email=Optional.ofNullable(req.getParameter("email")).orElse("").trim().toLowerCase(Locale.ROOT);Map<String,Object> user=dao.authenticate(email);
            if(user==null||!PasswordUtil.verify(req.getParameter("password"),(String)user.get("hash")))req.setAttribute("error","The email or password is incorrect.");
            else if(!"ACTIVE".equalsIgnoreCase(String.valueOf(user.get("status")))&&!"APPROVED".equalsIgnoreCase(String.valueOf(user.get("status"))))req.setAttribute("error","Your account is awaiting administrator approval.");
            else{req.changeSessionId();session.setAttribute("seekerId",user.get("id"));session.setAttribute("seekerName",user.get("name"));session.removeAttribute("seekerCsrf");resp.sendRedirect(req.getContextPath()+"/seeker");return;}
        }catch(Exception e){getServletContext().log("Seeker sign in failed",e);req.setAttribute("error","Sign in failed. Please check your account or try again later.");}
        req.setAttribute("csrf",session.getAttribute("seekerCsrf"));req.getRequestDispatcher("/WEB-INF/views/auth/seeker-login.jsp").forward(req,resp);
    }

    private boolean safeEquals(String first,String second){return first!=null&&second!=null&&java.security.MessageDigest.isEqual(first.getBytes(java.nio.charset.StandardCharsets.UTF_8),second.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
}
