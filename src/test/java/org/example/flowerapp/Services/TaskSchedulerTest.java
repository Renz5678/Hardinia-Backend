package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Repository.FlowerRepository;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskSchedulerTest {

    @Mock
    private FlowerRepository flowerRepository;

    @Mock
    private MaintenanceRepository maintenanceRepository;

    @InjectMocks
    private FlowerMaintenanceScheduler flowerMaintenanceScheduler;

    private Flower testFlower;

    @BeforeEach
    void setUp() {
        testFlower = new Flower();
        testFlower.setFlower_id(1L);
        testFlower.setFlowerName("Test Rose");
        testFlower.setPlantingDate(LocalDateTime.now().minusDays(10));
        testFlower.setAutoScheduling(true);
    }

    @Test
    void shouldCreateWateringTaskWhenDue() {
        // Given
        testFlower.setWaterFrequencyDays(2);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(3)); // 3 days ago

        when(flowerRepository.findAllFlower()).thenReturn(List.of(testFlower));
        when(maintenanceRepository.existsByFlowerAndTypeAndDateRange(anyLong(), any(), any(), any(), anyString()))
                .thenReturn(false);

        // When
        flowerMaintenanceScheduler.scheduleMaintenanceTasks();

        // Then
        verify(maintenanceRepository, times(1)).save(any(Maintenance.class));
    }

    @Test
    void shouldNotCreateTaskWhenNotDue() {
        // Given
        testFlower.setWaterFrequencyDays(2);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(1)); // Only 1 day ago

        when(flowerRepository.findAllFlower()).thenReturn(List.of(testFlower));

        // When
        flowerMaintenanceScheduler.scheduleMaintenanceTasks();

        // Then
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }

    @Test
    void shouldNotCreateTaskWhenAutoScheduleDisabled() {
        // Given
        testFlower.setAutoScheduling(false); // Disabled
        testFlower.setWaterFrequencyDays(2);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(3));

        when(flowerRepository.findAllFlower()).thenReturn(List.of(testFlower));

        // When
        flowerMaintenanceScheduler.scheduleMaintenanceTasks();

        // Then
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }

    @Test
    void shouldNotCreateDuplicateTask() {
        // Given
        testFlower.setWaterFrequencyDays(2);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(3));

        when(flowerRepository.findAllFlower()).thenReturn(List.of(testFlower));
        when(maintenanceRepository.existsByFlowerAndTypeAndDateRange(anyLong(), any(), any(), any(), anyString()))
                .thenReturn(true); // Task already exists

        // When
        flowerMaintenanceScheduler.scheduleMaintenanceTasks();

        // Then
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }

    @Test
    void shouldSkipFlowerWithoutFrequencySet() {
        // Given
        testFlower.setWaterFrequencyDays(null); // No frequency set

        when(flowerRepository.findAllFlower()).thenReturn(List.of(testFlower));

        // When
        flowerMaintenanceScheduler.scheduleMaintenanceTasks();

        // Then
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
    }

    @Test
    void shouldCreateMultipleTasksForMultipleFlowers() {
        // Given
        Flower rose = createFlowerWithWateringDue("Rose");
        Flower tulip = createFlowerWithWateringDue("Tulip");

        when(flowerRepository.findAllFlower()).thenReturn(Arrays.asList(rose, tulip));
        when(maintenanceRepository.existsByFlowerAndTypeAndDateRange(anyLong(), any(), any(), any(), anyString()))
                .thenReturn(false);

        // When
        flowerMaintenanceScheduler.scheduleMaintenanceTasks();

        // Then
        verify(maintenanceRepository, times(2)).save(any(Maintenance.class));
    }

    @Test
    void shouldCreateBothWateringAndFertilizingWhenBothDue() {
        // Given
        testFlower.setWaterFrequencyDays(2);
        testFlower.setLastWateredDate(LocalDateTime.now().minusDays(3));
        testFlower.setFertilizeFrequencyDays(14);
        testFlower.setLastFertilizedDate(LocalDateTime.now().minusDays(15));

        when(flowerRepository.findAllFlower()).thenReturn(List.of(testFlower));
        when(maintenanceRepository.existsByFlowerAndTypeAndDateRange(anyLong(), any(), any(), any(), anyString()))
                .thenReturn(false);

        // When
        flowerMaintenanceScheduler.scheduleMaintenanceTasks();

        // Then
        verify(maintenanceRepository, times(2)).save(any(Maintenance.class));
    }

    private Flower createFlowerWithWateringDue(String name) {
        Flower flower = new Flower();
        flower.setFlower_id(System.currentTimeMillis());
        flower.setFlowerName(name);
        flower.setPlantingDate(LocalDateTime.now().minusDays(10));
        flower.setAutoScheduling(true);
        flower.setWaterFrequencyDays(2);
        flower.setLastWateredDate(LocalDateTime.now().minusDays(3));
        return flower;
    }
}