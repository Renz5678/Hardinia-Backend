package org.example.flowerapp.Models.Enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum GrowthStage {
    SEED("Seed"),
    SEEDLING("Seedling"),
    BUDDING("Budding"),
    WILTING("Wilting"),
    BLOOMING("Blooming"),
    DEAD("Dead");

    private final String growthStage;

    GrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    @JsonValue  // This tells Jackson to use this value when serializing
    public String getGrowthStage() {
        return this.growthStage;
    }

    @JsonCreator  // This tells Jackson to use this method when deserializing
    public static GrowthStage fromString(String text) {
        for (GrowthStage stage : GrowthStage.values()) {
            if (stage.growthStage.equalsIgnoreCase(text) || stage.name().equalsIgnoreCase(text)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("No enum constant for: " + text);
    }
}