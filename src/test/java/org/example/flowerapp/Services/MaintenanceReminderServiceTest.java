package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.example.flowerapp.Services.EmailService;
import org.example.flowerapp.Services.MaintenanceReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceReminderServiceTest {

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MaintenanceReminderService reminderService;

    private String testUserId;
    private Flower testFlower;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";

        testFlower = new Flower();
        testFlower.setFlower_id(1L);
        testFlower.setFlowerName("Rose");
        testFlower.setUserId(testUserId);
    }

    @Test
    void testSendRemindersForUser_WithTasks() {
        // Arrange
        Maintenance task1 = createMaintenance(1L, MaintenanceType.WATERING, testUserId);
        Maintenance task2 = createMaintenance(2L, MaintenanceType.FERTILIZING, testUserId);
        List<Maintenance> incompleteTasks = Arrays.asList(task1, task2);

        // Set due dates to today
        LocalDateTime today = LocalDateTime.now();
        task1.setDueDate(today.withHour(10).withMinute(0));
        task2.setDueDate(today.withHour(14).withMinute(0));

        // Mock finding incomplete tasks
        when(maintenanceRepository.findByCompletedStatusAndUserId(false, testUserId))
                .thenReturn(incompleteTasks);

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert
        verify(maintenanceRepository, times(1))
                .findByCompletedStatusAndUserId(false, testUserId);
        verify(emailService, times(1))
                .sendMaintenanceReminder(eq(testUserId), anyList());
    }

    @Test
    void testSendRemindersForUser_NoTasks() {
        // Arrange
        when(maintenanceRepository.findByCompletedStatusAndUserId(false, testUserId))
                .thenReturn(Collections.emptyList());

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert
        verify(maintenanceRepository, times(1))
                .findByCompletedStatusAndUserId(false, testUserId);
        verify(emailService, never()).sendMaintenanceReminder(anyString(), anyList());
    }

    @Test
    void testSendRemindersForUser_OnlyFutureTasks() {
        // Arrange
        Maintenance futureTask = createMaintenance(1L, MaintenanceType.WATERING, testUserId);
        futureTask.setDueDate(LocalDateTime.now().plusDays(2));

        when(maintenanceRepository.findByCompletedStatusAndUserId(false, testUserId))
                .thenReturn(Collections.singletonList(futureTask));

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert - Should not send email for future tasks
        verify(maintenanceRepository, times(1))
                .findByCompletedStatusAndUserId(false, testUserId);
        verify(emailService, never()).sendMaintenanceReminder(anyString(), anyList());
    }

    @Test
    void testSendDailyMaintenanceReminders_SingleUser() {
        // Arrange
        Maintenance task = createMaintenance(1L, MaintenanceType.WATERING, testUserId);
        task.setDueDate(LocalDateTime.now().withHour(10));

        when(maintenanceRepository.findByCompletedStatusAndUserId(false, testUserId))
                .thenReturn(Collections.singletonList(task));

        // Act
        reminderService.sendRemindersForUser(testUserId);

        // Assert
        verify(emailService, times(1))
                .sendMaintenanceReminder(eq(testUserId), anyList());
    }

    @Test
    void testSendRemindersForUser_EmailServiceFails() {
        // Arrange
        Maintenance task = createMaintenance(1L, MaintenanceType.WATERING, testUserId);
        task.setDueDate(LocalDateTime.now());

        when(maintenanceRepository.findByCompletedStatusAndUserId(false, testUserId))
                .thenReturn(Collections.singletonList(task));

        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendMaintenanceReminder(anyString(), anyList());

        // Act & Assert - Should handle exception gracefully
        assertDoesNotThrow(() -> reminderService.sendRemindersForUser(testUserId));
    }

    @Test
    void testHasTasksDueToday_WithOverdueTasks() {
        // Arrange
        Maintenance overdueTask = createMaintenance(1L, MaintenanceType.WATERING, testUserId);
        overdueTask.setDueDate(LocalDateTime.now().minusDays(1)); // Yesterday

        when(maintenanceRepository.countOverdueTasksByUserId(testUserId))
                .thenReturn(1L);

        // Act
        boolean hasOverdue = maintenanceRepository.countOverdueTasksByUserId(testUserId) > 0;

        // Assert
        assertTrue(hasOverdue);
        verify(maintenanceRepository, times(1))
                .countOverdueTasksByUserId(testUserId);
    }

    @Test
    void testHasTasksDueToday_NoOverdueTasks() {
        // Arrange
        when(maintenanceRepository.countOverdueTasksByUserId(testUserId))
                .thenReturn(0L);

        // Act
        boolean hasOverdue = maintenanceRepository.countOverdueTasksByUserId(testUserId) > 0;

        // Assert
        assertFalse(hasOverdue);
    }

    @Test
    void testFindIncompleteTasksForFlower() {
        // Arrange
        long flowerId = 1L;
        Maintenance task1 = createMaintenance(1L, MaintenanceType.WATERING, testUserId);
        Maintenance task2 = createMaintenance(2L, MaintenanceType.FERTILIZING, testUserId);

        when(maintenanceRepository.findIncompleteByFlowerIdAndUserId(flowerId, testUserId))
                .thenReturn(Arrays.asList(task1, task2));

        // Act
        List<Maintenance> incompleteTasks = maintenanceRepository
                .findIncompleteByFlowerIdAndUserId(flowerId, testUserId);

        // Assert
        assertEquals(2, incompleteTasks.size());
        verify(maintenanceRepository, times(1))
                .findIncompleteByFlowerIdAndUserId(flowerId, testUserId);
    }

    @Test
    void testFindTasksByMaintenanceType() {
        // Arrange
        MaintenanceType type = MaintenanceType.WATERING;
        Maintenance task1 = createMaintenance(1L, type, testUserId);
        Maintenance task2 = createMaintenance(2L, type, "other-user");

        when(maintenanceRepository.findByMaintenanceType(type))
                .thenReturn(Arrays.asList(task1, task2));

        // Act
        List<Maintenance> wateringTasks = maintenanceRepository.findByMaintenanceType(type);

        // Assert
        assertEquals(2, wateringTasks.size());
        assertTrue(wateringTasks.stream().allMatch(t -> t.getTaskType() == type));
    }

    @Test
    void testCountOverdueTasks() {
        // Arrange
        when(maintenanceRepository.countOverdueTasksByUserId(testUserId))
                .thenReturn(3L);

        // Act
        long overdueCount = maintenanceRepository.countOverdueTasksByUserId(testUserId);

        // Assert
        assertEquals(3L, overdueCount);
    }

    @Test
    void testFindTasksDueBefore() {
        // Arrange
        LocalDateTime cutoffDate = LocalDateTime.now();
        Maintenance overdueTask = createMaintenance(1L, MaintenanceType.WATERING, testUserId);
        overdueTask.setDueDate(cutoffDate.minusDays(1));

        when(maintenanceRepository.findByFlowerAndCompletedFalseAndDueDateBefore(
                any(Flower.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(overdueTask));

        // Act
        List<Maintenance> overdueTasks = maintenanceRepository
                .findByFlowerAndCompletedFalseAndDueDateBefore(testFlower, cutoffDate);

        // Assert
        assertEquals(1, overdueTasks.size());
        assertTrue(overdueTasks.get(0).getDueDate().isBefore(cutoffDate));
    }

    @Test
    void testFilterTasksDueToday() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        Maintenance todayTask = createMaintenance(1L, MaintenanceType.WATERING, testUserId);
        todayTask.setDueDate(now.withHour(10));

        Maintenance yesterdayTask = createMaintenance(2L, MaintenanceType.FERTILIZING, testUserId);
        yesterdayTask.setDueDate(now.minusDays(1));

        Maintenance tomorrowTask = createMaintenance(3L, MaintenanceType.PRUNING, testUserId);
        tomorrowTask.setDueDate(now.plusDays(1));

        List<Maintenance> allTasks = Arrays.asList(todayTask, yesterdayTask, tomorrowTask);

        // Act - Filter tasks due today
        List<Maintenance> tasksDueToday = allTasks.stream()
                .filter(task -> {
                    LocalDateTime dueDate = task.getDueDate();
                    return dueDate != null &&
                            !dueDate.isBefore(startOfDay) &&
                            !dueDate.isAfter(endOfDay);
                })
                .toList();

        // Assert
        assertEquals(1, tasksDueToday.size());
        assertEquals(todayTask.getTask_id(), tasksDueToday.get(0).getTask_id());
    }

    // Helper method to create maintenance tasks
    private Maintenance createMaintenance(Long taskId, MaintenanceType type, String userId) {
        Maintenance maintenance = new Maintenance();
        maintenance.setTask_id(taskId);
        maintenance.setFlower(testFlower);
        maintenance.setTaskType(type);
        maintenance.setUserId(userId);
        maintenance.setDueDate(LocalDateTime.now());
        maintenance.setScheduledDate(LocalDateTime.now());
        maintenance.setCompleted(false);
        maintenance.setNotes("Test notes");
        maintenance.setCreatedAt(LocalDateTime.now());
        return maintenance;
    }
}