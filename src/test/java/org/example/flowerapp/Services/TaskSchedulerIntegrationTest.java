package org.example.flowerapp.Services;

import jakarta.persistence.EntityManager;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Growth;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Models.Enums.FlowerColor;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.GrowthRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Commit
class GrowthAutomationServiceIntegrationTest {

    @Autowired
    private GrowthAutomationService growthAutomationService;

    @Autowired
    private FlowerRepository flowerRepository;

    @Autowired
    private GrowthRepository growthRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    @AfterEach
    void cleanup() {
        // Clean up test data
        List<Maintenance> allMaintenance = maintenanceRepository.findAllMaintenance();
        for (Maintenance m : allMaintenance) {
            maintenanceRepository.deleteMaintenance(m.getTask_id());
        }

        List<Growth> allGrowth = growthRepository.findAll();
        for (Growth g : allGrowth) {
            growthRepository.delete(g);
        }

        List<Flower> allFlowers = flowerRepository.findAllFlower();
        for (Flower f : allFlowers) {
            flowerRepository.deleteFlower(f.getFlower_id());
        }
    }

    @Autowired
    private EntityManager entityManager;

    @Test
    void testUpdateFlowerGrowth_NormalGrowth_SavesCorrectly() {
        // Given
        Flower rose = createTestFlower("Test Rose", 100.0, 2.0);
        Flower savedFlower = flowerRepository.save(rose);

        Growth initialGrowth = createInitialGrowth(savedFlower, 25.0, GrowthStage.SEEDLING);
        growthRepository.save(initialGrowth);

        entityManager.flush();
        entityManager.clear();

        // When
        growthAutomationService.updateFlowerGrowth(savedFlower);

        entityManager.flush();
        entityManager.clear();

        // Then
        List<Growth> growthRecords = growthRepository.findByFlowerOrderByRecordedAtDesc(savedFlower);

        assertEquals(2, growthRecords.size());
        Growth latestGrowth = growthRecords.get(0);

        assertEquals(27.0, latestGrowth.getHeight(), 0.01);
        assertEquals(2.0, latestGrowth.getGrowthSinceLast(), 0.01);
    }

    @Test
    void testUpdateFlowerGrowth_StageProgression_SeedlingToBudding() {
        // Given: Flower near budding threshold
        Flower rose = createTestFlower("Budding Rose", 100.0, 15.0);
        Flower savedFlower = flowerRepository.save(rose);

        Growth initialGrowth = createInitialGrowth(savedFlower, 30.0, GrowthStage.SEEDLING);
        growthRepository.save(initialGrowth);

        System.out.println("Initial stage: " + initialGrowth.getStage());
        System.out.println("Initial height: " + initialGrowth.getHeight() + " (30% of max)");

        // When: Update growth (will cross 40% threshold)
        growthAutomationService.updateFlowerGrowth(savedFlower);

        // Then: Should progress to BUDDING
        Growth latestGrowth = growthRepository.findTopByFlowerOrderByRecordedAtDesc(savedFlower).orElseThrow();

        System.out.println("New stage: " + latestGrowth.getStage());
        System.out.println("New height: " + latestGrowth.getHeight());
        System.out.println("Notes: " + latestGrowth.getNotes());

        assertEquals(GrowthStage.BUDDING, latestGrowth.getStage(), "Should progress to BUDDING stage");
        assertTrue(latestGrowth.getHeight() >= 40.0, "Height should be at least 40% of max");
    }

    @Test
    void testUpdateFlowerGrowth_AllMaintenanceOverdue_SetsToWilting() {
        // Given: Flower with all maintenance overdue
        Flower rose = createTestFlower("Neglected Rose", 100.0, 2.0);
        rose.setLastWateredDate(LocalDateTime.now().minusDays(5));
        rose.setLastFertilizedDate(LocalDateTime.now().minusDays(10));
        rose.setLastPrunedDate(LocalDateTime.now().minusDays(20));
        Flower savedFlower = flowerRepository.save(rose);

        Growth initialGrowth = createInitialGrowth(savedFlower, 50.0, GrowthStage.SEEDLING);
        growthRepository.save(initialGrowth);

        System.out.println("Last watered: " + rose.getLastWateredDate() + " (should be every " + rose.getWaterFrequencyDays() + " days)");
        System.out.println("Last fertilized: " + rose.getLastFertilizedDate() + " (should be every " + rose.getFertilizeFrequencyDays() + " days)");
        System.out.println("Last pruned: " + rose.getLastPrunedDate() + " (should be every " + rose.getPruneFrequencyDays() + " days)");

        // When: Update growth
        growthAutomationService.updateFlowerGrowth(savedFlower);

        // Then: Should be wilting
        Growth latestGrowth = growthRepository.findTopByFlowerOrderByRecordedAtDesc(savedFlower).orElseThrow();

        System.out.println("New stage: " + latestGrowth.getStage());
        System.out.println("Color changes: " + latestGrowth.isColorChanges());
        System.out.println("Notes: " + latestGrowth.getNotes());

        assertEquals(GrowthStage.WILTING, latestGrowth.getStage(), "Should be in WILTING stage");
        assertTrue(latestGrowth.isColorChanges(), "Should have color changes");
        assertTrue(latestGrowth.getNotes().toLowerCase().contains("wilting"), "Notes should mention wilting");
    }

    @Test
    void testUpdateFlowerGrowth_WithOverdueMaintenanceTasks_SkipsGrowth() {
        // Given: Flower with overdue maintenance task
        Flower rose = createTestFlower("Task Rose", 100.0, 2.0);
        Flower savedFlower = flowerRepository.save(rose);

        Growth initialGrowth = createInitialGrowth(savedFlower, 25.0, GrowthStage.SEEDLING);
        growthRepository.save(initialGrowth);

        Maintenance overdueTask = new Maintenance();
        overdueTask.setFlower(savedFlower);
        overdueTask.setTaskType(MaintenanceType.WATERING);
        overdueTask.setDueDate(LocalDateTime.now().minusDays(2));
        overdueTask.setCompleted(false);
        overdueTask.setAutoGenerated(true);
        maintenanceRepository.save(overdueTask);

        int initialGrowthCount = growthRepository.findByFlowerOrderByRecordedAtDesc(savedFlower).size();
        System.out.println("Initial growth records: " + initialGrowthCount);
        System.out.println("Overdue task: " + overdueTask.getTaskType() + " due " + overdueTask.getDueDate());

        // When: Update growth
        growthAutomationService.updateFlowerGrowth(savedFlower);

        // Then: Should skip growth
        int finalGrowthCount = growthRepository.findByFlowerOrderByRecordedAtDesc(savedFlower).size();
        System.out.println("Final growth records: " + finalGrowthCount);

        assertEquals(initialGrowthCount, finalGrowthCount, "Should not create new growth record when maintenance is overdue");
    }

    @Test
    void testUpdateFlowerGrowth_MaxHeightReached_NoFurtherGrowth() {
        // Given: Flower at max height
        Flower rose = createTestFlower("Maxed Rose", 100.0, 2.0);
        Flower savedFlower = flowerRepository.save(rose);

        Growth initialGrowth = createInitialGrowth(savedFlower, 100.0, GrowthStage.BLOOMING);
        growthRepository.save(initialGrowth);

        int initialGrowthCount = growthRepository.findByFlowerOrderByRecordedAtDesc(savedFlower).size();
        System.out.println("Flower at max height: " + initialGrowth.getHeight());

        // When: Try to update growth
        growthAutomationService.updateFlowerGrowth(savedFlower);

        // Then: No new growth record
        int finalGrowthCount = growthRepository.findByFlowerOrderByRecordedAtDesc(savedFlower).size();
        System.out.println("Growth records after update: " + finalGrowthCount);

        assertEquals(initialGrowthCount, finalGrowthCount, "Should not create new growth record at max height");
    }

    @Test
    void testUpdateFlowerGrowth_CapsAtMaxHeight() {
        // Given: Flower near max with high growth rate
        Flower rose = createTestFlower("Almost Max Rose", 100.0, 10.0);
        Flower savedFlower = flowerRepository.save(rose);

        Growth initialGrowth = createInitialGrowth(savedFlower, 95.0, GrowthStage.BLOOMING);
        growthRepository.save(initialGrowth);

        System.out.println("Initial height: " + initialGrowth.getHeight());
        System.out.println("Growth rate: " + savedFlower.getGrowthRate() + " (would exceed max)");

        // When: Update growth
        growthAutomationService.updateFlowerGrowth(savedFlower);

        // Then: Should cap at max
        Growth latestGrowth = growthRepository.findTopByFlowerOrderByRecordedAtDesc(savedFlower).orElseThrow();
        System.out.println("Final height: " + latestGrowth.getHeight());

        assertEquals(100.0, latestGrowth.getHeight(), 0.01, "Height should be capped at max height");
    }

    @Test
    void testPerformDailyGrowthUpdate_UpdatesAllAutoScheduledFlowers() {
        // Given: Multiple flowers with auto-scheduling
        Flower flower1 = createTestFlower("Rose 1", 100.0, 2.0);
        Flower savedFlower1 = flowerRepository.save(flower1);
        Growth growth1 = createInitialGrowth(savedFlower1, 25.0, GrowthStage.SEEDLING);
        growthRepository.save(growth1);

        Flower flower2 = createTestFlower("Tulip 2", 50.0, 1.5);
        Flower savedFlower2 = flowerRepository.save(flower2);
        Growth growth2 = createInitialGrowth(savedFlower2, 10.0, GrowthStage.SEED);
        growthRepository.save(growth2);

        int initialTotalGrowthRecords = growthRepository.findAll().size();
        System.out.println("Initial total growth records: " + initialTotalGrowthRecords);

        // When: Run daily update
        growthAutomationService.performDailyGrowthUpdate();

        // Then: Both flowers should have new growth records
        int finalTotalGrowthRecords = growthRepository.findAll().size();
        System.out.println("Final total growth records: " + finalTotalGrowthRecords);

        assertEquals(initialTotalGrowthRecords + 2, finalTotalGrowthRecords,
                "Should create 2 new growth records (one for each flower)");

        Growth flower1Latest = growthRepository.findTopByFlowerOrderByRecordedAtDesc(savedFlower1).orElseThrow();
        Growth flower2Latest = growthRepository.findTopByFlowerOrderByRecordedAtDesc(savedFlower2).orElseThrow();

        System.out.println("Flower 1 grew from " + growth1.getHeight() + " to " + flower1Latest.getHeight());
        System.out.println("Flower 2 grew from " + growth2.getHeight() + " to " + flower2Latest.getHeight());

        assertTrue(flower1Latest.getHeight() > 25.0, "Flower 1 should have grown");
        assertTrue(flower2Latest.getHeight() > 10.0, "Flower 2 should have grown");
    }

    @Test
    void testUpdateFlowerGrowth_NoGrowthRecord_CreatesInitialRecord() {
        // Given: New flower without growth record
        Flower newFlower = createTestFlower("Brand New Orchid", 80.0, 1.0);
        Flower savedFlower = flowerRepository.save(newFlower);

        System.out.println("Created flower without growth record");

        // When: Update growth
        growthAutomationService.updateFlowerGrowth(savedFlower);

        // Then: Should create initial record
        Optional<Growth> initialGrowthOpt = growthRepository.findTopByFlowerOrderByRecordedAtDesc(savedFlower);
        assertTrue(initialGrowthOpt.isPresent(), "Should create initial growth record");

        Growth initialGrowth = initialGrowthOpt.get();
        System.out.println("Created initial record - Height: " + initialGrowth.getHeight() + ", Stage: " + initialGrowth.getStage());

        assertEquals(0.0, initialGrowth.getHeight(), "Initial height should be 0");
        assertEquals(GrowthStage.SEED, initialGrowth.getStage(), "Initial stage should be SEED");
        assertEquals("Initial growth record", initialGrowth.getNotes());
    }

    @Test
    void testUpdateFlowerGrowth_MultiDayGrowth_CalculatesCorrectly() {
        // Given: Flower with old growth record
        Flower rose = createTestFlower("Time Travel Rose", 100.0, 2.0);
        Flower savedFlower = flowerRepository.save(rose);

        Growth oldGrowth = createInitialGrowth(savedFlower, 25.0, GrowthStage.SEEDLING);
        oldGrowth.setRecordedAt(LocalDateTime.now().minusDays(5));
        growthRepository.save(oldGrowth);

        System.out.println("Last recorded: 5 days ago at height " + oldGrowth.getHeight());
        System.out.println("Growth rate: " + savedFlower.getGrowthRate() + " per day");

        // When: Update growth
        growthAutomationService.updateFlowerGrowth(savedFlower);

        // Then: Should calculate 5 days of growth
        Growth latestGrowth = growthRepository.findTopByFlowerOrderByRecordedAtDesc(savedFlower).orElseThrow();
        double expectedGrowth = 2.0 * 5;

        System.out.println("Expected growth: " + expectedGrowth);
        System.out.println("Actual growth: " + latestGrowth.getGrowthSinceLast());
        System.out.println("New height: " + latestGrowth.getHeight());

        assertEquals(expectedGrowth, latestGrowth.getGrowthSinceLast(), 0.01,
                "Should calculate multi-day growth correctly");
        assertEquals(25.0 + expectedGrowth, latestGrowth.getHeight(), 0.01);
    }

    @Test
    void testUpdateFlowerGrowth_AlreadyUpdatedToday_DoesNotUpdate() {
        // Given: Flower updated once
        Flower rose = createTestFlower("Same Day Rose", 100.0, 2.0);
        Flower savedFlower = flowerRepository.save(rose);

        Growth initialGrowth = createInitialGrowth(savedFlower, 25.0, GrowthStage.SEEDLING);
        growthRepository.save(initialGrowth);

        growthAutomationService.updateFlowerGrowth(savedFlower);
        int growthCountAfterFirst = growthRepository.findByFlowerOrderByRecordedAtDesc(savedFlower).size();
        System.out.println("Growth records after first update: " + growthCountAfterFirst);

        // When: Try to update again same day
        growthAutomationService.updateFlowerGrowth(savedFlower);

        // Then: Should not create another record
        int growthCountAfterSecond = growthRepository.findByFlowerOrderByRecordedAtDesc(savedFlower).size();
        System.out.println("Growth records after second update: " + growthCountAfterSecond);

        assertEquals(growthCountAfterFirst, growthCountAfterSecond,
                "Should not create another growth record on the same day");
    }

    @Test
    void testPerformDailyGrowthUpdate_SkipsNonAutoScheduledFlowers() {
        // Given: Flower with auto-scheduling disabled
        Flower rose = createTestFlower("Manual Rose", 100.0, 2.0);
        rose.setAutoScheduling(false);
        Flower savedFlower = flowerRepository.save(rose);

        Growth initialGrowth = createInitialGrowth(savedFlower, 25.0, GrowthStage.SEEDLING);
        growthRepository.save(initialGrowth);

        int initialGrowthCount = growthRepository.findByFlowerOrderByRecordedAtDesc(savedFlower).size();
        System.out.println("Auto-scheduling: " + savedFlower.isAutoScheduling());

        // When: Run daily update
        growthAutomationService.performDailyGrowthUpdate();

        // Then: Should not update
        int finalGrowthCount = growthRepository.findByFlowerOrderByRecordedAtDesc(savedFlower).size();
        System.out.println("Growth records before: " + initialGrowthCount + ", after: " + finalGrowthCount);

        assertEquals(initialGrowthCount, finalGrowthCount,
                "Should not update growth for non-auto-scheduled flowers");
    }

    // Helper methods
    private Flower createTestFlower(String name, double maxHeight, double growthRate) {
        Flower flower = new Flower();
        flower.setFlowerName(name);
        flower.setSpecies("Rosa testis");
        flower.setColor(FlowerColor.RED);
        flower.setPlantingDate(LocalDateTime.now().minusDays(30));
        flower.setGridPosition(1);
        flower.setMaxHeight(maxHeight);
        flower.setGrowthRate(growthRate);
        flower.setAutoScheduling(true);
        flower.setWaterFrequencyDays(3);
        flower.setFertilizeFrequencyDays(7);
        flower.setPruneFrequencyDays(14);
        flower.setLastWateredDate(LocalDateTime.now().minusDays(1));
        flower.setLastFertilizedDate(LocalDateTime.now().minusDays(3));
        flower.setLastPrunedDate(LocalDateTime.now().minusDays(5));
        return flower;
    }

    private Growth createInitialGrowth(Flower flower, double height, GrowthStage stage) {
        Growth growth = new Growth();
        growth.setFlower(flower);
        growth.setHeight(height);
        growth.setStage(stage);
        growth.setRecordedAt(LocalDateTime.now().minusDays(1));
        growth.setGrowthSinceLast(0.0);
        growth.setColorChanges(false);
        growth.setNotes("Initial growth record");
        return growth;
    }
}