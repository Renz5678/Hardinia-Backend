package org.example.flowerapp.Services;

import org.example.flowerapp.Models.SimplifiedWeather;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public SimplifiedWeather getWeather(String city) {
        String url = apiUrl + "/current.json?key=" + apiKey + "&q=" + city;

        try {
            String response = restTemplate.getForObject(url, String.class);
            Map<String, Object> jsonMap = objectMapper.readValue(response, Map.class);
            Map<String, Object> current = (Map<String, Object>) jsonMap.get("current");
            Map<String, Object> condition = (Map<String, Object>) current.get("condition");
            int conditionCode = (Integer) condition.get("code");

            return mapToSimplifiedWeather(conditionCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch weather data", e);
        }
    }

    public SimplifiedWeather getWeatherByCoordinates(double lat, double lng) {
        String coordinates = lat + "," + lng;
        String url = apiUrl + "/current.json?key=" + apiKey + "&q=" + coordinates;

        try {
            String response = restTemplate.getForObject(url, String.class);
            Map<String, Object> jsonMap = objectMapper.readValue(response, Map.class);
            Map<String, Object> current = (Map<String, Object>) jsonMap.get("current");
            Map<String, Object> condition = (Map<String, Object>) current.get("condition");
            int conditionCode = (Integer) condition.get("code");

            return mapToSimplifiedWeather(conditionCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch weather data", e);
        }
    }

    private SimplifiedWeather mapToSimplifiedWeather(int code) {
        return switch (code) {
            // SUNNY
            case 1000 -> new SimplifiedWeather("SUNNY", "Sunny");

            // SUNNY_CLOUDS
            case 1003 -> new SimplifiedWeather("SUNNY_CLOUDS", "Sunny with Clouds");

            // CLOUDY
            case 1006, 1009, 1030 -> new SimplifiedWeather("CLOUDY", "Cloudy");

            // SUNNY_RAIN (Patchy/Light rain)
            case 1063, 1180, 1183, 1240 -> new SimplifiedWeather("SUNNY_RAIN", "Sunny with Rain");

            // RAINY
            case 1150, 1153, 1168, 1171, 1186, 1189, 1192, 1195, 1198, 1201,
                 1243, 1246 -> new SimplifiedWeather("RAINY", "Rainy");

            // THUNDER (Possible thunderstorms)
            case 1087 -> new SimplifiedWeather("THUNDER", "Thunder");

            // THUNDERSTORM (Active thunderstorms)
            case 1273, 1276 -> new SimplifiedWeather("THUNDERSTORM", "Thunderstorm");

            // Default to cloudy if unknown
            default -> new SimplifiedWeather("CLOUDY", "Cloudy");
        };
    }
}