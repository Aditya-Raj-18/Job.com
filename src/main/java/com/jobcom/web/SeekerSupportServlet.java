package com.jobcom.web;

import com.jobcom.dao.SupportDAO;
import com.jobcom.dao.JobPortalDAO;
import com.jobcom.dao.ResumeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Seeker-facing support inbox for complaints and help requests. */
public class SeekerSupportServlet extends HttpServlet {
    private static final String CSRF = "seekerSupportCsrf";
    private final SupportDAO dao = new SupportDAO();
    private final JobPortalDAO jobs = new JobPortalDAO();
    private final ResumeDAO resumes = new ResumeDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Long seekerId = account(session, "seekerId");
        if (seekerId == null) { resp.sendRedirect(req.getContextPath() + "/seeker/login"); return; }
        try { req.setAttribute("profile", resumes.profile(seekerId)); }
        catch (Exception e) { getServletContext().log("Seeker profile could not be loaded for support", e); req.setAttribute("profile", Map.of()); }
        try { req.setAttribute("hasChats", !jobs.seekerChats(seekerId).isEmpty()); }
        catch (Exception e) { getServletContext().log("Seeker chat availability could not be loaded", e); req.setAttribute("hasChats", false); }

        if (session.getAttribute(CSRF) == null) session.setAttribute(CSRF, UUID.randomUUID().toString());
        req.setAttribute("csrf", session.getAttribute(CSRF));
        req.setAttribute("showCompose", "1".equals(req.getParameter("new")));
        req.setAttribute("flash", session.getAttribute("supportFlash"));
        req.setAttribute("flashError", Boolean.TRUE.equals(session.getAttribute("supportFlashError")));
        session.removeAttribute("supportFlash"); session.removeAttribute("supportFlashError");

        try {
            List<Map<String, Object>> requests = dao.forSeeker(seekerId);
            req.setAttribute("supportRequests", requests);
            req.setAttribute("openSupportCount", requests.stream().filter(row -> !"RESOLVED".equals(row.get("status"))).count());
            if (requests.isEmpty()) req.setAttribute("showCompose", true);
            String rawId = req.getParameter("conversationId");
            if (rawId != null && !rawId.isBlank()) {
                long id;
                try { id = Long.parseLong(rawId); }
                catch (NumberFormatException e) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid support request id."); return; }
                Map<String, Object> conversation = dao.conversation(id, seekerId);
                if (conversation == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("selectedRequest", conversation);
                req.setAttribute("messages", dao.messages(id));
                req.setAttribute("showCompose", false);
            }
        } catch (Exception e) {
            getServletContext().log("Seeker support inbox could not be loaded", e);
            req.setAttribute("error", "Your support inbox could not be loaded. Please try again shortly.");
            req.setAttribute("supportRequests", List.of());
            req.setAttribute("showCompose", true);
        }
        req.getRequestDispatcher("/WEB-INF/views/seeker/support.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        Long seekerId = account(session, "seekerId");
        if (seekerId == null) { resp.sendRedirect(req.getContextPath() + "/seeker/login"); return; }
        if (!safeEquals(session == null ? null : (String) session.getAttribute(CSRF), req.getParameter("csrf"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid form token."); return;
        }

        String action = value(req, "action");
        String body = value(req, "body");
        if (body.isBlank() || body.length() > 4000) {
            flash(session, "Write a message containing 1 to 4,000 characters.", true);
            redirect(req, resp, "create".equals(action) ? null : req.getParameter("conversationId"), "create".equals(action));
            return;
        }
        try {
            if ("create".equals(action)) {
                String category = value(req, "category");
                String subject = value(req, "subject");
                if (!Set.of("HELP", "COMPLAINT").contains(category) || subject.isBlank() || subject.length() > 180) {
                    flash(session, "Choose a request type and enter a subject up to 180 characters.", true);
                    redirect(req, resp, null, true); return;
                }
                long id = dao.create(seekerId, category, subject, body);
                flash(session, "Your request has been sent to the Job.com admin team.", false);
                redirect(req, resp, Long.toString(id), false);
            } else if ("reply".equals(action)) {
                long id = Long.parseLong(req.getParameter("conversationId"));
                if (!dao.replyAsSeeker(id, seekerId, body)) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                flash(session, "Your reply has been sent.", false);
                redirect(req, resp, Long.toString(id), false);
            } else {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown support action.");
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid support request id.");
        } catch (Exception e) {
            getServletContext().log("Seeker support message could not be saved", e);
            flash(session, "Your message could not be sent. Please try again.", true);
            redirect(req, resp, req.getParameter("conversationId"), "create".equals(action));
        }
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String id, boolean compose) throws IOException {
        String url = req.getContextPath() + "/support";
        if (compose) url += "?new=1";
        else if (id != null && id.matches("\\d+")) url += "?conversationId=" + id;
        resp.sendRedirect(url);
    }

    private void flash(HttpSession session, String message, boolean error) {
        session.setAttribute("supportFlash", message); session.setAttribute("supportFlashError", error);
    }

    private String value(HttpServletRequest req, String name) { return Optional.ofNullable(req.getParameter(name)).orElse("").trim(); }
    private Long account(HttpSession session, String key) {
        Object value = session == null ? null : session.getAttribute(key);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
    private boolean safeEquals(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
