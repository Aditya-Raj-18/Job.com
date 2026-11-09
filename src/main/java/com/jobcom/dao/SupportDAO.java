package com.jobcom.dao;

import com.jobcom.config.Db;
import java.sql.*;
import java.util.*;

/** Persistent seeker/recruiter-to-admin support requests and their two-way message threads. */
public class SupportDAO {
    public void ensureSchema() throws SQLException {
        try (Connection c = Db.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS support_conversations ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "user_id BIGINT NOT NULL,"
                    + "requester_type VARCHAR(20) NOT NULL DEFAULT 'SEEKER',"
                    + "category VARCHAR(20) NOT NULL,"
                    + "subject VARCHAR(180) NOT NULL,"
                    + "status VARCHAR(20) NOT NULL DEFAULT 'OPEN',"
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                    + "INDEX idx_support_user_updated(user_id,updated_at),"
                    + "INDEX idx_support_status_updated(status,updated_at)"
                    + ") ENGINE=InnoDB");
            if (!hasColumn(c, "support_conversations", "requester_type")) {
                s.executeUpdate("ALTER TABLE support_conversations ADD COLUMN requester_type VARCHAR(20) NOT NULL DEFAULT 'SEEKER' AFTER user_id");
            }
            s.executeUpdate("CREATE TABLE IF NOT EXISTS support_messages ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                    + "conversation_id BIGINT NOT NULL,"
                    + "sender_type VARCHAR(20) NOT NULL,"
                    + "sender_id BIGINT NOT NULL,"
                    + "body VARCHAR(4000) NOT NULL,"
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "INDEX idx_support_messages_thread(conversation_id,id),"
                    + "CONSTRAINT fk_support_message_conversation FOREIGN KEY(conversation_id) REFERENCES support_conversations(id) ON DELETE CASCADE"
                    + ") ENGINE=InnoDB");
        }
    }

    public List<Map<String, Object>> forSeeker(long userId) throws SQLException {
        return forRequester(userId, "SEEKER");
    }

    public List<Map<String, Object>> forRecruiter(long companyId) throws SQLException {
        return forRequester(companyId, "RECRUITER");
    }

    private List<Map<String, Object>> forRequester(long userId, String requesterType) throws SQLException {
        String sql = "SELECT c.id,c.subject,c.category,c.status,c.created_at,c.updated_at,"
                + "(SELECT m.body FROM support_messages m WHERE m.conversation_id=c.id ORDER BY m.id DESC LIMIT 1) AS last_message "
                + "FROM support_conversations c WHERE c.user_id=? AND (c.requester_type=? OR (?='SEEKER' AND c.requester_type IS NULL)) ORDER BY c.updated_at DESC,c.id DESC";
        try (Connection c = Db.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, userId); p.setString(2, requesterType); p.setString(3, requesterType);
            try (ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", r.getLong("id")); row.put("subject", r.getString("subject"));
                    row.put("category", r.getString("category")); row.put("status", r.getString("status"));
                    row.put("createdAt", r.getObject("created_at")); row.put("updatedAt", r.getObject("updated_at"));
                    row.put("lastMessage", r.getString("last_message")); rows.add(row);
                }
                return rows;
            }
        }
    }

    public List<Map<String, Object>> forAdmin() throws SQLException {
        try (Connection c = Db.getConnection()) {
            String userKey = column(c, "users", "id", "user_id");
            String nameKey = column(c, "users", "full_name", "name", "display_name");
            String companyKey = column(c, "companies", "id", "company_id");
            String companyNameKey = column(c, "companies", "name", "company_name", "display_name");
            String sql = "SELECT c.id,c.user_id,c.subject,c.category,c.status,c.created_at,c.updated_at,"
                + "COALESCE(u." + quote(nameKey) + ",co." + quote(companyNameKey) + ",CONCAT('Account #',c.user_id)) AS requester_name,"
                + "COALESCE(u.email,co.email,'') AS requester_email,COALESCE(c.requester_type,'SEEKER') AS requester_type,"
                + "(SELECT m.body FROM support_messages m WHERE m.conversation_id=c.id ORDER BY m.id DESC LIMIT 1) AS last_message "
                + "FROM support_conversations c LEFT JOIN users u ON COALESCE(c.requester_type,'SEEKER')='SEEKER' AND u." + quote(userKey) + "=c.user_id "
                + "LEFT JOIN companies co ON c.requester_type='RECRUITER' AND co." + quote(companyKey) + "=c.user_id ORDER BY "
                + "CASE WHEN c.status='RESOLVED' THEN 1 ELSE 0 END,c.updated_at DESC,c.id DESC";
            try (PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", r.getLong("id")); row.put("userId", r.getLong("user_id"));
                    row.put("subject", r.getString("subject")); row.put("category", r.getString("category"));
                    row.put("status", r.getString("status")); row.put("requesterName", r.getString("requester_name"));
                    row.put("requesterEmail", r.getString("requester_email")); row.put("requesterType", r.getString("requester_type"));
                    row.put("seekerName", r.getString("requester_name")); row.put("seekerEmail", r.getString("requester_email"));
                    row.put("createdAt", r.getObject("created_at"));
                    row.put("updatedAt", r.getObject("updated_at")); row.put("lastMessage", r.getString("last_message")); rows.add(row);
                }
                return rows;
            }
        }
    }

    public Map<String, Object> conversation(long id, Long seekerId) throws SQLException {
        return conversation(id, seekerId == null ? null : "SEEKER", seekerId);
    }

    public Map<String, Object> conversation(long id, String requesterType, Long requesterId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            String userKey = column(c, "users", "id", "user_id");
            String nameKey = column(c, "users", "full_name", "name", "display_name");
            String companyKey = column(c, "companies", "id", "company_id");
            String companyNameKey = column(c, "companies", "name", "company_name", "display_name");
            String sql = "SELECT c.id,c.user_id,c.subject,c.category,c.status,c.created_at,c.updated_at,COALESCE(c.requester_type,'SEEKER') AS requester_type,"
                    + "COALESCE(u." + quote(nameKey) + ",co." + quote(companyNameKey) + ",CONCAT('Account #',c.user_id)) AS requester_name,"
                    + "COALESCE(u.email,co.email,'') AS requester_email FROM support_conversations c "
                    + "LEFT JOIN users u ON COALESCE(c.requester_type,'SEEKER')='SEEKER' AND u." + quote(userKey) + "=c.user_id "
                    + "LEFT JOIN companies co ON c.requester_type='RECRUITER' AND co." + quote(companyKey) + "=c.user_id WHERE c.id=?"
                    + (requesterType == null ? "" : " AND c.requester_type=? AND c.user_id=?");
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, id); if (requesterType != null) { p.setString(2, requesterType); p.setLong(3, requesterId); }
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return null;
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", r.getLong("id")); row.put("userId", r.getLong("user_id"));
                    row.put("subject", r.getString("subject")); row.put("category", r.getString("category"));
                    row.put("status", r.getString("status")); row.put("requesterName", r.getString("requester_name"));
                    row.put("requesterEmail", r.getString("requester_email")); row.put("requesterType", r.getString("requester_type"));
                    row.put("seekerName", r.getString("requester_name")); row.put("seekerEmail", r.getString("requester_email"));
                    row.put("createdAt", r.getObject("created_at"));
                    row.put("updatedAt", r.getObject("updated_at")); return row;
                }
            }
        }
    }

    public List<Map<String, Object>> messages(long conversationId) throws SQLException {
        try (Connection c = Db.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT sender_type,sender_id,body,created_at FROM support_messages WHERE conversation_id=? ORDER BY id ASC LIMIT 1000")) {
            p.setLong(1, conversationId);
            try (ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("senderType", r.getString("sender_type")); row.put("senderId", r.getLong("sender_id"));
                    row.put("body", r.getString("body")); row.put("createdAt", r.getObject("created_at")); rows.add(row);
                }
                return rows;
            }
        }
    }

    public long create(long userId, String category, String subject, String body) throws SQLException {
        return create(userId, "SEEKER", category, subject, body);
    }

    public long create(long requesterId, String requesterType, String category, String subject, String body) throws SQLException {
        if (!Set.of("SEEKER", "RECRUITER").contains(requesterType)) throw new IllegalArgumentException("Choose a valid support account type.");
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO support_conversations(user_id,requester_type,category,subject) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    p.setLong(1, requesterId); p.setString(2, requesterType); p.setString(3, category); p.setString(4, subject); p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Support request id was not created.");
                        id = keys.getLong(1);
                    }
                }
                insertMessage(c, id, requesterType, requesterId, body);
                c.commit(); return id;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public boolean replyAsSeeker(long conversationId, long userId, String body) throws SQLException {
        return replyAsRequester(conversationId, "SEEKER", userId, body);
    }

    public boolean replyAsRequester(long conversationId, String requesterType, long requesterId, String body) throws SQLException {
        if (!Set.of("SEEKER", "RECRUITER").contains(requesterType)) throw new IllegalArgumentException("Choose a valid support account type.");
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (!lockOwnedConversation(c, conversationId, requesterType, requesterId)) { c.rollback(); return false; }
                insertMessage(c, conversationId, requesterType, requesterId, body);
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE support_conversations SET status=IF(status='RESOLVED','OPEN',status),updated_at=CURRENT_TIMESTAMP WHERE id=? AND requester_type=? AND user_id=?")) {
                    p.setLong(1, conversationId); p.setString(2, requesterType); p.setLong(3, requesterId); p.executeUpdate();
                }
                c.commit(); return true;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public boolean replyAsAdmin(long conversationId, long adminId, String body) throws SQLException {
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (!lockConversation(c, conversationId)) { c.rollback(); return false; }
                insertMessage(c, conversationId, "ADMIN", adminId, body);
                try (PreparedStatement p = c.prepareStatement(
                        "UPDATE support_conversations SET status='IN_PROGRESS',updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
                    p.setLong(1, conversationId); p.executeUpdate();
                }
                c.commit(); return true;
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public boolean setStatus(long conversationId, String status) throws SQLException {
        if (!Set.of("OPEN", "IN_PROGRESS", "RESOLVED").contains(status)) throw new IllegalArgumentException("Choose a valid support status.");
        try (Connection c = Db.getConnection(); PreparedStatement p = c.prepareStatement(
                "UPDATE support_conversations SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            p.setString(1, status); p.setLong(2, conversationId);
            if (p.executeUpdate() == 1) return true;
            try (PreparedStatement exists = c.prepareStatement("SELECT 1 FROM support_conversations WHERE id=?")) {
                exists.setLong(1, conversationId); try (ResultSet r = exists.executeQuery()) { return r.next(); }
            }
        }
    }

    public long openCount() throws SQLException {
        try (Connection c = Db.getConnection(); PreparedStatement p = c.prepareStatement(
                "SELECT COUNT(*) FROM support_conversations WHERE status<>'RESOLVED'"); ResultSet r = p.executeQuery()) {
            return r.next() ? r.getLong(1) : 0;
        }
    }

    private boolean lockOwnedConversation(Connection c, long id, String requesterType, long requesterId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM support_conversations WHERE id=? AND requester_type=? AND user_id=? FOR UPDATE")) {
            p.setLong(1, id); p.setString(2, requesterType); p.setLong(3, requesterId); try (ResultSet r = p.executeQuery()) { return r.next(); }
        }
    }

    private boolean lockConversation(Connection c, long id) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT id FROM support_conversations WHERE id=? FOR UPDATE")) {
            p.setLong(1, id); try (ResultSet r = p.executeQuery()) { return r.next(); }
        }
    }

    private void insertMessage(Connection c, long conversationId, String senderType, long senderId, String body) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO support_messages(conversation_id,sender_type,sender_id,body) VALUES(?,?,?,?)")) {
            p.setLong(1, conversationId); p.setString(2, senderType); p.setLong(3, senderId); p.setString(4, body); p.executeUpdate();
        }
    }

    private String column(Connection c, String table, String... candidates) throws SQLException {
        Set<String> available = new HashSet<>();
        try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, table, null)) {
            while (r.next()) available.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        for (String candidate : candidates) if (available.contains(candidate.toLowerCase(Locale.ROOT))) return candidate;
        throw new SQLException("Required column is missing from " + table + ".");
    }

    private String quote(String identifier) { return "`" + identifier.replace("`", "``") + "`"; }

    private boolean hasColumn(Connection c, String table, String requested) throws SQLException {
        try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, table, null)) {
            while (r.next()) if (requested.equalsIgnoreCase(r.getString("COLUMN_NAME"))) return true;
        }
        return false;
    }
}
