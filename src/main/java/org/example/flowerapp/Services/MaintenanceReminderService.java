package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaintenanceReminderService {
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceReminderService.class);

    private final MaintenanceRepository maintenanceRepository;
    private final EmailService emailService;

    public MaintenanceReminderService(MaintenanceRepository maintenanceRepository,
                                      EmailService emailService) {
        this.maintenanceRepository = maintenanceRepository;
        this.emailService = emailService;
    }

    /**
     * Scheduled task that runs daily at 8:00 AM
     * Checks for tasks due today and sends email reminders
     */
    @Scheduled(cron = "0 0 8 * * *") // Runs at 8:00 AM every day
    public void sendDailyMaintenanceReminders() {
        logger.info("Starting daily maintenance reminder job");

        try {
            // Get all incomplete tasks
            List<Maintenance> allIncompleteTasks = maintenanceRepository.findByCompletedStatus(false);

            // Group tasks by userId
            Map<String, List<Maintenance>> tasksByUser = allIncompleteTasks.stream()
                    .collect(Collectors.groupingBy(Maintenance::getUserId));

            logger.info("Found {} users with incomplete tasks", tasksByUser.size());

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

            int emailsSent = 0;
            int emailsFailed = 0;

            // Process each user
            for (Map.Entry<String, List<Maintenance>> entry : tasksByUser.entrySet()) {
                String userId = entry.getKey();

                // Filter tasks due today for this user
                List<Maintenance> tasksDueToday = entry.getValue().stream()
                        .filter(task -> {
                            LocalDateTime dueDate = task.getDueDate();
                            return dueDate != null &&
                                    !dueDate.isBefore(startOfDay) &&
                                    !dueDate.isAfter(endOfDay);
                        })
                        .collect(Collectors.toList());

                // Send email if user has tasks due today
                if (!tasksDueToday.isEmpty()) {
                    try {
                        emailService.sendMaintenanceReminder(userId, tasksDueToday);
                        logger.info("Sent reminder to user {} for {} tasks", userId, tasksDueToday.size());
                        emailsSent++;
                    } catch (Exception e) {
                        logger.error("Failed to send reminder to user {}", userId, e);
                        emailsFailed++;
                    }
                }
            }

            logger.info("Daily maintenance reminder job completed. Emails sent: {}, Failed: {}",
                    emailsSent, emailsFailed);

        } catch (Exception e) {
            logger.error("Error in daily maintenance reminder job", e);
        }
    }

    /**
     * Manual trigger for testing or on-demand reminders for a specific user
     */
    public void sendRemindersForUser(String userId) {
        logger.info("=== DEBUG: Starting sendRemindersForUser for userId: {} ===", userId);

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

            logger.info("DEBUG: Current time: {}", now);
            logger.info("DEBUG: Start of day: {}", startOfDay);
            logger.info("DEBUG: End of day: {}", endOfDay);

            // Get all incomplete tasks for the user
            List<Maintenance> incompleteTasks = maintenanceRepository
                    .findByCompletedStatusAndUserId(false, userId);

            logger.info("DEBUG: Found {} incomplete tasks for user", incompleteTasks.size());

            // Log each task's details
            for (Maintenance task : incompleteTasks) {
                logger.info("DEBUG: Task ID: {}, Due Date: {}, Completed: {}",
                        task.getTask_id(), task.getDueDate(), task.isCompleted());
            }

            // Filter tasks that are due today
            List<Maintenance> tasksDueToday = incompleteTasks.stream()
                    .filter(task -> {
                        LocalDateTime dueDate = task.getDueDate();
                        boolean isDueToday = dueDate != null &&
                                !dueDate.isBefore(startOfDay) &&
                                !dueDate.isAfter(endOfDay);

                        if (dueDate != null) {
                            logger.info("DEBUG: Task {} - Due: {}, Is due today: {}",
                                    task.getTask_id(), dueDate, isDueToday);
                        }

                        return isDueToday;
                    })
                    .collect(Collectors.toList());

            logger.info("DEBUG: {} tasks due today", tasksDueToday.size());

            if (!tasksDueToday.isEmpty()) {
                logger.info("DEBUG: Attempting to send email to userId: {}", userId);
                emailService.sendMaintenanceReminder(userId, tasksDueToday);
                logger.info("DEBUG: Email sent successfully to user: {} for {} tasks",
                        userId, tasksDueToday.size());
            } else {
                logger.info("DEBUG: No tasks due today for user: {}", userId);
            }

        } catch (Exception e) {
            logger.error("DEBUG ERROR: Error sending reminder to user: {}", userId, e);
            e.printStackTrace();
        }
    }

    /**
     * Check if a specific user has tasks due today
     */
    public boolean hasTasksDueToday(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<Maintenance> incompleteTasks = maintenanceRepository
                .findByCompletedStatusAndUserId(false, userId);

        return incompleteTasks.stream()
                .anyMatch(task -> {
                    LocalDateTime dueDate = task.getDueDate();
                    return dueDate != null &&
                            !dueDate.isBefore(startOfDay) &&
                            !dueDate.isAfter(endOfDay);
                });
    }

    /**
     * Get count of tasks due today for a user
     */
    public int getTasksDueTodayCount(String userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<Maintenance> incompleteTasks = maintenanceRepository
                .findByCompletedStatusAndUserId(false, userId);

        return (int) incompleteTasks.stream()
                .filter(task -> {
                    LocalDateTime dueDate = task.getDueDate();
                    return dueDate != null &&
                            !dueDate.isBefore(startOfDay) &&
                            !dueDate.isAfter(endOfDay);
                })
                .count();
    }
}