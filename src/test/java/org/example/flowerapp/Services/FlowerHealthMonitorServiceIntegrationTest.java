package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Enums.FlowerColor;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Growth;
import org.example.flowerapp.Models.Maintenance;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FlowerHealthMonitorServiceIntegrationTest {

    @Autowired
    private FlowerHealthMonitorService flowerHealthMonitorService;

    @Autowired
    private FlowerRepository flowerRepository;

    @Autowired
    private GrowthRepository growthRepository;

    @Autowired
    private MaintenanceRepository maintenanceRepository;

    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private Flower testFlower;

    @BeforeEach
    void setUp() {
        cleanUpTestData();
        testFlower = createTestFlower("Rose", TEST_USER_ID);
    }

    @Test
    void testMonitorFlowerHealth_NoOverdueTasks_StageUnchanged() {
        Growth initialGrowth = createGrowthRecord(testFlower, GrowthStage.BLOOMING);
        createMaintenanceTask(testFlower, MaintenanceType.WATERING, LocalDateTime.now(), false);

        flowerHealthMonitorService.monitorFlowerHealth();

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertNotNull(latestGrowth);
        assertEquals(GrowthStage.BLOOMING, latestGrowth.getStage());
    }

    @Test
    void testMonitorFlowerHealth_ThreeDaysOverdue_StageChangesToWilting() {
        createGrowthRecord(testFlower, GrowthStage.BLOOMING);
        createMaintenanceTask(testFlower, MaintenanceType.WATERING,
                LocalDateTime.now().minusDays(3), false);

        flowerHealthMonitorService.monitorFlowerHealth();

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertNotNull(latestGrowth);
        assertEquals(GrowthStage.WILTING, latestGrowth.getStage());
        assertTrue(latestGrowth.getNotes().contains("Auto-updated to Wilting"));
        assertTrue(latestGrowth.getNotes().contains("3 days"));
    }

    @Test
    void testMonitorFlowerHealth_FourDaysOverdue_StageChangesToWilting() {
        createGrowthRecord(testFlower, GrowthStage.BLOOMING);
        createMaintenanceTask(testFlower, MaintenanceType.FERTILIZING,
                LocalDateTime.now().minusDays(4), false);

        flowerHealthMonitorService.monitorFlowerHealth();

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertNotNull(latestGrowth);
        assertEquals(GrowthStage.WILTING, latestGrowth.getStage());
    }

    @Test
    void testMonitorFlowerHealth_SevenDaysOverdue_StageChangesToDead() {
        createGrowthRecord(testFlower, GrowthStage.BLOOMING);
        createMaintenanceTask(testFlower, MaintenanceType.WATERING,
                LocalDateTime.now().minusDays(7), false);

        flowerHealthMonitorService.monitorFlowerHealth();

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertNotNull(latestGrowth);
        assertEquals(GrowthStage.DEAD, latestGrowth.getStage());
        assertTrue(latestGrowth.getNotes().contains("Auto-updated to Dead"));
        assertTrue(latestGrowth.getNotes().contains("7 days"));
    }

    @Test
    void testMonitorFlowerHealth_TenDaysOverdue_StageChangesToDead() {
        createGrowthRecord(testFlower, GrowthStage.BLOOMING);
        createMaintenanceTask(testFlower, MaintenanceType.PRUNING,
                LocalDateTime.now().minusDays(10), false);

        flowerHealthMonitorService.monitorFlowerHealth();

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertNotNull(latestGrowth);
        assertEquals(GrowthStage.DEAD, latestGrowth.getStage());
    }

    @Test
    void testMonitorFlowerHealth_MultipleOverdueTasks_UsesMaximumOverdue() {
        createGrowthRecord(testFlower, GrowthStage.BLOOMING);
        createMaintenanceTask(testFlower, MaintenanceType.WATERING,
                LocalDateTime.now().minusDays(3), false);
        createMaintenanceTask(testFlower, MaintenanceType.FERTILIZING,
                LocalDateTime.now().minusDays(8), false);

        flowerHealthMonitorService.monitorFlowerHealth();

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertNotNull(latestGrowth);
        assertEquals(GrowthStage.DEAD, latestGrowth.getStage());
        assertTrue(latestGrowth.getNotes().contains("8 days"));
    }

    @Test
    void testMonitorFlowerHealth_AlreadyDead_NoUpdate() {
        createGrowthRecord(testFlower, GrowthStage.DEAD);
        createMaintenanceTask(testFlower, MaintenanceType.WATERING,
                LocalDateTime.now().minusDays(10), false);

        int initialGrowthRecordCount = getGrowthRecordCountForFlower(testFlower.getFlower_id());

        flowerHealthMonitorService.monitorFlowerHealth();

        int finalGrowthRecordCount = getGrowthRecordCountForFlower(testFlower.getFlower_id());
        assertEquals(initialGrowthRecordCount, finalGrowthRecordCount);

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertEquals(GrowthStage.DEAD, latestGrowth.getStage());
    }

    @Test
    void testMonitorFlowerHealth_CompletedTasks_NotConsideredOverdue() {
        createGrowthRecord(testFlower, GrowthStage.BLOOMING);
        createMaintenanceTask(testFlower, MaintenanceType.WATERING,
                LocalDateTime.now().minusDays(10), true);

        flowerHealthMonitorService.monitorFlowerHealth();

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertNotNull(latestGrowth);
        assertEquals(GrowthStage.BLOOMING, latestGrowth.getStage());
    }

    @Test
    void testMonitorFlowerHealth_NoGrowthRecord_SkipsFlower() {
        Flower flowerWithoutGrowth = createTestFlower("Tulip", TEST_USER_ID);
        createMaintenanceTask(flowerWithoutGrowth, MaintenanceType.WATERING,
                LocalDateTime.now().minusDays(5), false);

        assertDoesNotThrow(() -> flowerHealthMonitorService.monitorFlowerHealth());

        Growth latestGrowth = getLatestGrowthRecord(flowerWithoutGrowth.getFlower_id());
        assertNull(latestGrowth);
    }

    @Test
    void testCheckFlowerHealth_ManualCheck_ThreeDaysOverdue() {
        createGrowthRecord(testFlower, GrowthStage.BLOOMING);
        createMaintenanceTask(testFlower, MaintenanceType.WATERING,
                LocalDateTime.now().minusDays(3), false);

        flowerHealthMonitorService.checkFlowerHealth(testFlower.getFlower_id(), TEST_USER_ID);

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertNotNull(latestGrowth);
        assertEquals(GrowthStage.WILTING, latestGrowth.getStage());
    }

    @Test
    void testCheckFlowerHealth_WrongUserId_ThrowsException() {
        createGrowthRecord(testFlower, GrowthStage.BLOOMING);

        assertThrows(IllegalArgumentException.class, () ->
                flowerHealthMonitorService.checkFlowerHealth(testFlower.getFlower_id(), "wrong-user"));
    }

    @Test
    void testCheckFlowerHealth_NonExistentFlower_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                flowerHealthMonitorService.checkFlowerHealth(99999L, TEST_USER_ID));
    }

    @Test
    void testCheckFlowerHealth_AlreadyDead_NoChange() {
        createGrowthRecord(testFlower, GrowthStage.DEAD);
        createMaintenanceTask(testFlower, MaintenanceType.WATERING,
                LocalDateTime.now().minusDays(10), false);

        int initialGrowthRecordCount = getGrowthRecordCountForFlower(testFlower.getFlower_id());

        flowerHealthMonitorService.checkFlowerHealth(testFlower.getFlower_id(), TEST_USER_ID);

        int finalGrowthRecordCount = getGrowthRecordCountForFlower(testFlower.getFlower_id());
        assertEquals(initialGrowthRecordCount, finalGrowthRecordCount);
    }

    @Test
    void testMonitorFlowerHealth_PreservesFlowerHeight() {
        Growth initialGrowth = createGrowthRecord(testFlower, GrowthStage.BLOOMING);
        initialGrowth.setHeight(25.5);
        growthRepository.save(initialGrowth);

        createMaintenanceTask(testFlower, MaintenanceType.WATERING,
                LocalDateTime.now().minusDays(7), false);

        flowerHealthMonitorService.monitorFlowerHealth();

        Growth latestGrowth = getLatestGrowthRecord(testFlower.getFlower_id());
        assertNotNull(latestGrowth);
        assertEquals(GrowthStage.DEAD, latestGrowth.getStage());
        assertEquals(25.5, latestGrowth.getHeight());
    }

    // Helper Methods

    private Flower createTestFlower(String name, String userId) {
        Flower flower = new Flower();
        flower.setFlowerName(name);
        flower.setSpecies("Test Species");
        flower.setPlantingDate(LocalDateTime.now().minusDays(30));
        flower.setWaterFrequencyDays(7);
        flower.setFertilizeFrequencyDays(14);
        flower.setPruneFrequencyDays(30);
        flower.setAutoScheduling(true);
        flower.setUserId(userId);
        flower.setMaxHeight(50.0);
        flower.setGrowthRate(1.5);

        return flowerRepository.save(flower);
    }

    private Growth createGrowthRecord(Flower flower, GrowthStage stage) {
        Growth growth = new Growth();
        growth.setFlower(flower);
        growth.setStage(stage);
        growth.setHeight(10.0);
        growth.setRecordedAt(LocalDateTime.now());
        growth.setNotes("Test growth record");
        growth.setUserId(flower.getUserId());

        return growthRepository.save(growth);
    }

    private Maintenance createMaintenanceTask(Flower flower, MaintenanceType type,
                                              LocalDateTime scheduledDate, boolean completed) {
        Maintenance task = new Maintenance();
        task.setFlower(flower);
        task.setTaskType(type);
        task.setScheduledDate(scheduledDate);
        task.setCompleted(completed);
        task.setAutoGenerated(true);
        task.setCreatedAt(LocalDateTime.now());
        task.setNotes("Test maintenance task");
        task.setUserId(flower.getUserId());

        return maintenanceRepository.save(task);
    }

    /**
     * Get the latest growth record for a flower by querying the repository directly
     */
    private Growth getLatestGrowthRecord(Long flowerId) {
        // Assuming GrowthRepository has a method to find growth records by flower ID
        // You may need to adjust this based on your actual GrowthRepository methods
        try {
            return growthRepository.findLatestByFlowerId(flowerId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the count of growth records for a flower
     */
    private int getGrowthRecordCountForFlower(Long flowerId) {
        try {
            List<Growth> records = growthRepository.findByFlowerId(flowerId);
            return records != null ? records.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private void cleanUpTestData() {
        // Clean up is handled by @Transactional
    }
}