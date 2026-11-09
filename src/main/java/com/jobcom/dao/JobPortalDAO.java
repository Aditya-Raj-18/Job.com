package com.jobcom.dao;

import com.jobcom.config.Db;
import com.jobcom.util.CareerFields;
import java.sql.*;
import java.util.*;

/** Job applications and application-scoped seeker/recruiter conversations. */
public class JobPortalDAO {
    /** Additive local upgrades so existing installs gain compensation/chat support on startup. */
    public void ensureFeatureSchema() throws SQLException {
        try (Connection c = Db.getConnection(); Statement s = c.createStatement()) {
            Set<String> userColumns = columns(c, "users");
            if (!userColumns.contains("profile_photo_path")) s.executeUpdate("ALTER TABLE users ADD COLUMN profile_photo_path VARCHAR(500) NULL");
            if (!userColumns.contains("career_field")) s.executeUpdate("ALTER TABLE users ADD COLUMN career_field VARCHAR(120) NULL");
            Set<String> jobColumns = columns(c, "jobs");
            if (!jobColumns.contains("job_type_id")) s.executeUpdate("ALTER TABLE jobs ADD COLUMN job_type_id BIGINT NULL");
            if (!jobColumns.contains("package_ctc")) s.executeUpdate("ALTER TABLE jobs ADD COLUMN package_ctc VARCHAR(180) NULL");
            if (!jobColumns.contains("career_field")) s.executeUpdate("ALTER TABLE jobs ADD COLUMN career_field VARCHAR(120) NULL");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS job_types (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(120) NOT NULL UNIQUE, active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            String jobTypeName = column(c, "job_types", "name", "job_type", "type_name");
            s.executeUpdate("INSERT IGNORE INTO job_types(" + jobTypeName + ") VALUES('Full-time'),('Internship')");

            if (columns(c, "applications").isEmpty()) {
                String jobKey = column(c, "jobs", "id", "job_id");
                String seekerKey = column(c, "users", "id", "user_id");
                s.executeUpdate("CREATE TABLE applications (application_id BIGINT AUTO_INCREMENT PRIMARY KEY, job_id BIGINT NOT NULL, user_id BIGINT NOT NULL, status VARCHAR(40) NOT NULL DEFAULT 'APPLIED', resume_path VARCHAR(500) NULL, applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, INDEX(job_id), INDEX(user_id), FOREIGN KEY(job_id) REFERENCES jobs(" + jobKey + "), FOREIGN KEY(user_id) REFERENCES users(" + seekerKey + ")) ENGINE=InnoDB");
            }

            Set<String> applicationColumns = columns(c, "applications");
            if (!applicationColumns.contains("resume_path")) s.executeUpdate("ALTER TABLE applications ADD COLUMN resume_path VARCHAR(500) NULL");
            if (!applicationColumns.contains("accepted_at")) s.executeUpdate("ALTER TABLE applications ADD COLUMN accepted_at TIMESTAMP NULL DEFAULT NULL");

            // Legacy installs sometimes used an ENUM that defaulted new applications
            // to INPROCESS. Use a flexible status column so new submissions can stay
            // APPLIED until a recruiter accepts them, while preserving existing values.
            String applicationStatus = column(c, "applications", "status", "application_status").replace("`", "");
            boolean applicationStatusIsEnum = false;
            String applicationStatusNullability = "NOT NULL";
            try (ResultSet statusColumn = s.executeQuery("SHOW COLUMNS FROM applications")) {
                while (statusColumn.next()) {
                    if (!applicationStatus.equalsIgnoreCase(statusColumn.getString("Field"))) continue;
                    String type = Optional.ofNullable(statusColumn.getString("Type")).orElse("");
                    applicationStatusIsEnum = type.toLowerCase(Locale.ROOT).startsWith("enum(");
                    applicationStatusNullability = "YES".equalsIgnoreCase(statusColumn.getString("Null")) ? "NULL" : "NOT NULL";
                    break;
                }
            }
            if (applicationStatusIsEnum) s.executeUpdate("ALTER TABLE applications MODIFY COLUMN `" + applicationStatus + "` VARCHAR(40) " + applicationStatusNullability + " DEFAULT 'APPLIED'");

            // Older installations could mark an application INPROCESS by default.
            // Only a recruiter acceptance action now stamps accepted_at; legacy
            // ACCEPTED and HIRED rows are retained as explicit selections, but
            // unmarked INPROCESS rows are treated as pending when read.
            String statusColumn = column(c, "applications", "status", "application_status");
            s.executeUpdate("UPDATE applications SET accepted_at=CURRENT_TIMESTAMP WHERE UPPER(" + statusColumn + ") IN ('ACCEPTED','HIRED') AND accepted_at IS NULL");

            // Existing installations use applications.application_id; newer copies may use id.
            // Keep this table independent of a particular legacy primary-key name.
            s.executeUpdate("CREATE TABLE IF NOT EXISTS application_messages (id BIGINT AUTO_INCREMENT PRIMARY KEY, application_id BIGINT NOT NULL, sender_type ENUM('SEEKER','RECRUITER') NOT NULL, sender_id BIGINT NOT NULL, body VARCHAR(4000) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX(application_id,created_at)) ENGINE=InnoDB");
        }
    }

    public List<Map<String, Object>> openJobs(long seekerId) throws SQLException {
        return openJobs(seekerId, "", "", null);
    }

    public List<Map<String, Object>> openJobs(long seekerId, String query, String jobType) throws SQLException {
        return openJobs(seekerId, query, jobType, null);
    }

    public List<Map<String, Object>> openJobs(long seekerId, String query, String jobType, String seekerField) throws SQLException {
        List<Map<String, Object>> jobs = new ArrayList<>();
        try (Connection c = Db.getConnection()) {
            String jobKey = column(c, "jobs", "id", "job_id");
            String companyKey = column(c, "companies", "id", "company_id");
            String seekerKey = column(c, "users", "id", "user_id");
            String posted = column(c, "jobs", "created_at", "posted_at", "created_on");
            String type = column(c, "jobs", "job_type_id");
            String typeKey = primaryKey(c, "job_types", "id", "job_type_id", "jobtype_id", "type_id");
            String typeName = column(c, "job_types", "name", "job_type", "type_name");
            String ctc = column(c, "jobs", "package_ctc", "ctc", "salary_range");
            String careerField = column(c, "jobs", "career_field");
            String description = column(c, "jobs", "description", "job_description", "details", "notes");
            String companyName = column(c, "companies", "name", "company_name", "display_name");
            String sql = "SELECT j." + jobKey + " AS job_id,j.title,j." + description + " AS description,j." + ctc + " AS package_ctc,j." + careerField + " AS career_field,j." + posted + " AS created_at,"
                    + (type == null ? "''" : "jt." + typeName) + " AS job_type,c." + companyName + " AS company_name,"
                    + "EXISTS(SELECT 1 FROM applications a WHERE a.job_id=j." + jobKey + " AND a.user_id=?) AS applied "
                    + "FROM jobs j JOIN companies c ON c." + companyKey + "=j.company_id "
                    + (type == null ? "" : "LEFT JOIN job_types jt ON jt." + typeKey + "=j." + type + " ")
                    + "WHERE UPPER(j.status) IN ('VERIFIED','ACTIVE','APPROVED','OPEN') AND UPPER(c.status) IN ('APPROVED','ACTIVE') ";
            String search = query == null ? "" : query.trim();
            String selectedType = jobType == null ? "" : jobType.trim();
            if (!search.isEmpty()) sql += "AND (j.title LIKE ? OR j." + description + " LIKE ? OR c." + companyName + " LIKE ?) ";
            if (!selectedType.isEmpty() && type != null) sql += "AND jt." + typeName + "=? ";
            sql += "ORDER BY j." + posted + " DESC,j." + jobKey + " DESC";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, seekerId);
                int index = 2;
                if (!search.isEmpty()) {
                    String pattern = "%" + search + "%";
                    p.setString(index++, pattern); p.setString(index++, pattern); p.setString(index++, pattern);
                }
                if (!selectedType.isEmpty() && type != null) p.setString(index, selectedType);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        String title = r.getString("title");
                        String descriptionText = r.getString("description");
                        String resolvedField = CareerFields.resolveJob(r.getString("career_field"), title, descriptionText);
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("id", r.getLong("job_id")); m.put("title", title);
                        m.put("description", descriptionText); m.put("packageCtc", r.getString("package_ctc"));
                        m.put("careerField", resolvedField);
                        m.put("jobType", r.getString("job_type")); m.put("company", r.getString("company_name"));
                        m.put("created", r.getObject("created_at")); m.put("applied", r.getBoolean("applied"));
                        if (seekerField == null || seekerField.isBlank() || CareerFields.matches(seekerField, resolvedField, title, descriptionText)) jobs.add(m);
                    }
                }
            }
        }
        return jobs;
    }

    public List<Map<String, Object>> seekerApplications(long seekerId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = Db.getConnection()) {
            String appKey = column(c, "applications", "id", "application_id", "app_id");
            String acceptedAt = column(c, "applications", "accepted_at");
            String jobKey = column(c, "jobs", "id", "job_id");
            String companyKey = column(c, "companies", "id", "company_id");
            String appliedAt = column(c, "applications", "applied_at", "created_at");
            String description = column(c, "jobs", "description", "job_description", "details", "notes");
            String ctc = column(c, "jobs", "package_ctc", "ctc", "salary_range");
            String companyName = column(c, "companies", "name", "company_name", "display_name");
            String type = column(c, "jobs", "job_type_id");
            String typeKey = primaryKey(c, "job_types", "id", "job_type_id", "jobtype_id", "type_id");
            String typeName = column(c, "job_types", "name", "job_type", "type_name");
            String statusExpr = "CASE WHEN UPPER(a.status) IN ('INPROCESS','IN_PROGRESS','ACCEPTED') AND a." + acceptedAt + " IS NULL THEN 'APPLIED' ELSE a.status END";
            String sql = "SELECT a." + appKey + " AS application_id," + statusExpr + " AS status,a." + appliedAt + " AS applied_at,"
                    + "j." + jobKey + " AS job_id,j.title,j." + description + " AS description,j." + ctc + " AS package_ctc,"
                    + "jt." + typeName + " AS job_type,c." + companyName + " AS company_name "
                    + "FROM applications a JOIN jobs j ON j." + jobKey + "=a.job_id "
                    + "JOIN companies c ON c." + companyKey + "=j.company_id "
                    + "LEFT JOIN job_types jt ON jt." + typeKey + "=j." + type + " "
                    + "WHERE a.user_id=? ORDER BY a." + appliedAt + " DESC,a." + appKey + " DESC";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, seekerId);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("id", r.getLong("application_id")); row.put("jobId", r.getLong("job_id"));
                        row.put("title", r.getString("title")); row.put("company", r.getString("company_name"));
                        row.put("status", applicationStatusLabel(r.getString("status"))); row.put("appliedAt", r.getObject("applied_at"));
                        row.put("jobType", r.getString("job_type")); row.put("packageCtc", r.getString("package_ctc"));
                        row.put("description", r.getString("description")); result.add(row);
                    }
                }
            }
        }
        return result;
    }

    public void updateSeekerProfile(long seekerId, Map<String, String> values) throws SQLException {
        Map<String, String[]> candidates = new LinkedHashMap<>();
        candidates.put("full_name", new String[]{"full_name", "name"});
        candidates.put("phone", new String[]{"phone"});
        candidates.put("address", new String[]{"address"});
        candidates.put("city", new String[]{"city"});
        candidates.put("state", new String[]{"state"});
        candidates.put("postal_code", new String[]{"postal_code"});
        candidates.put("qualification", new String[]{"qualification"});
        candidates.put("career_field", new String[]{"career_field"});
        candidates.put("experience_years", new String[]{"experience_years"});
        candidates.put("skills", new String[]{"skills"});
        candidates.put("profile_headline", new String[]{"profile_headline"});
        candidates.put("profile_summary", new String[]{"profile_summary"});
        candidates.put("linkedin_url", new String[]{"linkedin_url"});
        candidates.put("portfolio_url", new String[]{"portfolio_url"});
        try (Connection c = Db.getConnection()) {
            Set<String> available = columns(c, "users");
            String userKey = column(c, "users", "id", "user_id");
            List<String> assignments = new ArrayList<>();
            List<String> updateKeys = new ArrayList<>();
            for (Map.Entry<String, String[]> field : candidates.entrySet()) {
                String actual = null;
                for (String candidate : field.getValue()) if (available.contains(candidate.toLowerCase(Locale.ROOT))) { actual = candidate; break; }
                if (actual != null) { assignments.add("`" + actual + "`=?"); updateKeys.add(field.getKey()); }
            }
            if (assignments.isEmpty()) throw new SQLException("No editable profile fields are available.");
            try (PreparedStatement p = c.prepareStatement("UPDATE users SET " + String.join(",", assignments) + " WHERE " + userKey + "=?")) {
                for (int i = 0; i < updateKeys.size(); i++) {
                    String key = updateKeys.get(i);
                    String value = values.getOrDefault(key, "");
                    if ("experience_years".equals(key) && value.isBlank()) p.setNull(i + 1, Types.DECIMAL);
                    else p.setString(i + 1, value);
                }
                p.setLong(updateKeys.size() + 1, seekerId);
                if (p.executeUpdate() != 1) throw new SQLException("Seeker profile was not found.");
            }
        }
    }

    public boolean apply(long seekerId, long jobId, String resumePath) throws SQLException {
        if (resumePath == null || resumePath.isBlank()) throw new SQLException("A resume is required for a job application.");
        try (Connection c = Db.getConnection()) {
            String applicationSeekerKey = column(c, "applications", "user_id", "seeker_id", "job_seeker_id");
            String initialStatus = initialApplicationStatus(c);
            try (PreparedStatement exists = c.prepareStatement("SELECT 1 FROM applications WHERE job_id=? AND " + applicationSeekerKey + "=?")) {
                exists.setLong(1, jobId); exists.setLong(2, seekerId);
                try (ResultSet r = exists.executeQuery()) { if (r.next()) return false; }
            }
            String jobKey = column(c, "jobs", "id", "job_id");
            String companyKey = column(c, "companies", "id", "company_id");
            String sql = "INSERT INTO applications(job_id," + applicationSeekerKey + ",status,resume_path) SELECT j." + jobKey + ",?,?,? FROM jobs j JOIN companies c ON c." + companyKey + "=j.company_id WHERE j." + jobKey + "=? AND UPPER(j.status) IN ('VERIFIED','ACTIVE','APPROVED','OPEN') AND UPPER(c.status) IN ('APPROVED','ACTIVE')";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, seekerId); p.setString(2, initialStatus); p.setString(3, resumePath); p.setLong(4, jobId);
                if (p.executeUpdate() == 1) return true;
            }
        }
        throw new SQLException("Job is no longer open for applications.");
    }

    /** Choose a status accepted by both legacy ENUM schemas and the current VARCHAR schema. */
    private String initialApplicationStatus(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SHOW COLUMNS FROM applications LIKE 'status'")) {
            if (!r.next()) throw new SQLException("Required column missing from applications (status)");
            String type = r.getString("Type");
            if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("enum(")) return "APPLIED";

            // Never put a new application into the recruiter-accepted stage.
            for (String preferred : List.of("APPLIED", "SUBMITTED", "PENDING")) {
                if (type.toUpperCase(Locale.ROOT).contains("'" + preferred + "'")) return preferred;
            }
            java.util.regex.Matcher values = java.util.regex.Pattern.compile("'((?:[^'\\\\]|\\\\.)*)'").matcher(type);
            while (values.find()) {
                String candidate = values.group(1).replace("\\\\'", "'");
                if (!Set.of("INPROCESS", "IN_PROGRESS", "ACCEPTED", "REJECTED", "HIRED").contains(candidate.toUpperCase(Locale.ROOT))) return candidate;
            }
            throw new SQLException("The applications status enum needs an unaccepted status such as APPLIED or SUBMITTED.");
        }
    }
    public List<Map<String, Object>> seekerChats(long seekerId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            String appKey = column(c, "applications", "id", "application_id", "app_id");
            String jobKey = column(c, "jobs", "id", "job_id");
            String companyKey = column(c, "companies", "id", "company_id");
            String companyName = column(c, "companies", "name", "company_name", "display_name");
            String appliedAt = column(c, "applications", "applied_at", "created_at");
            String sql = "SELECT a." + appKey + " AS application_id,j." + jobKey + " AS job_id,j.title,c." + companyName + " AS company_name,a.status,a.resume_path AS resume_path,a." + appliedAt + " AS applied_at FROM applications a JOIN jobs j ON j." + jobKey + "=a.job_id JOIN companies c ON c." + companyKey + "=j.company_id WHERE a.user_id=? ORDER BY a." + appliedAt + " DESC,a." + appKey + " DESC";
            return queryChats(c, sql, seekerId, "company_name");
        }
    }

    public String recruiterApplicationResume(long applicationId, long companyId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            String appKey = column(c, "applications", "id", "application_id", "app_id");
            String jobKey = column(c, "jobs", "id", "job_id");
            String companyKey = column(c, "companies", "id", "company_id");
            String sql = "SELECT a.resume_path FROM applications a JOIN jobs j ON j." + jobKey + "=a.job_id WHERE a." + appKey + "=? AND j." + companyKey + "=?";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, applicationId); p.setLong(2, companyId);
                try (ResultSet r = p.executeQuery()) { return r.next() ? r.getString("resume_path") : null; }
            }
        }
    }
    public List<Map<String, Object>> recruiterChats(long companyId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            String appKey = column(c, "applications", "id", "application_id", "app_id");
            String jobKey = column(c, "jobs", "id", "job_id");
            String userKey = column(c, "users", "id", "user_id");
            String appliedAt = column(c, "applications", "applied_at", "created_at");
            String userName = column(c, "users", "full_name", "name", "display_name");
            String sql = "SELECT a." + appKey + " AS application_id,j." + jobKey + " AS job_id,j.title,u." + userName + " AS seeker_name,a.status,a.resume_path AS resume_path,a." + appliedAt + " AS applied_at FROM applications a JOIN jobs j ON j." + jobKey + "=a.job_id JOIN users u ON u." + userKey + "=a.user_id WHERE j.company_id=? ORDER BY a." + appliedAt + " DESC,a." + appKey + " DESC";
            return queryChats(c, sql, companyId, "seeker_name");
        }
    }

    private List<Map<String, Object>> queryChats(Connection c, String sql, long id, String otherParty) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, id);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Map<String, Object> m = new LinkedHashMap<>(); m.put("applicationId", r.getLong("application_id"));
                    m.put("jobId", r.getLong("job_id")); m.put("jobTitle", r.getString("title"));
                    m.put("otherParty", r.getString(otherParty)); m.put("status", applicationStatusLabel(r.getString("status"))); m.put("resumePath", r.getString("resume_path"));
                    m.put("appliedAt", r.getObject("applied_at")); result.add(m);
                }
            }
        }
        return result;
    }

    /** Returns a conversation only if the supplied account belongs to its application. */
    public Map<String, Object> conversation(long applicationId, Long seekerId, Long companyId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            String appKey = column(c, "applications", "id", "application_id", "app_id");
            String jobKey = column(c, "jobs", "id", "job_id");
            String companyKey = column(c, "companies", "id", "company_id");
            String userKey = column(c, "users", "id", "user_id");
            String userName = column(c, "users", "full_name", "name", "display_name");
            String companyName = column(c, "companies", "name", "company_name", "display_name");
            String sql = "SELECT a." + appKey + " AS application_id,a.user_id,j.company_id,j.title,u." + userName + " AS seeker_name,c." + companyName + " AS company_name FROM applications a JOIN jobs j ON j." + jobKey + "=a.job_id JOIN users u ON u." + userKey + "=a.user_id JOIN companies c ON c." + companyKey + "=j.company_id WHERE a." + appKey + "=? AND ((? IS NOT NULL AND a.user_id=?) OR (? IS NOT NULL AND j.company_id=?))";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, applicationId); setNullableLong(p, 2, seekerId); setNullableLong(p, 3, seekerId); setNullableLong(p, 4, companyId); setNullableLong(p, 5, companyId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return null;
                    Map<String, Object> m = new LinkedHashMap<>(); m.put("applicationId", r.getLong("application_id"));
                    m.put("jobTitle", r.getString("title")); m.put("seekerName", r.getString("seeker_name"));
                    m.put("companyName", r.getString("company_name")); return m;
                }
            }
        }
    }

    private void setNullableLong(PreparedStatement p, int index, Long value) throws SQLException {
        if (value == null) p.setNull(index, Types.BIGINT); else p.setLong(index, value);
    }

    public List<Map<String, Object>> messages(long applicationId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = Db.getConnection(); PreparedStatement p = c.prepareStatement("SELECT sender_type,sender_id,body,created_at FROM application_messages WHERE application_id=? ORDER BY id ASC LIMIT 500")) {
            p.setLong(1, applicationId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) { Map<String, Object> m = new LinkedHashMap<>(); m.put("senderType", r.getString("sender_type")); m.put("senderId", r.getLong("sender_id")); m.put("body", r.getString("body")); m.put("created", r.getObject("created_at")); result.add(m); }
            }
        }
        return result;
    }

    public void sendMessage(long applicationId, String senderType, long senderId, String body) throws SQLException {
        try (Connection c = Db.getConnection()) {
            String appKey = column(c, "applications", "id", "application_id", "app_id");
            String jobKey = column(c, "jobs", "id", "job_id");
            String sql = "INSERT INTO application_messages(application_id,sender_type,sender_id,body) SELECT a." + appKey + ",?,?,? FROM applications a JOIN jobs j ON j." + jobKey + "=a.job_id WHERE a." + appKey + "=? AND ((?='SEEKER' AND a.user_id=?) OR (?='RECRUITER' AND j.company_id=?))";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, senderType); p.setLong(2, senderId); p.setString(3, body); p.setLong(4, applicationId);
                p.setString(5, senderType); p.setLong(6, senderId); p.setString(7, senderType); p.setLong(8, senderId);
                if (p.executeUpdate() != 1) throw new SQLException("Account is not a participant in this application.");
            }
        }
    }

    private String applicationStatusLabel(String status) {
        if (status == null || status.isBlank()) return "";
        if (Set.of("SUBMITTED", "PENDING", "APPLIED").contains(status.trim().toUpperCase(Locale.ROOT))) return "APPLIED";
        if (Set.of("INPROCESS", "IN_PROGRESS", "ACCEPTED").contains(status.trim().toUpperCase(Locale.ROOT))) return "IN PROGRESS";
        return status;
    }

    private Set<String> columns(Connection c, String table) throws SQLException {
        Set<String> names = new HashSet<>();
        try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, table, null)) { while (r.next()) names.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT)); }
        return names;
    }

    private String column(Connection c, String table, String... candidates) throws SQLException {
        Set<String> available = columns(c, table);
        for (String candidate : candidates) if (available.contains(candidate.toLowerCase(Locale.ROOT))) return "`" + candidate + "`";
        if (candidates.length == 0) throw new SQLException("No candidate database columns supplied for " + table);
        throw new SQLException("Required column missing from " + table + " (expected " + String.join(", ", candidates) + ")");
    }

    private String primaryKey(Connection c, String table, String... candidates) throws SQLException {
        try (ResultSet r = c.getMetaData().getPrimaryKeys(c.getCatalog(), null, table)) {
            if (r.next()) return "`" + r.getString("COLUMN_NAME") + "`";
        }
        return column(c, table, candidates);
    }
}
