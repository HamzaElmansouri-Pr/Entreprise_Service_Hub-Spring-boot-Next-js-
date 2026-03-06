package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.model.Lead;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Lead Scoring Service — ranks leads 0–100 based on:
 * <ul>
 *   <li>Budget tier (0–35 pts)</li>
 *   <li>Project description quality (0–30 pts)</li>
 *   <li>Company presence (0–15 pts)</li>
 *   <li>Contact completeness (0–10 pts)</li>
 *   <li>Timeline urgency (0–10 pts)</li>
 * </ul>
 *
 * <p>Scoring executes in-memory (sub-1ms) to maintain the
 * Elite Quality Gate of &lt;100ms end-to-end lead creation.</p>
 */
@Service
public class LeadScoringService {

    /**
     * Calculate a composite lead score and return a breakdown string.
     */
    public ScoringResult score(Lead lead) {
        List<String> breakdown = new ArrayList<>();
        int total = 0;

        // ── 1. Budget tier (0–35) ────────────────────────────────────────
        int budgetScore = scoreBudget(lead.getBudget());
        total += budgetScore;
        breakdown.add("Budget: " + budgetScore + "/35");

        // ── 2. Description quality (0–30) ────────────────────────────────
        int descScore = scoreDescription(lead.getProjectDescription());
        total += descScore;
        breakdown.add("Description: " + descScore + "/30");

        // ── 3. Company presence (0–15) ───────────────────────────────────
        int companyScore = scoreCompany(lead.getCompanyName());
        total += companyScore;
        breakdown.add("Company: " + companyScore + "/15");

        // ── 4. Contact completeness (0–10) ───────────────────────────────
        int contactScore = scoreContact(lead);
        total += contactScore;
        breakdown.add("Contact: " + contactScore + "/10");

        // ── 5. Timeline urgency (0–10) ───────────────────────────────────
        int timelineScore = scoreTimeline(lead.getTimeline());
        total += timelineScore;
        breakdown.add("Timeline: " + timelineScore + "/10");

        return new ScoringResult(Math.min(total, 100), String.join(" | ", breakdown));
    }

    private int scoreBudget(BigDecimal budget) {
        if (budget == null) return 0;
        double val = budget.doubleValue();
        if (val >= 100_000) return 35;
        if (val >= 50_000) return 30;
        if (val >= 25_000) return 25;
        if (val >= 10_000) return 18;
        if (val >= 5_000) return 10;
        if (val > 0) return 5;
        return 0;
    }

    private int scoreDescription(String desc) {
        if (desc == null || desc.isBlank()) return 0;
        int len = desc.trim().length();
        int score = 0;

        // Length-based scoring
        if (len >= 500) score += 15;
        else if (len >= 200) score += 10;
        else if (len >= 50) score += 5;
        else score += 2;

        // Keyword signals — enterprise terms boost score
        String lower = desc.toLowerCase();
        String[] highValueKeywords = {
                "enterprise", "scale", "migration", "api", "integration",
                "cloud", "microservice", "saas", "platform", "dashboard",
                "analytics", "ai", "machine learning", "automation", "security"
        };
        int keywordHits = 0;
        for (String keyword : highValueKeywords) {
            if (lower.contains(keyword)) keywordHits++;
        }
        score += Math.min(keywordHits * 3, 15);

        return Math.min(score, 30);
    }

    private int scoreCompany(String companyName) {
        if (companyName == null || companyName.isBlank()) return 0;
        return companyName.trim().length() >= 3 ? 15 : 8;
    }

    private int scoreContact(Lead lead) {
        int score = 0;
        if (lead.getEmail() != null && !lead.getEmail().isBlank()) score += 4;
        if (lead.getPhone() != null && !lead.getPhone().isBlank()) score += 3;
        if (lead.getFullName() != null && lead.getFullName().contains(" ")) score += 3;
        return Math.min(score, 10);
    }

    private int scoreTimeline(String timeline) {
        if (timeline == null || timeline.isBlank()) return 0;
        String lower = timeline.toLowerCase();
        if (lower.contains("asap") || lower.contains("urgent") || lower.contains("immediate")) return 10;
        if (lower.contains("1 month") || lower.contains("2 week") || lower.contains("this month")) return 8;
        if (lower.contains("3 month") || lower.contains("quarter")) return 6;
        if (lower.contains("6 month") || lower.contains("half year")) return 4;
        return 2; // has some timeline = mild intent
    }

    public record ScoringResult(int score, String breakdown) {}
}
