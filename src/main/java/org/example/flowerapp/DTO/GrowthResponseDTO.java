package org.example.flowerapp.DTO;

import org.example.flowerapp.Models.Enums.GrowthStage;

public record GrowthResponseDTO(
        Long growth_id,
        Long flower_id,
        GrowthStage stage,
        double height,
        boolean colorChanges,
        String notes
) {}