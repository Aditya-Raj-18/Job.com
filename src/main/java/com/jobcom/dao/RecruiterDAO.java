package com.jobcom.dao;

import com.jobcom.config.Db;
import com.jobcom.util.CareerFields;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;

/** Database access for recruiter/company accounts and company-owned job listings. */
public class RecruiterDAO {
    public Map<String, Object> authenticate(String email) throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> cols = columns(c, "companies");
            String id = required(cols, "id", "company_id");
            String name = required(cols, "name", "company_name", "display_name");
            String emailCol = required(cols, "email");
            String hash = required(cols, "password_hash", "password");
            String status = optional(cols, "status", "account_status");
            String verification = optional(cols, "verification_status", "id_verification_status");
            String sql = "SELECT " + id + " AS account_id," + name + " AS account_name," + emailCol + " AS account_email," + hash + " AS account_hash," + status + " AS account_status," + verification + " AS verification_status FROM companies WHERE " + emailCol + "=? LIMIT 1";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, email);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return null;
                    Map<String, Object> account = new LinkedHashMap<>();
                    account.put("id", r.getLong("account_id"));
                    account.put("name", r.getString("account_name"));
                    account.put("email", r.getString("account_email"));
                    account.put("hash", r.getString("account_hash"));
                    account.put("status", r.getString("account_status"));
                    account.put("verification", r.getString("verification_status"));
                    return account;
                }
            }
        }
    }

    public Map<String, Object> dashboard(long companyId) throws SQLException {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Connection c = Db.getConnection()) {
            Set<String> companyCols = columns(c, "companies");
            String companyKey = required(companyCols, "id", "company_id");
            try (PreparedStatement p = c.prepareStatement("SELECT * FROM companies WHERE " + companyKey + "=?")) {
                p.setLong(1, companyId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return null;
                    ResultSetMetaData md = r.getMetaData();
                    Map<String, Object> profile = new LinkedHashMap<>();
                    Set<String> allowed = Set.of("name", "company_name", "legal_name", "email", "contact_person", "contact_title", "phone", "website", "industry", "company_size", "license_number", "license_type", "company_registration_number", "tax_registration_number", "address", "city", "state", "country", "postal_code", "status", "verification_status", "created_at");
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        String key = md.getColumnLabel(i).toLowerCase(Locale.ROOT);
                        if (allowed.contains(key)) profile.put(key, r.getObject(i));
                    }
                    if (profile.get("name") == null) profile.put("name", profile.get("company_name"));
                    result.put("profile", profile);
                }
            }

            Set<String> jobCols = columns(c, "jobs");
            String fk = required(jobCols, "company_id", "employer_id", "recruiter_id");
            String jobKey = required(jobCols, "id", "job_id");
            String title = required(jobCols, "title", "job_title", "position");
            String status = optional(jobCols, "status", "approval_status");
            String created = optional(jobCols, "created_at", "created_on", "date_created", "posted_at");
            result.put("totalJobs", count(c, "SELECT COUNT(*) FROM jobs WHERE " + fk + "=?", companyId));
            result.put("pendingJobs", count(c, "SELECT COUNT(*) FROM jobs WHERE " + fk + "=? AND UPPER(" + status + ") IN ('PENDING','UNVERIFIED')", companyId));
            result.put("activeJobs", count(c, "SELECT COUNT(*) FROM jobs WHERE " + fk + "=? AND UPPER(" + status + ") IN ('VERIFIED','ACTIVE','APPROVED','OPEN')", companyId));

            List<Map<String, Object>> jobs = new ArrayList<>();
            String typeId = column(jobCols, "job_type_id");
            String packageCtc = column(jobCols, "package_ctc", "ctc", "salary_range");
            String jobTypeKey = primaryKey(c, "job_types", "id", "job_type_id", "jobtype_id", "type_id");
            String jobTypeName = column(columns(c, "job_types"), "name", "job_type", "type_name");
            String typeName = typeId == null ? "''" : "(SELECT jt." + jobTypeName + " FROM job_types jt WHERE jt." + jobTypeKey + "=jobs." + typeId + " LIMIT 1)";
            String sql = "SELECT " + jobKey + " AS job_id," + title + " AS job_title," + status + " AS job_status," + created + " AS job_created," + typeName + " AS job_type," + (packageCtc == null ? "''" : packageCtc) + " AS package_ctc,(SELECT COUNT(*) FROM applications a WHERE a.job_id=jobs." + jobKey + ") AS application_count FROM jobs WHERE " + fk + "=? ORDER BY " + jobKey + " DESC LIMIT 100";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, companyId);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        Map<String, Object> job = new LinkedHashMap<>();
                        job.put("id", r.getObject("job_id"));
                        job.put("title", r.getObject("job_title"));
                        job.put("status", r.getObject("job_status"));
                        job.put("created", r.getObject("job_created"));
                        job.put("jobType", r.getObject("job_type"));
                        job.put("packageCtc", r.getObject("package_ctc"));
                        job.put("applicationCount", r.getObject("application_count"));
                        jobs.add(job);
                    }
                }
            }
            result.put("jobs", jobs);
            return result;
        }
    }

    public void createJob(long companyId, String title, String description, String jobType, String packageCtc,
                          String careerField, String location, java.sql.Date lastDate) throws SQLException {
        if (!CareerFields.isValid(careerField)) throw new SQLException("Choose a valid career field for the job.");
        try (Connection c = Db.getConnection()) {
            Set<String> cols = columns(c, "jobs");
            String company = required(cols, "company_id", "employer_id", "recruiter_id");
            String titleCol = required(cols, "title", "job_title", "position");
            String descriptionCol = required(cols, "description", "job_description", "details", "notes");
            List<String> names = new ArrayList<>(List.of(company, titleCol, descriptionCol));
            List<Object> values = new ArrayList<>(Arrays.asList(companyId, title, description));
            addIfPresent(cols, names, values, location, "location", "job_location", "city");
            addIfPresent(cols, names, values, lastDate, "last_date", "application_deadline", "closing_date");
            addIfPresent(cols, names, values, careerField, "career_field");

            String recruiterEmailCol = column(cols, "recruiter_email", "contact_email", "company_email");
            if (recruiterEmailCol != null) {
                String email = companyEmail(c, companyId);
                if (email == null || email.isBlank()) throw new SQLException("Recruiter email is missing for company " + companyId);
                names.add(recruiterEmailCol); values.add(email);
            }
            String typeCol = column(cols, "job_type_id");
            if (typeCol != null) {
                names.add(typeCol); values.add(findOrCreateJobType(c, jobType));
            } else {
                // Older installations store the selected type directly on jobs instead of
                // using the job_types lookup table.
                String typeNameCol = column(cols, "job_type", "employment_type", "type");
                if (typeNameCol != null) { names.add(typeNameCol); values.add(jobType); }
            }
            String ctcCol = column(cols, "package_ctc", "ctc", "salary_range");
            if (ctcCol != null) { names.add(ctcCol); values.add(packageCtc); }
            String amountCol = column(cols, "package_amount", "salary_amount");
            if (amountCol != null) {
                names.add(amountCol);
                values.add(parseAmount(packageCtc));
            }
            String status = column(cols, "status", "approval_status");
            if (status != null) {
                String initialStatus = validInitialStatus(c, "jobs", status);
                if (initialStatus != null) { names.add(status); values.add(initialStatus); }
            }
            String sql = "INSERT INTO jobs(" + String.join(",", names) + ") VALUES(" + String.join(",", Collections.nCopies(values.size(), "?")) + ")";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                for (int i = 0; i < values.size(); i++) p.setObject(i + 1, values.get(i));
                p.executeUpdate();
            }
        }
    }

    /** Company-owned, non-deleted listings with the fields used by the recruiter workspace. */
    public List<Map<String, Object>> companyJobs(long companyId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = Db.getConnection()) {
            Set<String> cols = columns(c, "jobs");
            String company = required(cols, "company_id", "employer_id", "recruiter_id");
            String id = required(cols, "id", "job_id");
            String title = required(cols, "title", "job_title", "position");
            String status = required(cols, "status", "approval_status");
            String description = column(cols, "description", "job_description", "details", "notes");
            String location = column(cols, "location", "job_location", "city");
            String deadline = column(cols, "last_date", "application_deadline", "closing_date");
            String created = column(cols, "created_at", "created_on", "posted_at");
            String career = column(cols, "career_field");
            String packageCtc = column(cols, "package_ctc", "ctc", "salary_range");
            String packageAmount = column(cols, "package_amount", "salary_amount");
            String typeId = column(cols, "job_type_id");
            Set<String> applicationCols = columns(c, "applications");
            String applicationStatus = column(applicationCols, "status", "application_status");
            String acceptedAt = required(applicationCols, "accepted_at");
            String typeExpr = "''";
            if (typeId != null) {
                Set<String> typeCols = columns(c, "job_types");
                String typeKey = primaryKey(c, "job_types", "id", "job_type_id", "jobtype_id", "type_id");
                String typeName = required(typeCols, "name", "job_type", "type_name");
                typeExpr = "(SELECT jt." + typeName + " FROM job_types jt WHERE jt." + typeKey + "=j." + typeId + " LIMIT 1)";
            } else {
                String type = column(cols, "job_type", "employment_type", "type");
                if (type != null) typeExpr = "j." + type;
            }
            String packageExpr = packageCtc != null ? "j." + packageCtc : packageAmount != null ? "CAST(j." + packageAmount + " AS CHAR)" : "''";
            String selectedCountExpr = applicationStatus == null ? "0" : "(SELECT COUNT(*) FROM applications sa WHERE sa.job_id=j." + id + " AND UPPER(sa." + applicationStatus + ") IN ('INPROCESS','ACCEPTED','HIRED') AND sa." + acceptedAt + " IS NOT NULL)";
            String sql = "SELECT j." + id + " AS job_id,j." + title + " AS job_title," + selectOrEmpty("j", description) + " AS job_description," +
                    typeExpr + " AS job_type," + packageExpr + " AS package_ctc," + selectOrEmpty("j", career) + " AS career_field," +
                    selectOrEmpty("j", location) + " AS job_location," + selectOrEmpty("j", deadline) + " AS job_deadline," +
                    selectOrEmpty("j", created) + " AS job_created,j." + status + " AS job_status," + selectedCountExpr + " AS selected_count," +
                    "(SELECT COUNT(*) FROM applications a WHERE a.job_id=j." + id + ") AS application_count " +
                    "FROM jobs j WHERE j." + company + "=? AND UPPER(j." + status + ")<>'DELETED' ORDER BY j." + id + " DESC";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, companyId);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        Map<String, Object> job = new LinkedHashMap<>();
                        job.put("id", r.getLong("job_id")); job.put("title", r.getString("job_title"));
                        job.put("description", r.getString("job_description")); job.put("jobType", r.getString("job_type"));
                        job.put("packageCtc", r.getString("package_ctc")); job.put("careerField", r.getString("career_field"));
                        job.put("location", r.getString("job_location")); job.put("lastDate", r.getObject("job_deadline"));
                        job.put("created", r.getObject("job_created")); job.put("status", r.getString("job_status"));
                        job.put("applicationCount", r.getLong("application_count")); job.put("selectedCount", r.getLong("selected_count")); result.add(job);
                    }
                }
            }
        }
        return result;
    }

    public Map<String, Object> companyJob(long companyId, long jobId) throws SQLException {
        for (Map<String, Object> job : companyJobs(companyId)) {
            if (((Number) job.get("id")).longValue() == jobId) return job;
        }
        return null;
    }

    /** Update only an active listing owned by this company; existing approval status is preserved. */
    public boolean updateJob(long companyId, long jobId, String title, String description, String jobType,
                             String packageCtc, String careerField, String location, java.sql.Date lastDate) throws SQLException {
        if (!CareerFields.isValid(careerField)) throw new SQLException("Choose a valid career field for the job.");
        try (Connection c = Db.getConnection()) {
            Set<String> cols = columns(c, "jobs");
            String company = required(cols, "company_id", "employer_id", "recruiter_id");
            String id = required(cols, "id", "job_id");
            List<String> assignments = new ArrayList<>(); List<Object> values = new ArrayList<>();
            addUpdate(cols, assignments, values, title, "title", "job_title", "position");
            addUpdate(cols, assignments, values, description, "description", "job_description", "details", "notes");
            addUpdate(cols, assignments, values, careerField, "career_field");
            addUpdate(cols, assignments, values, location, "location", "job_location", "city");
            addUpdate(cols, assignments, values, lastDate, "last_date", "application_deadline", "closing_date");
            String typeId = column(cols, "job_type_id");
            if (typeId != null) { assignments.add(typeId + "=?"); values.add(findOrCreateJobType(c, jobType)); }
            else addUpdate(cols, assignments, values, jobType, "job_type", "employment_type", "type");
            String packageText = column(cols, "package_ctc", "ctc", "salary_range");
            if (packageText != null) { assignments.add(packageText + "=?"); values.add(packageCtc); }
            String packageAmount = column(cols, "package_amount", "salary_amount");
            BigDecimal amount = parseAmount(packageCtc);
            if (packageAmount != null && amount != null) { assignments.add(packageAmount + "=?"); values.add(amount); }
            if (assignments.isEmpty()) throw new SQLException("No editable job columns were found.");
            String status = column(cols, "status", "approval_status");
            String sql = "UPDATE jobs SET " + String.join(",", assignments) + " WHERE " + company + "=? AND " + id + "=?" +
                    (status == null ? "" : " AND UPPER(" + status + ")<>'DELETED'");
            values.add(companyId); values.add(jobId);
            try (PreparedStatement p = c.prepareStatement(sql)) {
                for (int i = 0; i < values.size(); i++) p.setObject(i + 1, values.get(i));
                return p.executeUpdate() == 1;
            }
        }
    }

    /** Soft-delete a listing so its application history and conversations remain intact. */
    public boolean deleteJob(long companyId, long jobId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> cols = columns(c, "jobs");
            String company = required(cols, "company_id", "employer_id", "recruiter_id");
            String id = required(cols, "id", "job_id");
            String status = required(cols, "status", "approval_status");
            String deleted = allowedStatus(c, "jobs", status, "DELETED");
            try (PreparedStatement p = c.prepareStatement("UPDATE jobs SET " + status + "=? WHERE " + company + "=? AND " + id + "=? AND UPPER(" + status + ")<>'DELETED'")) {
                p.setString(1, deleted); p.setLong(2, companyId); p.setLong(3, jobId); return p.executeUpdate() == 1;
            }
        }
    }

    public List<Map<String, Object>> applicantsForJob(long companyId, long jobId) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Connection c = Db.getConnection()) {
            Set<String> appCols = columns(c, "applications"), userCols = columns(c, "users"), jobCols = columns(c, "jobs");
            String appId = required(appCols, "id", "application_id", "app_id");
            String userId = required(userCols, "id", "user_id");
            String jobIdCol = required(jobCols, "id", "job_id");
            String company = required(jobCols, "company_id", "employer_id", "recruiter_id");
            String seekerKey = required(appCols, "user_id", "seeker_id", "job_seeker_id");
            String name = required(userCols, "full_name", "name", "display_name");
            String status = required(appCols, "status", "application_status");
            String acceptedAt = required(appCols, "accepted_at");
            String applied = column(appCols, "applied_at", "created_at");
            String headline = column(userCols, "profile_headline", "headline", "position");
            String career = column(userCols, "career_field");
            String resume = column(appCols, "resume_path");
            String statusExpr = "CASE WHEN UPPER(a." + status + ") IN ('INPROCESS','IN_PROGRESS','ACCEPTED') AND a." + acceptedAt + " IS NULL THEN 'APPLIED' ELSE a." + status + " END";
            String sql = "SELECT a." + appId + " AS application_id,u." + userId + " AS seeker_id,u." + name + " AS seeker_name," +
                    selectOrEmpty("u", headline) + " AS headline," + selectOrEmpty("u", career) + " AS career_field," + statusExpr + " AS application_status," +
                    selectOrEmpty("a", applied) + " AS applied_at," + selectOrEmpty("a", resume) + " AS resume_path,a." + acceptedAt + " AS accepted_at " +
                    "FROM applications a JOIN users u ON u." + userId + "=a." + seekerKey + " JOIN jobs j ON j." + jobIdCol + "=a.job_id " +
                    "WHERE j." + company + "=? AND j." + jobIdCol + "=? ORDER BY a." + (applied == null ? appId : applied) + " DESC,a." + appId + " DESC";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, companyId); p.setLong(2, jobId);
                try (ResultSet r = p.executeQuery()) {
                    while (r.next()) {
                        Map<String, Object> applicant = new LinkedHashMap<>();
                        applicant.put("applicationId", r.getLong("application_id")); applicant.put("seekerId", r.getLong("seeker_id"));
                        applicant.put("name", r.getString("seeker_name")); applicant.put("headline", r.getString("headline"));
                        applicant.put("careerField", r.getString("career_field")); applicant.put("status", r.getString("application_status"));
                        applicant.put("appliedAt", r.getObject("applied_at")); applicant.put("acceptedAt", r.getObject("accepted_at"));
                        applicant.put("resumePath", r.getString("resume_path")); result.add(applicant);
                    }
                }
            }
        }
        return result;
    }

    /** Applicants accepted into the selection stage or hired for a company-owned job. */
    public List<Map<String, Object>> selectedApplicantsForJob(long companyId, long jobId) throws SQLException {
        List<Map<String, Object>> selected = new ArrayList<>();
        for (Map<String, Object> applicant : applicantsForJob(companyId, jobId)) {
            String status = Objects.toString(applicant.get("status"), "");
            if (applicant.get("acceptedAt") != null && ("INPROCESS".equalsIgnoreCase(status) || "ACCEPTED".equalsIgnoreCase(status) || "HIRED".equalsIgnoreCase(status))) selected.add(applicant);
        }
        return selected;
    }

    /** Applicant contact and career details; government identity data is intentionally excluded. */
    public Map<String, Object> applicantForJob(long companyId, long jobId, long applicationId) throws SQLException {
        try (Connection c = Db.getConnection()) {
            Set<String> appCols = columns(c, "applications"), userCols = columns(c, "users"), jobCols = columns(c, "jobs");
            String appId = required(appCols, "id", "application_id", "app_id");
            String userId = required(userCols, "id", "user_id");
            String jobIdCol = required(jobCols, "id", "job_id");
            String company = required(jobCols, "company_id", "employer_id", "recruiter_id");
            String seekerKey = required(appCols, "user_id", "seeker_id", "job_seeker_id");
            String jobTitle = required(jobCols, "title", "job_title", "position");
            String applied = column(appCols, "applied_at", "created_at");
            String status = required(appCols, "status", "application_status");
            String acceptedAt = required(appCols, "accepted_at");
            String name = required(userCols, "full_name", "name", "display_name");
            String statusExpr = "CASE WHEN UPPER(a." + status + ") IN ('INPROCESS','IN_PROGRESS','ACCEPTED') AND a." + acceptedAt + " IS NULL THEN 'APPLIED' ELSE a." + status + " END";
            String sql = "SELECT a." + appId + " AS application_id," + statusExpr + " AS application_status," + selectOrEmpty("a", applied) + " AS applied_at," +
                    "j." + jobTitle + " AS job_title," + selectOrEmpty("u", name) + " AS full_name," +
                    selectOrEmpty("u", column(userCols, "email")) + " AS email," + selectOrEmpty("u", column(userCols, "phone")) + " AS phone," +
                    selectOrEmpty("u", column(userCols, "date_of_birth")) + " AS date_of_birth," + selectOrEmpty("u", column(userCols, "address")) + " AS address," +
                    selectOrEmpty("u", column(userCols, "city")) + " AS city," + selectOrEmpty("u", column(userCols, "state")) + " AS state," +
                    selectOrEmpty("u", column(userCols, "postal_code")) + " AS postal_code," + selectOrEmpty("u", column(userCols, "qualification", "highest_qualification")) + " AS qualification," +
                    selectOrEmpty("u", column(userCols, "career_field")) + " AS career_field," + selectOrEmpty("u", column(userCols, "experience_years", "experience")) + " AS experience_years," +
                    selectOrEmpty("u", column(userCols, "skills", "key_skills")) + " AS skills," + selectOrEmpty("u", column(userCols, "profile_headline", "headline", "position")) + " AS headline," +
                    selectOrEmpty("u", column(userCols, "profile_summary", "summary", "about")) + " AS profile_summary," + selectOrEmpty("u", column(userCols, "linkedin_url")) + " AS linkedin_url," +
                    selectOrEmpty("u", column(userCols, "portfolio_url", "website")) + " AS portfolio_url," + selectOrEmpty("a", column(appCols, "resume_path")) + " AS resume_path " +
                    "FROM applications a JOIN users u ON u." + userId + "=a." + seekerKey + " JOIN jobs j ON j." + jobIdCol + "=a.job_id " +
                    "WHERE j." + company + "=? AND j." + jobIdCol + "=? AND a." + appId + "=?";
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setLong(1, companyId); p.setLong(2, jobId); p.setLong(3, applicationId);
                try (ResultSet r = p.executeQuery()) {
                    if (!r.next()) return null;
                    Map<String, Object> applicant = new LinkedHashMap<>();
                    for (String key : List.of("application_id", "application_status", "applied_at", "job_title", "full_name", "email", "phone", "date_of_birth", "address", "city", "state", "postal_code", "qualification", "career_field", "experience_years", "skills", "headline", "profile_summary", "linkedin_url", "portfolio_url", "resume_path")) applicant.put(key, r.getObject(key));
                    return applicant;
                }
            }
        }
    }

    public boolean updateApplicationStatus(long companyId, long jobId, long applicationId, String requestedStatus) throws SQLException {
        if (!Set.of("INPROCESS", "REJECTED", "HIRED").contains(requestedStatus)) throw new SQLException("Invalid applicant status.");
        try (Connection c = Db.getConnection()) {
            Set<String> appCols = columns(c, "applications"), jobCols = columns(c, "jobs");
            String appId = required(appCols, "id", "application_id", "app_id");
            String status = required(appCols, "status", "application_status");
            String acceptedAt = required(appCols, "accepted_at");
            String jobIdCol = required(jobCols, "id", "job_id");
            String company = required(jobCols, "company_id", "employer_id", "recruiter_id");
            String allowed = allowedStatus(c, "applications", status, requestedStatus);
            String acceptedUpdate = "INPROCESS".equals(requestedStatus) ? "a." + acceptedAt + "=CURRENT_TIMESTAMP" : "REJECTED".equals(requestedStatus) ? "a." + acceptedAt + "=NULL" : "a." + acceptedAt + "=a." + acceptedAt;
            String transitionGuard = "HIRED".equals(requestedStatus)
                    ? " AND a." + acceptedAt + " IS NOT NULL AND UPPER(a." + status + ") IN ('INPROCESS','ACCEPTED')"
                    : "";
            String sql = "UPDATE applications a JOIN jobs j ON j." + jobIdCol + "=a.job_id SET a." + status + "=?," + acceptedUpdate + " WHERE a." + appId + "=? AND j." + jobIdCol + "=? AND j." + company + "=?" + transitionGuard;
            try (PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, allowed); p.setLong(2, applicationId); p.setLong(3, jobId); p.setLong(4, companyId); return p.executeUpdate() == 1;
            }
        }
    }

    /** Sends one recruiter announcement to every selected applicant chat atomically. */
    public int sendMessageToSelectedApplicants(long companyId, long jobId, String message) throws SQLException {
        String body = message == null ? "" : message.trim();
        if (body.isEmpty() || body.length() > 2000) throw new SQLException("Enter a message of 1 to 2000 characters.");
        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                Set<String> appCols = columns(c, "applications"), jobCols = columns(c, "jobs");
                String appId = required(appCols, "id", "application_id", "app_id");
                String status = required(appCols, "status", "application_status");
                String acceptedAt = required(appCols, "accepted_at");
                String jobIdCol = required(jobCols, "id", "job_id");
                String company = required(jobCols, "company_id", "employer_id", "recruiter_id");
                String selectionSql = "SELECT a." + appId + " FROM applications a JOIN jobs j ON j." + jobIdCol + "=a.job_id " +
                        "WHERE j." + company + "=? AND j." + jobIdCol + "=? AND UPPER(a." + status + ") IN ('INPROCESS','ACCEPTED','HIRED') AND a." + acceptedAt + " IS NOT NULL " +
                        "ORDER BY a." + appId + " FOR UPDATE";
                List<Long> applicationIds = new ArrayList<>();
                try (PreparedStatement p = c.prepareStatement(selectionSql)) {
                    p.setLong(1, companyId); p.setLong(2, jobId);
                    try (ResultSet r = p.executeQuery()) { while (r.next()) applicationIds.add(r.getLong(1)); }
                }
                if (applicationIds.isEmpty()) { c.rollback(); return 0; }
                try (PreparedStatement insert = c.prepareStatement("INSERT INTO application_messages(application_id,sender_type,sender_id,body) VALUES(?,'RECRUITER',?,?)")) {
                    for (Long applicationId : applicationIds) {
                        insert.setLong(1, applicationId); insert.setLong(2, companyId); insert.setString(3, body); insert.addBatch();
                    }
                    insert.executeBatch();
                }
                c.commit();
                return applicationIds.size();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void addUpdate(Set<String> cols, List<String> assignments, List<Object> values, Object value, String... candidates) {
        String col = column(cols, candidates);
        if (col != null) { assignments.add(col + "=?"); values.add(value); }
    }

    private String selectOrEmpty(String alias, String quotedColumn) { return quotedColumn == null ? "''" : alias + "." + quotedColumn; }

    private String allowedStatus(Connection c, String table, String quotedColumn, String requested) throws SQLException {
        String rawColumn = quotedColumn.replace("`", "");
        try (PreparedStatement p = c.prepareStatement("SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?")) {
            p.setString(1, table); p.setString(2, rawColumn);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("Status column is missing from " + table + ".");
                String type = Optional.ofNullable(r.getString("COLUMN_TYPE")).orElse("");
                if (!type.regionMatches(true, 0, "enum(", 0, 5)) return requested;
                java.util.regex.Matcher values = java.util.regex.Pattern.compile("'((?:[^'\\\\]|\\\\.)*)'").matcher(type);
                while (values.find()) if (requested.equalsIgnoreCase(values.group(1).replace("\\\\'", "'"))) return values.group(1).replace("\\\\'", "'");
                throw new SQLException("The " + table + " status field does not support " + requested + ".");
            }
        }
    }

    private void addIfPresent(Set<String> cols, List<String> names, List<Object> values, Object value, String... candidates) {
        String col = column(cols, candidates);
        if (col != null) { names.add(col); values.add(value); }
    }

    private String companyEmail(Connection c, long companyId) throws SQLException {
        Set<String> cols = columns(c, "companies");
        String id = required(cols, "id", "company_id");
        String email = required(cols, "email", "company_email");
        try (PreparedStatement p = c.prepareStatement("SELECT " + email + " FROM companies WHERE " + id + "=?")) {
            p.setLong(1, companyId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getString(1) : null; }
        }
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String numeric = raw.trim().replace(",", "");
        if (!numeric.matches("[₹$]?\\s*\\d+(\\.\\d+)?")) return null;
        numeric = numeric.replaceAll("[^0-9.]", "");
        try { return new BigDecimal(numeric); } catch (NumberFormatException ignored) { return null; }
    }

    /** Return a status accepted by this installation's enum; otherwise leave the DB default. */
    private String validInitialStatus(Connection c, String table, String quotedColumn) throws SQLException {
        String rawColumn = quotedColumn.replace("`", "");
        String sql = "SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, table);
            p.setString(2, rawColumn);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                String type = Optional.ofNullable(r.getString("COLUMN_TYPE")).orElse("");
                if (type.regionMatches(true, 0, "enum(", 0, 5)) {
                    boolean allowsPending = false;
                    java.util.regex.Matcher values = java.util.regex.Pattern.compile("'([^']*)'").matcher(type);
                    while (values.find()) {
                        String allowed = values.group(1);
                        if ("UNVERIFIED".equalsIgnoreCase(allowed)) return allowed;
                        if ("PENDING".equalsIgnoreCase(allowed)) allowsPending = true;
                    }
                    return allowsPending ? "PENDING" : null;
                }
                return "PENDING";
            }
        }
    }

    private long findOrCreateJobType(Connection c, String value) throws SQLException {
        Set<String> cols = columns(c, "job_types");
        String id = primaryKey(c, "job_types", "id", "job_type_id", "jobtype_id", "type_id");
        String name = required(cols, "name", "job_type", "type_name");
        String normalized = normalizeJobType(value);
        String normalizedColumn = "LOWER(REPLACE(REPLACE(" + name + ",'-',' '),'_',' '))";
        try (PreparedStatement p = c.prepareStatement("SELECT " + id + " FROM job_types WHERE " + normalizedColumn + "=? ORDER BY " + id + " LIMIT 1")) {
            p.setString(1, normalized);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return r.getLong(1);
            }
        }

        String active = column(cols, "active", "is_active");
        String insert = "INSERT INTO job_types(" + name + (active == null ? "" : "," + active) + ") VALUES(?" + (active == null ? "" : ",TRUE") + ")";
        try (PreparedStatement p = c.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, "Internship".equalsIgnoreCase(value) ? "Internship" : "Full-time");
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        } catch (SQLIntegrityConstraintViolationException duplicate) {
            // Another request may have inserted the same lookup value concurrently.
        }
        try (PreparedStatement p = c.prepareStatement("SELECT " + id + " FROM job_types WHERE " + normalizedColumn + "=? ORDER BY " + id + " LIMIT 1")) {
            p.setString(1, normalized);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) return r.getLong(1);
            }
        }
        throw new SQLException("Could not resolve job type in job_types: " + value);
    }

    private String normalizeJobType(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }

    private long count(Connection c, String sql, long companyId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setLong(1, companyId);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getLong(1); }
        }
    }

    private Set<String> columns(Connection c, String table) throws SQLException {
        Set<String> cols = new HashSet<>();
        try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, table, null)) {
            while (r.next()) cols.add(r.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
        }
        if (cols.isEmpty()) throw new SQLException("Required database table is unavailable: " + table);
        return cols;
    }

    private String column(Set<String> cols, String... candidates) {
        for (String candidate : candidates) if (cols.contains(candidate.toLowerCase(Locale.ROOT))) return "`" + candidate + "`";
        return null;
    }

    private String required(Set<String> cols, String... candidates) throws SQLException {
        String found = column(cols, candidates);
        if (found == null) throw new SQLException("Company/job table is missing a required column (expected one of " + String.join(", ", candidates) + ")");
        return found;
    }

    private String primaryKey(Connection c, String table, String... candidates) throws SQLException {
        try (ResultSet r = c.getMetaData().getPrimaryKeys(c.getCatalog(), null, table)) {
            if (r.next()) return "`" + r.getString("COLUMN_NAME") + "`";
        } catch (SQLException ignored) {
            // Use the known schema variants below when the database metadata provider is limited.
        }
        return required(columns(c, table), candidates);
    }

    private String optional(Set<String> cols, String... candidates) {
        String found = column(cols, candidates);
        return found == null ? "''" : found;
    }
}
