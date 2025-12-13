package org.example.flowerapp.DTO;

import org.example.flowerapp.Models.Enums.MaintenanceType;

import java.time.LocalDateTime;

public record MaintenanceResponseDTO (
         Long task_id,
         Long flower_id,
         MaintenanceType maintenanceType,
         LocalDateTime maintenanceDate,
         String notes,
         String performedBy,
         LocalDateTime createdAt
){

}