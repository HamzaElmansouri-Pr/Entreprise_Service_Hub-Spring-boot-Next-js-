package nova.enterprise_service_hub.config;

import nova.enterprise_service_hub.model.*;
import nova.enterprise_service_hub.repository.*;
import nova.enterprise_service_hub.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Elite Data Initializer — Seeds the database with Nova Agency branded content
 * on first startup. Only populates when the tables are empty, so it is
 * safe to run repeatedly without duplicating data.
 */
@Component
public class DataInitializer implements CommandLineRunner {

        /** Default tenant assigned to all seed data. */
        private static final String DEFAULT_TENANT = "nova-default";

        private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

        private final AgencyServiceRepository serviceRepo;
        private final ProjectRepository projectRepo;
        private final RoleRepository roleRepo;
        private final UserRepository userRepo;
        private final PasswordEncoder passwordEncoder;
        private final GlobalConfigRepository configRepo;
        private final SlideRepository slideRepo;
        private final PageSectionRepository pageSectionRepo;
        private final TenantRepository tenantRepo;
        private final LeadRepository leadRepo;
        private final InvoiceRepository invoiceRepo;
        private final ExpenseRepository expenseRepo;
        private final ProposalRepository proposalRepo;
        private final SkillNodeRepository skillNodeRepo;
        private final SprintSessionRepository sprintRepo;
        private final BadgeRepository badgeRepo;
        private final ClientUserRepository clientUserRepo;
        private final SubscriptionRepository subscriptionRepo;
        private final NotificationRepository notificationRepo;

        public DataInitializer(AgencyServiceRepository serviceRepo,
                        ProjectRepository projectRepo,
                        RoleRepository roleRepo,
                        UserRepository userRepo,
                        PasswordEncoder passwordEncoder,
                        GlobalConfigRepository configRepo,
                        SlideRepository slideRepo,
                        PageSectionRepository pageSectionRepo,
                        TenantRepository tenantRepo,
                        LeadRepository leadRepo,
                        InvoiceRepository invoiceRepo,
                        ExpenseRepository expenseRepo,
                        ProposalRepository proposalRepo,
                        SkillNodeRepository skillNodeRepo,
                        SprintSessionRepository sprintRepo,
                        BadgeRepository badgeRepo,
                        ClientUserRepository clientUserRepo,
                        SubscriptionRepository subscriptionRepo,
                        NotificationRepository notificationRepo) {
                this.serviceRepo = serviceRepo;
                this.projectRepo = projectRepo;
                this.roleRepo = roleRepo;
                this.userRepo = userRepo;
                this.passwordEncoder = passwordEncoder;
                this.configRepo = configRepo;
                this.slideRepo = slideRepo;
                this.pageSectionRepo = pageSectionRepo;
                this.tenantRepo = tenantRepo;
                this.leadRepo = leadRepo;
                this.invoiceRepo = invoiceRepo;
                this.expenseRepo = expenseRepo;
                this.proposalRepo = proposalRepo;
                this.skillNodeRepo = skillNodeRepo;
                this.sprintRepo = sprintRepo;
                this.badgeRepo = badgeRepo;
                this.clientUserRepo = clientUserRepo;
                this.subscriptionRepo = subscriptionRepo;
                this.notificationRepo = notificationRepo;
        }

        @Override
        public void run(String... args) {
                // Set default tenant context so TenantEntityListener stamps all seed data
                TenantContext.setTenantId(DEFAULT_TENANT);
                try {
                        seedRoles();
                        seedAdminUser();
                        seedSuperAdminUser();
                        seedStandardUser();
                        seedDefaultTenant();
                        seedGlobalConfig();
                        seedSlides();
                        seedServices();
                        seedProjects();
                        seedPageSections();
                        seedLeads();
                        seedInvoices();
                        seedExpenses();
                        seedProposals();
                        seedSkillNodes();
                        seedSprintSessions();
                        seedBadges();
                        seedClientUsers();
                        seedSubscription();
                        seedNotifications();
                        logCredentials();
                } finally {
                        TenantContext.clear();
                }
        }

        private void seedRoles() {
                // Ensure all three roles exist (safe to call repeatedly)
                for (String roleName : List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_SUPER_ADMIN")) {
                        if (roleRepo.findByName(roleName).isEmpty()) {
                                roleRepo.save(new Role(roleName));
                                log.info("✅ Seeded role: {}", roleName);
                        }
                }
        }

        private void seedAdminUser() {
                Role adminRole = roleRepo.findByName("ROLE_ADMIN")
                                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN not found"));
                Role userRole = roleRepo.findByName("ROLE_USER")
                                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

                var existingUser = userRepo.findByEmail("admin@nova.agency");
                if (existingUser.isPresent()) {
                        User admin = existingUser.get();
                        if (!admin.getRoles().contains(adminRole)) {
                                admin.getRoles().add(adminRole);
                                userRepo.save(admin);
                                log.info("⬆️ Upgraded admin@nova.agency to ROLE_ADMIN");
                        } else {
                                log.info("⚡ Admin user already has ROLE_ADMIN — skipping");
                        }
                        return;
                }

                log.info("🌱 Seeding Admin User...");

                User admin = new User();
                admin.setFullName("Nova Admin");
                admin.setEmail("admin@nova.agency");
                admin.setPassword(passwordEncoder.encode("elite2026!"));
                admin.setRoles(Set.of(adminRole, userRole));
                admin.setTenantId(DEFAULT_TENANT);

                userRepo.save(admin);
                log.info("✅ Seeded admin user: admin@nova.agency");
        }

        private void seedSuperAdminUser() {
                Role superAdminRole = roleRepo.findByName("ROLE_SUPER_ADMIN")
                                .orElseThrow(() -> new IllegalStateException("ROLE_SUPER_ADMIN not found"));
                Role userRole = roleRepo.findByName("ROLE_USER")
                                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

                var existing = userRepo.findByEmail("superadmin@nova.platform");
                if (existing.isPresent()) {
                        User sa = existing.get();
                        if (!sa.getRoles().contains(superAdminRole)) {
                                sa.getRoles().add(superAdminRole);
                                userRepo.save(sa);
                                log.info("⬆️ Upgraded superadmin@nova.platform to ROLE_SUPER_ADMIN");
                        } else {
                                log.info("⚡ Super-admin user already exists — skipping");
                        }
                        return;
                }

                log.info("🌱 Seeding Super-Admin User...");
                User superAdmin = new User();
                superAdmin.setFullName("Platform Super Admin");
                superAdmin.setEmail("superadmin@nova.platform");
                superAdmin.setPassword(passwordEncoder.encode("superAdmin2026!"));
                superAdmin.setRoles(Set.of(superAdminRole, userRole));
                // Super-admin has a special platform-level tenant
                superAdmin.setTenantId("platform-root");

                userRepo.save(superAdmin);
                log.info("✅ Seeded super-admin user: superadmin@nova.platform");
        }

        private void seedStandardUser() {
                Role userRole = roleRepo.findByName("ROLE_USER")
                                .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

                if (userRepo.findByEmail("user@nova.agency").isPresent()) {
                        log.info("⚡ Standard user already exists — skipping");
                        return;
                }

                log.info("🌱 Seeding Standard User...");
                User user = new User();
                user.setFullName("Standard User");
                user.setEmail("user@nova.agency");
                user.setPassword(passwordEncoder.encode("user2026!"));
                user.setRoles(Set.of(userRole));
                user.setTenantId(DEFAULT_TENANT);

                userRepo.save(user);
                log.info("✅ Seeded standard user: user@nova.agency");
        }

        private void seedDefaultTenant() {
                if (tenantRepo.existsByTenantId(DEFAULT_TENANT)) {
                        log.info("⚡ Default tenant already exists — skipping");
                        return;
                }

                log.info("🌱 Seeding Default Tenant...");
                Tenant tenant = new Tenant("Nova Elite Agency", DEFAULT_TENANT, SubscriptionPlan.ELITE);
                tenant.setContactEmail("admin@nova.agency");
                tenant.setEnabledModules(Set.of(
                                "ai_content", "finance", "advanced_analytics", "team_management"));
                tenantRepo.save(tenant);
                log.info("✅ Seeded default tenant: {}", DEFAULT_TENANT);
        }

        private void seedGlobalConfig() {
                if (configRepo.count() > 0) {
                        return;
                }
                log.info("🌱 Seeding Global Configuration...");
                GlobalConfig config = new GlobalConfig();
                config.setAgencyName("Nova Elite Agency");
                config.setContactEmail("contact@nova.agency");
                config.setContactPhone("+1 (555) 123-4567");
                config.setLinkedInUrl("https://linkedin.com/company/nova-agency");
                config.setTwitterUrl("https://twitter.com/nova_elite");
                config.setLogoUrl("/logo.svg");
                configRepo.save(config);
                log.info("✅ Seeded Global Configuration");
        }

        private void seedSlides() {
                if (slideRepo.count() > 0) {
                        return;
                }
                log.info("🌱 Seeding Hero Slides...");
                Slide s1 = new Slide();
                s1.setTitle("Elite Software Engineering");
                s1.setSubtitle("We build mission-critical enterprise platforms.");
                s1.setCtaText("Our Services");
                s1.setCtaLink("/services");
                s1.setDisplayOrder(1);

                Slide s2 = new Slide();
                s2.setTitle("AI-Powered Automation");
                s2.setSubtitle("Scale your workflows with tailored Artificial Intelligence.");
                s2.setCtaText("View Projects");
                s2.setCtaLink("/portfolio");
                s2.setDisplayOrder(2);

                slideRepo.saveAll(List.of(s1, s2));
                log.info("✅ Seeded {} Hero Slides", 2);
        }

        private void seedServices() {
                if (serviceRepo.count() > 0) {
                        log.info("⚡ Services already seeded — skipping ({} records found)", serviceRepo.count());
                        return;
                }

                log.info("🌱 Seeding Agency Services...");

                AgencyService s1 = new AgencyService();
                s1.setTitle("AI-Driven Digitalization");
                s1.setSlug("ai-digitalization");
                s1.setDescription(
                                "Transform your enterprise with cutting-edge artificial intelligence solutions. "
                                                + "From predictive analytics to intelligent automation, we architect AI pipelines "
                                                + "that turn raw data into strategic business advantages.");
                s1.setIconName("Cpu");
                s1.setImage(new ImageMetadata("/images/services/ai-digitalization.webp",
                                "AI neural network visualization", 800, 600));
                s1.setDisplayOrder(1);
                s1.setActive(true);

                AgencyService s2 = new AgencyService();
                s2.setTitle("Enterprise Cloud Architecture");
                s2.setSlug("cloud-architecture");
                s2.setDescription(
                                "Design, migrate, and optimize cloud-native infrastructures built for scale. "
                                                + "Our certified architects deploy resilient multi-cloud environments on AWS, Azure, "
                                                + "and GCP with zero-downtime guarantees.");
                s2.setIconName("Cloud");
                s2.setImage(new ImageMetadata("/images/services/cloud-architecture.webp",
                                "Multi-cloud infrastructure diagram", 800, 600));
                s2.setDisplayOrder(2);
                s2.setActive(true);

                AgencyService s3 = new AgencyService();
                s3.setTitle("Cybersecurity & Compliance");
                s3.setSlug("cybersecurity-compliance");
                s3.setDescription(
                                "Protect your digital assets with enterprise-grade security frameworks. "
                                                + "We implement ISO 27001, SOC 2, and GDPR-compliant architectures with "
                                                + "24/7 threat monitoring and incident response.");
                s3.setIconName("Shield");
                s3.setImage(new ImageMetadata("/images/services/cybersecurity.webp",
                                "Enterprise security operations center", 800, 600));
                s3.setDisplayOrder(3);
                s3.setActive(true);

                AgencyService s4 = new AgencyService();
                s4.setTitle("Custom Software Engineering");
                s4.setSlug("custom-software");
                s4.setDescription(
                                "End-to-end software development tailored to your business processes. "
                                                + "We build scalable microservices, APIs, and full-stack platforms using "
                                                + "Java 21, Spring Boot, React, and Next.js.");
                s4.setIconName("Code");
                s4.setImage(new ImageMetadata("/images/services/custom-software.webp",
                                "Clean code development workspace", 800, 600));
                s4.setDisplayOrder(4);
                s4.setActive(true);

                serviceRepo.saveAll(List.of(s1, s2, s3, s4));
                log.info("✅ Seeded {} Agency Services", 4);
        }

        private void seedProjects() {
                if (projectRepo.count() > 0) {
                        log.info("⚡ Projects already seeded — skipping ({} records found)", projectRepo.count());
                        return;
                }

                log.info("🌱 Seeding Portfolio Projects...");

                Project p1 = new Project();
                p1.setName("Global Logistics Dashboard");
                p1.setClientName("TransCorp International");
                p1.setCaseStudyChallenge(
                                "TransCorp needed a real-time logistics dashboard to track 50,000+ shipments "
                                                + "across 30 countries with live GPS accuracy and automated customs documentation.");
                p1.setCaseStudySolution(
                                "We delivered a high-performance platform with live GPS tracking, predictive ETA "
                                                + "calculations powered by machine learning, and automated customs workflows integrated "
                                                + "with 12 international trade APIs.");
                p1.setCaseStudyResult(
                                "Reduced delivery delays by 34% and cut operational costs by $2.1M annually. "
                                                + "Dashboard adoption reached 98% across all regional offices within 3 months.");
                p1.setImage(new ImageMetadata("/images/projects/logistics-dashboard.webp",
                                "TransCorp global logistics dashboard showing live shipment tracking", 1920, 1080));
                p1.setDisplayOrder(1);
                p1.setTechnologies(List.of("Spring Boot", "Next.js", "Docker", "PostgreSQL", "Redis"));

                Project p2 = new Project();
                p2.setName("FinTech Payment Gateway");
                p2.setClientName("NovaPay Solutions");
                p2.setCaseStudyChallenge(
                                "NovaPay required a PCI-DSS compliant payment processing gateway capable of "
                                                + "handling 10,000 transactions per second with multi-currency support.");
                p2.setCaseStudySolution(
                                "We engineered a microservices-based platform with real-time fraud detection using "
                                                + "ML models, multi-currency settlement engine, and 99.99% uptime SLA through "
                                                + "active-active Kubernetes deployment across 3 availability zones.");
                p2.setCaseStudyResult(
                                "The system now processes over $500M in monthly transactions with a fraud detection "
                                                + "rate of 99.7% and average processing time under 200ms.");
                p2.setImage(new ImageMetadata("/images/projects/fintech-gateway.webp",
                                "NovaPay payment gateway real-time transaction monitoring interface", 1920, 1080));
                p2.setDisplayOrder(2);
                p2.setTechnologies(List.of("Java 21", "Spring Boot", "Kafka", "Kubernetes", "PostgreSQL"));

                Project p3 = new Project();
                p3.setName("Smart City IoT Platform");
                p3.setClientName("MetroVille Municipality");
                p3.setCaseStudyChallenge(
                                "MetroVille commissioned an IoT platform to manage 120,000+ connected sensors "
                                                + "across traffic, energy, and waste management systems in real-time.");
                p3.setCaseStudySolution(
                                "Our solution processes 2 million data points per minute with edge computing nodes, "
                                                + "a centralized analytics dashboard, and AI-powered predictive maintenance alerts "
                                                + "for critical city infrastructure.");
                p3.setCaseStudyResult(
                                "Reduced city energy consumption by 22%, decreased traffic congestion by 15%, and "
                                                + "achieved ROI within 18 months of deployment.");
                p3.setImage(new ImageMetadata("/images/projects/smart-city-iot.webp",
                                "MetroVille smart city IoT sensor network analytics dashboard", 1920, 1080));
                p3.setDisplayOrder(3);
                p3.setTechnologies(List.of("Spring Boot", "React", "TimescaleDB", "Docker", "MQTT"));

                projectRepo.saveAll(List.of(p1, p2, p3));
                log.info("✅ Seeded {} Portfolio Projects", 3);
        }

        private void seedPageSections() {
                if (pageSectionRepo.count() > 0) {
                        log.info("⚡ Page sections already seeded — skipping ({} records found)",
                                        pageSectionRepo.count());
                        return;
                }

                log.info("🌱 Seeding Page Sections...");

                PageSection homeHero = new PageSection();
                homeHero.setPageName("home");
                homeHero.setSectionKey("hero-slider");
                homeHero.setTitle("Digitalization Hero Slider");
                homeHero.setDescription("Manage hero messaging, CTA labels, and visual sequencing.");
                homeHero.setDisplayOrder(1);
                homeHero.setImage(new ImageMetadata("/images/slides/slide-1.webp", "Hero digitalization background",
                                1920, 1080));
                homeHero.setContentData(new LinkedHashMap<>(java.util.Map.of(
                                "ctaStyle", "primary",
                                "animation", "fade-in",
                                "source", "slides")));

                PageSection homeServices = new PageSection();
                homeServices.setPageName("home");
                homeServices.setSectionKey("services-overview");
                homeServices.setTitle("Elite Services Overview");
                homeServices.setDescription("Choose featured services and control client-facing order.");
                homeServices.setDisplayOrder(2);
                homeServices.setContentData(new LinkedHashMap<>(java.util.Map.of(
                                "source", "services",
                                "maxItems", 6,
                                "featuredSlugs",
                                java.util.List.of("ai-digitalization", "cloud-architecture", "custom-software"))));

                PageSection homePortfolio = new PageSection();
                homePortfolio.setPageName("home");
                homePortfolio.setSectionKey("portfolio-highlight");
                homePortfolio.setTitle("Success Case Studies");
                homePortfolio.setDescription("Highlight enterprise impact stories for B2B conversion.");
                homePortfolio.setDisplayOrder(3);
                homePortfolio.setContentData(new LinkedHashMap<>(java.util.Map.of(
                                "source", "projects",
                                "featuredProjectIds", java.util.List.of(1, 2),
                                "layout", "grid")));

                PageSection homeStats = new PageSection();
                homeStats.setPageName("home");
                homeStats.setSectionKey("impact-bar");
                homeStats.setTitle("Enterprise Impact Metrics");
                homeStats.setDescription("Keep social proof KPIs updated in real-time.");
                homeStats.setDisplayOrder(4);
                homeStats.setContentData(new LinkedHashMap<>(java.util.Map.of(
                                "metrics", java.util.List.of(
                                                java.util.Map.of("label", "Enterprise Clients", "value", "50+"),
                                                java.util.Map.of("label", "Average SLA", "value", "99.98%"),
                                                java.util.Map.of("label", "Countries Served", "value", "30+")))));

                PageSection aboutMission = new PageSection();
                aboutMission.setPageName("about");
                aboutMission.setSectionKey("mission-vision");
                aboutMission.setTitle("Mission & Vision");
                aboutMission.setDescription("Define Nova's digitalization philosophy and enterprise promise.");
                aboutMission.setDisplayOrder(1);
                aboutMission.setContentData(new LinkedHashMap<>(java.util.Map.of(
                                "mission", "Accelerate enterprise transformation through secure digital systems.",
                                "vision", "Become the benchmark for elite B2B digitalization delivery.")));

                PageSection aboutHistory = new PageSection();
                aboutHistory.setPageName("about");
                aboutHistory.setSectionKey("agency-history");
                aboutHistory.setTitle("Agency History");
                aboutHistory.setDescription("Manage Nova's experience narrative and background visual assets.");
                aboutHistory.setDisplayOrder(2);
                aboutHistory.setImage(new ImageMetadata("/images/about/agency-history.webp", "Nova team collaboration",
                                1600, 900));
                aboutHistory.setContentData(new LinkedHashMap<>(java.util.Map.of(
                                "timeline", java.util.List.of("2017 Founded", "2021 Enterprise scale-up",
                                                "2025 Global expansion"))));

                PageSection aboutPartners = new PageSection();
                aboutPartners.setPageName("about");
                aboutPartners.setSectionKey("partner-tech-grid");
                aboutPartners.setTitle("Partner & Tech Stack Grid");
                aboutPartners.setDescription("Showcase trusted delivery technologies and platform partners.");
                aboutPartners.setDisplayOrder(3);
                aboutPartners.setContentData(new LinkedHashMap<>(java.util.Map.of(
                                "logos", java.util.List.of(
                                                java.util.Map.of("name", "Spring Boot", "url",
                                                                "/logos/spring-boot.svg"),
                                                java.util.Map.of("name", "Next.js", "url", "/logos/nextjs.svg"),
                                                java.util.Map.of("name", "Docker", "url", "/logos/docker.svg")))));

                pageSectionRepo.saveAll(List.of(
                                homeHero,
                                homeServices,
                                homePortfolio,
                                homeStats,
                                aboutMission,
                                aboutHistory,
                                aboutPartners));
                log.info("✅ Seeded {} Page Sections", 7);
        }

        // ══════════════════════════════════════════════════════════════════════
        // LEADS (Sales Pipeline)
        // ══════════════════════════════════════════════════════════════════════

        private void seedLeads() {
                if (leadRepo.count() > 0) {
                        log.info("⚡ Leads already seeded — skipping ({} records found)", leadRepo.count());
                        return;
                }

                log.info("🌱 Seeding Sales Leads...");

                Lead l1 = new Lead();
                l1.setTenantId(DEFAULT_TENANT);
                l1.setFullName("Sarah Mitchell");
                l1.setEmail("sarah.mitchell@globalretail.com");
                l1.setPhone("+1 (212) 555-0147");
                l1.setCompanyName("GlobalRetail Corp");
                l1.setProjectTitle("E-Commerce Platform Modernization");
                l1.setProjectDescription("We need to rebuild our legacy e-commerce platform to handle 50K concurrent users with "
                        + "real-time inventory sync across 200+ retail locations. Must integrate with SAP ERP and support "
                        + "multi-currency checkout. Current Magento system handles only 5K users and crashes during sales events.");
                l1.setBudget(new BigDecimal("250000.00"));
                l1.setTimeline("6 months");
                l1.setScore(87);
                l1.setScoreBreakdown("{\"budget\":35,\"description\":28,\"company\":12,\"timeline\":7,\"contact\":5}");
                l1.setStatus(Lead.LeadStatus.QUALIFIED);
                l1.setAssignedTo("admin@nova.agency");
                l1.setNotes("High-priority lead. GlobalRetail is a Fortune 1000 company. Previous vendor failed delivery.");

                Lead l2 = new Lead();
                l2.setTenantId(DEFAULT_TENANT);
                l2.setFullName("James Rodriguez");
                l2.setEmail("j.rodriguez@medicarepro.health");
                l2.setPhone("+1 (415) 555-0289");
                l2.setCompanyName("MedicarePro Health Systems");
                l2.setProjectTitle("HIPAA-Compliant Patient Portal");
                l2.setProjectDescription("Build a patient-facing portal with telemedicine video calls, prescription management, "
                        + "and lab result viewing. Must be HIPAA compliant and integrate with Epic EHR. Expected 100K+ patients.");
                l2.setBudget(new BigDecimal("180000.00"));
                l2.setTimeline("8 months");
                l2.setScore(72);
                l2.setScoreBreakdown("{\"budget\":28,\"description\":25,\"company\":10,\"timeline\":5,\"contact\":4}");
                l2.setStatus(Lead.LeadStatus.PROPOSAL_SENT);
                l2.setAssignedTo("admin@nova.agency");

                Lead l3 = new Lead();
                l3.setTenantId(DEFAULT_TENANT);
                l3.setFullName("David Chen");
                l3.setEmail("dchen@alphaventures.io");
                l3.setPhone("+1 (650) 555-0193");
                l3.setCompanyName("Alpha Ventures Capital");
                l3.setProjectTitle("Portfolio Analytics Dashboard");
                l3.setProjectDescription("Real-time dashboard for our VC fund showing portfolio company KPIs, "
                        + "market comparisons, and LP reporting. Need data pipelines from 40+ SaaS tools.");
                l3.setBudget(new BigDecimal("120000.00"));
                l3.setTimeline("4 months");
                l3.setScore(64);
                l3.setScoreBreakdown("{\"budget\":22,\"description\":22,\"company\":8,\"timeline\":8,\"contact\":4}");
                l3.setStatus(Lead.LeadStatus.CONTACTED);

                Lead l4 = new Lead();
                l4.setTenantId(DEFAULT_TENANT);
                l4.setFullName("Amara Okafor");
                l4.setEmail("amara@techstartup.ng");
                l4.setCompanyName("TechStartup Nigeria");
                l4.setProjectTitle("Mobile Money Platform");
                l4.setProjectDescription("Need a mobile money platform supporting NFC payments, P2P transfers, and agent "
                        + "banking for the Nigerian market. Must handle 1M+ transactions per day.");
                l4.setBudget(new BigDecimal("350000.00"));
                l4.setTimeline("12 months");
                l4.setScore(91);
                l4.setScoreBreakdown("{\"budget\":35,\"description\":30,\"company\":12,\"timeline\":9,\"contact\":5}");
                l4.setStatus(Lead.LeadStatus.NEW);

                Lead l5 = new Lead();
                l5.setTenantId(DEFAULT_TENANT);
                l5.setFullName("Erik Johansson");
                l5.setEmail("erik@nordiclogistics.se");
                l5.setCompanyName("Nordic Logistics AB");
                l5.setProjectTitle("Fleet Management IoT System");
                l5.setProjectDescription("GPS tracking and fleet management for 2,000 delivery vehicles across Scandinavia. "
                        + "Real-time route optimization with weather and traffic data integration.");
                l5.setBudget(new BigDecimal("95000.00"));
                l5.setTimeline("5 months");
                l5.setScore(55);
                l5.setScoreBreakdown("{\"budget\":18,\"description\":20,\"company\":8,\"timeline\":6,\"contact\":3}");
                l5.setStatus(Lead.LeadStatus.WON);

                Lead l6 = new Lead();
                l6.setTenantId(DEFAULT_TENANT);
                l6.setFullName("Lisa Park");
                l6.setEmail("lisa.park@retailchain.co");
                l6.setCompanyName("RetailChain Inc");
                l6.setProjectTitle("POS System Upgrade");
                l6.setProjectDescription("Upgrade point-of-sale terminals across 50 locations.");
                l6.setBudget(new BigDecimal("15000.00"));
                l6.setTimeline("1 month");
                l6.setScore(23);
                l6.setScoreBreakdown("{\"budget\":5,\"description\":8,\"company\":4,\"timeline\":3,\"contact\":3}");
                l6.setStatus(Lead.LeadStatus.LOST);
                l6.setNotes("Budget too low for scope. Referred to freelance partner.");

                leadRepo.saveAll(List.of(l1, l2, l3, l4, l5, l6));
                log.info("✅ Seeded {} Sales Leads", 6);
        }

        // ══════════════════════════════════════════════════════════════════════
        // INVOICES
        // ══════════════════════════════════════════════════════════════════════

        private void seedInvoices() {
                if (invoiceRepo.count() > 0) {
                        log.info("⚡ Invoices already seeded — skipping ({} records found)", invoiceRepo.count());
                        return;
                }

                log.info("🌱 Seeding Invoices...");

                List<Project> projects = projectRepo.findAll();
                Project p1 = projects.isEmpty() ? null : projects.get(0);
                Project p2 = projects.size() > 1 ? projects.get(1) : null;
                Project p3 = projects.size() > 2 ? projects.get(2) : null;

                Invoice inv1 = new Invoice();
                inv1.setTenantId(DEFAULT_TENANT);
                inv1.setReferenceNumber("INV-2026-001");
                inv1.setAmount(new BigDecimal("45000.00"));
                inv1.setStatus(Invoice.InvoiceStatus.PAID);
                inv1.setDueDate(LocalDate.of(2026, 1, 15));
                inv1.setProject(p1);

                Invoice inv2 = new Invoice();
                inv2.setTenantId(DEFAULT_TENANT);
                inv2.setReferenceNumber("INV-2026-002");
                inv2.setAmount(new BigDecimal("32500.00"));
                inv2.setStatus(Invoice.InvoiceStatus.PAID);
                inv2.setDueDate(LocalDate.of(2026, 1, 30));
                inv2.setProject(p2);

                Invoice inv3 = new Invoice();
                inv3.setTenantId(DEFAULT_TENANT);
                inv3.setReferenceNumber("INV-2026-003");
                inv3.setAmount(new BigDecimal("67000.00"));
                inv3.setStatus(Invoice.InvoiceStatus.PENDING);
                inv3.setDueDate(LocalDate.of(2026, 3, 15));
                inv3.setProject(p1);

                Invoice inv4 = new Invoice();
                inv4.setTenantId(DEFAULT_TENANT);
                inv4.setReferenceNumber("INV-2026-004");
                inv4.setAmount(new BigDecimal("28000.00"));
                inv4.setStatus(Invoice.InvoiceStatus.OVERDUE);
                inv4.setDueDate(LocalDate.of(2026, 2, 1));
                inv4.setProject(p3);

                Invoice inv5 = new Invoice();
                inv5.setTenantId(DEFAULT_TENANT);
                inv5.setReferenceNumber("INV-2026-005");
                inv5.setAmount(new BigDecimal("15750.00"));
                inv5.setStatus(Invoice.InvoiceStatus.PENDING);
                inv5.setDueDate(LocalDate.of(2026, 4, 1));
                inv5.setProject(p2);

                Invoice inv6 = new Invoice();
                inv6.setTenantId(DEFAULT_TENANT);
                inv6.setReferenceNumber("INV-2026-006");
                inv6.setAmount(new BigDecimal("8500.00"));
                inv6.setStatus(Invoice.InvoiceStatus.CANCELLED);
                inv6.setDueDate(LocalDate.of(2026, 2, 15));
                inv6.setProject(p3);

                invoiceRepo.saveAll(List.of(inv1, inv2, inv3, inv4, inv5, inv6));
                log.info("✅ Seeded {} Invoices", 6);
        }

        // ══════════════════════════════════════════════════════════════════════
        // EXPENSES
        // ══════════════════════════════════════════════════════════════════════

        private void seedExpenses() {
                if (expenseRepo.count() > 0) {
                        log.info("⚡ Expenses already seeded — skipping ({} records found)", expenseRepo.count());
                        return;
                }

                log.info("🌱 Seeding Expenses...");

                Expense e1 = new Expense();
                e1.setTenantId(DEFAULT_TENANT);
                e1.setDescription("AWS EC2 & RDS - January 2026");
                e1.setAmount(new BigDecimal("4250.00"));
                e1.setCategory(Expense.ExpenseCategory.CLOUD_HOSTING);
                e1.setVendor("Amazon Web Services");
                e1.setExpenseDate(LocalDate.of(2026, 1, 31));
                e1.setReferenceNumber("AWS-JAN-2026");
                e1.setRecurring(true);

                Expense e2 = new Expense();
                e2.setTenantId(DEFAULT_TENANT);
                e2.setDescription("AWS EC2 & RDS - February 2026");
                e2.setAmount(new BigDecimal("4680.00"));
                e2.setCategory(Expense.ExpenseCategory.CLOUD_HOSTING);
                e2.setVendor("Amazon Web Services");
                e2.setExpenseDate(LocalDate.of(2026, 2, 28));
                e2.setReferenceNumber("AWS-FEB-2026");
                e2.setRecurring(true);

                Expense e3 = new Expense();
                e3.setTenantId(DEFAULT_TENANT);
                e3.setDescription("JetBrains IntelliJ IDEA Ultimate - Annual License (5 seats)");
                e3.setAmount(new BigDecimal("2495.00"));
                e3.setCategory(Expense.ExpenseCategory.SOFTWARE_LICENSE);
                e3.setVendor("JetBrains s.r.o.");
                e3.setExpenseDate(LocalDate.of(2026, 1, 5));
                e3.setReferenceNumber("JB-2026-ANNUAL");
                e3.setRecurring(true);

                Expense e4 = new Expense();
                e4.setTenantId(DEFAULT_TENANT);
                e4.setDescription("Google Ads Campaign - Q1 Enterprise Lead Gen");
                e4.setAmount(new BigDecimal("8500.00"));
                e4.setCategory(Expense.ExpenseCategory.MARKETING);
                e4.setVendor("Google LLC");
                e4.setExpenseDate(LocalDate.of(2026, 1, 15));
                e4.setReferenceNumber("GADS-Q1-2026");

                Expense e5 = new Expense();
                e5.setTenantId(DEFAULT_TENANT);
                e5.setDescription("Senior Developer Salary - January 2026");
                e5.setAmount(new BigDecimal("12000.00"));
                e5.setCategory(Expense.ExpenseCategory.PAYROLL);
                e5.setVendor("Internal");
                e5.setExpenseDate(LocalDate.of(2026, 1, 31));
                e5.setReferenceNumber("PAY-JAN-SR-DEV");
                e5.setRecurring(true);

                Expense e6 = new Expense();
                e6.setTenantId(DEFAULT_TENANT);
                e6.setDescription("Senior Developer Salary - February 2026");
                e6.setAmount(new BigDecimal("12000.00"));
                e6.setCategory(Expense.ExpenseCategory.PAYROLL);
                e6.setVendor("Internal");
                e6.setExpenseDate(LocalDate.of(2026, 2, 28));
                e6.setReferenceNumber("PAY-FEB-SR-DEV");
                e6.setRecurring(true);

                Expense e7 = new Expense();
                e7.setTenantId(DEFAULT_TENANT);
                e7.setDescription("Co-working Space Rent - Q1 2026");
                e7.setAmount(new BigDecimal("4500.00"));
                e7.setCategory(Expense.ExpenseCategory.OFFICE);
                e7.setVendor("WeWork");
                e7.setExpenseDate(LocalDate.of(2026, 1, 1));
                e7.setReferenceNumber("WEWORK-Q1-2026");
                e7.setRecurring(true);

                Expense e8 = new Expense();
                e8.setTenantId(DEFAULT_TENANT);
                e8.setDescription("MacBook Pro M4 - New Developer Workstation");
                e8.setAmount(new BigDecimal("3499.00"));
                e8.setCategory(Expense.ExpenseCategory.HARDWARE);
                e8.setVendor("Apple Inc.");
                e8.setExpenseDate(LocalDate.of(2026, 2, 10));
                e8.setReferenceNumber("APPLE-MBP-2026");

                Expense e9 = new Expense();
                e9.setTenantId(DEFAULT_TENANT);
                e9.setDescription("KubeCon Europe 2026 - Conference + Travel");
                e9.setAmount(new BigDecimal("3200.00"));
                e9.setCategory(Expense.ExpenseCategory.TRAVEL);
                e9.setVendor("CNCF");
                e9.setExpenseDate(LocalDate.of(2026, 3, 1));
                e9.setReferenceNumber("KUBECON-EU-2026");

                Expense e10 = new Expense();
                e10.setTenantId(DEFAULT_TENANT);
                e10.setDescription("Security Penetration Test - Q1 Audit");
                e10.setAmount(new BigDecimal("7500.00"));
                e10.setCategory(Expense.ExpenseCategory.CONSULTING);
                e10.setVendor("CyberShield Security");
                e10.setExpenseDate(LocalDate.of(2026, 2, 20));
                e10.setReferenceNumber("PENTEST-Q1-2026");

                expenseRepo.saveAll(List.of(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10));
                log.info("✅ Seeded {} Expenses", 10);
        }

        // ══════════════════════════════════════════════════════════════════════
        // PROPOSALS
        // ══════════════════════════════════════════════════════════════════════

        private void seedProposals() {
                if (proposalRepo.count() > 0) {
                        log.info("⚡ Proposals already seeded — skipping ({} records found)", proposalRepo.count());
                        return;
                }

                List<Lead> leads = leadRepo.findAll();
                if (leads.size() < 2) {
                        log.warn("⚠️ Not enough leads to seed proposals — skipping");
                        return;
                }

                log.info("🌱 Seeding Proposals...");

                // Proposal for the QUALIFIED lead (GlobalRetail)
                Lead qualifiedLead = leads.stream()
                        .filter(l -> l.getStatus() == Lead.LeadStatus.QUALIFIED)
                        .findFirst().orElse(leads.get(0));

                Proposal prop1 = new Proposal();
                prop1.setTenantId(DEFAULT_TENANT);
                prop1.setProposalNumber("PROP-2026-001");
                prop1.setLead(qualifiedLead);
                prop1.setTitle("E-Commerce Platform Modernization — Technical Proposal");
                prop1.setScopeOfWork("Full-stack rebuild of legacy Magento e-commerce platform to a cloud-native "
                        + "microservices architecture using Spring Boot, Next.js, and PostgreSQL. Includes real-time "
                        + "inventory sync across 200+ locations, multi-currency checkout, and SAP ERP integration.");
                prop1.setDeliverables("1. Architecture design document and system blueprint\n"
                        + "2. Microservices backend (Spring Boot 4 + Java 21)\n"
                        + "3. Next.js storefront with SSR/ISR\n"
                        + "4. SAP ERP integration layer\n"
                        + "5. Real-time inventory sync engine\n"
                        + "6. Load testing report (50K concurrent users)\n"
                        + "7. 12-month warranty and SLA");
                prop1.setTermsAndConditions("Payment: 30% deposit, 40% at milestone 3, 30% on delivery. "
                        + "Warranty: 12 months post-launch bug fixes included. IP transfer upon final payment.");
                prop1.setTotalAmount(new BigDecimal("250000.00"));
                prop1.setDepositPercent(30);
                prop1.setCurrency("USD");
                prop1.setValidUntil(LocalDate.of(2026, 4, 15));
                prop1.setSigningToken(UUID.randomUUID().toString());
                prop1.setStatus(Proposal.ProposalStatus.SENT);

                // Line items for prop1
                ProposalLineItem li1 = new ProposalLineItem();
                li1.setProposal(prop1);
                li1.setDescription("Architecture Design & System Blueprint");
                li1.setQuantity(1);
                li1.setUnitPrice(new BigDecimal("25000.00"));
                li1.setDisplayOrder(1);

                ProposalLineItem li2 = new ProposalLineItem();
                li2.setProposal(prop1);
                li2.setDescription("Backend Microservices Development (8 services)");
                li2.setQuantity(8);
                li2.setUnitPrice(new BigDecimal("15000.00"));
                li2.setDisplayOrder(2);

                ProposalLineItem li3 = new ProposalLineItem();
                li3.setProposal(prop1);
                li3.setDescription("Next.js Storefront Development");
                li3.setQuantity(1);
                li3.setUnitPrice(new BigDecimal("45000.00"));
                li3.setDisplayOrder(3);

                ProposalLineItem li4 = new ProposalLineItem();
                li4.setProposal(prop1);
                li4.setDescription("SAP ERP Integration");
                li4.setQuantity(1);
                li4.setUnitPrice(new BigDecimal("35000.00"));
                li4.setDisplayOrder(4);

                ProposalLineItem li5 = new ProposalLineItem();
                li5.setProposal(prop1);
                li5.setDescription("Load Testing & Performance Optimization");
                li5.setQuantity(1);
                li5.setUnitPrice(new BigDecimal("25000.00"));
                li5.setDisplayOrder(5);

                prop1.setLineItems(List.of(li1, li2, li3, li4, li5));

                // Proposal for the PROPOSAL_SENT lead (MedicarePro)
                Lead proposalLead = leads.stream()
                        .filter(l -> l.getStatus() == Lead.LeadStatus.PROPOSAL_SENT)
                        .findFirst().orElse(leads.get(1));

                Proposal prop2 = new Proposal();
                prop2.setTenantId(DEFAULT_TENANT);
                prop2.setProposalNumber("PROP-2026-002");
                prop2.setLead(proposalLead);
                prop2.setTitle("HIPAA-Compliant Patient Portal — Technical Proposal");
                prop2.setScopeOfWork("Design and build a HIPAA-compliant patient portal with telemedicine capabilities, "
                        + "prescription management, and secure lab result viewing. Integration with Epic EHR system.");
                prop2.setDeliverables("1. HIPAA compliance architecture\n"
                        + "2. Patient portal (React + Spring Boot)\n"
                        + "3. Video telemedicine module (WebRTC)\n"
                        + "4. Epic EHR integration\n"
                        + "5. Security audit and penetration test\n"
                        + "6. HIPAA documentation package");
                prop2.setTermsAndConditions("Payment: 30% deposit, 30% at midpoint, 40% on delivery. "
                        + "Compliance guarantee: Full HIPAA compliance certification included.");
                prop2.setTotalAmount(new BigDecimal("180000.00"));
                prop2.setDepositPercent(30);
                prop2.setCurrency("USD");
                prop2.setValidUntil(LocalDate.of(2026, 5, 1));
                prop2.setSigningToken(UUID.randomUUID().toString());
                prop2.setStatus(Proposal.ProposalStatus.VIEWED);

                ProposalLineItem li6 = new ProposalLineItem();
                li6.setProposal(prop2);
                li6.setDescription("HIPAA Compliance Architecture & Documentation");
                li6.setQuantity(1);
                li6.setUnitPrice(new BigDecimal("30000.00"));
                li6.setDisplayOrder(1);

                ProposalLineItem li7 = new ProposalLineItem();
                li7.setProposal(prop2);
                li7.setDescription("Patient Portal Development");
                li7.setQuantity(1);
                li7.setUnitPrice(new BigDecimal("65000.00"));
                li7.setDisplayOrder(2);

                ProposalLineItem li8 = new ProposalLineItem();
                li8.setProposal(prop2);
                li8.setDescription("Telemedicine Video Module");
                li8.setQuantity(1);
                li8.setUnitPrice(new BigDecimal("40000.00"));
                li8.setDisplayOrder(3);

                ProposalLineItem li9 = new ProposalLineItem();
                li9.setProposal(prop2);
                li9.setDescription("Epic EHR Integration");
                li9.setQuantity(1);
                li9.setUnitPrice(new BigDecimal("35000.00"));
                li9.setDisplayOrder(4);

                ProposalLineItem li10 = new ProposalLineItem();
                li10.setProposal(prop2);
                li10.setDescription("Security Audit & Penetration Testing");
                li10.setQuantity(1);
                li10.setUnitPrice(new BigDecimal("10000.00"));
                li10.setDisplayOrder(5);

                prop2.setLineItems(List.of(li6, li7, li8, li9, li10));

                proposalRepo.saveAll(List.of(prop1, prop2));
                log.info("✅ Seeded {} Proposals with line items", 2);
        }

        // ══════════════════════════════════════════════════════════════════════
        // SKILL NODES (Gamification)
        // ══════════════════════════════════════════════════════════════════════

        private void seedSkillNodes() {
                if (skillNodeRepo.count() > 0) {
                        log.info("⚡ Skill nodes already seeded — skipping ({} records found)", skillNodeRepo.count());
                        return;
                }

                log.info("🌱 Seeding Skill Tree Nodes...");

                SkillNode sn1 = new SkillNode();
                sn1.setTenantId(DEFAULT_TENANT);
                sn1.setTechnologyName("Spring Boot");
                sn1.setXp(850);
                sn1.setLevel(5);
                sn1.setCategory(SkillNode.SkillCategory.BACKEND_FRAMEWORK);
                sn1.setProjectCount(4);

                SkillNode sn2 = new SkillNode();
                sn2.setTenantId(DEFAULT_TENANT);
                sn2.setTechnologyName("Java 21");
                sn2.setXp(1200);
                sn2.setLevel(7);
                sn2.setCategory(SkillNode.SkillCategory.JAVA_FUNDAMENTALS);
                sn2.setProjectCount(4);

                SkillNode sn3 = new SkillNode();
                sn3.setTenantId(DEFAULT_TENANT);
                sn3.setTechnologyName("Next.js");
                sn3.setXp(650);
                sn3.setLevel(4);
                sn3.setCategory(SkillNode.SkillCategory.FRONTEND_FRAMEWORK);
                sn3.setProjectCount(2);

                SkillNode sn4 = new SkillNode();
                sn4.setTenantId(DEFAULT_TENANT);
                sn4.setTechnologyName("PostgreSQL");
                sn4.setXp(700);
                sn4.setLevel(4);
                sn4.setCategory(SkillNode.SkillCategory.JDBC_DATABASE);
                sn4.setProjectCount(3);

                SkillNode sn5 = new SkillNode();
                sn5.setTenantId(DEFAULT_TENANT);
                sn5.setTechnologyName("Docker");
                sn5.setXp(550);
                sn5.setLevel(3);
                sn5.setCategory(SkillNode.SkillCategory.CLOUD_DEVOPS);
                sn5.setProjectCount(3);

                SkillNode sn6 = new SkillNode();
                sn6.setTenantId(DEFAULT_TENANT);
                sn6.setTechnologyName("Redis");
                sn6.setXp(300);
                sn6.setLevel(2);
                sn6.setCategory(SkillNode.SkillCategory.JDBC_DATABASE);
                sn6.setProjectCount(1);

                SkillNode sn7 = new SkillNode();
                sn7.setTenantId(DEFAULT_TENANT);
                sn7.setTechnologyName("Kafka");
                sn7.setXp(250);
                sn7.setLevel(2);
                sn7.setCategory(SkillNode.SkillCategory.CONCURRENCY);
                sn7.setProjectCount(1);

                SkillNode sn8 = new SkillNode();
                sn8.setTenantId(DEFAULT_TENANT);
                sn8.setTechnologyName("Kubernetes");
                sn8.setXp(400);
                sn8.setLevel(3);
                sn8.setCategory(SkillNode.SkillCategory.CLOUD_DEVOPS);
                sn8.setProjectCount(1);

                SkillNode sn9 = new SkillNode();
                sn9.setTenantId(DEFAULT_TENANT);
                sn9.setTechnologyName("React");
                sn9.setXp(500);
                sn9.setLevel(3);
                sn9.setCategory(SkillNode.SkillCategory.FRONTEND_FRAMEWORK);
                sn9.setProjectCount(2);

                SkillNode sn10 = new SkillNode();
                sn10.setTenantId(DEFAULT_TENANT);
                sn10.setTechnologyName("TimescaleDB");
                sn10.setXp(100);
                sn10.setLevel(1);
                sn10.setCategory(SkillNode.SkillCategory.JDBC_DATABASE);
                sn10.setProjectCount(1);

                SkillNode sn11 = new SkillNode();
                sn11.setTenantId(DEFAULT_TENANT);
                sn11.setTechnologyName("MQTT");
                sn11.setXp(100);
                sn11.setLevel(1);
                sn11.setCategory(SkillNode.SkillCategory.IO_NIO);
                sn11.setProjectCount(1);

                skillNodeRepo.saveAll(List.of(sn1, sn2, sn3, sn4, sn5, sn6, sn7, sn8, sn9, sn10, sn11));
                log.info("✅ Seeded {} Skill Nodes", 11);
        }

        // ══════════════════════════════════════════════════════════════════════
        // SPRINT SESSIONS (Gamification)
        // ══════════════════════════════════════════════════════════════════════

        private void seedSprintSessions() {
                if (sprintRepo.count() > 0) {
                        log.info("⚡ Sprint sessions already seeded — skipping ({} records found)", sprintRepo.count());
                        return;
                }

                log.info("🌱 Seeding Sprint Sessions...");

                User admin = userRepo.findByEmail("admin@nova.agency").orElse(null);
                if (admin == null) {
                        log.warn("⚠️ Admin user not found — skipping sprint seeds");
                        return;
                }
                Long userId = admin.getId();
                Instant now = Instant.now();

                SprintSession ss1 = new SprintSession();
                ss1.setTenantId(DEFAULT_TENANT);
                ss1.setUserId(userId);
                ss1.setTaskDescription("Implement SkillNode entity and repository");
                ss1.setTargetMinutes(45);
                ss1.setStartedAt(now.minus(7, ChronoUnit.DAYS));
                ss1.setCompletedAt(now.minus(7, ChronoUnit.DAYS).plus(42, ChronoUnit.MINUTES));
                ss1.setActualMinutes(42);
                ss1.setStatus(SprintSession.SprintStatus.COMPLETED);
                ss1.setFocusScore(92);
                ss1.setXpAwarded(42);

                SprintSession ss2 = new SprintSession();
                ss2.setTenantId(DEFAULT_TENANT);
                ss2.setUserId(userId);
                ss2.setTaskDescription("Build Sprint Challenge service layer");
                ss2.setTargetMinutes(60);
                ss2.setStartedAt(now.minus(6, ChronoUnit.DAYS));
                ss2.setCompletedAt(now.minus(6, ChronoUnit.DAYS).plus(58, ChronoUnit.MINUTES));
                ss2.setActualMinutes(58);
                ss2.setStatus(SprintSession.SprintStatus.COMPLETED);
                ss2.setFocusScore(88);
                ss2.setXpAwarded(56);

                SprintSession ss3 = new SprintSession();
                ss3.setTenantId(DEFAULT_TENANT);
                ss3.setUserId(userId);
                ss3.setTaskDescription("Design career roadmap visualization");
                ss3.setTargetMinutes(25);
                ss3.setStartedAt(now.minus(5, ChronoUnit.DAYS));
                ss3.setCompletedAt(now.minus(5, ChronoUnit.DAYS).plus(25, ChronoUnit.MINUTES));
                ss3.setActualMinutes(25);
                ss3.setStatus(SprintSession.SprintStatus.COMPLETED);
                ss3.setFocusScore(95);
                ss3.setXpAwarded(24);

                SprintSession ss4 = new SprintSession();
                ss4.setTenantId(DEFAULT_TENANT);
                ss4.setUserId(userId);
                ss4.setTaskDescription("Integrate Stripe payment webhook");
                ss4.setTargetMinutes(45);
                ss4.setStartedAt(now.minus(4, ChronoUnit.DAYS));
                ss4.setCompletedAt(now.minus(4, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES));
                ss4.setActualMinutes(30);
                ss4.setStatus(SprintSession.SprintStatus.ABANDONED);
                ss4.setFocusScore(45);
                ss4.setXpAwarded(0);

                SprintSession ss5 = new SprintSession();
                ss5.setTenantId(DEFAULT_TENANT);
                ss5.setUserId(userId);
                ss5.setTaskDescription("Refactor SecurityConfig for API keys");
                ss5.setTargetMinutes(25);
                ss5.setStartedAt(now.minus(3, ChronoUnit.DAYS));
                ss5.setCompletedAt(now.minus(3, ChronoUnit.DAYS).plus(24, ChronoUnit.MINUTES));
                ss5.setActualMinutes(24);
                ss5.setStatus(SprintSession.SprintStatus.COMPLETED);
                ss5.setFocusScore(85);
                ss5.setXpAwarded(22);

                SprintSession ss6 = new SprintSession();
                ss6.setTenantId(DEFAULT_TENANT);
                ss6.setUserId(userId);
                ss6.setTaskDescription("Write unit tests for Lead Scoring");
                ss6.setTargetMinutes(60);
                ss6.setStartedAt(now.minus(2, ChronoUnit.DAYS));
                ss6.setCompletedAt(now.minus(2, ChronoUnit.DAYS).plus(60, ChronoUnit.MINUTES));
                ss6.setActualMinutes(60);
                ss6.setStatus(SprintSession.SprintStatus.COMPLETED);
                ss6.setFocusScore(91);
                ss6.setXpAwarded(85);

                SprintSession ss7 = new SprintSession();
                ss7.setTenantId(DEFAULT_TENANT);
                ss7.setUserId(userId);
                ss7.setTaskDescription("Build admin dashboard skill tree page");
                ss7.setTargetMinutes(45);
                ss7.setStartedAt(now.minus(1, ChronoUnit.DAYS));
                ss7.setCompletedAt(now.minus(1, ChronoUnit.DAYS).plus(45, ChronoUnit.MINUTES));
                ss7.setActualMinutes(45);
                ss7.setStatus(SprintSession.SprintStatus.COMPLETED);
                ss7.setFocusScore(78);
                ss7.setXpAwarded(39);

                SprintSession ss8 = new SprintSession();
                ss8.setTenantId(DEFAULT_TENANT);
                ss8.setUserId(userId);
                ss8.setTaskDescription("Deploy Docker Compose production config");
                ss8.setTargetMinutes(25);
                ss8.setStartedAt(now.minus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS));
                ss8.setCompletedAt(now.minus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS).plus(25, ChronoUnit.MINUTES));
                ss8.setActualMinutes(25);
                ss8.setStatus(SprintSession.SprintStatus.COMPLETED);
                ss8.setFocusScore(82);
                ss8.setXpAwarded(21);

                sprintRepo.saveAll(List.of(ss1, ss2, ss3, ss4, ss5, ss6, ss7, ss8));
                log.info("✅ Seeded {} Sprint Sessions", 8);
        }

        // ══════════════════════════════════════════════════════════════════════
        // BADGES (Gamification)
        // ══════════════════════════════════════════════════════════════════════

        private void seedBadges() {
                if (badgeRepo.count() > 0) {
                        log.info("⚡ Badges already seeded — skipping ({} records found)", badgeRepo.count());
                        return;
                }

                User admin = userRepo.findByEmail("admin@nova.agency").orElse(null);
                if (admin == null) {
                        log.warn("⚠️ Admin user not found — skipping badge seeds");
                        return;
                }

                log.info("🌱 Seeding Badges...");
                Long userId = admin.getId();
                Instant now = Instant.now();

                Badge b1 = new Badge();
                b1.setTenantId(DEFAULT_TENANT);
                b1.setUserId(userId);
                b1.setBadgeType(Badge.BadgeType.FIRST_SPRINT);
                b1.setDescription("Completed your first focus sprint!");
                b1.setEarnedAt(now.minus(7, ChronoUnit.DAYS));

                Badge b2 = new Badge();
                b2.setTenantId(DEFAULT_TENANT);
                b2.setUserId(userId);
                b2.setBadgeType(Badge.BadgeType.SPRINT_STREAK_3);
                b2.setDescription("Completed 3 sprints in a single day!");
                b2.setEarnedAt(now.minus(5, ChronoUnit.DAYS));

                Badge b3 = new Badge();
                b3.setTenantId(DEFAULT_TENANT);
                b3.setUserId(userId);
                b3.setBadgeType(Badge.BadgeType.DEEP_FOCUS);
                b3.setDescription("Completed a 60-minute sprint with 91+ focus score");
                b3.setMetadata("Lead Scoring Tests — 60min, focus: 91");
                b3.setEarnedAt(now.minus(2, ChronoUnit.DAYS));

                Badge b4 = new Badge();
                b4.setTenantId(DEFAULT_TENANT);
                b4.setUserId(userId);
                b4.setBadgeType(Badge.BadgeType.TECH_EXPERT);
                b4.setDescription("Reached level 5 in Spring Boot");
                b4.setMetadata("Spring Boot");
                b4.setEarnedAt(now.minus(3, ChronoUnit.DAYS));

                Badge b5 = new Badge();
                b5.setTenantId(DEFAULT_TENANT);
                b5.setUserId(userId);
                b5.setBadgeType(Badge.BadgeType.TECH_MASTER);
                b5.setDescription("Reached level 7 in Java 21 — true mastery!");
                b5.setMetadata("Java 21");
                b5.setEarnedAt(now.minus(1, ChronoUnit.DAYS));

                Badge b6 = new Badge();
                b6.setTenantId(DEFAULT_TENANT);
                b6.setUserId(userId);
                b6.setBadgeType(Badge.BadgeType.POLYGLOT);
                b6.setDescription("Proficient in 5+ technologies at level 3+");
                b6.setEarnedAt(now.minus(1, ChronoUnit.DAYS));

                Badge b7 = new Badge();
                b7.setTenantId(DEFAULT_TENANT);
                b7.setUserId(userId);
                b7.setBadgeType(Badge.BadgeType.FIRST_PROJECT);
                b7.setDescription("Completed your first project!");
                b7.setEarnedAt(now.minus(30, ChronoUnit.DAYS));

                badgeRepo.saveAll(List.of(b1, b2, b3, b4, b5, b6, b7));
                log.info("✅ Seeded {} Badges", 7);
        }

        // ══════════════════════════════════════════════════════════════════════
        // CLIENT USERS (Client Portal)
        // ══════════════════════════════════════════════════════════════════════

        private void seedClientUsers() {
                if (clientUserRepo.count() > 0) {
                        log.info("⚡ Client users already seeded — skipping ({} records found)", clientUserRepo.count());
                        return;
                }

                log.info("🌱 Seeding Client Portal Users...");

                ClientUser c1 = new ClientUser();
                c1.setTenantId(DEFAULT_TENANT);
                c1.setFullName("Michael Torres");
                c1.setEmail("m.torres@transcorp.com");
                c1.setPasswordHash(passwordEncoder.encode("client2026!"));
                c1.setCompanyName("TransCorp International");
                c1.setInviteAccepted(true);
                c1.setActive(true);

                ClientUser c2 = new ClientUser();
                c2.setTenantId(DEFAULT_TENANT);
                c2.setFullName("Anna Kowalski");
                c2.setEmail("anna.k@novapay.io");
                c2.setPasswordHash(passwordEncoder.encode("client2026!"));
                c2.setCompanyName("NovaPay Solutions");
                c2.setInviteAccepted(true);
                c2.setActive(true);

                ClientUser c3 = new ClientUser();
                c3.setTenantId(DEFAULT_TENANT);
                c3.setFullName("Raj Patel");
                c3.setEmail("raj@metroville.gov");
                c3.setPasswordHash(passwordEncoder.encode("client2026!"));
                c3.setCompanyName("MetroVille Municipality");
                c3.setInviteToken(UUID.randomUUID().toString());
                c3.setInviteAccepted(false);
                c3.setActive(true);

                clientUserRepo.saveAll(List.of(c1, c2, c3));
                log.info("✅ Seeded {} Client Portal Users", 3);
        }

        // ══════════════════════════════════════════════════════════════════════
        // SUBSCRIPTION
        // ══════════════════════════════════════════════════════════════════════

        private void seedSubscription() {
                if (subscriptionRepo.count() > 0) {
                        log.info("⚡ Subscription already seeded — skipping ({} records found)", subscriptionRepo.count());
                        return;
                }

                log.info("🌱 Seeding Subscription...");

                Subscription sub = new Subscription();
                sub.setTenantId(DEFAULT_TENANT);
                sub.setPlanName("Elite");
                sub.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                sub.setStartDate(Instant.parse("2026-01-01T00:00:00Z"));
                sub.setEndDate(Instant.parse("2027-01-01T00:00:00Z"));

                subscriptionRepo.save(sub);
                log.info("✅ Seeded Subscription");
        }

        // ══════════════════════════════════════════════════════════════════════
        // NOTIFICATIONS
        // ══════════════════════════════════════════════════════════════════════

        private void seedNotifications() {
                if (notificationRepo.count() > 0) {
                        log.info("⚡ Notifications already seeded — skipping ({} records found)", notificationRepo.count());
                        return;
                }

                User admin = userRepo.findByEmail("admin@nova.agency").orElse(null);
                if (admin == null) {
                        log.warn("⚠️ Admin user not found — skipping notification seeds");
                        return;
                }

                log.info("🌱 Seeding Notifications...");
                Long userId = admin.getId();

                Notification n1 = new Notification(DEFAULT_TENANT, userId,
                        "New Lead Received",
                        "Amara Okafor from TechStartup Nigeria submitted a $350K lead for Mobile Money Platform.",
                        Notification.NotificationType.INFO);

                Notification n2 = new Notification(DEFAULT_TENANT, userId,
                        "Invoice Overdue",
                        "Invoice INV-2026-004 ($28,000) for Smart City IoT Platform is now overdue.",
                        Notification.NotificationType.WARNING);

                Notification n3 = new Notification(DEFAULT_TENANT, userId,
                        "Proposal Viewed",
                        "James Rodriguez viewed the HIPAA Patient Portal proposal (PROP-2026-002).",
                        Notification.NotificationType.SUCCESS);

                Notification n4 = new Notification(DEFAULT_TENANT, userId,
                        "Sprint Streak Achieved",
                        "You completed 7 consecutive days of focus sprints — Sprint Streak badge earned!",
                        Notification.NotificationType.SUCCESS);

                Notification n5 = new Notification(DEFAULT_TENANT, userId,
                        "AI Audit Alert",
                        "Anomaly detected: AWS hosting costs increased 10.1% month-over-month ($4,250 → $4,680).",
                        Notification.NotificationType.WARNING);

                Notification n6 = new Notification(DEFAULT_TENANT, userId,
                        "Lead Won",
                        "Nordic Logistics AB — Fleet Management IoT System deal closed ($95,000).",
                        Notification.NotificationType.SUCCESS);

                notificationRepo.saveAll(List.of(n1, n2, n3, n4, n5, n6));
                log.info("✅ Seeded {} Notifications", 6);
        }

        private void logCredentials() {
                log.info("\n\n" +
                                "╔════════════════════════════════════════════════════════════════════╗\n" +
                                "║  ENTERPRISE SERVICE HUB — CREDENTIALS (DEV ONLY)                   ║\n" +
                                "╠════════════════════════════════════════════════════════════════════╣\n" +
                                "║  Admin      : admin@nova.agency          / elite2026!              ║\n" +
                                "║  SuperAdmin : superadmin@nova.platform   / superAdmin2026!         ║\n" +
                                "║  User       : user@nova.agency           / user2026!               ║\n" +
                                "║  Client     : m.torres@transcorp.com     / client2026!             ║\n" +
                                "╚════════════════════════════════════════════════════════════════════╝\n");
        }
}
