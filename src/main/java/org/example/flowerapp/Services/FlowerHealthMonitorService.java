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
import org.springframework.transaction.annotation.Propagation;
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

            int flowersUpdated = 0;
            int flowersErrored = 0;

            for (Flower flower : flowers) {
                try {
                    // Process each flower in its own transaction
                    processFlowerHealth(flower);
                    flowersUpdated++;
                } catch (Exception e) {
                    flowersErrored++;
                    log.error("Error processing flower ID: {} - {}",
                            flower.getFlower_id(), e.getMessage());
                    // Continue with next flower
                }
            }

            log.info("=== Completed flower health monitoring. Updated {} flowers, {} errors ===",
                    flowersUpdated, flowersErrored);
        } catch (Exception e) {
            log.error("Error during flower health monitoring", e);
            throw e;
        }
    }

    /**
     * Process health check for a single flower in its own transaction.
     * Using REQUIRES_NEW ensures each flower gets its own transaction,
     * so failures don't cascade to other flowers.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processFlowerHealth(Flower flower) {
        LocalDateTime now = LocalDateTime.now();

        log.debug("Monitoring flower: {} (ID: {})", flower.getFlowerName(), flower.getFlower_id());

        // Query growth repository directly
        Growth currentGrowth = growthRepository.findLatestByFlowerId(flower.getFlower_id());
        if (currentGrowth == null) {
            log.debug("  No growth record found, skipping health check");
            return;
        }

        GrowthStage currentStage = currentGrowth.getStage();
        log.debug("  Current stage: {}", currentStage);

        // Skip if already dead
        if (currentStage == GrowthStage.DEAD) {
            log.debug("  Flower is already DEAD, skipping");
            return;
        }

        // Check for overdue tasks
        int maxOverdueDays = getMaxOverdueDays(flower, now);
        log.debug("  Max overdue days: {}", maxOverdueDays);

        GrowthStage newStage = determineNewStage(currentStage, maxOverdueDays);

        if (newStage != currentStage) {
            updateFlowerStage(flower, newStage, maxOverdueDays, now, currentGrowth);
        }
    }

    /**
     * Cleans up orphaned maintenance tasks and growth records for non-existent flowers.
     * This deletes records that reference flower IDs that no longer exist in the database.
     */
    public void cleanupOrphanedRecords() {
        log.info("=== Starting orphaned records cleanup at {} ===",
                LocalDateTime.now().format(DATE_FORMATTER));

        try {
            int maintenanceDeleted = 0;
            int maintenanceSkipped = 0;
            int growthDeleted = 0;
            int growthSkipped = 0;

            // Get all maintenance records
            List<Maintenance> allMaintenance = maintenanceRepository.findAll();
            log.info("Checking {} maintenance records for orphans", allMaintenance.size());

            for (Maintenance maintenance : allMaintenance) {
                try {
                    // Process each cleanup in its own transaction
                    boolean deleted = cleanupMaintenanceRecord(maintenance);
                    if (deleted) {
                        maintenanceDeleted++;
                    } else {
                        maintenanceSkipped++;
                    }
                } catch (Exception e) {
                    log.error("  ✗ Error checking maintenance record {}: {}",
                            maintenance.getTask_id(), e.getMessage());
                    maintenanceSkipped++;
                }
            }

            // Get all growth records
            List<Growth> allGrowth = growthRepository.findAll();
            log.info("Checking {} growth records for orphans", allGrowth.size());

            for (Growth growth : allGrowth) {
                try {
                    // Process each cleanup in its own transaction
                    boolean deleted = cleanupGrowthRecord(growth);
                    if (deleted) {
                        growthDeleted++;
                    } else {
                        growthSkipped++;
                    }
                } catch (Exception e) {
                    log.error("  ✗ Error checking growth record {}: {}",
                            growth.getGrowth_id(), e.getMessage());
                    growthSkipped++;
                }
            }

            log.info("=== Completed orphaned records cleanup ===");
            log.info("Maintenance: {} deleted, {} skipped/kept", maintenanceDeleted, maintenanceSkipped);
            log.info("Growth: {} deleted, {} skipped/kept", growthDeleted, growthSkipped);

        } catch (Exception e) {
            log.error("Error during orphaned records cleanup", e);
            throw e;
        }
    }

    /**
     * Checks and deletes a maintenance record if its flower no longer exists.
     * Returns true if deleted, false if kept.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected boolean cleanupMaintenanceRecord(Maintenance maintenance) {
        try {
            Long flowerId = maintenance.getFlower().getFlower_id();

            // Check if ANY flower with this ID exists (regardless of user)
            boolean flowerExists = flowerRepository.existsById(flowerId);

            if (!flowerExists) {
                // Flower doesn't exist at all - delete the orphaned maintenance record
                maintenanceRepository.delete(maintenance);
                log.info("  ✓ Deleted orphaned maintenance task {} for non-existent flower ID: {}",
                        maintenance.getTask_id(), flowerId);
                return true;
            } else {
                // Flower exists - keep the record
                log.debug("  ○ Maintenance task {} for flower ID {} - flower exists, keeping record",
                        maintenance.getTask_id(), flowerId);
                return false;
            }
        } catch (Exception e) {
            log.error("  ✗ Error processing maintenance record {}: {}",
                    maintenance.getTask_id(), e.getMessage());
            return false;
        }
    }

    /**
     * Checks and deletes a growth record if its flower no longer exists.
     * Returns true if deleted, false if kept.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected boolean cleanupGrowthRecord(Growth growth) {
        try {
            Long flowerId = growth.getFlower().getFlower_id();

            // Check if ANY flower with this ID exists (regardless of user)
            boolean flowerExists = flowerRepository.existsById(flowerId);

            if (!flowerExists) {
                // Flower doesn't exist at all - delete the orphaned growth record
                growthRepository.delete(growth);
                log.info("  ✓ Deleted orphaned growth record {} for non-existent flower ID: {}",
                        growth.getGrowth_id(), flowerId);
                return true;
            } else {
                // Flower exists - keep the record
                log.debug("  ○ Growth record {} for flower ID {} - flower exists, keeping record",
                        growth.getGrowth_id(), flowerId);
                return false;
            }
        } catch (Exception e) {
            log.error("  ✗ Error processing growth record {}: {}",
                    growth.getGrowth_id(), e.getMessage());
            return false;
        }
    }

    /**
     * Cleans up orphaned records for a specific flower ID.
     * Useful when a flower has been deleted but related records remain.
     * This will delete ALL maintenance and growth records for the specified flower ID.
     */
    @Transactional
    public void cleanupOrphanedRecordsByFlowerId(Long flowerId) {
        log.info("=== Starting orphaned records cleanup for flower ID: {} ===", flowerId);

        try {
            int maintenanceDeleted = 0;
            int growthDeleted = 0;

            // Check if flower exists
            boolean flowerExists = flowerRepository.existsById(flowerId);

            if (flowerExists) {
                log.warn("Flower ID {} exists in database. No cleanup needed.", flowerId);
                return;
            }

            log.info("Flower ID {} does not exist. Cleaning up orphaned records...", flowerId);

            // Delete maintenance records
            List<Maintenance> maintenanceRecords = maintenanceRepository.findByFlowerId(flowerId);
            for (Maintenance maintenance : maintenanceRecords) {
                maintenanceRepository.delete(maintenance);
                maintenanceDeleted++;
                log.debug("  Deleted maintenance task {}", maintenance.getTask_id());
            }

            // Delete growth records
            List<Growth> growthRecords = growthRepository.findByFlowerId(flowerId);
            for (Growth growth : growthRecords) {
                growthRepository.delete(growth);
                growthDeleted++;
                log.debug("  Deleted growth record {}", growth.getGrowth_id());
            }

            log.info("=== Completed cleanup for flower ID: {}. Deleted {} maintenance tasks and {} growth records ===",
                    flowerId, maintenanceDeleted, growthDeleted);

        } catch (Exception e) {
            log.error("Error during cleanup for flower ID: {}", flowerId, e);
            throw e;
        }
    }

    /**
     * Scheduled cleanup job that runs daily at 2 AM to remove orphaned records
     */
    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2 AM
    public void scheduledOrphanedRecordsCleanup() {
        log.info("Running scheduled orphaned records cleanup");
        cleanupOrphanedRecords();
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

        // Query growth repository directly
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