package org.example.flowerapp.DTO;

import jakarta.validation.constraints.NotNull;
import org.example.flowerapp.Models.Enums.MaintenanceType;

import java.time.LocalDateTime;


public record MaintenanceRequestDTO (
         Long flower_id,

        @NotNull(message = "Maintenance Type is required!")
         MaintenanceType maintenanceType,

         LocalDateTime maintenanceDate,
         String notes,
         String performedBy
) {

}