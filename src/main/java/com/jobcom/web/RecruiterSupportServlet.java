package com.jobcom.web;

import com.jobcom.dao.SupportDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Private recruiter-to-admin help and complaint conversations. */
public class RecruiterSupportServlet extends HttpServlet {
    private static final String CSRF = "recruiterSupportCsrf";
    private final SupportDAO dao = new SupportDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Long companyId = account(session);
        if (companyId == null) { resp.sendRedirect(req.getContextPath() + "/recruiter/login"); return; }
        if (session.getAttribute(CSRF) == null) session.setAttribute(CSRF, UUID.randomUUID().toString());
        req.setAttribute("csrf", session.getAttribute(CSRF));
        req.setAttribute("companyName", session.getAttribute("companyName"));
        req.setAttribute("showCompose", "1".equals(req.getParameter("new")));
        req.setAttribute("flash", session.getAttribute("recruiterSupportFlash"));
        req.setAttribute("flashError", Boolean.TRUE.equals(session.getAttribute("recruiterSupportFlashError")));
        session.removeAttribute("recruiterSupportFlash"); session.removeAttribute("recruiterSupportFlashError");
        try {
            List<Map<String, Object>> requests = dao.forRecruiter(companyId);
            req.setAttribute("supportRequests", requests);
            req.setAttribute("openSupportCount", requests.stream().filter(row -> !"RESOLVED".equals(row.get("status"))).count());
            if (requests.isEmpty()) req.setAttribute("showCompose", true);
            String rawId = req.getParameter("conversationId");
            if (rawId != null && !rawId.isBlank()) {
                long id;
                try { id = Long.parseLong(rawId); }
                catch (NumberFormatException e) { resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid support request id."); return; }
                Map<String, Object> conversation = dao.conversation(id, "RECRUITER", companyId);
                if (conversation == null) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                req.setAttribute("selectedRequest", conversation);
                req.setAttribute("messages", dao.messages(id));
                req.setAttribute("showCompose", false);
            }
        } catch (Exception e) {
            getServletContext().log("Recruiter support inbox could not be loaded", e);
            req.setAttribute("error", "Your support inbox could not be loaded. Please try again shortly.");
            req.setAttribute("supportRequests", List.of()); req.setAttribute("showCompose", true);
        }
        req.getRequestDispatcher("/WEB-INF/views/recruiter/support.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        Long companyId = account(session);
        if (companyId == null) { resp.sendRedirect(req.getContextPath() + "/recruiter/login"); return; }
        if (!safeEquals(session == null ? null : (String) session.getAttribute(CSRF), req.getParameter("csrf"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid form token."); return;
        }
        String action = value(req, "action"), body = value(req, "body");
        if (body.isBlank() || body.length() > 4000) {
            flash(session, "Write a message containing 1 to 4,000 characters.", true);
            redirect(req, resp, req.getParameter("conversationId"), "create".equals(action)); return;
        }
        try {
            if ("create".equals(action)) {
                String category = value(req, "category"), subject = value(req, "subject");
                if (!Set.of("HELP", "COMPLAINT").contains(category) || subject.isBlank() || subject.length() > 180) {
                    flash(session, "Choose a request type and enter a subject up to 180 characters.", true);
                    redirect(req, resp, null, true); return;
                }
                long id = dao.create(companyId, "RECRUITER", category, subject, body);
                flash(session, "Your request has been sent to the Job.com admin team.", false);
                redirect(req, resp, Long.toString(id), false);
            } else if ("reply".equals(action)) {
                long id = Long.parseLong(req.getParameter("conversationId"));
                if (!dao.replyAsRequester(id, "RECRUITER", companyId, body)) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                flash(session, "Your reply has been sent.", false); redirect(req, resp, Long.toString(id), false);
            } else resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown support action.");
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid support request id.");
        } catch (Exception e) {
            getServletContext().log("Recruiter support message could not be saved", e);
            flash(session, "Your message could not be sent. Please try again.", true);
            redirect(req, resp, req.getParameter("conversationId"), "create".equals(action));
        }
    }

    private Long account(HttpSession session) {
        Object value = session == null ? null : session.getAttribute("companyId");
        return value instanceof Number ? ((Number) value).longValue() : null;
    }
    private String value(HttpServletRequest req, String key) { return Optional.ofNullable(req.getParameter(key)).orElse("").trim(); }
    private boolean safeEquals(String a, String b) { return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8)); }
    private void flash(HttpSession session, String message, boolean error) { session.setAttribute("recruiterSupportFlash", message); session.setAttribute("recruiterSupportFlashError", error); }
    private void redirect(HttpServletRequest req, HttpServletResponse resp, String id, boolean compose) throws IOException {
        String url = req.getContextPath() + "/recruiter/support";
        if (compose) url += "?new=1"; else if (id != null && id.matches("\\d+")) url += "?conversationId=" + id;
        resp.sendRedirect(url);
    }
}
