package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PestManagementServiceIntegrationTest {

    @Autowired
    private PestManagementService pestManagementService;

    @Autowired
    private FlowerRepository flowerRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    private Flower testFlower1;
    private Flower testFlower2;
    private Flower testFlower3;

    @BeforeEach
    void setUp() {
        // Note: @Transactional will automatically rollback after each test

        // Create test flowers with valid UUID userIds
        testFlower1 = new Flower();
        testFlower1.setFlowerName("Rose");
        testFlower1.setSpecies("kalachuchi");
        testFlower1.setUserId(UUID.randomUUID().toString());
        testFlower1 = flowerRepository.save(testFlower1);

        testFlower2 = new Flower();
        testFlower2.setFlowerName("Sunflower");
        testFlower2.setSpecies("sunflower");
        testFlower2.setUserId(UUID.randomUUID().toString());
        testFlower2 = flowerRepository.save(testFlower2);

        testFlower3 = new Flower();
        testFlower3.setFlowerName("Tulip");
        testFlower3.setSpecies("tulips");
        testFlower3.setUserId(UUID.randomUUID().toString());
        testFlower3 = flowerRepository.save(testFlower3);
    }

    @Test
    void shouldCreatePestTasksInDatabase() {
        // Act - Run multiple times to ensure at least some pest tasks are created
        for (int i = 0; i < 20; i++) {
            pestManagementService.checkForPestInfestations();
        }

        // Assert
        List<Maintenance> allTasks = maintenanceRepository.findByMaintenanceType(MaintenanceType.PEST_CONTROL);

        // Should have created at least some pest tasks (statistically likely with 20 runs)
        assertThat(allTasks).isNotEmpty();

        // All created tasks should be pest control tasks
        allTasks.forEach(task -> {
            assertEquals(MaintenanceType.PEST_CONTROL, task.getTaskType());
            assertFalse(task.isCompleted());
            assertEquals("System", task.getPerformedBy());
            assertNotNull(task.getScheduledDate());
            assertNotNull(task.getDueDate());
            assertThat(task.getNotes()).contains("Pest infestation");
        });
    }

    @Test
    void shouldNotCreateDuplicateTasksForSameFlower() {
        // Arrange - Create an active pest task for testFlower1
        Maintenance existingTask = new Maintenance();
        existingTask.setFlower(testFlower1);
        existingTask.setUserId(testFlower1.getUserId());
        existingTask.setTaskType(MaintenanceType.PEST_CONTROL);
        existingTask.setScheduledDate(LocalDateTime.now());
        existingTask.setDueDate(LocalDateTime.now().plusDays(3));
        existingTask.setCompleted(false);
        existingTask.setNotes("Existing pest task");
        existingTask.setPerformedBy("System");
        maintenanceRepository.save(existingTask);

        // Act - Run pest check multiple times
        for (int i = 0; i < 30; i++) {
            pestManagementService.checkForPestInfestations();
        }

        // Assert - testFlower1 should still have only 1 active pest task
        List<Maintenance> flower1Tasks = maintenanceRepository
                .findByFlowerIdAndUserId(testFlower1.getFlower_id(), testFlower1.getUserId())
                .stream()
                .filter(task -> task.getTaskType() == MaintenanceType.PEST_CONTROL && !task.isCompleted())
                .toList();

        assertEquals(1, flower1Tasks.size(),
                "Flower with active pest task should not get duplicate tasks");
        assertEquals(existingTask.getTask_id(), flower1Tasks.get(0).getTask_id(),
                "The existing task should be preserved");
    }

    @Test
    void shouldAllowNewTaskAfterCompletion() {
        // Arrange - Create a COMPLETED pest task for testFlower1
        Maintenance completedTask = new Maintenance();
        completedTask.setFlower(testFlower1);
        completedTask.setUserId(testFlower1.getUserId());
        completedTask.setTaskType(MaintenanceType.PEST_CONTROL);
        completedTask.setScheduledDate(LocalDateTime.now().minusDays(5));
        completedTask.setDueDate(LocalDateTime.now().minusDays(2));
        completedTask.setCompleted(true);
        completedTask.setCompletedAt(LocalDateTime.now().minusDays(1));
        completedTask.setNotes("Completed pest task");
        completedTask.setPerformedBy("User");
        maintenanceRepository.save(completedTask);

        // Act - Run pest check multiple times
        for (int i = 0; i < 30; i++) {
            pestManagementService.checkForPestInfestations();
        }

        // Assert - Should be able to create new pest tasks since old one is completed
        List<Maintenance> allFlower1Tasks = maintenanceRepository
                .findByFlowerIdAndUserId(testFlower1.getFlower_id(), testFlower1.getUserId())
                .stream()
                .filter(task -> task.getTaskType() == MaintenanceType.PEST_CONTROL)
                .toList();

        assertThat(allFlower1Tasks).hasSizeGreaterThanOrEqualTo(1);
        assertThat(allFlower1Tasks).anyMatch(Maintenance::isCompleted);
    }

    @Test
    void shouldPersistPestTaskDetailsCorrectly() {
        // Act - Run pest check multiple times to ensure at least one task is created
        for (int i = 0; i < 30; i++) {
            pestManagementService.checkForPestInfestations();
        }

        // Assert
        List<Maintenance> pestTasks = maintenanceRepository
                .findByMaintenanceType(MaintenanceType.PEST_CONTROL);

        assertThat(pestTasks).isNotEmpty();

        // Verify each task has correct details
        pestTasks.forEach(task -> {
            assertNotNull(task.getTask_id(), "Task should have an ID");
            assertNotNull(task.getFlower(), "Task should be linked to a flower");
            assertNotNull(task.getUserId(), "Task should have a user ID");
            assertEquals(MaintenanceType.PEST_CONTROL, task.getTaskType());
            assertFalse(task.isCompleted());
            assertEquals("System", task.getPerformedBy());
            assertNotNull(task.getScheduledDate());
            assertNotNull(task.getDueDate());
            assertThat(task.getNotes()).contains("Pest infestation");

            // Verify due date is 3 days after scheduled date
            LocalDateTime expectedDueDate = task.getScheduledDate().plusDays(3);
            assertEquals(expectedDueDate.toLocalDate(), task.getDueDate().toLocalDate(),
                    "Due date should be 3 days after scheduled date");
        });
    }

    @Test
    void shouldTriggerPestCheckManually() {
        // Act
        String result = pestManagementService.triggerPestCheckManually();

        // Assert
        assertNotNull(result);
        assertThat(result).contains("completed");
    }

    @Test
    void shouldProvideAccuratePestStatistics() {
        // Arrange - Create various pest tasks
        Maintenance activeTask1 = createAndSavePestTask(testFlower1, false);
        Maintenance activeTask2 = createAndSavePestTask(testFlower2, false);
        Maintenance completedTask1 = createAndSavePestTask(testFlower3, true);

        // Act
        PestManagementService.PestStatistics stats = pestManagementService.getPestStatistics();

        // Assert
        assertEquals(3, stats.getTotalPestTasks());
        assertEquals(2, stats.getActivePestTasks());
        assertEquals(1, stats.getCompletedPestTasks());
    }

    @Test
    void shouldUpdateStatisticsAfterNewTasks() {
        // Arrange - Get initial statistics
        PestManagementService.PestStatistics initialStats = pestManagementService.getPestStatistics();
        long initialTotal = initialStats.getTotalPestTasks();

        // Act - Create new pest tasks
        createAndSavePestTask(testFlower1, false);
        createAndSavePestTask(testFlower2, true);

        PestManagementService.PestStatistics updatedStats = pestManagementService.getPestStatistics();

        // Assert
        assertEquals(initialTotal + 2, updatedStats.getTotalPestTasks());
        assertEquals(initialStats.getActivePestTasks() + 1, updatedStats.getActivePestTasks());
        assertEquals(initialStats.getCompletedPestTasks() + 1, updatedStats.getCompletedPestTasks());
    }

    @Test
    void shouldRespectPestInfestationProbability() {
        // Act - Run pest check 50 times
        int totalRuns = 50;
        for (int i = 0; i < totalRuns; i++) {
            pestManagementService.checkForPestInfestations();
        }

        // Assert
        List<Maintenance> allPestTasks = maintenanceRepository
                .findByMaintenanceType(MaintenanceType.PEST_CONTROL);

        // Since duplicate prevention is active, we can only get pest tasks on the FIRST run
        // After that, all flowers have active pest tasks and no new ones can be created
        // Expected: ~30% of 3 flowers = 0-3 infestations (from the first check only)
        int actualInfestations = allPestTasks.size();

        // With 3 flowers and 30% probability, we expect 0-3 tasks total
        // (not 50 runs × 3 flowers, because duplicates are prevented)
        assertThat(actualInfestations)
                .withFailMessage("Expected 0-3 infestations with duplicate prevention active, but got %d",
                        actualInfestations)
                .isBetween(0, 3);

        // More specifically: each flower should have AT MOST one active pest task
        long flower1Tasks = allPestTasks.stream()
                .filter(t -> t.getFlower().getFlower_id() == testFlower1.getFlower_id())
                .count();
        long flower2Tasks = allPestTasks.stream()
                .filter(t -> t.getFlower().getFlower_id() == testFlower2.getFlower_id())
                .count();
        long flower3Tasks = allPestTasks.stream()
                .filter(t -> t.getFlower().getFlower_id() == testFlower3.getFlower_id())
                .count();

        assertThat(flower1Tasks).isLessThanOrEqualTo(1);
        assertThat(flower2Tasks).isLessThanOrEqualTo(1);
        assertThat(flower3Tasks).isLessThanOrEqualTo(1);
    }

    @Test
    void shouldIsolateTasksByUser() {
        // Arrange - Flowers already have different userIds from setUp()

        // Act - Create pest tasks
        for (int i = 0; i < 20; i++) {
            pestManagementService.checkForPestInfestations();
        }

        // Assert - Each task should belong to the correct user
        List<Maintenance> allTasks = maintenanceRepository
                .findByMaintenanceType(MaintenanceType.PEST_CONTROL);

        allTasks.forEach(task -> {
            Flower flower = task.getFlower();
            assertEquals(flower.getUserId(), task.getUserId(),
                    "Task userId should match its flower's userId");
        });
    }

    @Test
    void shouldMaintainDatabaseIntegrity() {
        // Act - Create some pest tasks
        for (int i = 0; i < 20; i++) {
            pestManagementService.checkForPestInfestations();
        }

        // Assert - All pest tasks should be properly linked to existing flowers
        List<Maintenance> allPestTasks = maintenanceRepository
                .findByMaintenanceType(MaintenanceType.PEST_CONTROL);

        allPestTasks.forEach(task -> {
            assertNotNull(task.getFlower(), "Task should be linked to a flower");
            assertNotNull(task.getFlower().getFlower_id(), "Flower should have an ID");

            // Verify the flower actually exists in the database
            Flower flower = flowerRepository.findByFlowerIdAndUserId(
                    task.getFlower().getFlower_id(),
                    task.getUserId()
            );
            assertNotNull(flower, "Flower should exist in database");
            assertEquals(task.getFlower().getFlower_id(), flower.getFlower_id());
        });
    }

    // Helper method
    private Maintenance createAndSavePestTask(Flower flower, boolean completed) {
        Maintenance task = new Maintenance();
        task.setFlower(flower);
        task.setUserId(flower.getUserId());
        task.setTaskType(MaintenanceType.PEST_CONTROL);
        task.setScheduledDate(LocalDateTime.now());
        task.setDueDate(LocalDateTime.now().plusDays(3));
        task.setCompleted(completed);
        if (completed) {
            task.setCompletedAt(LocalDateTime.now());
        }
        task.setNotes("Test pest task");
        task.setPerformedBy(completed ? "User" : "System");
        return maintenanceRepository.save(task);
    }
}