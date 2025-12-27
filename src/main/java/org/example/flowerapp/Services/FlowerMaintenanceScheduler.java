package org.example.flowerapp.Services;

import lombok.extern.slf4j.Slf4j;
import org.example.flowerapp.Models.Enums.MaintenanceType;
import org.example.flowerapp.Models.Flower;
import org.example.flowerapp.Models.Maintenance;
import org.example.flowerapp.Repository.FlowerRepository;
import org.example.flowerapp.Repository.MaintenanceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class FlowerMaintenanceScheduler {

    private final FlowerRepository flowerRepository;
    private final MaintenanceRepository maintenanceRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public FlowerMaintenanceScheduler(FlowerRepository flowerRepository,
                                      MaintenanceRepository maintenanceRepository) {
        this.flowerRepository = flowerRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Scheduled(cron = "0 0 6 * * *") // 6 AM daily
    @Transactional
    public void scheduleMaintenanceTasks() {
        log.info("=== Starting scheduled maintenance task generation at {} ===",
                LocalDateTime.now().format(DATE_FORMATTER));

        try {
            List<Flower> flowers = flowerRepository.findAllFlower();
            log.info("Found {} flowers to process", flowers.size());

            if (flowers.isEmpty()) {
                log.warn("No flowers found in database!");
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            int tasksCreated = 0;

            for (Flower flower : flowers) {
                log.debug("Processing flower: {} (ID: {})", flower.getFlowerName(), flower.getFlower_id());
                log.debug("  Auto-scheduling enabled: {}", flower.isAutoScheduling());

                tasksCreated += checkAndScheduleWatering(flower, now);
                tasksCreated += checkAndScheduleFertilizing(flower, now);
                tasksCreated += checkAndSchedulePruning(flower, now);
            }

            log.info("=== Completed maintenance task generation. Created {} new tasks ===", tasksCreated);
        } catch (Exception e) {
            log.error("Error during scheduled maintenance task generation", e);
            throw e;
        }
    }

    private int checkAndScheduleWatering(Flower flower, LocalDateTime now) {
        log.debug("  Checking watering for flower: {}", flower.getFlowerName());

        if (flower.getWaterFrequencyDays() == null) {
            log.debug("    Skipped: No water frequency set");
            return 0;
        }

        if (!flower.isAutoScheduling()) {
            log.debug("    Skipped: Auto-scheduling disabled");
            return 0;
        }

        LocalDateTime lastWatered = flower.getLastWateredDate();
        LocalDateTime nextWaterDate = lastWatered != null
                ? lastWatered.plusDays(flower.getWaterFrequencyDays())
                : flower.getPlantingDate();

        log.debug("    Last watered: {}", lastWatered != null ? lastWatered.format(DATE_FORMATTER) : "NEVER");
        log.debug("    Next water date: {}", nextWaterDate.format(DATE_FORMATTER));
        log.debug("    Current time: {}", now.format(DATE_FORMATTER));
        log.debug("    Should water: {}", !now.isBefore(nextWaterDate));

        // Fixed: Use !now.isBefore() instead of now.isAfter() to include same-day scheduling
        if (!now.isBefore(nextWaterDate) && !taskExistsForToday(flower, MaintenanceType.WATERING)) {
            createMaintenanceTask(flower, MaintenanceType.WATERING, now);
            return 1;
        } else {
            if (taskExistsForToday(flower, MaintenanceType.WATERING)) {
                log.debug("    Skipped: Task already exists for today");
            }
        }
        return 0;
    }

    private int checkAndScheduleFertilizing(Flower flower, LocalDateTime now) {
        log.debug("  Checking fertilizing for flower: {}", flower.getFlowerName());

        if (flower.getFertilizeFrequencyDays() == null) {
            log.debug("    Skipped: No fertilize frequency set");
            return 0;
        }

        if (!flower.isAutoScheduling()) {
            log.debug("    Skipped: Auto-scheduling disabled");
            return 0;
        }

        LocalDateTime lastFertilized = flower.getLastFertilizedDate();
        LocalDateTime nextFertilizeDate = lastFertilized != null
                ? lastFertilized.plusDays(flower.getFertilizeFrequencyDays())
                : flower.getPlantingDate().plusDays(flower.getFertilizeFrequencyDays());

        log.debug("    Last fertilized: {}", lastFertilized != null ? lastFertilized.format(DATE_FORMATTER) : "NEVER");
        log.debug("    Next fertilize date: {}", nextFertilizeDate.format(DATE_FORMATTER));
        log.debug("    Should fertilize: {}", !now.isBefore(nextFertilizeDate));

        if (!now.isBefore(nextFertilizeDate) && !taskExistsForToday(flower, MaintenanceType.FERTILIZING)) {
            createMaintenanceTask(flower, MaintenanceType.FERTILIZING, now);
            return 1;
        } else {
            if (taskExistsForToday(flower, MaintenanceType.FERTILIZING)) {
                log.debug("    Skipped: Task already exists for today");
            }
        }
        return 0;
    }

    private int checkAndSchedulePruning(Flower flower, LocalDateTime now) {
        log.debug("  Checking pruning for flower: {}", flower.getFlowerName());

        if (flower.getPruneFrequencyDays() == null) {
            log.debug("    Skipped: No prune frequency set");
            return 0;
        }

        if (!flower.isAutoScheduling()) {
            log.debug("    Skipped: Auto-scheduling disabled");
            return 0;
        }

        LocalDateTime lastPruned = flower.getLastPrunedDate();
        LocalDateTime nextPruneDate = lastPruned != null
                ? lastPruned.plusDays(flower.getPruneFrequencyDays())
                : flower.getPlantingDate().plusDays(flower.getPruneFrequencyDays());

        log.debug("    Last pruned: {}", lastPruned != null ? lastPruned.format(DATE_FORMATTER) : "NEVER");
        log.debug("    Next prune date: {}", nextPruneDate.format(DATE_FORMATTER));
        log.debug("    Should prune: {}", !now.isBefore(nextPruneDate));

        if (!now.isBefore(nextPruneDate) && !taskExistsForToday(flower, MaintenanceType.PRUNING)) {
            createMaintenanceTask(flower, MaintenanceType.PRUNING, now);
            return 1;
        } else {
            if (taskExistsForToday(flower, MaintenanceType.PRUNING)) {
                log.debug("    Skipped: Task already exists for today");
            }
        }
        return 0;
    }

    private boolean taskExistsForToday(Flower flower, MaintenanceType type) {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        boolean exists = maintenanceRepository.existsByFlowerAndTypeAndDateRange(
                flower.getFlower_id(), type, startOfDay, endOfDay
        );

        log.debug("    Checking if {} task exists for today ({} to {}): {}",
                type.getMaintenanceType(),
                startOfDay.format(DATE_FORMATTER),
                endOfDay.format(DATE_FORMATTER),
                exists);

        return exists;
    }

    private void createMaintenanceTask(Flower flower, MaintenanceType type, LocalDateTime scheduledDate) {
        try {
            Maintenance task = new Maintenance();
            task.setFlower(flower);
            task.setTaskType(type);
            task.setScheduledDate(scheduledDate);
            task.setAutoGenerated(true);
            task.setCompleted(false);
            task.setCreatedAt(LocalDateTime.now());
            task.setNotes("Auto-generated " + type.getMaintenanceType() + " task for " + flower.getFlowerName());

            maintenanceRepository.save(task);
            log.info("    ✓ Created {} task for flower: {} (scheduled: {})",
                    type.getMaintenanceType(),
                    flower.getFlowerName(),
                    scheduledDate.format(DATE_FORMATTER));
        } catch (Exception e) {
            log.error("    ✗ Failed to create {} task for flower: {}",
                    type.getMaintenanceType(),
                    flower.getFlowerName(), e);
            throw e;
        }
    }
}