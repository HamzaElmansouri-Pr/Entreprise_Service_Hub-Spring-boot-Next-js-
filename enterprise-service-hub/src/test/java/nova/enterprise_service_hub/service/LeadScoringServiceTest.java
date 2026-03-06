package nova.enterprise_service_hub.service;

import nova.enterprise_service_hub.model.Lead;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LeadScoringService — validates AI scoring across all 5 criteria.
 */
class LeadScoringServiceTest {

    private final LeadScoringService scoringService = new LeadScoringService();

    private Lead createLead() {
        Lead lead = new Lead();
        lead.setFullName("John Doe");
        lead.setEmail("john@example.com");
        return lead;
    }

    @Nested
    @DisplayName("Budget Scoring (0–35)")
    class BudgetScoring {

        @Test
        @DisplayName("null budget → 0 points")
        void nullBudget() {
            Lead lead = createLead();
            lead.setBudget(null);
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Budget: 0/35");
        }

        @Test
        @DisplayName("$100K+ → 35 points")
        void highBudget() {
            Lead lead = createLead();
            lead.setBudget(BigDecimal.valueOf(150_000));
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Budget: 35/35");
        }

        @Test
        @DisplayName("$50K → 30 points")
        void midHighBudget() {
            Lead lead = createLead();
            lead.setBudget(BigDecimal.valueOf(50_000));
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Budget: 30/35");
        }

        @Test
        @DisplayName("$25K → 25 points")
        void midBudget() {
            Lead lead = createLead();
            lead.setBudget(BigDecimal.valueOf(25_000));
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Budget: 25/35");
        }

        @Test
        @DisplayName("$10K → 18 points")
        void lowMidBudget() {
            Lead lead = createLead();
            lead.setBudget(BigDecimal.valueOf(10_000));
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Budget: 18/35");
        }

        @Test
        @DisplayName("$5K → 10 points")
        void lowBudget() {
            Lead lead = createLead();
            lead.setBudget(BigDecimal.valueOf(5_000));
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Budget: 10/35");
        }

        @Test
        @DisplayName("$1K → 5 points")
        void veryLowBudget() {
            Lead lead = createLead();
            lead.setBudget(BigDecimal.valueOf(1_000));
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Budget: 5/35");
        }
    }

    @Nested
    @DisplayName("Description Scoring (0–30)")
    class DescriptionScoring {

        @Test
        @DisplayName("null description → 0 points")
        void nullDescription() {
            Lead lead = createLead();
            lead.setProjectDescription(null);
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Description: 0/30");
        }

        @Test
        @DisplayName("long description with enterprise keywords → high score")
        void richDescription() {
            Lead lead = createLead();
            lead.setProjectDescription(
                    "We need an enterprise-grade SaaS platform with cloud deployment, "
                    + "microservice architecture, API integration layer, and analytics dashboard. "
                    + "The solution must include security hardening, AI-powered automation, "
                    + "and machine learning capabilities for data processing. "
                    + "This is a large-scale migration from our legacy system. "
                    + "A ".repeat(200)); // push past 500 chars threshold
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Description: 30/30");
        }

        @Test
        @DisplayName("short description → low score")
        void shortDescription() {
            Lead lead = createLead();
            lead.setProjectDescription("Build a website");
            var result = scoringService.score(lead);
            // 2 (short) + 0 keywords = 2
            assertThat(result.breakdown()).contains("Description: 2/30");
        }
    }

    @Nested
    @DisplayName("Company Scoring (0–15)")
    class CompanyScoring {

        @Test
        @DisplayName("no company → 0 points")
        void noCompany() {
            Lead lead = createLead();
            lead.setCompanyName(null);
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Company: 0/15");
        }

        @Test
        @DisplayName("company name ≥ 3 chars → 15 points")
        void validCompany() {
            Lead lead = createLead();
            lead.setCompanyName("Acme Corporation");
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Company: 15/15");
        }

        @Test
        @DisplayName("very short company → 8 points")
        void shortCompany() {
            Lead lead = createLead();
            lead.setCompanyName("AB");
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Company: 8/15");
        }
    }

    @Nested
    @DisplayName("Contact Scoring (0–10)")
    class ContactScoring {

        @Test
        @DisplayName("email + phone + full name → 10 points")
        void fullContact() {
            Lead lead = createLead();
            lead.setPhone("+1-555-0100");
            var result = scoringService.score(lead);
            // email=4, phone=3, space-in-name=3 = 10
            assertThat(result.breakdown()).contains("Contact: 10/10");
        }

        @Test
        @DisplayName("email only, no phone, single name → 4 points")
        void emailOnly() {
            Lead lead = new Lead();
            lead.setFullName("John");
            lead.setEmail("john@test.com");
            var result = scoringService.score(lead);
            // email=4, no phone, no space in name = 4
            assertThat(result.breakdown()).contains("Contact: 4/10");
        }
    }

    @Nested
    @DisplayName("Timeline Scoring (0–10)")
    class TimelineScoring {

        @Test
        @DisplayName("ASAP → 10 points")
        void urgent() {
            Lead lead = createLead();
            lead.setTimeline("ASAP");
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Timeline: 10/10");
        }

        @Test
        @DisplayName("3 months → 6 points")
        void threeMonths() {
            Lead lead = createLead();
            lead.setTimeline("within 3 months");
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Timeline: 6/10");
        }

        @Test
        @DisplayName("null timeline → 0 points")
        void noTimeline() {
            Lead lead = createLead();
            lead.setTimeline(null);
            var result = scoringService.score(lead);
            assertThat(result.breakdown()).contains("Timeline: 0/10");
        }
    }

    @Nested
    @DisplayName("Composite Scores")
    class CompositeScores {

        @Test
        @DisplayName("perfect lead → score near 100")
        void perfectLead() {
            Lead lead = new Lead();
            lead.setFullName("Jane Smith");
            lead.setEmail("jane@enterprise.com");
            lead.setPhone("+1-555-0200");
            lead.setCompanyName("Enterprise Corp");
            lead.setBudget(BigDecimal.valueOf(200_000));
            lead.setTimeline("ASAP");
            lead.setProjectDescription(
                    "Enterprise-grade SaaS platform with cloud, microservice, API integration, "
                    + "analytics dashboard, AI automation, machine learning, security, migration, scale. "
                    + "A ".repeat(200));

            var result = scoringService.score(lead);
            assertThat(result.score()).isEqualTo(100);
        }

        @Test
        @DisplayName("empty lead → score is 0")
        void emptyLead() {
            Lead lead = new Lead();
            var result = scoringService.score(lead);
            assertThat(result.score()).isEqualTo(0);
        }

        @Test
        @DisplayName("score is capped at 100")
        void scoreCapped() {
            Lead lead = new Lead();
            lead.setFullName("Jane Smith");
            lead.setEmail("jane@enterprise.com");
            lead.setPhone("+1-555-0200");
            lead.setCompanyName("Enterprise Corp");
            lead.setBudget(BigDecimal.valueOf(500_000));
            lead.setTimeline("ASAP - urgent");
            lead.setProjectDescription(
                    "Enterprise SaaS cloud microservice API integration analytics dashboard "
                    + "AI machine learning automation security migration scale platform. "
                    + "A ".repeat(200));

            var result = scoringService.score(lead);
            assertThat(result.score()).isLessThanOrEqualTo(100);
        }

        @Test
        @DisplayName("breakdown contains all 5 categories")
        void breakdownFormat() {
            Lead lead = createLead();
            var result = scoringService.score(lead);
            assertThat(result.breakdown())
                    .contains("Budget:")
                    .contains("Description:")
                    .contains("Company:")
                    .contains("Contact:")
                    .contains("Timeline:");
        }
    }
}
