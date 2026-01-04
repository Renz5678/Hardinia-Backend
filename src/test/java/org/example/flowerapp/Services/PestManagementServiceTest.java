package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PestManagementServiceTest {

    @Mock
    private FlowerRepository flowerRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @Mock
    private Random mockRandom;

    private PestManagementService pestManagementService;

    private Flower testFlower1;
    private Flower testFlower2;
    private Flower testFlower3;

    @BeforeEach
    void setUp() {
        // Inject mocked Random into service
        pestManagementService = new PestManagementService(
                flowerRepository,
                maintenanceRepository,
                mockRandom
        );

        // Create test flowers
        testFlower1 = new Flower();
        testFlower1.setFlower_id(1L);
        testFlower1.setFlowerName("Rose");
        testFlower1.setUserId("user-123");

        testFlower2 = new Flower();
        testFlower2.setFlower_id(2L);
        testFlower2.setFlowerName("Sunflower");
        testFlower2.setUserId("user-456");

        testFlower3 = new Flower();
        testFlower3.setFlower_id(3L);
        testFlower3.setFlowerName("Tulip");
        testFlower3.setUserId("user-789");
    }

    @Test
    void testCheckForPestInfestations_NoInfestationsWhenUnlucky() {
        // Arrange
        List<Flower> flowers = Arrays.asList(testFlower1, testFlower2);
        when(flowerRepository.findAllFlower()).thenReturn(flowers);
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class),
                eq(MaintenanceType.PEST_CONTROL)
        )).thenReturn(false);

        // Mock random to always return values above threshold (no infestations)
        when(mockRandom.nextDouble()).thenReturn(0.80, 0.90);

        // Act
        pestManagementService.checkForPestInfestations();

        // Assert
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
        verify(flowerRepository, times(1)).findAllFlower();
    }

    @Test
    void testCheckForPestInfestations_CreatesTasksForSomeFlowers() {
        // Arrange
        List<Flower> flowers = Arrays.asList(testFlower1, testFlower2, testFlower3);
        when(flowerRepository.findAllFlower()).thenReturn(flowers);

        // Mock that none of the flowers have existing pest tasks
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class),
                eq(MaintenanceType.PEST_CONTROL)
        )).thenReturn(false);

        // Mock random to return values that trigger pest infestation
        // First flower: 0.25 < 0.30 = YES
        // Second flower: 0.35 > 0.30 = NO
        // Third flower: 0.15 < 0.30 = YES
        when(mockRandom.nextDouble())
                .thenReturn(0.25, 0.35, 0.15);

        // Act
        pestManagementService.checkForPestInfestations();

        // Assert
        // Should save exactly 2 pest tasks (flower 1 and 3)
        verify(maintenanceRepository, times(2)).save(any(Maintenance.class));
        verify(flowerRepository, times(1)).findAllFlower();
    }

    @Test
    void testCheckForPestInfestations_SkipsFlowersWithActivePestTasks() {
        // Arrange
        List<Flower> flowers = Arrays.asList(testFlower1, testFlower2);
        when(flowerRepository.findAllFlower()).thenReturn(flowers);

        // Mock that testFlower1 already has an active pest task
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                eq(testFlower1),
                eq(MaintenanceType.PEST_CONTROL)
        )).thenReturn(true);

        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                eq(testFlower2),
                eq(MaintenanceType.PEST_CONTROL)
        )).thenReturn(false);

        // Mock random to trigger infestation for testFlower2 only
        when(mockRandom.nextDouble()).thenReturn(0.20);

        // Act
        pestManagementService.checkForPestInfestations();

        // Assert
        // Should save only 1 pest task (for flower 2, not flower 1)
        ArgumentCaptor<Maintenance> captor = ArgumentCaptor.forClass(Maintenance.class);
        verify(maintenanceRepository, times(1)).save(captor.capture());

        // Verify the saved task is for testFlower2, not testFlower1
        Maintenance savedTask = captor.getValue();
        assertEquals(testFlower2, savedTask.getFlower());
    }

    @Test
    void testCheckForPestInfestations_CreatesCorrectTaskDetails() {
        // Arrange
        List<Flower> flowers = Arrays.asList(testFlower1);
        when(flowerRepository.findAllFlower()).thenReturn(flowers);
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class),
                eq(MaintenanceType.PEST_CONTROL)
        )).thenReturn(false);

        // Run multiple times to ensure at least one pest task is created
        for (int i = 0; i < 50; i++) {
            pestManagementService.checkForPestInfestations();
        }

        // Assert
        ArgumentCaptor<Maintenance> captor = ArgumentCaptor.forClass(Maintenance.class);
        verify(maintenanceRepository, atLeastOnce()).save(captor.capture());

        // Get one of the created tasks
        Maintenance pestTask = captor.getAllValues().stream()
                .filter(task -> task.getFlower().equals(testFlower1))
                .findFirst()
                .orElse(null);

        if (pestTask != null) {
            assertEquals(testFlower1, pestTask.getFlower());
            assertEquals(testFlower1.getUserId(), pestTask.getUserId());
            assertEquals(MaintenanceType.PEST_CONTROL, pestTask.getTaskType());
            assertFalse(pestTask.isCompleted());
            assertEquals("System", pestTask.getPerformedBy());
            assertNotNull(pestTask.getScheduledDate());
            assertNotNull(pestTask.getDueDate());
            assertTrue(pestTask.getNotes().contains("Pest infestation"));

            // Verify due date is 3 days from maintenance date
            LocalDateTime expectedDueDate = pestTask.getScheduledDate().plusDays(3);
            assertEquals(expectedDueDate.toLocalDate(), pestTask.getDueDate().toLocalDate());
        }
    }

    @Test
    void testCheckForPestInfestations_HandlesEmptyFlowerList() {
        // Arrange
        when(flowerRepository.findAllFlower()).thenReturn(Arrays.asList());

        // Act
        pestManagementService.checkForPestInfestations();

        // Assert
        verify(flowerRepository, times(1)).findAllFlower();
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }

    @Test
    void testCheckForPestInfestations_HandlesRepositoryException() {
        // Arrange
        when(flowerRepository.findAllFlower()).thenReturn(Arrays.asList(testFlower1));
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class),
                eq(MaintenanceType.PEST_CONTROL)
        )).thenThrow(new RuntimeException("Database error"));

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> pestManagementService.checkForPestInfestations());
    }

    @Test
    void testTriggerPestCheckManually_ReturnsSuccessMessage() {
        // Arrange
        when(flowerRepository.findAllFlower()).thenReturn(Arrays.asList(testFlower1));
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class),
                eq(MaintenanceType.PEST_CONTROL)
        )).thenReturn(false);

        // Act
        String result = pestManagementService.triggerPestCheckManually();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("completed"));
        verify(flowerRepository, times(1)).findAllFlower();
    }

    @Test
    void testGetPestStatistics_ReturnsCorrectCounts() {
        // Arrange
        Maintenance activePestTask1 = createPestTask(testFlower1, false);
        Maintenance activePestTask2 = createPestTask(testFlower2, false);
        Maintenance completedPestTask = createPestTask(testFlower3, true);

        List<Maintenance> allPestTasks = Arrays.asList(
                activePestTask1,
                activePestTask2,
                completedPestTask
        );

        when(maintenanceRepository.findByMaintenanceType(MaintenanceType.PEST_CONTROL))
                .thenReturn(allPestTasks);

        // Act
        PestManagementService.PestStatistics stats = pestManagementService.getPestStatistics();

        // Assert
        assertEquals(3, stats.getTotalPestTasks());
        assertEquals(2, stats.getActivePestTasks());
        assertEquals(1, stats.getCompletedPestTasks());
    }

    @Test
    void testGetPestStatistics_HandlesEmptyList() {
        // Arrange
        when(maintenanceRepository.findByMaintenanceType(MaintenanceType.PEST_CONTROL))
                .thenReturn(Arrays.asList());

        // Act
        PestManagementService.PestStatistics stats = pestManagementService.getPestStatistics();

        // Assert
        assertEquals(0, stats.getTotalPestTasks());
        assertEquals(0, stats.getActivePestTasks());
        assertEquals(0, stats.getCompletedPestTasks());
    }

    @Test
    void testPestProbability_ApproximatelyThirtyPercent() {
        // Arrange
        List<Flower> flowers = Arrays.asList(testFlower1, testFlower2, testFlower3);
        when(flowerRepository.findAllFlower()).thenReturn(flowers);
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class),
                eq(MaintenanceType.PEST_CONTROL)
        )).thenReturn(false);

        // DON'T use mockRandom for this test - use a real Random instance
        PestManagementService serviceWithRealRandom = new PestManagementService(
                flowerRepository,
                maintenanceRepository,
                new Random()  // Use real Random for probability testing
        );

        // Act - Run 100 times to test probability distribution
        int totalRuns = 100;

        for (int i = 0; i < totalRuns; i++) {
            serviceWithRealRandom.checkForPestInfestations();
        }

        // Count how many times save was called
        ArgumentCaptor<Maintenance> captor = ArgumentCaptor.forClass(Maintenance.class);
        verify(maintenanceRepository, atLeast(0)).save(captor.capture());
        int totalInfestations = captor.getAllValues().size();

        // Assert - Should be roughly 30% (with some variance due to randomness)
        // With 100 runs and 3 flowers = 300 total checks
        // Expected: ~90 infestations (30% of 300)
        // Allow reasonable variance: 60-120 infestations (20-40%)
        int expectedMin = 60;
        int expectedMax = 120;

        assertTrue(totalInfestations >= expectedMin && totalInfestations <= expectedMax,
                String.format("Expected between %d and %d infestations, but got %d",
                        expectedMin, expectedMax, totalInfestations));
    }

    // Helper method to create pest tasks for testing
    private Maintenance createPestTask(Flower flower, boolean completed) {
        Maintenance maintenance = new Maintenance();
        maintenance.setFlower(flower);
        maintenance.setUserId(flower.getUserId());
        maintenance.setTaskType(MaintenanceType.PEST_CONTROL);
        maintenance.setScheduledDate(LocalDateTime.now());
        maintenance.setDueDate(LocalDateTime.now().plusDays(3));
        maintenance.setCompleted(completed);
        maintenance.setNotes("Pest infestation detected");
        maintenance.setPerformedBy("System");
        return maintenance;
    }
}