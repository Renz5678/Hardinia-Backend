package org.example.flowerapp.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.flowerapp.Models.Enums.FlowerColor;

import java.time.LocalDateTime;

public record FlowerRequestDTO (
        @NotBlank(message = "Flower name is required")
        String flowerName,

        @NotBlank(message = "Species field must be indicated!")
        String species,

        @NotNull(message = "Enter a color!")
        FlowerColor color,

        LocalDateTime plantingDate) {
}