package com.jobcom.web;

import com.jobcom.dao.NotificationDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Lightweight authenticated polling endpoint for newly approved matching jobs. */
public class SeekerJobAlertsServlet extends HttpServlet {
    private final NotificationDAO notifications = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("seekerId") instanceof Number seekerId)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        long now = System.currentTimeMillis();
        long since = parseSince(req.getParameter("since"), now - 15_000L);
        since = Math.max(now - 30L * 24 * 60 * 60 * 1000, Math.min(now, since));
        try {
            List<Map<String, Object>> alerts = notifications.jobAlertsSince(seekerId.longValue(), since);
            int unread = notifications.unreadCount(seekerId.longValue());
            StringBuilder json = new StringBuilder("{\"alerts\":[");
            for (int i = 0; i < alerts.size(); i++) {
                Map<String, Object> alert = alerts.get(i);
                if (i > 0) json.append(',');
                Object jobId = alert.get("jobId");
                json.append("{\"id\":").append(number(alert.get("id")))
                        .append(",\"title\":").append(jsonString(alert.get("title")))
                        .append(",\"body\":").append(jsonString(alert.get("body")))
                        .append(",\"createdAt\":").append(number(alert.get("createdAt")))
                        .append(",\"jobUrl\":").append(jsonString(jobId == null ? "" : req.getContextPath() + "/seeker/jobs?jobId=" + jobId))
                        .append('}');
            }
            json.append("],\"unreadCount\":").append(unread).append('}');
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.getWriter().write(json.toString());
        } catch (Exception e) {
            getServletContext().log("Seeker job alerts could not be loaded", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private long parseSince(String value, long fallback) {
        try { return value == null ? fallback : Long.parseLong(value); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private long number(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private String jsonString(Object value) {
        String text = value == null ? "" : value.toString();
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> { if (ch < 0x20) out.append(String.format("\\u%04x", (int) ch)); else out.append(ch); }
            }
        }
        return out.append('"').toString();
    }
}
