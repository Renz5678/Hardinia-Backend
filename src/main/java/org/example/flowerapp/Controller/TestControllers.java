package org.example.flowerapp.Controller;

import lombok.RequiredArgsConstructor;
import org.example.flowerapp.Services.FlowerMaintenanceScheduler;
import org.example.flowerapp.Services.GrowthAutomationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestControllers {
    private final FlowerMaintenanceScheduler flowerMaintenanceScheduler;
    private final GrowthAutomationService growthAutomationService;

    @PostMapping("/run-scheduler")
    public String runScheduler() {
        flowerMaintenanceScheduler.scheduleMaintenanceTasks();
        return "Scheduler executed successfully";
    }

    @PostMapping("/run_growth")
    public String runGrowth() {
        return growthAutomationService.performDailyGrowthUpdateWithSummary();
    }
}