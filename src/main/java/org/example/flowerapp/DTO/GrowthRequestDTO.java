package org.example.flowerapp.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.flowerapp.Models.Enums.GrowthStage;

public record GrowthRequestDTO(
        @NotNull(message = "Flower ID is required!")
        Long flower_id,

        @NotNull(message = "Growth stage is required!")
        GrowthStage stage,

        @Positive(message = "Height must be a positive number!")
        double height,

        boolean colorChanges,

        String notes
) {}