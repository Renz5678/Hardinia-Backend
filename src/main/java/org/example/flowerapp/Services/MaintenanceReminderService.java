package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MaintenanceReminderService {
    private static final Logger logger = LoggerFactory.getLogger(MaintenanceReminderService.class);
    private static final ZoneId UTC = ZoneId.of("UTC");

    private final MaintenanceRepository maintenanceRepository;
    private final EmailService emailService;

    public MaintenanceReminderService(MaintenanceRepository maintenanceRepository,
                                      EmailService emailService) {
        this.maintenanceRepository = maintenanceRepository;
        this.emailService = emailService;
    }

    /**
     * Scheduled task that runs daily at 6:00 AM UTC (2:00 PM Philippine Time)
     * Checks for tasks due today and sends email reminders
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "UTC")
    public void sendDailyMaintenanceReminders() {
        logger.info("=== Starting daily maintenance reminder job ===");

        try {
            processReminders();
        } catch (Exception e) {
            logger.error("=== Critical error in daily maintenance reminder job ===", e);
        }
    }

    /**
     * Process reminders in a separate transaction
     */
    @Transactional(readOnly = true)
    protected void processReminders() {
        ZonedDateTime nowUtc = ZonedDateTime.now(UTC);
        ZonedDateTime startOfDay = nowUtc.toLocalDate().atStartOfDay(UTC);
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        logger.info("Current UTC time: {}", nowUtc);
        logger.info("Checking for tasks due between {} and {}", startOfDay, endOfDay);

        // Get all incomplete tasks EXCLUDING dead flowers
        List<Maintenance> allIncompleteTasks = maintenanceRepository
                .findByCompletedStatusExcludingDead(false);

        logger.info("Found {} total incomplete tasks (excluding dead flowers)",
                allIncompleteTasks.size());

        // Filter out invalid tasks
        List<Maintenance> validTasks = allIncompleteTasks.stream()
                .filter(task -> {
                    if (task.getUserId() == null) {
                        logger.warn("Task {} has null userId, skipping", task.getTask_id());
                        return false;
                    }
                    if (task.getDueDate() == null) {
                        logger.warn("Task {} has null due date, skipping", task.getTask_id());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (validTasks.size() < allIncompleteTasks.size()) {
            logger.warn("Filtered out {} invalid tasks",
                    allIncompleteTasks.size() - validTasks.size());
        }

        // Group tasks by userId
        Map<String, List<Maintenance>> tasksByUser = validTasks.stream()
                .collect(Collectors.groupingBy(Maintenance::getUserId));

        logger.info("Found {} unique users with incomplete tasks", tasksByUser.size());

        int emailsSent = 0;
        int emailsFailed = 0;
        int usersWithTasksDueToday = 0;

        // Process each user independently to prevent cascading failures
        for (Map.Entry<String, List<Maintenance>> entry : tasksByUser.entrySet()) {
            String userId = entry.getKey();

            try {
                // Filter tasks due today for this user
                List<Maintenance> tasksDueToday = entry.getValue().stream()
                        .filter(task -> isTaskDueToday(task, startOfDay, endOfDay))
                        .collect(Collectors.toList());

                if (!tasksDueToday.isEmpty()) {
                    usersWithTasksDueToday++;
                    logger.info("User {} has {} tasks due today", userId, tasksDueToday.size());

                    // Log task details
                    for (Maintenance task : tasksDueToday) {
                        logger.debug("  - Task {}: {} (Due: {})",
                                task.getTask_id(),
                                task.getTaskType(),
                                task.getDueDate());
                    }

                    // Send email
                    emailService.sendMaintenanceReminder(userId, tasksDueToday);
                    logger.info("✓ Successfully sent reminder to user {} for {} tasks",
                            userId, tasksDueToday.size());
                    emailsSent++;
                }
            } catch (Exception e) {
                // Log error but continue processing other users
                logger.error("✗ Failed to send reminder to user {}: {}",
                        userId, e.getMessage());
                logger.debug("Full error for user {}", userId, e);
                emailsFailed++;
            }
        }

        logger.info("=== Daily maintenance reminder job completed ===");
        logger.info("Users with tasks due today: {}", usersWithTasksDueToday);
        logger.info("Emails sent: {}, Failed: {}", emailsSent, emailsFailed);
    }

    /**
     * Check if a task is due today in UTC
     */
    private boolean isTaskDueToday(Maintenance task,
                                   ZonedDateTime startOfDay,
                                   ZonedDateTime endOfDay) {
        if (task.getDueDate() == null) {
            return false;
        }

        // Convert LocalDateTime to ZonedDateTime assuming UTC
        ZonedDateTime taskDueDate = task.getDueDate().atZone(UTC);

        return !taskDueDate.isBefore(startOfDay) && !taskDueDate.isAfter(endOfDay);
    }

    /**
     * Manual trigger for testing or on-demand reminders for a specific user
     */
    @Transactional(readOnly = true)
    public void sendRemindersForUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            logger.error("Cannot send reminders: userId is null or empty");
            throw new IllegalArgumentException("User ID is required");
        }

        logger.info("=== Starting sendRemindersForUser for userId: {} ===", userId);

        try {
            ZonedDateTime nowUtc = ZonedDateTime.now(UTC);
            ZonedDateTime startOfDay = nowUtc.toLocalDate().atStartOfDay(UTC);
            ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

            logger.info("Current UTC time: {}", nowUtc);
            logger.info("Time range: {} to {}", startOfDay, endOfDay);

            // Get all incomplete tasks for the user EXCLUDING dead flowers
            List<Maintenance> incompleteTasks = maintenanceRepository
                    .findByCompletedStatusAndUserIdExcludingDead(false, userId);

            logger.info("Found {} incomplete tasks for user (excluding dead flowers)",
                    incompleteTasks.size());

            // Filter out tasks with null due dates
            List<Maintenance> validTasks = incompleteTasks.stream()
                    .filter(task -> {
                        if (task.getDueDate() == null) {
                            logger.warn("Task {} has null due date, skipping",
                                    task.getTask_id());
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            // Log each valid task's details
            for (Maintenance task : validTasks) {
                logger.debug("Task ID: {}, Flower: {}, Due Date: {}, Completed: {}",
                        task.getTask_id(),
                        task.getFlower() != null ? task.getFlower().getFlowerName() : "Unknown",
                        task.getDueDate(),
                        task.isCompleted());
            }

            // Filter tasks that are due today
            List<Maintenance> tasksDueToday = validTasks.stream()
                    .filter(task -> {
                        boolean isDueToday = isTaskDueToday(task, startOfDay, endOfDay);
                        logger.debug("Task {} - Due: {}, Is due today: {}",
                                task.getTask_id(), task.getDueDate(), isDueToday);
                        return isDueToday;
                    })
                    .collect(Collectors.toList());

            logger.info("{} tasks due today for user", tasksDueToday.size());

            if (!tasksDueToday.isEmpty()) {
                logger.info("Attempting to send email to userId: {}", userId);
                emailService.sendMaintenanceReminder(userId, tasksDueToday);
                logger.info("✓ Email sent successfully to user: {} for {} tasks",
                        userId, tasksDueToday.size());
            } else {
                logger.info("No tasks due today for user: {}", userId);
            }

        } catch (Exception e) {
            logger.error("✗ Error sending reminder to user: {}", userId, e);
            throw e; // Rethrow for manual triggers
        }
    }

    /**
     * Check if a specific user has tasks due today
     */
    @Transactional(readOnly = true)
    public boolean hasTasksDueToday(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("hasTasksDueToday called with null or empty userId");
            return false;
        }

        ZonedDateTime nowUtc = ZonedDateTime.now(UTC);
        ZonedDateTime startOfDay = nowUtc.toLocalDate().atStartOfDay(UTC);
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<Maintenance> incompleteTasks = maintenanceRepository
                .findByCompletedStatusAndUserIdExcludingDead(false, userId);

        return incompleteTasks.stream()
                .filter(task -> task.getDueDate() != null)
                .anyMatch(task -> isTaskDueToday(task, startOfDay, endOfDay));
    }

    /**
     * Get count of tasks due today for a user
     */
    @Transactional(readOnly = true)
    public int getTasksDueTodayCount(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            logger.warn("getTasksDueTodayCount called with null or empty userId");
            return 0;
        }

        ZonedDateTime nowUtc = ZonedDateTime.now(UTC);
        ZonedDateTime startOfDay = nowUtc.toLocalDate().atStartOfDay(UTC);
        ZonedDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        List<Maintenance> incompleteTasks = maintenanceRepository
                .findByCompletedStatusAndUserIdExcludingDead(false, userId);

        return (int) incompleteTasks.stream()
                .filter(task -> task.getDueDate() != null)
                .filter(task -> isTaskDueToday(task, startOfDay, endOfDay))
                .count();
    }
}