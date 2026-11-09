package com.jobcom.dao;

import com.jobcom.config.Db;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Validates the account-specific recovery details and updates a password hash. */
public final class PasswordResetDAO {
    public boolean resetSeeker(String email, String phone, LocalDate dateOfBirth, String passwordHash) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            Set<String> columns = columns(connection, "users");
            String id = first(columns, "id", "user_id");
            String emailColumn = first(columns, "email");
            String phoneColumn = first(columns, "phone", "mobile", "phone_number");
            String dobColumn = first(columns, "date_of_birth", "dob", "birth_date");
            String passwordColumn = first(columns, "password_hash", "password");
            if (id == null || emailColumn == null || phoneColumn == null || dobColumn == null || passwordColumn == null) return false;

            String sql = "SELECT `" + id + "` AS recovery_id,`" + phoneColumn + "` AS recovery_phone,`" + dobColumn
                    + "` AS recovery_dob FROM users WHERE LOWER(`" + emailColumn + "`)=? LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email.toLowerCase(Locale.ROOT));
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return false;
                    if (!digits(result.getString("recovery_phone")).equals(digits(phone))) return false;
                    if (!dateOfBirth.equals(date(result.getObject("recovery_dob")))) return false;
                    return updatePassword(connection, "users", passwordColumn, id, result.getLong("recovery_id"), passwordHash);
                }
            }
        }
    }

    public boolean resetRecruiter(String email, String licenseNumber, String passwordHash) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            Set<String> columns = columns(connection, "companies");
            String id = first(columns, "id", "company_id");
            String emailColumn = first(columns, "email");
            String licenseColumn = first(columns, "license_number", "business_license_number", "license_no");
            String passwordColumn = first(columns, "password_hash", "password");
            if (id == null || emailColumn == null || licenseColumn == null || passwordColumn == null) return false;

            String sql = "SELECT `" + id + "` AS recovery_id,`" + licenseColumn + "` AS recovery_license FROM companies WHERE LOWER(`"
                    + emailColumn + "`)=? LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email.toLowerCase(Locale.ROOT));
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next() || !sameSecret(result.getString("recovery_license"), licenseNumber)) return false;
                    return updatePassword(connection, "companies", passwordColumn, id, result.getLong("recovery_id"), passwordHash);
                }
            }
        }
    }

    public boolean resetAdmin(String email, String recoveryNumber, String passwordHash) throws SQLException {
        try (Connection connection = Db.getConnection()) {
            Set<String> columns = columns(connection, "admins");
            String id = first(columns, "id", "admin_id");
            String emailColumn = first(columns, "email");
            String passwordColumn = first(columns, "password_hash", "password");
            String recoveryColumn = first(columns, "license_number", "admin_license_number", "recovery_code");
            if (id == null || emailColumn == null || passwordColumn == null) return false;

            String selectRecovery = recoveryColumn == null ? "" : ",`" + recoveryColumn + "` AS recovery_number";
            String sql = "SELECT `" + id + "` AS recovery_id" + selectRecovery + " FROM admins WHERE LOWER(`" + emailColumn + "`)=?"
                    + (columns.contains("status") ? " AND status='ACTIVE'" : "") + " LIMIT 1";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, email.toLowerCase(Locale.ROOT));
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) return false;
                    String storedNumber = recoveryColumn == null ? null : result.getString("recovery_number");
                    String configuredNumber = configuredAdminRecoveryNumber();
                    boolean valid = sameSecret(storedNumber, recoveryNumber) || sameSecret(configuredNumber, recoveryNumber);
                    if (!valid) return false;
                    return updatePassword(connection, "admins", passwordColumn, id, result.getLong("recovery_id"), passwordHash);
                }
            }
        }
    }

    public boolean adminRecoveryConfigured() {
        if (!configuredAdminRecoveryNumber().isBlank()) return true;
        try (Connection connection = Db.getConnection()) {
            Set<String> columns = columns(connection, "admins");
            String recovery = first(columns, "license_number", "admin_license_number", "recovery_code");
            if (recovery == null) return false;
            try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM admins WHERE `" + recovery + "` IS NOT NULL AND TRIM(`" + recovery + "`)<>'' LIMIT 1");
                 ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean updatePassword(Connection connection, String table, String passwordColumn, String idColumn, long id, String hash) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE `" + table + "` SET `" + passwordColumn + "`=? WHERE `" + idColumn + "`=?")) {
            statement.setString(1, hash);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    private Set<String> columns(Connection connection, String table) throws SQLException {
        Set<String> result = new HashSet<>();
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, table, null)) {
            while (columns.next()) result.add(columns.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        return result;
    }

    private String first(Set<String> columns, String... candidates) {
        for (String candidate : candidates) if (columns.contains(candidate.toLowerCase(Locale.ROOT))) return candidate;
        return null;
    }

    private String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }

    private boolean sameSecret(String stored, String supplied) {
        if (stored == null || supplied == null || stored.isBlank() || supplied.isBlank()) return false;
        byte[] left = stored.trim().toUpperCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] right = supplied.trim().toUpperCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(left, right);
    }

    private LocalDate date(Object value) {
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        if (value instanceof Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        if (value instanceof LocalDate date) return date;
        if (value == null) return null;
        String text = value.toString();
        try { return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text); }
        catch (RuntimeException ignored) { return null; }
    }

    private String configuredAdminRecoveryNumber() {
        String value = System.getenv("JOBCOM_ADMIN_LICENSE_NUMBER");
        if (value == null || value.isBlank()) value = System.getenv("JOBCOM_ADMIN_RECOVERY_CODE");
        return value == null ? "" : value.trim();
    }
}
