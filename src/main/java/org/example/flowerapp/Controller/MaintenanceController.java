package org.example.flowerapp.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.flowerapp.DTO.MaintenanceRequestDTO;
import org.example.flowerapp.DTO.MaintenanceResponseDTO;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Services.FlowerMaintenanceScheduler;
import org.example.flowerapp.Services.MaintenanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {
    private final MaintenanceService maintenanceService;

    @PostMapping
    public ResponseEntity<MaintenanceResponseDTO> createNewMaintenance(
            @Valid @RequestBody MaintenanceRequestDTO dto,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        MaintenanceResponseDTO created = maintenanceService.addNewMaintenance(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceResponseDTO>> getAllMaintenance(
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<MaintenanceResponseDTO> maintenances = maintenanceService.getAllMaintenance(userId);
        return ResponseEntity.ok(maintenances);
    }

    @GetMapping("/task/{task_id}")
    public ResponseEntity<MaintenanceResponseDTO> getMaintenanceById(
            @PathVariable("task_id") long taskId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        MaintenanceResponseDTO maintenance = maintenanceService.getMaintenanceById(taskId, userId);
        return ResponseEntity.ok(maintenance);
    }

    @GetMapping("/flower/{flower_id}")
    public ResponseEntity<List<MaintenanceResponseDTO>> getMaintenanceByFlowerId(
            @PathVariable("flower_id") long flowerId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<MaintenanceResponseDTO> maintenances = maintenanceService.getMaintenanceByFlowerId(flowerId, userId);
        return ResponseEntity.ok(maintenances);
    }

    @GetMapping("/type/{maintenance_type}")
    public ResponseEntity<List<MaintenanceResponseDTO>> getMaintenanceByType(
            @PathVariable("maintenance_type") MaintenanceType maintenanceType,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<MaintenanceResponseDTO> maintenances = maintenanceService.getMaintenanceByType(maintenanceType, userId);
        return ResponseEntity.ok(maintenances);
    }

    @GetMapping("/date")
    public ResponseEntity<List<MaintenanceResponseDTO>> getMaintenanceByDate(
            @RequestParam LocalDateTime maintenanceDate,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        List<MaintenanceResponseDTO> maintenances = maintenanceService.getMaintenanceByDate(maintenanceDate, userId);
        return ResponseEntity.ok(maintenances);
    }

    @PutMapping("/{maintenance_id}")
    public ResponseEntity<MaintenanceResponseDTO> updateMaintenance(
            @Valid @RequestBody MaintenanceRequestDTO dto,
            @PathVariable("maintenance_id") long maintenanceId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        MaintenanceResponseDTO updated = maintenanceService.updateMaintenance(dto, maintenanceId, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{maintenance_id}")
    public ResponseEntity<Void> deleteMaintenance(
            @PathVariable("maintenance_id") long maintenanceId,
            Authentication authentication) {
        String userId = (String) authentication.getPrincipal();
        maintenanceService.deleteMaintenance(maintenanceId, userId);
        return ResponseEntity.noContent().build();
    }
}