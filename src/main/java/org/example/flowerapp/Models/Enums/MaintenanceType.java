package org.example.flowerapp.Models.Enums;

public enum MaintenanceType {
    WATERING("Watering"),
    FERTILIZING("Fertilize"),
    PRUNING("Pruning"),
    PEST_CONTROL("Pest Control"),
    REPOTTING("Repotting");

    private final String maintenanceType;

    MaintenanceType(String maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public static MaintenanceType fromString(String text) {
        for (MaintenanceType type : MaintenanceType.values()) {
            if (type.maintenanceType.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant for: " + text);
    }
}