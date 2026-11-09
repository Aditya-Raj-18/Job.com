package com.jobcom.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;

/** Ensures uploaded assessment PDFs can be organized by company and job role. */
public final class AssessmentResourceSchema {
    private AssessmentResourceSchema() {}

    public static Set<String> ensureCategories(Connection connection) throws SQLException {
        Set<String> columns = readColumns(connection);
        if (columns.isEmpty()) throw new SQLException("Required assessments table is unavailable.");
        addColumnIfMissing(connection, columns, "company_name", "company_name VARCHAR(120) NOT NULL DEFAULT 'General'");
        columns = readColumns(connection);
        addColumnIfMissing(connection, columns, "role_name", "role_name VARCHAR(120) NOT NULL DEFAULT 'General'");
        return readColumns(connection);
    }

    private static void addColumnIfMissing(Connection connection, Set<String> columns, String name, String definition) throws SQLException {
        if (columns.contains(name)) return;
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE assessments ADD COLUMN " + definition);
        } catch (SQLException e) {
            if (!readColumns(connection).contains(name)) throw e;
        }
    }

    private static Set<String> readColumns(Connection connection) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet result = connection.getMetaData().getColumns(connection.getCatalog(), null, "assessments", null)) {
            while (result.next()) columns.add(result.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        return columns;
    }
}
