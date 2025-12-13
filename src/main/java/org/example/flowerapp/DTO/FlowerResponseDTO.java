package org.example.flowerapp.DTO;

import org.example.flowerapp.Models.Enums.FlowerColor;

import java.time.LocalDateTime;

public record FlowerResponseDTO (
        Long flower_id,
        String flowerName,
        String species,
        FlowerColor color,
        LocalDateTime plantingDate) {
}