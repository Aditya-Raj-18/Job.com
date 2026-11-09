package com.jobcom.web;

import com.jobcom.util.AtsScore;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.util.Map;

@MultipartConfig(maxFileSize = 5 * 1024 * 1024, maxRequestSize = 6 * 1024 * 1024)
public class AtsScoreServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var session = request.getSession(false);
        if (session == null || session.getAttribute("seekerId") == null) {
            response.sendRedirect(request.getContextPath() + "/seeker/login");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/views/seeker/ats-score.jsp").forward(request, response);
    }

    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json"); response.setCharacterEncoding("UTF-8");
        try {
            String resumeText = request.getParameter("resumeText");
            AtsScore.Result result;
            if (resumeText != null) {
                if (resumeText.length() > 30000) throw new IllegalArgumentException("Resume text is too long to score.");
                result = AtsScore.evaluate(resumeText);
            } else {
                Part file = request.getPart("resume");
                if (file == null || file.getSize() == 0) throw new IllegalArgumentException("Choose a PDF or DOCX resume first.");
                String filename = file.getSubmittedFileName();
                String ext = filename == null || filename.lastIndexOf('.') < 0 ? "" : filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
                result = AtsScore.evaluate(AtsScore.extract(file.getInputStream().readAllBytes(), ext));
            }
            response.getWriter().write(json(result, null));
        } catch (IllegalArgumentException e) {
            response.setStatus(400); response.getWriter().write(json(null, e.getMessage()));
        } catch (Exception e) {
            getServletContext().log("ATS resume score could not be calculated", e);
            response.setStatus(400); response.getWriter().write(json(null, "We could not read this resume. Try a text-based PDF or DOCX file."));
        }
    }

    private String json(AtsScore.Result result, String error) {
        if (error != null) return "{\"error\":\"" + escape(error) + "\"}";
        StringBuilder breakdown = new StringBuilder("{");
        for (Map.Entry<String, Integer> entry : result.breakdown().entrySet()) {
            if (breakdown.length() > 1) breakdown.append(',');
            breakdown.append('"').append(escape(entry.getKey())).append("\":").append(entry.getValue());
        }
        breakdown.append('}');
        return "{\"score\":" + result.score() + ",\"breakdown\":" + breakdown + ",\"suggestions\":" + array(result.suggestions()) + "}";
    }
    private String array(java.util.List<String> values) { return "[" + values.stream().map(v -> "\"" + escape(v) + "\"").collect(java.util.stream.Collectors.joining(",")) + "]"; }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n"); }
}
