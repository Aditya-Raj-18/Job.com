package com.jobcom.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared career-field choices and compatibility rules for seeker job matching. */
public final class CareerFields {
    public static final String OTHER = "Other / General";
    private static final List<String> FIELDS = List.of(
            "Computer Science & IT", "Engineering", "Business & Management",
            "Finance & Accounting", "Marketing & Sales", "Design & Creative",
            "Healthcare", "Education", "Operations & Logistics",
            "Human Resources", "Hospitality & Tourism", OTHER);

    private CareerFields() { }

    public static List<String> all() { return FIELDS; }

    public static boolean isValid(String value) {
        return value != null && FIELDS.stream().anyMatch(field -> field.equalsIgnoreCase(value.trim()));
    }

    public static String resolveSeeker(Map<String, String> profile) {
        String selected = profile.getOrDefault("career_field", "").trim();
        if (isValid(selected)) return canonical(selected);
        return infer(String.join(" ",
                profile.getOrDefault("qualification", ""),
                profile.getOrDefault("profile_headline", ""),
                profile.getOrDefault("skills", ""),
                profile.getOrDefault("profile_summary", "")));
    }

    public static String resolveJob(String selected, String title, String description) {
        if (isValid(selected)) return canonical(selected);
        return infer((title == null ? "" : title) + " " + (description == null ? "" : description));
    }

    public static boolean matches(String seekerField, String jobField, String title, String description) {
        return canonical(seekerField).equals(resolveJob(jobField, title, description));
    }

    private static String canonical(String value) {
        return FIELDS.stream().filter(field -> field.equalsIgnoreCase(value == null ? "" : value.trim()))
                .findFirst().orElse(OTHER);
    }

    private static String infer(String value) {
        String text = " " + (value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#]+", " ").replaceAll("\\s+", " ").trim()) + " ";
        if (has(text, "computer science", "computer engineering", "information technology", "software", "developer", "programming", "java", "python", "javascript", "web development", "full stack", "backend", "front end", "data science", "data analyst", "cybersecurity", "cyber security", "cloud", "devops", "machine learning", "artificial intelligence", " bca ", " mca ", " cse ", " it ", " c++ ", " c# ")) return "Computer Science & IT";
        if (has(text, "finance", "accounting", "accountant", "bookkeeping", "audit", "taxation", "investment", "banking", "chartered accountant", " ca ", " cfa ")) return "Finance & Accounting";
        if (has(text, "marketing", "sales", "seo", "social media", "digital marketing", "business development", "brand manager", "advertising", "customer acquisition")) return "Marketing & Sales";
        if (has(text, "graphic design", "ui ux", "ux ui", "product design", "visual design", "illustrator", "sketcher", "creative", "animation", "animator", "designer")) return "Design & Creative";
        if (has(text, "healthcare", "medical", "medicine", "nurse", "nursing", "doctor", "physician", "pharmacy", "pharmacist", "clinical", "dentist", "therapist")) return "Healthcare";
        if (has(text, "education", "teacher", "teaching", "tutor", "lecturer", "professor", "school", "academic")) return "Education";
        if (has(text, "human resources", "recruiter", "recruitment", "talent acquisition", " hr ", "people operations")) return "Human Resources";
        if (has(text, "supply chain", "logistics", "operations", "procurement", "warehouse", "inventory", "distribution")) return "Operations & Logistics";
        if (has(text, "hospitality", "hotel", "tourism", "travel", "chef", "restaurant", "food service")) return "Hospitality & Tourism";
        if (has(text, "mechanical", "civil engineering", "electrical engineering", "electronics", "chemical engineering", "structural", "manufacturing", "mechatronics", "automotive", "engineer", "engineering", " b tech ", " b.e ", " m.tech ")) return "Engineering";
        if (has(text, "business", "management", " mba ", " bba ", "strategy", "consultant", "administration", "entrepreneur")) return "Business & Management";
        return OTHER;
    }

    private static boolean has(String text, String... phrases) {
        for (String phrase : phrases) if (text.contains(phrase)) return true;
        return false;
    }
}
