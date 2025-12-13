package org.example.flowerapp.Models.Enums;

public enum FlowerColor {
    RED("Red"),
    YELLOW("Yellow"),
    PINK("Pink"),
    WHITE("White"),
    PURPLE("Purple");

    private final String displayName;

    FlowerColor(String displayName) {
        this.displayName = displayName;
    }

    public String getColorName() {
        return this.displayName;
    }

    public static FlowerColor fromString(String text) {
        for (FlowerColor color : FlowerColor.values()) {
            if (color.displayName.equalsIgnoreCase(text)) {
                return color;
            }
        }
        throw new IllegalArgumentException("No enum constant for: " + text);
    }
}