package org.example.flowerapp.DTO;

import org.example.flowerapp.Models.Enums.GrowthStage;

import java.time.LocalDateTime;

public record GrowthResponseDTO(
        Long growth_id,
        Long flower_id,
        GrowthStage stage,
        double height,
        boolean colorChanges,
        String notes,
        LocalDateTime recordedAt,
        Double growthSinceLast
) {}