package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Maintenance;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;  // Add this import
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbc;  // Add this field

    // Update constructor to inject JdbcTemplate
    public EmailService(JavaMailSender mailSender, JdbcTemplate jdbc) {
        this.mailSender = mailSender;
        this.jdbc = jdbc;  // Initialize jdbc
    }

    /**
     * Send maintenance reminder email to user
     */
    public void sendMaintenanceReminder(String userId, List<Maintenance> tasks) {
        try {
            // Get user email - you'll need to implement this based on your User model
            String userEmail = getUserEmail(userId);

            if (userEmail == null || userEmail.isEmpty()) {
                logger.warn("No email found for user: {}", userId);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject("🌸 Flower Maintenance Reminder - Tasks Due Today");
            helper.setText(buildEmailContent(tasks), true); // true = HTML

            mailSender.send(message);
            logger.info("Successfully sent reminder email to: {}", userEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send email for user: {}", userId, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Build HTML email content
     */
    private String buildEmailContent(List<Maintenance> tasks) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }");
        html.append(".task { border-left: 4px solid #4CAF50; padding: 15px; margin: 10px 0; background-color: #f9f9f9; }");
        html.append(".task-type { font-weight: bold; color: #4CAF50; }");
        html.append(".flower-name { font-size: 18px; color: #2196F3; }");
        html.append(".due-time { color: #ff5722; font-weight: bold; }");
        html.append(".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #777; font-size: 12px; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>🌸 Flower Maintenance Reminder</h1>");
        html.append("</div>");

        html.append("<p>Hello!</p>");
        html.append("<p>You have <strong>").append(tasks.size())
                .append(" maintenance task").append(tasks.size() > 1 ? "s" : "")
                .append("</strong> due today:</p>");

        for (Maintenance task : tasks) {
            html.append("<div class='task'>");
            html.append("<div class='flower-name'>🌺 ")
                    .append(task.getFlower().getFlowerName())
                    .append("</div>");
            html.append("<div class='task-type'>Task: ")
                    .append(formatTaskType(task.getTaskType().name()))
                    .append("</div>");

            if (task.getDueDate() != null) {
                html.append("<div class='due-time'>Due: ")
                        .append(task.getDueDate().format(TIME_FORMATTER))
                        .append("</div>");
            }

            if (task.getNotes() != null && !task.getNotes().isEmpty()) {
                html.append("<div style='margin-top: 10px; font-style: italic;'>")
                        .append("Notes: ").append(task.getNotes())
                        .append("</div>");
            }
            html.append("</div>");
        }

        html.append("<p style='margin-top: 20px;'>Don't forget to mark your tasks as complete once you're done!</p>");

        html.append("<div class='footer'>");
        html.append("<p>This is an automated reminder from your Flower Care App.</p>");
        html.append("<p>To manage your maintenance tasks, please log in to your account.</p>");
        html.append("</div>");

        html.append("</div></body></html>");

        return html.toString();
    }

    /**
     * Format task type enum to readable text
     */
    private String formatTaskType(String taskType) {
        if (taskType == null) return "";

        String[] words = taskType.replace("_", " ").toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Get user email from user ID via Supabase auth.users table
     */
    private String getUserEmail(String userId) {
        String sql = "SELECT email FROM users WHERE user_id = ?";
        try {
            return jdbc.queryForObject(sql, String.class, UUID.fromString(userId));
        } catch (EmptyResultDataAccessException e) {
            logger.error("User not found in users table: {}", userId);
            throw new RuntimeException("User not found: " + userId);
        }
    }
}