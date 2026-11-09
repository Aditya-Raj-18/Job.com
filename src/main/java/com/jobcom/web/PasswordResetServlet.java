package com.jobcom.web;

import com.jobcom.dao.PasswordResetDAO;
import com.jobcom.security.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class PasswordResetServlet extends HttpServlet {
    private static final String CSRF_PREFIX = "passwordResetCsrf:";
    private static final Set<String> TYPES = Set.of("seeker", "recruiter");
    private final PasswordResetDAO dao = new PasswordResetDAO();

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String type = validType(request.getParameter("type"));
        if (type == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        HttpSession session = request.getSession();
        String key = CSRF_PREFIX + type;
        String token = (String) session.getAttribute(key);
        if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute(key, token); }
        request.setAttribute("type", type);
        request.setAttribute("csrf", token);
        request.setAttribute("adminRecoveryConfigured", dao.adminRecoveryConfigured());
        request.getRequestDispatcher("/WEB-INF/views/auth/password-reset.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String type = validType(request.getParameter("type"));
        if (type == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        HttpSession session = request.getSession(false);
        String key = CSRF_PREFIX + type;
        String token = session == null ? null : (String) session.getAttribute(key);
        if (!safeEquals(token, request.getParameter("csrf"))) {
            session = request.getSession(true);
            token = UUID.randomUUID().toString();
            session.setAttribute(key, token);
            request.setAttribute("error", "Your recovery session expired. Please try again.");
            render(request, response, type, token);
            return;
        }

        String email = value(request, "email").toLowerCase(Locale.ROOT);
        String password = request.getParameter("new_password");
        String confirmation = request.getParameter("confirm_password");
        String error = validatePassword(password, confirmation);
        if (error == null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) error = "Enter a valid email address.";
        if (error != null) {
            request.setAttribute("error", error);
            render(request, response, type, token);
            return;
        }

        try {
            String hash = PasswordUtil.hash(password);
            boolean updated;
            if ("seeker".equals(type)) {
                String phone = value(request, "phone");
                String dobValue = value(request, "date_of_birth");
                if (phone.replaceAll("\\D", "").length() < 6) {
                    request.setAttribute("error", "Enter the mobile number linked to your account.");
                    render(request, response, type, token);
                    return;
                }
                LocalDate dob;
                try { dob = LocalDate.parse(dobValue); }
                catch (DateTimeParseException e) {
                    request.setAttribute("error", "Enter a valid date of birth.");
                    render(request, response, type, token);
                    return;
                }
                updated = dao.resetSeeker(email, phone, dob, hash);
            } else if ("recruiter".equals(type)) {
                updated = dao.resetRecruiter(email, value(request, "license_number"), hash);
            } else {
                if (!dao.adminRecoveryConfigured()) {
                    request.setAttribute("error", "Admin recovery is not configured. Set JOBCOM_ADMIN_LICENSE_NUMBER or JOBCOM_ADMIN_RECOVERY_CODE in Tomcat first.");
                    render(request, response, type, token);
                    return;
                }
                updated = dao.resetAdmin(email, value(request, "recovery_number"), hash);
            }
            if (!updated) {
                request.setAttribute("error", "We could not verify those details. Check them and try again.");
                render(request, response, type, token);
                return;
            }
            session.removeAttribute(key);
            response.sendRedirect(request.getContextPath() + loginPath(type) + "?reset=1");
        } catch (Exception e) {
            getServletContext().log("Password reset failed for " + type, e);
            request.setAttribute("error", "Password reset is temporarily unavailable. Please try again later.");
            render(request, response, type, token);
        }
    }

    private void render(HttpServletRequest request, HttpServletResponse response, String type, String token) throws ServletException, IOException {
        request.setAttribute("type", type);
        request.setAttribute("csrf", token);
        request.setAttribute("adminRecoveryConfigured", dao.adminRecoveryConfigured());
        request.getRequestDispatcher("/WEB-INF/views/auth/password-reset.jsp").forward(request, response);
    }

    private String validatePassword(String password, String confirmation) {
        if (password == null || password.length() < 12) return "Use a password with at least 12 characters.";
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) return "Use a password that is 72 bytes or fewer.";
        if (!password.equals(confirmation)) return "The new passwords do not match.";
        return null;
    }

    private String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private String validType(String type) { return TYPES.contains(type) ? type : null; }

    private String loginPath(String type) { return "admin".equals(type) ? "/admin/login" : "/" + type + "/login"; }

    private boolean safeEquals(String first, String second) {
        return first != null && second != null && java.security.MessageDigest.isEqual(first.getBytes(StandardCharsets.UTF_8), second.getBytes(StandardCharsets.UTF_8));
    }
}
