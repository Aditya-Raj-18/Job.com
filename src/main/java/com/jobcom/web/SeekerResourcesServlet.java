package com.jobcom.web;

import com.jobcom.config.Db;
import com.jobcom.dao.AssessmentResourceSchema;
import com.jobcom.dao.JobPortalDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Seeker-only preparation resources and downloadable assessments. */
public class SeekerResourcesServlet extends HttpServlet {
    private final JobPortalDAO jobs = new JobPortalDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("seekerId") instanceof Number)) {
            resp.sendRedirect(req.getContextPath() + "/seeker/login");
            return;
        }
        try {
            long seekerId = ((Number) session.getAttribute("seekerId")).longValue();
            req.setAttribute("hasChats", !jobs.seekerChats(seekerId).isEmpty());
        } catch (Exception e) {
            getServletContext().log("Seeker chat availability could not be loaded", e);
            req.setAttribute("hasChats", false);
        }

        String type = "assessments".equals(req.getParameter("type")) || "/seeker/assessments".equals(req.getServletPath()) ? "assessments" : "preparation";
        if ("assessments".equals(type) && (req.getParameter("download") != null || req.getParameter("view") != null)) {
            serveAssessment(req, resp, req.getParameter("download") != null);
            return;
        }

        req.setAttribute("resourceType", type);
        req.setAttribute("pageTitle", "assessments".equals(type) ? "Assessment PDFs" : "Preparation");
        try {
            if ("assessments".equals(type)) {
                String company = Objects.toString(req.getParameter("company"), "").trim();
                String role = Objects.toString(req.getParameter("role"), "").trim();
                if (company.length() > 120) company = "";
                if (role.length() > 120) role = "";
                req.setAttribute("selectedCompany", company);
                req.setAttribute("selectedRole", role);
                req.setAttribute("assessmentCompanies", assessmentValues("company_name", null));
                req.setAttribute("assessmentRoles", assessmentValues("role_name", company));
                req.setAttribute("resources", listAssessments(company, role));
            } else req.setAttribute("resources", listPreparation());
        } catch (Exception e) {
            getServletContext().log("Seeker resources could not be loaded", e);
            req.setAttribute("error", "These resources could not be loaded. Please try again in a moment.");
            req.setAttribute("resources", List.of());
        }
        req.getRequestDispatcher("/WEB-INF/views/seeker/resources.jsp").forward(req, resp);
    }

    private List<Map<String, Object>> listPreparation() throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> cols = columns(c, "practice_sets");
            String id = required(cols, "id", "practice_id", "set_id", "practice_set_id");
            String title = required(cols, "title", "name");
            String url = required(cols, "url", "content_url", "link");
            String created = column(cols, "created_at", "created_on", "uploaded_at");
            String active = activeFilter(cols);
            String sql = "SELECT " + id + " AS resource_id," + title + " AS resource_title," + url + " AS resource_url," +
                    (created == null ? "NULL" : created) + " AS resource_created FROM practice_sets" +
                    (active.isEmpty() ? "" : " WHERE " + active) + " ORDER BY " + (created == null ? id : created) + " DESC";
            return query(c, sql, true);
        }
    }

    private List<String> assessmentValues(String field, String company) throws SQLException {
        if (!"company_name".equals(field) && !"role_name".equals(field)) throw new IllegalArgumentException("Invalid assessment category.");
        try (Connection c = Db.getConnection()) {
            Set<String> cols = AssessmentResourceSchema.ensureCategories(c);
            String active = activeFilter(cols);
            String sql = "SELECT DISTINCT `" + field + "` FROM assessments";
            List<String> conditions = new ArrayList<>();
            if (!active.isEmpty()) conditions.add(active);
            if ("role_name".equals(field) && company != null && !company.isBlank()) conditions.add("`company_name`=?");
            if (!conditions.isEmpty()) sql += " WHERE " + String.join(" AND ", conditions);
            sql += " ORDER BY `" + field + "`";
            List<String> values = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement(sql)) {
                if ("role_name".equals(field) && company != null && !company.isBlank()) p.setString(1, company);
                try (ResultSet r = p.executeQuery()) { while (r.next()) { String value = r.getString(1); if (value != null && !value.isBlank()) values.add(value); } }
            }
            return values;
        }
    }

    private List<Map<String, Object>> listAssessments(String company, String role) throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> cols = AssessmentResourceSchema.ensureCategories(c);
            String id = required(cols, "assessment_id", "id");
            String title = required(cols, "title", "name");
            String created = column(cols, "created_at", "uploaded_at", "created_on");
            String active = activeFilter(cols);
            String companyCol = required(cols, "company_name"), roleCol = required(cols, "role_name");
            List<String> conditions = new ArrayList<>();
            if (!active.isEmpty()) conditions.add(active);
            if (company != null && !company.isBlank()) conditions.add(companyCol + "=?");
            if (role != null && !role.isBlank()) conditions.add(roleCol + "=?");
            String sql = "SELECT " + id + " AS resource_id," + title + " AS resource_title," + companyCol + " AS resource_company," + roleCol + " AS resource_role," +
                    (created == null ? "NULL" : created) + " AS resource_created FROM assessments" +
                    (conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions)) + " ORDER BY " + companyCol + "," + roleCol + "," + (created == null ? id : created) + " DESC";
            List<Map<String, Object>> rows = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement(sql)) {
                int index = 1;
                if (company != null && !company.isBlank()) p.setString(index++, company);
                if (role != null && !role.isBlank()) p.setString(index, role);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", r.getLong("resource_id")); row.put("title", r.getString("resource_title"));
                        row.put("company", r.getString("resource_company")); row.put("role", r.getString("resource_role"));
                        row.put("created", r.getObject("resource_created")); rows.add(row);
                    }
                }
            }
            return rows;
        }
    }

    private List<Map<String, Object>> query(Connection c, String sql, boolean includeUrl) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            while (r.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", r.getLong("resource_id"));
                row.put("title", r.getString("resource_title"));
                row.put("created", r.getObject("resource_created"));
                if (includeUrl) row.put("url", r.getString("resource_url"));
                rows.add(row);
            }
        }
        return rows;
    }

    private void serveAssessment(HttpServletRequest req, HttpServletResponse resp, boolean download) throws IOException {
        String requestedId = req.getParameter(download ? "download" : "view");
        final long id;
        try { id = Long.parseLong(requestedId); }
        catch (NumberFormatException e) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

        try (Connection c = Db.getConnection()) {
            Set<String> cols = columns(c, "assessments");
            String idCol = required(cols, "assessment_id", "id");
            String pathCol = required(cols, "file_path", "document_path");
            String active = activeFilter(cols);
            String sql = "SELECT " + pathCol + " FROM assessments WHERE " + idCol + "=?" + (active.isEmpty() ? "" : " AND " + active);
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, id);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                    String relative = r.getString(1);
                    if (relative == null || !relative.matches("assessments/[a-fA-F0-9-]{36}\\.pdf")) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    Path root = uploadRoot();
                    Path file = root.resolve(relative).normalize();
                    if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    resp.setContentType("application/pdf");
                    resp.setHeader("Content-Disposition", (download ? "attachment" : "inline") + "; filename=assessment-" + id + ".pdf");
                    resp.setHeader("X-Content-Type-Options", "nosniff");
                    resp.setHeader("Cache-Control", "private, no-store");
                    resp.setContentLengthLong(Files.size(file));
                    try (InputStream in = Files.newInputStream(file); OutputStream out = resp.getOutputStream()) {
                        in.transferTo(out);
                    }
                }
            }
        } catch (SQLException e) {
            getServletContext().log("Seeker assessment download failed", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Assessment download failed");
        }
    }

    private Path uploadRoot() {
        String configured = System.getenv("JOBCOM_UPLOAD_DIR");
        String realPath = getServletContext().getRealPath("/uploads");
        Path root = configured == null || configured.isBlank()
                ? (realPath == null ? Paths.get(System.getProperty("user.home"), "Job.com", "uploads") : Paths.get(realPath))
                : Paths.get(configured);
        return root.toAbsolutePath().normalize();
    }

    private Set<String> columns(Connection c, String table) throws SQLException {
        Set<String> names = new HashSet<>();
        try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, table, null)) {
            while (r.next()) names.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        if (names.isEmpty()) throw new SQLException("Required resources table is unavailable: " + table);
        return names;
    }

    private String activeFilter(Set<String> cols) {
        String status = column(cols, "status");
        if (status != null) return "UPPER(" + status + ")='ACTIVE'";
        String active = column(cols, "active", "is_active");
        return active == null ? "" : active + "=1";
    }

    private String column(Set<String> cols, String... candidates) {
        for (String candidate : candidates) if (cols.contains(candidate.toLowerCase(Locale.ROOT))) return "`" + candidate + "`";
        return null;
    }

    private String required(Set<String> cols, String... candidates) throws SQLException {
        String found = column(cols, candidates);
        if (found == null) throw new SQLException("Resource table is missing one of: " + String.join(", ", candidates));
        return found;
    }
}
