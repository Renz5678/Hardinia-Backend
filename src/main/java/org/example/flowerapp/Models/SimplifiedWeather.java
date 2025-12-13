package org.example.flowerapp.Models;

public class SimplifiedWeather {
    private String code;        // Frontend-friendly code
    private String condition;   // Human-readable condition

    public SimplifiedWeather() {}

    public SimplifiedWeather(String code, String condition) {
        this.code = code;
        this.condition = condition;
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
