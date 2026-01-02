package org.example.flowerapp.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.example.flowerapp.Models.Enums.GrowthStage;

import java.time.LocalDateTime;

public record GrowthRequestDTO(
        @NotNull(message = "Flower ID is required!")
        Long flower_id,

        @NotNull(message = "Growth stage is required!")
        GrowthStage stage,

        @PositiveOrZero(message = "Height must be zero or positive!")
        double height,

        boolean colorChanges,

        String notes,

        LocalDateTime recordedAt,  // Optional: allow manual timestamp

        @PositiveOrZero(message = "Growth since last must be zero or positive!")
        Double growthSinceLast     // Optional: for manual entry
) {}