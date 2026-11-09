package com.jobcom.dao;

import com.jobcom.config.Db;
import com.jobcom.util.CareerFields;
import java.sql.*;
import java.util.*;

/** Shared announcement feed backed by the project's messages/notifications schema. */
public class NotificationDAO {
    public void announce(String audience, String subject, String body, long adminId) throws SQLException {
        String normalized = audience == null ? "" : audience.trim();
        if (!Set.of("Everyone", "Users", "Companies").contains(normalized)) throw new IllegalArgumentException("Choose a valid announcement audience.");
        if (subject == null || subject.isBlank() || subject.length() > 180 || body == null || body.isBlank() || body.length() > 10000)
            throw new IllegalArgumentException("Enter a subject and message within the allowed length.");

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                Set<String> messageColumns = columns(c, "messages");
                if (messageColumns.containsAll(Set.of("audience", "subject", "body"))) {
                    String idCol = optionalColumn(messageColumns, "id", "message_id");
                    String adminCol = optionalColumn(messageColumns, "admin_id", "created_by");
                    List<String> names = new ArrayList<>(List.of(col(messageColumns, "audience"), col(messageColumns, "subject"), col(messageColumns, "body")));
                    if (adminCol != null) names.add(adminCol);
                    String sql = "INSERT INTO messages(" + String.join(",", names) + ") VALUES(" + String.join(",", Collections.nCopies(names.size(), "?")) + ")";
                    long messageId;
                    try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        p.setString(1, normalized); p.setString(2, subject.trim()); p.setString(3, body.trim());
                        if (adminCol != null) p.setLong(4, adminId);
                        p.executeUpdate();
                        try (ResultSet keys = p.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Announcement was saved without a message ID."); messageId = keys.getLong(1); }
                    }
                    if (!"Companies".equals(normalized)) deliverToSeekers(c, messageId);
                } else {
                    saveLegacyAnnouncement(c, normalized, subject.trim(), body.trim(), adminId);
                }
                c.commit();
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof SQLException sql) throw sql;
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new SQLException("Announcement could not be saved.", e);
            }
        }
    }

    public List<Map<String, Object>> adminList() throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> cols = columns(c, "messages");
            if (cols.containsAll(Set.of("audience", "subject", "body"))) {
                String id = required(cols, "id", "message_id");
                String created = optionalColumn(cols, "created_at", "created_on");
                String audience = col(cols, "audience");
                String sql = "SELECT " + id + " AS id," + col(cols, "subject") + " AS subject," + audience + " AS audience," + col(cols, "body") + " AS body," + (created == null ? "NULL" : created) + " AS created_at FROM messages WHERE UPPER(" + audience + ") NOT LIKE 'JOB:%' ORDER BY " + (created == null ? id : created) + " DESC LIMIT 100";
                return readRows(c, sql);
            }
            Set<String> legacy = columns(c, "notifications");
            if (legacy.containsAll(Set.of("recipient_role", "title", "message"))) {
                String id = required(legacy, "id", "notification_id");
                String created = optionalColumn(legacy, "created_at", "created_on");
                String sql = "SELECT " + id + " AS id," + col(legacy, "title") + " AS subject," + col(legacy, "recipient_role") + " AS audience," + col(legacy, "message") + " AS body," + (created == null ? "NULL" : created) + " AS created_at FROM notifications ORDER BY " + (created == null ? id : created) + " DESC LIMIT 100";
                return readRows(c, sql);
            }
            return List.of();
        }
    }

    public List<Map<String, Object>> seekerFeed(long userId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> message = columns(c, "messages");
            if (!message.containsAll(Set.of("id", "audience", "subject", "body"))) return List.of();
            Set<String> notice = columns(c, "notifications");
            String readExpr = "0";
            if (notice.containsAll(Set.of("user_id", "message_id", "read_at"))) {
                readExpr = "CASE WHEN EXISTS (SELECT 1 FROM notifications n WHERE n.user_id=? AND n.message_id=m.id AND n.read_at IS NOT NULL) THEN 1 ELSE 0 END";
            }
            String sql = "SELECT m.id,m.subject,m.body,m.audience,m.created_at," + readExpr + " AS is_read FROM messages m WHERE LOWER(m.audience) IN ('everyone','users','all','user')";
            if (notice.containsAll(Set.of("user_id", "message_id"))) sql += " OR EXISTS (SELECT 1 FROM notifications n WHERE n.user_id=? AND n.message_id=m.id)";
            sql += " ORDER BY m.created_at DESC,m.id DESC LIMIT 100";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                int index = 1;
                if (!"0".equals(readExpr)) p.setLong(index++, userId);
                if (notice.containsAll(Set.of("user_id", "message_id"))) p.setLong(index, userId);
                return readRows(c, p);
            }
        }
    }

    public List<Map<String, Object>> companyFeed() throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> cols = columns(c, "messages");
            if (!cols.containsAll(Set.of("id", "audience", "subject", "body"))) return List.of();
            String created = optionalColumn(cols, "created_at", "created_on");
            String sql = "SELECT " + col(cols, "id") + " AS id," + col(cols, "subject") + " AS subject," + col(cols, "body") + " AS body," + col(cols, "audience") + " AS audience," + (created == null ? "NULL" : created) + " AS created_at FROM messages WHERE LOWER(" + col(cols, "audience") + ") IN ('everyone','companies','all','company') ORDER BY " + (created == null ? col(cols, "id") : created) + " DESC LIMIT 50";
            return readRows(c, sql);
        }
    }

    public int unreadCount(long userId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> notice = columns(c, "notifications");
            if (!notice.containsAll(Set.of("user_id", "message_id", "read_at"))) return 0;
            String sql = "SELECT COUNT(*) FROM notifications WHERE user_id=? AND read_at IS NULL";
            try (PreparedStatement p = c.prepareStatement(sql)) { p.setLong(1, userId); try (ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : 0; } }
        }
    }

    public void markSeekerRead(long userId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> notice = columns(c, "notifications");
            if (!notice.containsAll(Set.of("user_id", "message_id", "read_at"))) return;
            String sql = "UPDATE notifications SET read_at=CURRENT_TIMESTAMP WHERE user_id=? AND read_at IS NULL";
            try (PreparedStatement p = c.prepareStatement(sql)) { p.setLong(1, userId); p.executeUpdate(); }
        }
    }

    /** Create one persisted alert per matching active seeker for a newly approved job. */
    public int notifyMatchingSeekersForJob(long jobId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                Set<String> jobs = columns(c, "jobs");
                Set<String> companies = columns(c, "companies");
                Set<String> users = columns(c, "users");
                Set<String> messages = columns(c, "messages");
                Set<String> notices = columns(c, "notifications");
                if (!jobs.containsAll(Set.of("title", "status")) || !companies.contains("id") || !users.contains("id")
                        || !messages.containsAll(Set.of("id", "audience", "subject", "body"))
                        || !notices.containsAll(Set.of("user_id", "message_id"))) {
                    c.rollback();
                    return 0;
                }

                String jobKey = required(jobs, "id", "job_id");
                String jobTitle = col(jobs, "title");
                String jobDescription = optionalColumn(jobs, "description", "job_description", "details", "notes");
                String jobField = optionalColumn(jobs, "career_field");
                String jobCompanyId = optionalColumn(jobs, "company_id", "employer_id");
                if (jobCompanyId == null) { c.rollback(); return 0; }
                String sql = "SELECT j." + jobTitle + " AS title," + (jobDescription == null ? "''" : "j." + jobDescription) + " AS description,"
                        + (jobField == null ? "''" : "j." + jobField) + " AS career_field,j." + col(jobs, "status") + " AS job_status,j." + jobCompanyId + " AS company_id "
                        + "FROM jobs j WHERE j." + jobKey + "=? FOR UPDATE";
                String title, description, field, jobStatus;
                long companyId;
                try (PreparedStatement p = c.prepareStatement(sql)) {
                    p.setLong(1, jobId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) { c.rollback(); return 0; }
                        title = Objects.toString(r.getString("title"), "").trim();
                        description = Objects.toString(r.getString("description"), "");
                        field = Objects.toString(r.getString("career_field"), "");
                        jobStatus = Objects.toString(r.getString("job_status"), "");
                        companyId = r.getLong("company_id");
                    }
                }
                if (title.isBlank() || !Set.of("VERIFIED", "ACTIVE", "APPROVED", "OPEN").contains(jobStatus.toUpperCase(Locale.ROOT))) {
                    c.commit();
                    return 0;
                }

                String companyIdCol = required(companies, "id", "company_id");
                String companyNameCol = required(companies, "name", "company_name", "display_name");
                String companyStatusCol = optionalColumn(companies, "status", "account_status", "approval_status");
                String companySql = "SELECT " + companyNameCol + " AS company_name" + (companyStatusCol == null ? "" : "," + companyStatusCol + " AS company_status")
                        + " FROM companies WHERE " + companyIdCol + "=?";
                String companyName;
                try (PreparedStatement p = c.prepareStatement(companySql)) {
                    p.setLong(1, companyId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) { c.commit(); return 0; }
                        companyName = Objects.toString(r.getString("company_name"), "Company");
                        if (companyStatusCol != null && !Set.of("APPROVED", "ACTIVE").contains(Objects.toString(r.getString("company_status"), "").toUpperCase(Locale.ROOT))) {
                            c.commit();
                            return 0;
                        }
                    }
                }

                String audienceTag = "JOB:" + jobId;
                try (PreparedStatement p = c.prepareStatement("SELECT id FROM messages WHERE audience=? LIMIT 1")) {
                    p.setString(1, audienceTag);
                    try (ResultSet r = p.executeQuery()) {
                        if (r.next()) { c.commit(); return 0; }
                    }
                }

                String userKey = required(users, "id", "user_id");
                String userStatus = optionalColumn(users, "status", "account_status", "approval_status");
                if (userStatus == null) { c.rollback(); return 0; }
                Map<String, String> profileColumns = new LinkedHashMap<>();
                for (String name : List.of("career_field", "qualification", "profile_headline", "skills", "profile_summary")) {
                    String selected = optionalColumn(users, name);
                    if (selected != null) profileColumns.put(name, selected);
                }
                List<Long> recipients = new ArrayList<>();
                StringBuilder userSql = new StringBuilder("SELECT ").append(userKey).append(" AS user_id");
                for (Map.Entry<String, String> entry : profileColumns.entrySet()) userSql.append(',').append(entry.getValue()).append(" AS `").append(entry.getKey()).append('`');
                userSql.append(" FROM users WHERE UPPER(").append(userStatus).append(") IN ('ACTIVE','APPROVED')");
                String resolvedJobField = CareerFields.resolveJob(field, title, description);
                try (PreparedStatement p = c.prepareStatement(userSql.toString()); ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        Map<String, String> profile = new HashMap<>();
                        for (String key : List.of("career_field", "qualification", "profile_headline", "skills", "profile_summary")) {
                            profile.put(key, profileColumns.containsKey(key) ? Objects.toString(r.getString(key), "") : "");
                        }
                        String seekerField = CareerFields.resolveSeeker(profile);
                        if (CareerFields.matches(seekerField, resolvedJobField, title, description)) recipients.add(r.getLong("user_id"));
                    }
                }
                if (recipients.isEmpty()) { c.commit(); return 0; }

                long messageId;
                String messageSql = "INSERT INTO messages(audience,subject,body) VALUES(?,?,?)";
                String body = companyName + " posted a new opening for " + title + " in " + resolvedJobField + ". Open Find Jobs to review the details and apply.";
                try (PreparedStatement p = c.prepareStatement(messageSql, Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1, audienceTag);
                    p.setString(2, title.length() > 180 ? title.substring(0, 180) : title);
                    p.setString(3, body);
                    p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Job alert was saved without a message ID.");
                        messageId = keys.getLong(1);
                    }
                }
                try (PreparedStatement p = c.prepareStatement("INSERT INTO notifications(user_id,message_id) VALUES(?,?)")) {
                    for (long recipient : recipients) {
                        p.setLong(1, recipient);
                        p.setLong(2, messageId);
                        p.addBatch();
                    }
                    p.executeBatch();
                }
                c.commit();
                return recipients.size();
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof SQLException sql) throw sql;
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new SQLException("Matching seeker job alerts could not be created.", e);
            }
        }
    }

    /** Recent job alerts for the signed-in seeker's lightweight notification poll. */
    public List<Map<String, Object>> jobAlertsSince(long userId, long sinceEpochMillis) throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> notice = columns(c, "notifications");
            Set<String> message = columns(c, "messages");
            if (!notice.containsAll(Set.of("id", "user_id", "message_id"))
                    || !message.containsAll(Set.of("id", "audience", "subject", "body"))) return List.of();
            String created = optionalColumn(notice, "created_at", "created_on");
            String timestamp = created == null ? "NULL" : "n." + created;
            String sql = "SELECT n.id AS id,m.subject AS title,m.body AS body,m.audience AS audience," + timestamp + " AS created_at FROM notifications n JOIN messages m ON m.id=n.message_id WHERE n.user_id=? AND UPPER(m.audience) LIKE 'JOB:%'";
            if (created != null) sql += " AND n." + created + ">=?";
            sql += " ORDER BY n.id DESC LIMIT 50";
            List<Map<String, Object>> result = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, userId);
                if (created != null) p.setTimestamp(2, new Timestamp(sinceEpochMillis));
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        String audience = Objects.toString(r.getString("audience"), "");
                        row.put("id", r.getLong("id"));
                        row.put("title", r.getString("title"));
                        row.put("body", r.getString("body"));
                        row.put("jobId", parseJobId(audience));
                        Timestamp createdAt = r.getTimestamp("created_at");
                        row.put("createdAt", createdAt == null ? 0L : createdAt.getTime());
                        result.add(row);
                    }
                }
            }
            Collections.reverse(result);
            return result;
        }
    }

    private Long parseJobId(String audience) {
        if (audience == null || !audience.regionMatches(true, 0, "JOB:", 0, 4)) return null;
        try { return Long.parseLong(audience.substring(4)); } catch (NumberFormatException ignored) { return null; }
    }

    private void deliverToSeekers(Connection c, long messageId) throws SQLException {
        Set<String> notice = columns(c, "notifications");
        Set<String> users = columns(c, "users");
        if (!notice.containsAll(Set.of("user_id", "message_id")) || !users.contains("id")) return;
        String userId = col(users, "id");
        String status = optionalColumn(users, "status", "account_status");
        String sql = "INSERT INTO notifications(user_id,message_id) SELECT u." + userId + ",? FROM users u" + (status == null ? "" : " WHERE UPPER(u." + status + ") IN ('ACTIVE','APPROVED')");
        try (PreparedStatement p = c.prepareStatement(sql)) { p.setLong(1, messageId); p.executeUpdate(); }
    }

    private void saveLegacyAnnouncement(Connection c, String audience, String subject, String body, long adminId) throws SQLException {
        Set<String> cols = columns(c, "notifications");
        if (!cols.containsAll(Set.of("recipient_role", "title", "message"))) throw new SQLException("The announcement tables are not available in this database.");
        String id = optionalColumn(cols, "created_by", "admin_id");
        List<String> names = new ArrayList<>(List.of(col(cols, "recipient_role"), col(cols, "title"), col(cols, "message")));
        if (id != null) names.add(id);
        String sql = "INSERT INTO notifications(" + String.join(",", names) + ") VALUES(" + String.join(",", Collections.nCopies(names.size(), "?")) + ")";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, switch (audience) { case "Users" -> "USER"; case "Companies" -> "COMPANY"; default -> "ALL"; });
            p.setString(2, subject); p.setString(3, body); if (id != null) p.setLong(4, adminId); p.executeUpdate();
        }
    }

    private List<Map<String, Object>> readRows(Connection c, String sql) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { return readRows(c, p); }
    }

    private List<Map<String, Object>> readRows(Connection c, PreparedStatement p) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (ResultSet r = p.executeQuery()) {
            while (r.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", r.getObject("id")); row.put("name", r.getObject("subject"));
                String audience = Objects.toString(r.getObject("audience"), "");
                boolean jobAlert = audience.regionMatches(true, 0, "JOB:", 0, 4);
                row.put("email", jobAlert ? "New job match" : audience);
                if (jobAlert) row.put("jobId", parseJobId(audience));
                row.put("body", r.getObject("body")); row.put("status", r.getObject("created_at")); row.put("created_at", r.getObject("created_at"));
                try { row.put("isRead", r.getBoolean("is_read")); } catch (SQLException ignored) { row.put("isRead", true); }
                result.add(row);
            }
        }
        return result;
    }

    private Set<String> columns(Connection c, String table) throws SQLException {
        Set<String> result = new HashSet<>();
        try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, table, null)) { while (r.next()) result.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT)); }
        return result;
    }

    private String col(Set<String> available, String name) throws SQLException {
        if (!available.contains(name.toLowerCase(Locale.ROOT))) throw new SQLException("Required column missing: " + name);
        return "`" + name + "`";
    }

    private String required(Set<String> available, String... candidates) throws SQLException {
        String selected = optionalColumn(available, candidates);
        if (selected == null) throw new SQLException("Required announcement identifier is missing.");
        return selected;
    }

    private String optionalColumn(Set<String> available, String... candidates) {
        for (String candidate : candidates) if (available.contains(candidate.toLowerCase(Locale.ROOT))) return "`" + candidate + "`";
        return null;
    }
}
