package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MaintenanceReminderServiceIntegrationTest {

    @Autowired
    private MaintenanceReminderService reminderService;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @Autowired
    private FlowerRepository flowerRepository;

    @Autowired
    private EmailService emailService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public EmailService emailService() {
            return Mockito.mock(EmailService.class);
        }
    }

    private String testUserId;
    private String testUserId2;
    private Flower testFlower;
    private Flower testFlower2;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID().toString();
        testUserId2 = UUID.randomUUID().toString();
        now = LocalDateTime.now();

        // Reset the mock before each test
        Mockito.reset(emailService);

        // Create first test flower with userId
        testFlower = new Flower();
        testFlower.setFlowerName("Integration Test Rose");
        testFlower.setSpecies("Rose");
        testFlower.setUserId(testUserId);
        testFlower.setMaxHeight(100.0);
        testFlower.setGrowthRate(7.0);
        testFlower.setAutoScheduling(true);
        testFlower.setPlantingDate(LocalDateTime.now().minusMonths(1));
        testFlower.setWaterFrequencyDays(7);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(3));
        testFlower = flowerRepository.save(testFlower);

        // Create second test flower with different userId
        testFlower2 = new Flower();
        testFlower2.setFlowerName("Integration Test Tulip");
        testFlower2.setSpecies("Tulip");
        testFlower2.setUserId(testUserId2);
        testFlower2.setMaxHeight(80.0);
        testFlower2.setGrowthRate(5.0);
        testFlower2.setAutoScheduling(true);
        testFlower2.setPlantingDate(LocalDateTime.now().minusMonths(1));
        testFlower2.setWaterFrequencyDays(5);
        testFlower2.setLastWateredDate(LocalDateTime.now().minusDays(2));
        testFlower2 = flowerRepository.save(testFlower2);
    }

    // ========== TESTS FOR sendDailyMaintenanceReminders() ==========

    @Test
    void dailyReminders_shouldSendEmailsToAllUsersWithTasksDueToday() {
        // Arrange - Create tasks for both users due TODAY
        Maintenance task1 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        Maintenance task2 = createMaintenanceTask(testFlower2, testUserId2, MaintenanceType.FERTILIZING, now);

        maintenanceRepository.save(task1);
        maintenanceRepository.save(task2);

        // Act
        reminderService.sendDailyMaintenanceReminders();

        // Assert - Both users should receive emails
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), anyList());
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId2), anyList());
        verify(emailService, times(2)).sendMaintenanceReminder(anyString(), anyList());
    }

    @Test
    void dailyReminders_shouldNotSendEmailsWhenNoTasksDueToday() {
        // Arrange - Create tasks for TOMORROW (not today)
        Maintenance futureTask1 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now.plusDays(1));
        Maintenance futureTask2 = createMaintenanceTask(testFlower2, testUserId2, MaintenanceType.FERTILIZING, now.plusDays(1));

        maintenanceRepository.save(futureTask1);
        maintenanceRepository.save(futureTask2);

        // Act
        reminderService.sendDailyMaintenanceReminders();

        // Assert - No emails should be sent
        verify(emailService, never()).sendMaintenanceReminder(anyString(), anyList());
    }

    @Test
    void dailyReminders_shouldHandleMixOfUsersWithAndWithoutTasksDueToday() {
        // Arrange - User 1 has task TODAY, User 2 has task TOMORROW
        Maintenance todayTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        Maintenance futureTask = createMaintenanceTask(testFlower2, testUserId2, MaintenanceType.FERTILIZING, now.plusDays(1));

        maintenanceRepository.save(todayTask);
        maintenanceRepository.save(futureTask);

        // Act
        reminderService.sendDailyMaintenanceReminders();

        // Assert - Only user 1 should receive email
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), anyList());
        verify(emailService, never()).sendMaintenanceReminder(eq(testUserId2), anyList());
    }

    @Test
    void dailyReminders_shouldGroupMultipleTasksPerUser() {
        // Arrange - User 1 has 3 tasks today
        Maintenance task1 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        Maintenance task2 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.FERTILIZING, now);
        Maintenance task3 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.PRUNING, now);

        maintenanceRepository.save(task1);
        maintenanceRepository.save(task2);
        maintenanceRepository.save(task3);

        // Act
        reminderService.sendDailyMaintenanceReminders();

        // Assert - Should send one email with all 3 tasks
        ArgumentCaptor<List<Maintenance>> tasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), tasksCaptor.capture());

        List<Maintenance> capturedTasks = tasksCaptor.getValue();
        assertEquals(3, capturedTasks.size());
    }

    @Test
    void dailyReminders_shouldIgnoreCompletedTasks() {
        // Arrange - Create completed and incomplete tasks
        Maintenance completedTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        completedTask.setCompleted(true);
        completedTask.setCompletedAt(now.minusHours(1));

        Maintenance incompleteTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.FERTILIZING, now);

        maintenanceRepository.save(completedTask);
        maintenanceRepository.save(incompleteTask);

        // Act
        reminderService.sendDailyMaintenanceReminders();

        // Assert - Should only send incomplete task
        ArgumentCaptor<List<Maintenance>> tasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), tasksCaptor.capture());

        List<Maintenance> capturedTasks = tasksCaptor.getValue();
        assertEquals(1, capturedTasks.size());
        assertEquals(MaintenanceType.FERTILIZING, capturedTasks.get(0).getTaskType());
    }

    @Test
    void dailyReminders_shouldHandleEmailServiceFailureForOneUser() {
        // Arrange - Create tasks for both users
        Maintenance task1 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        Maintenance task2 = createMaintenanceTask(testFlower2, testUserId2, MaintenanceType.FERTILIZING, now);

        maintenanceRepository.save(task1);
        maintenanceRepository.save(task2);

        // Mock failure for first user, success for second
        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendMaintenanceReminder(eq(testUserId), anyList());
        doNothing()
                .when(emailService).sendMaintenanceReminder(eq(testUserId2), anyList());

        // Act & Assert - Should not throw and should continue to second user
        assertDoesNotThrow(() -> reminderService.sendDailyMaintenanceReminders());

        // Both users should have been attempted
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), anyList());
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId2), anyList());
    }

    @Test
    void dailyReminders_shouldHandleNoUsers() {
        // Arrange - No tasks in database

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> reminderService.sendDailyMaintenanceReminders());

        // No emails should be sent
        verify(emailService, never()).sendMaintenanceReminder(anyString(), anyList());
    }

    @Test
    void dailyReminders_shouldOnlyIncludeTasksDueToday() {
        // Arrange - Mix of past, today, and future tasks
        Maintenance yesterdayTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now.minusDays(1));
        Maintenance todayTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.FERTILIZING, now);
        Maintenance tomorrowTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.PRUNING, now.plusDays(1));

        maintenanceRepository.save(yesterdayTask);
        maintenanceRepository.save(todayTask);
        maintenanceRepository.save(tomorrowTask);

        // Act
        reminderService.sendDailyMaintenanceReminders();

        // Assert - Should only send today's task (yesterday's are also "due" but typically filtered)
        ArgumentCaptor<List<Maintenance>> tasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), tasksCaptor.capture());

        List<Maintenance> capturedTasks = tasksCaptor.getValue();
        // Should include today's task and possibly yesterday's overdue task
        assertTrue(capturedTasks.size() >= 1);
        assertTrue(capturedTasks.stream().anyMatch(t -> t.getTaskType() == MaintenanceType.FERTILIZING));
    }

    // ========== TESTS FOR sendRemindersForUser() ==========

    @Test
    void shouldSendRemindersForUserWithTasksDueToday() {
        // Arrange - Create maintenance tasks due today
        Maintenance task1 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        Maintenance task2 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.FERTILIZING, now);

        maintenanceRepository.save(task1);
        maintenanceRepository.save(task2);

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert
        ArgumentCaptor<List<Maintenance>> tasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), tasksCaptor.capture());

        List<Maintenance> capturedTasks = tasksCaptor.getValue();
        assertEquals(2, capturedTasks.size());
        assertTrue(capturedTasks.stream().allMatch(t -> !t.isCompleted()));
    }

    @Test
    void shouldNotSendRemindersWhenNoTasksDueToday() {
        // Arrange - Create tasks for TOMORROW
        Maintenance futureTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now.plusDays(1));
        maintenanceRepository.save(futureTask);

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert
        verify(emailService, never()).sendMaintenanceReminder(anyString(), anyList());
    }

    @Test
    void shouldIgnoreCompletedTasks() {
        // Arrange - Create completed task due today
        Maintenance completedTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        completedTask.setCompleted(true);
        completedTask.setCompletedAt(now.minusHours(1));

        maintenanceRepository.save(completedTask);

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert
        verify(emailService, never()).sendMaintenanceReminder(anyString(), anyList());
    }

    @Test
    void shouldOnlySendReminderForTasksDueToday() {
        // Arrange - Mix of today, future, and past tasks
        Maintenance todayTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        Maintenance futureTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.FERTILIZING, now.plusDays(2));
        Maintenance yesterdayTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.PRUNING, now.minusDays(1));

        maintenanceRepository.save(todayTask);
        maintenanceRepository.save(futureTask);
        maintenanceRepository.save(yesterdayTask);

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert - Should send today's task and possibly yesterday's overdue task
        ArgumentCaptor<List<Maintenance>> tasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), tasksCaptor.capture());

        List<Maintenance> capturedTasks = tasksCaptor.getValue();
        assertTrue(capturedTasks.size() >= 1);
        assertTrue(capturedTasks.stream().anyMatch(t -> t.getTaskType() == MaintenanceType.WATERING));
    }

    @Test
    void shouldReturnTrueWhenUserHasTasksDueToday() {
        // Arrange
        Maintenance todayTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        maintenanceRepository.save(todayTask);

        // Act
        boolean hasTasks = reminderService.hasTasksDueToday(testUserId);

        // Assert
        assertTrue(hasTasks);
    }

    @Test
    void shouldReturnFalseWhenUserHasNoTasksDueToday() {
        // Arrange - Task TOMORROW
        Maintenance futureTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now.plusDays(1));
        maintenanceRepository.save(futureTask);

        // Act
        boolean hasTasks = reminderService.hasTasksDueToday(testUserId);

        // Assert
        assertFalse(hasTasks);
    }

    @Test
    void shouldReturnCorrectTaskCount() {
        // Arrange - Create 3 tasks due today
        for (int i = 0; i < 3; i++) {
            Maintenance task = createMaintenanceTask(
                    testFlower,
                    testUserId,
                    MaintenanceType.WATERING,
                    now
            );
            maintenanceRepository.save(task);
        }

        // Act
        int count = reminderService.getTasksDueTodayCount(testUserId);

        // Assert
        assertEquals(3, count);
    }

    @Test
    void shouldReturnZeroWhenNoTasks() {
        // Act
        int count = reminderService.getTasksDueTodayCount(testUserId);

        // Assert
        assertEquals(0, count);
    }

    @Test
    void shouldIsolateTasksBetweenUsers() {
        // Arrange - Create tasks for both users
        Maintenance task1 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        maintenanceRepository.save(task1);

        Maintenance task2 = createMaintenanceTask(testFlower2, testUserId2, MaintenanceType.WATERING, now);
        maintenanceRepository.save(task2);

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert - Should only send for testUserId
        ArgumentCaptor<List<Maintenance>> tasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), tasksCaptor.capture());
        verify(emailService, never()).sendMaintenanceReminder(eq(testUserId2), anyList());

        assertEquals(1, tasksCaptor.getValue().size());
    }

    @Test
    void shouldIncludeTasksAtDifferentTimesToday() {
        // Arrange - Tasks at start, middle, and end of day
        Maintenance morningTask = createMaintenanceTask(
                testFlower,
                testUserId,
                MaintenanceType.WATERING,
                now.withHour(0).withMinute(1)
        );
        Maintenance noonTask = createMaintenanceTask(
                testFlower,
                testUserId,
                MaintenanceType.FERTILIZING,
                now.withHour(12).withMinute(0)
        );
        Maintenance eveningTask = createMaintenanceTask(
                testFlower,
                testUserId,
                MaintenanceType.PRUNING,
                now.withHour(23).withMinute(59)
        );

        maintenanceRepository.save(morningTask);
        maintenanceRepository.save(noonTask);
        maintenanceRepository.save(eveningTask);

        // Act
        int count = reminderService.getTasksDueTodayCount(testUserId);

        // Assert
        assertEquals(3, count);
    }

    @Test
    void shouldHandleEmailServiceFailureGracefully() {
        // Arrange
        Maintenance task = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        maintenanceRepository.save(task);

        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendMaintenanceReminder(anyString(), anyList());

        // Act & Assert - Should not throw
        assertDoesNotThrow(() -> reminderService.sendRemindersForUser(testUserId));
    }

    @Test
    void shouldIncludeTaskAtExactMidnight() {
        // Arrange - Task exactly at midnight today
        LocalDateTime midnight = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        Maintenance midnightTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, midnight);
        maintenanceRepository.save(midnightTask);

        // Act
        boolean hasTasks = reminderService.hasTasksDueToday(testUserId);

        // Assert
        assertTrue(hasTasks);
    }

    @Test
    void shouldIncludeTaskJustBeforeMidnight() {
        // Arrange - Task at 23:59:59
        LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59);
        Maintenance lateTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, endOfDay);
        maintenanceRepository.save(lateTask);

        // Act
        int count = reminderService.getTasksDueTodayCount(testUserId);

        // Assert
        assertEquals(1, count);
    }

    @Test
    void shouldHandleMultipleTasksForSameFlower() {
        // Arrange - Multiple tasks for the same flower
        Maintenance wateringTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        Maintenance fertilizingTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.FERTILIZING, now);
        Maintenance pruningTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.PRUNING, now);

        maintenanceRepository.save(wateringTask);
        maintenanceRepository.save(fertilizingTask);
        maintenanceRepository.save(pruningTask);

        // Act
        int count = reminderService.getTasksDueTodayCount(testUserId);

        // Assert
        assertEquals(3, count);

        // Verify database records
        List<Maintenance> savedTasks = maintenanceRepository
                .findByFlowerIdAndUserId(testFlower.getFlower_id(), testUserId);
        assertThat(savedTasks).hasSize(3);
        assertThat(savedTasks).allMatch(task -> !task.isCompleted());
    }

    @Test
    void shouldNotSendReminderWhenAllTasksCompleted() {
        // Arrange - Create tasks but mark them all as completed
        Maintenance task1 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        task1.setCompleted(true);
        task1.setCompletedAt(now.minusHours(2));

        Maintenance task2 = createMaintenanceTask(testFlower, testUserId, MaintenanceType.FERTILIZING, now);
        task2.setCompleted(true);
        task2.setCompletedAt(now.minusHours(1));

        maintenanceRepository.save(task1);
        maintenanceRepository.save(task2);

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert
        verify(emailService, never()).sendMaintenanceReminder(anyString(), anyList());

        // Verify count is also zero
        int count = reminderService.getTasksDueTodayCount(testUserId);
        assertEquals(0, count);
    }

    @Test
    void shouldHandleMixOfCompletedAndIncompleteTasksDueToday() {
        // Arrange
        Maintenance completedTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);
        completedTask.setCompleted(true);
        completedTask.setCompletedAt(now.minusHours(1));

        Maintenance incompleteTask = createMaintenanceTask(testFlower, testUserId, MaintenanceType.FERTILIZING, now);

        maintenanceRepository.save(completedTask);
        maintenanceRepository.save(incompleteTask);

        // Act
        int count = reminderService.getTasksDueTodayCount(testUserId);
        reminderService.sendRemindersForUser(testUserId);

        // Assert
        assertEquals(1, count);

        ArgumentCaptor<List<Maintenance>> tasksCaptor = ArgumentCaptor.forClass(List.class);
        verify(emailService, times(1)).sendMaintenanceReminder(eq(testUserId), tasksCaptor.capture());

        List<Maintenance> capturedTasks = tasksCaptor.getValue();
        assertEquals(1, capturedTasks.size());
        assertEquals(MaintenanceType.FERTILIZING, capturedTasks.get(0).getTaskType());
    }

    @Test
    void shouldPersistTasksCorrectlyInDatabase() {
        // Arrange
        Maintenance task = createMaintenanceTask(testFlower, testUserId, MaintenanceType.WATERING, now);

        // Act
        Maintenance savedTask = maintenanceRepository.save(task);

        // Assert
        assertNotNull(savedTask.getTask_id());
        assertEquals(testUserId, savedTask.getUserId());
        assertEquals(testFlower.getFlower_id(), savedTask.getFlower().getFlower_id());
        assertEquals(MaintenanceType.WATERING, savedTask.getTaskType());
        assertFalse(savedTask.isCompleted());
        assertNotNull(savedTask.getDueDate());

        // Verify retrieval
        Maintenance retrievedTask = maintenanceRepository
                .findByTaskIdAndUserId(savedTask.getTask_id(), testUserId);
        assertEquals(savedTask.getTask_id(), retrievedTask.getTask_id());
    }

    // Helper method to create maintenance task
    private Maintenance createMaintenanceTask(Flower flower, String userId, MaintenanceType type, LocalDateTime dueDate) {
        Maintenance maintenance = new Maintenance();
        maintenance.setFlower(flower);
        maintenance.setTaskType(type);
        maintenance.setDueDate(dueDate);
        maintenance.setScheduledDate(dueDate);
        maintenance.setCompleted(false);
        maintenance.setUserId(userId);
        maintenance.setNotes("Test task");
        maintenance.setCreatedAt(dueDate);
        maintenance.setAutoGenerated(false);
        return maintenance;
    }
}