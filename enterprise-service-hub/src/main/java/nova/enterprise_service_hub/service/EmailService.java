package nova.enterprise_service_hub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Email Service — Phase 7.3
 * <p>
 * Sends transactional emails: welcome, invoice reminders, client invites.
 * Uses Spring Mail + configurable SMTP (SendGrid/Mailgun/Gmail).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send a plain-text email asynchronously.
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@enterprisehub.com");
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send an HTML email asynchronously.
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("noreply@enterprisehub.com");
            mailSender.send(mimeMessage);
            log.info("HTML email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send a welcome email on user registration.
     */
    @Async
    public void sendWelcomeEmail(String to, String fullName) {
        String subject = "Welcome to Enterprise Service Hub!";
        String body = String.format("""
                Hello %s,

                Welcome to Enterprise Service Hub — your all-in-one platform for managing
                digital agency operations.

                Your account has been created successfully. You can log in at:
                https://app.enterprisehub.com/login

                Best regards,
                The Enterprise Service Hub Team
                """, fullName);
        sendSimpleEmail(to, subject, body);
    }

    /**
     * Send an invoice reminder for overdue invoices.
     */
    @Async
    public void sendInvoiceReminder(String to, String referenceNumber, String amount, String dueDate) {
        String subject = "Invoice Reminder: " + referenceNumber;
        String body = String.format("""
                Dear Customer,

                This is a friendly reminder that invoice %s for %s was due on %s.

                Please arrange payment at your earliest convenience.

                Thank you,
                Enterprise Service Hub
                """, referenceNumber, amount, dueDate);
        sendSimpleEmail(to, subject, body);
    }

    /**
     * Send a client portal invitation.
     */
    @Async
    public void sendClientInvitation(String to, String clientName, String inviteLink) {
        String subject = "You've been invited to Enterprise Service Hub";
        String body = String.format("""
                Hello %s,

                You've been invited to access your project dashboard on Enterprise Service Hub.

                Click the link below to set up your account:
                %s

                This invitation expires in 7 days.

                Best regards,
                Enterprise Service Hub
                """, clientName, inviteLink);
        sendSimpleEmail(to, subject, body);
    }
}
