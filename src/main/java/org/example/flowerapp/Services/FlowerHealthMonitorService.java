package org.example.flowerapp.Services;

import lombok.extern.slf4j.Slf4j;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Growth;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.GrowthRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
public class FlowerHealthMonitorService {

    private final FlowerRepository flowerRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final GrowthRepository growthRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int WILTING_THRESHOLD_DAYS = 3;
    private static final int DEAD_THRESHOLD_DAYS = 7;

    public FlowerHealthMonitorService(FlowerRepository flowerRepository,
                                      MaintenanceRepository maintenanceRepository,
                                      GrowthRepository growthRepository) {
        this.flowerRepository = flowerRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.growthRepository = growthRepository;
    }

    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void monitorFlowerHealth() {
        log.info("=== Starting flower health monitoring at {} ===",
                LocalDateTime.now().format(DATE_FORMATTER));

        try {
            List<Flower> flowers = flowerRepository.findAllFlower();
            log.info("Found {} flowers to monitor", flowers.size());

            if (flowers.isEmpty()) {
                log.warn("No flowers found in database!");
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int flowersUpdated = 0;

            for (Flower flower : flowers) {
                try {
                    log.debug("Monitoring flower: {} (ID: {})", flower.getFlowerName(), flower.getFlower_id());

                    // Query growth repository directly instead of using flower.getLatestGrowth()
                    Growth currentGrowth = growthRepository.findLatestByFlowerId(flower.getFlower_id());
                    if (currentGrowth == null) {
                        log.debug("  No growth record found, skipping health check");
                        continue;
                    }

                    GrowthStage currentStage = currentGrowth.getStage();
                    log.debug("  Current stage: {}", currentStage);

                    // Skip if already dead
                    if (currentStage == GrowthStage.DEAD) {
                        log.debug("  Flower is already DEAD, skipping");
                        continue;
                    }

                    // Check for overdue tasks
                    int maxOverdueDays = getMaxOverdueDays(flower, now);
                    log.debug("  Max overdue days: {}", maxOverdueDays);

                    GrowthStage newStage = determineNewStage(currentStage, maxOverdueDays);

                    if (newStage != currentStage) {
                        updateFlowerStage(flower, newStage, maxOverdueDays, now, currentGrowth);
                        flowersUpdated++;
                    }
                } catch (Exception e) {
                    log.error("Error processing flower ID: {}", flower.getFlower_id(), e);
                    // Continue with next flower instead of failing entire batch
                }
            }

            log.info("=== Completed flower health monitoring. Updated {} flowers ===", flowersUpdated);
        } catch (Exception e) {
            log.error("Error during flower health monitoring", e);
            throw e;
        }
    }

    private int getMaxOverdueDays(Flower flower, LocalDateTime now) {
        List<Maintenance> incompleteTasks = maintenanceRepository
                .findIncompleteByFlowerIdAndUserId(flower.getFlower_id(), flower.getUserId());

        int maxOverdue = 0;

        for (Maintenance task : incompleteTasks) {
            try {
                LocalDateTime scheduledDate = task.getScheduledDate();
                if (scheduledDate == null) {
                    log.debug("    Task {} has null scheduled date, skipping", task.getTask_id());
                    continue;
                }

                long daysPastDue = ChronoUnit.DAYS.between(scheduledDate, now);

                if (daysPastDue > 0) {
                    // Safe access to task type with null checks
                    String taskTypeName = "UNKNOWN";
                    if (task.getTaskType() != null && task.getTaskType().getMaintenanceType() != null) {
                        taskTypeName = task.getTaskType().getMaintenanceType();
                    }

                    log.debug("    Task {} ({}) is {} days overdue (scheduled: {})",
                            task.getTask_id(),
                            taskTypeName,
                            daysPastDue,
                            scheduledDate.format(DATE_FORMATTER));

                    maxOverdue = Math.max(maxOverdue, (int) daysPastDue);
                }
            } catch (Exception e) {
                log.error("    Error processing task {}: {}", task.getTask_id(), e.getMessage());
                // Continue with next task
            }
        }

        return maxOverdue;
    }

    private GrowthStage determineNewStage(GrowthStage currentStage, int maxOverdueDays) {
        if (maxOverdueDays >= DEAD_THRESHOLD_DAYS) {
            return GrowthStage.DEAD;
        } else if (maxOverdueDays >= WILTING_THRESHOLD_DAYS) {
            return GrowthStage.WILTING;
        }
        return currentStage;
    }

    private void updateFlowerStage(Flower flower, GrowthStage newStage, int overdueDays,
                                   LocalDateTime now, Growth latestGrowth) {
        try {
            Growth newGrowth = new Growth();
            newGrowth.setFlower(flower);
            newGrowth.setStage(newStage);
            newGrowth.setRecordedAt(now);
            newGrowth.setUserId(flower.getUserId());

            // Get current height from latest growth or default
            if (latestGrowth != null) {
                newGrowth.setHeight(latestGrowth.getHeight());
            } else {
                newGrowth.setHeight(0.0);
            }

            // Safe access to stage name
            String stageName = (newStage != null && newStage.getGrowthStage() != null)
                    ? newStage.getGrowthStage()
                    : "UNKNOWN";

            newGrowth.setNotes(String.format(
                    "Auto-updated to %s due to %d days of overdue maintenance",
                    stageName,
                    overdueDays
            ));

            growthRepository.save(newGrowth);

            // Safe access to old stage name
            String oldStageName = "UNKNOWN";
            if (latestGrowth != null && latestGrowth.getStage() != null) {
                oldStageName = latestGrowth.getStage().getGrowthStage() != null
                        ? latestGrowth.getStage().getGrowthStage()
                        : latestGrowth.getStage().name();
            }

            log.info("  ✓ Updated flower '{}' from {} to {} (overdue: {} days)",
                    flower.getFlowerName(),
                    oldStageName,
                    stageName,
                    overdueDays);
        } catch (Exception e) {
            String stageName = (newStage != null) ? newStage.name() : "NULL";
            log.error("  ✗ Failed to update flower '{}' to {}",
                    flower.getFlowerName(),
                    stageName, e);
            throw e;
        }
    }

    /**
     * Manual health check for a specific flower (can be called from REST endpoint)
     */
    @Transactional
    public void checkFlowerHealth(Long flowerId, String userId) {
        log.info("Manual health check requested for flower ID: {} by user: {}", flowerId, userId);

        Flower flower = flowerRepository.findByFlowerIdAndUserId(flowerId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Flower not found with ID: " + flowerId));

        LocalDateTime now = LocalDateTime.now();

        // Query growth repository directly instead of using flower.getLatestGrowth()
        Growth currentGrowth = growthRepository.findLatestByFlowerId(flowerId);

        if (currentGrowth == null) {
            log.warn("No growth record found for flower ID: {}", flowerId);
            return;
        }

        GrowthStage currentStage = currentGrowth.getStage();
        if (currentStage == GrowthStage.DEAD) {
            log.info("Flower is already DEAD");
            return;
        }

        int maxOverdueDays = getMaxOverdueDays(flower, now);
        GrowthStage newStage = determineNewStage(currentStage, maxOverdueDays);

        if (newStage != currentStage) {
            updateFlowerStage(flower, newStage, maxOverdueDays, now, currentGrowth);

            String stageName = (newStage != null && newStage.getGrowthStage() != null)
                    ? newStage.getGrowthStage()
                    : newStage.name();

            log.info("Health check completed - flower updated to {}", stageName);
        } else {
            String stageName = (currentStage != null && currentStage.getGrowthStage() != null)
                    ? currentStage.getGrowthStage()
                    : currentStage.name();

            log.info("Health check completed - flower remains at {}", stageName);
        }
    }
}