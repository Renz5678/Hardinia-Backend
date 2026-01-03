package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Growth;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.GrowthRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.within;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GrowthAutomationServiceIntegrationTest {

    @Autowired
    private GrowthAutomationService growthAutomationService;

    @Autowired
    private FlowerRepository flowerRepository;

    @Autowired
    private GrowthRepository growthRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    private Flower testFlower;

    @BeforeEach
    void setUp() {
        // Note: @Transactional will automatically rollback after each test
        // No need to manually delete if using @Transactional

        // Create a test flower
        testFlower = new Flower();
        testFlower.setFlowerName("Integration Test Rose");
        testFlower.setMaxHeight(100.0);
        testFlower.setGrowthRate(7.0); // 7% per week
        testFlower.setAutoScheduling(true);
        testFlower.setPlantingDate(LocalDateTime.now().minusMonths(1));
        testFlower.setWaterFrequencyDays(7);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(3));
        testFlower = flowerRepository.save(testFlower);

        // Create initial growth record
        Growth initialGrowth = new Growth();
        initialGrowth.setFlower(testFlower);
        initialGrowth.setHeight(50.0);
        initialGrowth.setStage(GrowthStage.SEEDLING);
        initialGrowth.setRecordedAt(LocalDateTime.now().minusDays(7));
        initialGrowth.setGrowthSinceLast(0.0);
        initialGrowth.setColorChanges(false);
        initialGrowth.setNotes("Initial growth for integration test");
        growthRepository.save(initialGrowth);
    }

    @Test
    void shouldUpdateFlowerGrowthSuccessfully() {
        // Act
        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(testFlower);

        // Assert
        assertTrue(result.isUpdated());
        assertThat(result.getNewHeight()).isGreaterThan(50.0);
        assertThat(result.getNewHeight()).isLessThanOrEqualTo(100.0);
        assertEquals(testFlower.getFlower_id(), result.getFlowerId());

        // Verify database was updated
        Optional<Growth> updatedGrowth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower);
        assertTrue(updatedGrowth.isPresent());
        assertThat(updatedGrowth.get().getHeight()).isGreaterThan(50.0);
        assertNotNull(updatedGrowth.get().getNotes());
    }

    @Test
    void shouldSkipUpdateWhenMaintenancePartiallyOverdue() {
        // Arrange - Make watering overdue but fertilizing on time
        testFlower.setWaterFrequencyDays(7);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(10)); // Overdue
        testFlower.setFertilizeFrequencyDays(14);
        testFlower.setLastFertilizedDate(LocalDateTime.now().minusDays(5)); // Not overdue
        flowerRepository.save(testFlower);

        // Act
        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(testFlower);

        // Assert
        assertFalse(result.isUpdated());
        assertEquals("Skipped - maintenance overdue", result.getMessage());
        assertEquals(50.0, result.getNewHeight()); // Height unchanged

        // Verify database was NOT updated (height remains the same)
        Optional<Growth> growth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower);
        assertTrue(growth.isPresent());
        assertEquals(50.0, growth.get().getHeight());
    }

    @Test
    void shouldSetToWiltingWhenAllMaintenanceOverdue() {
        // Arrange - Make ALL maintenance tasks overdue
        testFlower.setWaterFrequencyDays(7);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(10));
        testFlower.setFertilizeFrequencyDays(14);
        testFlower.setLastFertilizedDate(LocalDateTime.now().minusDays(20));
        testFlower.setPruneFrequencyDays(30);
        testFlower.setLastPrunedDate(LocalDateTime.now().minusDays(40));
        flowerRepository.save(testFlower);

        // Act
        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(testFlower);

        // Assert
        assertTrue(result.isUpdated());
        assertEquals(GrowthStage.WILTING, result.getStage());
        assertTrue(result.getMessage().contains("WILTING"));

        // Verify database was updated with WILTING stage
        Optional<Growth> growth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower);
        assertTrue(growth.isPresent());
        assertEquals(GrowthStage.WILTING, growth.get().getStage());
        assertTrue(growth.get().isColorChanges());
        assertThat(growth.get().getNotes()).contains("wilting");
    }

    @Test
    void shouldSkipUpdateWhenTooSoon() {
        // Arrange - Update the growth record to be recent (only 3 days ago)
        Growth recentGrowth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower)
                .orElseThrow();
        recentGrowth.setRecordedAt(LocalDateTime.now().minusDays(3));
        growthRepository.save(recentGrowth);

        // Act
        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(testFlower);

        // Assert
        assertFalse(result.isUpdated());
        assertTrue(result.getMessage().contains("too soon"));
        assertEquals(50.0, result.getNewHeight());
    }

    @Test
    void shouldSkipUpdateWhenAtMaxHeight() {
        // Arrange - Set flower to max height
        Growth maxGrowth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower)
                .orElseThrow();
        maxGrowth.setHeight(100.0); // Max height
        growthRepository.save(maxGrowth);

        // Act
        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(testFlower);

        // Assert
        assertFalse(result.isUpdated());
        assertTrue(result.getMessage().contains("maximum height"));
        assertEquals(100.0, result.getNewHeight());
    }

    @Test
    void shouldCreateInitialGrowthRecordWhenMissing() {
        // Arrange - Create a new flower without growth record
        Flower newFlower = new Flower();
        newFlower.setFlowerName("New Test Flower");
        newFlower.setMaxHeight(80.0);
        newFlower.setGrowthRate(5.0);
        newFlower.setAutoScheduling(true);
        newFlower.setPlantingDate(LocalDateTime.now());
        newFlower = flowerRepository.save(newFlower);

        // Act
        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(newFlower);

        // Assert
        assertFalse(result.isUpdated()); // Initial creation doesn't count as update
        assertTrue(result.getMessage().contains("Initial growth record created"));

        // Verify initial growth record was created in database
        Optional<Growth> growth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(newFlower);
        assertTrue(growth.isPresent());
        assertEquals(0.0, growth.get().getHeight());
        assertEquals(GrowthStage.SEED, growth.get().getStage());
    }

    @Test
    void shouldProgressThroughGrowthStages() {
        // Arrange - Start at low height
        Growth growth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower)
                .orElseThrow();
        growth.setHeight(10.0); // 10% of max height
        growth.setStage(GrowthStage.SEED);
        growthRepository.save(growth);

        // Act & Assert - Progress through stages
        testFlower.setGrowthRate(15.0); // Faster growth for testing
        flowerRepository.save(testFlower);

        // First update: SEED -> SEEDLING (10% -> 25%)
        GrowthAutomationService.GrowthUpdateResult result1 =
                growthAutomationService.updateFlowerGrowth(testFlower);
        assertTrue(result1.isUpdated());
        assertEquals(GrowthStage.SEEDLING, result1.getStage());

        // Second update: SEEDLING -> BUDDING
        growth = growthRepository.findTopByFlowerOrderByRecordedAtDesc(testFlower).orElseThrow();
        growth.setRecordedAt(LocalDateTime.now().minusDays(7));
        growth.setHeight(35.0); // 35% of max height
        growthRepository.save(growth);

        GrowthAutomationService.GrowthUpdateResult result2 =
                growthAutomationService.updateFlowerGrowth(testFlower);
        assertTrue(result2.isUpdated());
        assertEquals(GrowthStage.BUDDING, result2.getStage());

        // Third update: BUDDING -> BLOOMING
        growth = growthRepository.findTopByFlowerOrderByRecordedAtDesc(testFlower).orElseThrow();
        growth.setRecordedAt(LocalDateTime.now().minusDays(7));
        growth.setHeight(75.0); // 75% of max height
        growthRepository.save(growth);

        GrowthAutomationService.GrowthUpdateResult result3 =
                growthAutomationService.updateFlowerGrowth(testFlower);
        assertTrue(result3.isUpdated());
        assertEquals(GrowthStage.BLOOMING, result3.getStage());
    }

    @Test
    void shouldPerformBatchUpdateForMultipleFlowers() {
        // Arrange - Create additional flowers
        Flower flower2 = new Flower();
        flower2.setFlowerName("Test Tulip");
        flower2.setMaxHeight(60.0);
        flower2.setGrowthRate(10.0);
        flower2.setAutoScheduling(true);
        flower2.setPlantingDate(LocalDateTime.now().minusMonths(1));
        flower2 = flowerRepository.save(flower2);

        Growth growth2 = new Growth();
        growth2.setFlower(flower2);
        growth2.setHeight(30.0);
        growth2.setStage(GrowthStage.SEEDLING);
        growth2.setRecordedAt(LocalDateTime.now().minusDays(7));
        growth2.setGrowthSinceLast(0.0);
        growthRepository.save(growth2);

        // Act
        String summary = growthAutomationService.performWeeklyGrowthUpdateWithSummary();

        // Assert
        assertNotNull(summary);
        assertTrue(summary.contains("Successfully updated: 2"));
        assertTrue(summary.contains(testFlower.getFlowerName()));
        assertTrue(summary.contains(flower2.getFlowerName()));
        assertTrue(summary.contains(testFlower.getFlowerName()));
        assertTrue(summary.contains(flower2.getFlowerName()));

        // Verify both flowers were updated in database
        Optional<Growth> growth1Updated = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower);
        Optional<Growth> growth2Updated = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(flower2);

        assertTrue(growth1Updated.isPresent());
        assertTrue(growth2Updated.isPresent());
        assertThat(growth1Updated.get().getHeight()).isGreaterThan(50.0);
        assertThat(growth2Updated.get().getHeight()).isGreaterThan(30.0);
    }

    @Test
    void shouldHandlePendingMaintenanceTasks() {
        // Arrange - Create overdue maintenance task
        Maintenance overdueTask = new Maintenance();
        overdueTask.setFlower(testFlower);
        overdueTask.setTaskType(MaintenanceType.WATERING);
        overdueTask.setDueDate(LocalDateTime.now().minusDays(3));
        overdueTask.setCompleted(false);
        overdueTask.setNotes("Overdue watering task");
        maintenanceRepository.save(overdueTask);

        // Act
        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(testFlower);

        // Assert
        assertFalse(result.isUpdated());
        assertTrue(result.getMessage().contains("maintenance overdue"));

        // Verify height didn't change
        Optional<Growth> growth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower);
        assertTrue(growth.isPresent());
        assertEquals(50.0, growth.get().getHeight());
    }

    @Test
    void shouldOnlyUpdateFlowersWithAutoSchedulingEnabled() {
        // Arrange - Create flower with auto-scheduling disabled
        Flower manualFlower = new Flower();
        manualFlower.setFlowerName("Manual Rose");
        manualFlower.setMaxHeight(80.0);
        manualFlower.setGrowthRate(8.0);
        manualFlower.setAutoScheduling(false); // Disabled
        manualFlower.setPlantingDate(LocalDateTime.now().minusMonths(1));
        manualFlower = flowerRepository.save(manualFlower);

        Growth manualGrowth = new Growth();
        manualGrowth.setFlower(manualFlower);
        manualGrowth.setHeight(40.0);
        manualGrowth.setStage(GrowthStage.SEEDLING);
        manualGrowth.setRecordedAt(LocalDateTime.now().minusDays(7));
        growthRepository.save(manualGrowth);

        // Act
        String summary = growthAutomationService.performWeeklyGrowthUpdateWithSummary();

        // Assert - Only testFlower should be updated (has auto-scheduling enabled)
        assertFalse(summary.contains("Manual Rose"));

        // Verify manual flower was NOT updated
        Optional<Growth> manualGrowthAfter = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(manualFlower);
        assertTrue(manualGrowthAfter.isPresent());
        assertEquals(40.0, manualGrowthAfter.get().getHeight()); // Unchanged
    }

    @Test
    void shouldCalculateCorrectGrowthBasedOnTimeElapsed() {
        // Arrange - Set specific time elapsed
        Growth growth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower)
                .orElseThrow();
        growth.setRecordedAt(LocalDateTime.now().minusDays(14)); // 2 weeks ago
        growthRepository.save(growth);

        testFlower.setGrowthRate(5.0); // 5% per week
        flowerRepository.save(testFlower);

        // Act
        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(testFlower);

        // Assert
        assertTrue(result.isUpdated());
        // Expected: 50.0 + (5% * 2 weeks) * 100 max height = 50.0 + 10.0 = 60.0
        assertThat(result.getNewHeight()).isCloseTo(60.0, within(0.5));
        assertThat(result.getNewHeight() - result.getOldHeight()).isCloseTo(10.0, within(0.5));
    }

    @Test
    void shouldPreserveWiltingStageOnceSet() {
        // Arrange - Set flower to WILTING
        Growth growth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower)
                .orElseThrow();
        growth.setStage(GrowthStage.WILTING);
        growth.setHeight(70.0); // Even at high percentage
        growthRepository.save(growth);

        testFlower.setWaterFrequencyDays(7);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(2)); // Not overdue
        flowerRepository.save(testFlower);

        // Act - Try to update (should keep wilting)
        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(testFlower);

        // Assert - Stage should remain WILTING
        assertTrue(result.isUpdated());
        assertEquals(GrowthStage.WILTING, result.getStage());

        // Verify in database
        Growth updatedGrowth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(testFlower)
                .orElseThrow();
        assertEquals(GrowthStage.WILTING, updatedGrowth.getStage());
    }
}