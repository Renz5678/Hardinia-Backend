package org.example.flowerapp.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.flowerapp.Models.Enums.FlowerColor;

import java.time.LocalDateTime;

public record FlowerRequestDTO (
        @NotBlank(message = "Flower name is required")
        String flowerName,

        @NotBlank(message = "Species field must be indicated!")
        String species,

        @NotNull(message = "Enter a color!")
        FlowerColor color,

        LocalDateTime plantingDate,

        Integer gridPosition,
        Integer waterFrequencyDays,
        Integer fertilizeFrequencyDays,
        Integer pruneFrequencyDays,
        LocalDateTime lastWateredDate,
        LocalDateTime lastFertilizedDate,
        LocalDateTime lastPrunedDate,
        Boolean autoScheduling,

        @Positive(message = "Max height must be greater than 0")
        Double maxHeight,

        @Positive(message = "Growth rate must be greater than 0")
        Double growthRate) {
}