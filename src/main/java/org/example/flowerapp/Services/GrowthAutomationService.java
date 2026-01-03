package org.example.flowerapp.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Growth;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Models.Enums.GrowthStage;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.GrowthRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthAutomationService {

    private final FlowerRepository flowerRepository;
    private final GrowthRepository growthRepository;
    private final MaintenanceRepository maintenanceRepository;

    // Run weekly: every Sunday at 2 AM
    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void performWeeklyGrowthUpdate() {
        log.info("Starting weekly growth update...");

        List<Flower> flowersToUpdate = flowerRepository.findByAutoSchedulingTrue();
        int updatedCount = 0;

        for (Flower flower : flowersToUpdate) {
            try {
                GrowthUpdateResult result = updateFlowerGrowth(flower);
                if (result.isUpdated()) {
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("Error updating growth for flower ID {}: {}", flower.getFlower_id(), e.getMessage());
            }
        }

        log.info("Weekly growth update completed. Updated {} out of {} flowers.", updatedCount, flowersToUpdate.size());
    }

    /**
     * Updates growth for a single flower by editing the existing record
     * Growth rate is calculated as percentage of max height per week
     */
    public GrowthUpdateResult updateFlowerGrowth(Flower flower) {
        // Get the latest growth record
        Growth existingGrowth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(flower)
                .orElse(null);

        if (existingGrowth == null) {
            log.warn("No growth record found for flower ID {}. Creating initial record.", flower.getFlower_id());
            createInitialGrowthRecord(flower);
            return new GrowthUpdateResult(
                    flower.getFlower_id(),
                    flower.getFlowerName(),
                    false,
                    "Initial growth record created",
                    0.0,
                    0.0,
                    GrowthStage.SEED
            );
        }

        // Store old values for comparison
        double oldHeight = existingGrowth.getHeight();
        GrowthStage oldStage = existingGrowth.getStage();

        // Check for missed maintenance tasks
        boolean hasMissedTasks = hasMissedMaintenanceTasks(flower);
        boolean allTasksOverdue = areAllMaintenanceTasksOverdue(flower);

        // If SOME (but not all) tasks are overdue, skip growth update
        if (hasMissedTasks && !allTasksOverdue) {
            log.info("Flower ID {} has missed maintenance tasks. Skipping growth update.", flower.getFlower_id());
            return new GrowthUpdateResult(
                    flower.getFlower_id(),
                    flower.getFlowerName(),
                    false,
                    "Skipped - maintenance overdue",
                    existingGrowth.getHeight(),
                    existingGrowth.getHeight(),
                    existingGrowth.getStage()
            );
        }

        // If ALL tasks are overdue, set to WILTING
        if (allTasksOverdue) {
            log.warn("Flower ID {} has ALL maintenance tasks overdue. Setting to WILTING stage.", flower.getFlower_id());

            // UPDATE existing record to WILTING
            existingGrowth.setStage(GrowthStage.WILTING);
            existingGrowth.setColorChanges(true);
            existingGrowth.setNotes("Flower is wilting due to neglected maintenance tasks");
            existingGrowth.setRecordedAt(LocalDateTime.now());
            existingGrowth.setGrowthSinceLast(0.0);

            growthRepository.save(existingGrowth);

            return new GrowthUpdateResult(
                    flower.getFlower_id(),
                    flower.getFlowerName(),
                    true,
                    "Set to WILTING - all maintenance overdue",
                    oldHeight,
                    existingGrowth.getHeight(),
                    GrowthStage.WILTING
            );
        }

        // Calculate weeks since last growth update
        long daysSinceLastUpdate = ChronoUnit.DAYS.between(
                existingGrowth.getRecordedAt().toLocalDate(),
                LocalDateTime.now().toLocalDate()
        );

        double weeksSinceLastUpdate = daysSinceLastUpdate / 7.0;

        // Must be at least 5 days before updating
        if (daysSinceLastUpdate < 5) {
            log.debug("Growth update too soon for flower ID {}. Only {} days since last update.",
                    flower.getFlower_id(), daysSinceLastUpdate);
            return new GrowthUpdateResult(
                    flower.getFlower_id(),
                    flower.getFlowerName(),
                    false,
                    "Update too soon (need at least 5 days)",
                    existingGrowth.getHeight(),
                    existingGrowth.getHeight(),
                    existingGrowth.getStage()
            );
        }

        // Check if flower has reached max height
        double currentHeight = existingGrowth.getHeight();
        if (currentHeight >= flower.getMaxHeight()) {
            log.info("Flower ID {} has reached maximum height. No further growth.", flower.getFlower_id());
            return new GrowthUpdateResult(
                    flower.getFlower_id(),
                    flower.getFlowerName(),
                    false,
                    "At maximum height",
                    currentHeight,
                    currentHeight,
                    existingGrowth.getStage()
            );
        }

        // Calculate new height based on weekly growth rate percentage
        // growthRate is stored as weekly percentage (e.g., 7 for 7% per week)
        double actualGrowthRate = flower.getGrowthRate() * weeksSinceLastUpdate; // Adjust for actual time elapsed

        // Calculate growth as percentage of max height
        double growthIncrement = (actualGrowthRate / 100.0) * flower.getMaxHeight();
        double newHeight = Math.min(currentHeight + growthIncrement, flower.getMaxHeight());

        // Determine growth stage based on height percentage
        GrowthStage newStage = determineGrowthStage(newHeight, flower.getMaxHeight(), existingGrowth.getStage());

        // UPDATE existing growth record (not create new)
        existingGrowth.setHeight(newHeight);
        existingGrowth.setStage(newStage);
        existingGrowth.setRecordedAt(LocalDateTime.now());
        existingGrowth.setGrowthSinceLast(newHeight - currentHeight);

        // Add note if stage changed
        if (!newStage.equals(oldStage)) {
            existingGrowth.setNotes("Stage changed from " + oldStage + " to " + newStage);
        } else {
            existingGrowth.setNotes(String.format("Grew %.1f cm over %.1f weeks (%.1f%% of max height)",
                    existingGrowth.getGrowthSinceLast(), weeksSinceLastUpdate, actualGrowthRate));
        }

        growthRepository.save(existingGrowth);  // This UPDATES the existing record

        log.info("Growth updated for flower ID {}. Height: {} cm (+{} cm over {:.1f} weeks), Stage: {}",
                flower.getFlower_id(), newHeight, existingGrowth.getGrowthSinceLast(), weeksSinceLastUpdate, newStage);

        String message = String.format("Updated successfully - grew %.1f cm over %.1f weeks (%.1f%% growth)",
                existingGrowth.getGrowthSinceLast(), weeksSinceLastUpdate, actualGrowthRate);

        if (!newStage.equals(oldStage)) {
            message += String.format(" - Stage: %s → %s", oldStage, newStage);
        }

        return new GrowthUpdateResult(
                flower.getFlower_id(),
                flower.getFlowerName(),
                true,
                message,
                oldHeight,
                newHeight,
                newStage
        );
    }

    /**
     * Performs batch update and returns summary
     */
    public String performWeeklyGrowthUpdateWithSummary() {
        log.info("Starting weekly growth update...");

        List<Flower> flowersToUpdate = flowerRepository.findByAutoSchedulingTrue();
        List<GrowthUpdateResult> results = new ArrayList<>();

        for (Flower flower : flowersToUpdate) {
            try {
                GrowthUpdateResult result = updateFlowerGrowth(flower);
                results.add(result);
            } catch (Exception e) {
                log.error("Error updating growth for flower ID {}: {}", flower.getFlower_id(), e.getMessage());
                results.add(new GrowthUpdateResult(
                        flower.getFlower_id(),
                        flower.getFlowerName(),
                        false,
                        "Error: " + e.getMessage(),
                        0.0,
                        0.0,
                        null
                ));
            }
        }

        // Build summary
        StringBuilder summary = new StringBuilder();
        summary.append("=== Weekly Growth Update Summary ===\n\n");
        summary.append(String.format("Total flowers processed: %d\n", flowersToUpdate.size()));

        long updated = results.stream().filter(GrowthUpdateResult::isUpdated).count();
        summary.append(String.format("Successfully updated: %d\n", updated));
        summary.append(String.format("Skipped/No change: %d\n\n", flowersToUpdate.size() - updated));

        summary.append("Details:\n");
        for (GrowthUpdateResult result : results) {
            summary.append(String.format("- [ID: %d] %s: %s (Height: %.1f → %.1f cm, Stage: %s)\n",
                    result.getFlowerId(),
                    result.getFlowerName(),
                    result.getMessage(),
                    result.getOldHeight(),
                    result.getNewHeight(),
                    result.getStage()
            ));
        }

        log.info("Weekly growth update completed. Updated {} out of {} flowers.", updated, flowersToUpdate.size());

        return summary.toString();
    }

    /**
     * Checks if a flower has any missed maintenance tasks
     */
    private boolean hasMissedMaintenanceTasks(Flower flower) {
        LocalDateTime now = LocalDateTime.now();

        // Check for overdue watering
        if (flower.getWaterFrequencyDays() != null && flower.getLastWateredDate() != null) {
            LocalDateTime nextWaterDue = flower.getLastWateredDate().plusDays(flower.getWaterFrequencyDays());
            if (now.isAfter(nextWaterDue)) {
                log.debug("Flower ID {} has overdue watering", flower.getFlower_id());
                return true;
            }
        }

        // Check for overdue fertilizing
        if (flower.getFertilizeFrequencyDays() != null && flower.getLastFertilizedDate() != null) {
            LocalDateTime nextFertilizeDue = flower.getLastFertilizedDate().plusDays(flower.getFertilizeFrequencyDays());
            if (now.isAfter(nextFertilizeDue)) {
                log.debug("Flower ID {} has overdue fertilizing", flower.getFlower_id());
                return true;
            }
        }

        // Check for overdue pruning
        if (flower.getPruneFrequencyDays() != null && flower.getLastPrunedDate() != null) {
            LocalDateTime nextPruneDue = flower.getLastPrunedDate().plusDays(flower.getPruneFrequencyDays());
            if (now.isAfter(nextPruneDue)) {
                log.debug("Flower ID {} has overdue pruning", flower.getFlower_id());
                return true;
            }
        }

        // Optional: Check MaintenanceDetails table for any incomplete tasks
        List<Maintenance> pendingTasks = maintenanceRepository
                .findByFlowerAndCompletedFalseAndDueDateBefore(flower, now);

        if (!pendingTasks.isEmpty()) {
            log.debug("Flower ID {} has {} pending maintenance tasks",
                    flower.getFlower_id(), pendingTasks.size());
            return true;
        }

        return false;
    }

    /**
     * Checks if ALL maintenance tasks are overdue (flower should wilt)
     */
    private boolean areAllMaintenanceTasksOverdue(Flower flower) {
        LocalDateTime now = LocalDateTime.now();
        boolean hasWateringTask = flower.getWaterFrequencyDays() != null && flower.getLastWateredDate() != null;
        boolean hasFertilizingTask = flower.getFertilizeFrequencyDays() != null && flower.getLastFertilizedDate() != null;
        boolean hasPruningTask = flower.getPruneFrequencyDays() != null && flower.getLastPrunedDate() != null;

        // If no maintenance tasks are configured, return false
        if (!hasWateringTask && !hasFertilizingTask && !hasPruningTask) {
            return false;
        }

        boolean wateringOverdue = false;
        boolean fertilizingOverdue = false;
        boolean pruningOverdue = false;

        // Check each configured task
        if (hasWateringTask) {
            LocalDateTime nextWaterDue = flower.getLastWateredDate().plusDays(flower.getWaterFrequencyDays());
            wateringOverdue = now.isAfter(nextWaterDue);
        }

        if (hasFertilizingTask) {
            LocalDateTime nextFertilizeDue = flower.getLastFertilizedDate().plusDays(flower.getFertilizeFrequencyDays());
            fertilizingOverdue = now.isAfter(nextFertilizeDue);
        }

        if (hasPruningTask) {
            LocalDateTime nextPruneDue = flower.getLastPrunedDate().plusDays(flower.getPruneFrequencyDays());
            pruningOverdue = now.isAfter(nextPruneDue);
        }

        // All configured tasks must be overdue
        boolean allOverdue = true;
        if (hasWateringTask) allOverdue = allOverdue && wateringOverdue;
        if (hasFertilizingTask) allOverdue = allOverdue && fertilizingOverdue;
        if (hasPruningTask) allOverdue = allOverdue && pruningOverdue;

        return allOverdue;
    }

    /**
     * Determines growth stage based on current height percentage
     * SEED: 0-20%
     * SEEDLING: 20-40%
     * BUDDING: 40-80%
     * BLOOMING: 80-100%
     * WILTING: When flower is dying (not based on height progression)
     */
    private GrowthStage determineGrowthStage(double currentHeight, double maxHeight, GrowthStage currentStage) {
        double heightPercentage = (currentHeight / maxHeight) * 100;

        // If already wilting, keep it wilting (dying state)
        if (currentStage == GrowthStage.WILTING) {
            return GrowthStage.WILTING;
        }

        if (heightPercentage < 20) {
            return GrowthStage.SEED;
        } else if (heightPercentage < 40) {
            return GrowthStage.SEEDLING;
        } else if (heightPercentage < 80) {
            return GrowthStage.BUDDING;
        } else {
            return GrowthStage.BLOOMING;
        }
    }

    /**
     * Creates an initial growth record for a flower that doesn't have one
     */
    private void createInitialGrowthRecord(Flower flower) {
        Growth initialGrowth = new Growth();
        initialGrowth.setFlower(flower);
        initialGrowth.setHeight(0.0);
        initialGrowth.setStage(GrowthStage.SEED);
        initialGrowth.setRecordedAt(flower.getPlantingDate() != null ?
                flower.getPlantingDate() : LocalDateTime.now());
        initialGrowth.setGrowthSinceLast(0.0);
        initialGrowth.setColorChanges(false);
        initialGrowth.setNotes("Initial growth record");

        growthRepository.save(initialGrowth);
        log.info("Created initial growth record for flower ID {}", flower.getFlower_id());
    }

    /**
     * Inner class to hold growth update results
     */
    public static class GrowthUpdateResult {
        private final Long flowerId;
        private final String flowerName;
        private final boolean updated;
        private final String message;
        private final double oldHeight;
        private final double newHeight;
        private final GrowthStage stage;

        public GrowthUpdateResult(Long flowerId, String flowerName, boolean updated, String message,
                                  double oldHeight, double newHeight, GrowthStage stage) {
            this.flowerId = flowerId;
            this.flowerName = flowerName;
            this.updated = updated;
            this.message = message;
            this.oldHeight = oldHeight;
            this.newHeight = newHeight;
            this.stage = stage;
        }

        public Long getFlowerId() { return flowerId; }
        public String getFlowerName() { return flowerName; }
        public boolean isUpdated() { return updated; }
        public String getMessage() { return message; }
        public double getOldHeight() { return oldHeight; }
        public double getNewHeight() { return newHeight; }
        public GrowthStage getStage() { return stage; }
    }
}