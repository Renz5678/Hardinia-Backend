package org.example.flowerapp.Controller;

import org.example.flowerapp.Models.SimplifiedWeather;
import org.example.flowerapp.Services.WeatherService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
@CrossOrigin(origins = "http://localhost:5173")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{city}")
    public SimplifiedWeather getWeather(@PathVariable String city) {
        return weatherService.getWeather(city);
    }

    // Get weather by coordinates
    @GetMapping("/coordinates")
    public SimplifiedWeather getWeatherByCoordinates(
            @RequestParam double lat,
            @RequestParam double lng) {
        return weatherService.getWeatherByCoordinates(lat, lng);
    }

    // Convenience endpoint for Manila
    @GetMapping
    public SimplifiedWeather getManilaWeather() {
        return weatherService.getWeather("Manila");
    }
}
