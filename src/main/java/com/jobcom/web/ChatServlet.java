package com.jobcom.web;

import com.jobcom.dao.JobPortalDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Private chat becomes available only for a job application and its two participants. */
public class ChatServlet extends HttpServlet {
    private static final String CSRF="applicationChatCsrf";
    private final JobPortalDAO dao=new JobPortalDAO();

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        HttpSession session=req.getSession(false);Long seeker=account(session,"seekerId"),company=account(session,"companyId");
        if(seeker==null&&company==null){resp.sendRedirect(req.getContextPath()+"/seeker/login");return;}
        try{
            req.setAttribute("chats",seeker!=null?dao.seekerChats(seeker):dao.recruiterChats(company));
            String raw=req.getParameter("applicationId");
            if(raw!=null&&!raw.isBlank()){
                long applicationId=Long.parseLong(raw);Map<String,Object> conversation=dao.conversation(applicationId,seeker,company);
                if(conversation==null){resp.sendError(HttpServletResponse.SC_FORBIDDEN,"Chat is available only to the seeker and recruiter on this application.");return;}
                req.setAttribute("conversation",conversation);req.setAttribute("messages",dao.messages(applicationId));
            }
        }catch(NumberFormatException e){resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Invalid application id");return;}
        catch(Exception e){getServletContext().log("Application chat could not be loaded",e);req.setAttribute("error","Chat could not be loaded. Please check the database schema.");req.setAttribute("chats",List.of());}
        Object chatList = req.getAttribute("chats");
        req.setAttribute("hasChats", seeker != null && chatList instanceof List<?> items && !items.isEmpty());
        if(session.getAttribute(CSRF)==null)session.setAttribute(CSRF,UUID.randomUUID().toString());
        req.setAttribute("csrf",session.getAttribute(CSRF));req.setAttribute("isRecruiter",company!=null);
        req.setAttribute("currentAccountId",seeker!=null?seeker:company);
        req.getRequestDispatcher("/WEB-INF/views/chat.jsp").forward(req,resp);
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");HttpSession session=req.getSession(false);Long seeker=account(session,"seekerId"),company=account(session,"companyId");
        if(seeker==null&&company==null){resp.sendRedirect(req.getContextPath()+"/seeker/login");return;}
        if(!safeEquals(session==null?null:(String)session.getAttribute(CSRF),req.getParameter("csrf"))){resp.sendError(403,"Invalid form token");return;}
        String body=Optional.ofNullable(req.getParameter("body")).orElse("").trim();
        try{
            long applicationId=Long.parseLong(req.getParameter("applicationId"));
            if(body.isBlank()||body.length()>4000){session.setAttribute("chatFlash","Write a message with up to 4,000 characters.");}
            else{Map<String,Object> participant=dao.conversation(applicationId,seeker,company);if(participant==null){resp.sendError(403);return;}dao.sendMessage(applicationId,seeker!=null?"SEEKER":"RECRUITER",(seeker!=null?seeker:company),body);}
            resp.sendRedirect(req.getContextPath()+"/chat?applicationId="+applicationId);
        }catch(NumberFormatException e){resp.sendError(400,"Invalid application id");}
        catch(Exception e){getServletContext().log("Application chat message could not be sent",e);session.setAttribute("chatFlash","Message could not be sent. Please try again.");resp.sendRedirect(req.getContextPath()+"/chat");}
    }

    private Long account(HttpSession s,String key){Object value=s==null?null:s.getAttribute(key);return value instanceof Number?((Number)value).longValue():null;}
    private boolean safeEquals(String a,String b){return a!=null&&b!=null&&MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),b.getBytes(StandardCharsets.UTF_8));}
}
