package com.jobcom.web;

import com.jobcom.dao.RecruiterDAO;
import com.jobcom.security.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class RecruiterLoginServlet extends HttpServlet {
    private static final String CSRF = "recruiterCsrf";
    private final RecruiterDAO dao = new RecruiterDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("1".equals(req.getParameter("logout"))) {
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            resp.sendRedirect(req.getContextPath() + "/recruiter/login?loggedOut=1");
            return;
        }
        HttpSession existing = req.getSession(false);
        if (existing != null && existing.getAttribute("companyId") != null) {
            resp.sendRedirect(req.getContextPath() + "/recruiter");
            return;
        }
        HttpSession session = req.getSession();
        String token = (String) session.getAttribute(CSRF);
        if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute(CSRF, token); }
        req.setAttribute("csrf", token);
        req.getRequestDispatcher("/WEB-INF/views/auth/recruiter-login.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null || !safeEquals((String) session.getAttribute(CSRF), req.getParameter("csrf"))) {
            // A login page may stay open across a Tomcat restart, session expiry,
            // or sign-out in another tab. Recover with a fresh session-bound form
            // instead of exposing Tomcat's raw 403 page.
            session = req.getSession(true);
            String refreshedToken = UUID.randomUUID().toString();
            session.setAttribute(CSRF, refreshedToken);
            req.setAttribute("csrf", refreshedToken);
            req.setAttribute("error", "Your sign-in session expired. Please try again.");
            req.getRequestDispatcher("/WEB-INF/views/auth/recruiter-login.jsp").forward(req, resp);
            return;
        }
        try {
            String email = Optional.ofNullable(req.getParameter("email")).orElse("").trim().toLowerCase(Locale.ROOT);
            Map<String, Object> account = dao.authenticate(email);
            if (account == null || !PasswordUtil.verify(req.getParameter("password"), (String) account.get("hash"))) {
                req.setAttribute("error", "The email or password is incorrect.");
            } else {
                String status = Objects.toString(account.get("status"), "").toUpperCase(Locale.ROOT);
                String verification = Objects.toString(account.get("verification"), "").toUpperCase(Locale.ROOT);
                if (Set.of("REJECTED", "BLOCKED").contains(status) || "REJECTED".equals(verification)) {
                    req.setAttribute("error", "This company account is not approved. Please contact the Job.com administrator.");
                } else if (!(Set.of("ACTIVE", "APPROVED").contains(status) && (verification.isBlank() || Set.of("ACTIVE", "APPROVED").contains(verification)))) {
                    req.setAttribute("error", "Your company account is awaiting administrator approval. Please sign in after it has been approved.");
                } else {
                    req.changeSessionId();
                    session.removeAttribute(CSRF);
                    session.setAttribute("companyId", account.get("id"));
                    session.setAttribute("companyName", account.get("name"));
                    resp.sendRedirect(req.getContextPath() + "/recruiter");
                    return;
                }
            }
        } catch (Exception e) {
            getServletContext().log("Recruiter sign in failed", e);
            req.setAttribute("error", "Sign in failed. Please check your company account or try again later.");
        }
        req.setAttribute("csrf", session.getAttribute(CSRF));
        req.getRequestDispatcher("/WEB-INF/views/auth/recruiter-login.jsp").forward(req, resp);
    }

    private boolean safeEquals(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
