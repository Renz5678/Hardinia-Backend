package org.example.flowerapp.Models.Enums;

public enum GrowthStage {
    SEED("Seed"),
    SEEDLING("Seedling"),
    BUDDING("Budding"),
    WILTING("Wilting"),
    BLOOMING("Blooming");

    private final String growthStage;

    GrowthStage(String growthStage) {
        this.growthStage = growthStage;
    }

    public String getGrowthStage() {
        return this.growthStage;
    }

    public static GrowthStage fromString(String text) {
        for (GrowthStage stage : GrowthStage.values()) {
            if (stage.growthStage.equalsIgnoreCase(text)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("No enum constant for: " + text);
    }
}