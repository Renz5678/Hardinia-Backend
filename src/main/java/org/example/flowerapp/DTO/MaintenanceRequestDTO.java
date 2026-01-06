package org.example.flowerapp.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.example.flowerapp.Models.Enums.MaintenanceType;

import java.time.LocalDateTime;


public record MaintenanceRequestDTO (
        @NotNull(message = "Flower ID is required!")
        @JsonProperty("flower_id")  // ✅ Maps snake_case from frontend
         Long flower_id,

        @NotNull(message = "Maintenance Type is required!")
         MaintenanceType maintenanceType,

         LocalDateTime maintenanceDate,
         String notes,
         String performedBy
) {

}