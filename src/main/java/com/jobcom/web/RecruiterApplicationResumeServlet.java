package com.jobcom.web;

import com.jobcom.dao.JobPortalDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/** Streams an application resume only to the company that owns the related job. */
public class RecruiterApplicationResumeServlet extends HttpServlet {
    private static final Pattern RESUME_PATH = Pattern.compile("resumes/[a-fA-F0-9-]{36}\\.pdf");
    private final JobPortalDAO jobs = new JobPortalDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("companyId") instanceof Number)) {
            resp.sendRedirect(req.getContextPath() + "/recruiter/login");
            return;
        }

        long applicationId;
        try {
            applicationId = Long.parseLong(req.getParameter("applicationId"));
            if (applicationId < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid application id");
            return;
        }

        try {
            long companyId = ((Number) session.getAttribute("companyId")).longValue();
            String relative = jobs.recruiterApplicationResume(applicationId, companyId);
            if (relative == null || !RESUME_PATH.matcher(relative).matches()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String configured = System.getenv("JOBCOM_UPLOAD_DIR");
            String realPath = getServletContext().getRealPath("/uploads");
            Path root = configured == null || configured.isBlank()
                    ? (realPath == null ? Paths.get(System.getProperty("user.home"), "Job.com", "uploads") : Paths.get(realPath))
                    : Paths.get(configured);
            root = root.toAbsolutePath().normalize();
            Path file = root.resolve(relative).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            resp.setContentType("application/pdf");
            resp.setContentLengthLong(Files.size(file));
            resp.setHeader("Content-Disposition", "attachment; filename=\"application-resume.pdf\"");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("Cache-Control", "private, no-store, max-age=0");
            try (var input = Files.newInputStream(file); var output = resp.getOutputStream()) {
                input.transferTo(output);
            }
        } catch (Exception e) {
            getServletContext().log("Recruiter application resume could not be loaded", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Resume could not be loaded.");
        }
    }
}