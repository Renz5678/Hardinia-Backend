package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbc;

    public EmailService(JavaMailSender mailSender, JdbcTemplate jdbc) {
        this.mailSender = mailSender;
        this.jdbc = jdbc;
    }

    /**
     * Send maintenance reminder email to user
     */
    public void sendMaintenanceReminder(String userId, List<Maintenance> tasks) {
        try {
            String userEmail = getUserEmail(userId);

            if (userEmail == null || userEmail.isEmpty()) {
                logger.warn("No email found for user: {}", userId);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject("🌸 Flower Maintenance Reminder - Tasks Due Today");
            helper.setText(buildEmailContent(tasks), true);

            mailSender.send(message);
            logger.info("Successfully sent reminder email to: {}", userEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send email for user: {}", userId, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send pest infestation alert email to user
     */
    public void sendPestInfestationAlert(String userId, Flower flower) {
        try {
            String userEmail = getUserEmail(userId);

            if (userEmail == null || userEmail.isEmpty()) {
                logger.warn("No email found for user: {}", userId);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject("🐛 Pest Alert - " + flower.getFlowerName() + " Needs Attention!");
            helper.setText(buildPestAlertContent(flower), true);

            mailSender.send(message);
            logger.info("Successfully sent pest alert email to: {} for flower: {}",
                    userEmail, flower.getFlowerName());

        } catch (MessagingException e) {
            logger.error("Failed to send pest alert email for user: {}", userId, e);
            // Don't throw exception - we don't want email failure to break the pest detection
            logger.warn("Continuing despite email failure");
        }
    }

    /**
     * Build HTML content for pest infestation alert
     */
    private String buildPestAlertContent(Flower flower) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background-color: #ff5722; color: white; padding: 20px; text-align: center; }");
        html.append(".alert-box { border: 3px solid #ff5722; padding: 20px; margin: 20px 0; background-color: #fff3e0; border-radius: 8px; }");
        html.append(".flower-name { font-size: 24px; color: #ff5722; font-weight: bold; margin-bottom: 15px; }");
        html.append(".warning-icon { font-size: 48px; text-align: center; margin: 10px 0; }");
        html.append(".action-required { background-color: #ff5722; color: white; padding: 15px; margin: 20px 0; border-radius: 5px; }");
        html.append(".treatment-steps { background-color: #e8f5e9; padding: 15px; margin: 15px 0; border-left: 4px solid #4CAF50; }");
        html.append(".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #777; font-size: 12px; }");
        html.append("</style></head><body>");

        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h1>🐛 Pest Infestation Alert</h1>");
        html.append("</div>");

        html.append("<div class='warning-icon'>⚠️</div>");

        html.append("<div class='alert-box'>");
        html.append("<div class='flower-name'>🌺 ").append(flower.getFlowerName()).append("</div>");
        html.append("<p style='font-size: 16px;'><strong>A pest infestation has been detected!</strong></p>");
        html.append("<p>Your flower requires immediate attention to prevent damage and ensure its health.</p>");
        html.append("</div>");

        html.append("<div class='action-required'>");
        html.append("<h3 style='margin-top: 0;'>⏰ Action Required</h3>");
        html.append("<p style='margin: 5px 0;'><strong>Treatment Deadline:</strong> Within 3 days</p>");
        html.append("<p style='margin: 5px 0;'><strong>Task Type:</strong> Pest Control</p>");
        html.append("</div>");

        html.append("<div class='treatment-steps'>");
        html.append("<h3>🌿 Recommended Treatment Steps:</h3>");
        html.append("<ol>");
        html.append("<li>Inspect the plant carefully for visible pests</li>");
        html.append("<li>Apply appropriate pesticide treatment</li>");
        html.append("<li>Isolate the plant if possible to prevent spread</li>");
        html.append("<li>Monitor the plant daily for improvement</li>");
        html.append("<li>Mark the task as complete once treated</li>");
        html.append("</ol>");
        html.append("</div>");

        html.append("<p style='margin-top: 20px;'><strong>💡 Tip:</strong> Early treatment is key to protecting your flower's health!</p>");

        html.append("<div class='footer'>");
        html.append("<p>This is an automated alert from your Flower Care App.</p>");
        html.append("<p>To view and manage this task, please log in to your account.</p>");
        html.append("</div>");

        html.append("</div></body></html>");

        return html.toString();
    }

    /**
     * Build HTML email content for maintenance reminders
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