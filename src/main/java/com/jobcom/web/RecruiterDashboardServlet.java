package com.jobcom.web;

import com.jobcom.dao.RecruiterDAO;
import com.jobcom.dao.JobPortalDAO;
import com.jobcom.dao.NotificationDAO;
import com.jobcom.util.CareerFields;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

public class RecruiterDashboardServlet extends HttpServlet {
    private static final String CSRF = "recruiterDashboardCsrf";
    private final RecruiterDAO dao = new RecruiterDAO();
    private final JobPortalDAO jobPortal = new JobPortalDAO();
    private final NotificationDAO notifications = new NotificationDAO();

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("companyId") == null) {
            resp.sendRedirect(req.getContextPath() + "/recruiter/login");
            return;
        }
        try {
            long companyId = ((Number) session.getAttribute("companyId")).longValue();
            Map<String, Object> dashboard = dao.dashboard(companyId);
            if (dashboard == null) {
                session.invalidate();
                resp.sendRedirect(req.getContextPath() + "/recruiter/login?accountUnavailable=1");
                return;
            }
            req.setAttribute("profile", dashboard.get("profile"));
            req.setAttribute("totalJobs", dashboard.get("totalJobs"));
            req.setAttribute("pendingJobs", dashboard.get("pendingJobs"));
            req.setAttribute("activeJobs", dashboard.get("activeJobs"));
            req.setAttribute("jobs", dashboard.get("jobs"));
            req.setAttribute("chats",jobPortal.recruiterChats(companyId));
            req.setAttribute("companyJobs", dao.companyJobs(companyId));
            req.setAttribute("announcements", notifications.companyFeed());
        } catch (Exception e) {
            getServletContext().log("Recruiter dashboard could not be loaded", e);
            req.setAttribute("error", "Dashboard data could not be loaded. Please check the database schema.");
            req.setAttribute("jobs", List.of());
            req.setAttribute("announcements", List.of());
        }
        String token = (String) session.getAttribute(CSRF);
        if (token == null) { token = UUID.randomUUID().toString(); session.setAttribute(CSRF, token); }
        req.setAttribute("csrf", token);
        req.setAttribute("companyName", session.getAttribute("companyName"));
        String section = req.getParameter("section");
        if (section == null || !Set.of("my-jobs", "profile", "post-job", "job-detail", "applicants", "applicant-list", "applicant-detail", "selected-applicants").contains(section)) section = "dashboard";
        long companyId = ((Number) session.getAttribute("companyId")).longValue();
        long jobId = positiveId(req.getParameter("jobId"));
        long applicationId = positiveId(req.getParameter("applicationId"));
        try {
            if ("job-detail".equals(section)) {
                Map<String, Object> job = jobId == 0 ? null : dao.companyJob(companyId, jobId);
                if (job == null) { req.setAttribute("error", "That job could not be found in your company listings."); section = "my-jobs"; }
                else req.setAttribute("job", job);
            } else if ("applicants".equals(section)) {
                req.setAttribute("companyJobs", dao.companyJobs(companyId));
            } else if ("applicant-list".equals(section)) {
                Map<String, Object> job = jobId == 0 ? null : dao.companyJob(companyId, jobId);
                if (job == null) { req.setAttribute("error", "That job could not be found in your company listings."); section = "applicants"; }
                else {
                    req.setAttribute("job", job);
                    List<Map<String, Object>> applicants = dao.applicantsForJob(companyId, jobId);
                    applicants.forEach(applicant -> applicant.put("statusLabel", displayStatus(applicant.get("status"))));
                    req.setAttribute("applicants", applicants);
                }
            } else if ("selected-applicants".equals(section) && jobId > 0) {
                Map<String, Object> job = dao.companyJob(companyId, jobId);
                if (job == null) { req.setAttribute("error", "That job could not be found in your company listings."); }
                else {
                    List<Map<String, Object>> selected = dao.selectedApplicantsForJob(companyId, jobId);
                    selected.forEach(applicant -> applicant.put("statusLabel", displayStatus(applicant.get("status"))));
                    req.setAttribute("job", job);
                    req.setAttribute("selectedApplicants", selected);
                }
            } else if ("applicant-detail".equals(section)) {
                Map<String, Object> job = jobId == 0 ? null : dao.companyJob(companyId, jobId);
                Map<String, Object> applicant = job == null || applicationId == 0 ? null : dao.applicantForJob(companyId, jobId, applicationId);
                if (job == null || applicant == null) { req.setAttribute("error", "That applicant could not be found for this job."); section = "applicants"; }
                else { applicant.put("statusLabel", displayStatus(applicant.get("application_status"))); req.setAttribute("job", job); req.setAttribute("applicant", applicant); }
            }
        } catch (Exception e) {
            getServletContext().log("Recruiter job or applicant view could not be loaded", e);
            req.setAttribute("error", "Job or applicant details could not be loaded. Please try again.");
        }
        req.setAttribute("view", section);
        req.setAttribute("careerFields", CareerFields.all());
        req.getRequestDispatcher("/WEB-INF/views/recruiter/dashboard.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("companyId") == null) {
            resp.sendRedirect(req.getContextPath() + "/recruiter/login");
            return;
        }
        if (!safeEquals((String) session.getAttribute(CSRF), req.getParameter("csrf"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid form token");
            return;
        }
        String action = value(req, "action");
        long companyId = ((Number) session.getAttribute("companyId")).longValue();
        if ("update-job".equals(action)) {
            long jobId = positiveId(value(req, "jobId"));
            try {
                String title = value(req, "title"), description = value(req, "description"), jobType = value(req, "jobType");
                String packageCtc = value(req, "packageCtc"), careerField = value(req, "careerField"), location = value(req, "location");
                LocalDate lastDate = LocalDate.parse(value(req, "lastDate"));
                if (jobId == 0 || title.isBlank() || title.length() > 180 || description.isBlank() || location.isBlank() || location.length() > 180 ||
                        !Set.of("Internship", "Full-time").contains(jobType) || !CareerFields.isValid(careerField) || packageCtc.length() > 180 || lastDate.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Enter valid job details and a current or future application deadline.");
                }
                if (!dao.updateJob(companyId, jobId, title, description, jobType, packageCtc, careerField, location, java.sql.Date.valueOf(lastDate))) {
                    throw new IllegalArgumentException("That job could not be updated. It may have been removed already.");
                }
                session.setAttribute("recruiterFlashSuccess", "Job details saved.");
            } catch (IllegalArgumentException e) { session.setAttribute("recruiterFlashError", e.getMessage()); }
            catch (Exception e) { getServletContext().log("Recruiter job could not be updated", e); session.setAttribute("recruiterFlashError", "Job details could not be saved. Please try again."); }
            resp.sendRedirect(req.getContextPath() + "/recruiter?section=job-detail&jobId=" + jobId);
            return;
        }
        if ("delete-job".equals(action)) {
            long jobId = positiveId(value(req, "jobId"));
            try {
                if (jobId == 0 || !dao.deleteJob(companyId, jobId)) throw new IllegalArgumentException("That job could not be removed.");
                session.setAttribute("recruiterFlashSuccess", "Job removed from your active listings. Its application history has been kept.");
            } catch (Exception e) { getServletContext().log("Recruiter job could not be removed", e); session.setAttribute("recruiterFlashError", e instanceof IllegalArgumentException ? e.getMessage() : "Job could not be removed. Please try again."); }
            resp.sendRedirect(req.getContextPath() + "/recruiter?section=my-jobs");
            return;
        }
        if ("send-selected-message".equals(action)) {
            long jobId = positiveId(value(req, "jobId"));
            String message = value(req, "message");
            try {
                if (jobId == 0 || message.isBlank() || message.length() > 2000 || dao.companyJob(companyId, jobId) == null) {
                    throw new IllegalArgumentException("Enter a message up to 2000 characters for one of your listed jobs.");
                }
                int recipients = dao.sendMessageToSelectedApplicants(companyId, jobId, message);
                if (recipients == 0) throw new IllegalArgumentException("There are no selected applicants for this job yet.");
                session.setAttribute("recruiterFlashSuccess", "Message sent to all " + recipients + " selected applicants in their Job.com chats.");
            } catch (IllegalArgumentException e) { session.setAttribute("recruiterFlashError", e.getMessage()); }
            catch (Exception e) { getServletContext().log("Message could not be sent to selected applicants", e); session.setAttribute("recruiterFlashError", "Message could not be sent to selected applicants. Please try again."); }
            resp.sendRedirect(req.getContextPath() + "/recruiter?section=selected-applicants&jobId=" + jobId);
            return;
        }
        if ("update-applicant".equals(action)) {
            long jobId = positiveId(value(req, "jobId")), applicationId = positiveId(value(req, "applicationId"));
            String status = value(req, "status");
            try {
                if (jobId == 0 || applicationId == 0 || !Set.of("INPROCESS", "REJECTED", "HIRED").contains(status) || !dao.updateApplicationStatus(companyId, jobId, applicationId, status)) {
                    throw new IllegalArgumentException("Applicant status could not be updated for this job.");
                }
                session.setAttribute("recruiterFlashSuccess", "Applicant status updated to " + status.replace("INPROCESS", "IN PROGRESS") + ".");
            } catch (Exception e) { getServletContext().log("Applicant status could not be updated", e); session.setAttribute("recruiterFlashError", e instanceof IllegalArgumentException ? e.getMessage() : "Applicant status could not be updated. Please try again."); }
            resp.sendRedirect(req.getContextPath() + "/recruiter?section=applicant-detail&jobId=" + jobId + "&applicationId=" + applicationId);
            return;
        }
        String title = value(req, "title");
        String description = value(req, "description");
        String jobType=value(req,"jobType");
        String packageCtc=value(req,"packageCtc");
        String careerField=value(req,"careerField");
        String location=value(req,"location");
        String lastDateText=value(req,"lastDate");
        LocalDate lastDate=null;
        try { if (!lastDateText.isBlank()) lastDate=LocalDate.parse(lastDateText); } catch (Exception ignored) { }
        if (title.isBlank() || title.length() > 180 || description.isBlank() || location.isBlank() || location.length()>180 ||
                !Set.of("Internship","Full-time").contains(jobType) || !CareerFields.isValid(careerField) || packageCtc.length()>180 ||
                lastDate==null || lastDate.isBefore(LocalDate.now())) {
            session.setAttribute("recruiterFlashError", "Enter a title, location, future application deadline, description, and valid job type. Package / CTC can be up to 180 characters.");
        } else {
            try {
                dao.createJob(companyId, title, description, jobType, packageCtc, careerField,
                        location, java.sql.Date.valueOf(lastDate));
                session.setAttribute("recruiterFlashSuccess", "Job submitted. It will appear publicly after administrator review.");
            } catch (Exception e) {
                getServletContext().log("Recruiter job post could not be saved", e);
                session.setAttribute("recruiterFlashError", "Job could not be saved. Check the jobs table schema.");
            }
        }
        resp.sendRedirect(req.getContextPath() + "/recruiter?section=post-job");
    }

    private String value(HttpServletRequest req, String name) { return Optional.ofNullable(req.getParameter(name)).orElse("").trim(); }
    private boolean safeEquals(String a, String b) { return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8)); }
    private long positiveId(String value) { try { long id = Long.parseLong(value); return id > 0 ? id : 0; } catch (Exception ignored) { return 0; } }
    private String displayStatus(Object value) {
        String status = value == null ? "" : value.toString();
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (Set.of("SUBMITTED", "PENDING", "APPLIED").contains(normalized)) return "APPLIED";
        if (Set.of("INPROCESS", "IN_PROGRESS", "ACCEPTED").contains(normalized)) return "IN PROGRESS";
        return status;
    }
}
