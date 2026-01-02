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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrowthAutomationService {

    private final FlowerRepository flowerRepository;
    private final GrowthRepository growthRepository;
    private final MaintenanceRepository maintenanceRepository;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void performDailyGrowthUpdate() {
        log.info("Starting daily growth update...");

        List<Flower> flowersToUpdate = flowerRepository.findByAutoSchedulingTrue();

        for (Flower flower : flowersToUpdate) {
            try {
                updateFlowerGrowth(flower);
            } catch (Exception e) {
                log.error("Error updating growth for flower ID {}: {}", flower.getFlower_id(), e.getMessage());
            }
        }

        log.info("Daily growth update completed. Updated {} flowers.", flowersToUpdate.size());
    }

    /**
     * Updates growth for a single flower
     */
    public void updateFlowerGrowth(Flower flower) {
        // Get the latest growth record
        Growth latestGrowth = growthRepository
                .findTopByFlowerOrderByRecordedAtDesc(flower)
                .orElse(null);

        if (latestGrowth == null) {
            log.warn("No growth record found for flower ID {}. Creating initial record.", flower.getFlower_id());
            createInitialGrowthRecord(flower);
            return;
        }

        // Check for missed maintenance tasks
        boolean hasMissedTasks = hasMissedMaintenanceTasks(flower);
        boolean allTasksOverdue = areAllMaintenanceTasksOverdue(flower);

        if (allTasksOverdue) {
            log.warn("Flower ID {} has ALL maintenance tasks overdue. Setting to WILTING stage.", flower.getFlower_id());
            setFlowerToWilting(flower, latestGrowth);
            return;
        }

        if (hasMissedTasks) {
            log.info("Flower ID {} has missed maintenance tasks. Skipping growth update.", flower.getFlower_id());
            return;
        }

        // Calculate days since last growth update
        long daysSinceLastUpdate = ChronoUnit.DAYS.between(
                latestGrowth.getRecordedAt().toLocalDate(),
                LocalDateTime.now().toLocalDate()
        );

        if (daysSinceLastUpdate < 1) {
            log.debug("Growth already updated today for flower ID {}", flower.getFlower_id());
            return;
        }

        // Calculate new height
        double currentHeight = latestGrowth.getHeight();
        double growthIncrement = flower.getGrowthRate() * daysSinceLastUpdate;
        double newHeight = Math.min(currentHeight + growthIncrement, flower.getMaxHeight());

        // Check if flower has reached max height
        if (currentHeight >= flower.getMaxHeight()) {
            log.info("Flower ID {} has reached maximum height. No further growth.", flower.getFlower_id());
            return;
        }

        // Determine growth stage based on height percentage
        GrowthStage newStage = determineGrowthStage(newHeight, flower.getMaxHeight(), latestGrowth.getStage());

        // Create new growth record
        Growth newGrowthRecord = new Growth();
        newGrowthRecord.setFlower(flower);
        newGrowthRecord.setHeight(newHeight);
        newGrowthRecord.setStage(newStage);
        newGrowthRecord.setRecordedAt(LocalDateTime.now());
        newGrowthRecord.setGrowthSinceLast(newHeight - currentHeight);
        newGrowthRecord.setColorChanges(latestGrowth.isColorChanges());

        // Add note if stage changed
        if (!newStage.equals(latestGrowth.getStage())) {
            newGrowthRecord.setNotes("Stage changed from " + latestGrowth.getStage() + " to " + newStage);
        }

        growthRepository.save(newGrowthRecord);

        log.info("Growth updated for flower ID {}. Height: {} cm (+{} cm), Stage: {}",
                flower.getFlower_id(), newHeight, newGrowthRecord.getGrowthSinceLast(), newStage);
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
     * Sets a flower to WILTING stage due to neglect
     */
    private void setFlowerToWilting(Flower flower, Growth latestGrowth) {
        Growth wiltingRecord = new Growth();
        wiltingRecord.setFlower(flower);
        wiltingRecord.setHeight(latestGrowth.getHeight());
        wiltingRecord.setStage(GrowthStage.WILTING);
        wiltingRecord.setRecordedAt(LocalDateTime.now());
        wiltingRecord.setGrowthSinceLast(0.0);
        wiltingRecord.setColorChanges(true);
        wiltingRecord.setNotes("Flower is wilting due to neglected maintenance tasks");

        growthRepository.save(wiltingRecord);
        log.info("Flower ID {} has been set to WILTING stage due to all maintenance tasks being overdue",
                flower.getFlower_id());
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
}