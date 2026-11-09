package com.jobcom.web;

import com.jobcom.dao.ResumeDAO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

/** Authenticated, self-only delivery of a seeker's profile photo. */
public class SeekerProfilePhotoServlet extends HttpServlet {
    private final ResumeDAO resumes = new ResumeDAO();

    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("seekerId") instanceof Number)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            Map<String, String> profile = resumes.profile(((Number) session.getAttribute("seekerId")).longValue());
            String relative = profile.getOrDefault("profile_photo_path", profile.getOrDefault("profile_picture", profile.getOrDefault("avatar_path", "")));
            if (!relative.matches("profile-pictures/[a-fA-F0-9-]{36}\\.(jpg|png)")) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Path root = uploadRoot(request).toRealPath();
            Path photo = root.resolve(relative).normalize();
            if (!photo.startsWith(root) || !Files.isRegularFile(photo)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Path realPhoto = photo.toRealPath();
            if (!realPhoto.startsWith(root)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String extension = relative.substring(relative.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            response.setContentType("png".equals(extension) ? "image/png" : "image/jpeg");
            response.setContentLengthLong(Files.size(realPhoto));
            response.setHeader("Content-Disposition", "inline; filename=profile-photo." + extension);
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("Cache-Control", "private, no-store, max-age=0");
            try (var input = Files.newInputStream(realPhoto); var output = response.getOutputStream()) { input.transferTo(output); }
        } catch (java.sql.SQLException e) {
            getServletContext().log("Seeker profile photo could not be loaded", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Path uploadRoot(HttpServletRequest request) {
        String configured = System.getenv("JOBCOM_UPLOAD_DIR");
        String realPath = request.getServletContext().getRealPath("/uploads");
        Path root = configured == null || configured.isBlank()
                ? (realPath == null ? Paths.get(System.getProperty("user.home"), "Job.com", "uploads") : Paths.get(realPath))
                : Paths.get(configured);
        return root.toAbsolutePath().normalize();
    }
}
