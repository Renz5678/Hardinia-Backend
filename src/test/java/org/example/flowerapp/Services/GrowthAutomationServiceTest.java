package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Growth;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.GrowthRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrowthAutomationServiceTest {

    @Mock
    private FlowerRepository flowerRepository;

    @Mock
    private GrowthRepository growthRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private GrowthAutomationService growthAutomationService;

    private Flower flower;
    private Growth existingGrowth;

    @BeforeEach
    void setUp() {
        flower = new Flower();
        flower.setFlower_id(1L);
        flower.setFlowerName("Test Rose");
        flower.setMaxHeight(100.0);
        flower.setGrowthRate(7.0); // 7% per week
        flower.setAutoScheduling(true);
        flower.setPlantingDate(LocalDateTime.now().minusMonths(1));

        existingGrowth = new Growth();
        existingGrowth.setFlower(flower);
        existingGrowth.setHeight(50.0);
        existingGrowth.setStage(GrowthStage.SEEDLING);
        existingGrowth.setRecordedAt(LocalDateTime.now().minusDays(7));
        existingGrowth.setGrowthSinceLast(0.0);
    }

    @Test
    void shouldSkipUpdateWhenMaintenanceOverdue() {
        flower.setWaterFrequencyDays(7);
        flower.setLastWateredDate(LocalDateTime.now().minusDays(10)); // OVERDUE

        flower.setFertilizeFrequencyDays(14);
        flower.setLastFertilizedDate(LocalDateTime.now().minusDays(5)); // NOT overdue

        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower))
                .thenReturn(Optional.of(existingGrowth));

        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(flower);

        assertFalse(result.isUpdated());
        assertEquals("Skipped - maintenance overdue", result.getMessage());
        assertEquals(50.0, result.getOldHeight());
        assertEquals(50.0, result.getNewHeight());

        verify(growthRepository, never()).save(any());
    }


    @Test
    void shouldSetToWiltingWhenAllMaintenanceOverdue() {
        flower.setWaterFrequencyDays(7);
        flower.setLastWateredDate(LocalDateTime.now().minusDays(10));

        flower.setFertilizeFrequencyDays(14);
        flower.setLastFertilizedDate(LocalDateTime.now().minusDays(20));

        flower.setPruneFrequencyDays(30);
        flower.setLastPrunedDate(LocalDateTime.now().minusDays(40));

        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower))
                .thenReturn(Optional.of(existingGrowth));
        when(growthRepository.save(any(Growth.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GrowthAutomationService.GrowthUpdateResult result =
                growthAutomationService.updateFlowerGrowth(flower);

        assertTrue(result.isUpdated());
        assertEquals("Set to WILTING - all maintenance overdue", result.getMessage());
        assertEquals(GrowthStage.WILTING, result.getStage());

        verify(growthRepository).save(any(Growth.class));
    }


    @Test
    void shouldUpdateGrowthSuccessfully() {
        // No overdue maintenance
        flower.setWaterFrequencyDays(7);
        flower.setLastWateredDate(LocalDateTime.now().minusDays(3));

        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower))
                .thenReturn(Optional.of(existingGrowth));
        when(maintenanceRepository.findByFlowerAndCompletedFalseAndDueDateBefore(any(), any()))
                .thenReturn(List.of());
        when(growthRepository.save(any(Growth.class))).thenReturn(existingGrowth);

        GrowthAutomationService.GrowthUpdateResult result = growthAutomationService.updateFlowerGrowth(flower);

        assertTrue(result.isUpdated());
        assertTrue(result.getNewHeight() > result.getOldHeight());
        verify(growthRepository).save(any(Growth.class));
    }

    @Test
    void shouldSkipUpdateWhenTooSoon() {
        existingGrowth.setRecordedAt(LocalDateTime.now().minusDays(3)); // Only 3 days ago

        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower))
                .thenReturn(Optional.of(existingGrowth));

        GrowthAutomationService.GrowthUpdateResult result = growthAutomationService.updateFlowerGrowth(flower);

        assertFalse(result.isUpdated());
        assertEquals("Update too soon (need at least 5 days)", result.getMessage());
        verify(growthRepository, never()).save(any());
    }

    @Test
    void shouldSkipUpdateWhenAtMaxHeight() {
        existingGrowth.setHeight(100.0); // At max height

        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower))
                .thenReturn(Optional.of(existingGrowth));

        GrowthAutomationService.GrowthUpdateResult result = growthAutomationService.updateFlowerGrowth(flower);

        assertFalse(result.isUpdated());
        assertEquals("At maximum height", result.getMessage());
        verify(growthRepository, never()).save(any());
    }

    @Test
    void shouldCreateInitialGrowthRecordWhenNoneExists() {
        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower))
                .thenReturn(Optional.empty());
        when(growthRepository.save(any(Growth.class))).thenReturn(existingGrowth);

        GrowthAutomationService.GrowthUpdateResult result = growthAutomationService.updateFlowerGrowth(flower);

        assertFalse(result.isUpdated());
        assertEquals("Initial growth record created", result.getMessage());
        verify(growthRepository).save(any(Growth.class));
    }

    @Test
    void shouldPerformWeeklyUpdateForMultipleFlowers() {
        Flower flower2 = new Flower();
        flower2.setFlower_id(2L);
        flower2.setFlowerName("Test Tulip");
        flower2.setMaxHeight(60.0);
        flower2.setGrowthRate(9.0);
        flower2.setAutoScheduling(true);

        Growth growth2 = new Growth();
        growth2.setFlower(flower2);
        growth2.setHeight(30.0);
        growth2.setStage(GrowthStage.SEEDLING);
        growth2.setRecordedAt(LocalDateTime.now().minusDays(7));

        when(flowerRepository.findByAutoSchedulingTrue())
                .thenReturn(Arrays.asList(flower, flower2));
        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower))
                .thenReturn(Optional.of(existingGrowth));
        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower2))
                .thenReturn(Optional.of(growth2));
        when(maintenanceRepository.findByFlowerAndCompletedFalseAndDueDateBefore(any(), any()))
                .thenReturn(List.of());
        when(growthRepository.save(any(Growth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String summary = growthAutomationService.performWeeklyGrowthUpdateWithSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("Total flowers processed: 2"));
        assertTrue(summary.contains("Successfully updated: 2"));
        verify(growthRepository, times(2)).save(any(Growth.class));
    }

    @Test
    void shouldHandleErrorsDuringBatchUpdate() {
        Flower flower2 = new Flower();
        flower2.setFlower_id(2L);
        flower2.setFlowerName("Test Tulip");
        flower2.setMaxHeight(60.0);
        flower2.setGrowthRate(9.0);
        flower2.setAutoScheduling(true);

        Growth growth2 = new Growth();
        growth2.setFlower(flower2);
        growth2.setHeight(30.0);
        growth2.setStage(GrowthStage.SEEDLING);
        growth2.setRecordedAt(LocalDateTime.now().minusDays(7));

        when(flowerRepository.findByAutoSchedulingTrue())
                .thenReturn(Arrays.asList(flower, flower2));
        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower))
                .thenThrow(new RuntimeException("Database error"));
        when(growthRepository.findTopByFlowerOrderByRecordedAtDesc(flower2))
                .thenReturn(Optional.of(growth2));
        when(maintenanceRepository.findByFlowerAndCompletedFalseAndDueDateBefore(any(), any()))
                .thenReturn(List.of());
        when(growthRepository.save(any(Growth.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String summary = growthAutomationService.performWeeklyGrowthUpdateWithSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("Total flowers processed: 2"));
        assertTrue(summary.contains("Successfully updated: 1"));
        assertTrue(summary.contains("Error: Database error"));
    }
}