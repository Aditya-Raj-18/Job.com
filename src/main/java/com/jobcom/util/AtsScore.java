package com.jobcom.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/** A transparent general resume-readiness estimate, not a prediction of an employer's ATS decision. */
public final class AtsScore {
    private static final Pattern WORD = Pattern.compile("[\\p{L}][\\p{L}+#.\\-]{1,}");
    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "you", "your", "this", "that", "are", "was", "were", "will", "from", "into", "our", "their", "they", "have", "has", "had", "job", "role", "work", "team", "about", "using", "use", "who", "what", "when", "where", "how", "all", "any", "can", "should", "must", "required", "requirements", "experience", "years", "year", "ability", "skills", "skill", "knowledge", "good", "strong", "preferred", "including", "such", "other", "each", "more", "than", "within", "across", "support", "responsibilities", "responsibility");
    private static final List<String> ACTION_WORDS = List.of("built", "created", "designed", "developed", "delivered", "improved", "increased", "reduced", "led", "managed", "launched", "implemented", "automated", "analyzed", "supported", "optimized", "resolved", "achieved", "coordinated", "maintained", "tested", "deployed", "generated", "streamlined");
    private AtsScore() {}

    public record Result(int score, Map<String, Integer> breakdown, List<String> suggestions) {}

    public static String extract(byte[] bytes, String extension) throws IOException {
        if (bytes == null || bytes.length == 0 || bytes.length > 5 * 1024 * 1024)
            throw new IllegalArgumentException("Choose a resume file up to 5 MB.");
        if ("pdf".equals(extension)) {
            if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F')
                throw new IllegalArgumentException("The selected file is not a valid PDF.");
            try (var document = Loader.loadPDF(bytes)) {
                return new PDFTextStripper().getText(document);
            }
        }
        if ("docx".equals(extension)) {
            if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K')
                throw new IllegalArgumentException("The selected file is not a valid DOCX document.");
            try (var document = new XWPFDocument(new ByteArrayInputStream(bytes));
                 var extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        }
        throw new IllegalArgumentException("Upload a PDF or DOCX resume.");
    }

    public static Result evaluate(String text) {
        String content = text == null ? "" : text.replace('\u0000', ' ').trim();
        Map<String, Integer> points = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();

        int contact = 0;
        if (Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE).matcher(content).find()) contact += 8;
        else suggestions.add("Add a clearly visible email address.");
        if (Pattern.compile("(?:\\+?\\d[\\d ()-]{7,}\\d)").matcher(content).find()) contact += 7;
        else suggestions.add("Add a phone number with its country code if relevant.");
        points.put("Contact details", contact);

        int sections = 0;
        sections += sectionPoints(content, "summary", 8, "Add a short, clearly labeled professional summary.", suggestions);
        sections += sectionPoints(content, "experience", 12, "Add a clearly labeled work experience section.", suggestions);
        sections += sectionPoints(content, "education", 8, "Add an Education section with school, qualification, and dates.", suggestions);
        sections += sectionPoints(content, "skills", 8, "Add a Skills section with specific tools and technologies.", suggestions);
        sections += sectionPoints(content, "projects", 4, "Add relevant projects, certifications, or publications if you have them.", suggestions);
        points.put("Resume sections", sections);

        int impact = 0;
        boolean hasMetric = Pattern.compile("\\b\\d{1,3}(?:\\.\\d+)?\\s?(?:%|percent|x|hours?|days?|weeks?|months?|years?|users?|clients?|projects?|records?|lpa|₹|\\$)", Pattern.CASE_INSENSITIVE).matcher(content).find()
                || Pattern.compile("\\b(?:increased|reduced|improved|grew|saved|generated|cut|raised|lowered)\\b[^\\n]{0,45}\\b\\d+(?:\\.\\d+)?\\b", Pattern.CASE_INSENSITIVE).matcher(content).find();
        if (hasMetric) impact += 10; else suggestions.add("Add a concrete result or scale to at least one achievement (for example, time saved, users reached, or percentage improved).");
        int actionCount = 0;
        for (String verb : ACTION_WORDS) if (Pattern.compile("\\b" + verb + "\\b", Pattern.CASE_INSENSITIVE).matcher(content).find()) actionCount++;
        if (actionCount >= 2) impact += 5; else suggestions.add("Start experience or project bullets with clear action verbs and describe your contribution.");
        points.put("Achievement evidence", impact);

        int words = wordCount(content);
        int length = words >= 350 && words <= 900 ? 10 : words >= 250 && words <= 1100 ? 8 : words >= 150 && words <= 1300 ? 5 : words >= 80 ? 2 : 0;
        if (length < 10) suggestions.add("Aim for roughly 350–900 words; keep only details relevant to the roles you want.");
        int bullets = 0;
        for (String line : content.split("\\R")) if (line.strip().matches("^(?:[•▪◦*\\-]|\\d+[.)])\\s+.+")) bullets++;
        if (bullets >= 3) length += 5; else suggestions.add("Use concise bullets for experience and project achievements so they are easier to scan.");
        points.put("Length and scanability", length);

        boolean hasLink = Pattern.compile("https?://|www\\.|linkedin\\.com", Pattern.CASE_INSENSITIVE).matcher(content).find();
        points.put("Professional link", hasLink ? 5 : 0);
        if (!hasLink) suggestions.add("Add a LinkedIn profile or portfolio link if it supports your application.");

        int skills = distinctSkillCount(content);
        int skillPoints = skills >= 8 ? 10 : skills >= 5 ? 8 : skills >= 3 ? 5 : skills > 0 ? 2 : 0;
        if (skillPoints < 10) suggestions.add("List at least five specific skills that accurately describe your experience.");
        points.put("Specific skills", skillPoints);

        int score = points.values().stream().mapToInt(Integer::intValue).sum();
        if (content.length() < 80) suggestions.add("Very little text could be read. If this is a scanned PDF, use a text-based PDF or DOCX file.");
        if (suggestions.isEmpty()) suggestions.add("Your resume covers the checks in this estimate. Tailor it to each job and verify the details are accurate.");
        return new Result(Math.min(100, score), Map.copyOf(points), List.copyOf(suggestions));
    }

    private static int sectionPoints(String content, String section, int points, String advice, List<String> suggestions) {
        String body = sectionBody(content, section);
        if (body.isBlank()) {
            suggestions.add(advice);
            return 0;
        }
        int words = wordCount(body);
        if (words >= 4) return points;
        if (words >= 2) {
            suggestions.add("Add more detail under your " + section + " section, such as dates, scope, tools, and results.");
            return Math.max(1, points / 2);
        }
        if (words == 1) {
            suggestions.add("Expand your " + section + " section beyond a title or single item.");
            return Math.max(1, points / 4);
        }
        suggestions.add(advice);
        return 0;
    }

    private static String sectionBody(String content, String wanted) {
        StringBuilder body = new StringBuilder();
        boolean inside = false;
        for (String line : content.split("\\R")) {
            String kind = headingKind(line);
            if (kind != null) {
                if (inside) break;
                inside = wanted.equals(kind);
                continue;
            }
            if (inside) body.append(line).append(' ');
        }
        return body.toString().trim();
    }

    private static String headingKind(String line) {
        String value = line.trim().replaceAll("[:|—–-]+$", "").trim().toLowerCase(Locale.ROOT);
        if (value.matches("(?:professional\\s+)?summary|objective|profile")) return "summary";
        if (value.matches("(?:work|professional)\\s+experience|employment\\s+history|experience")) return "experience";
        if (value.matches("education|academic\\s+background|qualifications")) return "education";
        if (value.matches("skills|technical\\s+skills|core\\s+competencies")) return "skills";
        if (value.matches("projects?|certifications?|certificates|publications")) return "projects";
        if (value.matches("languages|interests|references|awards|links|contact|certification")) return "other";
        return null;
    }

    private static int wordCount(String text) {
        Matcher matcher = WORD.matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private static int distinctSkillCount(String text) {
        Matcher section = Pattern.compile("(?is)(?:^|[\\n\\r])\\s*(?:skills|technical\\s+skills|core\\s+competencies)\\s*:?\\s*(.*?)(?=\\n\\s*(?:experience|education|projects?|certifications?|summary|objective|languages|interests)\\b|$)").matcher(text);
        if (!section.find()) return 0;
        Set<String> skills = new LinkedHashSet<>();
        for (String token : section.group(1).split("[,;|\\n•]+")) {
            String skill = token.trim().toLowerCase(Locale.ROOT);
            if (skill.length() >= 2 && skill.length() <= 40 && !STOP_WORDS.contains(skill)) skills.add(skill);
        }
        return skills.size();
    }

}
