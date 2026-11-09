package com.jobcom.web;

import com.jobcom.dao.JobPortalDAO;
import com.jobcom.dao.ResumeDAO;
import com.jobcom.util.CareerFields;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class SeekerJobsServlet extends HttpServlet {
    private final JobPortalDAO dao = new JobPortalDAO();
    private final ResumeDAO profiles = new ResumeDAO();
    private static final String CSRF = "seekerJobsCsrf";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("seekerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/seeker/login");
            return;
        }

        String query = Optional.ofNullable(req.getParameter("q")).orElse("").trim();
        String jobType = Optional.ofNullable(req.getParameter("type")).orElse("").trim();
        long requestedJobId = 0;
        try { requestedJobId = Long.parseLong(Optional.ofNullable(req.getParameter("jobId")).orElse("0")); }
        catch (NumberFormatException ignored) { }
        if (!Set.of("", "Internship", "Full-time").contains(jobType)) jobType = "";
        req.setAttribute("searchQuery", query);
        req.setAttribute("selectedJobType", jobType);
        req.setAttribute("careerField", CareerFields.OTHER);

        try {
            long seekerId = ((Number) session.getAttribute("seekerId")).longValue();
            Map<String, String> profile = profiles.profile(seekerId);
            req.setAttribute("profile", profile);
            req.setAttribute("applicationReady", hasContactDetails(profile));
            String careerField = CareerFields.resolveSeeker(profile);
            req.setAttribute("careerField", careerField);
            List<Map<String, Object>> visibleJobs = dao.openJobs(seekerId, query, jobType, careerField);
            if (requestedJobId > 0) {
                long exactJobId = requestedJobId;
                visibleJobs.removeIf(job -> ((Number) job.get("id")).longValue() != exactJobId);
            }
            req.setAttribute("jobs", visibleJobs);
            List<Map<String, Object>> chats = dao.seekerChats(seekerId);
            req.setAttribute("chats", chats);
            req.setAttribute("hasChats", !chats.isEmpty());
        } catch (Exception e) {
            getServletContext().log("Seeker job board could not be loaded", e);
            req.setAttribute("error", "Jobs could not be loaded. Please check the database schema.");
            req.setAttribute("jobs", List.of());
            req.setAttribute("chats", List.of());
            req.setAttribute("hasChats", false);
        }

        if (session.getAttribute(CSRF) == null) session.setAttribute(CSRF, UUID.randomUUID().toString());
        req.setAttribute("csrf", session.getAttribute(CSRF));
        req.getRequestDispatcher("/WEB-INF/views/seeker/jobs.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("seekerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/seeker/login");
            return;
        }
        if (!safeEquals((String) session.getAttribute(CSRF), req.getParameter("csrf"))) {
            resp.sendError(403, "Invalid form token");
            return;
        }

        Path resumeFile = null;
        try {
            long seekerId = ((Number) session.getAttribute("seekerId")).longValue();
            long jobId = Long.parseLong(req.getParameter("jobId"));
            Map<String, String> profile = profiles.profile(seekerId);
            if (!hasContactDetails(profile)) {
                throw new IllegalArgumentException("Add your name, email, and phone number to your profile before applying.");
            }

            String careerField = CareerFields.resolveSeeker(profile);
            boolean matched = dao.openJobs(seekerId, "", "", careerField).stream()
                    .anyMatch(job -> ((Number) job.get("id")).longValue() == jobId);
            if (!matched) throw new IllegalArgumentException("This opening does not match your selected career field.");

            resumeFile = saveResume(req.getPart("resume"));
            String storedPath = "resumes/" + resumeFile.getFileName();
            boolean applied = dao.apply(seekerId, jobId, storedPath);
            if (!applied) {
                deleteQuietly(resumeFile);
                resumeFile = null;
                setFlash(session, "You have already applied to this job.", false);
            } else {
                resumeFile = null;
                setFlash(session, "Application submitted. Your profile contact details and resume were sent to the recruiter.", false);
            }
        } catch (NumberFormatException e) {
            setFlash(session, "Choose a valid job listing.", true);
        } catch (IllegalArgumentException e) {
            setFlash(session, e.getMessage(), true);
        } catch (ServletException | IOException e) {
            deleteQuietly(resumeFile);
            throw e;
        } catch (Exception e) {
            deleteQuietly(resumeFile);
            getServletContext().log("Job application could not be saved", e);
            setFlash(session, "Application could not be submitted. Please try again.", true);
        }
        resp.sendRedirect(req.getContextPath() + "/seeker/jobs");
    }

    private Path saveResume(Part part) throws IOException {
        if (part == null || part.getSize() == 0) throw new IllegalArgumentException("Upload your resume as a PDF to apply.");
        if (part.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("Resume must be 5 MB or smaller.");
        String filename = part.getSubmittedFileName();
        String extension = filename == null || filename.lastIndexOf('.') < 0
                ? "" : filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!"pdf".equals(extension)) throw new IllegalArgumentException("Resume upload must be a PDF file.");

        byte[] signature = new byte[4];
        int read;
        try (var input = part.getInputStream()) { read = input.read(signature); }
        if (read < 4 || signature[0] != '%' || signature[1] != 'P' || signature[2] != 'D' || signature[3] != 'F') {
            throw new IllegalArgumentException("The uploaded resume is not a valid PDF.");
        }

        String configured = System.getenv("JOBCOM_UPLOAD_DIR");
        String realPath = getServletContext().getRealPath("/uploads");
        Path root = configured == null || configured.isBlank()
                ? (realPath == null ? Paths.get(System.getProperty("user.home"), "Job.com", "uploads") : Paths.get(realPath))
                : Paths.get(configured);
        root = root.toAbsolutePath().normalize();
        Path folder = root.resolve("resumes").normalize();
        if (!folder.startsWith(root)) throw new IOException("Invalid resume upload directory.");
        Files.createDirectories(folder);
        Path target = folder.resolve(UUID.randomUUID() + ".pdf").normalize();
        if (!target.startsWith(root)) throw new IOException("Invalid resume upload path.");
        try (var input = part.getInputStream()) { Files.copy(input, target); }
        return target;
    }

    private boolean hasContactDetails(Map<String, String> profile) {
        return !profile.getOrDefault("full_name", "").isBlank()
                && !profile.getOrDefault("email", "").isBlank()
                && !profile.getOrDefault("phone", "").isBlank();
    }

    private void setFlash(HttpSession session, String message, boolean error) {
        session.setAttribute("seekerJobsFlash", message);
        session.setAttribute("seekerJobsFlashError", error);
    }

    private void deleteQuietly(Path file) {
        if (file != null) try { Files.deleteIfExists(file); } catch (IOException ignored) { }
    }

    private boolean safeEquals(String a, String b) {
        return a != null && b != null
                && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
