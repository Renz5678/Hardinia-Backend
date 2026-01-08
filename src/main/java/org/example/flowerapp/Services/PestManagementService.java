package org.example.flowerapp.Services;

import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private EmailService emailService;

    @Mock
    private Random mockRandom;

    private PestManagementService pestManagementService;

    @BeforeEach
    void setUp() {
        // Use the 4-parameter constructor for testing
        pestManagementService = new PestManagementService(
                flowerRepository,
                maintenanceRepository,
                emailService,
                mockRandom  // This allows us to control randomness in tests
        );
    }

    @Test
    void testCheckForPestInfestations_WithInfestation() {
        // Arrange
        Flower flower = createTestFlower(1L, "Rose", "user123");
        when(flowerRepository.findAllFlower()).thenReturn(List.of(flower));
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class), eq(MaintenanceType.PEST_CONTROL))).thenReturn(false);
        when(mockRandom.nextDouble()).thenReturn(0.2); // Less than 0.3, will trigger infestation

        // Act
        pestManagementService.checkForPestInfestations();

        // Assert
        verify(maintenanceRepository, times(1)).save(any(Maintenance.class));
        verify(emailService, times(1)).sendPestInfestationAlert(eq("user123"), eq(flower));
    }

    @Test
    void testCheckForPestInfestations_NoInfestation() {
        // Arrange
        Flower flower = createTestFlower(1L, "Tulip", "user456");
        when(flowerRepository.findAllFlower()).thenReturn(List.of(flower));
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class), eq(MaintenanceType.PEST_CONTROL))).thenReturn(false);
        when(mockRandom.nextDouble()).thenReturn(0.8); // Greater than 0.3, no infestation

        // Act
        pestManagementService.checkForPestInfestations();

        // Assert
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
        verify(emailService, never()).sendPestInfestationAlert(any(), any());
    }

    @Test
    void testCheckForPestInfestations_SkipsFlowerWithActivePestTask() {
        // Arrange
        Flower flower = createTestFlower(1L, "Daisy", "user789");
        when(flowerRepository.findAllFlower()).thenReturn(List.of(flower));
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class), eq(MaintenanceType.PEST_CONTROL))).thenReturn(true);

        // Act
        pestManagementService.checkForPestInfestations();

        // Assert
        verify(maintenanceRepository, never()).save(any(Maintenance.class));
        verify(emailService, never()).sendPestInfestationAlert(any(), any());
    }

    @Test
    void testCheckForPestInfestations_MultipleFlowers() {
        // Arrange
        Flower flower1 = createTestFlower(1L, "Rose", "user1");
        Flower flower2 = createTestFlower(2L, "Tulip", "user2");
        Flower flower3 = createTestFlower(3L, "Daisy", "user3");

        when(flowerRepository.findAllFlower()).thenReturn(Arrays.asList(flower1, flower2, flower3));
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class), eq(MaintenanceType.PEST_CONTROL))).thenReturn(false);

        // flower1: gets infestation, flower2: no infestation, flower3: gets infestation
        when(mockRandom.nextDouble()).thenReturn(0.2, 0.8, 0.1);

        // Act
        pestManagementService.checkForPestInfestations();

        // Assert
        verify(maintenanceRepository, times(2)).save(any(Maintenance.class));
        verify(emailService, times(2)).sendPestInfestationAlert(any(), any());
    }

    @Test
    void testCheckForPestInfestations_EmailFailureDoesNotStopProcess() {
        // Arrange
        Flower flower = createTestFlower(1L, "Rose", "user123");
        when(flowerRepository.findAllFlower()).thenReturn(List.of(flower));
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class), eq(MaintenanceType.PEST_CONTROL))).thenReturn(false);
        when(mockRandom.nextDouble()).thenReturn(0.2);
        doThrow(new RuntimeException("Email service down"))
                .when(emailService).sendPestInfestationAlert(any(), any());

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> pestManagementService.checkForPestInfestations());
        verify(maintenanceRepository, times(1)).save(any(Maintenance.class));
    }

    @Test
    void testGetPestStatistics() {
        // Arrange
        Maintenance activePest1 = createMaintenance(1L, false);
        Maintenance activePest2 = createMaintenance(2L, false);
        Maintenance completedPest = createMaintenance(3L, true);

        when(maintenanceRepository.findByMaintenanceType(MaintenanceType.PEST_CONTROL))
                .thenReturn(Arrays.asList(activePest1, activePest2, completedPest));

        // Act
        PestManagementService.PestStatistics stats = pestManagementService.getPestStatistics();

        // Assert
        assertEquals(3, stats.getTotalPestTasks());
        assertEquals(2, stats.getActivePestTasks());
        assertEquals(1, stats.getCompletedPestTasks());
    }

    @Test
    void testTriggerPestCheckManually() {
        // Arrange
        Flower flower = createTestFlower(1L, "Rose", "user123");
        when(flowerRepository.findAllFlower()).thenReturn(List.of(flower));
        when(maintenanceRepository.existsByFlowerAndMaintenanceTypeAndCompletedFalse(
                any(Flower.class), eq(MaintenanceType.PEST_CONTROL))).thenReturn(false);
        when(mockRandom.nextDouble()).thenReturn(0.2);

        // Act
        String result = pestManagementService.triggerPestCheckManually();

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("completed"));
        verify(maintenanceRepository, times(1)).save(any(Maintenance.class));
    }

    // Helper methods
    private Flower createTestFlower(Long id, String name, String userId) {
        Flower flower = new Flower();
        flower.setFlower_id(id);
        flower.setFlowerName(name);
        flower.setUserId(userId);
        return flower;
    }

    private Maintenance createMaintenance(Long id, boolean completed) {
        Maintenance maintenance = new Maintenance();
        maintenance.setCompleted(completed);
        maintenance.setTaskType(MaintenanceType.PEST_CONTROL);
        return maintenance;
    }
}