package com.jobcom.web;

import com.jobcom.dao.JobPortalDAO;
import com.jobcom.dao.NotificationDAO;
import com.jobcom.dao.ResumeDAO;
import com.jobcom.util.CareerFields;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Home, application history, and editable profile for an approved job seeker. */
public class SeekerPanelServlet extends HttpServlet {
    private static final String CSRF_KEY = "seekerPanelCsrf";
    private final JobPortalDAO jobs = new JobPortalDAO();
    private final ResumeDAO resumes = new ResumeDAO();
    private final NotificationDAO notifications = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("seekerId") instanceof Number)) {
            resp.sendRedirect(req.getContextPath() + "/seeker/login");
            return;
        }

        String section = req.getParameter("section");
        if (section == null || !Set.of("applications", "profile", "notifications").contains(section)) section = "dashboard";
        long seekerId = ((Number) session.getAttribute("seekerId")).longValue();
        req.setAttribute("careerFields", CareerFields.all());
        req.setAttribute("careerField", CareerFields.OTHER);
        try {
            Map<String, String> profile = resumes.profile(seekerId);
            String seekerField = CareerFields.resolveSeeker(profile);
            profile.put("career_field", seekerField);
            List<Map<String, Object>> applications = jobs.seekerApplications(seekerId);
            List<Map<String, Object>> openJobs = jobs.openJobs(seekerId, "", "", seekerField);
            List<Map<String, Object>> chats = jobs.seekerChats(seekerId);
            req.setAttribute("profile", profile);
            req.setAttribute("careerField", seekerField);
            req.setAttribute("careerFields", CareerFields.all());
            req.setAttribute("applications", applications);
            req.setAttribute("openJobs", openJobs);
            req.setAttribute("applicationCount", applications.size());
            req.setAttribute("openJobCount", openJobs.size());
            req.setAttribute("chatCount", chats.size());
            req.setAttribute("hasChats", !chats.isEmpty());
            req.setAttribute("profileCompletion", profileCompletion(profile));
            req.setAttribute("recentApplications", applications.subList(0, Math.min(4, applications.size())));
            req.setAttribute("recommendedJobs", openJobs.subList(0, Math.min(3, openJobs.size())));
        } catch (Exception e) {
            getServletContext().log("Seeker workspace could not be loaded", e);
            req.setAttribute("error", "Your workspace could not be loaded. Please try again in a moment.");
            req.setAttribute("profile", Map.of("career_field", CareerFields.OTHER));
            req.setAttribute("careerField", CareerFields.OTHER);
            req.setAttribute("applications", List.of());
            req.setAttribute("openJobs", List.of());
            req.setAttribute("recentApplications", List.of());
            req.setAttribute("recommendedJobs", List.of());
            req.setAttribute("applicationCount", 0);
            req.setAttribute("openJobCount", 0);
            req.setAttribute("chatCount", 0);
            req.setAttribute("hasChats", false);
            req.setAttribute("profileCompletion", 0);
        }
        try {
            req.setAttribute("announcements", notifications.seekerFeed(seekerId));
            req.setAttribute("unreadNotificationCount", notifications.unreadCount(seekerId));
        } catch (Exception e) {
            getServletContext().log("Seeker announcements could not be loaded", e);
            req.setAttribute("announcements", List.of());
            req.setAttribute("unreadNotificationCount", 0);
        }

        String token = (String) session.getAttribute(CSRF_KEY);
        if (token == null) {
            token = UUID.randomUUID().toString();
            session.setAttribute(CSRF_KEY, token);
        }
        req.setAttribute("csrf", token);
        req.setAttribute("section", section);
        req.setAttribute("pageTitle", "profile".equals(section) ? "My profile" : "applications".equals(section) ? "My applications" : "notifications".equals(section) ? "Notifications" : "Seeker dashboard");
        req.setAttribute("pageSubtitle", "profile".equals(section) ? "Keep your career details ready for your next opportunity." : "notifications".equals(section) ? "Updates and announcements from Job.com." : "Track your applications and find your next opportunity.");
        Object flash = session.getAttribute("seekerPanelFlash");
        if (flash != null) {
            req.setAttribute("flash", flash);
            req.setAttribute("flashError", session.getAttribute("seekerPanelFlashError"));
            session.removeAttribute("seekerPanelFlash");
            session.removeAttribute("seekerPanelFlashError");
        }
        if ("1".equals(req.getParameter("saved"))) req.setAttribute("flash", "Your profile has been saved.");
        req.getRequestDispatcher("/WEB-INF/views/seeker/dashboard.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        if (session == null || !(session.getAttribute("seekerId") instanceof Number)) {
            resp.sendRedirect(req.getContextPath() + "/seeker/login");
            return;
        }
        if (!safeEquals((String) session.getAttribute(CSRF_KEY), req.getParameter("csrf"))) {
            session.setAttribute(CSRF_KEY, UUID.randomUUID().toString());
            session.setAttribute("seekerPanelFlash", "Your form session expired. Please submit the form again.");
            session.setAttribute("seekerPanelFlashError", true);
            resp.sendRedirect(req.getContextPath() + "/seeker?section=profile");
            return;
        }
        if ("mark-notifications-read".equals(req.getParameter("action"))) {
            try {
                notifications.markSeekerRead(((Number) session.getAttribute("seekerId")).longValue());
                resp.sendRedirect(req.getContextPath() + "/seeker?section=notifications&read=1");
            } catch (Exception e) {
                getServletContext().log("Seeker notifications could not be marked read", e);
                session.setAttribute("seekerPanelFlash", "Notifications could not be updated. Please try again.");
                session.setAttribute("seekerPanelFlashError", true);
                resp.sendRedirect(req.getContextPath() + "/seeker?section=notifications");
            }
            return;
        }
        if ("upload-profile-photo".equals(req.getParameter("action"))) {
            Path saved = null;
            long seekerId = ((Number) session.getAttribute("seekerId")).longValue();
            try {
                Part photo = req.getPart("profilePhoto");
                saved = saveProfilePhoto(photo);
                String relative = "profile-pictures/" + saved.getFileName();
                Map<String, String> profile = resumes.profile(seekerId);
                String previous = profile.getOrDefault("profile_photo_path", profile.getOrDefault("profile_picture", profile.getOrDefault("avatar_path", "")));
                resumes.updateProfilePhoto(seekerId, relative);
                deletePreviousPhoto(previous);
                session.setAttribute("seekerPanelFlash", "Your profile photo has been updated.");
                session.setAttribute("seekerPanelFlashError", false);
            } catch (IllegalArgumentException e) {
                deleteQuietly(saved);
                session.setAttribute("seekerPanelFlash", e.getMessage());
                session.setAttribute("seekerPanelFlashError", true);
            } catch (Exception e) {
                deleteQuietly(saved);
                getServletContext().log("Seeker profile photo could not be saved", e);
                session.setAttribute("seekerPanelFlash", "Your profile photo could not be saved. Please try again.");
                session.setAttribute("seekerPanelFlashError", true);
            }
            resp.sendRedirect(req.getContextPath() + "/seeker?section=profile");
            return;
        }
        if (!"save-profile".equals(req.getParameter("action"))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported action");
            return;
        }

        Map<String, String> values = new LinkedHashMap<>();
        values.put("full_name", value(req, "full_name", 160));
        values.put("phone", value(req, "phone", 40));
        values.put("address", value(req, "address", 300));
        values.put("city", value(req, "city", 100));
        values.put("state", value(req, "state", 100));
        values.put("postal_code", value(req, "postal_code", 20));
        values.put("qualification", value(req, "qualification", 180));
        values.put("career_field", value(req, "career_field", 120));
        values.put("experience_years", value(req, "experience_years", 8));
        values.put("skills", value(req, "skills", 4000));
        values.put("profile_headline", value(req, "profile_headline", 220));
        values.put("profile_summary", value(req, "profile_summary", 5000));
        values.put("linkedin_url", value(req, "linkedin_url", 500));
        values.put("portfolio_url", value(req, "portfolio_url", 500));

        String error = validate(values);
        if (error != null) {
            session.setAttribute("seekerPanelFlash", error);
            session.setAttribute("seekerPanelFlashError", true);
        } else {
            try {
                jobs.updateSeekerProfile(((Number) session.getAttribute("seekerId")).longValue(), values);
                session.setAttribute("seekerName", values.get("full_name"));
                resp.sendRedirect(req.getContextPath() + "/seeker?section=profile&saved=1");
                return;
            } catch (Exception e) {
                getServletContext().log("Seeker profile could not be saved", e);
                session.setAttribute("seekerPanelFlash", "Your profile could not be saved. Please try again.");
                session.setAttribute("seekerPanelFlashError", true);
            }
        }
        resp.sendRedirect(req.getContextPath() + "/seeker?section=profile");
    }

    private String value(HttpServletRequest req, String key, int maxLength) {
        String value = req.getParameter(key);
        if (value == null) return "";
        value = value.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength + 1);
    }

    private Path saveProfilePhoto(Part part) throws IOException {
        if (part == null || part.getSize() == 0) throw new IllegalArgumentException("Choose a JPG or PNG profile photo to upload.");
        if (part.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("Profile photos must be 5 MB or smaller.");
        String submitted = part.getSubmittedFileName();
        String extension = submitted == null || submitted.lastIndexOf('.') < 0 ? "" : submitted.substring(submitted.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!Set.of("jpg", "jpeg", "png").contains(extension)) throw new IllegalArgumentException("Upload a JPG or PNG image.");
        byte[] signature = new byte[8]; int read;
        try (var input = part.getInputStream()) { read = input.read(signature); }
        boolean png = extension.equals("png") && read >= 8 && (signature[0] & 255) == 137 && signature[1] == 'P' && signature[2] == 'N' && signature[3] == 'G' && signature[4] == 13 && signature[5] == 10 && signature[6] == 26 && signature[7] == 10;
        boolean jpeg = (extension.equals("jpg") || extension.equals("jpeg")) && read >= 3 && (signature[0] & 255) == 255 && (signature[1] & 255) == 216 && (signature[2] & 255) == 255;
        if (!png && !jpeg) throw new IllegalArgumentException("The selected file is not a valid JPG or PNG image.");

        Path root = uploadRoot();
        Path folder = root.resolve("profile-pictures").normalize();
        if (!folder.startsWith(root)) throw new IOException("Invalid profile photo upload directory.");
        Files.createDirectories(folder);
        Path target = folder.resolve(UUID.randomUUID() + (png ? ".png" : ".jpg")).normalize();
        if (!target.startsWith(root)) throw new IOException("Invalid profile photo upload path.");
        try (var input = part.getInputStream()) { Files.copy(input, target); }
        return target;
    }

    private Path uploadRoot() throws IOException {
        String configured = System.getenv("JOBCOM_UPLOAD_DIR");
        String realPath = getServletContext().getRealPath("/uploads");
        Path root = configured == null || configured.isBlank()
                ? (realPath == null ? Paths.get(System.getProperty("user.home"), "Job.com", "uploads") : Paths.get(realPath))
                : Paths.get(configured);
        return root.toAbsolutePath().normalize();
    }

    private void deletePreviousPhoto(String previous) {
        if (previous == null || !previous.matches("profile-pictures/[a-fA-F0-9-]{36}\\.(jpg|png)")) return;
        try {
            Path root = uploadRoot();
            Path old = root.resolve(previous).normalize();
            if (old.startsWith(root)) Files.deleteIfExists(old);
        } catch (IOException e) { getServletContext().log("Previous seeker profile photo could not be removed", e); }
    }

    private void deleteQuietly(Path path) { if (path != null) try { Files.deleteIfExists(path); } catch (IOException ignored) { } }

    private String validate(Map<String, String> values) {
        if (values.get("full_name").isBlank()) return "Please enter your full name.";
        if (!CareerFields.isValid(values.get("career_field"))) return "Please select your career field so we can match the right jobs.";
        for (Map.Entry<String, String> entry : values.entrySet()) {
            int max = switch (entry.getKey()) {
                case "full_name" -> 160; case "phone" -> 40; case "address" -> 300; case "city", "state" -> 100;
                case "postal_code" -> 20; case "qualification" -> 180; case "career_field" -> 120; case "experience_years" -> 8;
                case "skills" -> 4000; case "profile_headline" -> 220; case "profile_summary" -> 5000;
                default -> 500;
            };
            if (entry.getValue().length() > max) return "Please shorten the " + entry.getKey().replace('_', ' ') + " field.";
        }
        String experience = values.get("experience_years");
        if (!experience.isBlank()) {
            try {
                BigDecimal years = new BigDecimal(experience);
                if (years.signum() < 0 || years.compareTo(new BigDecimal("99.99")) > 0) return "Enter experience between 0 and 99.99 years.";
            } catch (NumberFormatException e) {
                return "Enter experience as a number of years, for example 2 or 2.5.";
            }
        }
        for (String key : List.of("linkedin_url", "portfolio_url")) {
            String url = values.get(key);
            if (!url.isBlank() && !(url.regionMatches(true, 0, "https://", 0, 8) || url.regionMatches(true, 0, "http://", 0, 7))) {
                return "LinkedIn and portfolio links must start with http:// or https://.";
            }
        }
        return null;
    }

    private int profileCompletion(Map<String, String> profile) {
        List<String> fields = List.of("full_name", "email", "phone", "profile_headline", "profile_summary", "skills", "qualification", "career_field", "experience_years", "city", "linkedin_url", "portfolio_url");
        long completed = fields.stream().filter(key -> !profile.getOrDefault(key, "").isBlank()).count();
        return (int) Math.round(completed * 100.0 / fields.size());
    }

    private boolean safeEquals(String expected, String actual) {
        return expected != null && actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
